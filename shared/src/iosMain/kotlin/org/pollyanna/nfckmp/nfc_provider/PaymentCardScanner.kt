package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.ScannerType

class IosNfcScanDataSource (
    override val scannerType: ScannerType = ScannerType.IOS
) : PaymentCardScanDataSource {
    override suspend fun scan(amount: Double): ByteArray {

        // mock ios nfc api
        return byteArrayOf(0x00)
    }
}