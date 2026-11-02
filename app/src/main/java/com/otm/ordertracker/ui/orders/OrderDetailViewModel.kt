package com.otm.ordertracker.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderRules
import com.otm.ordertracker.data.repo.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class OrderDetailUiState(
    val loading: Boolean = true,
    val order: Order? = null,
)

class OrderDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: OrderRepository,
) : ViewModel() {
    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    val uiState: StateFlow<OrderDetailUiState> =
        repository.order(orderId)
            .map { OrderDetailUiState(loading = false, order = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderDetailUiState())

    private val _message = MutableStateFlow<String?>(null)
    /** One-off error to show in a snackbar. */
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setPaid(paid: Boolean) = change { order, now -> OrderRules.setPaid(order, paid, now) }
    fun setShipped(shipped: Boolean) = change { order, now -> OrderRules.setShipped(order, shipped, now) }
    fun setReceived(received: Boolean) = change { order, now -> OrderRules.setReceived(order, received, now) }
    fun cancel() = change { order, now -> OrderRules.cancel(order, now) }

    fun messageShown() {
        _message.value = null
    }

    private fun change(transform: (Order, Long) -> Order) {
        val order = uiState.value.order ?: return
        try {
            repository.updateStatus(transform(order, System.currentTimeMillis()))
        } catch (e: IllegalStateException) {
            _message.value = e.message
        }
    }
}
