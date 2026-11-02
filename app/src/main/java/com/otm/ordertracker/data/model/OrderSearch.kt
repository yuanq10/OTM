package com.otm.ordertracker.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A customer or product picked from the search suggestions. */
sealed interface SearchSelection {
    val label: String

    data class ByCustomer(val customerId: String, override val label: String) : SearchSelection
    data class ByProduct(val productId: String, override val label: String) : SearchSelection
}

/** An inclusive range of calendar days, e.g. Oct 1 – Oct 31. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    /** True when [millis] falls on a day within the range, in the given time zone. */
    fun contains(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return !day.isBefore(start) && !day.isAfter(end)
    }
}

/** Order search logic, kept free of Android/Firebase so it can be unit tested. */
object OrderSearch {

    /**
     * True when [order] matches the search. A [selection] matches by id, so it still finds orders after a
     * customer or product is renamed. Free [text] matches the order name, customer name or any product name;
     * both the names stored on the order and the current names (from [customerNames]/[productNames] by id) count.
     * A [completedIn] range keeps only orders completed on a day within it.
     */
    fun matches(
        order: Order,
        text: String,
        selection: SearchSelection?,
        customerNames: Map<String, String> = emptyMap(),
        productNames: Map<String, String> = emptyMap(),
        completedIn: DateRange? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val matchesSelection = when (selection) {
            null -> true
            is SearchSelection.ByCustomer -> order.customerId == selection.customerId
            is SearchSelection.ByProduct -> order.items.any { it.productId == selection.productId }
        }
        val matchesRange = completedIn == null || order.completedAt?.let { completedIn.contains(it, zone) } == true
        return matchesSelection && matchesRange && matchesText(order, text, customerNames, productNames)
    }

    private fun matchesText(
        order: Order,
        text: String,
        customerNames: Map<String, String>,
        productNames: Map<String, String>,
    ): Boolean {
        val q = text.trim()
        if (q.isEmpty()) return true
        val candidates = buildList {
            add(order.name)
            add(order.customerName)
            customerNames[order.customerId]?.let(::add)
            order.items.forEach { item ->
                add(item.productName)
                productNames[item.productId]?.let(::add)
            }
        }
        return candidates.any { it.contains(q, ignoreCase = true) }
    }
}
