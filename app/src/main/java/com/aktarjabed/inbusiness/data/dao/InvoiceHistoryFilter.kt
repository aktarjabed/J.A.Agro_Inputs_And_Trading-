package com.aktarjabed.inbusiness.data.dao

data class InvoiceHistoryFilter(
    val query: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val status: String? = null, // PAID, PARTIAL, DUE, CANCELLED
    val documentType: String? = null
)
