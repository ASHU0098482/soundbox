package com.ashupaybox.presentation.testpayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ashupaybox.PayBoxApp
import com.ashupaybox.core.model.PaymentEvent
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.model.PaymentSource
import com.ashupaybox.core.model.PaymentStatus
import com.ashupaybox.domain.processor.PaymentEventProcessor
import com.ashupaybox.domain.processor.PaymentProcessResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class TestPaymentUiState(
    val amountRupeesInput: String = "300",
    val customerName: String = "",
    val selectedMethod: PaymentMethod = PaymentMethod.UPI,
    val selectedStatus: PaymentStatus = PaymentStatus.CAPTURED,
    val lastResult: PaymentProcessResult? = null,
    val isSimulating: Boolean = false,
    val statusMessage: String? = null
)

class TestPaymentViewModel(
    private val eventProcessor: PaymentEventProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestPaymentUiState())
    val uiState: StateFlow<TestPaymentUiState> = _uiState.asStateFlow()

    fun onAmountChanged(amount: String) {
        val clean = amount.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(amountRupeesInput = clean)
    }

    fun onQuickAmountSelected(rupees: Long) {
        _uiState.value = _uiState.value.copy(amountRupeesInput = rupees.toString())
    }

    fun onCustomerNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
    }

    fun onMethodSelected(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(selectedMethod = method)
    }

    fun onStatusSelected(status: PaymentStatus) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
    }

    fun simulatePayment() {
        val state = _uiState.value
        val amountRupees = state.amountRupeesInput.toLongOrNull() ?: return
        if (amountRupees <= 0) return

        val amountPaise = amountRupees * 100L
        val randomSuffix = UUID.randomUUID().toString().substring(0, 8)
        val eventId = "sim_ev_$randomSuffix"
        val paymentId = "pay_sim_$randomSuffix"
        val orderId = "order_sim_$randomSuffix"

        val event = PaymentEvent(
            eventId = eventId,
            paymentId = paymentId,
            orderId = orderId,
            amountPaise = amountPaise,
            status = state.selectedStatus,
            paymentMethod = state.selectedMethod,
            payerName = state.customerName.takeIf { it.isNotBlank() } ?: "Simulated Merchant Customer",
            payerVpaMasked = if (state.selectedMethod == PaymentMethod.UPI) "sim***@upi" else null,
            source = PaymentSource.LOCAL_TEST,
            isTest = true
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSimulating = true, statusMessage = "Processing payment event...")
            val result = eventProcessor.processEvent(event)
            _uiState.value = _uiState.value.copy(
                isSimulating = false,
                lastResult = result,
                statusMessage = when (result) {
                    is PaymentProcessResult.Success -> "Success! Received ₹$amountRupees via ${event.paymentMethod.displayName}"
                    is PaymentProcessResult.DuplicateIgnored -> "Duplicate Event Ignored: ${result.eventId}"
                    is PaymentProcessResult.StatusIgnored -> "Status ${result.status}: Saved without announcement"
                    is PaymentProcessResult.Error -> "Error: ${result.message}"
                }
            )
        }
    }

    fun simulateDuplicateTest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSimulating = true, statusMessage = "Testing Duplicate Protection...")
            val testId = "dup_test_${UUID.randomUUID().toString().substring(0, 8)}"
            val event = PaymentEvent(
                eventId = testId,
                paymentId = "pay_$testId",
                orderId = "order_$testId",
                amountPaise = 50000L, // ₹500
                status = PaymentStatus.CAPTURED,
                paymentMethod = PaymentMethod.UPI,
                payerName = "Duplicate Test Payer",
                source = PaymentSource.LOCAL_TEST,
                isTest = true
            )

            // First send
            val firstResult = eventProcessor.processEvent(event)
            // Second send with EXACT SAME eventId
            val secondResult = eventProcessor.processEvent(event)

            _uiState.value = _uiState.value.copy(
                isSimulating = false,
                lastResult = secondResult,
                statusMessage = if (secondResult is PaymentProcessResult.DuplicateIgnored) {
                    "Duplicate Protection Verified! Second identical event was safely rejected."
                } else {
                    "Duplicate test outcome: $secondResult"
                }
            )
        }
    }

    fun simulateRapidFire() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSimulating = true, statusMessage = "Simulating 3 rapid payments...")
            val amounts = listOf(35L, 100L, 250L)
            for (amt in amounts) {
                val suffix = UUID.randomUUID().toString().substring(0, 6)
                val event = PaymentEvent(
                    eventId = "rapid_ev_$suffix",
                    paymentId = "pay_rapid_$suffix",
                    orderId = "order_rapid_$suffix",
                    amountPaise = amt * 100L,
                    status = PaymentStatus.CAPTURED,
                    paymentMethod = PaymentMethod.UPI,
                    payerName = "Customer ₹$amt",
                    source = PaymentSource.LOCAL_TEST,
                    isTest = true
                )
                eventProcessor.processEvent(event)
                delay(1200) // Stagger slightly
            }
            _uiState.value = _uiState.value.copy(
                isSimulating = false,
                statusMessage = "Dispatched 3 queued payments successfully!"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    companion object {
        fun factory(app: PayBoxApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TestPaymentViewModel(app.eventProcessor) as T
            }
        }
    }
}
