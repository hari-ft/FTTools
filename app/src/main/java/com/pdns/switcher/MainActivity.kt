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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dnsStore = DnsStore(this)
        val timeoutStore = TimeoutStore(this)
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                App(dnsStore, timeoutStore, resumeTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

private enum class Screen { Home, PrivateDns, DnsProviders, ScreenTimeout }

@Composable
private fun App(dnsStore: DnsStore, timeoutStore: TimeoutStore, resumeTick: Int) {
    val ctx = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    var dnsEntries by remember { mutableStateOf(dnsStore.load()) }
    var dnsState by remember { mutableStateOf(PrivateDns.read(ctx)) }
    var customTimeouts by remember { mutableStateOf(timeoutStore.loadCustom()) }
    var timeoutMs by remember { mutableStateOf(ScreenTimeout.read(ctx)) }
    var hasSecure by remember { mutableStateOf(Grants.hasSecureSettings(ctx)) }
    var canWriteSystem by remember { mutableStateOf(Grants.canWriteSystemSettings(ctx)) }

    fun refresh() {
        dnsState = PrivateDns.read(ctx)
        timeoutMs = ScreenTimeout.read(ctx)
        hasSecure = Grants.hasSecureSettings(ctx)
        canWriteSystem = Grants.canWriteSystemSettings(ctx)
    }
    LaunchedEffect(resumeTick) { refresh() }

    fun toastIfDenied(ok: Boolean) {
        if (!ok) Toast.makeText(ctx, "Permission missing - see the card at the top", Toast.LENGTH_LONG).show()
    }

    fun setDns(host: String?) {
        toastIfDenied(if (host == null) PrivateDns.disable(ctx) else PrivateDns.enable(ctx, host))
        refresh()
    }

    fun updateDnsEntries(list: List<DnsEntry>) {
        dnsEntries = list
        dnsStore.save(list)
    }

    fun updateCustomTimeouts(list: List<Int>) {
        customTimeouts = list
        timeoutStore.saveCustom(list)
    }

    if (screen != Screen.Home) {
        BackHandler {
            screen = if (screen == Screen.DnsProviders) Screen.PrivateDns else Screen.Home
        }
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            hasSecure = hasSecure,
            dnsSummary = privateDnsSummary(dnsState),
            timeoutSummary = timeoutMs?.let(ScreenTimeout::label) ?: "Unknown",
            onOpen = { screen = it },
        )
        Screen.PrivateDns -> PrivateDnsScreen(
            entries = dnsEntries,
            state = dnsState,
            hasPermission = hasSecure,
            onToggle = { e, on -> setDns(if (on) e.hostname else null) },
            onOpenSettings = { screen = Screen.DnsProviders },
            onBack = { screen = Screen.Home },
        )
        Screen.DnsProviders -> DnsProvidersScreen(
            entries = dnsEntries,
            activeHost = dnsState.activeHost,
            onBack = { screen = Screen.PrivateDns },
            onSave = { old, new ->
                updateDnsEntries(
                    if (old == null) dnsEntries + new else dnsEntries.map { if (it.id == old.id) new else it }
                )
                if (old != null && old.hostname == dnsState.activeHost && old.hostname != new.hostname) {
                    setDns(new.hostname)
                }
            },
            onDelete = { e ->
                updateDnsEntries(dnsEntries.filterNot { it.id == e.id })
                if (e.hostname == dnsState.activeHost) setDns(null)
            },
        )
        Screen.ScreenTimeout -> ScreenTimeoutScreen(
            currentMs = timeoutMs,
            customMs = customTimeouts,
            canWrite = canWriteSystem,
            onSelect = { ms ->
                toastIfDenied(ScreenTimeout.write(ctx, ms))
                refresh()
            },
            onAddCustom = { ms -> if (ms !in customTimeouts) updateCustomTimeouts(customTimeouts + ms) },
            onDeleteCustom = { ms -> updateCustomTimeouts(customTimeouts - ms) },
            onBack = { screen = Screen.Home },
        )
    }
}

@Composable
private fun HomeScreen(
    hasSecure: Boolean,
    dnsSummary: String,
    timeoutSummary: String,
    onOpen: (Screen) -> Unit,
) {
    ToolScaffold(title = "FTTools") { pad ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(pad).fillMaxSize(),
        ) {
            if (!hasSecure) item { SecureGrantCard() }
            item { ToolCard("Private DNS", dnsSummary) { onOpen(Screen.PrivateDns) } }
            item { ToolCard("Screen timeout", timeoutSummary) { onOpen(Screen.ScreenTimeout) } }
        }
    }
}

@Composable
private fun ToolCard(title: String, summary: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = actions,
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
fun StatusCard(title: String, detail: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SecureGrantCard() {
    val ctx = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("One-time setup needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Android only lets apps change protected settings such as Private DNS after a permission " +
                    "is granted from a computer. Connect the phone with USB debugging on and run:"
            )
            Text(Grants.SECURE_GRANT_COMMAND, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("adb", Grants.SECURE_GRANT_COMMAND))
                Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
            }) { Text("Copy command") }
        }
    }
}
