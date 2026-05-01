package org.pollyanna.nfckmp.security

import android.content.Context
import android.content.pm.ApplicationInfo

class AndroidAttestationCheckProvider(private val context: Context) : AttestationCheckProvider {
    override fun checkLocalSecurity() {
        if (isRooted() || isDebuggerAttached()) throw SecurityException("device should not be rooted!")
    }

    override suspend fun fetchHardwareAssertion(): String {
        checkLocalSecurity()
        return fetchPlayIntegrityToken()
    }

    private fun isRooted(): Boolean {
        return checkPaths(arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su"))
    }
    private fun isDebuggerAttached(): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun checkPaths(vararg paths: Array<String>): Boolean {
        // mock the modification of checking android file path
        return true
    }

    private fun fetchPlayIntegrityToken(): String {
        // mock token fetching from Google integrity api
        return "mockTokenFromIntegrityApi"
    }
}