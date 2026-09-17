package com.ashupaybox.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.model.PaymentSource
import com.ashupaybox.core.model.PaymentStatus

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["paymentId"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["isTest"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val paymentId: String,
    val orderId: String,
    val amountPaise: Long,
    val currency: String = "INR",
    val status: String,
    val paymentMethod: String,
    val payerName: String?,
    val payerVpaMasked: String?,
    val timestamp: Long,
    val receivedAt: Long,
    val source: String,
    val isTest: Boolean
) {
    fun toDomain(): PaymentEvent {
        return PaymentEvent(
            eventId = eventId,
            paymentId = paymentId,
            orderId = orderId,
            amountPaise = amountPaise,
            currency = currency,
            status = try { PaymentStatus.valueOf(status) } catch (e: Exception) { PaymentStatus.CAPTURED },
            paymentMethod = try { PaymentMethod.valueOf(paymentMethod) } catch (e: Exception) { PaymentMethod.UPI },
            payerName = payerName,
            payerVpaMasked = payerVpaMasked,
            timestamp = timestamp,
            source = try { PaymentSource.valueOf(source) } catch (e: Exception) { PaymentSource.LOCAL_TEST },
            receivedAt = receivedAt,
            isTest = isTest
        )
    }

    companion object {
        fun fromDomain(event: PaymentEvent): TransactionEntity {
            return TransactionEntity(
                eventId = event.eventId,
                paymentId = event.paymentId,
                orderId = event.orderId,
                amountPaise = event.amountPaise,
                currency = event.currency,
                status = event.status.name,
                paymentMethod = event.paymentMethod.name,
                payerName = event.payerName,
                payerVpaMasked = event.payerVpaMasked,
                timestamp = event.timestamp,
                receivedAt = event.receivedAt,
                source = event.source.name,
                isTest = event.isTest
            )
        }
    }
}
