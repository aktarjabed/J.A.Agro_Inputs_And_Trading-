package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.PaymentDao
import com.aktarjabed.inbusiness.data.entities.Payment
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao,
    private val businessContext: BusinessContext
) {
    fun getPaymentsForInvoice(invoiceId: String): Flow<List<Payment>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        paymentDao.getPaymentsForInvoice(businessId, invoiceId)
    }

    suspend fun getTotalPaidForInvoice(invoiceId: String): Double {
        val businessId = businessContext.activeBusinessId.first()
        return paymentDao.getTotalPaidForInvoice(businessId, invoiceId) ?: 0.0
    }

    suspend fun addPayment(payment: Payment): Long {
        require(payment.businessId == businessContext.activeBusinessId.first()) { "Payment must belong to active business" }
        return paymentDao.insertPayment(payment)
    }
}
