package com.ashupaybox.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ashupaybox.PayBoxApp
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.preferences.PayBoxPreferences
import com.ashupaybox.data.repository.PaymentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HourlyPoint(
    val hour: Int,
    val label: String,
    val amountPaise: Long,
    val count: Int
)

data class DailyPoint(
    val dayLabel: String,
    val dateLabel: String,
    val amountPaise: Long,
    val count: Int
)

data class MethodBreakdown(
    val method: PaymentMethod,
    val amountPaise: Long,
    val count: Int,
    val percentage: Float
)

data class AnalyticsUiState(
    val todayTotalPaise: Long = 0L,
    val todayTxCount: Int = 0,
    val todayAvgPaise: Long = 0L,
    val todayMaxPaise: Long = 0L,
    val hourlyPoints: List<HourlyPoint> = emptyList(),
    val dailyPoints: List<DailyPoint> = emptyList(),
    val methodBreakdowns: List<MethodBreakdown> = emptyList(),
    val maxHourlyPaise: Long = 1L,
    val maxDailyPaise: Long = 1L
)

class AnalyticsViewModel(
    private val repository: PaymentRepository,
    private val preferences: PayBoxPreferences
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllTransactions(true),
        preferences.configFlow
    ) { allTx, config ->
        val includeTest = config.includeTestInRevenue
        val validTx = allTx.filter { (includeTest || !it.isTest) && it.status.isSuccessful }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        val todayTx = validTx.filter { it.timestamp in todayStart..now }
        val todayTotal = todayTx.sumOf { it.amountPaise }
        val todayCount = todayTx.size
        val todayAvg = if (todayCount > 0) todayTotal / todayCount else 0L
        val todayMax = todayTx.maxOfOrNull { it.amountPaise } ?: 0L

        // 1. Hourly breakdown (0 to 23)
        val hourlyMap = mutableMapOf<Int, MutableList<PaymentEvent>>()
        for (i in 0..23) hourlyMap[i] = mutableListOf()
        for (tx in todayTx) {
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val hour = txCal.get(Calendar.HOUR_OF_DAY)
            hourlyMap[hour]?.add(tx)
        }
        val hourlyList = (0..23).map { hour ->
            val txs = hourlyMap[hour] ?: emptyList()
            val total = txs.sumOf { it.amountPaise }
            HourlyPoint(
                hour = hour,
                label = String.format(Locale.US, "%02d:00", hour),
                amountPaise = total,
                count = txs.size
            )
        }
        val maxHourly = (hourlyList.maxOfOrNull { it.amountPaise } ?: 0L).coerceAtLeast(1000L)

        // 2. Last 7 days breakdown
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val dateFormat = SimpleDateFormat("dd MMM", Locale.US)
        val dailyList = (6 downTo 0).map { daysAgo ->
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = dayCal.timeInMillis
            val end = start + 86400000L

            val dayTxs = validTx.filter { it.timestamp in start until end }
            val total = dayTxs.sumOf { it.amountPaise }
            DailyPoint(
                dayLabel = if (daysAgo == 0) "Today" else dayFormat.format(Date(start)),
                dateLabel = dateFormat.format(Date(start)),
                amountPaise = total,
                count = dayTxs.size
            )
        }
        val maxDaily = (dailyList.maxOfOrNull { it.amountPaise } ?: 0L).coerceAtLeast(1000L)

        // 3. Payment method distribution
        val totalMethodPaise = validTx.sumOf { it.amountPaise }
        val methodList = PaymentMethod.entries.map { method ->
            val txs = validTx.filter { it.paymentMethod == method }
            val total = txs.sumOf { it.amountPaise }
            val pct = if (totalMethodPaise > 0) (total.toFloat() / totalMethodPaise.toFloat()) * 100f else 0f
            MethodBreakdown(
                method = method,
                amountPaise = total,
                count = txs.size,
                percentage = pct
            )
        }.filter { it.count > 0 || it.method == PaymentMethod.UPI }

        AnalyticsUiState(
            todayTotalPaise = todayTotal,
            todayTxCount = todayCount,
            todayAvgPaise = todayAvg,
            todayMaxPaise = todayMax,
            hourlyPoints = hourlyList,
            dailyPoints = dailyList,
            methodBreakdowns = methodList,
            maxHourlyPaise = maxHourly,
            maxDailyPaise = maxDaily
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    companion object {
        fun factory(app: PayBoxApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AnalyticsViewModel(
                    repository = app.repository,
                    preferences = app.preferences
                ) as T
            }
        }
    }
}
