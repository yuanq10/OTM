package com.otm.ordertracker.data.repo

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.otm.ordertracker.data.model.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepository(
    private val db: FirebaseFirestore,
    private val uid: () -> String,
) {
    private fun collection() = db.collection("users").document(uid()).collection("customers")

    /** All customers, including archived ones, sorted by last then first name. */
    fun customers(): Flow<List<Customer>> =
        collection().snapshotFlow().map { snapshot ->
            snapshot.documents.map { it.toCustomer() }
                .sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))
        }

    fun customer(id: String): Flow<Customer?> =
        collection().document(id).snapshotFlow().map { if (it.exists()) it.toCustomer() else null }

    /** Returns the new customer's id. */
    fun add(firstName: String, lastName: String, phone: String, notes: String): String {
        val doc = collection().document()
        doc.set(
            mapOf(
                "firstName" to firstName.trim(),
                "lastName" to lastName.trim(),
                "phone" to phone.trim(),
                "notes" to notes.trim(),
                "archived" to false,
                "createdAt" to System.currentTimeMillis(),
            )
        ).logFailure("Add customer")
        return doc.id
    }

    fun update(id: String, firstName: String, lastName: String, phone: String, notes: String) {
        collection().document(id).update(
            mapOf(
                "firstName" to firstName.trim(),
                "lastName" to lastName.trim(),
                "phone" to phone.trim(),
                "notes" to notes.trim(),
            )
        ).logFailure("Update customer")
    }

    fun setArchived(id: String, archived: Boolean) {
        collection().document(id).update("archived", archived).logFailure("Archive customer")
    }

    private fun DocumentSnapshot.toCustomer() = Customer(
        id = id,
        firstName = getString("firstName").orEmpty(),
        lastName = getString("lastName").orEmpty(),
        phone = getString("phone").orEmpty(),
        notes = getString("notes").orEmpty(),
        archived = getBoolean("archived") ?: false,
        createdAt = getLong("createdAt") ?: 0L,
    )
}
