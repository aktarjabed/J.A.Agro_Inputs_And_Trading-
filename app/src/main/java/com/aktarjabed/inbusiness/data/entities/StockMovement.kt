package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["productId"]),
        Index(value = ["referenceId"])
    ]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "businessId")
    val businessId: String,
    @ColumnInfo(name = "productId")
    val productId: Long,
    @ColumnInfo(name = "movementType")
    val movementType: String, // e.g., "SALE", "SALE_REVERSAL", "MANUAL_ADD", "MANUAL_DEDUCT"
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    @ColumnInfo(name = "stockBefore")
    val stockBefore: Double,
    @ColumnInfo(name = "stockAfter")
    val stockAfter: Double,
    @ColumnInfo(name = "referenceType")
    val referenceType: String, // e.g., "INVOICE", "MANUAL"
    @ColumnInfo(name = "referenceId")
    val referenceId: String, // Invoice ID or other reference
    @ColumnInfo(name = "reason")
    val reason: String = "",
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)
