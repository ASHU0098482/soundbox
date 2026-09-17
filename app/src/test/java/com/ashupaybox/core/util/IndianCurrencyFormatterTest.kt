package com.ashupaybox.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IndianCurrencyFormatterTest {

    @Test
    fun `formatRupees accurately formats various amounts in Indian numbering format`() {
        // ₹1 (100 paise)
        assertThat(IndianCurrencyFormatter.formatRupees(100L)).isEqualTo("₹1")

        // ₹10 (1000 paise)
        assertThat(IndianCurrencyFormatter.formatRupees(1000L)).isEqualTo("₹10")

        // ₹35 (3500 paise)
        assertThat(IndianCurrencyFormatter.formatRupees(3500L)).isEqualTo("₹35")

        // ₹100 (10000 paise)
        assertThat(IndianCurrencyFormatter.formatRupees(10000L)).isEqualTo("₹100")

        // ₹999 (99900 paise)
        assertThat(IndianCurrencyFormatter.formatRupees(99900L)).isEqualTo("₹999")

        // ₹1,000 (100000 paise)
        assertThat(IndianCurrencyFormatter.formatRupees(100000L)).isEqualTo("₹1,000")

        // ₹10,000 (1000000 paise)
        assertThat(IndianCurrencyFormatter.formatRupees(1000000L)).isEqualTo("₹10,000")

        // ₹12,540 (1254000 paise)
        assertThat(IndianCurrencyFormatter.formatRupees(1254000L)).isEqualTo("₹12,540")

        // ₹1,00,000 (10000000 paise) - 1 Lakh
        assertThat(IndianCurrencyFormatter.formatRupees(10000000L)).isEqualTo("₹1,00,000")

        // ₹2,50,000 (25000000 paise) - 2.5 Lakhs
        assertThat(IndianCurrencyFormatter.formatRupees(25000000L)).isEqualTo("₹2,50,000")

        // ₹12,50,000 (125000000 paise) - 12.5 Lakhs
        assertThat(IndianCurrencyFormatter.formatRupees(125000000L)).isEqualTo("₹12,50,000")

        // ₹1,00,00,000 (1000000000 paise) - 1 Crore
        assertThat(IndianCurrencyFormatter.formatRupees(1000000000L)).isEqualTo("₹1,00,00,000")

        // ₹1,25,00,000 (1250000000 paise) - 1.25 Crore
        assertThat(IndianCurrencyFormatter.formatRupees(1250000000L)).isEqualTo("₹1,25,00,000")
    }

    @Test
    fun `formatRupees handles fractional paise correctly`() {
        assertThat(IndianCurrencyFormatter.formatRupees(3550L)).isEqualTo("₹35.50")
        assertThat(IndianCurrencyFormatter.formatRupees(1005L)).isEqualTo("₹10.05")
    }

    @Test
    fun `formatRupees handles zero amount`() {
        assertThat(IndianCurrencyFormatter.formatRupees(0L)).isEqualTo("₹0")
    }
}
