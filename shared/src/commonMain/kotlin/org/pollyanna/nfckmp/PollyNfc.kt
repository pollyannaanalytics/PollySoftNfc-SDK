package org.pollyanna.nfckmp

import org.pollyanna.nfckmp.network.BackendService

/**
 * SDK entry point — the only class you need to construct a [PollyPaymentEngine].
 *
 * **Android**
 * ```kotlin
 * val engine = PollyNfc(context).createEngine(backendService = YourBackendService())
 * ```
 *
 * **iOS**
 * ```kotlin
 * val engine = PollyNfc().createEngine(backendService = YourBackendService())
 * ```
 *
 * All internal dependencies (crypto, NFC, attestation) are wired automatically.
 * No Koin or other DI knowledge is required on the caller side.
 */
expect class PollyNfc {
    /**
     * Builds a fully wired [PollyPaymentEngine].
     *
     * @param backendService Your server-side bridge. Implement [BackendService] to connect the
     *   SDK to your payment backend.
     * @param config Timeout and tuning parameters. Defaults cover most production scenarios.
     * @param logger Supply [PollyLogger.Silent] to disable SDK log output.
     */
    fun createEngine(
        backendService: BackendService,
        config: EngineConfig = EngineConfig(),
        logger: PollyLogger = PollyLogger.Default,
    ): PollyPaymentEngine
}
