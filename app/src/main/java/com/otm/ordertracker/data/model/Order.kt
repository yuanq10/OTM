package com.otm.ordertracker.data.model

/** A line on an order. Name and price are copied at order time so later product edits don't change history. */
data class OrderItem(
    val productId: String,
    val productName: String,
    val unitPriceCents: Long,
    val quantity: Int,
) {
    val lineTotalCents: Long get() = unitPriceCents * quantity
}

data class Order(
    val id: String = "",
    val customerId: String = "",
    /** Customer's name at the time the order was placed. */
    val customerName: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalCents: Long = 0L,
    val createdAt: Long = 0L,
    val notes: String = "",
    val isPaid: Boolean = false,
    val paidAt: Long? = null,
    val isShipped: Boolean = false,
    val shippedAt: Long? = null,
    val isReceived: Boolean = false,
    val receivedAt: Long? = null,
    val isCanceled: Boolean = false,
    val canceledAt: Long? = null,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val canTogglePaid: Boolean get() = !isCanceled
    /** Shipped can't be turned off while the order is marked received. */
    val canToggleShipped: Boolean get() = !isCanceled && !isReceived
    /** Received requires the order to be shipped first. */
    val canToggleReceived: Boolean get() = !isCanceled && isShipped
}

enum class OrderFilter(val label: String) {
    ALL("All"),
    UNPAID("Unpaid"),
    TO_SHIP("To ship"),
    SHIPPED("Shipped"),
    RECEIVED("Received"),
    CANCELED("Canceled");

    fun matches(order: Order): Boolean = when (this) {
        ALL -> true
        UNPAID -> !order.isCanceled && !order.isPaid
        TO_SHIP -> !order.isCanceled && !order.isShipped
        SHIPPED -> !order.isCanceled && order.isShipped && !order.isReceived
        RECEIVED -> !order.isCanceled && order.isReceived
        CANCELED -> order.isCanceled
    }
}

/** Pure order logic, kept free of Android/Firebase so it can be unit tested. */
object OrderRules {

    /** Builds order lines from the chosen quantities, locking in each product's current effective price. */
    fun buildItems(products: List<Product>, quantities: Map<String, Int>): List<OrderItem> =
        products.mapNotNull { product ->
            val qty = quantities[product.id] ?: 0
            if (qty <= 0) null
            else OrderItem(product.id, product.name, product.effectivePriceCents, qty)
        }

    fun total(items: List<OrderItem>): Long = items.sumOf { it.lineTotalCents }

    fun setPaid(order: Order, paid: Boolean, now: Long): Order {
        check(order.canTogglePaid) { "Canceled orders can't be changed" }
        return order.copy(isPaid = paid, paidAt = if (paid) order.paidAt ?: now else null)
    }

    fun setShipped(order: Order, shipped: Boolean, now: Long): Order {
        check(!order.isCanceled) { "Canceled orders can't be changed" }
        check(shipped || !order.isReceived) { "Unmark received before unmarking shipped" }
        return order.copy(isShipped = shipped, shippedAt = if (shipped) order.shippedAt ?: now else null)
    }

    fun setReceived(order: Order, received: Boolean, now: Long): Order {
        check(!order.isCanceled) { "Canceled orders can't be changed" }
        check(!received || order.isShipped) { "Mark the order as shipped first" }
        return order.copy(isReceived = received, receivedAt = if (received) order.receivedAt ?: now else null)
    }

    fun cancel(order: Order, now: Long): Order {
        check(!order.isCanceled) { "Order is already canceled" }
        return order.copy(isCanceled = true, canceledAt = now)
    }
}
