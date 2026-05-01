package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.CardReadResult

interface CardReadRepository {
    suspend fun readSecureData(amount: Double): CardReadResult
}