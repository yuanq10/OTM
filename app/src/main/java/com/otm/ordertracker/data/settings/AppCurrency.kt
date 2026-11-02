package com.otm.ordertracker.data.settings

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

/**
 * Currencies the app can show prices in. Only the [symbol] is displayed next to amounts.
 * Amounts are plain numbers, so switching currency changes the symbol, not the values.
 */
enum class AppCurrency(val code: String, val symbol: String) {
    USD("USD", "$"),
    CAD("CAD", "$"),
    CNY("CNY", "¥");

    companion object {
        fun fromCode(code: String?): AppCurrency? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        /** The currency of the phone's region if the app supports it, e.g. CAD on a Canadian phone. */
        fun forRegion(locale: Locale?): AppCurrency? =
            locale?.country?.takeIf { it.isNotEmpty() }
                ?.let { country -> runCatching { java.util.Currency.getInstance(Locale("", country)).currencyCode }.getOrNull() }
                ?.let(::fromCode)
    }
}

/** Remembers the chosen currency on this device. */
class CurrencyPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppCurrency? = AppCurrency.fromCode(prefs.getString(KEY_CURRENCY, null))

    fun save(currency: AppCurrency) = prefs.edit { putString(KEY_CURRENCY, currency.code) }

    private companion object {
        const val KEY_CURRENCY = "currency"
    }
}
