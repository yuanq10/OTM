package com.otm.ordertracker

import com.otm.ordertracker.data.settings.AppCurrency
import com.otm.ordertracker.ui.common.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {

    private companion object {
        // Non-breaking spaces that French number formats use; written as code points so they stay visible here.
        val NBSP = 0x00A0.toChar()
        val NARROW_NBSP = 0x202F.toChar()
    }

    private fun String.normalizeSpaces() = replace(NBSP, ' ').replace(NARROW_NBSP, ' ')

    @Test
    fun format_usesNumberStyleOfLocale() {
        assertEquals("$12.34", Money.format(1234, Locale.US, AppCurrency.USD))
        assertEquals("$0.05", Money.format(5, Locale.US, AppCurrency.USD))
        assertEquals("$1,000.00", Money.format(100_000, Locale.US, AppCurrency.USD))
    }

    @Test
    fun format_showsOnlyTheSymbol_inAnyLanguage() {
        // Never "US$", "CA$", "$US" or "CN¥": just the symbol, in the language's number style.
        assertEquals("$12.34", Money.format(1234, Locale.US, AppCurrency.CAD))
        assertEquals("¥12.34", Money.format(1234, Locale.US, AppCurrency.CNY))
        assertEquals("12,34 $", Money.format(1234, Locale.FRENCH, AppCurrency.USD).normalizeSpaces())
        assertEquals("12,34 $", Money.format(1234, Locale.CANADA_FRENCH, AppCurrency.CAD).normalizeSpaces())
        assertEquals("¥12.34", Money.format(1234, Locale.SIMPLIFIED_CHINESE, AppCurrency.CNY))
        assertEquals("$12.34", Money.format(1234, Locale.SIMPLIFIED_CHINESE, AppCurrency.USD))
    }

    @Test
    fun currency_defaultsToUsd_andReadsSavedCodes() {
        assertEquals(AppCurrency.USD, AppCurrency.DEFAULT)
        assertEquals(AppCurrency.CAD, AppCurrency.fromCode("CAD"))
        assertEquals(AppCurrency.CNY, AppCurrency.fromCode("cny"))
        assertNull("unknown code", AppCurrency.fromCode("EUR"))
        assertNull("never saved", AppCurrency.fromCode(null))
    }

    @Test
    fun parse_acceptsDecimalComma() {
        assertEquals(1250L, Money.parse("12,50"))
        assertEquals(1250L, Money.parse("12,5"))
        assertEquals(123450L, Money.parse("1.234,50"))
        assertEquals(123450L, Money.parse("1 234,50 €"))
        assertEquals("comma before 3 digits is a thousands separator", 123400L, Money.parse("1,234"))
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
