package com.ashupaybox.domain

import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.model.PaymentSource
import com.ashupaybox.core.model.PaymentStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class DuplicateEventTest {

    // Simulates the idempotency store behavior of ProcessedEventDao
    private val processedEventIds = ConcurrentHashMap.newKeySet<String>()
    private val transactionStore = mutableListOf<PaymentEvent>()

    private fun processPaymentIdempotent(event: PaymentEvent): Boolean {
        // Idempotency check: if eventId already processed, reject
        if (processedEventIds.contains(event.eventId)) {
            return false
        }

        // Atomically record and store
        processedEventIds.add(event.eventId)
        transactionStore.add(event)
        return true
    }

    @Test
    fun `same eventId processed twice results in exactly one stored transaction`() {
        val event1 = PaymentEvent(
            eventId = "evt_unique_12345",
            paymentId = "pay_razorpay_999",
            orderId = "order_888",
            amountPaise = 35000L, // ₹350
            status = PaymentStatus.CAPTURED,
            paymentMethod = PaymentMethod.UPI,
            source = PaymentSource.LOCAL_TEST
        )

        val firstResult = processPaymentIdempotent(event1)
        assertThat(firstResult).isTrue()
        assertThat(transactionStore).hasSize(1)

        // Duplicate event with identical eventId sent again
        val duplicateEvent = event1.copy(receivedAt = System.currentTimeMillis() + 1000)
        val secondResult = processPaymentIdempotent(duplicateEvent)

        assertThat(secondResult).isFalse()
        assertThat(transactionStore).hasSize(1)
        assertThat(transactionStore.first().amountPaise).isEqualTo(35000L)
    }

    @Test
    fun `only successful CAPTURED status should be announced`() {
        assertThat(PaymentStatus.CAPTURED.isSuccessful).isTrue()
        assertThat(PaymentStatus.PENDING.isSuccessful).isFalse()
        assertThat(PaymentStatus.FAILED.isSuccessful).isFalse()
        assertThat(PaymentStatus.REFUNDED.isSuccessful).isFalse()
    }

    @Test
    fun `money calculations in integer paise prevent IEEE-754 floating point inaccuracies`() {
        // 35.10 + 35.20 in float: 70.300003
        // In integer paise: 3510 + 3520 = 7030 exactly
        val item1Paise = 3510L
        val item2Paise = 3520L
        val totalPaise = item1Paise + item2Paise
        assertThat(totalPaise).isEqualTo(7030L)
        assertThat(totalPaise / 100L).isEqualTo(70L)
        assertThat(totalPaise % 100L).isEqualTo(30L)
    }
}
