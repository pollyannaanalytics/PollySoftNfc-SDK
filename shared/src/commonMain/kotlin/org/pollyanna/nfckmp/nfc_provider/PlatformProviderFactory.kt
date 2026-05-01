package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.PollyLogger
import org.pollyanna.nfckmp.PollyPaymentEngine
import org.pollyanna.nfckmp.network.BackendService
import org.pollyanna.nfckmp.security.DeviceSecurityRepository

expect class PlatformProviderFactory {
    fun createCardReadRepository(): CardReadRepository
    fun createDeviceSecurityRepository(): DeviceSecurityRepository
}

fun PlatformProviderFactory.createEngine(
    backendService: BackendService,
    logger: PollyLogger = PollyLogger.Default,
): PollyPaymentEngine = PollyPaymentEngine(
    cardReadRepository = createCardReadRepository(),
    backendService = backendService,
    deviceSecurityRepository = createDeviceSecurityRepository(),
    logger = logger,
)
