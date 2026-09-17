package com.ashupaybox.core.util

enum class VoiceLanguage(val id: String, val displayName: String, val localeCode: String) {
    ENGLISH("en", "English", "en-IN"),
    HINDI("hi", "Hindi", "hi-IN"),
    HINGLISH("hinglish", "Hinglish", "hi-IN")
}

enum class AnnouncementFormat(val id: String, val displayName: String) {
    PAYMENT_RECEIVED_AMOUNT("format_1", "Payment received. [Amount]"),
    AMOUNT_RECEIVED("format_2", "[Amount] received"),
    PAYMENT_RECEIVED_SUCCESS_AMOUNT("format_3", "Payment received successfully. [Amount]"),
    CUSTOM("format_4", "Custom Template")
}

object IndianVoiceFormatter {

    private val UNITS_EN = arrayOf(
        "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    )

    private val TENS_EN = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    )

    private val UNITS_HI = arrayOf(
        "", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ",
        "दस", "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस"
    )

    private val TENS_HI = arrayOf(
        "", "दस", "बीस", "तीस", "चालीस", "पचास", "साठ", "सत्तर", "अस्सी", "नब्बे"
    )

    private val UNITS_HINGLISH = arrayOf(
        "", "ek", "do", "teen", "chaar", "paanch", "chhah", "saat", "aath", "nau",
        "das", "gyarah", "barah", "terah", "chaudah", "pandrah", "solah", "satrah", "atharah", "unnees"
    )

    private val TENS_HINGLISH = arrayOf(
        "", "das", "bees", "tees", "chalees", "pachaas", "saath", "sattar", "assi", "nabbe"
    )

    /**
     * Converts a rupees amount (Long) into natural English words according to the Indian numbering system.
     * Examples:
     * 35 -> "Thirty-five"
     * 350 -> "Three hundred fifty"
     * 1250 -> "One thousand two hundred fifty"
     * 10500 -> "Ten thousand five hundred"
     * 100000 -> "One lakh"
     * 250000 -> "Two lakh fifty thousand"
     * 12500000 -> "One crore twenty-five lakh"
     */
    fun rupeesToWordsEnglish(rupees: Long): String {
        if (rupees == 0L) return "Zero"
        val raw = convertRawEnglish(rupees)
        return raw.replaceFirstChar { it.uppercase() }
    }

    private fun convertRawEnglish(rupees: Long): String {
        if (rupees == 0L) return "zero"
        if (rupees < 0L) return "minus " + convertRawEnglish(-rupees)

        val parts = mutableListOf<String>()

        val crore = rupees / 10000000L
        var remainder = rupees % 10000000L

        if (crore > 0) {
            parts.add("${convertRawEnglish(crore)} crore")
        }

        val lakh = remainder / 100000L
        remainder %= 100000L

        if (lakh > 0) {
            parts.add("${convertRawEnglish(lakh)} lakh")
        }

        val thousand = remainder / 1000L
        remainder %= 1000L

        if (thousand > 0) {
            parts.add("${convertRawEnglish(thousand)} thousand")
        }

        val hundred = remainder / 100L
        remainder %= 100L

        if (hundred > 0) {
            parts.add("${UNITS_EN[hundred.toInt()]} hundred")
        }

        if (remainder > 0) {
            if (remainder < 20) {
                parts.add(UNITS_EN[remainder.toInt()])
            } else {
                val ten = (remainder / 10).toInt()
                val unit = (remainder % 10).toInt()
                val word = if (unit > 0) "${TENS_EN[ten]}-${UNITS_EN[unit]}" else TENS_EN[ten]
                parts.add(word)
            }
        }

        return parts.joinToString(" ")
    }

    /**
     * Converts a rupees amount to Hindi words using Indian numbering.
     */
    fun rupeesToWordsHindi(rupees: Long): String {
        if (rupees == 0L) return "शून्य"
        if (rupees < 0L) return "माइनस " + rupeesToWordsHindi(-rupees)

        val parts = mutableListOf<String>()

        val crore = rupees / 10000000L
        var remainder = rupees % 10000000L

        if (crore > 0) {
            parts.add("${rupeesToWordsHindi(crore)} करोड़")
        }

        val lakh = remainder / 100000L
        remainder %= 100000L

        if (lakh > 0) {
            parts.add("${rupeesToWordsHindi(lakh)} लाख")
        }

        val thousand = remainder / 1000L
        remainder %= 1000L

        if (thousand > 0) {
            parts.add("${rupeesToWordsHindi(thousand)} हज़ार")
        }

        val hundred = remainder / 100L
        remainder %= 100L

        if (hundred > 0) {
            parts.add("${UNITS_HI[hundred.toInt()]} सौ")
        }

        if (remainder > 0) {
            if (remainder < 20) {
                parts.add(UNITS_HI[remainder.toInt()])
            } else {
                val ten = (remainder / 10).toInt()
                val unit = (remainder % 10).toInt()
                val word = if (unit > 0) "${TENS_HI[ten]} ${UNITS_HI[unit]}" else TENS_HI[ten]
                parts.add(word)
            }
        }

        return parts.joinToString(" ")
    }

