package com.otm.ordertracker.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderItem
import com.otm.ordertracker.data.model.OrderRules
import com.otm.ordertracker.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrderRepository(
    private val db: FirebaseFirestore,
    private val uid: () -> String,
) {
    private fun collection() = db.collection("users").document(uid()).collection("orders")

    /** All orders, newest first. */
    fun orders(): Flow<List<Order>> =
        collection().snapshotFlow().map { snapshot ->
            snapshot.documents.map { it.toOrder() }.sortedByDescending { it.createdAt }
        }

    /** A customer's order history, newest first. Sorted on-device so no Firestore index is needed. */
    fun ordersForCustomer(customerId: String): Flow<List<Order>> =
        collection().whereEqualTo("customerId", customerId).snapshotFlow().map { snapshot ->
            snapshot.documents.map { it.toOrder() }.sortedByDescending { it.createdAt }
        }

    fun order(id: String): Flow<Order?> =
        collection().document(id).snapshotFlow().map { if (it.exists()) it.toOrder() else null }

    /** Returns the new order's id. */
    fun create(customer: Customer, items: List<OrderItem>, name: String, notes: String): String {
        require(items.isNotEmpty()) { "An order needs at least one product" }
        val doc = collection().document()
        doc.set(
            mapOf(
                "name" to name.trim(),
                "customerId" to customer.id,
                "customerName" to customer.fullName,
                "items" to items.map(::itemFields),
                "totalCents" to OrderRules.total(items),
                "createdAt" to System.currentTimeMillis(),
                "notes" to notes.trim(),
                "isCanceled" to false,
                "canceledAt" to null,
                "completedAt" to null,
            ) + OrderStatus.entries.associate { it.field to null }
        ).logFailure("Create order")
        return doc.id
    }

    /** Writes all status fields of [order] (statuses, canceled, completed) in one atomic update. */
    fun updateStatus(order: Order) {
        collection().document(order.id).update(
            OrderStatus.entries.associate { it.field to order.dateOf(it) } + mapOf(
                "isCanceled" to order.isCanceled,
                "canceledAt" to order.canceledAt,
                "completedAt" to order.completedAt,
            )
        ).logFailure("Update order status")
    }

    /** Saves an edited order's details. Statuses, creation date and cancellation are left unchanged. */
    fun updateDetails(
        orderId: String,
        customerId: String,
        customerName: String,
        items: List<OrderItem>,
        name: String,
        notes: String,
    ) {
        require(items.isNotEmpty()) { "An order needs at least one product" }
        collection().document(orderId).update(
            mapOf(
                "name" to name.trim(),
                "customerId" to customerId,
                "customerName" to customerName,
                "items" to items.map(::itemFields),
                "totalCents" to OrderRules.total(items),
                "notes" to notes.trim(),
            )
        ).logFailure("Update order")
    }

    private fun itemFields(item: OrderItem): Map<String, Any> = mapOf(
        "productId" to item.productId,
        "productName" to item.productName,
        "unitPriceCents" to item.unitPriceCents,
        "quantity" to item.quantity,
    )

    private fun DocumentSnapshot.toOrder(): Order {
        @Suppress("UNCHECKED_CAST")
        val rawItems = get("items") as? List<Map<String, Any?>> ?: emptyList()
        return Order(
            id = id,
            name = getString("name").orEmpty(),
            customerId = getString("customerId").orEmpty(),
            customerName = getString("customerName").orEmpty(),
            items = rawItems.map { item ->
                OrderItem(
                    productId = item["productId"] as? String ?: "",
                    productName = item["productName"] as? String ?: "",
                    unitPriceCents = (item["unitPriceCents"] as? Number)?.toLong() ?: 0L,
                    quantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                )
            },
            totalCents = getLong("totalCents") ?: 0L,
            createdAt = getLong("createdAt") ?: 0L,
            notes = getString("notes").orEmpty(),
            statusDates = OrderStatus.entries.mapNotNull { status -> getLong(status.field)?.let { status to it } }.toMap(),
            isCanceled = getBoolean("isCanceled") ?: false,
            canceledAt = getLong("canceledAt"),
            completedAt = getLong("completedAt"),
        )
    }
}
