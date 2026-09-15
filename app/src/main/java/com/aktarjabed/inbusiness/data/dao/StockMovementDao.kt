package com.aktarjabed.inbusiness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aktarjabed.inbusiness.data.entities.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: StockMovement): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND businessId = :businessId ORDER BY createdAt DESC")
    fun getMovementsForProduct(businessId: Long, productId: Long): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE referenceType = :referenceType AND referenceId = :referenceId AND businessId = :businessId")
    suspend fun getMovementsByReference(businessId: Long, referenceType: String, referenceId: String): List<StockMovement>
}
