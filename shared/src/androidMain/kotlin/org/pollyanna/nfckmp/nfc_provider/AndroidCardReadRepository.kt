package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.CardReadResult
import org.pollyanna.nfckmp.security.AttestationCheckProvider

class AndroidCardReadRepository(
    private val scanner: PaymentCardScanDataSource,
    private val securityChecker: AttestationCheckProvider
) : CardReadRepository {
    override suspend fun readSecureData(amount: Double): CardReadResult {
        if (!securityChecker.checkLocalSecurity())
            return CardReadResult.Failure.SecurityViolation("Device doesn't support local security")
        return CardReadResult.Success(scanner.scan(amount))
    }
}