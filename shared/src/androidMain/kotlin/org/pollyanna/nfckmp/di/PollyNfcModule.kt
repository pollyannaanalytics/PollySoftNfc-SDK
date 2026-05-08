package org.pollyanna.nfckmp.di

import android.content.Context
import org.koin.dsl.module
import org.pollyanna.nfckmp.AndroidNfcScanDataSource
import org.pollyanna.nfckmp.nfc_provider.AndroidCardReadRepository
import org.pollyanna.nfckmp.nfc_provider.CardReadRepository
import org.pollyanna.nfckmp.nfc_provider.PaymentCardScanDataSource
import org.pollyanna.nfckmp.security.AndroidAttestationCheckProvider
import org.pollyanna.nfckmp.security.AndroidCryptoDataSource
import org.pollyanna.nfckmp.security.AndroidDeviceSecurityRepository
import org.pollyanna.nfckmp.security.AndroidPrivateKeyDataSource
import org.pollyanna.nfckmp.security.AndroidTransactionIdentifyRepository
import org.pollyanna.nfckmp.security.AttestationCheckProvider
import org.pollyanna.nfckmp.security.CryptoDataSource
import org.pollyanna.nfckmp.security.DeviceSecurityRepository
import org.pollyanna.nfckmp.security.PrivateKeyDataSource
import org.pollyanna.nfckmp.security.TransactionIdentifyRepository

private const val KEYSTORE_ALIAS = "polly_payment_key"

internal fun pollyNfcModule(context: Context) = module {
    single<PrivateKeyDataSource> { AndroidPrivateKeyDataSource(KEYSTORE_ALIAS) }
    single<TransactionIdentifyRepository> { AndroidTransactionIdentifyRepository(get()) }
    single<CryptoDataSource> { AndroidCryptoDataSource() }
    single<DeviceSecurityRepository> { AndroidDeviceSecurityRepository(get(), get()) }
    single<AttestationCheckProvider> { AndroidAttestationCheckProvider(context) }
    single<PaymentCardScanDataSource> { AndroidNfcScanDataSource(context) }
    single<CardReadRepository> { AndroidCardReadRepository(get(), get()) }
}
