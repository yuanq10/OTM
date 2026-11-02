package com.otm.ordertracker.ui.orders


import androidx.compose.ui.res.pluralStringResource
import com.otm.ordertracker.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otm.ordertracker.data.model.Order
import com.otm.ordertracker.ui.common.Money
import com.otm.ordertracker.ui.common.OrderStatusBadges
import com.otm.ordertracker.ui.common.formatDate

/** Order summary card used in the orders list and a customer's order history. */
@Composable
fun OrderRow(
    order: Order,
    onClick: () -> Unit,
    showCustomer: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (order.isCanceled) 0.6f else 1f),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val itemsLabel = pluralStringResource(R.plurals.item_count, order.itemCount, order.itemCount)
                    // Title is the order name when set; otherwise the customer (or date in a customer's history).
                    val fallbackTitle = if (showCustomer) order.customerName else formatDate(order.createdAt)
                    val details = buildList {
                        if (order.name.isNotBlank() && showCustomer) add(order.customerName)
                        if (order.name.isNotBlank() || showCustomer) add(formatDate(order.createdAt))
                        add(itemsLabel)
                    }
                    Text(
                        order.name.ifBlank { fallbackTitle },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    Money.format(order.totalCents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            OrderStatusBadges(order)
        }
    }
}
