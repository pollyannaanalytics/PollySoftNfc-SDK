package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.CardReadResult

class IosCardReadRepository(
    private val scanner: PaymentCardScanDataSource,
) : CardReadRepository {

    override suspend fun readSecureData(amount: Double): CardReadResult {
        // TODO: implement local security check and App Attest / DeviceCheck
        val rawData = scanner.scan(amount)
        return try {
            CardReadResult.Success(rawData, integrityToken = "")
        } finally {
            scanner.clearSensitiveData()
        }
    }
}
