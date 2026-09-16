package com.aktarjabed.inbusiness.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.domain.invoice.CalculateInvoiceTotalsUseCase
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@RunWith(AndroidJUnit4::class)
class InvoiceIdempotencyTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: InvoiceRepository
    private lateinit var businessContext: BusinessContext
    private lateinit var quotaGate: QuotaGate

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        businessContext = BusinessContext(context)
        runBlocking {
            businessContext.setUserId("u1")
            businessContext.setActiveBusinessId("b1")

            db.businessDao().insertBusinessData(
                BusinessData(id = "b1", name = "TestBiz", gstin = "", address = "", city = "", state = "", pincode = "", phoneNumber = "", email = "", scenarioName = "", unitPrice = 0.0, quantity = 0.0, rawMaterialsCost = 0.0, supplierCosts = 0.0, monthlyRent = 0.0, transportCosts = 0.0, labourCosts = 0.0, utilityCosts = 0.0, marketingCosts = 0.0, insuranceCosts = 0.0, interestCosts = 0.0, depreciation = 0.0, incomeTaxSlab = 0.0, tdsAmount = 0.0, otherIncome = 0.0, outputGst = 0.0, inputGst = 0.0)
            )
            db.businessDao().insertBusinessData(
                BusinessData(id = "b2", name = "OtherBiz", gstin = "", address = "", city = "", state = "", pincode = "", phoneNumber = "", email = "", scenarioName = "", unitPrice = 0.0, quantity = 0.0, rawMaterialsCost = 0.0, supplierCosts = 0.0, monthlyRent = 0.0, transportCosts = 0.0, labourCosts = 0.0, utilityCosts = 0.0, marketingCosts = 0.0, insuranceCosts = 0.0, interestCosts = 0.0, depreciation = 0.0, incomeTaxSlab = 0.0, tdsAmount = 0.0, otherIncome = 0.0, outputGst = 0.0, inputGst = 0.0)
            )
        }

        val classifier = com.aktarjabed.inbusiness.domain.device.DeviceClassifier()
        val clock = com.aktarjabed.inbusiness.util.SystemClock()
        quotaGate = QuotaGate(db.userQuotaDao(), classifier, clock, context)

        repo = InvoiceRepository(
            db, db.invoiceDao(), db.paymentDao(), db.stockMovementDao(), db.productDao(), db.businessDao(),
            CalculateInvoiceTotalsUseCase(), quotaGate, businessContext
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testSameKeySameFingerprintReplays() = runBlocking {
        val items = listOf(InvoiceItem("", "", "Item", 1.0, 10.0, "", 10.0, 0.0, 0.0, 10.0, null))
        val res1 = repo.createInvoice("Cust", null, "", SupplyType.INTRA_STATE, items, "key1", 0.0, "NONE")
        assertTrue(res1 is InvoiceCreationResult.Success)

        val res2 = repo.createInvoice("Cust", null, "", SupplyType.INTRA_STATE, items, "key1", 0.0, "NONE")
        assertTrue(res2 is InvoiceCreationResult.IdempotentReplay)

        val invoices = db.invoiceDao().getAllInvoicesOnce("b1")
        assertEquals(1, invoices.size)
    }

    @Test
    fun testSameKeyDifferentFingerprintFails() = runBlocking {
        val items1 = listOf(InvoiceItem("", "", "Item", 1.0, 10.0, "", 10.0, 0.0, 0.0, 10.0, null))
        val items2 = listOf(InvoiceItem("", "", "Item", 2.0, 20.0, "", 20.0, 0.0, 0.0, 20.0, null))

        val res1 = repo.createInvoice("Cust", null, "", SupplyType.INTRA_STATE, items1, "key1", 0.0, "NONE")
        assertTrue(res1 is InvoiceCreationResult.Success)

        val res2 = repo.createInvoice("Cust", null, "", SupplyType.INTRA_STATE, items2, "key1", 0.0, "NONE")
        assertTrue("Expected InvalidRequest for conflict, got: $res2", res2 is InvoiceCreationResult.InvalidRequest)

        val invoices = db.invoiceDao().getAllInvoicesOnce("b1")
        assertEquals(1, invoices.size)
    }

    @Test
    fun testDifferentBusinessSameKeySucceeds() = runBlocking {
        val items = listOf(InvoiceItem("", "", "Item", 1.0, 10.0, "", 10.0, 0.0, 0.0, 10.0, null))
        val res1 = repo.createInvoice("Cust", null, "", SupplyType.INTRA_STATE, items, "key1", 0.0, "NONE")
        assertTrue(res1 is InvoiceCreationResult.Success)

        businessContext.setActiveBusinessId("b2") // Switch business
        val res2 = repo.createInvoice("Cust", null, "", SupplyType.INTRA_STATE, items, "key1", 0.0, "NONE")
        assertTrue(res2 is InvoiceCreationResult.Success)

        assertEquals(1, db.invoiceDao().getAllInvoicesOnce("b1").size)
        assertEquals(1, db.invoiceDao().getAllInvoicesOnce("b2").size)
    }

    @Test
    fun testConcurrentInsertionsSamePayloadReplays() = runBlocking {
        val items = listOf(InvoiceItem("", "", "Item", 1.0, 10.0, "", 10.0, 0.0, 0.0, 10.0, null))

        val jobs = (1..5).map {
            async(kotlinx.coroutines.Dispatchers.IO) {
                repo.createInvoice("Cust", null, "", SupplyType.INTRA_STATE, items, "keyRace", 0.0, "NONE")
            }
        }

        val results = jobs.awaitAll()
        val successes = results.filterIsInstance<InvoiceCreationResult.Success>()
        val replays = results.filterIsInstance<InvoiceCreationResult.IdempotentReplay>()

        assertEquals(1, successes.size)
        assertEquals(4, replays.size)

        val invoices = db.invoiceDao().getAllInvoicesOnce("b1")
        assertEquals(1, invoices.size)
    }
}
