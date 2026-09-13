package com.aktarjabed.inbusiness.domain.invoice

import org.junit.Assert.assertEquals
import org.junit.Test

class GstCalculatorTest {

    // --- Intra-State Tests ---

    @Test
    fun testIntraStateRounding() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.5,
            gstPercentage = 10.0,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(100.5, result.subtotal, 0.0001)
        assertEquals(10.05, result.taxAmount, 0.0001)
        assertEquals(5.03, result.cgstAmount, 0.0001)
        assertEquals(5.02, result.sgstAmount, 0.0001)
        assertEquals(0.0, result.igstAmount, 0.0001)
        assertEquals(110.55, result.totalAmount, 0.0001)
    }

    @Test
    fun testZeroGst() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 10.0,
            unitPrice = 15.0,
            gstPercentage = 0.0,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(150.0, result.subtotal, 0.0)
        assertEquals(0.0, result.taxAmount, 0.0)
        assertEquals(0.0, result.cgstAmount, 0.0)
        assertEquals(0.0, result.sgstAmount, 0.0)
        assertEquals(0.0, result.igstAmount, 0.0)
        assertEquals(150.0, result.totalAmount, 0.0)
    }

    @Test
    fun testDecimalGst() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.0,
            gstPercentage = 5.5,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(100.0, result.subtotal, 0.0)
        assertEquals(5.5, result.taxAmount, 0.0)
        assertEquals(2.75, result.cgstAmount, 0.0)
        assertEquals(2.75, result.sgstAmount, 0.0)
        assertEquals(105.5, result.totalAmount, 0.0)
    }

    @Test
    fun testRounding() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 3.0,
            unitPrice = 1.33,
            gstPercentage = 18.0,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(3.99, result.subtotal, 0.0)
        assertEquals(0.72, result.taxAmount, 0.0)
        assertEquals(0.36, result.cgstAmount, 0.0)
        assertEquals(0.36, result.sgstAmount, 0.0)
        assertEquals(4.71, result.totalAmount, 0.0)
    }

    // --- Inter-State Tests ---

    @Test
    fun testInterStateRounding() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.5,
            gstPercentage = 10.0,
            supplyType = SupplyType.INTER_STATE
        )
        assertEquals(100.5, result.subtotal, 0.0001)
        assertEquals(10.05, result.taxAmount, 0.0001)
        assertEquals(0.0, result.cgstAmount, 0.0001)
        assertEquals(0.0, result.sgstAmount, 0.0001)
        assertEquals(10.05, result.igstAmount, 0.0001)
        assertEquals(110.55, result.totalAmount, 0.0001)
    }

    @Test
    fun testInterStateZeroGst() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 5.0,
            unitPrice = 200.0,
            gstPercentage = 0.0,
            supplyType = SupplyType.INTER_STATE
        )
        assertEquals(1000.0, result.subtotal, 0.0)
        assertEquals(0.0, result.taxAmount, 0.0)
        assertEquals(0.0, result.igstAmount, 0.0)
        assertEquals(1000.0, result.totalAmount, 0.0)
    }

    // --- GSTIN Supply Type Resolution ---

    @Test
    fun testGstinValidationAndSupplyType() {
        assertEquals(SupplyType.INTRA_STATE, GstCalculator.determineSupplyType("29ABCDE1234F1Z5", "29XYZAB5678C1Z9"))
        assertEquals(SupplyType.INTER_STATE, GstCalculator.determineSupplyType("29ABCDE1234F1Z5", "27XYZAB5678C1Z9"))
        assertEquals(SupplyType.UNKNOWN, GstCalculator.determineSupplyType("INVALID", "27XYZAB5678C1Z9"))
        assertEquals(SupplyType.UNKNOWN, GstCalculator.determineSupplyType(null, null))
    }
}
