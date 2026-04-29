package org.pollyanna.nfckmp

sealed interface PaymentResult{
    data object Idle: PaymentResult
    data object CheckingSecurity: PaymentResult
    data object WaitingForCard: PaymentResult
    data object Communicating: PaymentResult
    data object Success: PaymentResult
    data object Failed: PaymentResult
}
