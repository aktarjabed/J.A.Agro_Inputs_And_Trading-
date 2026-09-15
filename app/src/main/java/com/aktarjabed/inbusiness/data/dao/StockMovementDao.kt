package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE businessId = :businessId AND productId = :productId ORDER BY createdAt DESC")
    fun getMovementsForProduct(businessId: String, productId: Long): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE businessId = :businessId AND referenceType = :referenceType AND referenceId = :referenceId")
    suspend fun getMovementsByReference(businessId: String, referenceType: String, referenceId: String): List<StockMovement>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: StockMovement): Long
}
