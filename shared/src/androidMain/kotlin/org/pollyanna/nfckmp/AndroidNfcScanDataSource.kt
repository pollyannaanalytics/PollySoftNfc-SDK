package org.pollyanna.nfckmp

import android.content.Context
import org.pollyanna.nfckmp.model.ScannerType
import org.pollyanna.nfckmp.nfc_provider.PaymentCardScanDataSource

class AndroidNfcScanDataSource (
    private val context: Context, override val scannerType: ScannerType = ScannerType.ANDROID
) : PaymentCardScanDataSource {
    override suspend fun scan(amount: Double): ByteArray {

        // mock android nfc ex. isodep
        return byteArrayOf(0x00)
    }
}