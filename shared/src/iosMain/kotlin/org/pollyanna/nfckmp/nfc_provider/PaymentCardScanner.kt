package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.ScannerType

class IosNfcScanner (
    override val scannerType: ScannerType = ScannerType.IOS
) : PaymentCardScanner {
    override suspend fun scan(): ByteArray {

        // mock ios nfc api
        return byteArrayOf(0x00)
    }
}