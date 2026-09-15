package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.InvoiceSequence
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getAllInvoices(businessId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC")
    suspend fun getAllInvoicesOnce(businessId: String): List<Invoice>

    @Query("SELECT * FROM invoices WHERE id = :id AND businessId = :businessId LIMIT 1")
    suspend fun getInvoiceById(id: String, businessId: String): Invoice?

    @Query("SELECT * FROM invoices WHERE idempotencyKey = :idempotencyKey AND businessId = :businessId LIMIT 1")
    suspend fun getInvoiceByIdempotencyKey(idempotencyKey: String, businessId: String): Invoice?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId AND invoiceId IN (SELECT id FROM invoices WHERE businessId = :businessId)")
    suspend fun getInvoiceItems(invoiceId: String, businessId: String): List<InvoiceItem>

        @Query("""
        SELECT i.*
        FROM invoice_items i
        INNER JOIN invoices inv ON i.invoiceId = inv.id
        WHERE inv.businessId = :businessId
          AND i.id = (
              SELECT i2.id FROM invoice_items i2
              INNER JOIN invoices inv2 ON i2.invoiceId = inv2.id
              WHERE inv2.businessId = :businessId
                AND IFNULL(i2.productId, -1) = IFNULL(i.productId, -1)
                AND (
                    (i.productId IS NOT NULL) OR
                    (i.productId IS NULL AND LOWER(TRIM(i2.description)) = LOWER(TRIM(i.description)))
                )
              ORDER BY inv2.createdAt DESC, i2.id DESC
              LIMIT 1
          )
        ORDER BY inv.createdAt DESC
    """)
    fun getHistoricalInvoiceItems(businessId: String): Flow<List<InvoiceItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: Invoice)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: InvoiceItem)

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE businessId = :businessId AND (invoiceNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%')")
    fun searchInvoices(businessId: String, query: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentInvoicesByBusiness(businessId: String, limit: Int): List<Invoice>

    @RawQuery
    suspend fun getInvoicesByQuery(query: androidx.sqlite.db.SupportSQLiteQuery): List<Invoice>

    @Query("SELECT * FROM invoice_sequence WHERE businessId = :businessId LIMIT 1")
    suspend fun getInvoiceSequence(businessId: String): InvoiceSequence?

    @Query("UPDATE invoice_sequence SET lastSequenceNumber = lastSequenceNumber + 1 WHERE businessId = :businessId")
    suspend fun incrementSequence(businessId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSequence(sequence: InvoiceSequence): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSequence(sequence: InvoiceSequence)
}
