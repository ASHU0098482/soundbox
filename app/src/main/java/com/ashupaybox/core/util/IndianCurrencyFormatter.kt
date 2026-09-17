package com.ashupaybox.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object IndianCurrencyFormatter {

    /**
     * Formats integer paise into standard Indian Rupee notation.
     * Example:
     * 3500 paise -> ₹35
     * 150000 paise -> ₹1,500
     * 1254000 paise -> ₹12,540
     * 12500000 paise -> ₹1,25,000
     * 125000000 paise -> ₹12,50,000
     */
    fun formatRupees(amountPaise: Long, includeSymbol: Boolean = true): String {
        val rupees = amountPaise / 100L
        val paise = amountPaise % 100L
        val formattedNumber = formatIndianGrouping(rupees)

        val prefix = if (includeSymbol) "₹" else ""
        return if (paise > 0) {
            val paiseStr = String.format(Locale.US, "%02d", paise)
            "$prefix$formattedNumber.$paiseStr"
        } else {
            "$prefix$formattedNumber"
        }
    }

    fun formatPaise(amountPaise: Long, includeSymbol: Boolean = true): String {
        return formatRupees(amountPaise, includeSymbol)
    }

    /**
     * Converts a number to standard Indian grouping string (e.g. 12,50,000).
     */
    fun formatIndianGrouping(value: Long): String {
        if (value == 0L) return "0"
        val isNegative = value < 0
        val absValue = if (isNegative) -value else value

        val str = absValue.toString()
        val len = str.length

        if (len <= 3) {
            return if (isNegative) "-$str" else str
        }

        // Last 3 digits
        val lastThree = str.substring(len - 3)
        var remaining = str.substring(0, len - 3)

        val sb = StringBuilder()
        while (remaining.length > 2) {
            val part = remaining.substring(remaining.length - 2)
            sb.insert(0, ",$part")
            remaining = remaining.substring(0, remaining.length - 2)
        }
        if (remaining.isNotEmpty()) {
            sb.insert(0, remaining)
        }
        sb.append(",").append(lastThree)

        val result = sb.toString()
        return if (isNegative) "-$result" else result
    }
}
