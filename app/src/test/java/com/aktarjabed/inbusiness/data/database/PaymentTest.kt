package com.aktarjabed.inbusiness.data.database

import com.aktarjabed.inbusiness.domain.invoice.GstCalculator
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests payment and balance calculations using GstCalculator-computed totals,
 * verifying correct balance due under various payment scenarios.
 */
class PaymentTest {

    private fun computeTotal(quantity: Double, unitPrice: Double, gstPercentage: Double): Double {
        val result = GstCalculator.calculateItemTaxes(
            quantity = quantity,
            unitPrice = unitPrice,
            gstPercentage = gstPercentage,
            supplyType = SupplyType.INTRA_STATE
        )
        return result.totalAmount
    }

    @Test
    fun testFullPaymentYieldsZeroBalance() {
        val totalAmount = computeTotal(quantity = 10.0, unitPrice = 100.0, gstPercentage = 18.0)
        // 10 * 100 = 1000 subtotal, 18% GST = 180, total = 1180
        assertEquals(1180.0, totalAmount, 0.01)

        val balanceDue = totalAmount - totalAmount
        assertEquals(0.0, balanceDue, 0.0)
    }

    @Test
    fun testPartialPaymentYieldsPositiveBalance() {
        val totalAmount = computeTotal(quantity = 5.0, unitPrice = 200.0, gstPercentage = 12.0)
        // 5 * 200 = 1000 subtotal, 12% GST = 120, total = 1120
        assertEquals(1120.0, totalAmount, 0.01)

        val amountPaid = 500.0
        val balanceDue = totalAmount - amountPaid
        assertEquals(620.0, balanceDue, 0.01)
        assertTrue("Balance should be positive for partial payment", balanceDue > 0)
    }

    @Test
    fun testZeroPaymentYieldsFullBalance() {
        val totalAmount = computeTotal(quantity = 3.0, unitPrice = 150.0, gstPercentage = 5.0)
        // 3 * 150 = 450 subtotal, 5% GST = 22.5, total = 472.5
        assertEquals(472.5, totalAmount, 0.01)

        val amountPaid = 0.0
        val balanceDue = totalAmount - amountPaid
        assertEquals(totalAmount, balanceDue, 0.0)
    }

    @Test
    fun testOverpaymentDetection() {
        val totalAmount = computeTotal(quantity = 1.0, unitPrice = 100.0, gstPercentage = 18.0)
        assertEquals(118.0, totalAmount, 0.01)

        val amountPaid = 150.0
        val balanceDue = totalAmount - amountPaid
        assertTrue("Overpayment should result in negative balance", balanceDue < 0)
        assertEquals(-32.0, balanceDue, 0.01)
    }

    @Test
    fun testMultiItemInvoicePayment() {
        // Simulate a multi-item invoice
        val item1Total = computeTotal(quantity = 2.0, unitPrice = 500.0, gstPercentage = 12.0)
        val item2Total = computeTotal(quantity = 10.0, unitPrice = 50.0, gstPercentage = 5.0)
        val item3Total = computeTotal(quantity = 1.0, unitPrice = 1000.0, gstPercentage = 0.0)

        // item1: 2*500=1000, GST=120, total=1120
        // item2: 10*50=500, GST=25, total=525
        // item3: 1*1000=1000, GST=0, total=1000
        val grandTotal = item1Total + item2Total + item3Total
        assertEquals(2645.0, grandTotal, 0.01)

        val amountPaid = 2000.0
        val balanceDue = grandTotal - amountPaid
        assertEquals(645.0, balanceDue, 0.01)
    }

    @Test
    fun testExactPennyPayment() {
        // Tests fractional amounts with rounding
        val totalAmount = computeTotal(quantity = 3.0, unitPrice = 1.33, gstPercentage = 18.0)
        // subtotal = 3.99, tax = 0.72, total = 4.71
        assertEquals(4.71, totalAmount, 0.01)

        val amountPaid = 4.71
        val balanceDue = totalAmount - amountPaid
        assertEquals(0.0, balanceDue, 0.01)
    }
}
