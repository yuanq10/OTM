package com.otm.ordertracker.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.model.DateRange
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderFilter
import com.otm.ordertracker.data.model.OrderSearch
import com.otm.ordertracker.data.model.OrderTab
import com.otm.ordertracker.data.model.Product
import com.otm.ordertracker.data.model.SearchSelection
import com.otm.ordertracker.data.repo.CustomerRepository
import com.otm.ordertracker.data.repo.OrderRepository
import com.otm.ordertracker.data.repo.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class OrderListUiState(
    val loading: Boolean = true,
    val tab: OrderTab = OrderTab.IN_PROGRESS,
    /** Orders shown in the current tab, after search, status filter and date range. */
    val orders: List<Order> = emptyList(),
    /** Number of orders in each tab that match the search. */
    val tabCounts: Map<OrderTab, Int> = emptyMap(),
    /** Status filter, used in the In progress tab. */
    val filter: OrderFilter = OrderFilter.ALL,
    /** Number of in-progress orders per status filter, among orders matching the search. */
    val counts: Map<OrderFilter, Int> = emptyMap(),
    /** Completion date range, used in the Completed tab. */
    val dateRange: DateRange? = null,
    val query: String = "",
    val selection: SearchSelection? = null,
    /** Customers and products whose name matches the query, offered as "show all orders for…" shortcuts. */
    val customerSuggestions: List<Customer> = emptyList(),
    val productSuggestions: List<Product> = emptyList(),
) {
    val isSearching: Boolean get() = query.isNotBlank() || selection != null
}

private data class SearchInput(
    val query: String,
    val selection: SearchSelection?,
    val tab: OrderTab,
    val filter: OrderFilter,
    val dateRange: DateRange?,
)

class OrderListViewModel(
    orderRepository: OrderRepository,
    customerRepository: CustomerRepository,
    productRepository: ProductRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selection = MutableStateFlow<SearchSelection?>(null)
    private val tab = MutableStateFlow(OrderTab.IN_PROGRESS)
    private val filter = MutableStateFlow(OrderFilter.ALL)
    private val dateRange = MutableStateFlow<DateRange?>(null)

    private val searchInput = combine(query, selection, tab, filter, dateRange, ::SearchInput)

    val uiState: StateFlow<OrderListUiState> = combine(
        orderRepository.orders(),
        customerRepository.customers(),
        productRepository.products(),
        searchInput,
    ) { orders, customers, products, input ->
        val customerNames = customers.associate { it.id to it.fullName }
        val productNames = products.associate { it.id to it.name }
        val searched = orders.filter {
            OrderSearch.matches(it, input.query, input.selection, customerNames, productNames)
        }
        val inProgress = searched.filter(OrderTab.IN_PROGRESS::matches)
        val shown = when (input.tab) {
            OrderTab.IN_PROGRESS -> inProgress.filter(input.filter::matches)
            OrderTab.COMPLETED -> searched
                .filter { OrderTab.COMPLETED.matches(it) && OrderSearch.matches(it, "", null, completedIn = input.dateRange) }
                .sortedByDescending { it.completedAt }
        }
        val q = input.query.trim()
        val suggest = q.isNotEmpty() && input.selection == null
        OrderListUiState(
            loading = false,
            tab = input.tab,
            orders = shown,
            tabCounts = OrderTab.entries.associateWith { t -> searched.count(t::matches) },
            filter = input.filter,
            counts = OrderFilter.entries.associateWith { f -> inProgress.count(f::matches) },
            dateRange = input.dateRange,
            query = input.query,
            selection = input.selection,
            customerSuggestions = if (suggest) customers.filter { it.fullName.contains(q, ignoreCase = true) }.take(MAX_SUGGESTIONS) else emptyList(),
            productSuggestions = if (suggest) products.filter { it.name.contains(q, ignoreCase = true) }.take(MAX_SUGGESTIONS) else emptyList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderListUiState())

    fun setTab(value: OrderTab) {
        tab.value = value
    }

    fun setFilter(value: OrderFilter) {
        filter.value = value
    }

    fun setDateRange(value: DateRange?) {
        dateRange.value = value
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Shows all orders for a customer or containing a product, replacing the typed text. */
    fun select(value: SearchSelection) {
        selection.value = value
        query.value = ""
    }

    fun clearSearch() {
        selection.value = null
        query.value = ""
    }

    private companion object {
        const val MAX_SUGGESTIONS = 5
    }
}
