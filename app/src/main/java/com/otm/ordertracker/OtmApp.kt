package com.otm.ordertracker

import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.otm.ordertracker.data.repo.AuthRepository
import com.otm.ordertracker.data.repo.CustomerRepository
import com.otm.ordertracker.data.repo.OrderRepository
import com.otm.ordertracker.data.repo.ProductRepository
import com.otm.ordertracker.data.settings.AppCurrency
import com.otm.ordertracker.data.settings.SettingsRepository
import com.otm.ordertracker.ui.common.Money

class OtmApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Currency last used on this device (USD by default). The signed-in user's saved choice is applied
        // once their settings load; see MainActivity.
        Money.currency = container.settingsRepository.localCurrency() ?: AppCurrency.DEFAULT
    }
}

/** Manual dependency container. Data lives under users/{uid}/ in Firestore. */
class AppContainer(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid: () -> String = { auth.currentUser?.uid ?: error("Not signed in") }

    val authRepository = AuthRepository(auth)
    val settingsRepository = SettingsRepository(context, db) { auth.currentUser?.uid }
    val customerRepository = CustomerRepository(db, uid)
    val productRepository = ProductRepository(db, uid)
    val orderRepository = OrderRepository(db, uid)
}
