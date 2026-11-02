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

/**
 * Independent order statuses: any combination can be set, in any order.
 * [field] is the Firestore field holding the date the status was set (null/absent = not set).
 */
enum class OrderStatus(val field: String) {
    PAID_TO_ME("paidAt"),
    PAID_TO_SUPPLIER("supplierPaidAt"),
    SHIPPED("shippedAt"),
    RECEIVED("receivedAt"),
}

data class Order(
    val id: String = "",
    /** Optional name to help find the order later. */
    val name: String = "",
    val customerId: String = "",
    /** Customer's name at the time the order was placed. */
    val customerName: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalCents: Long = 0L,
    val createdAt: Long = 0L,
    val notes: String = "",
    /** Statuses that are set, with the date each was set. */
    val statusDates: Map<OrderStatus, Long> = emptyMap(),
    val isCanceled: Boolean = false,
    val canceledAt: Long? = null,
    /** When the order was marked completed; null while it's in progress. */
    val completedAt: Long? = null,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val isCompleted: Boolean get() = completedAt != null

    fun has(status: OrderStatus): Boolean = status in statusDates

    fun dateOf(status: OrderStatus): Long? = statusDates[status]
}

/** The two panels of the Orders page. Canceled orders appear in neither. */
enum class OrderTab {
    IN_PROGRESS,
    COMPLETED;

    fun matches(order: Order): Boolean =
        !order.isCanceled && order.isCompleted == (this == COMPLETED)
}

/** Status filters for in-progress orders: each shows the orders still missing that status. */
enum class OrderFilter(private val missing: OrderStatus?) {
    ALL(null),
    NOT_PAID_TO_ME(OrderStatus.PAID_TO_ME),
    SUPPLIER_NOT_PAID(OrderStatus.PAID_TO_SUPPLIER),
    NOT_SHIPPED(OrderStatus.SHIPPED),
    NOT_RECEIVED(OrderStatus.RECEIVED);

    fun matches(order: Order): Boolean = missing == null || !order.has(missing)
}

/** Pure order logic, kept free of Android/Firebase so it can be unit tested. */
object OrderRules {

    /**
     * Builds order lines from the chosen quantities (product id -> quantity).
     * Lines already on the order ([existing], when editing) keep the name and price they were ordered at;
     * newly added products are locked in at their current effective price.
     */
    fun buildItems(
        products: List<Product>,
        quantities: Map<String, Int>,
        existing: List<OrderItem> = emptyList(),
    ): List<OrderItem> {
        val existingIds = existing.map { it.productId }.toSet()
        val kept = existing.mapNotNull { item ->
            val qty = quantities[item.productId] ?: 0
            if (qty <= 0) null else item.copy(quantity = qty)
        }
        val added = products.filter { it.id !in existingIds }.mapNotNull { product ->
            val qty = quantities[product.id] ?: 0
            if (qty <= 0) null
            else OrderItem(product.id, product.name, product.effectivePriceCents, qty)
        }
        return kept + added
    }

    fun total(items: List<OrderItem>): Long = items.sumOf { it.lineTotalCents }

    /** Turns a status on (keeping its original date if already on) or off. */
    fun setStatus(order: Order, status: OrderStatus, on: Boolean, now: Long): Order {
        check(!order.isCanceled) { "Canceled orders can't be changed" }
        val dates = if (on) order.statusDates + (status to (order.dateOf(status) ?: now))
        else order.statusDates - status
        return order.copy(statusDates = dates)
    }

    fun cancel(order: Order, now: Long): Order {
        check(!order.isCanceled) { "Order is already canceled" }
        return order.copy(isCanceled = true, canceledAt = now)
    }

    /** Moves the order to the Completed panel. Statuses are left as they are. */
    fun complete(order: Order, now: Long): Order {
        check(!order.isCanceled) { "Canceled orders can't be changed" }
        check(!order.isCompleted) { "Order is already completed" }
        return order.copy(completedAt = now)
    }

    /** Moves a completed order back to In progress. */
    fun reopen(order: Order): Order {
        check(order.isCompleted) { "Order isn't completed" }
        return order.copy(completedAt = null)
    }
}
