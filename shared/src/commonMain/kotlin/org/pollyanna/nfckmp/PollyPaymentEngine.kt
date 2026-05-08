package org.pollyanna.nfckmp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.pollyanna.nfckmp.model.CardReadResult
import org.pollyanna.nfckmp.model.PaymentState
import org.pollyanna.nfckmp.model.SecurityResult
import org.pollyanna.nfckmp.network.BackendService
import org.pollyanna.nfckmp.nfc_provider.CardReadRepository
import org.pollyanna.nfckmp.security.DeviceSecurityRepository

/**
 * Orchestrates NFC payment transactions on behalf of a merchant device.
 *
 * Construct via [PollyNfc.createEngine] — do not instantiate directly.
 *
 * Typical lifecycle:
 * 1. Call [initialize] once on app start to register the device with your backend.
 * 2. For each payment, call [startTransaction] with the charge amount.
 * 3. Observe [paymentState] to drive UI updates.
 *
 * Both methods are suspend functions and are safe to call from any coroutine context.
 * Timeouts and cancellation are governed by the [EngineConfig] passed at construction time.
 */
class PollyPaymentEngine(
    private val cardReadRepository: CardReadRepository,
    private val backendService: BackendService,
    private val deviceSecurityRepository: DeviceSecurityRepository,
    private val config: EngineConfig = EngineConfig(),
    private val logger: PollyLogger = PollyLogger.Default,
) {
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)

    /** Real-time state of the payment engine. Collect this to update your UI. */
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private var isInitialized = false

    /**
     * Registers this device with the backend using hardware-backed key attestation.
     *
     * Must complete successfully before [startTransaction] can be called.
     * Emits [PaymentState.Initializing] while in progress, then:
     * - [PaymentState.Idle] on success
     * - [PaymentState.Failed.LocalSecurityFailed] if key attestation is unavailable
     * - [PaymentState.Failed.BackendError] if the backend rejects the registration
     * - [PaymentState.Failed.TimedOut] if any step exceeds [EngineConfig.networkTimeoutMs]
     */
    suspend fun initialize() {
        initFlow().collect { _paymentState.value = it }
    }

    /**
     * Runs a full NFC payment transaction for the given [amount].
     *
     * Requires a prior successful [initialize] call; emits [PaymentState.Failed.NotInitialized]
     * otherwise. State sequence on the happy path:
     * Idle → WaitingForCard → Communicating → Success
     *
     * @param amount Charge amount in the merchant's local currency.
     */
    suspend fun startTransaction(amount: Double) {
        transactionFlow(amount).collect { _paymentState.value = it }
    }

    private fun initFlow() = flow {
        logger.log(TAG, "Initializing device")
        emit(PaymentState.Initializing)

        try {
            logger.log(TAG, "Fetching registration challenge")
            val challenge = withTimeout(config.networkTimeoutMs) {
                backendService.getRegistrationChallenge()
            }

            logger.log(TAG, "Generating attestation certificate")
            val certChain = deviceSecurityRepository.getRegistrationCertificate(challenge)

            if (certChain != null) {
                logger.log(TAG, "Registering device with backend (${certChain.size} bytes)")
                withTimeout(config.networkTimeoutMs) {
                    backendService.registerDevice(certChain)
                }
                isInitialized = true
                logger.log(TAG, "Device initialized successfully")
                emit(PaymentState.Idle)
            } else {
                logger.log(TAG, "Device initialization failed: key attestation returned null")
                emit(PaymentState.Failed.LocalSecurityFailed)
            }
        } catch (e: TimeoutCancellationException) {
            logger.log(TAG, "Initialization timed out after ${config.networkTimeoutMs}ms")
            emit(PaymentState.Failed.TimedOut)
        } catch (e: CancellationException) {
            throw e
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
            withTimeout(config.networkTimeoutMs) { backendService.getPublicKey() }
        } catch (e: TimeoutCancellationException) {
            logger.log(TAG, "Key fetch timed out")
            emit(PaymentState.Failed.TimedOut)
            return@flow
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log(TAG, "Failed to fetch backend key: ${e.message}")
            emit(PaymentState.Failed.BackendError("network error: ${e.message}"))
            return@flow
        }

        try {
            emit(PaymentState.WaitingForCard)
            logger.log(TAG, "Waiting for card tap (timeout=${config.cardReadTimeoutMs}ms)")

            val cardResult = withTimeout(config.cardReadTimeoutMs) {
                cardReadRepository.readSecureData(amount)
            }

            when (cardResult) {
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
                                withTimeout(config.networkTimeoutMs) {
                                    backendService.submitDeviceBinding(securityResult.payload, integrityToken)
                                }
                                logger.log(TAG, "Transaction completed successfully")
                                emit(PaymentState.Success)
                            } catch (e: TimeoutCancellationException) {
                                logger.log(TAG, "Backend submission timed out")
                                emit(PaymentState.Failed.TimedOut)
                            } catch (e: CancellationException) {
                                throw e
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
        } catch (e: TimeoutCancellationException) {
            logger.log(TAG, "Card read timed out after ${config.cardReadTimeoutMs}ms")
            emit(PaymentState.Failed.TimedOut)
        } catch (e: CancellationException) {
            throw e
        } finally {
            backendKey.fill(0)
        }
    }

    private companion object {
        const val TAG = "PollyPaymentEngine"
    }
}
