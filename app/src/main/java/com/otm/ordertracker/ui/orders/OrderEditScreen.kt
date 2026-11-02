package com.otm.ordertracker.ui.orders


import androidx.compose.ui.res.stringResource
import com.otm.ordertracker.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otm.ordertracker.ui.common.Money
import com.otm.ordertracker.ui.common.ProductPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditScreen(
    onBack: () -> Unit,
    /** Called with the order's id after it is created or saved. */
    onSaved: (String) -> Unit,
    onNewCustomer: () -> Unit,
    viewModel: OrderEditViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (viewModel.isNew) R.string.new_order else R.string.edit_order)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.total), style = MaterialTheme.typography.labelMedium)
                    Text(
                        Money.format(state.totalCents),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = { viewModel.save()?.let(onSaved) },
                    enabled = state.canSave,
                    modifier = Modifier.padding(end = 16.dp),
                ) { Text(stringResource(if (viewModel.isNew) R.string.create_order else R.string.save_changes)) }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else LazyColumn(
            modifier = Modifier.padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.order_name_optional)) },
                    placeholder = { Text(stringResource(R.string.order_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            item { SectionTitle(stringResource(R.string.customer)) }
            val selected = state.selectedCustomer
            if (selected != null) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(selected.fullName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.selectCustomer(null) }) { Text(stringResource(R.string.change)) }
                        }
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = state.customerQuery,
                        onValueChange = viewModel::onCustomerQueryChange,
                        placeholder = { Text(stringResource(R.string.search_customers)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                items(state.customerMatches, key = { "c-" + it.id }) { customer ->
                    ListItem(
                        headlineContent = { Text(customer.fullName) },
                        supportingContent = if (customer.phone.isNotBlank()) {
                            { Text(customer.phone) }
                        } else null,
                        modifier = Modifier.clickable { viewModel.selectCustomer(customer.id) },
                    )
                    HorizontalDivider()
                }
                if (state.hasCustomers && state.customerMatches.isEmpty()) {
                    item { HintText(stringResource(R.string.no_customers_match, state.customerQuery)) }
                }
                item {
                    TextButton(onClick = onNewCustomer) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.new_customer), Modifier.padding(start = 8.dp))
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.products), Modifier.padding(top = 8.dp)) }
            if (state.lines.isEmpty()) {
                item { HintText(stringResource(R.string.no_products_for_order)) }
            }
            items(state.lines, key = { "p-" + it.productId }) { line ->
                ProductQuantityRow(
                    line = line,
                    quantity = state.quantities[line.productId] ?: 0,
                    onQuantityChange = { viewModel.setQuantity(line.productId, it) },
                )
                HorizontalDivider()
            }

            item {
                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text(stringResource(R.string.notes_optional)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductQuantityRow(line: ProductLine, quantity: Int, onQuantityChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.bodyLarge)
            if (line.locked || line.product == null) {
                // Already on the order: show the price it was ordered at, and today's price if different.
                val current = line.product?.effectivePriceCents
                val note = when {
                    line.product == null || line.product.archived -> " · " + stringResource(R.string.product_removed)
                    current != line.unitPriceCents -> " · " + stringResource(R.string.price_now, Money.format(current!!))
                    else -> ""
                }
                Text(
                    stringResource(R.string.order_price, Money.format(line.unitPriceCents)) + note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ProductPrice(line.product)
            }
        }
        TextButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 0) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp),
        )
        TextButton(onClick = { onQuantityChange(quantity + 1) }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
private fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
