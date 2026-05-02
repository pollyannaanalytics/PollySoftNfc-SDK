package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.security.DeviceSecurityRepository
import org.pollyanna.nfckmp.security.IosDeviceSecurityRepository

actual class PlatformProviderFactory {

    actual fun createCardReadRepository(): CardReadRepository {
        return IosCardReadRepository(IosNfcScanDataSource(), IosAttestationCheckProvider())
    }

    actual fun createDeviceSecurityRepository(): DeviceSecurityRepository {
        return IosDeviceSecurityRepository()
    }
}
