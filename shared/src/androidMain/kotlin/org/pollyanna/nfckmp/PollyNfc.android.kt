package org.pollyanna.nfckmp

import android.content.Context
import org.koin.dsl.koinApplication
import org.pollyanna.nfckmp.di.pollyNfcModule
import org.pollyanna.nfckmp.network.BackendService
import org.pollyanna.nfckmp.nfc_provider.CardReadRepository
import org.pollyanna.nfckmp.security.DeviceSecurityRepository

actual class PollyNfc(private val context: Context) {

    actual fun createEngine(
        backendService: BackendService,
        config: EngineConfig,
        logger: PollyLogger,
    ): PollyPaymentEngine {
        val koin = koinApplication {
            modules(pollyNfcModule(context))
        }.koin

        return PollyPaymentEngine(
            cardReadRepository = koin.get<CardReadRepository>(),
            backendService = backendService,
            deviceSecurityRepository = koin.get<DeviceSecurityRepository>(),
            config = config,
            logger = logger,
        )
    }
}
