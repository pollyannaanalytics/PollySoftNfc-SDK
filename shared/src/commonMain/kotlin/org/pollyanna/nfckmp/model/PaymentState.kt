package org.pollyanna.nfckmp.model

sealed interface PaymentState{
    data object Idle: PaymentState
    data object CheckingSecurity: PaymentState
    data object WaitingForCard: PaymentState
    data object Communicating: PaymentState
    data object Success: PaymentState
    data object Failed: PaymentState
}