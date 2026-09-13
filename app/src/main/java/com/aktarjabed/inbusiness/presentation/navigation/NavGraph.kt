package com.aktarjabed.inbusiness.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aktarjabed.inbusiness.presentation.screens.CalculatorScreen
import com.aktarjabed.inbusiness.presentation.screens.DashboardScreen
import com.aktarjabed.inbusiness.presentation.screens.inventory.InventoryListScreen
import com.aktarjabed.inbusiness.presentation.screens.inventory.ProductEntryScreen
import com.aktarjabed.inbusiness.presentation.screens.invoice.InvoiceScreen
import com.aktarjabed.inbusiness.presentation.screens.invoice_preview.InvoicePreviewScreen
import com.aktarjabed.inbusiness.presentation.screens.SplashScreen
import com.aktarjabed.inbusiness.presentation.screens.SetupScreen

@Composable
fun InBusinessNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = NavigationRoutes.SPLASH) {
        composable(NavigationRoutes.SPLASH) {
            SplashScreen(
                onNavigateToDashboard = {
                    navController.navigate(NavigationRoutes.DASHBOARD) {
                        popUpTo(NavigationRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToSetup = {
                    navController.navigate(NavigationRoutes.SETUP) {
                        popUpTo(NavigationRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(NavigationRoutes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(NavigationRoutes.DASHBOARD) {
                        popUpTo(NavigationRoutes.SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(NavigationRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToCalculator = { navController.navigate(NavigationRoutes.CALCULATOR) },
                onNavigateToInvoice = { navController.navigate(NavigationRoutes.INVOICE) },
                onNavigateToInventory = { navController.navigate(NavigationRoutes.INVENTORY) }
            )
        }
        composable(NavigationRoutes.CALCULATOR) {
            CalculatorScreen()
        }
        composable(NavigationRoutes.INVOICE) {
            InvoiceScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUpgrade = { /* TODO: Navigate to upgrade screen */ },
                onNavigateToPreview = { invoiceId ->
                    navController.popBackStack()
                    navController.navigate(NavigationRoutes.invoicePreview(invoiceId))
                }
            )
        }
        composable(
            route = NavigationRoutes.INVOICE_PREVIEW,
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) {
            InvoicePreviewScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavigationRoutes.INVENTORY) {
            InventoryListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = { navController.navigate(NavigationRoutes.ADD_PRODUCT) },
                onNavigateToEditProduct = { productId -> navController.navigate(NavigationRoutes.editProduct(productId)) }
            )
        }
        composable(NavigationRoutes.ADD_PRODUCT) {
            ProductEntryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavigationRoutes.EDIT_PRODUCT,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId")
            ProductEntryScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}