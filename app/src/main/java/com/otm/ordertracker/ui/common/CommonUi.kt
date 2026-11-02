package com.otm.ordertracker.ui.common


import androidx.compose.ui.res.stringResource
import com.otm.ordertracker.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.data.model.OrderStatus
import com.otm.ordertracker.data.model.Product

@Composable
fun StatusBadge(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** One badge per status: highlighted with a check mark when set, muted when not. Canceled orders show only "Canceled". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderStatusBadges(order: Order, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (order.isCanceled) {
            StatusBadge(stringResource(R.string.badge_canceled), colors.errorContainer, colors.onErrorContainer)
        } else {
            if (order.isCompleted) {
                StatusBadge(stringResource(R.string.badge_done, stringResource(R.string.badge_completed)), colors.tertiaryContainer, colors.onTertiaryContainer)
            }
            OrderStatus.entries.forEach { status ->
                if (order.has(status)) {
                    StatusBadge(stringResource(R.string.badge_done, stringResource(status.shortLabelRes)), colors.primaryContainer, colors.onPrimaryContainer)
                } else {
                    StatusBadge(stringResource(status.shortLabelRes), colors.surfaceVariant, colors.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}

/** Shows the effective price; when on sale, also the struck-through regular price. */
@Composable
fun ProductPrice(product: Product, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            Money.format(product.effectivePriceCents),
            fontWeight = if (product.onSale) FontWeight.Bold else null,
            color = if (product.onSale) MaterialTheme.colorScheme.primary else Color.Unspecified,
        )
        if (product.onSale) {
            Text(
                Money.format(product.priceCents),
                textDecoration = TextDecoration.LineThrough,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatusBadge(stringResource(R.string.badge_sale), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isError) colors.errorContainer else colors.secondaryContainer,
        contentColor = if (isError) colors.onErrorContainer else colors.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = stringResource(R.string.keep),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
