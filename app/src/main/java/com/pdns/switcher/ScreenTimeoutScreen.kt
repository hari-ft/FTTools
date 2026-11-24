package com.pdns.switcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ScreenTimeoutScreen(
    currentMs: Int?,
    baseOptions: List<TimeoutOption>,
    customMs: List<Int>,
    canWrite: Boolean,
    onSelect: (Int) -> Unit,
    onAddCustom: (Int) -> Unit,
    onDeleteCustom: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var dialogOpen by remember { mutableStateOf(false) }
    val baseMs = baseOptions.map { it.ms }.toSet()
    val options = (baseOptions + customMs.filter { it !in baseMs }.map { TimeoutOption(it, ScreenTimeout.label(it), fromPhone = false) })
        .sortedBy { it.ms }
    val currentLabel = currentMs?.let { ms -> options.firstOrNull { it.ms == ms }?.label ?: ScreenTimeout.label(ms) }
    val fromPhone = baseOptions.any { it.fromPhone }

    ToolScaffold(
        title = "Screen timeout",
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialogOpen = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add custom") },
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad).fillMaxSize(),
        ) {
            if (!canWrite) item { SystemSettingsAccessCard() }
            item {
                StatusCard(
                    "Current: ${currentLabel ?: "Unknown"}",
                    if (fromPhone) "Options are taken from your phone's Settings"
                    else "Your phone's options couldn't be read, so standard options are shown",
                )
            }
            items(options, key = { it.ms }) { option ->
                val ms = option.ms
                val isCustom = ms in customMs && ms !in baseMs
                Card(Modifier.fillMaxWidth().clickable { onSelect(ms) }) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(option.label, style = MaterialTheme.typography.titleMedium)
                            if (isCustom) {
                                Text(
                                    "Custom",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (isCustom) {
                            IconButton(onClick = { onDeleteCustom(ms) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        RadioButton(selected = ms == currentMs, onClick = { onSelect(ms) })
                    }
                }
            }
        }
    }

    if (dialogOpen) {
        CustomTimeoutDialog(
            onDismiss = { dialogOpen = false },
            onConfirm = { onAddCustom(it); dialogOpen = false },
        )
    }
}

@Composable
private fun SystemSettingsAccessCard() {
    val ctx = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Permission needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Allow FTTools to modify system settings. No computer is needed for this one.")
            Button(onClick = { ctx.startActivity(Grants.systemSettingsAccessIntent(ctx)) }) {
                Text("Open permission settings")
            }
        }
    }
}

private enum class TimeUnitChoice(val label: String, val ms: Int) {
    Seconds("Seconds", 1_000),
    Minutes("Minutes", 60_000),
    Hours("Hours", 3_600_000),
}

@Composable
private fun CustomTimeoutDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(TimeUnitChoice.Minutes) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom timeout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(5); error = null },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeUnitChoice.entries.forEach { choice ->
                        FilterChip(
                            selected = unit == choice,
                            onClick = { unit = choice; error = null },
                            label = { Text(choice.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ms = amount.toLongOrNull()?.times(unit.ms)
                error = when {
                    ms == null -> "Enter a number"
                    ms < ScreenTimeout.MIN_MS -> "Minimum is ${ScreenTimeout.label(ScreenTimeout.MIN_MS)}"
                    ms > ScreenTimeout.MAX_MS -> "Maximum is ${ScreenTimeout.label(ScreenTimeout.MAX_MS)}"
                    else -> null
                }
                if (error == null && ms != null) onConfirm(ms.toInt())
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
