package com.ashupaybox.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IndianVoiceFormatterTest {

    @Test
    fun `rupeesToWordsEnglish converts amounts to natural Indian English speech`() {
        // ₹1
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(1L)).isEqualTo("One")

        // ₹10
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(10L)).isEqualTo("Ten")

        // ₹35
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(35L)).isEqualTo("Thirty-five")

        // ₹100
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(100L)).isEqualTo("One hundred")

        // ₹350
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(350L)).isEqualTo("Three hundred fifty")

        // ₹999
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(999L)).isEqualTo("Nine hundred ninety-nine")

        // ₹1,000
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(1000L)).isEqualTo("One thousand")

        // ₹1,250
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(1250L)).isEqualTo("One thousand two hundred fifty")

        // ₹10,500
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(10500L)).isEqualTo("Ten thousand five hundred")

        // ₹1,00,000 (1 Lakh)
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(100000L)).isEqualTo("One lakh")

        // ₹2,50,000 (2.5 Lakh)
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(250000L)).isEqualTo("Two lakh fifty thousand")

        // ₹1,00,00,000 (1 Crore)
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(10000000L)).isEqualTo("One crore")

        // ₹1,25,00,000 (1 Crore 25 Lakh)
        assertThat(IndianVoiceFormatter.rupeesToWordsEnglish(12500000L)).isEqualTo("One crore twenty-five lakh")
    }

    @Test
    fun `buildAnnouncementText builds complete phrases for templates`() {
        val phrase1 = IndianVoiceFormatter.buildAnnouncementText(
            amountPaise = 30000L, // ₹300
            language = VoiceLanguage.ENGLISH,
            format = AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT
        )
        assertThat(phrase1).isEqualTo("Payment received. Three hundred rupees.")

        val phrase2 = IndianVoiceFormatter.buildAnnouncementText(
            amountPaise = 30000L,
            language = VoiceLanguage.ENGLISH,
            format = AnnouncementFormat.AMOUNT_RECEIVED
        )
        assertThat(phrase2).isEqualTo("Three hundred rupees received.")

        val phrase3 = IndianVoiceFormatter.buildAnnouncementText(
            amountPaise = 3500L, // ₹35
            language = VoiceLanguage.ENGLISH,
            format = AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT
        )
        assertThat(phrase3).isEqualTo("Payment received. Thirty-five rupees.")

        val phraseHindi = IndianVoiceFormatter.buildAnnouncementText(
            amountPaise = 30000L,
            language = VoiceLanguage.HINDI,
            format = AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT
        )
        assertThat(phraseHindi).contains("रुपये")

        val phraseHinglish = IndianVoiceFormatter.buildAnnouncementText(
            amountPaise = 30000L,
            language = VoiceLanguage.HINGLISH,
            format = AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT
        )
        assertThat(phraseHinglish).isEqualTo("Payment received. Teen sau rupaye.")
    }

    @Test
    fun `announcement includes payment method when requested`() {
        val phrase = IndianVoiceFormatter.buildAnnouncementText(
            amountPaise = 50000L,
            language = VoiceLanguage.ENGLISH,
            format = AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT,
            paymentMethodName = "UPI"
        )
        assertThat(phrase).isEqualTo("Payment received. Five hundred rupees. via UPI.")
    }
}
