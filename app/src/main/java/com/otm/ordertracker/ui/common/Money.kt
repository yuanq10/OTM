package com.otm.ordertracker.ui.common

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Money is stored as whole cents (Long) to avoid floating-point rounding errors. */
object Money {
    private val inputPattern = Regex("""^(\d+(\.\d{0,2})?|\.\d{1,2})$""")

    fun format(cents: Long, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getCurrencyInstance(locale).format(BigDecimal.valueOf(cents, 2))

    /** Parses user input like "12", "12.5", "$1,234.50" into cents. Returns null if invalid or negative. */
    fun parse(text: String): Long? {
        val cleaned = text.trim().removePrefix("$").replace(",", "").trim()
        if (!inputPattern.matches(cleaned)) return null
        return BigDecimal(cleaned).movePointRight(2).longValueExact()
    }

    /** Formats cents for an editable text field, e.g. 1200 -> "12.00". */
    fun toInput(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
}
