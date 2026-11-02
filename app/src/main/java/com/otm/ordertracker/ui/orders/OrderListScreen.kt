package com.otm.ordertracker.ui.orders


import androidx.compose.ui.res.stringResource
import com.otm.ordertracker.R
import com.otm.ordertracker.ui.common.labelRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange as DateRangeIcon
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.model.DateRange
import com.otm.ordertracker.data.model.OrderFilter
import com.otm.ordertracker.data.model.OrderTab
import com.otm.ordertracker.data.model.Product
import com.otm.ordertracker.data.model.SearchSelection
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.common.ConfirmDialog
import com.otm.ordertracker.ui.common.EmptyState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    viewModel: OrderListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.orders_title)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewOrder,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_order)) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
                OrderTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(stringResource(R.string.label_with_count, stringResource(tab.labelRes), state.tabCounts[tab] ?: 0)) },
                    )
                }
            }
            SearchBar(
                query = state.query,
                selection = state.selection,
                onQueryChange = viewModel::onQueryChange,
                onClear = viewModel::clearSearch,
            )
            if (state.customerSuggestions.isNotEmpty() || state.productSuggestions.isNotEmpty()) {
                Suggestions(
                    customers = state.customerSuggestions,
                    products = state.productSuggestions,
                    onSelect = viewModel::select,
                )
            }
            when (state.tab) {
                OrderTab.IN_PROGRESS -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(OrderFilter.entries) { filter ->
                        val count = state.counts[filter] ?: 0
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(stringResource(R.string.label_with_count, stringResource(filter.labelRes), count)) },
                        )
                    }
                }
                OrderTab.COMPLETED -> DateRangeFilter(
                    range = state.dateRange,
                    onRangeChange = viewModel::setDateRange,
                )
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.orders.isEmpty() -> EmptyState(
                    when {
                        state.isSearching || state.dateRange != null && state.tab == OrderTab.COMPLETED ->
                            stringResource(R.string.orders_empty_search)
                        state.tab == OrderTab.COMPLETED -> stringResource(R.string.orders_empty_completed)
                        state.filter == OrderFilter.ALL -> stringResource(R.string.orders_empty_in_progress)
                        else -> stringResource(R.string.orders_empty_filter, stringResource(state.filter.labelRes))
                    }
                )
                else -> LazyColumn(
                    // Extra bottom space so the last card isn't hidden behind the button.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.orders, key = { it.id }) { order ->
                        OrderRow(order, onClick = { onOpenOrder(order.id) }, showCustomer = true)
                    }
                }
            }
        }
    }
}

/** Free-text search box, or — once a customer/product suggestion is picked — a chip showing that selection. */
@Composable
private fun SearchBar(
    query: String,
    selection: SearchSelection?,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)
    if (selection != null) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            InputChip(
                selected = true,
                onClick = onClear,
                label = {
                    val format = if (selection is SearchSelection.ByCustomer) R.string.search_selected_customer
                    else R.string.search_selected_product
                    Text(stringResource(format, selection.label))
                },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search)) },
            )
        }
    } else {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.orders_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            } else null,
            singleLine = true,
            modifier = modifier,
        )
    }
}

/** Shortcuts to show every order for a matching customer, or every order containing a matching product. */
@Composable
private fun Suggestions(
    customers: List<Customer>,
    products: List<Product>,
    onSelect: (SearchSelection) -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp)) {
        val removed = stringResource(R.string.removed_suffix)
        customers.forEach { customer ->
            ListItem(
                overlineContent = { Text(stringResource(R.string.customer)) },
                headlineContent = {
                    Text(stringResource(R.string.search_all_orders_for, customer.fullName) + if (customer.archived) removed else "")
                },
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                modifier = Modifier.clickable { onSelect(SearchSelection.ByCustomer(customer.id, customer.fullName)) },
            )
        }
        products.forEach { product ->
            ListItem(
                overlineContent = { Text(stringResource(R.string.product)) },
                headlineContent = {
                    Text(stringResource(R.string.search_all_orders_with, product.name) + if (product.archived) removed else "")
                },
                leadingContent = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                modifier = Modifier.clickable { onSelect(SearchSelection.ByProduct(product.id, product.name)) },
            )
        }
    }
}

/** "Completed between" control: a button that opens a date range picker, plus a clear button once set. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilter(range: DateRange?, onRangeChange: (DateRange?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val format = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = range != null,
            onClick = { picking = true },
            leadingIcon = { Icon(Icons.Filled.DateRangeIcon, contentDescription = null) },
            label = {
                Text(
                    if (range == null) stringResource(R.string.completed_any_date)
                    else stringResource(R.string.completed_range, range.start.format(format), range.end.format(format))
                )
            },
            trailingIcon = if (range != null) {
                {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear_date_range),
                        modifier = Modifier.clickable { onRangeChange(null) },
                    )
                }
            } else null,
        )
    }

    if (picking) {
        // The picker works in UTC midnight millis; convert to and from calendar days.
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = range?.start?.toPickerMillis(),
            initialSelectedEndDateMillis = range?.end?.toPickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val start = state.selectedStartDateMillis?.toPickerDate()
                        if (start != null) {
                            // A single tapped day means just that day.
                            onRangeChange(DateRange(start, state.selectedEndDateMillis?.toPickerDate() ?: start))
                        }
                        picking = false
                    },
                ) { Text(stringResource(R.string.apply)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DateRangePicker(
                state = state,
                title = { Text(stringResource(R.string.completed_between), Modifier.padding(start = 24.dp, top = 16.dp)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toPickerDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
