package com.otm.ordertracker

import com.otm.ordertracker.ui.common.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {

    @Test
    fun format_usesCurrencyOfLocale() {
        assertEquals("$12.34", Money.format(1234, Locale.US))
        assertEquals("$0.05", Money.format(5, Locale.US))
        assertEquals("$1,000.00", Money.format(100_000, Locale.US))
    }

    @Test
    fun parse_acceptsCommonInputs() {
        assertEquals(1200L, Money.parse("12"))
        assertEquals(1250L, Money.parse("12.5"))
        assertEquals(1234L, Money.parse("12.34"))
        assertEquals(1200L, Money.parse("12."))
        assertEquals(50L, Money.parse(".5"))
        assertEquals(123450L, Money.parse("$1,234.50"))
        assertEquals(999L, Money.parse("  9.99 "))
        assertEquals(0L, Money.parse("0"))
    }

    @Test
    fun parse_rejectsInvalidInputs() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse("1.234"))
        assertNull(Money.parse("1.2.3"))
        assertNull(Money.parse("."))
    }

    @Test
    fun toInput_roundTripsWithParse() {
        assertEquals("12.00", Money.toInput(1200))
        assertEquals("0.99", Money.toInput(99))
        assertEquals(1234L, Money.parse(Money.toInput(1234)))
    }
}
