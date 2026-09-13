package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import com.aktarjabed.inbusiness.presentation.components.LoadingScreen
import com.aktarjabed.inbusiness.presentation.components.QuotaBlockedDialog
import com.aktarjabed.inbusiness.presentation.components.QuotaWarningBanner
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    viewModel: InvoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    val customerGSTIN by viewModel.customerGSTIN.collectAsState()
    val buyerAddress by viewModel.buyerAddress.collectAsState()
    val supplyType by viewModel.supplyType.collectAsState()
    val items by viewModel.invoiceItems.collectAsState()
    val calculationResult by viewModel.calculationResult.collectAsState()
    val productSuggestions by viewModel.productSuggestions.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItemInput by remember { mutableStateOf<InvoiceItemInput?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.checkQuotaAndPrepare()
    }

    // Show Snackbar when a calculation error occurs
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is InvoiceUiState.Success) {
            val invoiceId = (uiState as InvoiceUiState.Success).invoiceId
            delay(1500)
            viewModel.resetIdempotencyKey() // Reset for the next invoice if we come back
            onNavigateToPreview(invoiceId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Invoice") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is InvoiceUiState.CreateAllowed) {
                FloatingActionButton(onClick = { editingItemInput = null
                        showAddItemDialog = true }) {
                    Icon(Icons.Default.Add, "Add Item")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is InvoiceUiState.Initial,
                is InvoiceUiState.Loading -> LoadingScreen(message = "Processing...")

                is InvoiceUiState.CreateAllowed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (state.remainingToday <= 5) {
                            QuotaWarningBanner(
                                remaining = state.remainingToday,
                                onUpgrade = onNavigateToUpgrade
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text("Invoice No: ${state.invoiceNumber}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Customer Details Section
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Buyer Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { viewModel.updateCustomerData(it, customerGSTIN, buyerAddress) },
                                    label = { Text("Customer Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = customerGSTIN,
                                    onValueChange = { viewModel.updateCustomerData(customerName, it, buyerAddress) },
                                    label = { Text("Customer GSTIN (Optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = buyerAddress,
                                    onValueChange = { viewModel.updateCustomerData(customerName, customerGSTIN, it) },
                                    label = { Text("Billing Address") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Supply Type Selection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Supply Type:", modifier = Modifier.weight(1f))
                            SegmentedSupplyTypeButton(
                                selected = supplyType,
                                onSelect = { viewModel.setSupplyType(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Items List
                        Text("Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        if (items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No items added. Click + to add.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            val processedItems = calculationResult?.processedItems ?: emptyList()
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(items) { index, itemInput ->
                                    val processedItem = processedItems.getOrNull(index)
                                    InvoiceItemCard(
                                        itemInput = itemInput,
                                        processedItem = processedItem,
                                        onEdit = {
                                        viewModel.setEditingItemIndex(index)
                                        editingItemInput = itemInput
                                        showAddItemDialog = true
                                    },
                                    onRemove = { viewModel.removeItem(index) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Payment Details
                        val amountPaid by viewModel.amountPaid.collectAsState()
                        val paymentMethod by viewModel.paymentMethod.collectAsState()
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Payment Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    var amountStr by remember { mutableStateOf(if (amountPaid > 0) amountPaid.toString() else "") }
                                    OutlinedTextField(
                                        value = amountStr,
                                        onValueChange = {
                                            amountStr = it
                                            val d = it.toDoubleOrNull()
                                            if (d != null) {
                                                viewModel.setAmountPaid(d)
                                            } else if (it.isBlank()) {
                                                viewModel.setAmountPaid(0.0)
                                            }
                                        },
                                        label = { Text("Amount Paid (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )

                                    var expanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = !expanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = paymentMethod,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Method") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            listOf("NONE", "CASH", "CARD", "UPI", "BANK_TRANSFER").forEach { method ->
                                                DropdownMenuItem(
                                                    text = { Text(method) },
                                                    onClick = {
                                                        viewModel.paymentMethod.value = method
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Totals & Submit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val formattedTotal = String.format(java.util.Locale.US, "%.2f", calculationResult?.totalAmount ?: 0.0)
                            Text("Grand Total: ₹$formattedTotal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Button(
                                onClick = { viewModel.createInvoice() },
                                enabled = customerName.isNotBlank() && items.isNotEmpty() && supplyType != SupplyType.UNKNOWN
                            ) {
                                Text("Create Invoice")
                            }
                        }
                    }
                }

                is InvoiceUiState.QuotaBlocked -> {
                    QuotaBlockedDialog(
                        verdict = state.verdict,
                        onUpgrade = onNavigateToUpgrade,
                        onDismiss = onNavigateBack
                    )
                }

                is InvoiceUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(state.message, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                is InvoiceUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetState(); viewModel.checkQuotaAndPrepare() }) {
                            Text("Try again")
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddItemDialog(
            suggestions = productSuggestions,
            initialItem = editingItemInput,
            onDismiss = {
                showAddItemDialog = false
                viewModel.setEditingItemIndex(null)
                editingItemInput = null
            },
            onAdd = { newItem ->
                viewModel.addItem(newItem)
                showAddItemDialog = false
                editingItemInput = null
            }
        )
    }
}

@Composable
fun SegmentedSupplyTypeButton(
    selected: SupplyType,
    onSelect: (SupplyType) -> Unit
) {
    Row {
        OutlinedButton(
            onClick = { onSelect(SupplyType.INTRA_STATE) },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selected == SupplyType.INTRA_STATE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Text("Intra-state (CGST/SGST)")
        }
        OutlinedButton(
            onClick = { onSelect(SupplyType.INTER_STATE) },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selected == SupplyType.INTER_STATE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            )
        ) {
            Text("Inter-state (IGST)")
        }
    }
}

@Composable
fun InvoiceItemCard(itemInput: InvoiceItemInput, processedItem: com.aktarjabed.inbusiness.data.entities.InvoiceItem?, onEdit: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(itemInput.description, fontWeight = FontWeight.Bold)
                    if (itemInput.isAdHoc) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Text("Ad-hoc", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("Qty: ${itemInput.quantity} x ₹${itemInput.pricePerUnit}", style = MaterialTheme.typography.bodySmall)
                val taxStr = String.format(java.util.Locale.US, "%.2f", processedItem?.taxAmount ?: 0.0)
                Text("GST: ${itemInput.gstPercentage}% (₹$taxStr)", style = MaterialTheme.typography.bodySmall)
            }

            Column(horizontalAlignment = Alignment.End) {
                val totStr = String.format(java.util.Locale.US, "%.2f", processedItem?.totalAmount ?: 0.0)
                Text("₹$totStr", fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    suggestions: List<com.aktarjabed.inbusiness.domain.usecase.ProductSuggestion>,
    initialItem: InvoiceItemInput?,
    onDismiss: () -> Unit,
    onAdd: (InvoiceItemInput) -> Unit
) {
    var selectedProductId by remember { mutableStateOf<Long?>(initialItem?.productId) }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var quantity by remember { mutableStateOf(initialItem?.quantity?.let { if (it > 0) it.toString() else "" } ?: "") }
    var unitType by remember { mutableStateOf(initialItem?.unitType ?: "") }
    var price by remember { mutableStateOf(initialItem?.pricePerUnit?.let { if (it > 0) it.toString() else "" } ?: "") }
    var gstRate by remember { mutableStateOf(initialItem?.gstPercentage?.let { if (it > 0) it.toString() else "" } ?: "") }

    var expanded by remember { mutableStateOf(false) }

    val isInputValid = description.isNotBlank() && quantity.toDoubleOrNull() != null && quantity.toDoubleOrNull()!! > 0 && price.toDoubleOrNull() != null && price.toDoubleOrNull()!! >= 0 && gstRate.toDoubleOrNull() != null && gstRate.toDoubleOrNull()!! >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "Add Line Item" else "Edit Line Item") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            selectedProductId = null // User manually typing unlinks from catalog
                            expanded = true // Show suggestions as they type
                        },
                        label = { Text("Item Description (Type to search)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        isError = description.isBlank()
                    )

                    val filteredSuggestions = suggestions.filter {
                        it.description.contains(description, ignoreCase = true)
                    }

                    if (filteredSuggestions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteredSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = {
                                        val sourceText = if (suggestion.product != null) {
                                            "${suggestion.description} (Catalog - Stock: ${suggestion.product.availableStock})"
                                        } else {
                                            "${suggestion.description} (History)"
                                        }
                                        Text(sourceText)
                                    },
                                    onClick = {
                                        description = suggestion.description
                                        unitType = suggestion.unitType
                                        price = suggestion.pricePerUnit.toString()
                                        gstRate = suggestion.gstPercentage.toString()
                                        selectedProductId = suggestion.product?.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        isError = quantity.toDoubleOrNull() == null || quantity.toDoubleOrNull()!! <= 0
                    )
                    OutlinedTextField(
                        value = unitType,
                        onValueChange = { unitType = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Unit Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        isError = price.toDoubleOrNull() == null || price.toDoubleOrNull()!! < 0
                    )
                    OutlinedTextField(
                        value = gstRate,
                        onValueChange = { gstRate = it },
                        label = { Text("GST Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        isError = gstRate.toDoubleOrNull() == null || gstRate.toDoubleOrNull()!! < 0
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantity.toDoubleOrNull()
                    val p = price.toDoubleOrNull()
                    val g = gstRate.toDoubleOrNull()
                    if (q != null && p != null && g != null && description.isNotBlank()) {
                        onAdd(
                            InvoiceItemInput(
                                description = description.trim(),
                                quantity = q,
                                pricePerUnit = p,
                                unitType = unitType.trim(),
                                gstPercentage = g,
                                productId = selectedProductId
                            )
                        )
                    }
                },
                enabled = isInputValid
            ) {
                Text(if (initialItem == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
