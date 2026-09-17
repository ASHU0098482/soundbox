package com.ashupaybox.service.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ashupaybox.PayBoxApp
import com.ashupaybox.notification.PaymentNotificationManager
import com.ashupaybox.service.PayBoxForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BOOT_RECEIVER", "Device reboot completed. Reinitializing PayBox state.")

            val notificationManager = PaymentNotificationManager(context)
            notificationManager.createNotificationChannels()

            val app = context.applicationContext as? PayBoxApp
            if (app != null) {
                CoroutineScope(Dispatchers.Default).launch {
                    val config = app.preferences.configFlow.first()
                    if (config.keepServiceAlive) {
                        val serviceIntent = Intent(context, PayBoxForegroundService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }
            }
        }
    }
}
