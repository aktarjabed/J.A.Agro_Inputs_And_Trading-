package com.aktarjabed.inbusiness.presentation.screens.invoice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.data.repository.ProductRepository
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.usecase.CreateInvoiceUseCase
import com.aktarjabed.inbusiness.domain.usecase.GetProductSuggestionsUseCase
import com.aktarjabed.inbusiness.domain.usecase.ProductSuggestion
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.invoice.CalculateInvoiceTotalsUseCase
import com.aktarjabed.inbusiness.domain.usecase.DetermineSupplyTypeUseCase
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCalculationResult
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val quotaGate: QuotaGate,
    private val createInvoiceUseCase: CreateInvoiceUseCase,
    private val calculateInvoiceTotalsUseCase: CalculateInvoiceTotalsUseCase,
    private val determineSupplyTypeUseCase: DetermineSupplyTypeUseCase,
    private val businessContext: BusinessContext,
    private val productRepository: ProductRepository,
    private val businessRepository: BusinessRepository,
    private val getProductSuggestionsUseCase: GetProductSuggestionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvoiceUiState>(InvoiceUiState.Initial)
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    // UI state for inputs
    val customerName = MutableStateFlow("")
    val customerGSTIN = MutableStateFlow("")
    val buyerAddress = MutableStateFlow("")

    val sellerName = MutableStateFlow("")
    val sellerAddress = MutableStateFlow("")
    val sellerGstin = MutableStateFlow("")
    val supplyType = MutableStateFlow(SupplyType.UNKNOWN)

    // Items state
    private val _invoiceItems = MutableStateFlow<List<InvoiceItemInput>>(emptyList())
    private var editingItemIndex: Int? = null
    val invoiceItems = _invoiceItems.asStateFlow()

    private val _calculationResult = MutableStateFlow<InvoiceCalculationResult?>(null)
    val calculationResult: StateFlow<InvoiceCalculationResult?> = _calculationResult.asStateFlow()

    // Products for dropdown
    private val _productSuggestions = MutableStateFlow<List<ProductSuggestion>>(emptyList())
    val productSuggestions = _productSuggestions.asStateFlow()

    val amountPaid = MutableStateFlow(0.0)
    val paymentMethod = MutableStateFlow("NONE")

    // Error message for surfacing calculation errors to the UI
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentIdempotencyKey: String? = null

    init {
        // Load product suggestions for autocomplete
        viewModelScope.launch {
            getProductSuggestionsUseCase().collect { suggestionsList ->
                _productSuggestions.value = suggestionsList
            }
        }

        // Observe business data for Seller GSTIN
        viewModelScope.launch {
            try {
                val currentBusinessId = businessContext.activeBusinessId.first()
                val businessData = businessRepository.getBusinessDataById(currentBusinessId)
                if (businessData != null) {
                    sellerName.value = businessData.name
                    sellerAddress.value = businessData.address
                    if (!businessData.gstin.isNullOrBlank()) {
                        sellerGstin.value = businessData.gstin
                    }

                    // Re-evaluate supply type if customer GSTIN is already provided
                    if (customerGSTIN.value.isNotBlank() && sellerGstin.value.isNotBlank()) {
                        supplyType.value = determineSupplyTypeUseCase(sellerGstin.value, customerGSTIN.value)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Failed to load business data for GSTIN", e)
            }
        }
    }

    fun checkQuotaAndPrepare() {
        viewModelScope.launch {
            _uiState.value = InvoiceUiState.Loading

            try {
                val currentUserId = businessContext.currentUserId.first()

                // Peek without consuming
                val verdict = quotaGate.assertQuota(currentUserId, consume = false)

                when (verdict) {
                    is QuotaVerdict.Allowed -> {
                        val nextInvoiceNumber = "Invoice number will be assigned when saved"
                        _uiState.value = InvoiceUiState.CreateAllowed(
                            remainingToday = verdict.remaining,
                            invoiceNumber = nextInvoiceNumber
                        )
                    }
                    is QuotaVerdict.DailyCap -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                    is QuotaVerdict.MonthlyCap -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                    is QuotaVerdict.FreeExpired -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error checking quota", e)
                _uiState.value = InvoiceUiState.Error("Failed to check quota: ${e.message}")
            }
        }
    }

    fun updateCustomerData(name: String, gstin: String, address: String) {
        customerName.value = name
        customerGSTIN.value = gstin
        buyerAddress.value = address

        // Auto-detect supply type
        if (sellerGstin.value.isNotBlank() && gstin.isNotBlank()) {
            supplyType.value = determineSupplyTypeUseCase(sellerGstin.value, gstin)
        }
        recalculateItems()
    }


    fun setAmountPaid(amount: Double) {
        val normalized = if (amount.isFinite()) java.math.BigDecimal(amount.toString()).setScale(2, java.math.RoundingMode.HALF_UP).toDouble() else 0.0
        amountPaid.value = normalized
        recalculateItems()
    }

    fun setPaymentMethod(method: String) {
        paymentMethod.value = method
    }

    fun setSupplyType(type: SupplyType) {
        supplyType.value = type
        recalculateItems()
    }


    fun setEditingItemIndex(index: Int?) {
        editingItemIndex = index
    }

    fun addItem(item: InvoiceItemInput) {
        val currentItems = _invoiceItems.value.toMutableList()
        val index = editingItemIndex
        if (index != null && index in currentItems.indices) {
            currentItems[index] = item
        } else {
            currentItems.add(item)
        }
        editingItemIndex = null
        _invoiceItems.value = currentItems
        recalculateItems()
    }

    fun removeItem(index: Int) {
        val currentItems = _invoiceItems.value.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _invoiceItems.value = currentItems
            recalculateItems()
        }
    }

    private fun recalculateItems() {
        val currentSupplyType = supplyType.value
        if (currentSupplyType == SupplyType.UNKNOWN) {
            _calculationResult.value = null
            return
        }

        try {
            val validInputs = _invoiceItems.value.filter { input ->
                val isBlank = input.description.isBlank() && input.quantity == 0.0 && input.pricePerUnit == 0.0 && input.gstPercentage == 0.0
                if (isBlank) return@filter false

                if (input.description.isBlank() || input.quantity <= 0 || input.pricePerUnit < 0 || input.gstPercentage < 0) {
                    throw IllegalArgumentException("Partially filled or invalid item found: ${input.description}")
                }
                true
            }

            val rawItems = validInputs.map { input ->
                InvoiceItem(
                    description = input.description,
                    quantity = input.quantity,
                    pricePerUnit = input.pricePerUnit,
                    unitType = input.unitType,
                    gstPercentage = input.gstPercentage,
                    productId = input.productId
                )
            }
            if (rawItems.isNotEmpty()) {
                val calcResult = calculateInvoiceTotalsUseCase(
                    items = rawItems,
                    supplyType = currentSupplyType,
                    amountPaid = amountPaid.value
                )
                _calculationResult.value = calcResult
                _errorMessage.value = null
            } else {
                _calculationResult.value = null
                _errorMessage.value = null
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Calculation error", e)
            _calculationResult.value = null
            _errorMessage.value = e.message ?: "Calculation failed"
        }
    }

    fun createInvoice() {
        viewModelScope.launch {
            val validInputs = _invoiceItems.value.filter { input ->
                val isBlank = input.description.isBlank() && input.quantity == 0.0 && input.pricePerUnit == 0.0 && input.gstPercentage == 0.0
                if (!isBlank && (input.description.isBlank() || input.quantity <= 0 || input.pricePerUnit < 0 || input.gstPercentage < 0)) {
                    _uiState.value = InvoiceUiState.Error("Partially filled or invalid item found")
                    return@launch
                }
                !isBlank
            }
            if (validInputs.isEmpty()) {
                _uiState.value = InvoiceUiState.Error("Please add at least one item")
                return@launch
            }

            if (supplyType.value == SupplyType.UNKNOWN) {
                _uiState.value = InvoiceUiState.Error("Please select a valid Supply Type (Intra/Inter State)")
                return@launch
            }

            _uiState.value = InvoiceUiState.Loading

            try {
                val idempotencyKey = currentIdempotencyKey ?: java.util.UUID.randomUUID().toString().also { currentIdempotencyKey = it }

                val rawItems = validInputs.map { input ->
                    InvoiceItem(
                        description = input.description,
                        quantity = input.quantity,
                        pricePerUnit = input.pricePerUnit,
                        unitType = input.unitType,
                        gstPercentage = input.gstPercentage,
                        productId = input.productId
                    )
                }

                val result = createInvoiceUseCase(
                    customerName = customerName.value,
                    customerGSTIN = customerGSTIN.value,
                    buyerAddress = buyerAddress.value,
                    supplyType = supplyType.value,
                    items = rawItems,
                    idempotencyKey = idempotencyKey,
                    amountPaid = amountPaid.value,
                    paymentMethod = paymentMethod.value
                )

                when(result) {
                    is InvoiceCreationResult.Success -> {
                        _uiState.value = InvoiceUiState.Success(
                            invoiceId = result.invoiceId,
                            message = "Invoice ${result.invoiceNumber} created successfully"
                        )
                    }
                    is InvoiceCreationResult.IdempotentReplay -> {
                         _uiState.value = InvoiceUiState.Success(
                            invoiceId = result.invoiceId,
                            message = "Invoice already created"
                        )
                    }
                    is InvoiceCreationResult.QuotaExceeded -> {
                         _uiState.value = InvoiceUiState.Error("Quota Exceeded") // Using Error for now
                    }
                    is InvoiceCreationResult.InsufficientStock -> {
                         _uiState.value = InvoiceUiState.Error("Insufficient stock for ${result.productName}. Requested: ${result.requested}, Available: ${result.available}")
                    }
                    is InvoiceCreationResult.ProductNotFound -> {
                        _uiState.value = InvoiceUiState.Error("Product not found")
                    }
                    is InvoiceCreationResult.InvalidRequest -> {
                        _uiState.value = InvoiceUiState.Error(result.message)
                    }
                    is InvoiceCreationResult.UnexpectedFailure -> {
                        _uiState.value = InvoiceUiState.Error("Failed to create invoice: ${result.cause.message}")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error creating invoice", e)
                _uiState.value = InvoiceUiState.Error("Failed to create invoice: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = InvoiceUiState.Initial
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetIdempotencyKey() {
        currentIdempotencyKey = null
    }

    companion object {
        private const val TAG = "InvoiceViewModel"
    }
}

// Temporary data class for UI input before mapping to domain InvoiceItem
data class InvoiceItemInput(
    val description: String,
    val quantity: Double,
    val pricePerUnit: Double,
    val gstPercentage: Double,
    val unitType: String = "",
    val productId: Long? = null // null for ad-hoc
) {
    val isAdHoc: Boolean get() = productId == null
}
