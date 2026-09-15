package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.CustomerDao
import com.aktarjabed.inbusiness.data.entities.Customer
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val businessContext: BusinessContext
) {
    fun getAllCustomers(): Flow<List<Customer>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        customerDao.getAllCustomers(businessId)
    }

    fun searchCustomers(query: String): Flow<List<Customer>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        customerDao.searchCustomers(businessId, query)
    }

    suspend fun getCustomerByName(name: String): Customer? {
        val businessId = businessContext.activeBusinessId.first()
        return customerDao.getCustomerByName(businessId, name)
    }

    suspend fun getCustomerById(id: Long): Customer? {
        val businessId = businessContext.activeBusinessId.first()
        return customerDao.getCustomerById(businessId, id)
    }

    suspend fun saveCustomer(customer: Customer): Long {
        require(customer.businessId == businessContext.activeBusinessId.first()) { "Customer must belong to active business" }
        return if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }
}
