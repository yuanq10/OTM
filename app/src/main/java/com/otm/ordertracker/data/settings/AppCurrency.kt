package com.otm.ordertracker.data.settings

/**
 * Currencies the app can show prices in. Only the [symbol] is displayed next to amounts.
 * Amounts are plain numbers, so switching currency changes the symbol, not the values.
 */
enum class AppCurrency(val code: String, val symbol: String) {
    USD("USD", "$"),
    CAD("CAD", "$"),
    CNY("CNY", "¥");

    companion object {
        val DEFAULT = USD

        fun fromCode(code: String?): AppCurrency? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
