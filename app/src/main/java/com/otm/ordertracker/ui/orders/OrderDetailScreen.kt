package com.otm.ordertracker.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.common.ConfirmDialog
import com.otm.ordertracker.ui.common.EmptyState
import com.otm.ordertracker.ui.common.InfoBanner
import com.otm.ordertracker.ui.common.Money
import com.otm.ordertracker.ui.common.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    viewModel: OrderDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmCancel by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val order = state.order
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            order == null -> EmptyState("This order no longer exists.", Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (order.isCanceled) {
                    InfoBanner(
                        "Canceled on ${order.canceledAt?.let(::formatDateTime) ?: "-"}. This order is kept for history and can't be changed.",
                        isError = true,
                    )
                }

                // Customer and date
                Column {
                    TextButton(
                        onClick = { onOpenCustomer(order.customerId) },
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                    ) {
                        Text(order.customerName, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "Placed ${formatDateTime(order.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }

                ItemsCard(order)

                if (order.notes.isNotBlank()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Notes", style = MaterialTheme.typography.titleSmall)
                            Text(order.notes)
                        }
                    }
                }

                StatusCard(
                    order = order,
                    onPaidChange = viewModel::setPaid,
                    onShippedChange = viewModel::setShipped,
                    onReceivedChange = viewModel::setReceived,
                )

                if (!order.isCanceled) {
                    OutlinedButton(
                        onClick = { confirmCancel = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancel order") }
                }
            }
        }
    }

    if (confirmCancel) {
        ConfirmDialog(
            title = "Cancel this order?",
            message = "The order stays in the customer's history marked as canceled. This can't be undone.",
            confirmLabel = "Cancel order",
            onConfirm = viewModel::cancel,
            onDismiss = { confirmCancel = false },
        )
    }
}

@Composable
private fun ItemsCard(order: Order) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Items", style = MaterialTheme.typography.titleSmall)
            order.items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${item.quantity} × ${item.productName}")
                        Text(
                            "${Money.format(item.unitPriceCents)} each",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(Money.format(item.lineTotalCents))
                }
            }
            HorizontalDivider()
            Row {
                Text("Total", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(Money.format(order.totalCents), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatusCard(
    order: Order,
    onPaidChange: (Boolean) -> Unit,
    onShippedChange: (Boolean) -> Unit,
    onReceivedChange: (Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Status", style = MaterialTheme.typography.titleSmall)
            StatusRow(
                label = "Paid",
                checked = order.isPaid,
                date = order.paidAt,
                enabled = order.canTogglePaid,
                hint = null,
                onChange = onPaidChange,
            )
            StatusRow(
                label = "Shipped",
                checked = order.isShipped,
                date = order.shippedAt,
                enabled = order.canToggleShipped,
                hint = if (!order.isCanceled && order.isReceived) "Turn off Received to change" else null,
                onChange = onShippedChange,
            )
            StatusRow(
                label = "Received",
                checked = order.isReceived,
                date = order.receivedAt,
                enabled = order.canToggleReceived,
                hint = if (!order.isCanceled && !order.isShipped) "Mark as shipped first" else null,
                onChange = onReceivedChange,
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    checked: Boolean,
    date: Long?,
    enabled: Boolean,
    hint: String?,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            val detail = when {
                checked && date != null -> formatDateTime(date)
                hint != null -> hint
                else -> null
            }
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
