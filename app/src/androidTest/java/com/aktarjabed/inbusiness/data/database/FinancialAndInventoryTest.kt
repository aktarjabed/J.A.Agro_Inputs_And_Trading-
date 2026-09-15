package com.aktarjabed.inbusiness.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktarjabed.inbusiness.data.dao.*
import com.aktarjabed.inbusiness.data.entities.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class FinancialAndInventoryTest {
    private lateinit var database: AppDatabase
    private lateinit var invoiceDao: InvoiceDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var stockMovementDao: StockMovementDao
    private lateinit var productDao: ProductDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        invoiceDao = database.invoiceDao()
        paymentDao = database.paymentDao()
        stockMovementDao = database.stockMovementDao()
        productDao = database.productDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPaymentInsertionUpdatesInvoiceBalance() = runBlocking {
        val bizId = "biz-1"
        val invId = "inv-1"

        // Setup initial invoice
        val invoice = Invoice(
            id = invId,
            businessId = bizId,
            totalAmount = 1000.0,
            amountPaid = 0.0,
            balanceDue = 1000.0,
            status = "COMPLETED"
        )
        invoiceDao.insertInvoice(invoice)

        // Add partial payment
        val payment1 = Payment(
            businessId = bizId,
            invoiceId = invId,
            amount = 300.0,
            paymentMethod = "CASH"
        )
        paymentDao.insertPayment(payment1)

        // Manually update invoice to simulate PaymentUseCases transaction for test
        val currentTotalPaid = paymentDao.getTotalPaidForInvoice(bizId, invId) ?: 0.0
        invoiceDao.updateInvoice(invoice.copy(amountPaid = currentTotalPaid, balanceDue = invoice.totalAmount - currentTotalPaid))

        var updatedInvoice = invoiceDao.getInvoiceById(invId, bizId)
        assertNotNull(updatedInvoice)
        assertEquals(300.0, updatedInvoice!!.amountPaid, 0.001)
        assertEquals(700.0, updatedInvoice.balanceDue, 0.001)

        // Add another payment
        val payment2 = Payment(
            businessId = bizId,
            invoiceId = invId,
            amount = 700.0,
            paymentMethod = "UPI"
        )
        paymentDao.insertPayment(payment2)

        val newTotalPaid = paymentDao.getTotalPaidForInvoice(bizId, invId) ?: 0.0
        invoiceDao.updateInvoice(invoice.copy(amountPaid = newTotalPaid, balanceDue = invoice.totalAmount - newTotalPaid))

        updatedInvoice = invoiceDao.getInvoiceById(invId, bizId)
        assertEquals(1000.0, updatedInvoice!!.amountPaid, 0.001)
        assertEquals(0.0, updatedInvoice.balanceDue, 0.001)
    }

    @Test
    fun testStockMovementOnSaleAndCancellation() = runBlocking {
        val bizId = "biz-1"
        val productId = 1L
        val invId = "inv-1"

        // Initial product
        val product = Product(
            id = productId,
            businessId = bizId,
            name = "Test Prod",
            brand = "Brand",
            category = "Cat",
            unitType = "PCS",
            pricePerUnit = 10.0,
            availableStock = 50.0
        )
        productDao.insertProduct(product)

        // 1. Simulate Sale
        val qtySold = 5.0
        productDao.deductStock(productId, bizId, qtySold)

        val moveSale = StockMovement(
            businessId = bizId,
            productId = productId,
            movementType = "SALE",
            quantity = qtySold,
            stockBefore = 50.0,
            stockAfter = 45.0,
            referenceType = "INVOICE",
            referenceId = invId
        )
        stockMovementDao.insertMovement(moveSale)

        val productAfterSale = productDao.getProductById(productId, bizId)
        assertEquals(45.0, productAfterSale!!.availableStock, 0.001)

        // 2. Simulate Cancellation Reversal
        productDao.addStock(productId, bizId, qtySold)
        val moveReversal = StockMovement(
            businessId = bizId,
            productId = productId,
            movementType = "SALE_REVERSAL",
            quantity = qtySold,
            stockBefore = 45.0,
            stockAfter = 50.0,
            referenceType = "INVOICE",
            referenceId = invId
        )
        stockMovementDao.insertMovement(moveReversal)

        val productAfterReversal = productDao.getProductById(productId, bizId)
        assertEquals(50.0, productAfterReversal!!.availableStock, 0.001)

        val movements = stockMovementDao.getMovementsByReference(bizId, "INVOICE", invId)
        assertEquals(2, movements.size)
        assertEquals("SALE", movements[0].movementType)
        assertEquals("SALE_REVERSAL", movements[1].movementType)
    }
}
