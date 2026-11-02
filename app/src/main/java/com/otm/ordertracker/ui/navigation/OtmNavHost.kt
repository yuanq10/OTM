package com.otm.ordertracker.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.otm.ordertracker.R
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.customers.CustomerDetailScreen
import com.otm.ordertracker.ui.customers.CustomerEditScreen
import com.otm.ordertracker.ui.customers.CustomerListScreen
import com.otm.ordertracker.ui.orders.OrderEditScreen
import com.otm.ordertracker.ui.orders.OrderEditViewModel
import com.otm.ordertracker.ui.orders.OrderDetailScreen
import com.otm.ordertracker.ui.orders.OrderListScreen
import com.otm.ordertracker.ui.products.ProductEditScreen
import com.otm.ordertracker.ui.products.ProductListScreen
import com.otm.ordertracker.ui.settings.SettingsScreen

private object Routes {
    const val ORDERS = "orders"
    const val CUSTOMERS = "customers"
    const val PRODUCTS = "products"
    const val SETTINGS = "settings"

    const val NEW_ORDER = "order/new?customerId={customerId}"
    fun newOrder(customerId: String? = null) =
        if (customerId == null) "order/new" else "order/new?customerId=$customerId"

    const val ORDER_DETAIL = "order/detail/{orderId}"
    fun orderDetail(orderId: String) = "order/detail/$orderId"

    const val ORDER_EDIT = "order/edit/{orderId}"
    fun orderEdit(orderId: String) = "order/edit/$orderId"

    const val CUSTOMER_EDIT = "customer/edit?customerId={customerId}"
    fun customerEdit(customerId: String? = null) =
        if (customerId == null) "customer/edit" else "customer/edit?customerId=$customerId"

    const val CUSTOMER_DETAIL = "customer/detail/{customerId}"
    fun customerDetail(customerId: String) = "customer/detail/$customerId"

    const val PRODUCT_EDIT = "product/edit?productId={productId}"
    fun productEdit(productId: String? = null) =
        if (productId == null) "product/edit" else "product/edit?productId=$productId"
}

/** Set on the previous screen's SavedStateHandle when a customer is created, so New Order can select them. */
private const val NEW_CUSTOMER_ID = "newCustomerId"

private data class TopLevelDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.ORDERS, R.string.orders_title, Icons.AutoMirrored.Filled.List),
    TopLevelDestination(Routes.CUSTOMERS, R.string.customers, Icons.Filled.Person),
    TopLevelDestination(Routes.PRODUCTS, R.string.products, Icons.Filled.ShoppingCart),
    TopLevelDestination(Routes.SETTINGS, R.string.settings, Icons.Filled.Settings),
)

private fun optionalStringArg(name: String) = navArgument(name) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OtmNavHost(accountEmail: String?, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        // Each screen has its own Scaffold that handles system bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                nav.navigate(destination.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.ORDERS,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Routes.ORDERS) {
                OrderListScreen(
                    onNewOrder = { nav.navigate(Routes.newOrder()) },
                    onOpenOrder = { nav.navigate(Routes.orderDetail(it)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(accountEmail = accountEmail, onLogout = onSignOut)
            }
            composable(Routes.NEW_ORDER, arguments = listOf(optionalStringArg("customerId"))) { entry ->
                OrderEditDestination(
                    entry = entry,
                    nav = nav,
                    // Replace the form with the new order's detail page.
                    onSaved = { orderId ->
                        nav.popBackStack()
                        nav.navigate(Routes.orderDetail(orderId))
                    },
                )
            }
            composable(Routes.ORDER_EDIT) { entry ->
                // Back to the order's detail page, which updates live.
                OrderEditDestination(entry = entry, nav = nav, onSaved = { nav.popBackStack() })
            }
            composable(Routes.ORDER_DETAIL) {
                OrderDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenCustomer = { nav.navigate(Routes.customerDetail(it)) },
                    onEdit = { nav.navigate(Routes.orderEdit(it)) },
                )
            }

            composable(Routes.CUSTOMERS) {
                CustomerListScreen(
                    onAddCustomer = { nav.navigate(Routes.customerEdit()) },
                    onOpenCustomer = { nav.navigate(Routes.customerDetail(it)) },
                )
            }
            composable(Routes.CUSTOMER_EDIT, arguments = listOf(optionalStringArg("customerId"))) {
                CustomerEditScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { customerId ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(NEW_CUSTOMER_ID, customerId)
                        nav.popBackStack()
                    },
                )
            }
            composable(Routes.CUSTOMER_DETAIL) {
                CustomerDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.customerEdit(it)) },
                    onNewOrder = { nav.navigate(Routes.newOrder(it)) },
                    onOpenOrder = { nav.navigate(Routes.orderDetail(it)) },
                )
            }

            composable(Routes.PRODUCTS) {
                ProductListScreen(
                    onAddProduct = { nav.navigate(Routes.productEdit()) },
                    onOpenProduct = { nav.navigate(Routes.productEdit(it)) },
                )
            }
            composable(Routes.PRODUCT_EDIT, arguments = listOf(optionalStringArg("productId"))) {
                ProductEditScreen(onDone = { nav.popBackStack() })
            }
        }
    }
}

/** The order form, for both new and existing orders. Selects a customer created via "New customer" on return. */
@Composable
private fun OrderEditDestination(entry: NavBackStackEntry, nav: NavHostController, onSaved: (String) -> Unit) {
    val viewModel: OrderEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val newCustomerId by entry.savedStateHandle
        .getStateFlow<String?>(NEW_CUSTOMER_ID, null)
        .collectAsStateWithLifecycle()
    LaunchedEffect(newCustomerId) {
        newCustomerId?.let {
            viewModel.selectCustomer(it)
            entry.savedStateHandle[NEW_CUSTOMER_ID] = null
        }
    }
    OrderEditScreen(
        onBack = { nav.popBackStack() },
        onSaved = onSaved,
        onNewCustomer = { nav.navigate(Routes.customerEdit()) },
        viewModel = viewModel,
    )
}
