# PollySoftNfc SDK

A Kotlin Multiplatform SDK that turns any Android device into an NFC payment terminal — no dedicated hardware required.

> **Platform support:** Android only. iOS scaffolding exists but security and NFC integrations are not yet implemented.

---

## Overview

### Who is this for?

This SDK is designed for **merchants** who want to accept contactless payments using an ordinary Android phone or tablet, without buying a dedicated POS terminal.

### How does it work?

The **customer** experience is completely unchanged — they pay exactly as they would with Google Pay or Apple Pay: open their wallet app, tap their phone or card, done.

The **merchant** simply opens their own app (which integrates this SDK) and the device is ready to receive the payment over NFC. No extra hardware needed.

```
Customer                    Merchant's Android Device
   │                                │
   │  opens Google / Apple Pay      │  opens merchant app
   │  selects amount to pay         │  (PollySoftNfc SDK inside)
   │                                │
   │ ── taps phone / card ─────────▶│ reads NFC, encrypts data,
   │                                │ verifies device integrity,
   │                                │ submits to backend
   │                                │
   │                         Payment confirmed
```

### What does the SDK handle?

| Concern | Handled by SDK |
|---|---|
| NFC card reading | Yes |
| Device integrity check (root, debugger, Play Integrity) | Yes |
| End-to-end encryption (RSA/OAEP, hardware-backed keys) | Yes |
| Transaction signing (SHA256withRSA) | Yes |
| Payment state management | Yes |
| Unit tests (engine state machine, error paths, sensitive data cleanup) | Yes |
| Backend communication | **You implement** `BackendService` |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.20 |
| Multiplatform | Kotlin Multiplatform Mobile (KMM) |
| UI (demo app) | Compose Multiplatform 1.10.3 |
| Async | Kotlin Coroutines 1.10.2 |
| Encryption | Android Keystore (RSA/OAEP SHA-256) |
| Signing | SHA256withRSA via Android Keystore |
| Security | Root detection, debugger detection, Play Integrity API |
| Key storage | AndroidKeyStore (hardware-backed) |
| Build system | Gradle 8.14.3 (Kotlin DSL) |
| Min SDK | Android API 30 |
| Target SDK | Android API 36 |
| Publishing | GitHub Packages (Maven) |
| CI/CD | GitHub Actions |

---

## Architecture

### Part 1 — Responsibility of Objects

```mermaid
flowchart TD
    subgraph host["Host App"]
        Factory["PlatformProviderFactory\n─────────────────────\nComposition root\nHolds Android Context\nConstructs & wires all deps"]
    end

    subgraph sdk["SDK · commonMain  (platform-agnostic)"]
        Engine["PollyPaymentEngine\n─────────────────────\nOrchestrator\nDepends only on interfaces\nNo platform code"]
        BS["YourBackendService\n─────────────────────\nYou implement this\nOwns the real network layer"]
        CR(["CardReadRepository\nNFC hardware boundary"])
        DSR(["DeviceSecurityRepository\nPlatform crypto boundary"])
        BSI(["BackendService\nNetwork boundary"])
    end

    Factory -->|"assembles via createEngine()"| Engine
    Factory -.->|"creates Android impl"| CR
    Factory -.->|"creates Android impl"| DSR
    BS -.->|"implements"| BSI

    Engine -->|"read card data"| CR
    Engine -->|"encrypt & sign"| DSR
    Engine -->|"register & submit"| BSI
```

---

### Part 2 — Dependency Injection Diagram

