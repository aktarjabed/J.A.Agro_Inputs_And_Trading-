package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.data.entities.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class ProductDaoIsolationTest {
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productDao = db.productDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testBusinessIsolation() = runBlocking {
        // Given products for two different businesses
        val productA = Product(businessId = "biz-A", name = "Prod 1", brand = "Brand", category = "Cat", unitType = "kg", pricePerUnit = 10.0, availableStock = 100.0)
        val productB = Product(businessId = "biz-B", name = "Prod 2", brand = "Brand", category = "Cat", unitType = "kg", pricePerUnit = 20.0, availableStock = 200.0)

        productDao.insertProduct(productA)
        productDao.insertProduct(productB)

        // When reading for business A
        val productsA = productDao.getAllProducts("biz-A").first()

        // Then it should only see business A's products
        assertEquals(1, productsA.size)
        assertEquals("Prod 1", productsA[0].name)
    }

    @Test
    fun testAtomicStockDeductionSuccess() = runBlocking {
        val productId = productDao.insertProduct(
            Product(businessId = "1L", name = "Prod 1", brand = "B", category = "C", unitType = "U", pricePerUnit = 10.0, availableStock = 50.0)
        )

        val affectedRows = productDao.deductStock(productId, "biz-1", 10.0)
        assertEquals(1, affectedRows)

        val updatedProduct = productDao.getProductById(productId, "biz-1")
        assertNotNull(updatedProduct)
        assertEquals(40.0, updatedProduct!!.availableStock)
    }

    @Test
    fun testAtomicStockDeductionFailsOnInsufficientStock() = runBlocking {
        val productId = productDao.insertProduct(
            Product(businessId = "1L", name = "Prod 1", brand = "B", category = "C", unitType = "U", pricePerUnit = 10.0, availableStock = 5.0)
        )

        val affectedRows = productDao.deductStock(productId, "biz-1", 10.0)
        assertEquals(0, affectedRows)

        val updatedProduct = productDao.getProductById(productId, "biz-1")
        assertNotNull(updatedProduct)
        assertEquals(5.0, updatedProduct!!.availableStock) // Unchanged
    }

    @Test
    fun testAtomicStockDeductionFailsOnWrongBusinessId() = runBlocking {
        val productId = productDao.insertProduct(
            Product(businessId = "1L", name = "Prod 1", brand = "B", category = "C", unitType = "U", pricePerUnit = 10.0, availableStock = 50.0)
        )

        val affectedRows = productDao.deductStock(productId, "biz-2", 10.0)
        assertEquals(0, affectedRows)

        val updatedProduct = productDao.getProductById(productId, "biz-1")
        assertNotNull(updatedProduct)
        assertEquals(50.0, updatedProduct!!.availableStock) // Unchanged
    }

    @Test
    fun testCrossBusinessUpdateFails() = runBlocking {
        val productId = productDao.insertProduct(
            Product(businessId = "1L", name = "Original", brand = "B", category = "C", unitType = "U", pricePerUnit = 10.0, availableStock = 50.0)
        )

        val affectedRows = productDao.updateProduct(
            id = productId,
            businessId = "2L", // Wrong business ID
            name = "Hacked",
            brand = "B", category = "C", unitType = "U", pricePerUnit = 10.0, availableStock = 50.0, batchNumber = "", isWholesaleOnly = false, gstPercentage = 0.0
        )

        assertEquals(0, affectedRows) // Update should fail

        val product = productDao.getProductById(productId, "biz-1")
        assertNotNull(product)
        assertEquals("Original", product!!.name) // Verify not changed
    }

    @Test
    fun testCrossBusinessDeleteFails() = runBlocking {
        val productId = productDao.insertProduct(
            Product(businessId = "1L", name = "Original", brand = "B", category = "C", unitType = "U", pricePerUnit = 10.0, availableStock = 50.0)
        )

        val affectedRows = productDao.deleteProduct(productId, "biz-2") // Wrong business ID

        assertEquals(0, affectedRows) // Delete should fail

        val product = productDao.getProductById(productId, "biz-1")
        assertNotNull(product) // Verify still exists
    }
}
