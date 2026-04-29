package org.pollyanna.nfckmp.nfc_provider

actual class ScannerFactory {
    actual fun create(): PaymentCardScanner = IosNfcScanner()
}