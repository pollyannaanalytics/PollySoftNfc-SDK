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
import kotlin.test.assertIs

class PollyPaymentEngineTest {

    // region fakes

    private fun backendService(
        publicKey: ByteArray = byteArrayOf(1, 2, 3),
        onSubmit: () -> Unit = {},
    ) = object : BackendService {
        override suspend fun getRegistrationChallenge() = byteArrayOf()
        override suspend fun getPublicKey() = publicKey
        override suspend fun submitDeviceBinding(payload: SecurePayload) = onSubmit()
    }

    private fun backendServiceKeyFails() = object : BackendService {
        override suspend fun getRegistrationChallenge() = byteArrayOf()
        override suspend fun getPublicKey(): ByteArray = throw RuntimeException("Network unavailable")
        override suspend fun submitDeviceBinding(payload: SecurePayload) {}
    }

    private fun backendServiceSubmitFails() = object : BackendService {
        override suspend fun getRegistrationChallenge() = byteArrayOf()
        override suspend fun getPublicKey() = byteArrayOf(1, 2, 3)
        override suspend fun submitDeviceBinding(payload: SecurePayload): Unit =
            throw RuntimeException("Submit failed")
    }

    private fun cardReader(rawData: ByteArray = byteArrayOf(10, 20, 30)) =
        object : CardReadRepository {
            override suspend fun readSecureData(amount: Double) = CardReadResult.Success(rawData)
        }

    private fun cardReaderFails() = object : CardReadRepository {
        override suspend fun readSecureData(amount: Double) =
            CardReadResult.Failure.SecurityViolation("rooted device")
    }

    private fun securityRepository(
        payload: SecurePayload = SecurePayload(byteArrayOf(4, 5), byteArrayOf(6, 7)),
    ) = object : SecurityRepository {
        override suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray) =
            SecurityResult.Success(payload)
    }

    private fun securityRepositoryFails() = object : SecurityRepository {
        override suspend fun encrypt(rawData: ByteArray, publicKey: ByteArray) =
            SecurityResult.Failure(SecurityError.EncryptionFailed(RuntimeException("Encryption error")))
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

    // endregion

    // region initial state

    @Test
    fun initialStateIsIdle() {
        assertEquals(PaymentState.Idle, engine().paymentState.value)
    }

    // endregion

    // region state transitions

    @Test
    fun happyPathEmitsIdleWaitingCommunicatingSuccess() = runTest {
        val engine = engine()
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
    fun backendKeyFailureEmitsIdleThenBackendError() = runTest {
        val engine = engine(backendService = backendServiceKeyFails())
        val states = mutableListOf<PaymentState>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            engine.paymentState.collect { states.add(it) }
        }

        engine.startTransaction(19.99)
        collectJob.cancel()

        assertEquals(
            listOf(PaymentState.Idle, PaymentState.Failed.BackendError("network error: Network unavailable")),
            states,
        )
    }

    @Test
    fun cardReadFailureEmitsLocalSecurityFailed() = runTest {
        val engine = engine(cardReadRepository = cardReaderFails())
        val states = mutableListOf<PaymentState>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            engine.paymentState.collect { states.add(it) }
        }

        engine.startTransaction(19.99)
        collectJob.cancel()

        assertEquals(PaymentState.Failed.LocalSecurityFailed, states.last())
    }

    @Test
    fun encryptionFailureEmitsLocalSecurityFailed() = runTest {
        val engine = engine(securityRepository = securityRepositoryFails())
        engine.startTransaction(19.99)
        assertEquals(PaymentState.Failed.LocalSecurityFailed, engine.paymentState.value)
    }

    @Test
    fun backendSubmitFailureEmitsBackendError() = runTest {
        val engine = engine(backendService = backendServiceSubmitFails())
        engine.startTransaction(19.99)
        assertIs<PaymentState.Failed.BackendError>(engine.paymentState.value)
    }

    @Test
    fun backendSubmitFailureMessageContainsReason() = runTest {
        val engine = engine(backendService = backendServiceSubmitFails())
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
        engine.startTransaction(19.99)
        assertContentEquals(ByteArray(4), rawData)
    }

    @Test
    fun backendKeyIsZeroedAfterSuccessfulTransaction() = runTest {
        val publicKey = byteArrayOf(1, 2, 3, 4)
        val engine = engine(backendService = backendService(publicKey = publicKey))
        engine.startTransaction(19.99)
        assertContentEquals(ByteArray(4), publicKey)
    }

    @Test
    fun backendKeyIsZeroedEvenOnCardReadFailure() = runTest {
        val publicKey = byteArrayOf(1, 2, 3, 4)
        val engine = engine(
            backendService = backendService(publicKey = publicKey),
            cardReadRepository = cardReaderFails(),
        )
        engine.startTransaction(19.99)
        assertContentEquals(ByteArray(4), publicKey)
    }

    @Test
    fun backendKeyIsZeroedEvenOnEncryptionFailure() = runTest {
        val publicKey = byteArrayOf(1, 2, 3, 4)
        val engine = engine(
            backendService = backendService(publicKey = publicKey),
            securityRepository = securityRepositoryFails(),
        )
        engine.startTransaction(19.99)
        assertContentEquals(ByteArray(4), publicKey)
    }

    // endregion

    // region submit is called with correct payload

    @Test
    fun submitIsCalledOnSuccessfulTransaction() = runTest {
        var submitCalled = false
        val engine = engine(backendService = backendService(onSubmit = { submitCalled = true }))
        engine.startTransaction(19.99)
        assertEquals(true, submitCalled)
    }

    @Test
    fun submitIsNotCalledOnCardReadFailure() = runTest {
        var submitCalled = false
        val engine = engine(
            backendService = backendService(onSubmit = { submitCalled = true }),
            cardReadRepository = cardReaderFails(),
        )
        engine.startTransaction(19.99)
        assertEquals(false, submitCalled)
    }

    // endregion
}
