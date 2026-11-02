package com.otm.ordertracker.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val priceCents: Long = 0L,
    /** Null when the product is not on sale. */
    val salePriceCents: Long? = null,
    /** Archived products stay in order history but can't be added to new orders. */
    val archived: Boolean = false,
    val updatedAt: Long = 0L,
) {
    val onSale: Boolean get() = salePriceCents != null

    /** The price a new order is charged right now. */
    val effectivePriceCents: Long get() = salePriceCents ?: priceCents
}
