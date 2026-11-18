package com.pdns.switcher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    // Bumped on resume so the UI re-reads the system state (it may have changed in Settings).
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = DnsStore(this)
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                App(store, resumeTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
private fun App(store: DnsStore, resumeTick: Int) {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf(store.load()) }
    var state by remember { mutableStateOf(PrivateDns.read(ctx)) }
    var hasPermission by remember { mutableStateOf(PrivateDns.hasPermission(ctx)) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    fun refresh() {
        state = PrivateDns.read(ctx)
        hasPermission = PrivateDns.hasPermission(ctx)
    }
    LaunchedEffect(resumeTick) { refresh() }

    fun updateEntries(list: List<DnsEntry>) {
        entries = list
        store.save(list)
    }

    fun setActive(host: String?) {
        val ok = if (host == null) PrivateDns.disable(ctx) else PrivateDns.enable(ctx, host)
        if (!ok) Toast.makeText(ctx, "Permission missing - see the setup card", Toast.LENGTH_LONG).show()
        refresh()
    }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            entries = entries,
            activeHost = state.activeHost,
            onBack = { showSettings = false },
            onSave = { old, new ->
                updateEntries(
                    if (old == null) entries + new else entries.map { if (it.id == old.id) new else it }
                )
                // Editing the provider that is currently on: apply the new hostname to the system.
                if (old != null && old.hostname == state.activeHost && old.hostname != new.hostname) {
                    setActive(new.hostname)
                }
            },
            onDelete = { e ->
                updateEntries(entries.filterNot { it.id == e.id })
                if (e.hostname == state.activeHost) setActive(null)
            },
        )
    } else {
        HomeScreen(
            entries = entries,
            state = state,
            hasPermission = hasPermission,
            onToggle = { e, on -> setActive(if (on) e.hostname else null) },
            onOpenSettings = { showSettings = true },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    entries: List<DnsEntry>,
    state: PrivateDns.State,
    hasPermission: Boolean,
    onToggle: (DnsEntry, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private DNS") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad).fillMaxSize(),
        ) {
            if (!hasPermission) item { PermissionCard() }
            item { StatusCard(state) }
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
                Text("No providers yet. Add one in Settings.", Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(state: PrivateDns.State) {
    val (title, detail) = when (state.mode) {
        PrivateDns.MODE_OFF -> "Off" to "Private DNS is turned off"
        PrivateDns.MODE_HOSTNAME -> "On" to (state.hostname ?: "")
        else -> "Auto" to "Uses DNS-over-TLS when the network supports it"
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Current: $title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PermissionCard() {
    val ctx = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("One-time setup needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Android only lets apps change Private DNS after a permission is granted from a computer. " +
                    "Connect the phone with USB debugging on and run:"
            )
            Text(PrivateDns.GRANT_COMMAND, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("adb", PrivateDns.GRANT_COMMAND))
                Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
            }) { Text("Copy command") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    entries: List<DnsEntry>,
    activeHost: String?,
    onBack: () -> Unit,
    onSave: (old: DnsEntry?, new: DnsEntry) -> Unit,
    onDelete: (DnsEntry) -> Unit,
) {
    var dialogOpen by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<DnsEntry?>(null) } // null while adding
    var deleting by remember { mutableStateOf<DnsEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
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
        EditDialog(
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
private fun EditDialog(
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
