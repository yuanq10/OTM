package com.otm.ordertracker.ui.orders

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
import com.otm.ordertracker.data.model.Product
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.common.Money
import com.otm.ordertracker.ui.common.ProductPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    onNewCustomer: () -> Unit,
    viewModel: NewOrderViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New order") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text("Total", style = MaterialTheme.typography.labelMedium)
                    Text(
                        Money.format(state.totalCents),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = { viewModel.save()?.let(onCreated) },
                    enabled = state.canSave,
                    modifier = Modifier.padding(end = 16.dp),
                ) { Text("Create order") }
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
            item { SectionTitle("Customer") }
            val selected = state.selectedCustomer
            if (selected != null) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(selected.fullName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.selectCustomer(null) }) { Text("Change") }
                        }
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = state.customerQuery,
                        onValueChange = viewModel::onCustomerQueryChange,
                        placeholder = { Text("Search customers") },
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
                    item { HintText("No customers match \"${state.customerQuery}\".") }
                }
                item {
                    TextButton(onClick = onNewCustomer) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("New customer", Modifier.padding(start = 8.dp))
                    }
                }
            }

            item { SectionTitle("Products", Modifier.padding(top = 8.dp)) }
            if (state.products.isEmpty()) {
                item { HintText("No products yet. Add some in the Products tab first.") }
            }
            items(state.products, key = { "p-" + it.id }) { product ->
                ProductQuantityRow(
                    product = product,
                    quantity = state.quantities[product.id] ?: 0,
                    onQuantityChange = { viewModel.setQuantity(product.id, it) },
                )
                HorizontalDivider()
            }

            item {
                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text("Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductQuantityRow(product: Product, quantity: Int, onQuantityChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.bodyLarge)
            ProductPrice(product)
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
