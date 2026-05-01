package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.ScannerType

interface PaymentCardScanDataSource {
    val scannerType: ScannerType
    suspend fun scan(amount: Double): ByteArray
    fun clearSensitiveData()
}