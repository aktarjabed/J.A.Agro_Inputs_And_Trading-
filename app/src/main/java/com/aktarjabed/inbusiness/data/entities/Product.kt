package com.aktarjabed.inbusiness.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(
            value = ["businessId", "name", "brand", "category", "unitType", "batchNumber"],
            unique = true
        )
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val businessId: String,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val brand: String,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val category: String,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val unitType: String,

    val pricePerUnit: Double,
    val availableStock: Double,
    val batchNumber: String = "",
    val isWholesaleOnly: Boolean = false,
    val gstPercentage: Double = 0.0,
    @androidx.room.ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true,
    @androidx.room.ColumnInfo(defaultValue = "0.0")
    val reorderThreshold: Double = 0.0,
    @androidx.room.ColumnInfo(defaultValue = "")
    val hsnSac: String = "",
    @androidx.room.ColumnInfo(defaultValue = "")
    val uqc: String = ""
)
