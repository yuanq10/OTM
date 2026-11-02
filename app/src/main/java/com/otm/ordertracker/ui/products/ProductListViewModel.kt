package com.otm.ordertracker.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Product
import com.otm.ordertracker.data.repo.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProductListUiState(
    val loading: Boolean = true,
    val products: List<Product> = emptyList(),
    val showArchived: Boolean = false,
)

class ProductListViewModel(private val repository: ProductRepository) : ViewModel() {
    private val showArchived = MutableStateFlow(false)

    val uiState: StateFlow<ProductListUiState> =
        combine(repository.products(), showArchived) { products, archived ->
            ProductListUiState(
                loading = false,
                products = products.filter { it.archived == archived },
                showArchived = archived,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    fun setShowArchived(show: Boolean) {
        showArchived.value = show
    }

    fun restore(id: String) = repository.setArchived(id, archived = false)
}
