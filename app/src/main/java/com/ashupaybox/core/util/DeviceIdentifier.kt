package com.ashupaybox.core.util

import android.content.Context
import java.util.UUID

object DeviceIdentifier {
    private const val PREF_FILE = "paybox_device_id_pref"
    private const val KEY_DEVICE_ID = "app_installation_id"

    /**
     * Returns a privacy-safe, app-scoped installation identifier.
     * Does NOT access hardware IMEI, MAC address, or Android ID,
     * ensuring 100% compliance with Google Play Developer Policies.
     */
    @Synchronized
    fun getOrCreateInstallationId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = "PAYBOX-" + UUID.randomUUID().toString().uppercase().substring(0, 13)
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }
}
