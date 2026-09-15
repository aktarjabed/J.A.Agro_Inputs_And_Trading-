package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.InvoiceHistoryFilter
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceHistoryRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val businessContext: BusinessContext
) {
    suspend fun getInvoices(
        search: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        status: String? = null,
        paymentStatus: String? = null,
        documentType: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<Invoice> {
        val businessId = businessContext.activeBusinessId.first()
        val filter = InvoiceHistoryFilter(
            businessId = businessId,
            search = search,
            startDate = startDate,
            endDate = endDate,
            status = status,
            paymentStatus = paymentStatus,
            documentType = documentType,
            limit = limit,
            offset = offset
        )
        return invoiceDao.getInvoicesByQuery(filter.toSQLiteQuery())
    }
}
