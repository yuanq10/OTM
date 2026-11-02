package com.otm.ordertracker.ui.products

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.repo.ProductRepository
import com.otm.ordertracker.ui.common.Money
import kotlinx.coroutines.launch

data class ProductForm(
    val name: String = "",
    val price: String = "",
    val onSale: Boolean = false,
    val salePrice: String = "",
    val nameError: String? = null,
    val priceError: String? = null,
    val salePriceError: String? = null,
)

class ProductEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository,
) : ViewModel() {
    /** Null when adding a new product. */
    val productId: String? = savedStateHandle["productId"]
    val isNew: Boolean get() = productId == null

    var form by mutableStateOf(ProductForm())
        private set
    var archived by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(productId == null)
        private set

    init {
        if (productId != null) {
            viewModelScope.launch {
                repository.product(productId).collect { product ->
                    if (product == null) return@collect
                    archived = product.archived
                    if (!loaded) {
                        form = ProductForm(
                            name = product.name,
                            price = Money.toInput(product.priceCents),
                            onSale = product.onSale,
                            salePrice = product.salePriceCents?.let(Money::toInput).orEmpty(),
                        )
                        loaded = true
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) { form = form.copy(name = value, nameError = null) }
    fun onPriceChange(value: String) { form = form.copy(price = value, priceError = null) }
    fun onSaleChange(value: Boolean) { form = form.copy(onSale = value, salePriceError = null) }
    fun onSalePriceChange(value: String) { form = form.copy(salePrice = value, salePriceError = null) }

    /** Validates and saves. Returns true when saved. */
    fun save(): Boolean {
        val price = Money.parse(form.price)
        val salePrice = if (form.onSale) Money.parse(form.salePrice) else null
        val nameError = if (form.name.isBlank()) "Enter a product name" else null
        val priceError = if (price == null) "Enter a valid price, e.g. 12.50" else null
        val salePriceError = when {
            !form.onSale -> null
            salePrice == null -> "Enter a valid sale price"
            price != null && salePrice >= price -> "Sale price must be lower than the regular price"
            else -> null
        }
        if (nameError != null || priceError != null || salePriceError != null || price == null) {
            form = form.copy(nameError = nameError, priceError = priceError, salePriceError = salePriceError)
            return false
        }

        if (productId == null) repository.add(form.name, price, salePrice)
        else repository.update(productId, form.name, price, salePrice)
        return true
    }

    fun markArchived(value: Boolean) {
        productId?.let { repository.setArchived(it, value) }
    }
}
