package com.aktarjabed.inbusiness.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.presentation.components.MetricCard
import com.aktarjabed.inbusiness.presentation.screens.dashboard.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToCalculator: () -> Unit,
    onNavigateToInvoice: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToHistory: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
        }

        if (state.isLoading) {
            item {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            item {
                Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.refreshData() }) { Text("Retry") }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Today's Revenue",
                        value = "₹${"%.2f".format(state.todayRevenue)}",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    MetricCard(
                        title = "Total Revenue",
                        value = "₹${"%.2f".format(state.totalRevenue)}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Pending Dues",
                        value = "₹${"%.2f".format(state.pendingDues)}",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    MetricCard(
                        title = "Invoices Today",
                        value = "${state.invoicesToday}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Active Products",
                        value = "${state.activeProducts}",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    MetricCard(
                        title = "Low Stock",
                        value = "${state.lowStockCount}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // 7-day chart mockup mapping
            item {
                Text("7-Day Revenue", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                state.chartData.forEach { point ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(point.date.toString())
                        Text("₹${"%.2f".format(point.revenue)}")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        item {
            Button(
                onClick = onNavigateToInvoice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Invoice")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Invoice History")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateToInventory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Inventory")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNavigateToCalculator,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Calculator")
            }
        }
    }
}
