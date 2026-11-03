package com.otm.ordertracker.ui.products


import androidx.compose.ui.res.stringResource
import com.otm.ordertracker.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.common.ConfirmDialog
import com.otm.ordertracker.ui.common.InfoBanner
import com.otm.ordertracker.ui.common.ProductStockBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onDone: () -> Unit,
    viewModel: ProductEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val form = viewModel.form
    var confirmRemove by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (viewModel.isNew) R.string.new_product else R.string.edit_product)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!viewModel.isNew) {
                        if (viewModel.archived) {
                            TextButton(onClick = { viewModel.markArchived(false) }) { Text(stringResource(R.string.restore)) }
                        } else {
                            IconButton(onClick = { confirmRemove = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove_product))
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!viewModel.loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (viewModel.archived) {
                InfoBanner(stringResource(R.string.product_removed_banner))
            }
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.name)) },
                isError = form.nameError != null,
                supportingText = if (form.nameError != null) { { Text(stringResource(form.nameError)) } } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text(stringResource(R.string.regular_price)) },
                isError = form.priceError != null,
                supportingText = if (form.priceError != null) { { Text(stringResource(form.priceError)) } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.in_stock), style = MaterialTheme.typography.bodyLarge)
                    ProductStockBadge(inStock = form.inStock)
                }
                Switch(checked = form.inStock, onCheckedChange = viewModel::onInStockChange)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.on_sale), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = form.onSale, onCheckedChange = viewModel::onSaleChange)
            }
            if (form.onSale) {
                OutlinedTextField(
                    value = form.salePrice,
                    onValueChange = viewModel::onSalePriceChange,
                    label = { Text(stringResource(R.string.sale_price)) },
                    isError = form.salePriceError != null,
                    supportingText = if (form.salePriceError != null) { { Text(stringResource(form.salePriceError)) } } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                stringResource(R.string.price_change_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { if (viewModel.save()) onDone() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = stringResource(R.string.remove_product_title),
            message = stringResource(R.string.remove_product_message),
            confirmLabel = stringResource(R.string.remove),
            onConfirm = { viewModel.markArchived(true); onDone() },
            onDismiss = { confirmRemove = false },
        )
    }
}
