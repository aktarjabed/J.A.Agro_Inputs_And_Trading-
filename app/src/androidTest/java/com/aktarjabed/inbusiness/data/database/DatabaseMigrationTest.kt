package com.aktarjabed.inbusiness.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate13To18() {
        // Create the database at version 13
        var db = helper.createDatabase(TEST_DB, 13)

        // Insert some data in older schema
        db.execSQL("""
            INSERT INTO invoices (id, businessId, invoiceNumber, subtotal, totalAmount, taxAmount, amountPaid, balanceDue, paymentMethod, createdAt, updatedAt)
            VALUES ('inv-1', 'biz-1', 'INV-0001', 100.0, 118.0, 18.0, 0.0, 118.0, 'NONE', 1000000, 1000000)
        """)
        db.execSQL("""
            INSERT INTO products (businessId, name, brand, category, unitType, pricePerUnit, availableStock, isWholesaleOnly, gstPercentage)
            VALUES ('biz-1', 'Product1', 'Brand1', 'Cat1', 'PCS', 10.0, 100.0, 0, 18.0)
        """)

        db.close()

        // Run migrations up to 18
        db = helper.runMigrationsAndValidate(TEST_DB, 18, true,
            AppDatabase.MIGRATION_13_14,
            AppDatabase.MIGRATION_14_15,
            AppDatabase.MIGRATION_15_16,
            AppDatabase.MIGRATION_16_17,
            AppDatabase.MIGRATION_17_18
        )

        // Verify the new columns and tables exist by querying them

        // 1. Invoices should have status, documentType, placeOfSupply, reverseCharge
        val invoiceCursor = db.query("SELECT status, documentType, placeOfSupply, reverseCharge FROM invoices WHERE id = 'inv-1'")
        invoiceCursor.moveToFirst()
        assertEquals("COMPLETED", invoiceCursor.getString(0))
        assertEquals("TAX_INVOICE", invoiceCursor.getString(1))
        assertEquals("", invoiceCursor.getString(2))
        assertEquals(0, invoiceCursor.getInt(3))
        invoiceCursor.close()

        // 2. Products should have isActive, hsnSac, uqc
        val prodCursor = db.query("SELECT isActive, hsnSac, uqc FROM products WHERE name = 'Product1'")
        prodCursor.moveToFirst()
        assertEquals(1, prodCursor.getInt(0))
        assertEquals("", prodCursor.getString(1))
        assertEquals("", prodCursor.getString(2))
        prodCursor.close()

        // 3. New tables should be accessible
        db.execSQL("INSERT INTO customers (businessId, name, gstin, address, phone, email, createdAt) VALUES ('biz-1', 'Cust1', '', '', '', '', 100)")
        db.execSQL("INSERT INTO payments (businessId, invoiceId, amount, paymentMethod, paymentDate, referenceId, notes) VALUES ('biz-1', 'inv-1', 100.0, 'CASH', 100, '', '')")
        db.execSQL("INSERT INTO stock_movements (businessId, productId, movementType, quantity, stockBefore, stockAfter, referenceType, referenceId, reason, createdAt) VALUES ('biz-1', 1, 'SALE', 5.0, 100.0, 95.0, 'INVOICE', 'inv-1', '', 100)")
    }
}
