package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.DashboardDao
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.utils.AppDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class ChartPoint(
    val date: LocalDate,
    val revenue: Double
)

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardDao: DashboardDao,
    private val businessContext: BusinessContext
) {
    fun observeTotalRevenue(): Flow<Double> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        dashboardDao.observeTotalRevenue(businessId.toLong())
    }

    fun observeTodayRevenue(): Flow<Double> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        val startOfDay = AppDateUtils.getTodayStart()
        val startOfNextDay = AppDateUtils.getTomorrowStart()
        dashboardDao.observeTodayRevenue(businessId.toLong(), startOfDay, startOfNextDay)
    }

    fun observePendingDues(): Flow<Double> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        dashboardDao.observePendingDues(businessId.toLong())
    }

    fun observeInvoicesTodayCount(): Flow<Int> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        val startOfDay = AppDateUtils.getTodayStart()
        val startOfNextDay = AppDateUtils.getTomorrowStart()
        dashboardDao.getInvoicesTodayCount(businessId.toLong(), startOfDay, startOfNextDay)
    }

    fun observeActiveProductsCount(): Flow<Int> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        dashboardDao.observeActiveProductsCount(businessId.toLong())
    }

    fun observeLowStockProductsCount(): Flow<Int> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        dashboardDao.observeLowStockProductsCount(businessId.toLong())
    }

    suspend fun getSevenDayChartData(): List<ChartPoint> = withContext(Dispatchers.IO) {
        val businessId = businessContext.activeBusinessId.first().toLong()
        val pastSevenDays = AppDateUtils.getPastSevenDays()
        val startTime = AppDateUtils.getStartOfDay(pastSevenDays.first()) // 6 days ago start of day

        val rawData = dashboardDao.getRevenueForChartRaw(businessId, startTime)

        // Group by local date
        val aggregatedMap = rawData.groupBy { raw ->
            Instant.ofEpochMilli(raw.createdAt).atZone(AppDateUtils.businessZoneId).toLocalDate()
        }.mapValues { entry ->
            entry.value.sumOf { it.totalAmount }
        }

        // Map strictly to the 7 consecutive dates, zero-filling missing dates
        pastSevenDays.map { date ->
            ChartPoint(
                date = date,
                revenue = aggregatedMap[date] ?: 0.0
            )
        }
    }
}
