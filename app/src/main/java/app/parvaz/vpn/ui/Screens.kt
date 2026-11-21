@file:OptIn(ExperimentalMaterial3Api::class)

package app.parvaz.vpn.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.parvaz.vpn.MainViewModel
import app.parvaz.vpn.R
import app.parvaz.vpn.vpn.ParvazVpnService
import app.parvaz.vpn.vpn.VpnState
import kotlinx.coroutines.delay

@Composable
fun ParvazApp(vm: MainViewModel, onToggle: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    val msg = vm.message
    LaunchedEffect(msg) {
        if (msg != null) {
            val text = if (msg.arg != null) ctx.getString(msg.res, msg.arg) else ctx.getString(msg.res)
            snack.showSnackbar(text)
            vm.message = null
        }
    }
    val err by ParvazVpnService.lastError.collectAsState()
    LaunchedEffect(err) {
        err?.let {
            snack.showSnackbar(ctx.getString(R.string.connect_failed, it))
            ParvazVpnService.lastError.value = null
        }
    }

    val tabs: List<Pair<Int, ImageVector>> = listOf(
        R.string.tab_home to Icons.Rounded.Home,
        R.string.tab_servers to Icons.Rounded.Dns,
        R.string.tab_settings to Icons.Rounded.Settings,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(vm, onToggle, onAdd = { tab = 1 })
                1 -> ServersScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
fun HomeScreen(vm: MainViewModel, onToggle: () -> Unit, onAdd: () -> Unit) {
    val state by ParvazVpnService.state.collectAsState()
    var seconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        seconds = 0
        if (state == VpnState.Connected) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    val cs = MaterialTheme.colorScheme
    val target = when (state) {
        VpnState.Connected -> cs.primary
        VpnState.Connecting -> cs.tertiary
        VpnState.Disconnected -> cs.outline
    }
    val color by animateColorAsState(target, label = "color")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (state == VpnState.Disconnected) 1f else 1.1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "scale",
    )

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = cs.primary,
        )
        Spacer(Modifier.height(36.dp))

        Box(
            Modifier.size(230.dp).scale(scale).background(color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(172.dp).clip(CircleShape).background(color).clickable { onToggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(68.dp),
                )
            }
        }
        Spacer(Modifier.height(28.dp))

        Text(
            stringResource(
                when (state) {
                    VpnState.Connected -> R.string.status_connected
                    VpnState.Connecting -> R.string.status_connecting
                    VpnState.Disconnected -> R.string.status_disconnected
                },
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (state == VpnState.Connected) {
                "%02d:%02d:%02d".format(seconds / 3600, (seconds / 60) % 60, seconds % 60)
            } else {
                stringResource(R.string.tap_to_connect)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        val node = vm.selectedNode
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
        ) {
            if (node != null) {
                Column(Modifier.padding(16.dp)) {
                    Text(node.p.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${node.p.protocol.uppercase()} · ${node.p.host}:${node.p.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.add_server_hint),
                        textAlign = TextAlign.Center,
                        color = cs.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.go_servers)) }
                }
            }
        }
    }
}

@Composable
fun ServersScreen(vm: MainViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    val clip = LocalClipboardManager.current
    val cs = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.tab_servers),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add))
            }
            OutlinedButton(onClick = { clip.getText()?.text?.let { vm.add(it) } }) {
                Icon(Icons.Rounded.ContentPaste, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.paste))
            }
            OutlinedButton(onClick = { vm.pingAll() }) {
                Icon(Icons.Rounded.Speed, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.test_all))
            }
            OutlinedButton(onClick = { vm.refreshSubscriptions() }) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.refresh_subs))
            }
        }
        if (vm.busy) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (vm.nodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_servers), color = cs.onSurfaceVariant)
            }
        } else {
            val selectedId = vm.selectedNode?.id
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.nodes, key = { it.id }) { n ->
                    val selected = n.id == selectedId
                    Card(
                        onClick = { vm.select(n.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) cs.primaryContainer else cs.surfaceVariant,
                        ),
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = { vm.select(n.id) })
                            Column(Modifier.weight(1f)) {
                                Text(n.p.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${n.p.protocol.uppercase()} · ${n.p.net}" +
                                        if (n.p.security != "none") " · ${n.p.security}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                            val ping = vm.pings[n.id]
                            when {
                                ping == null -> {}
                                ping == -2 -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                ping < 0 -> Text(stringResource(R.string.ping_fail), color = Color(0xFFDC2626), style = MaterialTheme.typography.labelMedium)
                                else -> Text(stringResource(R.string.ping_ms, ping), color = cs.primary, style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = { vm.delete(n.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.add_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.add_hint)) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.add(text); showAdd = false }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val cs = MaterialTheme.colorScheme
    val current = AppCompatDelegate.getApplicationLocales().let { if (it.isEmpty) null else it[0]?.language }

    fun setLang(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.settings_language), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = current == null, onClick = { setLang("") }, label = { Text(stringResource(R.string.lang_system)) })
            FilterChip(selected = current == "fa", onClick = { setLang("fa") }, label = { Text("فارسی") })
            FilterChip(selected = current == "en", onClick = { setLang("en") }, label = { Text("English") })
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.settings_theme), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = vm.themeMode == 0, onClick = { vm.setTheme(0) }, label = { Text(stringResource(R.string.theme_system)) })
            FilterChip(selected = vm.themeMode == 1, onClick = { vm.setTheme(1) }, label = { Text(stringResource(R.string.theme_light)) })
            FilterChip(selected = vm.themeMode == 2, onClick = { vm.setTheme(2) }, label = { Text(stringResource(R.string.theme_dark)) })
        }

        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.about), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.credits),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}
