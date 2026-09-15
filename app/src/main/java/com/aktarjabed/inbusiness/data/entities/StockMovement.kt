package com.aktarjabed.inbusiness.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["productId"]),
        Index(value = ["referenceId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "businessId")
    val businessId: Long,
    @ColumnInfo(name = "productId")
    val productId: Long,
    @ColumnInfo(name = "movementType")
    val movementType: String,
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    @ColumnInfo(name = "stockBefore")
    val stockBefore: Double,
    @ColumnInfo(name = "stockAfter")
    val stockAfter: Double,
    @ColumnInfo(name = "referenceType")
    val referenceType: String,
    @ColumnInfo(name = "referenceId")
    val referenceId: String,
    @ColumnInfo(name = "reason")
    val reason: String = "",
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)
