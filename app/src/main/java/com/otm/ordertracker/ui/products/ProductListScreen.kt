package com.otm.ordertracker.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                FloatingActionButton(onClick = onAddProduct) {
                    Icon(Icons.Filled.Add, contentDescription = "Add product")
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.products.isEmpty() -> EmptyState(
                if (state.showArchived) "No archived products." else "No products yet.\nTap + to add your first product.",
                Modifier.padding(padding),
            )
            else -> LazyColumn(contentPadding = padding) {
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
