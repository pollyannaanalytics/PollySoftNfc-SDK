package org.pollyanna.nfckmp.model

sealed interface CardReadResult {
    data class Success(val rawData: ByteArray) : CardReadResult {
        fun clear() = rawData.fill(0)
    }
    sealed class Failure : CardReadResult {
        data class SecurityViolation(val reason: String) : Failure()
    }
}