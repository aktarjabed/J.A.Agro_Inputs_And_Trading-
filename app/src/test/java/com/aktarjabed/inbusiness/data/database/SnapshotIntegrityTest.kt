package com.aktarjabed.inbusiness.data.database

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.domain.invoice.GstCalculator
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests that invoice item snapshots are computed correctly via GstCalculator
 * and remain independent of subsequent product or input changes.
 */
class SnapshotIntegrityTest {

    @Test
    fun testSnapshotValuesComputedFromGstCalculator() {
        // Given a product at a known price
        val product = Product(
            id = 1L,
            businessId = "biz-1",
            name = "Fertilizer",
            brand = "Brand",
            category = "Cat",
            unitType = "Kg",
            pricePerUnit = 100.0,
            availableStock = 50.0
        )

        // When we calculate taxes for an invoice item using GstCalculator
        val taxResult = GstCalculator.calculateItemTaxes(
            quantity = 5.0,
            unitPrice = product.pricePerUnit,
            gstPercentage = 5.0,
            supplyType = SupplyType.INTRA_STATE
        )

        // Then the snapshot should have correct computed values
        val snapshotItem = InvoiceItem(
            id = "item-1",
            invoiceId = "inv-1",
            description = product.name,
            quantity = 5.0,
            pricePerUnit = product.pricePerUnit,
            unitType = product.unitType,
            subTotal = taxResult.subtotal,
            gstPercentage = 5.0,
            taxAmount = taxResult.taxAmount,
            totalAmount = taxResult.totalAmount,
            productId = product.id
        )

        assertEquals(500.0, snapshotItem.subTotal, 0.0)
        assertEquals(25.0, snapshotItem.taxAmount, 0.0)
        assertEquals(525.0, snapshotItem.totalAmount, 0.0)
        assertEquals("Fertilizer", snapshotItem.description)
        assertEquals(100.0, snapshotItem.pricePerUnit, 0.0)
    }

    @Test
    fun testSnapshotUnaffectedByProductPriceChange() {
        // Given an item snapshot created at old price
        val originalPrice = 100.0
        val taxResult = GstCalculator.calculateItemTaxes(
            quantity = 10.0,
            unitPrice = originalPrice,
            gstPercentage = 18.0,
            supplyType = SupplyType.INTRA_STATE
        )

        val snapshotItem = InvoiceItem(
            id = "item-1",
            invoiceId = "inv-1",
            description = "Pesticide",
            quantity = 10.0,
            pricePerUnit = originalPrice,
            unitType = "L",
            subTotal = taxResult.subtotal,
            gstPercentage = 18.0,
            taxAmount = taxResult.taxAmount,
            totalAmount = taxResult.totalAmount,
            productId = 1L
        )

        // When the product price changes and we recalculate
        val newPrice = 150.0
        val newTaxResult = GstCalculator.calculateItemTaxes(
            quantity = 10.0,
            unitPrice = newPrice,
            gstPercentage = 18.0,
            supplyType = SupplyType.INTRA_STATE
        )

        // Then the original snapshot remains unchanged
        assertEquals(originalPrice, snapshotItem.pricePerUnit, 0.0)
        assertEquals(taxResult.subtotal, snapshotItem.subTotal, 0.0)
        assertEquals(taxResult.totalAmount, snapshotItem.totalAmount, 0.0)

        // And the new calculation is different
        assertNotEquals(snapshotItem.subTotal, newTaxResult.subtotal, 0.0)
        assertNotEquals(snapshotItem.totalAmount, newTaxResult.totalAmount, 0.0)
    }

    @Test
    fun testMultipleItemsHaveIndependentSnapshots() {
        val result1 = GstCalculator.calculateItemTaxes(
            quantity = 2.0, unitPrice = 500.0,
            gstPercentage = 12.0, supplyType = SupplyType.INTRA_STATE
        )
        val result2 = GstCalculator.calculateItemTaxes(
            quantity = 10.0, unitPrice = 50.0,
            gstPercentage = 5.0, supplyType = SupplyType.INTER_STATE
        )

        val item1 = InvoiceItem(
            id = "item-1", invoiceId = "inv-1",
            description = "Seeds", quantity = 2.0, pricePerUnit = 500.0,
            unitType = "Kg", subTotal = result1.subtotal, gstPercentage = 12.0,
            taxAmount = result1.taxAmount, totalAmount = result1.totalAmount, productId = 1L
        )
        val item2 = InvoiceItem(
            id = "item-2", invoiceId = "inv-1",
            description = "Spray", quantity = 10.0, pricePerUnit = 50.0,
            unitType = "L", subTotal = result2.subtotal, gstPercentage = 5.0,
            taxAmount = result2.taxAmount, totalAmount = result2.totalAmount, productId = 2L
        )

        // Item 1: intra-state, CGST/SGST split
        assertEquals(1000.0, item1.subTotal, 0.0)
        assertEquals(120.0, item1.taxAmount, 0.0)
        assertEquals(1120.0, item1.totalAmount, 0.0)

        // Item 2: inter-state, full IGST
        assertEquals(500.0, item2.subTotal, 0.0)
        assertEquals(25.0, item2.taxAmount, 0.0)
        assertEquals(525.0, item2.totalAmount, 0.0)
    }

    @Test
    fun testAdHocItemWithNullProductId() {
        val taxResult = GstCalculator.calculateItemTaxes(
            quantity = 1.0, unitPrice = 500.0,
            gstPercentage = 0.0, supplyType = SupplyType.INTRA_STATE
        )

        val adHocItem = InvoiceItem(
            id = "item-3", invoiceId = "inv-1",
            description = "Custom Labour", quantity = 1.0, pricePerUnit = 500.0,
            unitType = "Day", subTotal = taxResult.subtotal, gstPercentage = 0.0,
            taxAmount = taxResult.taxAmount, totalAmount = taxResult.totalAmount,
            productId = null
        )

        assertNull(adHocItem.productId)
        assertEquals(500.0, adHocItem.totalAmount, 0.0)
        assertEquals(0.0, adHocItem.taxAmount, 0.0)
    }
}
