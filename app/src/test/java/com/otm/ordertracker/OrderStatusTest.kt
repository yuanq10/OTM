package com.otm.ordertracker

import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderFilter
import com.otm.ordertracker.data.model.OrderRules
import com.otm.ordertracker.data.model.OrderStatus
import com.otm.ordertracker.data.model.OrderTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStatusTest {

    private val order = Order(id = "o1", createdAt = 1)

    @Test
    fun status_setsAndClearsDate() {
        val paid = OrderRules.setStatus(order, OrderStatus.PAID_TO_ME, on = true, now = 100)
        assertTrue(paid.has(OrderStatus.PAID_TO_ME))
        assertEquals(100L, paid.dateOf(OrderStatus.PAID_TO_ME))

        val repaid = OrderRules.setStatus(paid, OrderStatus.PAID_TO_ME, on = true, now = 200)
        assertEquals("keeps original date", 100L, repaid.dateOf(OrderStatus.PAID_TO_ME))

        val unpaid = OrderRules.setStatus(paid, OrderStatus.PAID_TO_ME, on = false, now = 300)
        assertFalse(unpaid.has(OrderStatus.PAID_TO_ME))
        assertNull(unpaid.dateOf(OrderStatus.PAID_TO_ME))
    }

    @Test
    fun statuses_areIndependentAndCanBeSetInAnyOrder() {
        // Received before shipped, supplier paid before customer paid: all allowed.
        val received = OrderRules.setStatus(order, OrderStatus.RECEIVED, true, 1)
        val supplierPaid = OrderRules.setStatus(received, OrderStatus.PAID_TO_SUPPLIER, true, 2)
        assertTrue(supplierPaid.has(OrderStatus.RECEIVED))
        assertTrue(supplierPaid.has(OrderStatus.PAID_TO_SUPPLIER))
        assertFalse(supplierPaid.has(OrderStatus.SHIPPED))

        val all = OrderStatus.entries.fold(order) { o, s -> OrderRules.setStatus(o, s, true, 5) }
        assertTrue(OrderStatus.entries.all { all.has(it) })

        // Turning one off leaves the others alone.
        val shippedOff = OrderRules.setStatus(all, OrderStatus.SHIPPED, false, 6)
        assertFalse(shippedOff.has(OrderStatus.SHIPPED))
        assertTrue(shippedOff.has(OrderStatus.RECEIVED))
    }

    @Test(expected = IllegalStateException::class)
    fun canceledOrders_areReadOnly() {
        val canceled = OrderRules.cancel(order, now = 100)
        assertTrue(canceled.isCanceled)
        OrderRules.setStatus(canceled, OrderStatus.SHIPPED, true, now = 200)
    }

    @Test
    fun filters_matchOrdersMissingThatStatus() {
        val paid = OrderRules.setStatus(order, OrderStatus.PAID_TO_ME, true, 1)
        val shipped = OrderRules.setStatus(order, OrderStatus.SHIPPED, true, 1)
        val orders = listOf(order, paid, shipped)

        assertEquals(listOf(order, shipped), orders.filter { OrderFilter.NOT_PAID_TO_ME.matches(it) })
        assertEquals(listOf(order, paid), orders.filter { OrderFilter.NOT_SHIPPED.matches(it) })
        assertEquals(orders, orders.filter { OrderFilter.ALL.matches(it) })
    }

    @Test
    fun completeAndReopen_moveOrderBetweenTabs() {
        val completed = OrderRules.complete(order, now = 500)
        assertTrue(completed.isCompleted)
        assertEquals(500L, completed.completedAt)
        assertTrue(OrderTab.COMPLETED.matches(completed))
        assertFalse(OrderTab.IN_PROGRESS.matches(completed))

        val reopened = OrderRules.reopen(completed)
        assertFalse(reopened.isCompleted)
        assertTrue(OrderTab.IN_PROGRESS.matches(reopened))
    }

    @Test
    fun canceledOrders_appearInNeitherTab() {
        val canceled = OrderRules.cancel(order, 3)
        assertFalse(OrderTab.IN_PROGRESS.matches(canceled))
        assertFalse(OrderTab.COMPLETED.matches(canceled))
    }

    @Test(expected = IllegalStateException::class)
    fun canceledOrders_cantBeCompleted() {
        OrderRules.complete(OrderRules.cancel(order, 3), now = 4)
    }
}
