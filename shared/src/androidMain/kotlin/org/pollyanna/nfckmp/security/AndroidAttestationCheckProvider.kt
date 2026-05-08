package org.pollyanna.nfckmp.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import org.pollyanna.nfckmp.shared.BuildConfig
import java.io.File
import java.security.SecureRandom
import java.util.Scanner
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class LocalSecurityException(message: String) : SecurityException(message) {
    class RootedDevices(detail: String) : LocalSecurityException("Security Violation: Device is rooted ($detail)")
    class DebuggerAttached : LocalSecurityException("Security Violation: Debugger is attached")
}

data class RootResult(
    val isRooted: Boolean,
    val reason: String = "None"
)

class AndroidAttestationCheckProvider(private val context: Context) : AttestationCheckProvider {

    override fun checkLocalSecurity(): Boolean {
        val rootCheck = getRootCheckResult()
        if (rootCheck.isRooted) {
            if (BuildConfig.DEBUG) {
                println("Security Check Failed: Root detected via ${rootCheck.reason}")
            }
            throw LocalSecurityException.RootedDevices(rootCheck.reason)
        }

        if (isDebuggerAttached()) {
            if (BuildConfig.DEBUG) {
                println("Security Check Failed: Debugger is currently attached.")
            }
            throw LocalSecurityException.DebuggerAttached()
        }

        return true
    }

    override suspend fun fetchHardwareAssertion(): String {
        checkLocalSecurity()
        return fetchPlayIntegrityToken()
    }

    fun getRootCheckResult(): RootResult {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return RootResult(true, "Build tags contain 'test-keys'")
        }

        val commonPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/working/bin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )

        for (path in commonPaths) {
            if (File(path).exists()) {
                return RootResult(true, "Binary found at: $path")
            }
        }

        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val scanner = Scanner(process.inputStream)
            if (scanner.hasNext()) {
                return RootResult(true, "Execution check: 'which su' returned a path")
            }
        } catch (t: Throwable) {
            println(t.message)
        } finally {
            process?.destroy()
        }

        return RootResult(false)
    }

    private fun isDebuggerAttached(): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private suspend fun fetchPlayIntegrityToken(): String {
        val nonce = generateNonce()
        val manager = IntegrityManagerFactory.create(context)
        val request = IntegrityTokenRequest.newBuilder()
            .setNonce(nonce)
            .build()

        return suspendCancellableCoroutine { cont ->
            manager.requestIntegrityToken(request)
                .addOnSuccessListener { response ->
                    if (cont.isActive) cont.resume(response.token())
                }
                .addOnFailureListener { exception ->
                    if (cont.isActive) cont.resumeWithException(exception)
                }
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }
}
