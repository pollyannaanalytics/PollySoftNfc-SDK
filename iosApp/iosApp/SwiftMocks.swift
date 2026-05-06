import Foundation
import Shared

// MARK: - Mock implementations of Kotlin interfaces
//
// Each class below implements a Kotlin interface defined in commonMain.
// In production these would call real iOS APIs (Security.framework, Core NFC, etc.),
// but for the PoC they return fake data to prove the data flow works end-to-end.
//
// KEY POINT FOR THE TEAM:
//   These are plain Swift classes — no Kotlin/Native cinterop, no memScoped,
//   no CFDictionary. When we go to production, replace each mock with real
//   Security.framework / Core NFC / App Attest calls, all in native Swift.

// MARK: - PaymentCardScanDataSource (NFC scanning)

class SwiftMockNfcScanner: PaymentCardScanDataSource {
    let scannerType: ScannerType = .ios

    func scan(amount: Double) async throws -> KotlinByteArray {
        // Simulate NFC tap delay
        try await Task.sleep(nanoseconds: 300_000_000)
        // Fake APDU response
        return Data([0x6F, 0x1A, 0x84, 0x07]).toKotlinByteArray()
    }

    func clearSensitiveData() {
        // Nothing to clear in mock
    }
}

// MARK: - AttestationCheckProvider (jailbreak + attestation)

class SwiftMockAttestationChecker: AttestationCheckProvider {

    func checkLocalSecurity() -> KotlinBoolean {
        // Real impl: check jailbreak paths, sysctl P_TRACED, etc.
        return true
    }

    func fetchHardwareAssertion() async throws -> String {
        // Real impl: DCAppAttestService.shared.attestKey / generateAssertion
        return "mock-ios-attestation-token"
    }
}

// MARK: - CryptoDataSource (RSA encryption)

class SwiftMockCryptoDataSource: CryptoDataSource {

    func encrypt(rawData: KotlinByteArray, backendPublicKey: KotlinByteArray) -> KotlinByteArray {
        // Real impl:
        //   let key = SecKeyCreateWithData(...)
        //   let encrypted = SecKeyCreateEncryptedData(key, .rsaEncryptionOAEPSHA256, ...)
        //
        // Mock: return input XOR'd as fake "encryption"
        let size = rawData.size
        let result = KotlinByteArray(size: size)
        for i in 0..<size {
            result.set(index: i, value: rawData.get(index: i) ^ 0x42)
        }
        return result
    }
}

// MARK: - TransactionIdentifyRepository (key management + signing)

class SwiftMockTransactionIdentifyRepo: TransactionIdentifyRepository {

    func getAttestationCertificate(challenge: KotlinByteArray?) async throws -> KotlinByteArray? {
        // Real impl:
        //   SecKeyCreateRandomKey(...) → store in Keychain
        //   SecKeyCopyExternalRepresentation(...) → return public key DER
        return Data([0x30, 0x82, 0x01, 0x22]).toKotlinByteArray()
    }

    func signTransaction(data: KotlinByteArray) -> KotlinByteArray {
        // Real impl:
        //   SecKeyCreateSignature(privateKey, .rsaSignatureMessagePKCS1v15SHA256, ...)
        let size = data.size
        let sig = KotlinByteArray(size: size)
        for i in 0..<size {
            sig.set(index: i, value: data.get(index: i) ^ 0xFF.toInt8())
        }
        return sig
    }
}

// MARK: - Data ↔ KotlinByteArray helpers

extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let result = KotlinByteArray(size: Int32(self.count))
        self.withUnsafeBytes { ptr in
            guard let base = ptr.baseAddress?.assumingMemoryBound(to: Int8.self) else { return }
            for i in 0..<self.count {
                result.set(index: Int32(i), value: base[i])
            }
        }
        return result
    }
}

extension KotlinByteArray {
    func toData() -> Data {
        var bytes = [UInt8](repeating: 0, count: Int(self.size))
        for i in 0..<Int(self.size) {
            bytes[i] = UInt8(bitPattern: self.get(index: Int32(i)))
        }
        return Data(bytes)
    }
}

private extension Int {
    func toInt8() -> Int8 { Int8(truncatingIfNeeded: self) }
}
