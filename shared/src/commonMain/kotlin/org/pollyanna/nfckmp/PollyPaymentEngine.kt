package org.pollyanna.nfckmp

import org.pollyanna.nfckmp.model.PaymentState
import org.pollyanna.nfckmp.nfc_provider.PaymentCardScanner

class PollyPaymentEngine(
    private val platformScanner: PaymentCardScanner,
    private val attestationChecker: AttestationChecker
) {
    suspend fun startTransaction(amount: Double): PaymentState {

    }
}