package org.pollyanna.nfckmp.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import java.io.File
import java.util.Scanner

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

    private fun fetchPlayIntegrityToken(): String {
        // mock token fetching from Google integrity api
        return "mockTokenFromIntegrityApi"
    }
}