package org.pollyanna.nfckmp.model

/**
 * Represents every observable state of a [org.pollyanna.nfckmp.PollyPaymentEngine] session.
 *
 * Collect [org.pollyanna.nfckmp.PollyPaymentEngine.paymentState] and switch on this sealed
 * interface to drive UI transitions.
 */
sealed interface PaymentState {
    /** Engine is idle and ready to start a transaction. */
    data object Idle : PaymentState

    /** Device registration with the backend is in progress. */
    data object Initializing : PaymentState

    /** Engine is actively listening for an NFC card tap. */
    data object WaitingForCard : PaymentState

    /** Encrypted payload is being transmitted to the backend. */
    data object Communicating : PaymentState

    /** Transaction completed and was accepted by the backend. */
    data object Success : PaymentState

    /** A terminal error state. Check the specific subtype for recovery guidance. */
    sealed class Failed : PaymentState {
        /** [org.pollyanna.nfckmp.PollyPaymentEngine.startTransaction] was called before
         *  [org.pollyanna.nfckmp.PollyPaymentEngine.initialize] succeeded. */
        data object NotInitialized : Failed()

        /** Root detection, debugger detection, or hardware key attestation failed.
         *  The device does not meet security requirements to process payments. */
        data object LocalSecurityFailed : Failed()

        /** A backend call returned an error. [message] contains the raw reason for logging. */
        data class BackendError(val message: String) : Failed()

        /** A network or card-read operation exceeded its configured timeout.
         *  Tune via [org.pollyanna.nfckmp.EngineConfig]. */
        data object TimedOut : Failed()
    }
}
