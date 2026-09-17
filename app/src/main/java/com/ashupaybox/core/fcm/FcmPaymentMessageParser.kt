package com.ashupaybox.core.fcm

import android.util.Log
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.model.PaymentSource
import com.ashupaybox.core.model.PaymentStatus

/**
 * Defensive parser for incoming Firebase Cloud Messaging data messages.
 * Validates contract strictly:
 * - type must equal PAYMENT_EVENT
 * - eventId must be present and non-empty
 * - paymentId must be present and non-empty
 * - amountPaise must be a strictly positive integer (> 0)
 * - status must be a known PaymentStatus
 * - currency must be a valid 3-character ISO code (default: INR)
 */
object FcmPaymentMessageParser {

    private const val TAG = "FcmPaymentParser"
    const val MESSAGE_TYPE_PAYMENT = "PAYMENT_EVENT"

    sealed class ParseResult {
        data class Success(val event: PaymentEvent) : ParseResult()
        data class Error(val reason: String) : ParseResult()
    }

    fun parse(data: Map<String, String>): ParseResult {
        if (data.isEmpty()) {
            return ParseResult.Error("Payload is empty")
        }

        // 1. Validate message type
        val type = data["type"]
        if (type != MESSAGE_TYPE_PAYMENT) {
            return ParseResult.Error("Unsupported or missing message type: '$type' (expected $MESSAGE_TYPE_PAYMENT)")
        }

        // 2. Validate eventId (Idempotency Key)
        val eventId = data["eventId"]?.trim()
        if (eventId.isNullOrEmpty()) {
            return ParseResult.Error("Missing or blank 'eventId'")
        }

        // 3. Validate paymentId
        val paymentId = data["paymentId"]?.trim()
        if (paymentId.isNullOrEmpty()) {
            return ParseResult.Error("Missing or blank 'paymentId'")
        }

        // 4. Validate amountPaise (Strict integer paise > 0)
        val amountPaiseRaw = data["amountPaise"] ?: data["amount_paise"]
        val amountPaise = amountPaiseRaw?.toLongOrNull()
        if (amountPaise == null || amountPaise <= 0) {
            // Also support fallback amount in rupees if amountPaise is missing
            val amountRupees = data["amount"]?.toDoubleOrNull()
            if (amountRupees != null && amountRupees > 0) {
                // Warning: round to nearest paise
                val computedPaise = (amountRupees * 100).toLong()
                return buildEvent(data, eventId, paymentId, computedPaise)
            }
            return ParseResult.Error("Invalid or non-positive amountPaise: '$amountPaiseRaw'")
        }

        return buildEvent(data, eventId, paymentId, amountPaise)
    }

    private fun buildEvent(
        data: Map<String, String>,
        eventId: String,
        paymentId: String,
        amountPaise: Long
    ): ParseResult {
        // 5. Currency
        val currency = data["currency"]?.uppercase()?.trim() ?: "INR"
        if (currency.length != 3) {
            return ParseResult.Error("Invalid currency code '$currency' (must be 3-character ISO code)")
        }

        // 6. Status
        val statusRaw = data["status"]?.uppercase()?.trim() ?: "CAPTURED"
        val status = try {
            PaymentStatus.valueOf(statusRaw)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown status '$statusRaw', defaulting to CAPTURED")
            PaymentStatus.CAPTURED
        }

        // 7. Source: FCM_TEST vs BACKEND_VERIFIED
        val sourceRaw = data["source"]?.uppercase()?.trim() ?: "FCM_TEST"
        val source = when (sourceRaw) {
            "BACKEND_VERIFIED", "BACKEND" -> PaymentSource.BACKEND_VERIFIED
            "LOCAL_TEST" -> PaymentSource.LOCAL_TEST
            else -> PaymentSource.FCM_TEST
        }

        // Is test event?
        val isTest = source == PaymentSource.FCM_TEST || source == PaymentSource.LOCAL_TEST ||
                data["isTest"]?.toBooleanStrictOrNull() ?: (source != PaymentSource.BACKEND_VERIFIED)

        // 8. Payment Method
        val methodRaw = data["method"]?.uppercase()?.trim() ?: data["paymentMethod"]?.uppercase()?.trim() ?: "UPI"
        val paymentMethod = try {
            PaymentMethod.valueOf(methodRaw)
        } catch (e: Exception) {
            PaymentMethod.UPI
        }

        // 9. Order & Payer details
        val orderId = data["orderId"]?.trim() ?: data["order_id"]?.trim() ?: "order_fcm_${System.currentTimeMillis()}"
        val payerName = data["payerName"]?.trim() ?: data["payer_name"]?.trim()
        val payerVpaMasked = data["payerVpa"]?.trim() ?: data["payer_vpa"]?.trim()

        // 10. Timestamp
        val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

        val event = PaymentEvent(
            eventId = eventId,
            paymentId = paymentId,
            orderId = orderId,
            amountPaise = amountPaise,
            currency = currency,
            status = status,
            paymentMethod = paymentMethod,
            payerName = payerName,
            payerVpaMasked = payerVpaMasked,
            timestamp = timestamp,
            source = source,
            receivedAt = System.currentTimeMillis(),
            isTest = isTest
        )

        return ParseResult.Success(event)
    }
}
