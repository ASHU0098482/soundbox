package com.ashupaybox.service.fcm

import android.util.Log
import com.ashupaybox.PayBoxApp
import com.ashupaybox.core.fcm.FcmPaymentMessageParser
import com.ashupaybox.core.fcm.FcmProcessResult
import com.ashupaybox.core.fcm.FirebaseMessageLog
import com.ashupaybox.core.util.IndianCurrencyFormatter
import com.ashupaybox.domain.processor.PaymentProcessResult
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PayBoxFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_SERVICE"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM registration token received: $token")
        val app = applicationContext as? PayBoxApp ?: return
        app.fcmTokenManager.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM onMessageReceived: from=${remoteMessage.from}, data=${remoteMessage.data}")

        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.w(TAG, "Received FCM message with empty data payload, ignoring.")
            FirebaseMessageLog.log(
                messageType = "EMPTY_DATA",
                eventId = "unknown",
                result = FcmProcessResult.REJECTED,
                details = "Empty data payload"
            )
            return
        }

        val app = applicationContext as? PayBoxApp ?: return
        app.fcmTokenManager.recordMessageReceived()

        when (val parseResult = FcmPaymentMessageParser.parse(data)) {
            is FcmPaymentMessageParser.ParseResult.Success -> {
                val event = parseResult.event
                val formattedAmount = IndianCurrencyFormatter.formatPaise(event.amountPaise)
                Log.i(TAG, "Valid payment event received: ${event.eventId} for $formattedAmount (Source: ${event.source})")

                CoroutineScope(Dispatchers.Default).launch {
                    val result = app.eventProcessor.processEvent(event)
                    when (result) {
                        is PaymentProcessResult.Success -> {
                            FirebaseMessageLog.log(
                                messageType = FcmPaymentMessageParser.MESSAGE_TYPE_PAYMENT,
                                eventId = event.eventId,
                                result = FcmProcessResult.PROCESSED,
                                amountFormatted = formattedAmount,
                                details = "Processed and announced successfully"
                            )
                        }
                        is PaymentProcessResult.DuplicateIgnored -> {
                            FirebaseMessageLog.log(
                                messageType = FcmPaymentMessageParser.MESSAGE_TYPE_PAYMENT,
                                eventId = event.eventId,
                                result = FcmProcessResult.DUPLICATE_IGNORED,
                                amountFormatted = formattedAmount,
                                details = "Duplicate eventId ignored"
                            )
                        }
                        is PaymentProcessResult.StatusIgnored -> {
                            FirebaseMessageLog.log(
                                messageType = FcmPaymentMessageParser.MESSAGE_TYPE_PAYMENT,
                                eventId = event.eventId,
                                result = FcmProcessResult.PROCESSED,
                                amountFormatted = formattedAmount,
                                details = "Non-captured status recorded"
                            )
                        }
                        is PaymentProcessResult.Error -> {
                            FirebaseMessageLog.log(
                                messageType = FcmPaymentMessageParser.MESSAGE_TYPE_PAYMENT,
                                eventId = event.eventId,
                                result = FcmProcessResult.FAILED,
                                amountFormatted = formattedAmount,
                                details = result.message
                            )
                        }
                    }
                }
            }
            is FcmPaymentMessageParser.ParseResult.Error -> {
                Log.w(TAG, "Rejected FCM payment message: ${parseResult.reason}")
                FirebaseMessageLog.log(
                    messageType = data["type"] ?: "UNKNOWN",
                    eventId = data["eventId"] ?: "unknown",
                    result = FcmProcessResult.REJECTED,
                    details = parseResult.reason
                )
            }
        }
    }
}
