import Foundation
import Shared

// MARK: - Payment state mapping
//
// Kotlin sealed interface → Swift enum. Maps KMM types to something SwiftUI can switch on.

enum AppPaymentState: Equatable {
    case idle
    case initializing
    case waitingForCard
    case communicating
    case success
    case failedNotInitialized
    case failedLocalSecurity
    case failedBackend(String)

    static func from(_ ks: PaymentState) -> AppPaymentState {
        switch ks {
        case is PaymentState.Idle:                         return .idle
        case is PaymentState.Initializing:                 return .initializing
        case is PaymentState.WaitingForCard:               return .waitingForCard
        case is PaymentState.Communicating:                 return .communicating
        case is PaymentState.Success:                       return .success
        case is PaymentState.FailedNotInitialized:          return .failedNotInitialized
        case is PaymentState.FailedLocalSecurityFailed:     return .failedLocalSecurity
        case let e as PaymentState.FailedBackendError:      return .failedBackend(e.message)
        default:                                            return .idle
        }
    }

    var label: String {
        switch self {
        case .idle:                   return "Ready"
        case .initializing:           return "Initializing…"
        case .waitingForCard:         return "Tap card now"
        case .communicating:          return "Processing…"
        case .success:                return "Payment accepted"
        case .failedNotInitialized:   return "Not initialized"
        case .failedLocalSecurity:    return "Security check failed"
        case .failedBackend(let m):   return "Error: \(m)"
        }
    }

    var isLoading: Bool {
        switch self {
        case .initializing, .waitingForCard, .communicating: return true
        default: return false
        }
    }
}

// MARK: - ViewModel
//
// HOW STATEFLOW / COROUTINES WORK FROM SWIFT
// ───────────────────────────────────────────
//
//  Problem:
//    Kotlin StateFlow.collect is a suspend fun that never returns.
//    Swift can't host a Kotlin CoroutineScope directly.
//
//  Solution (IosPaymentEngineObserver, defined in iosMain Kotlin):
//    ┌──────────────────────────────────────────────────────────────┐
//    │  Kotlin side (iosMain)                                       │
//    │    MainScope().launch { engine.paymentState.collect { cb } } │
//    │                                                              │
//    │  Swift side                                                  │
//    │    observer.watchState { state in ... }  ← plain callback    │
//    │    observer.initialize()                 ← fire & forget     │
//    │    observer.startTransaction(amount:)    ← fire & forget     │
//    │    observer.cancel()                     ← cleanup           │
//    └──────────────────────────────────────────────────────────────┘
//
//  The observer owns a MainScope, so all callbacks arrive on the main thread.
//  Swift never touches CoroutineScope, Dispatchers, or collect.

@MainActor
class PaymentViewModel: ObservableObject {

    @Published var state: AppPaymentState = .idle

    private let observer: IosPaymentEngineObserver

    init() {
        // 1. Build platform dependencies — all pure Swift, injected into Kotlin factory
        let factory = PlatformProviderFactory(
            scanner: SwiftMockNfcScanner(),
            attestation: SwiftMockAttestationChecker(),
            crypto: SwiftMockCryptoDataSource(),
            transactionIdentify: SwiftMockTransactionIdentifyRepo()
        )

        // 2. createEngine returns PollyPaymentEngine (commonMain Kotlin)
        let engine = PlatformProviderFactoryKt.createEngine(
            factory,
            backendService: MockBackendService(),
            logger: PollyLogger.companion.Default
        )

        // 3. Observer bridges StateFlow → callback
        observer = IosPaymentEngineObserver(engine: engine)
        observer.watchState { [weak self] kotlinState in
            self?.state = AppPaymentState.from(kotlinState)
        }
    }

    func initialize() {
        observer.initialize()
    }

    func startTransaction(amount: Double = 100.0) {
        observer.startTransaction(amount: amount)
    }

    deinit {
        observer.cancel()
    }
}
