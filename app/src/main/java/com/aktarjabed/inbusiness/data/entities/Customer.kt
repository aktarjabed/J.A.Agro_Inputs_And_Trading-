package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["businessId", "name"], unique = true)
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "businessId")
    val businessId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "gstin")
    val gstin: String = "",
    @ColumnInfo(name = "address")
    val address: String = "",
    @ColumnInfo(name = "phone")
    val phone: String = "",
    @ColumnInfo(name = "email")
    val email: String = "",
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)
