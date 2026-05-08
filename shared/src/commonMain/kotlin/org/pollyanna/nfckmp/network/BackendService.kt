package org.pollyanna.nfckmp.network

import org.pollyanna.nfckmp.model.SecurePayload

/**
 * Your server-side integration point. Implement this interface to connect the SDK to your
 * payment backend.
 *
 * All methods are `suspend` functions called from SDK-managed coroutines. Throw any
 * [Exception] on failure — the engine maps it to an appropriate [org.pollyanna.nfckmp.model.PaymentState.Failed]
 * subtype. Timeouts are enforced by the engine itself (see [org.pollyanna.nfckmp.EngineConfig]);
 * you do not need to add your own.
 */
interface BackendService {

    /**
     * Returns a one-time challenge nonce that binds the device attestation certificate to this
     * specific registration session, preventing replay attacks.
     *
     * Your backend should generate a cryptographically random value on every call and
     * invalidate it once used.
     */
    suspend fun getRegistrationChallenge(): ByteArray

    /**
     * Submits the device's hardware-backed certificate chain for server-side verification.
     *
     * Your backend should parse the chain, verify it against Google's root CA (for Android)
     * or Apple's root CA (for iOS), and store the leaf certificate's public key for future
     * transaction verification.
     *
     * @param certificateChain Concatenated DER-encoded X.509 certificates, leaf certificate first.
     */
    suspend fun registerDevice(certificateChain: ByteArray)

    /**
     * Returns the backend's RSA public key as a DER-encoded X.509 SubjectPublicKeyInfo blob.
     *
     * The SDK uses this key to encrypt card data via RSA-OAEP/SHA-256 before it leaves the
     * device, ensuring plaintext card data never crosses the network boundary.
     */
    suspend fun getPublicKey(): ByteArray

    /**
     * Submits an encrypted, signed payment payload for processing.
     *
     * Your backend should:
     * 1. Validate [integrityToken] against Google Play Integrity / Apple App Attest.
     * 2. Decrypt [payload.encryptedData] using its private key.
     * 3. Verify [payload.signature] against the registered device certificate.
     * 4. Forward the card data to your payment processor.
     *
     * The SDK zeros both byte arrays in [payload] immediately after this call returns,
     * regardless of success or failure.
     *
     * @param payload RSA-OAEP encrypted card data and its SHA256withRSA signature.
     * @param integrityToken Play Integrity / App Attest assertion for server-side device validation.
     */
    suspend fun submitDeviceBinding(payload: SecurePayload, integrityToken: String)
}
