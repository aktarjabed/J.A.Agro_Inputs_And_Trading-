package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY name ASC")
    fun getAllCustomers(businessId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(businessId: String, query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND name = :name LIMIT 1")
    suspend fun getCustomerByName(businessId: String, name: String): Customer?

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND id = :id LIMIT 1")
    suspend fun getCustomerById(businessId: String, id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer): Int
}