```mermaid
classDiagram
    namespace HostApp {
        class PlatformProviderFactory {
            <<Android>>
            -context : Context
            +createCardReadRepository() CardReadRepository
            +createDeviceSecurityRepository() DeviceSecurityRepository
            +createEngine(backendService) PollyPaymentEngine
        }
    }
    
    namespace RealBackend {
        class BackendServiceImpl
    }

    namespace SDKCommonMain {
        class BackendService {
            <<interface>>
            +getRegistrationChallenge() String
            +registerDevice(certs) Unit
            +getPublicKey() ByteArray
            +submitDeviceBinding(payload) Unit
        }
        class PollyPaymentEngine {
            +paymentState : StateFlow~PaymentState~
            +initialize()
            +startTransaction(amount)
        }
        class CardReadRepository {
            <<interface>>
            +readSecureData(amount) CardReadResult
        }
        class DeviceSecurityRepository {
            <<interface>>
            +getRegistrationCertificate(challenge) List~String~
            +encrypt(data, publicKey) SecurityResult
        }
    }

    namespace SDKAndroidMain {
        class AndroidCardReadRepository {
            -scanner : AndroidNfcScanDataSource
            -attestationChecker : AndroidAttestationCheckProvider
        }
        class AndroidNfcScanDataSource
        class AndroidAttestationCheckProvider
        class AndroidDeviceSecurityRepository {
            -cryptoDataSource : AndroidCryptoDataSource
            -transactionRepo : AndroidTransactionIdentifyRepository
        }
        class AndroidCryptoDataSource
        class AndroidTransactionIdentifyRepository {
            -privateKeyDataSource : AndroidPrivateKeyDataSource
        }
        class AndroidPrivateKeyDataSource
    }

    PlatformProviderFactory ..> AndroidCardReadRepository       : creates
    PlatformProviderFactory ..> AndroidDeviceSecurityRepository  : creates
    PlatformProviderFactory ..> PollyPaymentEngine               : assembles

    AndroidCardReadRepository       ..|> CardReadRepository
    AndroidDeviceSecurityRepository ..|> DeviceSecurityRepository
    BackendServiceImpl ..|> BackendService : network communication

    AndroidCardReadRepository       *-- AndroidNfcScanDataSource
    AndroidCardReadRepository       *-- AndroidAttestationCheckProvider
    AndroidDeviceSecurityRepository *-- AndroidCryptoDataSource
    AndroidDeviceSecurityRepository *-- AndroidTransactionIdentifyRepository
    AndroidTransactionIdentifyRepository *-- AndroidPrivateKeyDataSource

    PollyPaymentEngine --> CardReadRepository       : injected
    PollyPaymentEngine --> DeviceSecurityRepository  : injected
    PollyPaymentEngine --> BackendService            : injected
```

### Module Layout

```
PollySoftNfc-SDK/
├── shared/                         # SDK library (published to GitHub Packages)
│   └── src/
│       ├── commonMain/             # Engine + interfaces (platform-agnostic)
│       ├── androidMain/            # Concrete implementations (NFC, Keystore, crypto)
│       └── iosMain/                # iOS stubs (not yet implemented)
├── composeApp/                     # Demo Android app
├── iosApp/                         # iOS app wrapper (Xcode)
└── .github/workflows/publish.yml  # CI: publishes on version tags
```

---

## Business Logic Workflow

The engine operates in two distinct stages: **Initialization** and **Transaction**.

### Stage 1 — Device Initialization

```
Host App                 PollyPaymentEngine              Backend
    │                           │                           │
    │──── initialize() ────────▶│                           │
    │                           │── fetchChallenge() ──────▶│
    │                           │◀─ challenge ──────────────│
    │                           │                           │
    │                           │  [generate RSA key pair   │
    │                           │   in Android Keystore     │
    │                           │   with hardware attest]   │
    │                           │                           │
    │                           │── registerDevice() ──────▶│
    │                           │   (certificate chain)     │
    │◀── PaymentState.Idle ─────│◀─ accessToken ────────────│
```

1. Fetch a one-time challenge from the backend.
2. Generate a hardware-backed RSA key pair in the Android Keystore.
3. Produce an attestation certificate chain proving the key lives in secure hardware.
4. Register the device with the backend; receive an access token.

---

### Stage 2 — Payment Transaction

