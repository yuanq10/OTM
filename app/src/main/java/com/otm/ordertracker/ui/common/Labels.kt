package com.otm.ordertracker.ui.common

import androidx.annotation.StringRes
import com.otm.ordertracker.R
import com.otm.ordertracker.data.model.OrderFilter
import com.otm.ordertracker.data.model.OrderStatus
import com.otm.ordertracker.data.model.OrderTab

// Translatable labels for model enums. The model stays free of Android resources so it can be unit tested.

@get:StringRes
val OrderStatus.labelRes: Int
    get() = when (this) {
        OrderStatus.PAID_TO_ME -> R.string.status_paid_to_me
        OrderStatus.PAID_TO_SUPPLIER -> R.string.status_paid_to_supplier
        OrderStatus.SHIPPED -> R.string.status_shipped
        OrderStatus.RECEIVED -> R.string.status_received
    }

/** Shorter label for the badges on order cards. */
@get:StringRes
val OrderStatus.shortLabelRes: Int
    get() = when (this) {
        OrderStatus.PAID_TO_ME -> R.string.status_paid_to_me
        OrderStatus.PAID_TO_SUPPLIER -> R.string.status_paid_to_supplier_short
        OrderStatus.SHIPPED -> R.string.status_shipped
        OrderStatus.RECEIVED -> R.string.status_received
    }

@get:StringRes
val OrderFilter.labelRes: Int
    get() = when (this) {
        OrderFilter.ALL -> R.string.filter_all
        OrderFilter.NOT_PAID_TO_ME -> R.string.filter_not_paid_to_me
        OrderFilter.SUPPLIER_NOT_PAID -> R.string.filter_supplier_not_paid
        OrderFilter.NOT_SHIPPED -> R.string.filter_not_shipped
        OrderFilter.NOT_RECEIVED -> R.string.filter_not_received
    }

@get:StringRes
val OrderTab.labelRes: Int
    get() = when (this) {
        OrderTab.IN_PROGRESS -> R.string.tab_in_progress
        OrderTab.COMPLETED -> R.string.tab_completed
    }
