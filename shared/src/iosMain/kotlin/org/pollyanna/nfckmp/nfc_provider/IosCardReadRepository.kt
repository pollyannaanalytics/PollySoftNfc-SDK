package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.CardReadResult

class IosCardReadRepository(
    private val scanner: PaymentCardScanDataSource,
) : CardReadRepository {

    override suspend fun readSecureData(amount: Double): CardReadResult {
        val rawData = scanner.scan(amount)
        return try {
            CardReadResult.Success(rawData)
        } finally {
            scanner.clearSensitiveData()
        }
    }
}
