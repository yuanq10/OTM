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

enum class ProductFilter(val label: String) {
    ALL("All"),
    ON_SALE("On sale"),
    NOT_ON_SALE("Not on sale");

    fun matches(product: Product): Boolean = when (this) {
        ALL -> true
        ON_SALE -> product.onSale
        NOT_ON_SALE -> !product.onSale
    }
}

data class ProductListUiState(
    val loading: Boolean = true,
    val products: List<Product> = emptyList(),
    val showArchived: Boolean = false,
    val filter: ProductFilter = ProductFilter.ALL,
    /** Number of products per filter, among the active (or archived) products matching the search. */
    val counts: Map<ProductFilter, Int> = emptyMap(),
    val query: String = "",
)

class ProductListViewModel(private val repository: ProductRepository) : ViewModel() {
    private val showArchived = MutableStateFlow(false)
    private val filter = MutableStateFlow(ProductFilter.ALL)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<ProductListUiState> =
        combine(repository.products(), showArchived, filter, query) { products, archived, selected, q ->
            val shown = products.filter { it.archived == archived && it.name.contains(q.trim(), ignoreCase = true) }
            ProductListUiState(
                loading = false,
                products = shown.filter(selected::matches),
                showArchived = archived,
                filter = selected,
                counts = ProductFilter.entries.associateWith { f -> shown.count(f::matches) },
                query = q,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun setShowArchived(show: Boolean) {
        showArchived.value = show
    }

    fun setFilter(value: ProductFilter) {
        filter.value = value
    }

    fun restore(id: String) = repository.setArchived(id, archived = false)
}
