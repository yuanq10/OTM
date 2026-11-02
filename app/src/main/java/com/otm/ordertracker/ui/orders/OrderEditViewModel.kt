package com.otm.ordertracker.ui.orders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderItem
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A product row on the order form. Lines already on the order being edited are [locked]: they keep the
 * name and price they were ordered at. [product] is the current product, if it still exists.
 */
data class ProductLine(
    val productId: String,
    val name: String,
    val unitPriceCents: Long,
    val locked: Boolean,
    val product: Product?,
)

data class OrderEditUiState(
    val loading: Boolean = true,
    /** Active customers matching the search; archived customers are never offered. */
    val customerMatches: List<Customer> = emptyList(),
    val hasCustomers: Boolean = false,
    val selectedCustomer: Customer? = null,
    val customerQuery: String = "",
    /** The order's existing lines first (when editing), then all other active products. */
    val lines: List<ProductLine> = emptyList(),
    val quantities: Map<String, Int> = emptyMap(),
    val totalCents: Long = 0L,
) {
    val canSave: Boolean get() = selectedCustomer != null && quantities.values.any { it > 0 }
}

private data class Selection(val customerId: String?, val quantities: Map<String, Int>, val query: String)

/** Creates a new order, or edits an existing one when an "orderId" navigation argument is given. */
class OrderEditViewModel(
    savedStateHandle: SavedStateHandle,
    customerRepository: CustomerRepository,
    productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    /** Null when creating a new order. */
    val orderId: String? = savedStateHandle["orderId"]
    val isNew: Boolean get() = orderId == null

    private val original = MutableStateFlow<Order?>(null)
    private val loaded = MutableStateFlow(orderId == null)
    private val selectedCustomerId = MutableStateFlow<String?>(savedStateHandle["customerId"])
    private val quantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val customerQuery = MutableStateFlow("")

    var name by mutableStateOf("")
        private set
    var notes by mutableStateOf("")
        private set

    init {
        if (orderId != null) {
            viewModelScope.launch {
                orderRepository.order(orderId).first()?.let { order ->
                    original.value = order
                    selectedCustomerId.value = order.customerId
                    quantities.value = order.items.associate { it.productId to it.quantity }
                    name = order.name
                    notes = order.notes
                }
                loaded.value = true
            }
        }
    }

    private val selection = combine(selectedCustomerId, quantities, customerQuery, ::Selection)

    val uiState: StateFlow<OrderEditUiState> = combine(
        customerRepository.customers(),
        productRepository.products(),
        original,
        loaded,
        selection,
    ) { customers, products, order, isLoaded, sel ->
        val activeCustomers = customers.filterNot { it.archived }
        val activeProducts = products.filterNot { it.archived }
        val existingItems = order?.items.orEmpty()
        val productsById = products.associateBy { it.id }
        val existingIds = existingItems.map { it.productId }.toSet()

        val lines = existingItems.map {
            ProductLine(it.productId, it.productName, it.unitPriceCents, locked = true, product = productsById[it.productId])
        } + activeProducts.filter { it.id !in existingIds }.map {
            ProductLine(it.id, it.name, it.effectivePriceCents, locked = false, product = it)
        }
        val lineIds = lines.map { it.productId }.toSet()
        // Drop quantities for products that were archived while this screen was open.
        val validQty = sel.quantities.filterKeys { it in lineIds }
        // The order's current customer stays selectable even if they've since been removed.
        val selectable = activeCustomers + customers.filter { it.archived && it.id == order?.customerId }

        OrderEditUiState(
            loading = !isLoaded,
            customerMatches = activeCustomers.filter { it.matches(sel.query) },
            hasCustomers = activeCustomers.isNotEmpty(),
            selectedCustomer = selectable.firstOrNull { it.id == sel.customerId },
            customerQuery = sel.query,
            lines = lines,
            quantities = validQty,
            totalCents = OrderRules.total(OrderRules.buildItems(activeProducts, validQty, existingItems)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderEditUiState())

    fun selectCustomer(id: String?) {
        selectedCustomerId.value = id
    }

    fun onCustomerQueryChange(value: String) {
        customerQuery.value = value
    }

    fun setQuantity(productId: String, quantity: Int) {
        quantities.update { it + (productId to quantity.coerceIn(0, MAX_QUANTITY)) }
    }

    fun onNameChange(value: String) {
        name = value
    }

    fun onNotesChange(value: String) {
        notes = value
    }

    /** Creates or updates the order. Returns the order's id, or null if it can't be saved yet. */
    fun save(): String? {
        val state = uiState.value
        val customer = state.selectedCustomer ?: return null
        val order = original.value
        val activeProducts = state.lines.mapNotNull { line -> line.product?.takeIf { !line.locked } }
        val items: List<OrderItem> = OrderRules.buildItems(activeProducts, state.quantities, order?.items.orEmpty())
        if (items.isEmpty()) return null

        if (order == null) {
            // Editing an order that no longer exists: don't silently create a new one.
            return if (isNew) orderRepository.create(customer, items, name, notes) else null
        }
        // Keep the name the order was placed under unless the customer was changed.
        val customerName = if (customer.id == order.customerId) order.customerName else customer.fullName
        orderRepository.updateDetails(order.id, customer.id, customerName, items, name, notes)
        return order.id
    }

    private companion object {
        const val MAX_QUANTITY = 999
    }
}
