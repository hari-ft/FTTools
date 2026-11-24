package com.pdns.switcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun privateDnsSummary(state: PrivateDns.State): String = when (state.mode) {
    PrivateDns.MODE_OFF -> "Off"
    PrivateDns.MODE_HOSTNAME -> state.hostname ?: "On"
    else -> "Auto"
}

@Composable
fun PrivateDnsScreen(
    entries: List<DnsEntry>,
    state: PrivateDns.State,
    hasPermission: Boolean,
    onToggle: (DnsEntry, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    ToolScaffold(
        title = "Private DNS",
        onBack = onBack,
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Manage providers")
            }
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad).fillMaxSize(),
        ) {
            if (!hasPermission) item { SecureGrantCard() }
            item {
                val detail = when (state.mode) {
                    PrivateDns.MODE_OFF -> "Private DNS is turned off"
                    PrivateDns.MODE_HOSTNAME -> state.hostname.orEmpty()
                    else -> "Uses DNS-over-TLS when the network supports it"
                }
                val title = when (state.mode) {
                    PrivateDns.MODE_OFF -> "Off"
                    PrivateDns.MODE_HOSTNAME -> "On"
                    else -> "Auto"
                }
                StatusCard("Current: $title", detail)
            }
            items(entries, key = { it.id }) { e ->
                val on = state.activeHost.equals(e.hostname, ignoreCase = true)
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                e.hostname,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = on, onCheckedChange = { onToggle(e, it) })
                    }
                }
            }
            if (entries.isEmpty()) item {
                Text("No providers yet. Add one with the gear icon.", Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun DnsProvidersScreen(
    entries: List<DnsEntry>,
    activeHost: String?,
    onBack: () -> Unit,
    onSave: (old: DnsEntry?, new: DnsEntry) -> Unit,
    onDelete: (DnsEntry) -> Unit,
) {
    var dialogOpen by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<DnsEntry?>(null) }
    var deleting by remember { mutableStateOf<DnsEntry?>(null) }

    ToolScaffold(
        title = "DNS providers",
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editTarget = null; dialogOpen = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add provider") },
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad).fillMaxSize(),
        ) {
            items(entries, key = { it.id }) { e ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                e.hostname,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editTarget = e; dialogOpen = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { deleting = e }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    if (dialogOpen) {
        val target = editTarget
        DnsEditDialog(
            entry = target,
            existing = entries,
            onDismiss = { dialogOpen = false },
            onConfirm = { onSave(target, it); dialogOpen = false },
        )
    }

    deleting?.let { e ->
        val isActive = e.hostname.equals(activeHost, ignoreCase = true)
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${e.label}?") },
            text = { if (isActive) Text("This provider is on now. Deleting it will turn Private DNS off.") },
            confirmButton = { TextButton(onClick = { onDelete(e); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DnsEditDialog(
    entry: DnsEntry?,
    existing: List<DnsEntry>,
    onDismiss: () -> Unit,
    onConfirm: (DnsEntry) -> Unit,
) {
    var label by remember { mutableStateOf(entry?.label ?: "") }
    var host by remember { mutableStateOf(entry?.hostname ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Add provider" else "Edit provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Name (optional)") }, singleLine = true,
                )
                OutlinedTextField(
                    value = host, onValueChange = { host = it; error = null },
                    label = { Text("Provider hostname") }, singleLine = true,
                    placeholder = { Text("dns.example.com") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = host.trim().lowercase()
                val dup = existing.any { it.id != entry?.id && it.hostname.equals(h, ignoreCase = true) }
                error = PrivateDns.validate(h) ?: if (dup) "This hostname is already in the list" else null
                if (error == null) {
                    val name = label.trim().ifEmpty { h }
                    onConfirm(entry?.copy(label = name, hostname = h) ?: DnsEntry(label = name, hostname = h))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
