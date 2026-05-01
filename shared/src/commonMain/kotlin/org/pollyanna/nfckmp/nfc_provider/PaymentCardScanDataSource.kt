package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.ScannerType

interface PaymentCardScanDataSource {
    suspend fun scan(amount: Double): ByteArray
    val scannerType: ScannerType

    fun clearSensitiveData(data: ByteArray) {
        data.fill(0)
    }
}