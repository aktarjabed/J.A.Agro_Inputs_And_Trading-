package com.aktarjabed.inbusiness.presentation.navigation

/**
 * Centralized navigation route constants.
 * Eliminates hardcoded route strings and reduces typo risk.
 */
object NavigationRoutes {
    const val SPLASH = "splash"
    const val SETUP = "setup"
    const val DASHBOARD = "dashboard"
    const val CALCULATOR = "calculator"
    const val INVOICE = "invoice"
    const val INVOICE_PREVIEW = "invoice-preview/{invoiceId}"
    const val INVENTORY = "inventory"
    const val ADD_PRODUCT = "addProduct"
    const val EDIT_PRODUCT = "editProduct/{productId}"

    // Helper functions for parameterized routes
    fun invoicePreview(invoiceId: String) = "invoice-preview/$invoiceId"
    fun editProduct(productId: Long) = "editProduct/$productId"
}
