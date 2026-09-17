package com.ashupaybox.core.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Arrays

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failed(val reason: String) : ValidationResult()
}

object UpdateSecurityValidator {

    private const val TAG = "UPDATE_SECURITY"

    /**
     * Strictly verifies APK integrity and authenticity before permitting installation.
     */
    fun verifyDownloadedApk(
        context: Context,
        apkFile: File,
        expectedSha256: String?,
        installedVersionCode: Long
    ): ValidationResult {
        // 1. Check file exists and is not empty
        if (!apkFile.exists() || apkFile.length() <= 0) {
            Log.e(TAG, "Security check failed: Downloaded file is missing or empty")
            return ValidationResult.Failed("Downloaded file is missing or empty.")
        }

        // 2. Verify SHA-256 checksum if provided
        if (!expectedSha256.isNullOrBlank()) {
            val calculatedHash = calculateSha256(apkFile)
            if (!calculatedHash.equals(expectedSha256.trim(), ignoreCase = true)) {
                Log.e(TAG, "Security check failed: SHA-256 mismatch. Expected $expectedSha256, got $calculatedHash")
                apkFile.delete()
                return ValidationResult.Failed("Digest mismatch: Expected $expectedSha256, got $calculatedHash")
            }
        }

        // 3. Verify valid APK archive format
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo: PackageInfo? = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
        if (archiveInfo == null) {
            Log.e(TAG, "Security check failed: Corrupted APK archive")
            apkFile.delete()
            return ValidationResult.Failed("The downloaded file is not a valid Android package archive.")
        }

        // 4. Verify Application Package Name
        val archivePackage = archiveInfo.packageName
        if (archivePackage != context.packageName) {
            Log.e(TAG, "Security check failed: Package name mismatch. Expected ${context.packageName}, got $archivePackage")
            apkFile.delete()
            return ValidationResult.Failed("Package name mismatch ($archivePackage != ${context.packageName}).")
        }

        // 5. Verify Version Code is strictly newer
        val archiveVersionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        if (archiveVersionCode <= installedVersionCode) {
            Log.e(TAG, "Security check failed: Downloaded version ($archiveVersionCode) is not newer than installed ($installedVersionCode)")
            apkFile.delete()
            return ValidationResult.Failed("Downloaded version ($archiveVersionCode) must be higher than current ($installedVersionCode).")
        }

        // 6. Verify Signing Certificate matches current app (prevent spoofing / malicious updates)
        val certMatch = verifySigningCertificateMatches(context, archiveInfo)
        if (!certMatch) {
            Log.e(TAG, "Security check failed: Signing certificate mismatch")
            apkFile.delete()
            return ValidationResult.Failed("Update signing certificate does not match the currently installed app.")
        }

        Log.i(TAG, "APK passed all 6 security checks successfully! Ready for installation.")
        return ValidationResult.Success
    }

    private fun verifySigningCertificateMatches(context: Context, archiveInfo: PackageInfo): Boolean {
        try {
            val pm = context.packageManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val installedSigningInfo = installedInfo.signingInfo ?: return false
                val archiveSigningInfo = archiveInfo.signingInfo ?: return false

                val installedSignatures = if (installedSigningInfo.hasMultipleSigners()) {
                    installedSigningInfo.apkContentsSigners
                } else {
                    installedSigningInfo.signingCertificateHistory
                }

                val archiveSignatures = if (archiveSigningInfo.hasMultipleSigners()) {
                    archiveSigningInfo.apkContentsSigners
                } else {
                    archiveSigningInfo.signingCertificateHistory
                }

                if (installedSignatures.isEmpty() || archiveSignatures.isEmpty()) return false

                return Arrays.equals(installedSignatures[0].toByteArray(), archiveSignatures[0].toByteArray())
            } else {
                @Suppress("DEPRECATION")
                val installedInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val installedSigs = installedInfo.signatures
                @Suppress("DEPRECATION")
                val archiveSigs = archiveInfo.signatures

                if (installedSigs == null || archiveSigs == null || installedSigs.isEmpty() || archiveSigs.isEmpty()) {
                    return false
                }

                return Arrays.equals(installedSigs[0].toByteArray(), archiveSigs[0].toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking signing certificate match", e)
            return false
        }
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val sb = StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
