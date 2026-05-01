package org.pollyanna.nfckmp.model

sealed interface PaymentState{
    data object Idle: PaymentState
    data object Initializing: PaymentState
    data object WaitingForCard: PaymentState
    data object Communicating: PaymentState
    data object Success: PaymentState
    sealed class Failed: PaymentState {
        data object NotInitialized: Failed()
        data object LocalSecurityFailed: Failed()
        data class BackendError(val message: String): Failed()
    }
}