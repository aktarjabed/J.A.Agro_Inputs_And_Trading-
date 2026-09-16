package com.aktarjabed.inbusiness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aktarjabed.inbusiness.data.entities.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND isActive = 1 ORDER BY name ASC")
    fun getAllCustomers(businessId: Long): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND name LIKE '%' || :query || '%' AND isActive = 1 ORDER BY name ASC")
    fun searchCustomers(businessId: Long, query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id AND businessId = :businessId")
    suspend fun getCustomerById(businessId: Long, id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name = :name AND businessId = :businessId LIMIT 1")
    suspend fun getCustomerByName(businessId: Long, name: String): Customer?
}
