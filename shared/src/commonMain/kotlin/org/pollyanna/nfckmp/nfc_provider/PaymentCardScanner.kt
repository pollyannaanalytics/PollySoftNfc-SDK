package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.ScannerType

interface PaymentCardScanner {
    suspend fun scan(): ByteArray
    val scannerType: ScannerType
}