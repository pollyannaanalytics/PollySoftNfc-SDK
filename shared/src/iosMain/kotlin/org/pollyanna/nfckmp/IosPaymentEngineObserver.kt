package org.pollyanna.nfckmp

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.pollyanna.nfckmp.model.PaymentState

/**
 * Swift-friendly wrapper around PollyPaymentEngine.
 *
 * WHY THIS EXISTS
 * ───────────────
 * Kotlin's StateFlow and suspend functions are not directly usable from Swift without
 * additional libraries (e.g. SKIE, KMP-NativeCoroutines). This observer class bridges
 * the gap using the callback pattern:
 *
 *   1. watchState(onChange:)  — starts collecting the StateFlow and fires onChange
 *      on every emission (always on the main thread via MainScope).
 *   2. initialize() / startTransaction() — launch coroutines internally so Swift
 *      callers don't need to know about CoroutineScope at all.
 *   3. cancel() — must be called from Swift's deinit to avoid leaks.
 *
 * SWIFT USAGE PATTERN
 * ───────────────────
 * class PaymentViewModel: ObservableObject {
 *     private let observer: IosPaymentEngineObserver
 *
 *     init() {
 *         let engine = PlatformProviderFactory()
 *             .createEngine(backendService: MockBackendService(), logger: PollyLogger.Default)
 *         observer = IosPaymentEngineObserver(engine: engine)
 *         observer.watchState { [weak self] state in
 *             Task { @MainActor in self?.refresh(state) }
 *         }
 *     }
 *     deinit { observer.cancel() }
 * }
 */
class IosPaymentEngineObserver(private val engine: PollyPaymentEngine) {

    // MainScope uses Dispatchers.Main — callbacks arrive on the main thread.
    private val scope = MainScope()

    /** Starts collecting paymentState; fires [onChange] for every new value. */
    fun watchState(onChange: (PaymentState) -> Unit) {
        scope.launch {
            engine.paymentState.collect { onChange(it) }
        }
    }

    fun initialize() {
        scope.launch { engine.initialize() }
    }

    fun startTransaction(amount: Double) {
        scope.launch { engine.startTransaction(amount) }
    }

    /** Call from Swift deinit to stop the internal coroutine scope. */
    fun cancel() {
        scope.cancel()
    }
}
