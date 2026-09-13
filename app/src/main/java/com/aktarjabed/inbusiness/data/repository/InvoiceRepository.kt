package com.aktarjabed.inbusiness.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.dao.BusinessDao
import com.aktarjabed.inbusiness.data.entities.InvoiceSequence
import com.aktarjabed.inbusiness.domain.invoice.CalculateInvoiceTotalsUseCase
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton

private class TransactionAbortException(val result: com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult) : Exception()

@Singleton
class InvoiceRepository @Inject constructor(
    private val database: AppDatabase,
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao,
    private val businessDao: BusinessDao,
    private val calculateInvoiceTotalsUseCase: CalculateInvoiceTotalsUseCase,
    private val quotaGate: QuotaGate,
    private val businessContext: BusinessContext
) {

    companion object {
        private const val TAG = "InvoiceRepository"
    }

    suspend fun getAllInvoicesOnce(): List<Invoice> {
        val businessId = businessContext.activeBusinessId.first()
        return invoiceDao.getAllInvoicesOnce(businessId)
    }

    suspend fun getInvoiceById(id: String): Invoice? {
        val businessId = businessContext.activeBusinessId.first()
        return invoiceDao.getInvoiceById(id, businessId)
    }

    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem> {
        val businessId = businessContext.activeBusinessId.first()
        return invoiceDao.getInvoiceItems(invoiceId, businessId)
    }

    fun getHistoricalInvoiceItems(): Flow<List<InvoiceItem>> {
        return businessContext.activeBusinessId.flatMapLatest { businessId -> invoiceDao.getHistoricalInvoiceItems(businessId) }
    }




    suspend fun createInvoice(
        customerName: String,
        customerGSTIN: String?,
        buyerAddress: String,
        supplyType: SupplyType,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null,
        amountPaid: Double = 0.0,
        paymentMethod: String = "NONE"
    ): InvoiceCreationResult = withContext(Dispatchers.IO) {

        if (items.isEmpty()) {
            return@withContext InvoiceCreationResult.InvalidRequest("Invoice must have at least one item")
        }

        val businessId = businessContext.activeBusinessId.first()
        val userId = businessContext.currentUserId.first()

        var requestFingerprint: String? = null

        try {
            database.withTransaction {
                val businessData = businessDao.getBusinessDataById(businessId)
                    ?: throw TransactionAbortException(InvoiceCreationResult.InvalidRequest("Business data not found"))

                val sellerName = businessData.name
                val sellerAddress = businessData.address
                val sellerGSTIN = businessData.gstin

                val calcResult = try {
                    calculateInvoiceTotalsUseCase(items, supplyType, amountPaid)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    throw TransactionAbortException(InvoiceCreationResult.InvalidRequest(e.message ?: "Invalid calculation"))
                }

                requestFingerprint = com.aktarjabed.inbusiness.utils.RequestFingerprint.generate(
                    businessId = businessId,
                    sellerName = sellerName,
                    sellerAddress = sellerAddress,
                    sellerGSTIN = sellerGSTIN,
                    customerName = customerName,
                    customerGSTIN = customerGSTIN,
                    buyerAddress = buyerAddress,
                    supplyType = supplyType,
                    subtotal = calcResult.subtotal,
                    totalAmount = calcResult.totalAmount,
                    taxAmount = calcResult.taxAmount,
                    items = calcResult.processedItems,
                    amountPaid = calcResult.amountPaid,
                    paymentMethod = paymentMethod
                )

                // 1. Idempotency Check (Fast path)
                if (idempotencyKey != null) {
                    val existingInvoice = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey, businessId)
                    if (existingInvoice != null) {
                        if (existingInvoice.requestFingerprint == requestFingerprint) {
                            throw TransactionAbortException(InvoiceCreationResult.IdempotentReplay(existingInvoice.id, existingInvoice.invoiceNumber))
                        } else {
                            throw TransactionAbortException(InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload"))
                        }
                    }
                }

                // 2. Consume Quota within the transaction
                val verdict = quotaGate.assertQuota(userId, consume = true)
                if (verdict !is QuotaVerdict.Allowed) {
                    throw TransactionAbortException(InvoiceCreationResult.QuotaExceeded(
                        required = 1,
                        available = 0 // Approximate for now, could be enhanced
                    ))
                }

                // 3. Stock Deductions for linked products
                for (item in items) {
                    if (item.productId != null) {
                        val product = productDao.getProductById(item.productId, businessId)
                            ?: throw TransactionAbortException(InvoiceCreationResult.ProductNotFound(item.productId))

                        val affectedRows = productDao.deductStock(item.productId, businessId, item.quantity)
                        if (affectedRows == 0) {
                            // Rollback and return InsufficientStock
                            throw TransactionAbortException(InvoiceCreationResult.InsufficientStock(
                                productId = item.productId,
                                productName = product.name,
                                requested = item.quantity,
                                available = product.availableStock
                            ))
                        }
                    }
                }

                // 4. Atomic Sequence logic
                // Ensure the sequence row exists safely
                invoiceDao.insertSequence(InvoiceSequence(businessId, 0))

                // Atomically increment
                invoiceDao.incrementSequence(businessId)

                // Read the resulting value
                val currentSeq = invoiceDao.getInvoiceSequence(businessId)
                val nextSeqNumber = currentSeq?.lastSequenceNumber ?: 1

                val nextInvoiceNumber = "INV-${String.format(java.util.Locale.US, "%05d", nextSeqNumber)}"

                val invoiceId = UUID.randomUUID().toString()
                val invoice = Invoice(
                    id = invoiceId,
                    businessId = businessId,
                    idempotencyKey = idempotencyKey,
                    requestFingerprint = requestFingerprint,
                    invoiceNumber = nextInvoiceNumber,
                    sellerName = sellerName,
                    sellerAddress = sellerAddress,
                    sellerGSTIN = sellerGSTIN,
                    customerId = "", // Reserved for full customer management
                    customerName = customerName,
                    customerGSTIN = customerGSTIN,
                    buyerAddress = buyerAddress,
                    subtotal = calcResult.subtotal,
                    totalAmount = calcResult.totalAmount,
                    taxAmount = calcResult.taxAmount,
                    totalCgst = calcResult.totalCgst,
                    totalSgst = calcResult.totalSgst,
                    totalIgst = calcResult.totalIgst,
                    supplyType = supplyType.name,
                    amountPaid = calcResult.amountPaid,
                    balanceDue = calcResult.balanceDue,
                    paymentMethod = paymentMethod,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                val updatedItems = calcResult.processedItems.map {
                    it.copy(
                        invoiceId = invoiceId,
                        id = UUID.randomUUID().toString()
                    )
                }

                invoiceDao.insertInvoice(invoice)
                updatedItems.forEach { invoiceDao.insertItem(it) }

                return@withTransaction InvoiceCreationResult.Success(invoiceId, nextInvoiceNumber)
            }
        } catch (e: TransactionAbortException) {
            return@withContext e.result
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
             // 5. Concurrency fallback for Idempotency (Slow path - unique constraint collision)
             // Transaction has rolled back by now
             Log.e(TAG, "Idempotency constraint conflict", e)
             val msg = e.message ?: ""
             if (!msg.contains("idempotencyKey", ignoreCase = true) && !msg.contains("index_invoices_businessId_idempotencyKey", ignoreCase = true)) {
                 return@withContext InvoiceCreationResult.UnexpectedFailure(e)
             }
             if (idempotencyKey != null) {
                 val existing = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey, businessId)
                 if (existing != null) {
                     if (existing.requestFingerprint == requestFingerprint) {
                         return@withContext InvoiceCreationResult.IdempotentReplay(existing.id, existing.invoiceNumber)
                     } else {
                         return@withContext InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload")
                     }
                 }
             }
             // No matching invoice exists, propagate the original DB failure
             return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        } catch (e: kotlinx.coroutines.CancellationException) {
             throw e // Explicitly rethrow CancellationException
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to create invoice", e)
            return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        }
    }
}
