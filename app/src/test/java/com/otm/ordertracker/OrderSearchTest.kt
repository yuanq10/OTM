package com.otm.ordertracker

import com.otm.ordertracker.data.model.DateRange
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderItem
import com.otm.ordertracker.data.model.OrderSearch
import com.otm.ordertracker.data.model.SearchSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class OrderSearchTest {

    private val teaOrder = Order(
        id = "o1",
        name = "Spring restock",
        customerId = "c1",
        customerName = "Jane Doe",
        items = listOf(OrderItem("tea", "Green Tea", 1000, 2)),
    )
    private val mugOrder = Order(
        id = "o2",
        customerId = "c2",
        customerName = "Sam Lee",
        items = listOf(OrderItem("mug", "Mug", 1500, 1), OrderItem("tea", "Green Tea", 1000, 1)),
    )
    private val orders = listOf(teaOrder, mugOrder)

    private fun search(
        text: String,
        selection: SearchSelection? = null,
        customerNames: Map<String, String> = emptyMap(),
        productNames: Map<String, String> = emptyMap(),
    ) = orders.filter { OrderSearch.matches(it, text, selection, customerNames, productNames) }.map { it.id }

    @Test
    fun emptySearch_matchesEverything() {
        assertEquals(listOf("o1", "o2"), search(""))
        assertEquals(listOf("o1", "o2"), search("   "))
    }

    @Test
    fun text_matchesOrderNameCustomerOrProduct_ignoringCase() {
        assertEquals(listOf("o1"), search("restock"))
        assertEquals(listOf("o2"), search("sam"))
        assertEquals(listOf("o2"), search("MUG"))
        assertEquals(listOf("o1", "o2"), search("green tea"))
        assertEquals(emptyList<String>(), search("coffee"))
    }

    @Test
    fun text_alsoMatchesCurrentNamesAfterRename() {
        val customerNames = mapOf("c1" to "Jane Smith")
        val productNames = mapOf("mug" to "Travel Mug")
        assertEquals(listOf("o1"), search("smith", customerNames = customerNames))
        assertEquals(listOf("o2"), search("travel", productNames = productNames))
    }

    @Test
    fun customerSelection_showsAllOrdersForThatCustomer() {
        assertEquals(listOf("o1"), search("", SearchSelection.ByCustomer("c1", "Jane Doe")))
    }

    @Test
    fun dateRange_matchesOrdersCompletedOnDaysWithinIt_inclusive() {
        val zone = ZoneId.of("America/Toronto")
        fun completedOn(date: LocalDate, hour: Int) =
            teaOrder.copy(completedAt = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli())
        val october = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        fun inOctober(order: Order) = OrderSearch.matches(order, "", null, completedIn = october, zone = zone)

        assertTrue("first day", inOctober(completedOn(LocalDate.of(2026, 10, 1), 0)))
        assertTrue("last day, late evening", inOctober(completedOn(LocalDate.of(2026, 10, 31), 23)))
        assertFalse("day before", inOctober(completedOn(LocalDate.of(2026, 9, 30), 23)))
        assertFalse("day after", inOctober(completedOn(LocalDate.of(2026, 11, 1), 0)))
        assertFalse("not completed", inOctober(teaOrder))
    }

    @Test
    fun productSelection_showsAllOrdersContainingThatProduct() {
        assertEquals(listOf("o1", "o2"), search("", SearchSelection.ByProduct("tea", "Green Tea")))
        assertEquals(listOf("o2"), search("", SearchSelection.ByProduct("mug", "Mug")))
    }
}
