package com.otm.ordertracker.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otm.ordertracker.data.model.OrderFilter
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.common.ConfirmDialog
import com.otm.ordertracker.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: OrderListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders") },
                actions = {
                    IconButton(onClick = { confirmSignOut = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewOrder,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New order") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(OrderFilter.entries) { filter ->
                    val count = state.counts[filter] ?: 0
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(if (filter == OrderFilter.ALL) filter.label else "${filter.label} ($count)") },
                    )
                }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.orders.isEmpty() -> EmptyState(
                    if (state.filter == OrderFilter.ALL) "No orders yet.\nTap \"New order\" to create one."
                    else "No orders in \"${state.filter.label}\"."
                )
                else -> LazyColumn(
                    // Extra bottom space so the last card isn't hidden behind the button.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.orders, key = { it.id }) { order ->
                        OrderRow(order, onClick = { onOpenOrder(order.id) }, showCustomer = true)
                    }
                }
            }
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = "Your data stays saved in your account. Sign in again to see it.",
            confirmLabel = "Sign out",
            onConfirm = onSignOut,
            onDismiss = { confirmSignOut = false },
            dismissLabel = "Stay signed in",
        )
    }
}
