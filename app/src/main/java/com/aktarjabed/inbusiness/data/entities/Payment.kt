package com.aktarjabed.inbusiness.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["invoiceId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "businessId")
    val businessId: Long,
    @ColumnInfo(name = "invoiceId")
    val invoiceId: String,
    val amount: Double,
    val paymentMode: String = "CASH",
    val paymentDate: Long = System.currentTimeMillis(),
    val referenceNumber: String = "",
    val status: String = "SUCCESS"
)
