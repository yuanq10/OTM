package com.otm.ordertracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otm.ordertracker.data.settings.AppCurrency
import com.otm.ordertracker.data.settings.AppLanguage
import com.otm.ordertracker.ui.auth.SignInScreen
import com.otm.ordertracker.ui.common.Money
import com.otm.ordertracker.ui.navigation.OtmNavHost
import com.otm.ordertracker.ui.theme.OrderTrackerTheme
import kotlinx.coroutines.launch

/** AppCompatActivity (rather than ComponentActivity) so the in-app language setting can be applied. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguage.applyDefaultIfUnset()
        enableEdgeToEdge()
        val container = (application as OtmApp).container
        val authRepository = container.authRepository

        setContent {
            OrderTrackerTheme {
                val user by remember { authRepository.authState() }
                    .collectAsStateWithLifecycle(initialValue = authRepository.currentUser)
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                val signedInUser = user
                if (signedInUser == null) {
                    SignInScreen(authRepository)
                } else {
                    // Apply this account's saved language and currency (defaults: English, USD).
                    LaunchedEffect(signedInUser.uid) {
                        container.settingsRepository.loadOncePerSignIn()?.let { settings ->
                            val currency = settings.currency ?: AppCurrency.DEFAULT
                            Money.currency = currency
                            container.settingsRepository.rememberCurrencyLocally(currency)
                            AppLanguage.apply(settings.language ?: AppLanguage.DEFAULT)
                        }
                    }
                    // Keyed by user so all screens and their data reset when the account changes.
                    key(signedInUser.uid) {
                        OtmNavHost(accountEmail = signedInUser.email, onSignOut = { scope.launch { authRepository.signOut(context) } })
                    }
                }
            }
        }
    }
}