    /**
     * Converts rupees into Hinglish phonetic words.
     */
    fun rupeesToWordsHinglish(rupees: Long): String {
        if (rupees == 0L) return "Zero"
        val raw = convertRawHinglish(rupees)
        return raw.replaceFirstChar { it.uppercase() }
    }

    private fun convertRawHinglish(rupees: Long): String {
        if (rupees == 0L) return "zero"
        if (rupees < 0L) return "minus " + convertRawHinglish(-rupees)

        val parts = mutableListOf<String>()

        val crore = rupees / 10000000L
        var remainder = rupees % 10000000L

        if (crore > 0) {
            parts.add("${convertRawHinglish(crore)} crore")
        }

        val lakh = remainder / 100000L
        remainder %= 100000L

        if (lakh > 0) {
            parts.add("${convertRawHinglish(lakh)} lakh")
        }

        val thousand = remainder / 1000L
        remainder %= 1000L

        if (thousand > 0) {
            parts.add("${convertRawHinglish(thousand)} hazaar")
        }

        val hundred = remainder / 100L
        remainder %= 100L

        if (hundred > 0) {
            parts.add("${UNITS_HINGLISH[hundred.toInt()]} sau")
        }

        if (remainder > 0) {
            if (remainder < 20) {
                parts.add(UNITS_HINGLISH[remainder.toInt()])
            } else {
                val ten = (remainder / 10).toInt()
                val unit = (remainder % 10).toInt()
                val word = if (unit > 0) "${TENS_HINGLISH[ten]} ${UNITS_HINGLISH[unit]}" else TENS_HINGLISH[ten]
                parts.add(word)
            }
        }

        return parts.joinToString(" ")
    }

    /**
     * Builds the complete spoken announcement phrase based on language, template format,
     * and amount in paise.
     */
    fun buildAnnouncementText(
        amountPaise: Long,
        language: VoiceLanguage,
        format: AnnouncementFormat,
        customPrefix: String = "",
        customSuffix: String = "",
        paymentMethodName: String? = null
    ): String {
        val rupees = amountPaise / 100L
        val amountWords = when (language) {
            VoiceLanguage.ENGLISH -> rupeesToWordsEnglish(rupees)
            VoiceLanguage.HINDI -> rupeesToWordsHindi(rupees)
            VoiceLanguage.HINGLISH -> rupeesToWordsHinglish(rupees)
        }

        return when (language) {
            VoiceLanguage.ENGLISH -> {
                val base = when (format) {
                    AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT ->
                        "Payment received. $amountWords rupees."
                    AnnouncementFormat.AMOUNT_RECEIVED ->
                        "$amountWords rupees received."
                    AnnouncementFormat.PAYMENT_RECEIVED_SUCCESS_AMOUNT ->
                        "Payment received successfully. $amountWords rupees."
                    AnnouncementFormat.CUSTOM -> {
                        val p = if (customPrefix.isNotBlank()) "$customPrefix " else ""
                        val s = if (customSuffix.isNotBlank()) " $customSuffix" else ""
                        "$p$amountWords rupees$s."
                    }
                }
                if (paymentMethodName != null) "$base via $paymentMethodName." else base
            }
            VoiceLanguage.HINDI -> {
                val base = when (format) {
                    AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT ->
                        "भुगतान प्राप्त हुआ। $amountWords रुपये।"
                    AnnouncementFormat.AMOUNT_RECEIVED ->
                        "$amountWords रुपये प्राप्त हुए।"
                    AnnouncementFormat.PAYMENT_RECEIVED_SUCCESS_AMOUNT ->
                        "सफलतापूर्वक भुगतान प्राप्त हुआ। $amountWords रुपये।"
                    AnnouncementFormat.CUSTOM -> {
                        val p = if (customPrefix.isNotBlank()) "$customPrefix " else ""
                        val s = if (customSuffix.isNotBlank()) " $customSuffix" else ""
                        "$p$amountWords रुपये$s।"
                    }
                }
                if (paymentMethodName != null) "$base $paymentMethodName द्वारा।" else base
            }
            VoiceLanguage.HINGLISH -> {
                val base = when (format) {
                    AnnouncementFormat.PAYMENT_RECEIVED_AMOUNT ->
                        "Payment received. $amountWords rupaye."
                    AnnouncementFormat.AMOUNT_RECEIVED ->
                        "$amountWords rupaye prapt hue."
                    AnnouncementFormat.PAYMENT_RECEIVED_SUCCESS_AMOUNT ->
                        "Safaltapoorvak payment prapt hua. $amountWords rupaye."
                    AnnouncementFormat.CUSTOM -> {
                        val p = if (customPrefix.isNotBlank()) "$customPrefix " else ""
                        val s = if (customSuffix.isNotBlank()) " $customSuffix" else ""
                        "$p$amountWords rupaye$s."
                    }
                }
                if (paymentMethodName != null) "$base $paymentMethodName se." else base
            }
        }
    }
}
