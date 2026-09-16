package com.aktarjabed.inbusiness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aktarjabed.inbusiness.data.entities.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: Payment): Long

    @Query("SELECT SUM(amount) FROM payments WHERE invoiceId = :invoiceId AND businessId = :businessId AND status = 'SUCCESS'")
    suspend fun getTotalPaidForInvoice(businessId: Long, invoiceId: String): Double?

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId AND businessId = :businessId ORDER BY paymentDate DESC")
    fun getPaymentsForInvoice(businessId: Long, invoiceId: String): Flow<List<Payment>>
}
