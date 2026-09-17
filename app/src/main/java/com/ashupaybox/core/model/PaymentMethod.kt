package com.ashupaybox.core.model

enum class PaymentMethod(val displayName: String) {
    UPI("UPI"),
    CARD("Card"),
    WALLET("Wallet"),
    NETBANKING("NetBanking"),
    OTHER("Other")
}
