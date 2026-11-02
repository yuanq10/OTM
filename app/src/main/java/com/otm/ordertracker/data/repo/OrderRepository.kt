package com.otm.ordertracker.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderItem
import com.otm.ordertracker.data.model.OrderRules
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
    fun create(customer: Customer, items: List<OrderItem>, notes: String): String {
        require(items.isNotEmpty()) { "An order needs at least one product" }
        val doc = collection().document()
        doc.set(
            mapOf(
                "customerId" to customer.id,
                "customerName" to customer.fullName,
                "items" to items.map {
                    mapOf(
                        "productId" to it.productId,
                        "productName" to it.productName,
                        "unitPriceCents" to it.unitPriceCents,
                        "quantity" to it.quantity,
                    )
                },
                "totalCents" to OrderRules.total(items),
                "createdAt" to System.currentTimeMillis(),
                "notes" to notes.trim(),
                "isPaid" to false,
                "paidAt" to null,
                "isShipped" to false,
                "shippedAt" to null,
                "isReceived" to false,
                "receivedAt" to null,
                "isCanceled" to false,
                "canceledAt" to null,
            )
        ).logFailure("Create order")
        return doc.id
    }

    /** Writes all status fields of [order] in one atomic update. */
    fun updateStatus(order: Order) {
        collection().document(order.id).update(
            mapOf(
                "isPaid" to order.isPaid,
                "paidAt" to order.paidAt,
                "isShipped" to order.isShipped,
                "shippedAt" to order.shippedAt,
                "isReceived" to order.isReceived,
                "receivedAt" to order.receivedAt,
                "isCanceled" to order.isCanceled,
                "canceledAt" to order.canceledAt,
            )
        ).logFailure("Update order status")
    }

    private fun DocumentSnapshot.toOrder(): Order {
        @Suppress("UNCHECKED_CAST")
        val rawItems = get("items") as? List<Map<String, Any?>> ?: emptyList()
        return Order(
            id = id,
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
            isPaid = getBoolean("isPaid") ?: false,
            paidAt = getLong("paidAt"),
            isShipped = getBoolean("isShipped") ?: false,
            shippedAt = getLong("shippedAt"),
            isReceived = getBoolean("isReceived") ?: false,
            receivedAt = getLong("receivedAt"),
            isCanceled = getBoolean("isCanceled") ?: false,
            canceledAt = getLong("canceledAt"),
        )
    }
}
