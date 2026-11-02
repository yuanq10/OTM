package com.otm.ordertracker.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.common.EmptyState
import com.otm.ordertracker.ui.common.ProductPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onAddProduct: () -> Unit,
    onOpenProduct: (String) -> Unit,
    viewModel: ProductListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.showArchived) "Archived products" else "Products") },
                actions = {
                    TextButton(onClick = { viewModel.setShowArchived(!state.showArchived) }) {
                        Text(if (state.showArchived) "Show active" else "Show archived")
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.showArchived) {
                ExtendedFloatingActionButton(
                    onClick = onAddProduct,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New product") },
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(Modifier.padding(padding)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search products") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ProductFilter.entries) { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text("${filter.label} (${state.counts[filter] ?: 0})") },
                        )
                    }
                }
                if (state.products.isEmpty()) {
                    EmptyState(
                        when {
                            state.query.isNotBlank() -> "No products match \"${state.query.trim()}\"."
                            state.filter != ProductFilter.ALL -> "No products in \"${state.filter.label}\"."
                            state.showArchived -> "No archived products."
                            else -> "No products yet.\nTap \"New product\" to add your first product."
                        }
                    )
                } else {
                    LazyColumn {
                        items(state.products, key = { it.id }) { product ->
                            ListItem(
                                headlineContent = { Text(product.name) },
                                supportingContent = { ProductPrice(product) },
                                trailingContent = if (product.archived) {
                                    { TextButton(onClick = { viewModel.restore(product.id) }) { Text("Restore") } }
                                } else null,
                                modifier = Modifier.clickable { onOpenProduct(product.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
