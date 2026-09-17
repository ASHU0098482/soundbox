package com.ashupaybox.data.remote

import com.ashupaybox.core.model.PaymentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Abstraction for remote incoming payment events.
 *
 * PHASE_2_FIREBASE_INTEGRATION:
 * In Phase 2, this will be implemented by [FirebasePaymentEventSource] which listens to
 * high-priority FCM data messages containing signed/verified payment metadata from our backend.
 */
interface RemotePaymentEventSource {
    val incomingEvents: Flow<PaymentEvent>

    suspend fun registerDevice(fcmToken: String): Result<Boolean>
    suspend fun fetchMissedPayments(lastSyncTimestamp: Long): Result<List<PaymentEvent>>
}

/**
 * Phase 1 implementation of [RemotePaymentEventSource].
 * Uses an in-memory SharedFlow allowing the local simulator to feed events into the exact
 * same pipeline that Firebase will use in Phase 2.
 */
class FakePaymentEventSource : RemotePaymentEventSource {

    private val _incomingEvents = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 64)
    override val incomingEvents: Flow<PaymentEvent> = _incomingEvents.asSharedFlow()

    suspend fun emitSimulatedEvent(event: PaymentEvent) {
        _incomingEvents.emit(event)
    }

    override suspend fun registerDevice(fcmToken: String): Result<Boolean> {
        // PHASE_2_FIREBASE_INTEGRATION: Connect to backend /api/v1/devices/register
        return Result.success(true)
    }

    override suspend fun fetchMissedPayments(lastSyncTimestamp: Long): Result<List<PaymentEvent>> {
        // PHASE_2_FIREBASE_INTEGRATION: Connect to backend /api/v1/payments/sync
        return Result.success(emptyList())
    }
}
