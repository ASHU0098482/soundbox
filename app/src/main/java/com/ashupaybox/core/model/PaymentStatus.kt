package com.ashupaybox.core.model

enum class PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED;

    val isSuccessful: Boolean
        get() = this == CAPTURED
}
