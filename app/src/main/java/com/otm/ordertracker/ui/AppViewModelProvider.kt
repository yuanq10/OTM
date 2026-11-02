package com.otm.ordertracker.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.otm.ordertracker.AppContainer
import com.otm.ordertracker.OtmApp
import com.otm.ordertracker.ui.customers.CustomerDetailViewModel
import com.otm.ordertracker.ui.customers.CustomerEditViewModel
import com.otm.ordertracker.ui.customers.CustomerListViewModel
import com.otm.ordertracker.ui.orders.NewOrderViewModel
import com.otm.ordertracker.ui.orders.OrderDetailViewModel
import com.otm.ordertracker.ui.orders.OrderListViewModel
import com.otm.ordertracker.ui.products.ProductEditViewModel
import com.otm.ordertracker.ui.products.ProductListViewModel

/** Builds every ViewModel from the app's [AppContainer]. Navigation arguments arrive via SavedStateHandle. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { OrderListViewModel(container().orderRepository) }
        initializer {
            NewOrderViewModel(
                createSavedStateHandle(),
                container().customerRepository,
                container().productRepository,
                container().orderRepository,
            )
        }
        initializer { OrderDetailViewModel(createSavedStateHandle(), container().orderRepository) }

        initializer { CustomerListViewModel(container().customerRepository) }
        initializer { CustomerEditViewModel(createSavedStateHandle(), container().customerRepository) }
        initializer {
            CustomerDetailViewModel(
                createSavedStateHandle(),
                container().customerRepository,
                container().orderRepository,
            )
        }

        initializer { ProductListViewModel(container().productRepository) }
        initializer { ProductEditViewModel(createSavedStateHandle(), container().productRepository) }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as OtmApp).container
