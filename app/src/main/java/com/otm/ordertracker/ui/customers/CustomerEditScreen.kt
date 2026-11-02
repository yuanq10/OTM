package com.otm.ordertracker.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otm.ordertracker.ui.AppViewModelProvider
import com.otm.ordertracker.ui.common.InfoBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerEditScreen(
    onBack: () -> Unit,
    /** Called with the customer's id after a successful save. */
    onSaved: (String) -> Unit,
    viewModel: CustomerEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val form = viewModel.form
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val duplicate = viewModel.duplicateOf(allCustomers)
    val nameOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "New customer" else "Edit customer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = form.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("First name") },
                isError = form.firstNameError != null,
                supportingText = if (form.firstNameError != null) { { Text(form.firstNameError) } } else null,
                singleLine = true,
                keyboardOptions = nameOptions,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text("Last name") },
                isError = form.lastNameError != null,
                supportingText = if (form.lastNameError != null) { { Text(form.lastNameError) } } else null,
                singleLine = true,
                keyboardOptions = nameOptions,
                modifier = Modifier.fillMaxWidth(),
            )
            if (duplicate != null) {
                InfoBanner("A customer named ${duplicate.fullName} already exists. You can still save, but they'll be hard to tell apart.")
            }
            OutlinedTextField(
                value = form.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Phone (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.save()?.let(onSaved) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (duplicate != null) "Save anyway" else "Save") }
        }
    }
}
