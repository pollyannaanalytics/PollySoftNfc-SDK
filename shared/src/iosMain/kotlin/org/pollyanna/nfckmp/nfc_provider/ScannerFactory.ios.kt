package org.pollyanna.nfckmp.nfc_provider

actual class PlatformProviderFactory {
    actual fun createScanner(): PaymentCardScanDataSource = IosNfcScanDataSource()
}