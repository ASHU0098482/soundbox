package com.ashupaybox.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ashupaybox.PayBoxApp
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.preferences.PayBoxPreferences
import com.ashupaybox.core.preferences.SoundBoxConfig
import com.ashupaybox.data.repository.PaymentRepository
import com.ashupaybox.domain.processor.PaymentEventProcessor
import com.ashupaybox.sync.SyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val isOnline: Boolean = true,
    val config: SoundBoxConfig = SoundBoxConfig(),
    val todayRevenuePaise: Long = 0L,
    val todayPaymentCount: Int = 0,
    val todayAveragePaise: Long = 0L,
    val todayHighestPaise: Long = 0L,
    val yesterdayRevenuePaise: Long = 0L,
    val weekRevenuePaise: Long = 0L,
    val monthRevenuePaise: Long = 0L,
    val totalReceivedPaise: Long = 0L,
    val recentTransactions: List<PaymentEvent> = emptyList(),
    val livePayment: PaymentEvent? = null
) {
    val percentageVsYesterday: Int
        get() {
            if (yesterdayRevenuePaise == 0L) {
                return if (todayRevenuePaise > 0) 100 else 0
            }
            val diff = todayRevenuePaise - yesterdayRevenuePaise
            return ((diff.toDouble() / yesterdayRevenuePaise.toDouble()) * 100).toInt()
        }
}

class HomeViewModel(
    private val repository: PaymentRepository,
    private val eventProcessor: PaymentEventProcessor,
    private val preferences: PayBoxPreferences,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _livePayment = MutableStateFlow<PaymentEvent?>(null)
    private var dismissJob: Job? = null

    init {
        // Collect live events to trigger the animated live payment card
        viewModelScope.launch {
            eventProcessor.livePaymentEvents.collect { event ->
                _livePayment.value = event
                dismissJob?.cancel()
                dismissJob = launch {
                    delay(6000) // Show card for 6 seconds
                    _livePayment.value = null
                }
            }
        }
    }

    fun dismissLiveCard() {
        dismissJob?.cancel()
        _livePayment.value = null
    }

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getYesterdayStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getWeekStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getMonthStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    val uiState: StateFlow<HomeUiState> = combine(
        syncManager.isOnline,
        preferences.configFlow,
        _livePayment
    ) { isOnline, config, livePayment ->
        Triple(isOnline, config, livePayment)
    }.combine(repository.getAllTransactions(true)) { (isOnline, config, livePayment), allTx ->
        val now = System.currentTimeMillis()
        val todayStart = getTodayStartMillis()
        val yesterdayStart = getYesterdayStartMillis()
        val weekStart = getWeekStartMillis()
        val monthStart = getMonthStartMillis()

        val includeTest = config.includeTestInRevenue

        val validTx = allTx.filter { (includeTest || !it.isTest) && it.status.isSuccessful }

        val todayTx = validTx.filter { it.timestamp in todayStart..now }
        val yesterdayTx = validTx.filter { it.timestamp in yesterdayStart until todayStart }
        val weekTx = validTx.filter { it.timestamp in weekStart..now }
        val monthTx = validTx.filter { it.timestamp in monthStart..now }

        val todayRevenue = todayTx.sumOf { it.amountPaise }
        val todayCount = todayTx.size
        val todayAvg = if (todayCount > 0) todayRevenue / todayCount else 0L
        val todayMax = todayTx.maxOfOrNull { it.amountPaise } ?: 0L

        val yesterdayRevenue = yesterdayTx.sumOf { it.amountPaise }
        val weekRevenue = weekTx.sumOf { it.amountPaise }
        val monthRevenue = monthTx.sumOf { it.amountPaise }
        val totalRevenue = validTx.sumOf { it.amountPaise }

        val recent = allTx.filter { includeTest || !it.isTest }.take(5)

        HomeUiState(
            isOnline = isOnline,
            config = config,
            todayRevenuePaise = todayRevenue,
            todayPaymentCount = todayCount,
            todayAveragePaise = todayAvg,
            todayHighestPaise = todayMax,
            yesterdayRevenuePaise = yesterdayRevenue,
            weekRevenuePaise = weekRevenue,
            monthRevenuePaise = monthRevenue,
            totalReceivedPaise = totalRevenue,
            recentTransactions = recent,
            livePayment = livePayment
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    companion object {
        fun factory(app: PayBoxApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    repository = app.repository,
                    eventProcessor = app.eventProcessor,
                    preferences = app.preferences,
                    syncManager = app.syncManager
                ) as T
            }
        }
    }
}
