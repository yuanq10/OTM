package com.otm.ordertracker.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.otm.ordertracker.OtmApp
import com.otm.ordertracker.R
import com.otm.ordertracker.data.settings.AppCurrency
import com.otm.ordertracker.data.settings.AppLanguage
import com.otm.ordertracker.ui.common.ConfirmDialog
import com.otm.ordertracker.ui.common.Money

/**
 * A language the app can be shown in, by its BCP 47 [tag]. Languages are listed by their own name
 * (Français, 简体中文) so they're recognizable whatever language the app is in.
 */
private data class LanguageOption(val tag: String, val nativeName: String)

private val languages = listOf(
    LanguageOption("en", "English"),
    LanguageOption("fr", "Français"),
    LanguageOption("zh-CN", "简体中文"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(accountEmail: String?, onLogout: () -> Unit) {
    // Changing the language recreates the screen, so reading it once is enough.
    val currentLanguage = remember { AppLanguage.current() }
    val context = LocalContext.current
    val settingsRepository = remember { (context.applicationContext as OtmApp).container.settingsRepository }
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
            RadioOptions(
                options = languages,
                isSelected = { it.tag.equals(currentLanguage, ignoreCase = true) },
                label = { it.nativeName },
                onSelect = { option ->
                    // Saved with the account so it follows the user to other devices.
                    settingsRepository.saveLanguage(option.tag)
                    AppLanguage.apply(option.tag)
                },
            )

            Text(
                stringResource(R.string.currency),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            RadioOptions(
                options = AppCurrency.entries,
                isSelected = { it == Money.currency },
                label = { currency -> stringResource(currency.labelRes, currency.symbol, currency.code) },
                onSelect = { currency ->
                    // Every price on screen updates right away; the choice is saved with the account.
                    Money.currency = currency
                    settingsRepository.saveCurrency(currency)
                },
            )
            Text(
                stringResource(R.string.currency_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                stringResource(R.string.account),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.signed_in_as, accountEmail.orEmpty()),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(onClick = { confirmLogout = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Text(stringResource(R.string.logout), Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = stringResource(R.string.logout_title),
            message = stringResource(R.string.logout_message),
            confirmLabel = stringResource(R.string.logout),
            onConfirm = onLogout,
            onDismiss = { confirmLogout = false },
            dismissLabel = stringResource(R.string.stay_logged_in),
        )
    }
}

/** A card of mutually exclusive choices, each a row with a radio button. */
@Composable
private fun <T> RadioOptions(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.selectableGroup()) {
            options.forEachIndexed { index, option ->
                val selected = isSelected(option)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            onClick = { if (!selected) onSelect(option) },
                            role = Role.RadioButton,
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(
                        label(option),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                if (index < options.lastIndex) HorizontalDivider()
            }
        }
    }
}

/** Translatable name of each currency, e.g. "US dollar ($ · USD)". */
@get:StringRes
private val AppCurrency.labelRes: Int
    get() = when (this) {
        AppCurrency.USD -> R.string.currency_usd
        AppCurrency.CAD -> R.string.currency_cad
        AppCurrency.CNY -> R.string.currency_cny
    }
