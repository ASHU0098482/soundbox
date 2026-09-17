package com.ashupaybox.core.update

/**
 * Centralized configuration for in-app GitHub releases update checker.
 * All repository endpoints, channel rules, and asset filters are declared here.
 */
object UpdateConfig {
    // Target GitHub repository from merchant setup
    const val GITHUB_OWNER = "ASHU0098482"
    const val GITHUB_REPO = "soundbox"

    val LATEST_RELEASE_API_URL: String
        get() = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    val ALL_RELEASES_API_URL: String
        get() = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"

    // Only accept genuine production APK assets, strictly rejecting source tarballs or debug zips
    val APK_ASSET_REGEX = Regex("^(AshuPayBox|app)-.*\\.apk$", RegexOption.IGNORE_CASE)

    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 30_000

    // Minimum check interval to avoid aggressive GitHub rate-limiting (e.g. 1 hour)
    const val MIN_CHECK_INTERVAL_MILLIS = 3_600_000L
}
