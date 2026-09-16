package com.aktarjabed.inbusiness.presentation.screens.invoice_history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.repository.InvoiceHistoryRepository
import com.aktarjabed.inbusiness.utils.AppDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InvoiceHistoryUiState(
    val invoices: List<Invoice> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null, // Inclusive UI perspective
    val status: String? = "COMPLETED", // Default
    val paymentStatus: String? = null,
    val documentType: String? = null,
    val isEndOfList: Boolean = false
)

@HiltViewModel
class InvoiceHistoryViewModel @Inject constructor(
    private val repository: InvoiceHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceHistoryUiState())
    val uiState: StateFlow<InvoiceHistoryUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 50

    init {
        loadInvoices(reset = true)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadInvoices(reset = true)
    }

    fun onDateRangeSelected(start: LocalDate?, end: LocalDate?) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
        loadInvoices(reset = true)
    }

    fun onStatusChanged(status: String?) {
        _uiState.update { it.copy(status = status) }
        loadInvoices(reset = true)
    }

    fun onPaymentStatusChanged(paymentStatus: String?) {
        _uiState.update { it.copy(paymentStatus = paymentStatus) }
        loadInvoices(reset = true)
    }

    fun loadMore() {
        if (!_uiState.value.isLoading && !_uiState.value.isEndOfList) {
            loadInvoices(reset = false)
        }
    }

    fun loadInvoices(reset: Boolean = false) {
        if (reset) {
            currentOffset = 0
            _uiState.update { it.copy(invoices = emptyList(), isEndOfList = false) }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _uiState.value
                val startMillis = state.startDate?.let { AppDateUtils.getStartOfDay(it) }
                // User's endDate is inclusive. E.g. Sep 16 -> We want < Sep 17 00:00.
                val endMillis = state.endDate?.let { AppDateUtils.getStartOfNextDay(it) }

                val newInvoices = repository.getInvoices(
                    search = state.searchQuery,
                    startDate = startMillis,
                    endDate = endMillis,
                    status = state.status,
                    paymentStatus = state.paymentStatus,
                    documentType = state.documentType,
                    limit = limit,
                    offset = currentOffset
                )

                _uiState.update {
                    it.copy(
                        invoices = if (reset) newInvoices else it.invoices + newInvoices,
                        isEndOfList = newInvoices.size < limit,
                        isLoading = false
                    )
                }
                currentOffset += newInvoices.size

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
