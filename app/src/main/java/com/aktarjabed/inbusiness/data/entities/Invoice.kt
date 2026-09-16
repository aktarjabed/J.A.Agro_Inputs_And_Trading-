package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

import androidx.room.Index

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["businessId", "invoiceNumber"], unique = true),
        Index(value = ["businessId", "idempotencyKey"], unique = true)
    ]
)
data class Invoice(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val idempotencyKey: String? = null,
    val requestFingerprint: String? = null,
    val invoiceNumber: String = "",
    val sellerName: String = "",
    val sellerAddress: String = "",
    val sellerGSTIN: String? = null,
    val customerId: String = "",
    val customerName: String = "",
    val customerGSTIN: String? = null,
    val buyerAddress: String = "",
    val subtotal: Double = 0.0,
    val totalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalCgst: Double = 0.0,
    val totalSgst: Double = 0.0,
    val totalIgst: Double = 0.0,
    val supplyType: String = "",
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val paymentMethod: String = "NONE",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val irn: String? = null,
    val ackNo: String? = null,
    val ackDate: Instant? = null,
    val qrCodeData: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "COMPLETED")
    val status: String = "COMPLETED",
    @androidx.room.ColumnInfo(defaultValue = "TAX_INVOICE")
    val documentType: String = "TAX_INVOICE"
)