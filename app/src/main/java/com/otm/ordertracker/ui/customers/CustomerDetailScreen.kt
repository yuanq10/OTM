package com.otm.ordertracker.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.common.ConfirmDialog
import com.otm.ordertracker.ui.common.EmptyState
import com.otm.ordertracker.ui.common.InfoBanner
import com.otm.ordertracker.ui.common.Money
import com.otm.ordertracker.ui.orders.OrderRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onNewOrder: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    viewModel: CustomerDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val customer = state.customer
    var confirmRemove by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.fullName ?: "Customer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (customer != null) {
                        IconButton(onClick = { onEdit(customer.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit customer")
                        }
                        if (customer.archived) {
                            TextButton(onClick = { viewModel.setArchived(false) }) { Text("Restore") }
                        } else {
                            IconButton(onClick = { confirmRemove = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove customer")
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            customer == null -> EmptyState("This customer no longer exists.", Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (customer.archived) {
                    item {
                        InfoBanner("This customer is removed. Their order history is kept, but they can't be picked for new orders.")
                    }
                }
                if (customer.phone.isNotBlank() || customer.notes.isNotBlank()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (customer.phone.isNotBlank()) Text("Phone: ${customer.phone}")
                            if (customer.notes.isNotBlank()) {
                                Text(customer.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Orders", state.orderCount.toString(), Modifier.weight(1f))
                        Stat("Unpaid", state.unpaidCount.toString(), Modifier.weight(1f))
                        Stat("Not received", state.notReceivedCount.toString(), Modifier.weight(1f))
                        Stat("Total", Money.format(state.totalSpentCents), Modifier.weight(1f))
                    }
                }
                if (!customer.archived) {
                    item {
                        Button(onClick = { onNewOrder(customer.id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("New order for ${customer.firstName}", Modifier.padding(start = 8.dp))
                        }
                    }
                }
                item {
                    Text("Order history", style = MaterialTheme.typography.titleMedium)
                }
                if (state.orders.isEmpty()) {
                    item {
                        Text("No orders yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.orders, key = { it.id }) { order ->
                    OrderRow(order, onClick = { onOpenOrder(order.id) }, showCustomer = false)
                }
            }
        }
    }

    if (confirmRemove && customer != null) {
        ConfirmDialog(
            title = "Remove ${customer.fullName}?",
            message = "They won't be available for new orders. Their order history is kept. You can restore them from \"Show archived\".",
            confirmLabel = "Remove",
            onConfirm = { viewModel.setArchived(true) },
            onDismiss = { confirmRemove = false },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
