package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.StockMovementDao
import com.aktarjabed.inbusiness.data.entities.StockMovement
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockMovementRepository @Inject constructor(
    private val stockMovementDao: StockMovementDao,
    private val businessContext: BusinessContext
) {
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovement>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        stockMovementDao.getMovementsForProduct(businessId.toLong(), productId)
    }

    suspend fun getMovementsByReference(referenceType: String, referenceId: String): List<StockMovement> {
        val businessId = businessContext.activeBusinessId.first()
        return stockMovementDao.getMovementsByReference(businessId.toLong(), referenceType, referenceId)
    }

    suspend fun addMovement(movement: StockMovement): Long {
        require(movement.businessId == businessContext.activeBusinessId.first().toLong()) { "Movement must belong to active business" }
        return stockMovementDao.insertMovement(movement)
    }
}
