package com.otm.ordertracker.ui.customers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otm.ordertracker.data.model.Customer
import com.otm.ordertracker.data.repo.CustomerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomerForm(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val notes: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
)

class CustomerEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: CustomerRepository,
) : ViewModel() {
    /** Null when adding a new customer. */
    val customerId: String? = savedStateHandle["customerId"]
    val isNew: Boolean get() = customerId == null

    var form by mutableStateOf(CustomerForm())
        private set
    var loaded by mutableStateOf(customerId == null)
        private set

    /** Used to warn about duplicate names, since customers are identified by name. */
    val allCustomers: StateFlow<List<Customer>> =
        repository.customers().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (customerId != null) {
            viewModelScope.launch {
                repository.customer(customerId).collect { customer ->
                    if (customer != null && !loaded) {
                        form = CustomerForm(customer.firstName, customer.lastName, customer.phone, customer.notes)
                        loaded = true
                    }
                }
            }
        }
    }

    fun onFirstNameChange(value: String) { form = form.copy(firstName = value, firstNameError = null) }
    fun onLastNameChange(value: String) { form = form.copy(lastName = value, lastNameError = null) }
    fun onPhoneChange(value: String) { form = form.copy(phone = value) }
    fun onNotesChange(value: String) { form = form.copy(notes = value) }

    /** Another active customer with the same first and last name, if any. */
    fun duplicateOf(customers: List<Customer>): Customer? =
        if (form.firstName.isBlank() || form.lastName.isBlank()) null
        else customers.firstOrNull { !it.archived && it.id != customerId && it.hasSameName(form.firstName, form.lastName) }

    /** Validates and saves. Returns the customer's id when saved, null when the form is invalid. */
    fun save(): String? {
        val firstNameError = if (form.firstName.isBlank()) "Enter a first name" else null
        val lastNameError = if (form.lastName.isBlank()) "Enter a last name" else null
        if (firstNameError != null || lastNameError != null) {
            form = form.copy(firstNameError = firstNameError, lastNameError = lastNameError)
            return null
        }
        return if (customerId == null) {
            repository.add(form.firstName, form.lastName, form.phone, form.notes)
        } else {
            repository.update(customerId, form.firstName, form.lastName, form.phone, form.notes)
            customerId
        }
    }
}
