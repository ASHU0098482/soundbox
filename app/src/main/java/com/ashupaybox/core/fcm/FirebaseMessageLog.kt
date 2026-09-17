package com.ashupaybox.core.fcm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

data class FcmLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: String,
    val eventId: String,
    val amountFormatted: String? = null,
    val result: FcmProcessResult,
    val details: String? = null
)

enum class FcmProcessResult {
    PROCESSED,
    DUPLICATE_IGNORED,
    REJECTED,
    FAILED
}

/**
 * Thread-safe in-memory ring buffer of the last 20 FCM events received.
 * Exposes a StateFlow for developer / diagnostic screens.
 */
object FirebaseMessageLog {
    private const val MAX_LOG_SIZE = 20
    private val deque = ConcurrentLinkedDeque<FcmLogEntry>()
    private val _logsFlow = MutableStateFlow<List<FcmLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<FcmLogEntry>> = _logsFlow.asStateFlow()

    fun log(
        messageType: String,
        eventId: String,
        result: FcmProcessResult,
        amountFormatted: String? = null,
        details: String? = null
    ) {
        val entry = FcmLogEntry(
            timestamp = System.currentTimeMillis(),
            messageType = messageType,
            eventId = eventId,
            amountFormatted = amountFormatted,
            result = result,
            details = details
        )
        deque.addFirst(entry)
        while (deque.size > MAX_LOG_SIZE) {
            deque.pollLast()
        }
        _logsFlow.value = deque.toList()
    }

    fun getRecentLogs(): List<FcmLogEntry> = deque.toList()

    fun clear() {
        deque.clear()
        _logsFlow.value = emptyList()
    }
}
