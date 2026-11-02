package com.otm.ordertracker.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.repo.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CustomerListUiState(
    val loading: Boolean = true,
    val customers: List<Customer> = emptyList(),
    val query: String = "",
    val showArchived: Boolean = false,
)

class CustomerListViewModel(private val repository: CustomerRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val showArchived = MutableStateFlow(false)

    val uiState: StateFlow<CustomerListUiState> =
        combine(repository.customers(), query, showArchived) { customers, q, archived ->
            CustomerListUiState(
                loading = false,
                customers = customers.filter { it.archived == archived && it.matches(q) },
                query = q,
                showArchived = archived,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerListUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun setShowArchived(show: Boolean) {
        showArchived.value = show
    }

    fun restore(id: String) = repository.setArchived(id, archived = false)
}

/** Case-insensitive match on full name or phone. */
fun Customer.matches(query: String): Boolean {
    val q = query.trim()
    return q.isEmpty() || fullName.contains(q, ignoreCase = true) || phone.contains(q)
}
