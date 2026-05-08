package org.pollyanna.nfckmp

import org.pollyanna.nfckmp.network.BackendService
import org.pollyanna.nfckmp.nfc_provider.IosCardReadRepository
import org.pollyanna.nfckmp.nfc_provider.IosNfcScanDataSource
import org.pollyanna.nfckmp.security.IosDeviceSecurityRepository

actual class PollyNfc {

    actual fun createEngine(
        backendService: BackendService,
        config: EngineConfig,
        logger: PollyLogger,
    ): PollyPaymentEngine = PollyPaymentEngine(
        cardReadRepository = IosCardReadRepository(IosNfcScanDataSource()),
        backendService = backendService,
        deviceSecurityRepository = IosDeviceSecurityRepository(),
        config = config,
        logger = logger,
    )
}
