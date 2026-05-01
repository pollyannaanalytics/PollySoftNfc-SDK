package org.pollyanna.nfckmp.nfc_provider

import android.content.Context
import org.pollyanna.nfckmp.AndroidNfcScanDataSource
import org.pollyanna.nfckmp.security.AndroidAttestationCheckProvider
import org.pollyanna.nfckmp.security.AndroidCryptoDataSource
import org.pollyanna.nfckmp.security.AndroidDeviceSecurityRepository
import org.pollyanna.nfckmp.security.AndroidPrivateKeyDataSource
import org.pollyanna.nfckmp.security.AndroidTransactionIdentifyRepository
import org.pollyanna.nfckmp.security.DeviceSecurityRepository

actual class PlatformProviderFactory(private val context: Context) {

    actual fun createCardReadRepository(): CardReadRepository {
        val scanner = AndroidNfcScanDataSource(context)
        val attestationChecker = AndroidAttestationCheckProvider(context)
        return AndroidCardReadRepository(scanner, attestationChecker)
    }

    actual fun createDeviceSecurityRepository(): DeviceSecurityRepository {
        val privateKeyDataSource = AndroidPrivateKeyDataSource(KEYSTORE_ALIAS)
        val transactionRepo = AndroidTransactionIdentifyRepository(privateKeyDataSource)
        val cryptoDataSource = AndroidCryptoDataSource(context)
        return AndroidDeviceSecurityRepository(cryptoDataSource, transactionRepo)
    }

    private companion object {
        const val KEYSTORE_ALIAS = "polly_payment_key"
    }
}
