package com.otm.ordertracker.ui.orders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.model.OrderRules
import com.otm.ordertracker.data.model.Product
import com.otm.ordertracker.data.repo.CustomerRepository
import com.otm.ordertracker.data.repo.OrderRepository
import com.otm.ordertracker.data.repo.ProductRepository
import com.otm.ordertracker.ui.customers.matches
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class NewOrderUiState(
    val loading: Boolean = true,
    /** Active customers matching the search; archived customers are never offered. */
    val customerMatches: List<Customer> = emptyList(),
    val hasCustomers: Boolean = false,
    val selectedCustomer: Customer? = null,
    val customerQuery: String = "",
    /** Active products only. */
    val products: List<Product> = emptyList(),
    val quantities: Map<String, Int> = emptyMap(),
    val totalCents: Long = 0L,
) {
    val canSave: Boolean get() = selectedCustomer != null && quantities.values.any { it > 0 }
}

class NewOrderViewModel(
    savedStateHandle: SavedStateHandle,
    customerRepository: CustomerRepository,
    productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val selectedCustomerId = MutableStateFlow<String?>(savedStateHandle["customerId"])
    private val quantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val customerQuery = MutableStateFlow("")

    var notes by mutableStateOf("")
        private set

    val uiState: StateFlow<NewOrderUiState> = combine(
        customerRepository.customers(),
        productRepository.products(),
        selectedCustomerId,
        quantities,
        customerQuery,
    ) { customers, products, customerId, qty, query ->
        val activeCustomers = customers.filterNot { it.archived }
        val activeProducts = products.filterNot { it.archived }
        // Drop quantities for products that were archived while this screen was open.
        val validQty = qty.filterKeys { id -> activeProducts.any { it.id == id } }
        NewOrderUiState(
            loading = false,
            customerMatches = activeCustomers.filter { it.matches(query) },
            hasCustomers = activeCustomers.isNotEmpty(),
            selectedCustomer = activeCustomers.firstOrNull { it.id == customerId },
            customerQuery = query,
            products = activeProducts,
            quantities = validQty,
            totalCents = OrderRules.total(OrderRules.buildItems(activeProducts, validQty)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewOrderUiState())

    fun selectCustomer(id: String?) {
        selectedCustomerId.value = id
    }

    fun onCustomerQueryChange(value: String) {
        customerQuery.value = value
    }

    fun setQuantity(productId: String, quantity: Int) {
        quantities.update { it + (productId to quantity.coerceIn(0, MAX_QUANTITY)) }
    }

    fun onNotesChange(value: String) {
        notes = value
    }

    /** Creates the order with prices locked in at their current values. Returns the new order's id. */
    fun save(): String? {
        val state = uiState.value
        val customer = state.selectedCustomer ?: return null
        val items = OrderRules.buildItems(state.products, state.quantities)
        if (items.isEmpty()) return null
        return orderRepository.create(customer, items, notes)
    }

    private companion object {
        const val MAX_QUANTITY = 999
    }
}
