package com.otm.ordertracker.data.settings

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.otm.ordertracker.data.repo.logFailure
import kotlinx.coroutines.tasks.await

/** A user's saved settings; null fields were never changed and use the defaults. */
data class UserSettings(val language: String?, val currency: AppCurrency?)

/**
 * Stores each user's language and currency with their account (users/{uid}/settings/preferences in Firestore),
 * so the choices follow them to other devices. The currency is also kept on this device so prices show
 * correctly at startup, before the account's settings load.
 */
class SettingsRepository(
    context: Context,
    private val db: FirebaseFirestore,
    private val uid: () -> String?,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** The account whose settings were last loaded in this app session. */
    private var loadedForUid: String? = null

    fun localCurrency(): AppCurrency? = AppCurrency.fromCode(prefs.getString(KEY_CURRENCY, null))

    fun rememberCurrencyLocally(currency: AppCurrency) = prefs.edit { putString(KEY_CURRENCY, currency.code) }

    fun saveCurrency(currency: AppCurrency) {
        rememberCurrencyLocally(currency)
        saveForUser(FIELD_CURRENCY, currency.code)
    }

    fun saveLanguage(tag: String) = saveForUser(FIELD_LANGUAGE, tag)

    /**
     * The signed-in user's saved settings, loaded once per sign-in (later changes are made here, so there's
     * no need to reload). Returns null if already loaded for this user, or if they can't be read right now
     * (e.g. offline with nothing cached), in which case the device's current settings are kept.
     */
    suspend fun loadOncePerSignIn(): UserSettings? {
        val id = uid() ?: return null
        if (loadedForUid == id) return null
        val snapshot = runCatching { document(id).get().await() }.getOrNull() ?: return null
        loadedForUid = id
        return UserSettings(
            language = snapshot.getString(FIELD_LANGUAGE),
            currency = AppCurrency.fromCode(snapshot.getString(FIELD_CURRENCY)),
        )
    }

    private fun saveForUser(field: String, value: String) {
        val id = uid() ?: return
        document(id).set(mapOf(field to value), SetOptions.merge()).logFailure("Save setting")
    }

    private fun document(id: String) =
        db.collection("users").document(id).collection("settings").document("preferences")

    private companion object {
        const val KEY_CURRENCY = "currency"
        const val FIELD_CURRENCY = "currency"
        const val FIELD_LANGUAGE = "language"
    }
}
