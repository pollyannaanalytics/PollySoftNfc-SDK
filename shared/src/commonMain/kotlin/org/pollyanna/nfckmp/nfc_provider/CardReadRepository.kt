package org.pollyanna.nfckmp.nfc_provider

import org.pollyanna.nfckmp.model.CardReadResult

/**
 * Abstraction over the NFC hardware layer.
 *
 * Implementations are responsible for:
 * - Enforcing local security checks (root/debugger detection) before any card interaction.
 * - Fetching a hardware assertion token (Play Integrity / App Attest).
 * - Reading raw card data via the platform NFC stack (IsoDep / CoreNFC).
 * - Zeroing sensitive buffers immediately after the data is returned.
 *
 * The engine calls [readSecureData] exactly once per transaction and does not retry on failure.
 */
interface CardReadRepository {

    /**
     * Performs a full secure NFC card read for a payment of [amount].
     *
     * Returns [CardReadResult.Success] with the raw card bytes and an integrity token, or
     * [CardReadResult.Failure.SecurityViolation] if a local security check fails before the
     * NFC read can begin.
     *
     * @param amount The transaction amount passed to the NFC data source so the card reader
     *   can embed it into the command APDU where required by the payment protocol.
     */
    suspend fun readSecureData(amount: Double): CardReadResult
}
