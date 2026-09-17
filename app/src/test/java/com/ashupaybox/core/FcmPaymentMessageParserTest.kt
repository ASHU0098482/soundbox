package com.ashupaybox.core

import com.ashupaybox.core.fcm.FcmPaymentMessageParser
import com.ashupaybox.core.model.PaymentSource
import com.ashupaybox.core.model.PaymentStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FcmPaymentMessageParserTest {

    @Test
    fun parse_valid35RupeesPayment_success() {
        val payload = mapOf(
            "type" to "PAYMENT_EVENT",
            "eventId" to "evt_test_35",
            "paymentId" to "pay_test_35",
            "amountPaise" to "3500",
            "currency" to "INR",
            "status" to "CAPTURED",
            "source" to "FCM_TEST"
        )

        val result = FcmPaymentMessageParser.parse(payload)
        assertThat(result).isInstanceOf(FcmPaymentMessageParser.ParseResult.Success::class.java)

        val event = (result as FcmPaymentMessageParser.ParseResult.Success).event
        assertThat(event.amountPaise).isEqualTo(3500L)
        assertThat(event.amountRupees).isEqualTo(35L)
        assertThat(event.eventId).isEqualTo("evt_test_35")
        assertThat(event.status).isEqualTo(PaymentStatus.CAPTURED)
        assertThat(event.source).isEqualTo(PaymentSource.FCM_TEST)
        assertThat(event.isTest).isTrue()
    }

    @Test
    fun parse_valid300RupeesPayment_backendVerified_success() {
        val payload = mapOf(
            "type" to "PAYMENT_EVENT",
            "eventId" to "evt_live_300",
            "paymentId" to "pay_live_300",
            "orderId" to "order_300",
            "amountPaise" to "30000",
            "currency" to "INR",
            "status" to "CAPTURED",
            "source" to "BACKEND_VERIFIED",
            "payerName" to "Rahul Sharma"
        )

        val result = FcmPaymentMessageParser.parse(payload)
        assertThat(result).isInstanceOf(FcmPaymentMessageParser.ParseResult.Success::class.java)

        val event = (result as FcmPaymentMessageParser.ParseResult.Success).event
        assertThat(event.amountPaise).isEqualTo(30000L)
        assertThat(event.amountRupees).isEqualTo(300L)
        assertThat(event.source).isEqualTo(PaymentSource.BACKEND_VERIFIED)
        assertThat(event.isTest).isFalse()
        assertThat(event.payerName).isEqualTo("Rahul Sharma")
    }

    @Test
    fun parse_missingEventId_rejected() {
        val payload = mapOf(
            "type" to "PAYMENT_EVENT",
            "paymentId" to "pay_test_missing_evt",
            "amountPaise" to "10000"
        )

        val result = FcmPaymentMessageParser.parse(payload)
        assertThat(result).isInstanceOf(FcmPaymentMessageParser.ParseResult.Error::class.java)
        assertThat((result as FcmPaymentMessageParser.ParseResult.Error).reason).contains("eventId")
    }

    @Test
    fun parse_invalidAmount_rejected() {
        val payload = mapOf(
            "type" to "PAYMENT_EVENT",
            "eventId" to "evt_invalid_amt",
            "paymentId" to "pay_123",
            "amountPaise" to "-500"
        )

        val result = FcmPaymentMessageParser.parse(payload)
        assertThat(result).isInstanceOf(FcmPaymentMessageParser.ParseResult.Error::class.java)
    }

    @Test
    fun parse_unknownMessageType_rejected() {
        val payload = mapOf(
            "type" to "DEVICE_CONFIG_UPDATE",
            "eventId" to "evt_123",
            "paymentId" to "pay_123",
            "amountPaise" to "5000"
        )

        val result = FcmPaymentMessageParser.parse(payload)
        assertThat(result).isInstanceOf(FcmPaymentMessageParser.ParseResult.Error::class.java)
        assertThat((result as FcmPaymentMessageParser.ParseResult.Error).reason).contains("type")
    }
}
