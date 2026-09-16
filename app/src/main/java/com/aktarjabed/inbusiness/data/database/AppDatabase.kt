package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aktarjabed.inbusiness.data.converters.Converters
import com.aktarjabed.inbusiness.data.dao.*
import com.aktarjabed.inbusiness.data.entities.*
import com.aktarjabed.inbusiness.security.KeyProvider
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        BusinessData::class,
        Invoice::class,
        InvoiceItem::class,
        CalculationResult::class,
        UserQuotaEntity::class,
        InvoiceSequence::class,
        Product::class,
        Customer::class,
        Payment::class,
        StockMovement::class
    ],
    version = 18,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun userQuotaDao(): UserQuotaDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun paymentDao(): PaymentDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun dashboardDao(): DashboardDao

    companion object {
        private const val DATABASE_NAME = "inbusiness_ultra.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, keyProvider: KeyProvider): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, keyProvider)
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure foreign keys are turned off during migration
                db.execSQL("PRAGMA foreign_keys=OFF")

                // Create the invoice sequence table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_sequences` (
                        `businessId` TEXT NOT null,
                        `currentNumber` INTEGER NOT null,
                        PRIMARY KEY(`businessId`)
                    )
                """)

                // Drop and recreate invoices table with composite unique index
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoices_new` (
                        `id` TEXT NOT null,
                        `businessId` TEXT NOT null,
                        `invoiceNumber` TEXT NOT null,
                        `customerId` TEXT NOT null,
                        `customerName` TEXT NOT null,
                        `customerGSTIN` TEXT,
                        `totalAmount` REAL NOT null,
                        `taxAmount` REAL NOT null,
                        `createdAt` INTEGER NOT null,
                        `updatedAt` INTEGER NOT null,
                        `irn` TEXT,
                        `ackNo` TEXT,
                        `ackDate` INTEGER,
                        `qrCodeData` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("INSERT INTO invoices_new SELECT * FROM invoices")
                db.execSQL("DROP TABLE invoices")
                db.execSQL("ALTER TABLE invoices_new RENAME TO invoices")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_businessId_invoiceNumber` ON `invoices` (`businessId`, `invoiceNumber`)")

                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN idempotencyKey TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_idempotencyKey` ON `invoices` (`idempotencyKey`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `products` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `businessId` TEXT NOT NULL,
                        `name` TEXT NOT NULL COLLATE NOCASE,
                        `brand` TEXT NOT NULL COLLATE NOCASE,
                        `category` TEXT NOT NULL COLLATE NOCASE,
                        `unitType` TEXT NOT NULL COLLATE NOCASE,
                        `pricePerUnit` REAL NOT NULL,
                        `availableStock` REAL NOT NULL,
                        `batchNumber` TEXT NOT NULL,
                        `isWholesaleOnly` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_businessId_name_brand_category_unitType_batchNumber` ON `products` (`businessId`, `name`, `brand`, `category`, `unitType`, `batchNumber`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN buyerAddress TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE invoices ADD COLUMN totalCgst REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN totalSgst REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN totalIgst REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN supplyType TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE invoice_items ADD COLUMN gstPercentage REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoice_items ADD COLUMN taxAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoice_items ADD COLUMN totalAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoice_items ADD COLUMN productId INTEGER DEFAULT NULL")
            }
        }


        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add payment fields to invoices table
                db.execSQL("ALTER TABLE invoices ADD COLUMN amountPaid REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN balanceDue REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'NONE'")

                // 2. Fix the sequence table name and column from migration 3->4
                val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='invoice_sequences'")
                if (cursor.moveToFirst()) {
                    // Only do the renaming dance if the bad table exists
                    db.execSQL("CREATE TABLE IF NOT EXISTS `invoice_sequence_new` (`businessId` TEXT NOT NULL, `lastSequenceNumber` INTEGER NOT NULL, PRIMARY KEY(`businessId`))")
                    db.execSQL("INSERT INTO invoice_sequence_new (businessId, lastSequenceNumber) SELECT businessId, currentNumber FROM invoice_sequences")
                    db.execSQL("DROP TABLE invoice_sequences")
                    db.execSQL("DROP TABLE IF EXISTS invoice_sequence")
                    db.execSQL("ALTER TABLE invoice_sequence_new RENAME TO invoice_sequence")
                }
                cursor.close()
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN requestFingerprint TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN sellerName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE invoices ADD COLUMN sellerAddress TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE invoices ADD COLUMN sellerGSTIN TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN subtotal REAL NOT NULL DEFAULT 0.0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_businessId_idempotencyKey` ON `invoices` (`businessId`, `idempotencyKey`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_invoices_idempotencyKey`")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN gstPercentage REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                var hasGstPercentage = false
                db.query("PRAGMA table_info(products)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        while (cursor.moveToNext()) {
                            if (cursor.getString(nameIndex) == "gstPercentage") {
                                hasGstPercentage = true
                                break
                            }
                        }
                    }
                }
                if (!hasGstPercentage) {
                    db.execSQL("ALTER TABLE products ADD COLUMN gstPercentage REAL NOT NULL DEFAULT 0.0")
                }
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `businessId` INTEGER NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL DEFAULT '', `gstin` TEXT NOT NULL DEFAULT '', `phone` TEXT NOT NULL DEFAULT '', `isActive` INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(`businessId`) REFERENCES `business_data`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_businessId_name` ON `customers` (`businessId`, `name`)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `businessId` INTEGER NOT NULL, `invoiceId` TEXT NOT NULL, `amount` REAL NOT NULL, `paymentMode` TEXT NOT NULL DEFAULT 'CASH', `paymentDate` INTEGER NOT NULL, `referenceNumber` TEXT NOT NULL DEFAULT '', `status` TEXT NOT NULL DEFAULT 'SUCCESS', FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_businessId` ON `payments` (`businessId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_invoiceId` ON `payments` (`invoiceId`)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `businessId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `movementType` TEXT NOT NULL, `quantity` REAL NOT NULL, `stockBefore` REAL NOT NULL, `stockAfter` REAL NOT NULL, `referenceType` TEXT NOT NULL, `referenceId` TEXT NOT NULL, `reason` TEXT NOT NULL DEFAULT '', `createdAt` INTEGER NOT NULL, FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_businessId` ON `stock_movements` (`businessId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_referenceId` ON `stock_movements` (`referenceId`)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
                db.execSQL("ALTER TABLE invoices ADD COLUMN documentType TEXT NOT NULL DEFAULT 'TAX_INVOICE'")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN reorderThreshold REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE products ADD COLUMN hsnSac TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN uqc TEXT NOT NULL DEFAULT ''")
            }
        }


        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure foreign keys are turned off during migration
                db.execSQL("PRAGMA foreign_keys=OFF")

                // Create new invoice_items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_items_new` (
                        `id` TEXT NOT NULL,
                        `invoiceId` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `pricePerUnit` REAL NOT NULL,
                        `unitType` TEXT NOT NULL,
                        `subTotal` REAL NOT NULL,
                        `gstPercentage` REAL NOT NULL,
                        `taxAmount` REAL NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `productId` INTEGER,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)

                // Copy data, mapping old fields to new ones. Note: taxRate goes to gstPercentage ONLY if gstPercentage is 0.0 (or just take max, or coalesce)
                // Actually user said: Migrate the old taxRate value into gstPercentage when taxRate represents the historical GST percentage.
                // If the old column gstPercentage had values we should preserve them. If taxRate has values we should use them if gstPercentage is 0.

                db.execSQL("""
                    INSERT INTO invoice_items_new (id, invoiceId, description, quantity, pricePerUnit, unitType, subTotal, gstPercentage, taxAmount, totalAmount, productId)
                    SELECT
                        id,
                        invoiceId,
                        description,
                        quantity,
                        unitPrice as pricePerUnit,
                        '' as unitType,
                        amount as subTotal,
                        CASE WHEN gstPercentage = 0.0 AND taxRate > 0.0 THEN taxRate ELSE gstPercentage END as gstPercentage,
                        taxAmount,
                        totalAmount,
                        productId
                    FROM invoice_items
                """)

                db.execSQL("DROP TABLE invoice_items")
                db.execSQL("ALTER TABLE invoice_items_new RENAME TO invoice_items")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoiceId` ON `invoice_items` (`invoiceId`)")

                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }

    private fun buildDatabase(context: Context, keyProvider: KeyProvider): AppDatabase {
            val passphrase = keyProvider.getDatabasePassphrase()
            val passphraseBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())
            val factory = SupportFactory(passphraseBytes)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
                .addCallback(DatabaseCallback())
                .build()
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("PRAGMA foreign_keys=ON")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }
    }
}
