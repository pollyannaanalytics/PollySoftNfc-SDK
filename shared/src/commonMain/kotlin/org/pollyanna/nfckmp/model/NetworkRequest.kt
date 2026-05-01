package org.pollyanna.nfckmp.model

data class TransactionRequest(
    val amount: Double,
    val encryptedData: String,
    val signature: String,
    val terminalId: String
)
