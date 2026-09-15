package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["invoiceId"])
    ]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "businessId")
    val businessId: String,
    @ColumnInfo(name = "invoiceId")
    val invoiceId: String,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "paymentMethod")
    val paymentMethod: String,
    @ColumnInfo(name = "paymentDate")
    val paymentDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "referenceId")
    val referenceId: String = "",
    @ColumnInfo(name = "notes")
    val notes: String = ""
)
