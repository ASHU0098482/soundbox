package com.ashupaybox.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ashupaybox.MainActivity
import com.ashupaybox.app.R
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.util.IndianCurrencyFormatter

class PaymentNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_PAYMENT_ALERTS = "channel_payment_alerts"
        const val CHANNEL_APP_SERVICE = "channel_app_service"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. High-importance payment channel
            val paymentChannel = NotificationChannel(
                CHANNEL_PAYMENT_ALERTS,
                "Payment Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate loud alerts for verified received merchant payments"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 300)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // 2. Normal service channel for background sync
            val serviceChannel = NotificationChannel(
                CHANNEL_APP_SERVICE,
                "App Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background connectivity and synchronization status"
                setShowBadge(false)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(paymentChannel)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun showPaymentNotification(event: PaymentEvent) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_EVENT_ID", event.eventId)
            putExtra("EXTRA_PAYMENT_ID", event.paymentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = IndianCurrencyFormatter.formatRupees(event.amountPaise)
        val title = "Payment Received: $formattedAmount"
        val methodText = event.paymentMethod.displayName
        val payerText = if (!event.payerName.isNullOrBlank()) " from ${event.payerName}" else ""
        val body = "$formattedAmount received successfully via $methodText$payerText."

        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENT_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 350))
            .build()

        try {
            val notificationId = (event.eventId.hashCode() and 0x7FFFFFFF)
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not granted
            e.printStackTrace()
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}
