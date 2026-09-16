package com.aktarjabed.inbusiness.data.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

data class InvoiceHistoryFilter(
    val businessId: String,
    val search: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val status: String? = null,
    val paymentStatus: String? = null,
    val documentType: String? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    fun toSQLiteQuery(): SupportSQLiteQuery {
        var queryString = "SELECT * FROM invoices WHERE businessId = ?"
        val bindArgs = mutableListOf<Any>(businessId)

        if (!search.isNullOrBlank()) {
            queryString += " AND (invoiceNumber LIKE '%' || ? || '%' OR customerName LIKE '%' || ? || '%')"
            bindArgs.add(search)
            bindArgs.add(search)
        }

        if (startDate != null) {
            queryString += " AND createdAt >= ?"
            bindArgs.add(startDate)
        }

        if (endDate != null) {
            // endDate is assumed to be start-of-next-day (exclusive) as per requirements
            queryString += " AND createdAt < ?"
            bindArgs.add(endDate)
        }

        if (status != null) {
            queryString += " AND status = ?"
            bindArgs.add(status)
        }

        if (paymentStatus != null) {
            if (paymentStatus == "PAID") {
                queryString += " AND balanceDue <= 0 AND status = 'COMPLETED'"
            } else if (paymentStatus == "UNPAID") {
                queryString += " AND balanceDue > 0 AND status = 'COMPLETED'"
            }
        }

        if (documentType != null) {
            queryString += " AND documentType = ?"
            bindArgs.add(documentType)
        }

        queryString += " ORDER BY createdAt DESC LIMIT ? OFFSET ?"
        bindArgs.add(limit)
        bindArgs.add(offset)

        return SimpleSQLiteQuery(queryString, bindArgs.toTypedArray())
    }
}
