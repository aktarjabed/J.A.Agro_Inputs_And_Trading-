package com.aktarjabed.inbusiness.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.dao.InvoiceHistoryFilter
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceHistoryRepository @Inject constructor(
    private val database: AppDatabase,
    private val businessContext: BusinessContext
) {
    suspend fun getFilteredInvoices(filter: InvoiceHistoryFilter): List<Invoice> {
        val businessId = businessContext.activeBusinessId.first()
        val queryBuilder = StringBuilder("SELECT * FROM invoices WHERE businessId = ?")
        val args = mutableListOf<Any>(businessId)

        if (filter.query.isNotBlank()) {
            queryBuilder.append(" AND (invoiceNumber LIKE ? OR customerName LIKE ?)")
            args.add("%${filter.query}%")
            args.add("%${filter.query}%")
        }

        if (filter.startDate != null) {
            queryBuilder.append(" AND createdAt >= ?")
            args.add(filter.startDate)
        }

        if (filter.endDate != null) {
            queryBuilder.append(" AND createdAt <= ?")
            args.add(filter.endDate)
        }

        if (filter.documentType != null) {
            queryBuilder.append(" AND documentType = ?")
            args.add(filter.documentType)
        }

        if (filter.status != null) {
            when (filter.status) {
                "CANCELLED" -> {
                    queryBuilder.append(" AND status = 'CANCELLED'")
                }
                "PAID" -> {
                    queryBuilder.append(" AND status = 'COMPLETED' AND balanceDue <= 0 AND totalAmount > 0")
                }
                "PARTIAL" -> {
                    queryBuilder.append(" AND status = 'COMPLETED' AND balanceDue > 0 AND amountPaid > 0")
                }
                "DUE" -> {
                    queryBuilder.append(" AND status = 'COMPLETED' AND amountPaid <= 0 AND balanceDue > 0")
                }
            }
        }

        queryBuilder.append(" ORDER BY createdAt DESC")

        val sqliteQuery = SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
        return database.invoiceDao().getInvoicesByQuery(sqliteQuery)
    }
}
