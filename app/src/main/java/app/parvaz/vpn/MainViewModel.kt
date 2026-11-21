package app.parvaz.vpn

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.parvaz.vpn.data.LinkParser
import app.parvaz.vpn.data.Node
import app.parvaz.vpn.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

data class Msg(val res: Int, val arg: Int? = null)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var nodes by mutableStateOf(store.links.mapNotNull { l -> LinkParser.parse(l)?.let { Node(l, it) } })
        private set
    var selectedId by mutableStateOf(store.selected)
        private set
    var themeMode by mutableIntStateOf(store.theme)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<Msg?>(null)

    /** ms; -1 = failed, -2 = testing */
    val pings = mutableStateMapOf<String, Int>()

    val selectedNode: Node?
        get() = nodes.firstOrNull { it.id == selectedId } ?: nodes.firstOrNull()

    fun select(id: String) {
        selectedId = id
        store.selected = id
    }

    fun delete(id: String) {
        nodes = nodes.filter { it.id != id }
        pings.remove(id)
        store.links = nodes.map { it.link }
        if (selectedId == id) selectedId = null
    }

    fun setTheme(mode: Int) {
        themeMode = mode
        store.theme = mode
    }

    fun add(input: String) {
        val t = input.trim()
        if (t.startsWith("http://", true) || t.startsWith("https://", true)) {
            addSubscription(t)
        } else {
            report(merge(LinkParser.extract(t)))
        }
    }

    private fun addSubscription(url: String) {
        viewModelScope.launch {
            busy = true
            val links = withContext(Dispatchers.IO) { fetch(url) }
            busy = false
            if (links == null) {
                message = Msg(R.string.sub_failed)
                return@launch
            }
            if (url !in store.subs) store.subs = store.subs + url
            report(merge(links))
        }
    }

    fun refreshSubscriptions() {
        viewModelScope.launch {
            val subs = store.subs
            if (subs.isEmpty()) return@launch
            busy = true
            var total = 0
            for (u in subs) {
                val l = withContext(Dispatchers.IO) { fetch(u) }
                if (l != null) total += merge(l)
            }
            busy = false
            report(total)
        }
    }

    fun pingAll() {
        viewModelScope.launch {
            val gate = Semaphore(8)
            val snapshot = nodes
            snapshot.forEach { pings[it.id] = -2 }
            snapshot.map { n ->
                async(Dispatchers.IO) {
                    gate.withPermit { pings[n.id] = tcpPing(n.p.host, n.p.port) }
                }
            }.awaitAll()
        }
    }

    private fun merge(links: List<String>): Int {
        val existing = nodes.map { it.link }.toSet()
        val fresh = links.distinct()
            .filter { it !in existing }
            .mapNotNull { l -> LinkParser.parse(l)?.let { Node(l, it) } }
        if (fresh.isNotEmpty()) {
            nodes = nodes + fresh
            store.links = nodes.map { it.link }
        }
        return fresh.size
    }

    private fun report(n: Int) {
        message = if (n > 0) Msg(R.string.added_n, n) else Msg(R.string.nothing_added)
    }

    private fun fetch(url: String): List<String>? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "Parvaz/1.0")
        val body = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        LinkParser.extract(body)
    } catch (e: Exception) {
        null
    }

    private fun tcpPing(host: String, port: Int): Int = try {
        Socket().use { s ->
            val t = System.nanoTime()
            s.connect(InetSocketAddress(host, port), 3000)
            ((System.nanoTime() - t) / 1_000_000).toInt()
        }
    } catch (e: Exception) {
        -1
    }
}
