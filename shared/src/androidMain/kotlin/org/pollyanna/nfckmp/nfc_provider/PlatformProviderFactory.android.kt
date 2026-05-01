package org.pollyanna.nfckmp.nfc_provider

import android.content.Context
import org.pollyanna.nfckmp.AndroidNfcScanDataSource
import org.pollyanna.nfckmp.security.AndroidAttestationCheckProvider
import org.pollyanna.nfckmp.security.AttestationCheckProvider

actual class PlatformProviderFactory(private val context: Context) {
    actual fun createScanner(): PaymentCardScanDataSource = AndroidNfcScanDataSource(context)
    actual fun createAttestationChecker(): AttestationCheckProvider = AndroidAttestationCheckProvider(context)
}