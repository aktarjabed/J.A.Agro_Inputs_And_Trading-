package com.aktarjabed.inbusiness.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.repository.DashboardRepository
import com.aktarjabed.inbusiness.data.repository.ChartPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalRevenue: Double = 0.0,
    val todayRevenue: Double = 0.0,
    val pendingDues: Double = 0.0,
    val invoicesToday: Int = 0,
    val activeProducts: Int = 0,
    val lowStockCount: Int = 0,
    val chartData: List<ChartPoint> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _chartData = MutableStateFlow<List<ChartPoint>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeTotalRevenue(),
        repository.observeTodayRevenue(),
        repository.observePendingDues(),
        repository.observeInvoicesTodayCount(),
        repository.observeActiveProductsCount(),
        repository.observeLowStockProductsCount(),
        _chartData,
        _isLoading,
        _error
    ) { flows ->
        DashboardUiState(
            totalRevenue = flows[0] as Double,
            todayRevenue = flows[1] as Double,
            pendingDues = flows[2] as Double,
            invoicesToday = flows[3] as Int,
            activeProducts = flows[4] as Int,
            lowStockCount = flows[5] as Int,
            chartData = flows[6] as List<ChartPoint>,
            isLoading = flows[7] as Boolean,
            error = flows[8] as String?
        )
    }
    .catch { e ->
        _error.value = e.message
        _isLoading.value = false
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val chart = repository.getSevenDayChartData()
                _chartData.value = chart
            } catch (e: Exception) {
                _error.value = "Failed to load chart data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
