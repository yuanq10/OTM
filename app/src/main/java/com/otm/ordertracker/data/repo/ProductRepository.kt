package com.otm.ordertracker.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.otm.ordertracker.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val db: FirebaseFirestore,
    private val uid: () -> String,
) {
    private fun collection() = db.collection("users").document(uid()).collection("products")

    /** All products, including archived ones, sorted by name. */
    fun products(): Flow<List<Product>> =
        collection().snapshotFlow().map { snapshot ->
            snapshot.documents.map { it.toProduct() }.sortedBy { it.name.lowercase() }
        }

    fun product(id: String): Flow<Product?> =
        collection().document(id).snapshotFlow().map { if (it.exists()) it.toProduct() else null }

    fun add(name: String, priceCents: Long, salePriceCents: Long?): String {
        val doc = collection().document()
        val now = System.currentTimeMillis()
        doc.set(
            mapOf(
                "name" to name.trim(),
                "priceCents" to priceCents,
                "salePriceCents" to salePriceCents,
                "archived" to false,
                "createdAt" to now,
                "updatedAt" to now,
            )
        ).logFailure("Add product")
        return doc.id
    }

    fun update(id: String, name: String, priceCents: Long, salePriceCents: Long?) {
        collection().document(id).update(
            mapOf(
                "name" to name.trim(),
                "priceCents" to priceCents,
                "salePriceCents" to salePriceCents,
                "updatedAt" to System.currentTimeMillis(),
            )
        ).logFailure("Update product")
    }

    fun setArchived(id: String, archived: Boolean) {
        collection().document(id).update("archived", archived).logFailure("Archive product")
    }

    private fun DocumentSnapshot.toProduct() = Product(
        id = id,
        name = getString("name").orEmpty(),
        priceCents = getLong("priceCents") ?: 0L,
        salePriceCents = getLong("salePriceCents"),
        archived = getBoolean("archived") ?: false,
        updatedAt = getLong("updatedAt") ?: 0L,
    )
}
