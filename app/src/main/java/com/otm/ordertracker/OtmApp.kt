package com.otm.ordertracker

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.otm.ordertracker.data.repo.AuthRepository
import com.otm.ordertracker.data.repo.CustomerRepository
import com.otm.ordertracker.data.repo.OrderRepository
import com.otm.ordertracker.data.repo.ProductRepository

class OtmApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}

/** Manual dependency container. Data lives under users/{uid}/ in Firestore. */
class AppContainer {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid: () -> String = { auth.currentUser?.uid ?: error("Not signed in") }

    val authRepository = AuthRepository(auth)
    val customerRepository = CustomerRepository(db, uid)
    val productRepository = ProductRepository(db, uid)
    val orderRepository = OrderRepository(db, uid)
}
