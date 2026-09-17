package com.ashupaybox.presentation.transactions

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ashupaybox.PayBoxApp
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.model.PaymentStatus
import com.ashupaybox.core.preferences.PayBoxPreferences
import com.ashupaybox.core.util.CsvExporter
import com.ashupaybox.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilter(val displayName: String) {
    ALL("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days")
}

data class TransactionsUiState(
    val transactions: List<PaymentEvent> = emptyList(),
    val searchQuery: String = "",
    val selectedDateFilter: DateFilter = DateFilter.ALL,
    val selectedMethod: PaymentMethod? = null,
    val selectedStatus: PaymentStatus? = null,
    val selectedTransaction: PaymentEvent? = null,
    val exportUri: Uri? = null,
    val isLoading: Boolean = false
)

class TransactionsViewModel(
    private val repository: PaymentRepository,
    private val preferences: PayBoxPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    private val _methodFilter = MutableStateFlow<PaymentMethod?>(null)
    private val _statusFilter = MutableStateFlow<PaymentStatus?>(null)
    private val _selectedTransaction = MutableStateFlow<PaymentEvent?>(null)
    private val _exportUri = MutableStateFlow<Uri?>(null)

    private val filterCriteria = combine(
        _searchQuery,
        _dateFilter,
        _methodFilter,
        _statusFilter,
        preferences.configFlow
    ) { query, dateFilter, method, status, config ->
        FilterParams(query, dateFilter, method, status, config.includeTestInRevenue)
    }

    private val transactionListFlow = filterCriteria.flatMapLatest { params ->
        val (start, end) = calculateTimeRange(params.dateFilter)
        repository.filterTransactions(
            searchQuery = params.query.trim().takeIf { it.isNotBlank() },
            method = params.method,
            status = params.status,
            startMillis = start,
            endMillis = end,
            includeTest = params.includeTest
        )
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionListFlow,
        filterCriteria,
        _selectedTransaction,
        _exportUri
    ) { list, params, selectedTx, exportUri ->
        TransactionsUiState(
            transactions = list,
            searchQuery = params.query,
            selectedDateFilter = params.dateFilter,
            selectedMethod = params.method,
            selectedStatus = params.status,
            selectedTransaction = selectedTx,
            exportUri = exportUri,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(isLoading = true)
    )

    private fun calculateTimeRange(filter: DateFilter): Pair<Long?, Long?> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        return when (filter) {
            DateFilter.ALL -> Pair(null, null)
            DateFilter.TODAY -> Pair(todayStart, now + 1000)
            DateFilter.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStart = cal.timeInMillis
                Pair(yesterdayStart, todayStart)
            }
            DateFilter.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                Pair(cal.timeInMillis, now + 1000)
            }
            DateFilter.LAST_30_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -29)
                Pair(cal.timeInMillis, now + 1000)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onDateFilterSelected(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun onMethodFilterSelected(method: PaymentMethod?) {
        _methodFilter.value = method
    }

    fun onStatusFilterSelected(status: PaymentStatus?) {
        _statusFilter.value = status
    }

    fun selectTransaction(event: PaymentEvent?) {
        _selectedTransaction.value = event
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val transactionsToExport = uiState.value.transactions
            val uri = CsvExporter.exportTransactionsToCsv(context, transactionsToExport)
            _exportUri.value = uri
        }
    }

    fun clearExportUri() {
        _exportUri.value = null
    }

    private data class FilterParams(
        val query: String,
        val dateFilter: DateFilter,
        val method: PaymentMethod?,
        val status: PaymentStatus?,
        val includeTest: Boolean
    )

    companion object {
        fun factory(app: PayBoxApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TransactionsViewModel(
                    repository = app.repository,
                    preferences = app.preferences
                ) as T
            }
        }
    }
}
