package org.pollyanna.nfckmp

/**
 * Tuning parameters for [PollyPaymentEngine].
 *
 * @param networkTimeoutMs Maximum milliseconds to wait for any single backend call
 *   (challenge fetch, key fetch, device registration, payload submission).
 *   Exceeding the limit emits [model.PaymentState.Failed.TimedOut].
 * @param cardReadTimeoutMs Maximum milliseconds to wait for a card tap after entering
 *   [model.PaymentState.WaitingForCard]. Exceeding the limit emits
 *   [model.PaymentState.Failed.TimedOut].
 */
data class EngineConfig(
    val networkTimeoutMs: Long = 30_000L,
    val cardReadTimeoutMs: Long = 60_000L,
)
