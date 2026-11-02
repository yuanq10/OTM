package com.otm.ordertracker.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.otm.ordertracker.data.settings.AppCurrency
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Money is stored as whole cents (Long) to avoid floating-point rounding errors. */
object Money {
    private val dotDecimal = Regex("""^(\d+(\.\d{0,2})?|\.\d{1,2})$""")
    private val commaDecimal = Regex("""^\d+,\d{1,2}$""")
    private val currencyAndSpaces = Regex("""[\s  $€£¥]""")

    /**
     * The currency chosen in Settings. Compose state, so every price on screen updates when it changes.
     * It's independent of the app language (a French UI can still show dollars).
     */
    var currency: AppCurrency by mutableStateOf(AppCurrency.USD)

    /**
     * Formats [cents] with the number style of [locale] and just the currency's symbol,
     * e.g. "$12.34", "12,34 $", "¥12.34" (never "US$" or "CA$").
     */
    fun format(cents: Long, locale: Locale = Locale.getDefault(), currency: AppCurrency = this.currency): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        if (format is DecimalFormat) {
            format.decimalFormatSymbols = format.decimalFormatSymbols.apply {
                this.currency = Currency.getInstance(currency.code)
                currencySymbol = currency.symbol
            }
        }
        return format.format(BigDecimal.valueOf(cents, 2))
    }

    /**
     * Parses user input like "12", "12.5", "$1,234.50" or "12,50" into cents. A comma followed by one or two
     * digits at the end is a decimal comma (as typed in French); otherwise commas separate thousands.
     * Returns null if invalid or negative.
     */
    fun parse(text: String): Long? {
        var cleaned = text.replace(currencyAndSpaces, "")
        cleaned = when {
            commaDecimal.matches(cleaned) -> cleaned.replace(',', '.')
            // Both separators: whichever comes last is the decimal point ("1.234,50" or "1,234.50").
            ',' in cleaned && '.' in cleaned && cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.') ->
                cleaned.replace(".", "").replace(',', '.')
            else -> cleaned.replace(",", "")
        }
        if (!dotDecimal.matches(cleaned)) return null
        return BigDecimal(cleaned).movePointRight(2).longValueExact()
    }

    /** Formats cents for an editable text field, e.g. 1200 -> "12.00". */
    fun toInput(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
}
