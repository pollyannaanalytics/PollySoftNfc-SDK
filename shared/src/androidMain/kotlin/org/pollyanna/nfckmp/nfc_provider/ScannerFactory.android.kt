package org.pollyanna.nfckmp.nfc_provider

import android.content.Context
import org.pollyanna.nfckmp.AndroidNfcScanner

actual class ScannerFactory(private val context: Context) {
    actual fun create(): PaymentCardScanner = AndroidNfcScanner(context)
}