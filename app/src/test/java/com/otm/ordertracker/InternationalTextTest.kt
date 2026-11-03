package com.otm.ordertracker

import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderItem
import com.otm.ordertracker.data.model.OrderSearch
import com.otm.ordertracker.data.model.OrderRules
import com.otm.ordertracker.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chinese and French names must survive the app's logic unchanged and be findable by search. */
class InternationalTextTest {

    private val chineseOrder = Order(
        id = "zh",
        name = "春季补货",
        customerId = "c1",
        customerName = "王小明",
        items = listOf(OrderItem("p1", "绿茶", 1000, 2)),
    )
    private val frenchOrder = Order(
        id = "fr",
        name = "Commande de Noël",
        customerId = "c2",
        customerName = "Hélène Côté",
        items = listOf(OrderItem("p2", "Crème brûlée", 650, 1)),
    )

    private fun matches(order: Order, text: String) = OrderSearch.matches(order, text, null)

    @Test
    fun namesAreKeptExactlyWhenPlacingAnOrder() {
        val products = listOf(Product(id = "p1", name = "绿茶", priceCents = 1000), Product(id = "p2", name = "Crème brûlée", priceCents = 650))
        val items = OrderRules.buildItems(products, mapOf("p1" to 1, "p2" to 1))
        assertEquals(listOf("绿茶", "Crème brûlée"), items.map { it.productName })
        assertEquals("王小明", Customer(firstName = "小明", lastName = "王").let { it.lastName + it.firstName })
    }

    @Test
    fun chineseNames_areFoundBySearch() {
        assertTrue("full customer name", matches(chineseOrder, "王小明"))
        assertTrue("part of a name", matches(chineseOrder, "小明"))
        assertTrue("product", matches(chineseOrder, "绿茶"))
        assertTrue("order name", matches(chineseOrder, "补货"))
        assertFalse("different characters", matches(chineseOrder, "红茶"))
    }

    @Test
    fun frenchNames_areFoundBySearch_ignoringCapitals() {
        assertTrue(matches(frenchOrder, "Hélène"))
        assertTrue("capitals, including accented ones", matches(frenchOrder, "HÉLÈNE"))
        assertTrue(matches(frenchOrder, "crème"))
        assertTrue(matches(frenchOrder, "noël"))
    }

    @Test
    fun accents_mustBeTyped_toMatch() {
        // Current behaviour: searching without the accent doesn't find the accented name.
        assertFalse(matches(frenchOrder, "creme"))
        assertFalse(matches(frenchOrder, "Helene"))
    }

    @Test
    fun duplicateCustomerCheck_worksForChineseAndFrenchNames() {
        assertTrue(Customer(firstName = "小明", lastName = "王").hasSameName("小明", "王"))
        assertTrue(Customer(firstName = "Hélène", lastName = "Côté").hasSameName("HÉLÈNE", "côté"))
        assertFalse(Customer(firstName = "小明", lastName = "王").hasSameName("小红", "王"))
    }
}
