package com.otm.ordertracker.data.model

data class Customer(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val notes: String = "",
    /** Archived customers stay in order history but can't be picked for new orders. */
    val archived: Boolean = false,
    val createdAt: Long = 0L,
) {
    val fullName: String get() = "$firstName $lastName".trim()

    fun hasSameName(firstName: String, lastName: String): Boolean =
        this.firstName.trim().equals(firstName.trim(), ignoreCase = true) &&
            this.lastName.trim().equals(lastName.trim(), ignoreCase = true)
}
