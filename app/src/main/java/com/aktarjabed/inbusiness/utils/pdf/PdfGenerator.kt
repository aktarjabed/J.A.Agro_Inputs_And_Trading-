package com.aktarjabed.inbusiness.utils.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument

import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595 // A4 Width in PostScript points
        private const val PAGE_HEIGHT = 842 // A4 Height
        private const val MARGIN = 40f
    }

    private val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 24f
        color = Color.BLACK
    }

    private val boldPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        color = Color.BLACK
    }

    private val textPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 12f
        color = Color.BLACK
    }

    private val smallTextPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 10f
        color = Color.DKGRAY
    }

    fun generateInvoicePdf(
        invoice: Invoice,
        items: List<InvoiceItem>
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var yPosition = MARGIN

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header
        yPosition = drawHeader(canvas, invoice, yPosition)

        // Items Table Header
        yPosition = drawTableHeader(canvas, yPosition)

        // Items
        for (item in items) {
            // Check if we need a new page
            if (yPosition > PAGE_HEIGHT - MARGIN - 100) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = MARGIN

                // Redraw seller and buyer header on new page
                yPosition = drawHeader(canvas, invoice, yPosition)

                // Redraw table header on new page
                yPosition = drawTableHeader(canvas, yPosition)
            }

            yPosition = drawItemRow(canvas, item, yPosition)
        }

        // Draw line after items
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, boldPaint)
        yPosition += 20f

        // Check if totals fit (increased margin to accommodate new sections)
        if (yPosition > PAGE_HEIGHT - MARGIN - 250) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = MARGIN

            // Redraw header for context on the new page
            yPosition = drawHeader(canvas, invoice, yPosition)
        }

        // Totals
        yPosition = drawTotals(canvas, invoice, yPosition)

        // Amount in words
        yPosition = drawAmountInWords(canvas, invoice.totalAmount, yPosition)

        // Payment Details
        yPosition = drawPaymentDetails(canvas, invoice, yPosition)

        // Terms
        drawTerms(canvas, yPosition)

        // Footer
        drawFooter(canvas)

        pdfDocument.finishPage(page)

        // Save to FileProvider cache directory
        val cachePath = File(context.cacheDir, "invoices")
        cachePath.mkdirs()
        // Ensure unique filename to prevent overwrite
        val file = File(cachePath, "Invoice_${invoice.invoiceNumber}_${System.currentTimeMillis()}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private val dividerPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val thinDividerPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private fun drawDivider(canvas: Canvas, y: Float) {
        canvas.drawLine(MARGIN, y - 5f, PAGE_WIDTH - MARGIN, y - 5f, dividerPaint)
        canvas.drawLine(MARGIN, y - 1f, PAGE_WIDTH - MARGIN, y - 1f, dividerPaint)
    }

    private fun drawThinDivider(canvas: Canvas, y: Float) {
        canvas.drawLine(MARGIN, y - 3f, PAGE_WIDTH - MARGIN, y - 3f, thinDividerPaint)
    }

    private fun drawHeader(canvas: Canvas, invoice: Invoice, startY: Float): Float {
        var y = startY

        // Title
                // Title uses document type if available
        val titleText = invoice.documentType.replace("_", " ")
        canvas.drawText(titleText, PAGE_WIDTH / 2f - boldPaint.measureText(titleText) / 2, y, boldPaint)
        y += 15f

        drawDivider(canvas, y)
        y += 20f

        // Fixed Business Header (Centered)
                // Immutable Snapshot Seller Header (Centered)
        canvas.drawText(invoice.sellerName, PAGE_WIDTH / 2f - boldPaint.measureText(invoice.sellerName) / 2, y, boldPaint)
        y += 20f
        canvas.drawText(PdfConstants.DEALS_IN, PAGE_WIDTH / 2f - textPaint.measureText(PdfConstants.DEALS_IN) / 2, y, textPaint)
        y += 15f
        canvas.drawText(invoice.sellerAddress, PAGE_WIDTH / 2f - textPaint.measureText(invoice.sellerAddress) / 2, y, textPaint)
        y += 15f
                val sellerGstinText = if (!invoice.sellerGSTIN.isNullOrBlank()) "GSTIN: ${invoice.sellerGSTIN}" else ""
        if (sellerGstinText.isNotEmpty()) {
             canvas.drawText(sellerGstinText, PAGE_WIDTH / 2f - textPaint.measureText(sellerGstinText) / 2, y, textPaint)
             y += 15f
        }
        y += 15f

        drawDivider(canvas, y)
        y += 30f

        // Dynamic Invoice Info (Left/Right)
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy").withZone(ZoneId.systemDefault())
        val rightMargin = PAGE_WIDTH - MARGIN

        val invNoText = "INVOICE NO: ${invoice.invoiceNumber}"
        val dateText = "DATE: ${dateFormatter.format(invoice.createdAt)}"
        canvas.drawText(invNoText, MARGIN, y, textPaint)
        canvas.drawText(dateText, rightMargin - textPaint.measureText(dateText), y, textPaint)
        y += 20f

        val supplyText = "SUPPLY TYPE: ${invoice.supplyType}"
        canvas.drawText(supplyText, MARGIN, y, textPaint)
        y += 30f

        // Buyer Info
        canvas.drawText("BILLED TO:", MARGIN, y, textPaint)
        y += 20f
        canvas.drawText("Name: ${invoice.customerName}", MARGIN, y, textPaint)
        y += 15f
        canvas.drawText("Address: ${invoice.buyerAddress}", MARGIN, y, textPaint)
        y += 15f

        val gstinStr = if (invoice.customerGSTIN.isNullOrBlank()) "Unregistered" else invoice.customerGSTIN
        canvas.drawText("GSTIN: $gstinStr", MARGIN, y, textPaint)
        y += 20f

        drawThinDivider(canvas, y)

        return y + 20f
    }

    private fun drawTableHeader(canvas: Canvas, startY: Float): Float {
        var y = startY

        drawThinDivider(canvas, y - 10f)

        canvas.drawText("| Item Description", MARGIN + 5f, y, boldPaint)
        canvas.drawText("| Qty", 250f, y, boldPaint)
        canvas.drawText("| Unit", 290f, y, boldPaint)
        canvas.drawText("| Rate (₹)", 340f, y, boldPaint)
        canvas.drawText("| GST %", 410f, y, boldPaint)
        canvas.drawText("| Total (₹)", PAGE_WIDTH - MARGIN - boldPaint.measureText("| Total (₹)") - 5f, y, boldPaint)

        y += 15f
        drawThinDivider(canvas, y)

        return y + 20f
    }

    private fun drawItemRow(canvas: Canvas, item: InvoiceItem, startY: Float): Float {
        var y = startY
        val colDescWidth = 190f

        // Text wrapping for description
        val words = item.description.split(" ")
        var currentLine = ""
        val lines = mutableListOf<String>()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (textPaint.measureText(testLine) < colDescWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        // Draw first line of description and the other columns
        if (lines.isNotEmpty()) {
            canvas.drawText("| ${lines[0]}", MARGIN + 5f, y, textPaint)
        }

        canvas.drawText("| ${item.quantity}", 250f, y, textPaint)
        val unitStr = if(item.unitType.isNotBlank()) item.unitType else "-"
        canvas.drawText("| $unitStr", 290f, y, textPaint)
        canvas.drawText("| ${String.format(Locale.US, "%.2f", item.pricePerUnit)}", 340f, y, textPaint)
        canvas.drawText("| ${item.gstPercentage}", 410f, y, textPaint)

        val totalStr = "| ${String.format(Locale.US, "%.2f", item.totalAmount)}"
        canvas.drawText(totalStr, PAGE_WIDTH - MARGIN - textPaint.measureText(totalStr) - 5f, y, textPaint)

        y += 20f

        // Draw remaining lines of description
        for (i in 1 until lines.size) {
            canvas.drawText("| ${lines[i]}", MARGIN + 5f, y, textPaint)
            y += 20f
        }

        return y
    }

    private fun drawTotals(canvas: Canvas, invoice: Invoice, startY: Float): Float {
        var y = startY

        drawThinDivider(canvas, y - 15f)
        y += 10f

        canvas.drawText("FINANCIAL SUMMARY:", MARGIN, y, boldPaint)
        y += 20f

        val rightMargin = PAGE_WIDTH - MARGIN - 5f

        val subtotalLabel = "Subtotal:"
        val subtotalVal = "₹ ${String.format(Locale.US, "%.2f", invoice.subtotal)}"
        canvas.drawText(subtotalLabel, MARGIN, y, textPaint)
        canvas.drawText(subtotalVal, rightMargin - textPaint.measureText(subtotalVal), y, textPaint)
        y += 20f

        val cgstLabel = "Add: CGST:"
        val cgstVal = "₹ ${String.format(Locale.US, "%.2f", invoice.totalCgst)}"
        canvas.drawText(cgstLabel, MARGIN, y, textPaint)
        canvas.drawText(cgstVal, rightMargin - textPaint.measureText(cgstVal), y, textPaint)
        y += 20f

        val sgstLabel = "Add: SGST:"
        val sgstVal = "₹ ${String.format(Locale.US, "%.2f", invoice.totalSgst)}"
        canvas.drawText(sgstLabel, MARGIN, y, textPaint)
        canvas.drawText(sgstVal, rightMargin - textPaint.measureText(sgstVal), y, textPaint)
        y += 20f

        val igstLabel = "Add: IGST:"
        val igstVal = "₹ ${String.format(Locale.US, "%.2f", invoice.totalIgst)}"
        canvas.drawText(igstLabel, MARGIN, y, textPaint)
        canvas.drawText(igstVal, rightMargin - textPaint.measureText(igstVal), y, textPaint)
        y += 15f

        // Separator line for Grand Total
        val grandTotalLine = "----------"
        canvas.drawText(grandTotalLine, rightMargin - textPaint.measureText(grandTotalLine), y, textPaint)
        y += 20f

        val grandTotalLabel = "GRAND TOTAL:"
        val grandTotalVal = "₹ ${String.format(Locale.US, "%.2f", invoice.totalAmount)}"
        canvas.drawText(grandTotalLabel, MARGIN, y, boldPaint)
        canvas.drawText(grandTotalVal, rightMargin - boldPaint.measureText(grandTotalVal), y, boldPaint)

        y += 15f
        drawDivider(canvas, y)

        return y + 30f
    }

    private fun drawAmountInWords(canvas: Canvas, amount: Double, startY: Float): Float {
        var y = startY
        val amountInWords = com.aktarjabed.inbusiness.utils.AmountInWordsConverter.convertAmountToWords(amount)
        val text = "Amount in Words: $amountInWords"

        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val words = text.split(" ")
        var currentLine = ""
        val lines = mutableListOf<String>()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (boldPaint.measureText(testLine) < maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        for (line in lines) {
            canvas.drawText(line, MARGIN, y, boldPaint)
            y += 20f
        }
        return y + 10f
    }

    private fun drawPaymentDetails(canvas: Canvas, invoice: Invoice, startY: Float): Float {
        var y = startY

        canvas.drawText("LEDGER / PAYMENT DETAILS:", MARGIN, y, boldPaint)
        y += 20f

        val rightMargin = PAGE_WIDTH - MARGIN - 5f

        val amountPaidLabel = "TOTAL AMOUNT PAID:"
        val amountPaidVal = "₹ ${String.format(Locale.US, "%.2f", invoice.amountPaid)}"
        canvas.drawText(amountPaidLabel, MARGIN, y, textPaint)
        canvas.drawText(amountPaidVal, rightMargin - textPaint.measureText(amountPaidVal), y, textPaint)
        y += 20f

        val balanceDueLabel = "BALANCE DUE (Debit):"
        val balanceDueVal = "₹ ${String.format(Locale.US, "%.2f", invoice.balanceDue)}"
        canvas.drawText(balanceDueLabel, MARGIN, y, textPaint)
        canvas.drawText(balanceDueVal, rightMargin - textPaint.measureText(balanceDueVal), y, textPaint)
        y += 20f

        val paymentMethodStr = "Payment Method: [ ${invoice.paymentMethod} ]"
        canvas.drawText(paymentMethodStr, MARGIN, y, textPaint)

        y += 15f
        drawDivider(canvas, y)

        return y + 30f
    }

    private fun drawTerms(canvas: Canvas, startY: Float): Float {
        var y = startY
        canvas.drawText("Terms & Conditions:", MARGIN, y, boldPaint)
        y += 20f
        canvas.drawText("1. Goods once sold will not be taken back.", MARGIN, y, smallTextPaint)
        y += 15f
        canvas.drawText("2. Interest @ 18% p.a. will be charged if payment is delayed.", MARGIN, y, smallTextPaint)
        return y + 20f
    }

    private fun drawFooter(canvas: Canvas) {
        val y = PAGE_HEIGHT - 80f

        val signatureLine = "__________________________________"
        canvas.drawText(signatureLine, PAGE_WIDTH - MARGIN - boldPaint.measureText(signatureLine), y, textPaint)

        canvas.drawText("Authorized Signatory", PAGE_WIDTH - MARGIN - textPaint.measureText("Authorized Signatory") - 30f, y + 20f, textPaint)
        canvas.drawText(PdfConstants.BUSINESS_NAME_SHORT, PAGE_WIDTH - MARGIN - textPaint.measureText(PdfConstants.BUSINESS_NAME_SHORT) - 20f, y + 40f, textPaint)
    }
}
