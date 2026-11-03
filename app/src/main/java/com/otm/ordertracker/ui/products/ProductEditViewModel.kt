package com.otm.ordertracker.ui.products


import com.otm.ordertracker.R
import androidx.annotation.StringRes
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
    val inStock: Boolean = true,
    @StringRes val nameError: Int? = null,
    @StringRes val priceError: Int? = null,
    @StringRes val salePriceError: Int? = null,
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
                            inStock = product.inStock,
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
    fun onInStockChange(value: Boolean) { form = form.copy(inStock = value) }

    /** Validates and saves. Returns true when saved. */
    fun save(): Boolean {
        val price = Money.parse(form.price)
        val salePrice = if (form.onSale) Money.parse(form.salePrice) else null
        val nameError = if (form.name.isBlank()) R.string.error_product_name else null
        val priceError = if (price == null) R.string.error_price else null
        val salePriceError = when {
            !form.onSale -> null
            salePrice == null -> R.string.error_sale_price
            price != null && salePrice >= price -> R.string.error_sale_price_too_high
            else -> null
        }
        if (nameError != null || priceError != null || salePriceError != null || price == null) {
            form = form.copy(nameError = nameError, priceError = priceError, salePriceError = salePriceError)
            return false
        }

        if (productId == null) repository.add(form.name, price, salePrice, form.inStock)
        else repository.update(productId, form.name, price, salePrice, form.inStock)
        return true
    }

    fun markArchived(value: Boolean) {
        productId?.let { repository.setArchived(it, value) }
    }
}
