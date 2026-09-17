package com.ashupaybox.domain.processor

import android.util.Log
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentStatus
import com.ashupaybox.core.preferences.PayBoxPreferences
import com.ashupaybox.core.preferences.SoundBoxConfig
import com.ashupaybox.data.repository.PaymentRepository
import com.ashupaybox.notification.PaymentNotificationManager
import com.ashupaybox.sound.PaymentAnnouncementManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class PaymentProcessResult {
    data class Success(val event: PaymentEvent) : PaymentProcessResult()
    data class DuplicateIgnored(val eventId: String) : PaymentProcessResult()
    data class StatusIgnored(val status: PaymentStatus) : PaymentProcessResult()
    data class Error(val message: String) : PaymentProcessResult()
}

/**
 * Authoritative unified pipeline for processing all payment events.
 * Both local test simulations and future remote FCM notifications pass through this exact class.
 *
 * Implements strict idempotency using a mutex and Room duplicate checks.
 */
class PaymentEventProcessor(
    private val repository: PaymentRepository,
    private val notificationManager: PaymentNotificationManager,
    private val announcementManager: PaymentAnnouncementManager,
    private val preferences: PayBoxPreferences,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "PAYMENT_EVENT"
    }

    private val mutex = Mutex()

    // Shared flow to drive live animated UI card
    private val _livePaymentEvents = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 16)
    val livePaymentEvents: SharedFlow<PaymentEvent> = _livePaymentEvents.asSharedFlow()

    /**
     * Atomically processes an incoming payment event.
     */
    suspend fun processEvent(event: PaymentEvent): PaymentProcessResult = mutex.withLock {
        Log.d(TAG, "Incoming payment event: eventId=${event.eventId}, paymentId=${event.paymentId}, amountPaise=${event.amountPaise}, status=${event.status}")

        // 1. Validate basic event integrity
        if (event.eventId.isBlank() || event.paymentId.isBlank() || event.amountPaise <= 0) {
            Log.w(TAG, "Validation failed for event ${event.eventId}")
            return PaymentProcessResult.Error("Invalid event parameters")
        }

        // 2. Strict Duplicate / Idempotency Check
        if (repository.isEventProcessed(event.eventId)) {
            Log.w(TAG, "DUPLICATE_CHECK: Event ${event.eventId} was already processed! Dropping silently.")
            return PaymentProcessResult.DuplicateIgnored(event.eventId)
        }

        // 3. Status Check: only CAPTURED events announced
        if (!event.status.isSuccessful) {
            Log.d(TAG, "Status ${event.status} is non-successful. Saving transaction without voice announcement.")
            repository.insertTransaction(event)
            repository.recordProcessedEvent(event, announced = false)
            return PaymentProcessResult.StatusIgnored(event.status)
        }

        // 4. Save transaction to Room Database atomically
        val inserted = repository.insertTransaction(event)
        if (!inserted) {
            Log.w(TAG, "Database insert ignored or failed for ${event.eventId}")
            return PaymentProcessResult.DuplicateIgnored(event.eventId)
        }

        // 5. Emit to UI for animated live payment card
        _livePaymentEvents.tryEmit(event)

        // 6. Fetch current user preferences
        val config: SoundBoxConfig = preferences.configFlow.first()

        // 7. Trigger Payment Notification
        if (config.isNotificationEnabled) {
            notificationManager.showPaymentNotification(event)
        }

        // 8. Trigger Voice Announcement
        if (config.isSoundBoxEnabled && config.isVoiceEnabled) {
            announcementManager.announcePayment(event, config)
        }

        // 9. Mark event as permanently processed
        repository.recordProcessedEvent(event, announced = true)

        Log.i(TAG, "Successfully processed and announced payment ${event.paymentId} (₹${event.amountPaise / 100})")
        return PaymentProcessResult.Success(event)
    }
}
