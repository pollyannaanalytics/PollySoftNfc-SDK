package org.pollyanna.nfckmp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import org.pollyanna.nfckmp.model.CardReadResult
import org.pollyanna.nfckmp.model.PaymentState
import org.pollyanna.nfckmp.model.SecurityResult
import org.pollyanna.nfckmp.network.BackendService
import org.pollyanna.nfckmp.nfc_provider.CardReadRepository
import org.pollyanna.nfckmp.security.DeviceSecurityRepository

class PollyPaymentEngine(
    private val cardReadRepository: CardReadRepository,
    private val backendService: BackendService,
    private val deviceSecurityRepository: DeviceSecurityRepository,
    private val logger: PollyLogger = PollyLogger.Default,
) {
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private var isInitialized = false

    suspend fun initialize() {
        initFlow().collect { _paymentState.value = it }
    }

    suspend fun startTransaction(amount: Double) {
        transactionFlow(amount).collect { _paymentState.value = it }
    }

    private fun initFlow() = flow {
        logger.log(TAG, "Initializing device")
        emit(PaymentState.Initializing)

        try {
            logger.log(TAG, "Fetching registration challenge")
            val challenge = backendService.getRegistrationChallenge()

            logger.log(TAG, "Generating attestation certificate")
            val certChain = deviceSecurityRepository.getRegistrationCertificate(challenge)

            logger.log(TAG, "Registering device with backend (${certChain?.size} certs)")
            certChain?.let {
                backendService.registerDevice(certChain)
                isInitialized = true
                logger.log(TAG, "Device initialized successfully")
                emit(PaymentState.Idle)
            }?: run {
                logger.log(TAG, "Device initialization failed: certificateChain = null")
            }
        } catch (e: Exception) {
            logger.log(TAG, "Initialization failed: ${e.message}")
            emit(PaymentState.Failed.BackendError("init error: ${e.message}"))
        }
    }

    private fun transactionFlow(amount: Double) = flow {
        if (!isInitialized) {
            logger.log(TAG, "Transaction attempted before initialization")
            emit(PaymentState.Failed.NotInitialized)
            return@flow
        }

        logger.log(TAG, "Starting transaction, amount=$amount")
        emit(PaymentState.Idle)

        val backendKey = try {
            logger.log(TAG, "Fetching backend public key")
            backendService.getPublicKey()
        } catch (e: Exception) {
            logger.log(TAG, "Failed to fetch backend key: ${e.message}")
            emit(PaymentState.Failed.BackendError("network error: ${e.message}"))
            return@flow
        }

        try {
            emit(PaymentState.WaitingForCard)
            logger.log(TAG, "Waiting for card tap")

            when (val cardResult = cardReadRepository.readSecureData(amount)) {
                is CardReadResult.Success -> {
                    logger.log(TAG, "Card read succeeded, encrypting data")
                    emit(PaymentState.Communicating)

                    val integrityToken = cardResult.integrityToken
                    val securityResult = deviceSecurityRepository.encrypt(cardResult.rawData, backendKey)
                    cardResult.clear()

                    when (securityResult) {
                        is SecurityResult.Success -> {
                            logger.log(TAG, "Encryption succeeded, submitting to backend")
                            try {
                                backendService.submitDeviceBinding(securityResult.payload, integrityToken)
                                logger.log(TAG, "Transaction completed successfully")
                                emit(PaymentState.Success)
                            } catch (e: Exception) {
                                logger.log(TAG, "Backend submission failed: ${e.message}")
                                emit(PaymentState.Failed.BackendError("submit error: ${e.message}"))
                            } finally {
                                securityResult.payload.clear()
                            }
                        }
                        is SecurityResult.Failure -> {
                            logger.log(TAG, "Encryption failed: ${securityResult.error}")
                            emit(PaymentState.Failed.LocalSecurityFailed)
                        }
                    }
                }
                is CardReadResult.Failure -> {
                    val reason = (cardResult as? CardReadResult.Failure.SecurityViolation)?.reason ?: "unknown"
                    logger.log(TAG, "Card read failed — security violation: $reason")
                    emit(PaymentState.Failed.LocalSecurityFailed)
                }
            }
        } finally {
            backendKey.fill(0)
        }
    }

    private companion object {
        const val TAG = "PollyPaymentEngine"
    }
}
