package com.ashupaybox.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "processed_events",
    indices = [
        Index(value = ["paymentId"])
    ]
)
data class ProcessedEventEntity(
    @PrimaryKey
    val eventId: String,
    val paymentId: String,
    val processedAt: Long = System.currentTimeMillis(),
    val status: String,
    val amountPaise: Long,
    val announced: Boolean = false
)
