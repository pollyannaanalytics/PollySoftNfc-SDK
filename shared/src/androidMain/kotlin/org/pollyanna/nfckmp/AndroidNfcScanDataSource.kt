package org.pollyanna.nfckmp

import android.content.Context
import org.pollyanna.nfckmp.model.ScannerType
import org.pollyanna.nfckmp.nfc_provider.PaymentCardScanDataSource

class AndroidNfcScanDataSource(
    private val context: Context,
    override val scannerType: ScannerType = ScannerType.ANDROID,
) : PaymentCardScanDataSource {

    private var internalBuffer: ByteArray? = null

    override suspend fun scan(amount: Double): ByteArray {
        // mock android nfc e.g. IsoDep APDU exchange
        internalBuffer = byteArrayOf(0x00)
        return internalBuffer!!.copyOf()
    }

    override fun clearSensitiveData() {
        internalBuffer?.fill(0)
        internalBuffer = null
    }
}
