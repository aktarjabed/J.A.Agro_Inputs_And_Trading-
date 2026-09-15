package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE businessId = :businessId AND invoiceId = :invoiceId ORDER BY paymentDate DESC")
    fun getPaymentsForInvoice(businessId: String, invoiceId: String): Flow<List<Payment>>

    @Query("SELECT SUM(amount) FROM payments WHERE businessId = :businessId AND invoiceId = :invoiceId")
    suspend fun getTotalPaidForInvoice(businessId: String, invoiceId: String): Double?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: Payment): Long
}
