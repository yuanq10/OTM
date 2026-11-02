package com.otm.ordertracker

import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderFilter
import com.otm.ordertracker.data.model.OrderRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStatusTest {

    private val order = Order(id = "o1", createdAt = 1)

    @Test
    fun paid_setsAndClearsDate() {
        val paid = OrderRules.setPaid(order, true, now = 100)
        assertTrue(paid.isPaid)
        assertEquals(100L, paid.paidAt)

        val repaid = OrderRules.setPaid(paid, true, now = 200)
        assertEquals("keeps original date", 100L, repaid.paidAt)

        val unpaid = OrderRules.setPaid(paid, false, now = 300)
        assertFalse(unpaid.isPaid)
        assertNull(unpaid.paidAt)
    }

    @Test(expected = IllegalStateException::class)
    fun received_requiresShipped() {
        OrderRules.setReceived(order, true, now = 100)
    }

    @Test
    fun received_afterShipped() {
        val shipped = OrderRules.setShipped(order, true, now = 100)
        val received = OrderRules.setReceived(shipped, true, now = 200)
        assertTrue(received.isReceived)
        assertEquals(200L, received.receivedAt)
        assertFalse(received.canToggleShipped)
    }

    @Test(expected = IllegalStateException::class)
    fun cannotUnshipWhileReceived() {
        val received = OrderRules.setReceived(OrderRules.setShipped(order, true, 1), true, 2)
        OrderRules.setShipped(received, false, now = 3)
    }

    @Test(expected = IllegalStateException::class)
    fun canceledOrders_areReadOnly() {
        val canceled = OrderRules.cancel(order, now = 100)
        assertTrue(canceled.isCanceled)
        OrderRules.setPaid(canceled, true, now = 200)
    }

    @Test
    fun filters_matchExpectedOrders() {
        val unpaid = order
        val shipped = OrderRules.setShipped(order, true, 1)
        val received = OrderRules.setReceived(shipped, true, 2)
        val canceled = OrderRules.cancel(order, 3)

        assertEquals(listOf(unpaid), listOf(unpaid, shipped, received, canceled).filter { OrderFilter.TO_SHIP.matches(it) })
        assertEquals(listOf(shipped), listOf(unpaid, shipped, received, canceled).filter { OrderFilter.SHIPPED.matches(it) })
        assertEquals(listOf(received), listOf(unpaid, shipped, received, canceled).filter { OrderFilter.RECEIVED.matches(it) })
        assertEquals(listOf(canceled), listOf(unpaid, shipped, received, canceled).filter { OrderFilter.CANCELED.matches(it) })
        assertFalse(OrderFilter.UNPAID.matches(canceled))
    }
}
