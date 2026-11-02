package com.otm.ordertracker

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.otm.ordertracker.data.repo.AuthRepository
import com.otm.ordertracker.data.repo.CustomerRepository
import com.otm.ordertracker.data.repo.OrderRepository
import com.otm.ordertracker.data.repo.ProductRepository
import com.otm.ordertracker.data.settings.AppCurrency
import com.otm.ordertracker.data.settings.CurrencyPreferences
import com.otm.ordertracker.ui.common.Money
import java.util.Locale

class OtmApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // The currency chosen in Settings; until one is chosen, the phone region's currency if supported, else USD.
        Money.currency = container.currencyPreferences.load()
            ?: AppCurrency.forRegion(phoneLocale())
            ?: AppCurrency.USD
    }

    /**
     * The phone's own language/region, ignoring the language chosen in the app. On Android 13+ the app's
     * chosen language also replaces the locale the app sees in its configuration, so ask LocaleManager.
     */
    private fun phoneLocale(): Locale? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)?.systemLocales?.takeIf { !it.isEmpty }?.get(0)
        } else {
            Resources.getSystem().configuration.locales[0]
        }
}

/** Manual dependency container. Data lives under users/{uid}/ in Firestore. */
class AppContainer(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid: () -> String = { auth.currentUser?.uid ?: error("Not signed in") }

    val authRepository = AuthRepository(auth)
    val currencyPreferences = CurrencyPreferences(context)
    val customerRepository = CustomerRepository(db, uid)
    val productRepository = ProductRepository(db, uid)
    val orderRepository = OrderRepository(db, uid)
}
