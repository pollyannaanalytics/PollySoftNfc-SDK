package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.ScannerType

class IosNfcScanDataSource(
    override val scannerType: ScannerType = ScannerType.IOS,
) : PaymentCardScanDataSource {

    private var internalBuffer: ByteArray? = null

    override suspend fun scan(amount: Double): ByteArray {
        // mock iOS NFC CoreNFC
        internalBuffer = byteArrayOf(0x00)
        return internalBuffer!!.copyOf()
    }

    override fun clearSensitiveData() {
        internalBuffer?.fill(0)
        internalBuffer = null
    }
}