```
Host App             PollyPaymentEngine        Card (NFC)       Backend
    │                        │                     │                │
    │── startTransaction() ─▶│                     │                │
    │                        │── fetchPublicKey() ────────────────▶│
    │◀─ WaitingForCard ──────│◀────────────────── backendPubKey ───│
    │                        │                     │                │
    │                        │◀──── NFC tap ───────│                │
    │◀─ Communicating ───────│   [read card data   │                │
    │                        │    via APDU]        │                │
    │                        │                     │                │
    │                        │  [security checks:  │                │
    │                        │   root, debugger]   │                │
    │                        │                     │                │
    │                        │  [encrypt card data │                │
    │                        │   with backend pub  │                │
    │                        │   key (RSA/OAEP)]   │                │
    │                        │                     │                │
    │                        │  [sign payload with │                │
    │                        │   device private key│                │
    │                        │   (SHA256withRSA)]  │                │
    │                        │                     │                │
    │                        │── submitTransaction() ─────────────▶│
    │◀─ Success / Failed ────│◀──────────────────────── result ────│
```

### Payment States

```
PaymentState
├── Idle                        # Ready; initialization complete
├── Initializing                # Fetching challenge / registering device
├── WaitingForCard              # NFC listener active, waiting for tap
├── Communicating               # Encrypting & submitting to backend
├── Success                     # Transaction accepted by backend
└── Failed
    ├── NotInitialized          # startTransaction called before initialize
    ├── LocalSecurityFailed     # Root/debugger detected, or encryption error
    └── BackendError(message)   # Network or backend rejection
```

---

## Installation

### 1. Add the GitHub Packages repository

In your project-level `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/pollyannaanalytics/PollySoftNfc-SDK")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.token").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### 2. Add credentials

GitHub Packages requires authentication even for public packages. Add your credentials to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

The token needs at least the `read:packages` scope.

### 3. Add the dependency

In your app-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.pollyanna:shared:0.1.1")
}
```

### 4. Add NFC permission

In your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

---

## Usage

### Implement BackendService

The only interface you must implement is `BackendService`, which connects the SDK to your own server. A `MockBackendService` is included for local testing and development.

| Method | Called when | What to send | What to return |
|---|---|---|---|
| `getRegistrationChallenge()` | Device initialization starts | — | A random nonce (`ByteArray`, e.g. 32 bytes) to bind the key generation to this specific request |
| `registerDevice(certificateChain)` | After hardware key generation | DER-encoded X.509 certificate chain (leaf + intermediates, concatenated) from Android Keystore attestation | — |
| `getPublicKey()` | Start of every transaction | — | Your server's RSA public key in DER format; the SDK uses it to encrypt card data so only your server can decrypt it |
| `submitDeviceBinding(payload, integrityToken)` | After card data is encrypted and signed | `payload.encryptedData` (RSA/OAEP encrypted card data), `payload.signature` (SHA256withRSA over encrypted data), `integrityToken` (Play Integrity JWT — verify with Google before processing) | — |

### Wire up the engine

```kotlin
val engine = PlatformProviderFactory(context)
    .createEngine(backendService = YourBackendServiceImpl())

// Observe state
lifecycleScope.launch {
    engine.paymentState.collect { state ->
        when (state) {
            is PaymentState.Idle           -> showReadyUI()
            is PaymentState.WaitingForCard -> showTapCardPrompt()
            is PaymentState.Communicating  -> showLoadingUI()
            is PaymentState.Success        -> showSuccess()
            is PaymentState.Failed         -> showError(state)
        }
    }
}

// Initialize once (e.g. on app start)
lifecycleScope.launch { engine.initialize() }

// Start a transaction when the merchant is ready to receive payment
lifecycleScope.launch { engine.startTransaction(amount = 49.99) }
```

---

## Publishing

A new version is published to GitHub Packages automatically when a version tag is pushed:

```bash
git tag v0.1.2
git push origin v0.1.2
```

The workflow (`.github/workflows/publish.yml`) builds and publishes the `:shared` module to:
`https://maven.pkg.github.com/pollyannaanalytics/PollySoftNfc-SDK`
