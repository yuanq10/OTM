package com.otm.ordertracker.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    onAddCustomer: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    viewModel: CustomerListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.showArchived) "Archived customers" else "Customers") },
                actions = {
                    TextButton(onClick = { viewModel.setShowArchived(!state.showArchived) }) {
                        Text(if (state.showArchived) "Show active" else "Show archived")
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.showArchived) {
                FloatingActionButton(onClick = onAddCustomer) {
                    Icon(Icons.Filled.Add, contentDescription = "Add customer")
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search by name or phone") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.customers.isEmpty() -> EmptyState(
                    when {
                        state.query.isNotBlank() -> "No customers match \"${state.query}\"."
                        state.showArchived -> "No archived customers."
                        else -> "No customers yet.\nTap + to add your first customer."
                    }
                )
                else -> LazyColumn {
                    items(state.customers, key = { it.id }) { customer ->
                        ListItem(
                            headlineContent = { Text(customer.fullName) },
                            supportingContent = if (customer.phone.isNotBlank()) {
                                { Text(customer.phone) }
                            } else null,
                            trailingContent = if (customer.archived) {
                                { TextButton(onClick = { viewModel.restore(customer.id) }) { Text("Restore") } }
                            } else null,
                            modifier = Modifier.clickable { onOpenCustomer(customer.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
