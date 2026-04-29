package org.pollyanna.nfckmp.nfc_provider

/**
 * Platform-specific factory responsible for initializing the scanning infrastructure.
 *
 * Responsibilities:
 * 1. Encapsulates platform dependencies (e.g., Android [Context]).
 * 2. Decouples the business logic layer from hardware-specific implementations.
 * 3. Acts as a bridge between the [PollyPaymentEngine] and the native NFC hardware APIs.
 *
 * Design Note:
 * This is an 'expect' class to allow heterogeneous constructors across platforms
 * while maintaining a unified interface for the common module.
 */
expect class ScannerFactory {
    fun create(): PaymentCardScanner
}