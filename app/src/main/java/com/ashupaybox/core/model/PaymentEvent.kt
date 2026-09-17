package com.ashupaybox.core.model

/**
 * Authoritative immutable payment event received either locally from the simulator
 * or from future FCM / backend webhooks.
 *
 * NOTE: Monetary amounts are strictly stored and manipulated as integer paise (Long)
 * to eliminate IEEE-754 floating-point inaccuracies.
 */
data class PaymentEvent(
    val eventId: String,
    val paymentId: String,
    val orderId: String,
    val amountPaise: Long,
    val currency: String = "INR",
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val payerName: String? = null,
    val payerVpaMasked: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val source: PaymentSource = PaymentSource.LOCAL_TEST,
    val receivedAt: Long = System.currentTimeMillis(),
    val isTest: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    val amountRupees: Long
        get() = amountPaise / 100L

    val remainderPaise: Long
        get() = amountPaise % 100L
}
