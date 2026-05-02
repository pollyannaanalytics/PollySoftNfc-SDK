package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.CardReadResult
import org.pollyanna.nfckmp.security.AttestationCheckProvider

class AndroidCardReadRepository(
    private val scanner: PaymentCardScanDataSource,
    private val securityChecker: AttestationCheckProvider
) : CardReadRepository {
    override suspend fun readSecureData(amount: Double): CardReadResult {
        try {
            securityChecker.checkLocalSecurity()
        } catch (e: SecurityException) {
            return CardReadResult.Failure.SecurityViolation(e.message ?: "Local security check failed")
        }

        val integrityToken = try {
            securityChecker.fetchHardwareAssertion()
        } catch (e: Exception) {
            return CardReadResult.Failure.SecurityViolation("Hardware attestation failed: ${e.message}")
        }

        val rawData = scanner.scan(amount)
        return try {
            CardReadResult.Success(rawData, integrityToken)
        } finally {
            scanner.clearSensitiveData()
        }
    }
}
