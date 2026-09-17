package com.ashupaybox.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.ashupaybox.data.remote.RemotePaymentEventSource
import com.ashupaybox.domain.processor.PaymentEventProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncManager(
    private val context: Context,
    private val eventSource: RemotePaymentEventSource,
    private val eventProcessor: PaymentEventProcessor,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "SYNC"
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                Log.d(TAG, "Network is ONLINE. Triggering missed payment reconciliation.")
                scope.launch(Dispatchers.IO) {
                    syncMissedPayments()
                }
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                Log.d(TAG, "Network is OFFLINE.")
            }
        })
    }

    /**
     * Synchronizes any transactions missed while the phone was offline or powered off.
     */
    suspend fun syncMissedPayments(): Int {
        if (!_isOnline.value) {
            Log.d(TAG, "Cannot sync: device is offline.")
            return 0
        }

        Log.d(TAG, "PHASE_2_FIREBASE_INTEGRATION: Syncing missed payments from backend API...")
        val lastSyncTimestamp = System.currentTimeMillis() - 86400000L // last 24h
        val result = eventSource.fetchMissedPayments(lastSyncTimestamp)

        var count = 0
        result.onSuccess { missedList ->
            for (event in missedList) {
                eventProcessor.processEvent(event)
                count++
            }
        }
        return count
    }
}
