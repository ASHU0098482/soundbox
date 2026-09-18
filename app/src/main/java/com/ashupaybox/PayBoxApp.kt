package com.ashupaybox

import android.app.Application
import com.ashupaybox.core.preferences.PayBoxPreferences
import com.ashupaybox.data.local.PayBoxDatabase
import com.ashupaybox.data.remote.FakePaymentEventSource
import com.ashupaybox.data.repository.PaymentRepository
import com.ashupaybox.domain.processor.PaymentEventProcessor
import com.ashupaybox.notification.PaymentNotificationManager
import com.ashupaybox.sound.PaymentAnnouncementManager
import com.ashupaybox.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PayBoxApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: PayBoxDatabase
        private set

    lateinit var repository: PaymentRepository
        private set

    lateinit var preferences: PayBoxPreferences
        private set

    lateinit var notificationManager: PaymentNotificationManager
        private set

    lateinit var announcementManager: PaymentAnnouncementManager
        private set

    lateinit var eventProcessor: PaymentEventProcessor
        private set

    lateinit var remoteSource: FakePaymentEventSource
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var updateManager: com.ashupaybox.core.update.UpdateManager
        private set

    lateinit var fcmTokenManager: com.ashupaybox.core.fcm.FcmTokenManager
        private set

    lateinit var deviceRegistrationRepository: com.ashupaybox.data.repository.DeviceRegistrationRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = PayBoxDatabase.getInstance(this)
        repository = PaymentRepository(database)
        preferences = PayBoxPreferences(this)
        notificationManager = PaymentNotificationManager(this)
        announcementManager = PaymentAnnouncementManager(this, appScope)

        eventProcessor = PaymentEventProcessor(
            repository = repository,
            notificationManager = notificationManager,
            announcementManager = announcementManager,
            preferences = preferences,
            scope = appScope
        )

        remoteSource = FakePaymentEventSource()

        syncManager = SyncManager(
            context = this,
            eventSource = remoteSource,
            eventProcessor = eventProcessor,
            scope = appScope
        )

        updateManager = com.ashupaybox.core.update.UpdateManager(
            context = this,
            preferences = preferences,
            scope = appScope
        )

        fcmTokenManager = com.ashupaybox.core.fcm.FcmTokenManager(this, appScope)
        deviceRegistrationRepository = com.ashupaybox.data.repository.DeviceRegistrationRepository(this, fcmTokenManager)

        // Initialize FCM token non-blockingly
        fcmTokenManager.initialize()

        // Register this device with the Phase 3 backend whenever Firebase issues a token.
        appScope.launch {
            fcmTokenManager.fcmTokenFlow.collect { token ->
                if (!token.isNullOrBlank()) {
                    deviceRegistrationRepository.syncWithBackend()
                }
            }
        }

        // Asynchronous non-blocking update check on startup
        updateManager.checkForUpdates(isManual = false)

        // Listen for events from remote source
        appScope.launch {
            remoteSource.incomingEvents.collect { event ->
                eventProcessor.processEvent(event)
            }
        }

        // Initialize demo seed data in Phase 1 if DB is empty
        appScope.launch {
            repository.seedDemoTransactions()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        announcementManager.destroy()
    }
}
