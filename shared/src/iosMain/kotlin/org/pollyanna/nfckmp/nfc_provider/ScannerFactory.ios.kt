package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.security.AttestationCheckProvider
import org.pollyanna.nfckmp.security.CryptoDataSource
import org.pollyanna.nfckmp.security.DefaultDeviceSecurityRepository
import org.pollyanna.nfckmp.security.DeviceSecurityRepository
import org.pollyanna.nfckmp.security.TransactionIdentifyRepository

actual class PlatformProviderFactory(
    private val scanner: PaymentCardScanDataSource,
    private val attestation: AttestationCheckProvider,
    private val crypto: CryptoDataSource,
    private val transactionIdentify: TransactionIdentifyRepository,
) {
    actual fun createCardReadRepository(): CardReadRepository =
        DefaultCardReadRepository(scanner, attestation)

    actual fun createDeviceSecurityRepository(): DeviceSecurityRepository =
        DefaultDeviceSecurityRepository(crypto, transactionIdentify)
}
