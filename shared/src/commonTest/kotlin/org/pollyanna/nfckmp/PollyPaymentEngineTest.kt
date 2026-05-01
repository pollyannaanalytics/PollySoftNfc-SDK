package org.pollyanna.nfckmp

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.pollyanna.nfckmp.model.CardReadResult
import org.pollyanna.nfckmp.model.PaymentState
import org.pollyanna.nfckmp.model.SecurePayload
import org.pollyanna.nfckmp.model.SecurityError
import org.pollyanna.nfckmp.model.SecurityResult
import org.pollyanna.nfckmp.network.BackendService
import org.pollyanna.nfckmp.nfc_provider.CardReadRepository
import org.pollyanna.nfckmp.security.SecurityRepository
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PollyPaymentEngineTest {

    // region fakes

    private fun backendService(
        publicKey: ByteArray = byteArrayOf(1, 2, 3),
        onRegister: (ByteArray) -> Unit = {},
        onSubmit: () -> Unit = {},
    ) = object : BackendService {
        override suspend fun getRegistrationChallenge() = byteArrayOf(0xAA.toByte())
        override suspend fun registerDevice(certificateChain: ByteArray) = onRegister(certificateChain)
        override suspend fun getPublicKey() = publicKey
        override suspend fun submitDeviceBinding(payload: SecurePayload) = onSubmit()
    }

    private fun backendServiceChallengeFails() = object : BackendService {
        override suspend fun getRegistrationChallenge(): ByteArray = throw RuntimeException("Network unavailable")
        override suspend fun registerDevice(certificateChain: ByteArray) {}
        override suspend fun getPublicKey() = byteArrayOf(1, 2, 3)
        override suspend fun submitDeviceBinding(payload: SecurePayload) {}
    }

    private fun backendServiceRegisterFails() = object : BackendService {
        override suspend fun getRegistrationChallenge() = byteArrayOf(0xAA.toByte())
        override suspend fun registerDevice(certificateChain: ByteArray): Unit =
            throw RuntimeException("Registration rejected")
        override suspend fun getPublicKey() = byteArrayOf(1, 2, 3)
        override suspend fun submitDeviceBinding(payload: SecurePayload) {}
    }

    private fun backendServiceKeyFails() = object : BackendService {
        override suspend fun getRegistrationChallenge() = byteArrayOf()
        override suspend fun registerDevice(certificateChain: ByteArray) {}
        override suspend fun getPublicKey(): ByteArray = throw RuntimeException("Network unavailable")
        override suspend fun submitDeviceBinding(payload: SecurePayload) {}
    }

    private fun backendServiceSubmitFails() = object : BackendService {
        override suspend fun getRegistrationChallenge() = byteArrayOf()
        override suspend fun registerDevice(certificateChain: ByteArray) {}
        override suspend fun getPublicKey() = byteArrayOf(1, 2, 3)
        override suspend fun submitDeviceBinding(payload: SecurePayload): Unit =
            throw RuntimeException("Submit failed")
    }

    private fun securityRepository(
        cert: ByteArray? = byteArrayOf(0x30, 0x82.toByte()),
        payload: SecurePayload = SecurePayload(byteArrayOf(4, 5), byteArrayOf(6, 7)),
    ) = object : SecurityRepository {
        override suspend fun getRegistrationCertificate(challenge: ByteArray) = cert
        override suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray) =
            SecurityResult.Success(payload)
    }

    private fun securityRepositoryCertNull() = object : SecurityRepository {
        override suspend fun getRegistrationCertificate(challenge: ByteArray): ByteArray? = null
        override suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray) =
            SecurityResult.Success(SecurePayload(byteArrayOf(4, 5), byteArrayOf(6, 7)))
    }

    private fun securityRepositoryEncryptFails() = object : SecurityRepository {
        override suspend fun getRegistrationCertificate(challenge: ByteArray) = byteArrayOf(0x30)
        override suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray) =
            SecurityResult.Failure(SecurityError.EncryptionFailed(RuntimeException("Encryption error")))
    }

    private fun cardReader(rawData: ByteArray = byteArrayOf(10, 20, 30)) =
        object : CardReadRepository {
            override suspend fun readSecureData(amount: Double) = CardReadResult.Success(rawData)
        }

    private fun cardReaderFails() = object : CardReadRepository {
        override suspend fun readSecureData(amount: Double) =
            CardReadResult.Failure.SecurityViolation("rooted device")
    }

    private fun engine(
        backendService: BackendService = backendService(),
        cardReadRepository: CardReadRepository = cardReader(),
        securityRepository: SecurityRepository = securityRepository(),
    ) = PollyPaymentEngine(
        cardReadRepository = cardReadRepository,
        backendService = backendService,
        securityRepository = securityRepository,
        logger = PollyLogger.Silent,
    )

    private suspend fun PollyPaymentEngine.initAndStart(amount: Double = 19.99) {
        initialize()
        startTransaction(amount)
    }

    // endregion

    // region initial state

    @Test
    fun initialStateIsIdle() {
        assertEquals(PaymentState.Idle, engine().paymentState.value)
    }

    // endregion

    // region initialization

    @Test
    fun initEmitsInitializingThenIdle() = runTest {
        val engine = engine()
        val states = mutableListOf<PaymentState>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            engine.paymentState.collect { states.add(it) }
        }

        engine.initialize()
        collectJob.cancel()

        assertEquals(
            listOf(PaymentState.Idle, PaymentState.Initializing, PaymentState.Idle),
            states,
        )
    }

    @Test
    fun initChallengeFails() = runTest {
        val engine = engine(backendService = backendServiceChallengeFails())
        engine.initialize()
        assertIs<PaymentState.Failed.BackendError>(engine.paymentState.value)
    }

    @Test
    fun initRegisterFails() = runTest {
        val engine = engine(backendService = backendServiceRegisterFails())
        engine.initialize()
        assertIs<PaymentState.Failed.BackendError>(engine.paymentState.value)
    }

    @Test
    fun initCertNullLeavesEngineUninitialized() = runTest {
        val engine = engine(securityRepository = securityRepositoryCertNull())
        engine.initialize()
        // cert null → silent failure, state stays at Initializing, engine not ready
        assertEquals(PaymentState.Initializing, engine.paymentState.value)
        engine.startTransaction(19.99)
        assertEquals(PaymentState.Failed.NotInitialized, engine.paymentState.value)
    }

    @Test
    fun initPassesChallengeToSecurityRepository() = runTest {
        val expectedChallenge = byteArrayOf(0xAA.toByte())
        var receivedChallenge: ByteArray? = null
        val captureSecurity = object : SecurityRepository {
            override suspend fun getRegistrationCertificate(challenge: ByteArray): ByteArray {
                receivedChallenge = challenge
                return byteArrayOf(0x30)
            }
            override suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray) =
                SecurityResult.Success(SecurePayload(byteArrayOf(), byteArrayOf()))
        }
        engine(securityRepository = captureSecurity).initialize()
        assertContentEquals(expectedChallenge, receivedChallenge)
    }

    @Test
    fun initSendsCertToBackend() = runTest {
        val fakeCert = byteArrayOf(0x30, 0x82.toByte())
        var receivedCert: ByteArray? = null
        val engine = engine(
            backendService = backendService(onRegister = { receivedCert = it }),
            securityRepository = securityRepository(cert = fakeCert),
        )
        engine.initialize()
        assertContentEquals(fakeCert, receivedCert)
    }

    // endregion

    // region not-initialized guard

    @Test
    fun startTransactionWithoutInitEmitsNotInitialized() = runTest {
        val engine = engine()
        engine.startTransaction(19.99)
        assertEquals(PaymentState.Failed.NotInitialized, engine.paymentState.value)
    }

    // endregion

    // region transaction state transitions

    @Test
    fun happyPathEmitsIdleWaitingCommunicatingSuccess() = runTest {
        val engine = engine()
        engine.initialize()

        val states = mutableListOf<PaymentState>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            engine.paymentState.collect { states.add(it) }
        }
        engine.startTransaction(19.99)
        collectJob.cancel()

        assertEquals(
            listOf(
                PaymentState.Idle,
                PaymentState.WaitingForCard,
                PaymentState.Communicating,
                PaymentState.Success,
            ),
            states,
        )
    }

    @Test
    fun backendKeyFailureEmitsBackendError() = runTest {
        val engine = engine(backendService = backendServiceKeyFails())
        engine.initialize()
        engine.startTransaction(19.99)
        assertIs<PaymentState.Failed.BackendError>(engine.paymentState.value)
    }

    @Test
    fun cardReadFailureEmitsLocalSecurityFailed() = runTest {
        val engine = engine(cardReadRepository = cardReaderFails())
        engine.initialize()
        engine.startTransaction(19.99)
        assertEquals(PaymentState.Failed.LocalSecurityFailed, engine.paymentState.value)
    }

    @Test
    fun encryptionFailureEmitsLocalSecurityFailed() = runTest {
        val engine = engine(securityRepository = securityRepositoryEncryptFails())
        engine.initialize()
        engine.startTransaction(19.99)
        assertEquals(PaymentState.Failed.LocalSecurityFailed, engine.paymentState.value)
    }

    @Test
    fun backendSubmitFailureMessageContainsReason() = runTest {
        val engine = engine(backendService = backendServiceSubmitFails())
        engine.initialize()
        engine.startTransaction(19.99)
        val state = engine.paymentState.value as PaymentState.Failed.BackendError
        assertEquals("submit error: Submit failed", state.message)
    }

    // endregion

    // region sensitive data cleanup

    @Test
    fun rawDataIsZeroedAfterSuccessfulTransaction() = runTest {
        val rawData = byteArrayOf(1, 2, 3, 4)
        val engine = engine(cardReadRepository = cardReader(rawData))
        engine.initAndStart()
        assertContentEquals(ByteArray(4), rawData)
    }

    @Test
    fun backendKeyIsZeroedAfterSuccessfulTransaction() = runTest {
        val publicKey = byteArrayOf(1, 2, 3, 4)
        val engine = engine(backendService = backendService(publicKey = publicKey))
        engine.initAndStart()
        assertContentEquals(ByteArray(4), publicKey)
    }

    @Test
    fun backendKeyIsZeroedEvenOnCardReadFailure() = runTest {
        val publicKey = byteArrayOf(1, 2, 3, 4)
        val engine = engine(
            backendService = backendService(publicKey = publicKey),
            cardReadRepository = cardReaderFails(),
        )
        engine.initAndStart()
        assertContentEquals(ByteArray(4), publicKey)
    }

    @Test
    fun backendKeyIsZeroedEvenOnEncryptionFailure() = runTest {
        val publicKey = byteArrayOf(1, 2, 3, 4)
        val engine = engine(
            backendService = backendService(publicKey = publicKey),
            securityRepository = securityRepositoryEncryptFails(),
        )
        engine.initAndStart()
        assertContentEquals(ByteArray(4), publicKey)
    }

    // endregion

    // region submit behaviour

    @Test
    fun submitIsCalledOnSuccessfulTransaction() = runTest {
        var submitCalled = false
        val engine = engine(backendService = backendService(onSubmit = { submitCalled = true }))
        engine.initAndStart()
        assertEquals(true, submitCalled)
    }

    @Test
    fun submitIsNotCalledOnCardReadFailure() = runTest {
        var submitCalled = false
        val engine = engine(
            backendService = backendService(onSubmit = { submitCalled = true }),
            cardReadRepository = cardReaderFails(),
        )
        engine.initAndStart()
        assertFalse(submitCalled)
    }

    // endregion
}
