package com.otm.ordertracker.ui.orders


import androidx.compose.ui.res.stringResource
import com.otm.ordertracker.R
import com.otm.ordertracker.ui.common.labelRes
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
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
import com.otm.ordertracker.data.model.OrderStatus
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
    onEdit: (String) -> Unit,
    viewModel: OrderDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmCancel by remember { mutableStateOf(false) }

    val messageText = message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        messageText?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.order?.name?.ifBlank { null } ?: stringResource(R.string.order_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    val order = state.order
                    if (order != null && !order.isCanceled) {
                        TextButton(onClick = { onEdit(order.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Text(stringResource(R.string.edit), Modifier.padding(start = 8.dp))
                        }
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
            order == null -> EmptyState(stringResource(R.string.order_missing), Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (order.isCanceled) {
                    InfoBanner(
                        stringResource(R.string.order_canceled_banner, order.canceledAt?.let(::formatDateTime) ?: "-"),
                        isError = true,
                    )
                }
                order.completedAt?.let { completedAt ->
                    InfoBanner(stringResource(R.string.order_completed_banner, formatDateTime(completedAt)))
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
                        stringResource(R.string.order_placed, formatDateTime(order.createdAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }

                ItemsCard(order)

                if (order.notes.isNotBlank()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.notes), style = MaterialTheme.typography.titleSmall)
                            Text(order.notes)
                        }
                    }
                }

                StatusCard(order = order, onStatusChange = viewModel::setStatus)

                if (!order.isCanceled) {
                    if (order.isCompleted) {
                        OutlinedButton(onClick = viewModel::reopen, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.reopen_order))
                        }
                    } else {
                        Button(onClick = viewModel::complete, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.mark_completed))
                        }
                        OutlinedButton(
                            onClick = { confirmCancel = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.cancel_order)) }
                    }
                }
            }
        }
    }

    if (confirmCancel) {
        ConfirmDialog(
            title = stringResource(R.string.cancel_order_title),
            message = stringResource(R.string.cancel_order_message),
            confirmLabel = stringResource(R.string.cancel_order),
            // A canceled order disappears from the Orders page, so leave its detail page too.
            onConfirm = { viewModel.cancel(); onBack() },
            onDismiss = { confirmCancel = false },
        )
    }
}

@Composable
private fun ItemsCard(order: Order) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.items), style = MaterialTheme.typography.titleSmall)
            order.items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.item_line, item.quantity, item.productName))
                        Text(
                            stringResource(R.string.price_each, Money.format(item.unitPriceCents)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(Money.format(item.lineTotalCents))
                }
            }
            HorizontalDivider()
            Row {
                Text(stringResource(R.string.total), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(Money.format(order.totalCents), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** One switch per status. Statuses are independent, so every switch is always available (unless canceled). */
@Composable
private fun StatusCard(order: Order, onStatusChange: (OrderStatus, Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.status), style = MaterialTheme.typography.titleSmall)
            OrderStatus.entries.forEach { status ->
                StatusRow(
                    label = stringResource(status.labelRes),
                    date = order.dateOf(status),
                    enabled = !order.isCanceled,
                    onChange = { onStatusChange(status, it) },
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    /** When the status was set; null when it isn't set. */
    date: Long?,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (date != null) {
                Text(
                    formatDateTime(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = date != null, onCheckedChange = onChange, enabled = enabled)
    }
}
