package com.otm.ordertracker.ui.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderStatus
import com.otm.ordertracker.data.repo.CustomerRepository
import com.otm.ordertracker.data.repo.OrderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CustomerDetailUiState(
    val loading: Boolean = true,
    val customer: Customer? = null,
    val orders: List<Order> = emptyList(),
) {
    private val activeOrders get() = orders.filterNot { it.isCanceled }
    val orderCount: Int get() = activeOrders.size
    val unpaidCount: Int get() = activeOrders.count { !it.has(OrderStatus.PAID_TO_ME) }
    val notReceivedCount: Int get() = activeOrders.count { !it.has(OrderStatus.RECEIVED) }
    val totalSpentCents: Long get() = activeOrders.sumOf { it.totalCents }
}

class CustomerDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val customerRepository: CustomerRepository,
    orderRepository: OrderRepository,
) : ViewModel() {
    val customerId: String = checkNotNull(savedStateHandle["customerId"])

    val uiState: StateFlow<CustomerDetailUiState> =
        combine(
            customerRepository.customer(customerId),
            orderRepository.ordersForCustomer(customerId),
        ) { customer, orders ->
            CustomerDetailUiState(loading = false, customer = customer, orders = orders)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerDetailUiState())

    fun setArchived(archived: Boolean) = customerRepository.setArchived(customerId, archived)
}
