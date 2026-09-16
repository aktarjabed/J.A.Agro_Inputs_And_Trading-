package com.aktarjabed.inbusiness.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["businessId", "name"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = BusinessData::class,
            parentColumns = ["id"],
            childColumns = ["businessId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "businessId")
    val businessId: Long,
    val name: String,
    val address: String = "",
    val gstin: String = "",
    val phone: String = "",
    val isActive: Boolean = true
)
