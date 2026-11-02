package com.otm.ordertracker

import com.otm.ordertracker.data.model.OrderRules
import com.otm.ordertracker.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderTotalsTest {

    private val tea = Product(id = "tea", name = "Tea", priceCents = 1000)
    private val mug = Product(id = "mug", name = "Mug", priceCents = 2000, salePriceCents = 1500)
    private val pot = Product(id = "pot", name = "Pot", priceCents = 5000)

    @Test
    fun buildItems_usesSalePriceAndSkipsZeroQuantities() {
        val items = OrderRules.buildItems(listOf(tea, mug, pot), mapOf("tea" to 2, "mug" to 1, "pot" to 0))

        assertEquals(2, items.size)
        assertEquals(1000L, items.first { it.productId == "tea" }.unitPriceCents)
        assertEquals(1500L, items.first { it.productId == "mug" }.unitPriceCents)
        assertEquals("Mug", items.first { it.productId == "mug" }.productName)
    }

    @Test
    fun total_sumsLineTotals() {
        val items = OrderRules.buildItems(listOf(tea, mug), mapOf("tea" to 3, "mug" to 2))
        assertEquals(3 * 1000L + 2 * 1500L, OrderRules.total(items))
    }

    @Test
    fun lockedPrice_isUnaffectedByLaterPriceChanges() {
        val items = OrderRules.buildItems(listOf(tea), mapOf("tea" to 1))
        val repriced = tea.copy(priceCents = 9999)

        assertEquals(1000L, items.single().unitPriceCents)
        assertEquals(9999L, repriced.effectivePriceCents)
    }
}
