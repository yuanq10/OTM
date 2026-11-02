package com.otm.ordertracker.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderFilter
import com.otm.ordertracker.data.repo.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class OrderListUiState(
    val loading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val filter: OrderFilter = OrderFilter.ALL,
    val counts: Map<OrderFilter, Int> = emptyMap(),
)

class OrderListViewModel(repository: OrderRepository) : ViewModel() {
    private val filter = MutableStateFlow(OrderFilter.ALL)

    val uiState: StateFlow<OrderListUiState> =
        combine(repository.orders(), filter) { orders, selected ->
            OrderListUiState(
                loading = false,
                orders = orders.filter(selected::matches),
                filter = selected,
                counts = OrderFilter.entries.associateWith { f -> orders.count(f::matches) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderListUiState())

    fun setFilter(value: OrderFilter) {
        filter.value = value
    }
}
