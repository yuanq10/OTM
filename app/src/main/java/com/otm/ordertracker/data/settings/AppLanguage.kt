package com.otm.ordertracker.data.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** The app's display language. AppCompat applies it and remembers it on this device. */
object AppLanguage {
    const val DEFAULT = "en"

    /** BCP 47 tags of the languages the app is translated into. */
    val supported = listOf("en", "fr", "zh-CN")

    /** The language the app is shown in (English unless another has been chosen). */
    fun current(): String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',').ifEmpty { DEFAULT }

    /** Switches language if it differs from the current one; the screen is then recreated in that language. */
    fun apply(tag: String) {
        val supportedTag = supported.firstOrNull { it.equals(tag, ignoreCase = true) } ?: DEFAULT
        if (!supportedTag.equals(current(), ignoreCase = true) || AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(supportedTag))
        }
    }

    /** Shows the app in English unless a language has been chosen (rather than following the phone). */
    fun applyDefaultIfUnset() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(DEFAULT))
        }
    }
}
