package org.pollyanna.nfckmp

import android.content.Context
import org.pollyanna.nfckmp.model.ScannerType
import org.pollyanna.nfckmp.nfc_provider.PaymentCardScanner

class AndroidNfcScanner (
    private val context: Context, override val scannerType: ScannerType = ScannerType.ANDROID
) : PaymentCardScanner {
    override suspend fun scan(): ByteArray {

        // mock android nfc ex. isodep
        return byteArrayOf(0x00)
    }
}