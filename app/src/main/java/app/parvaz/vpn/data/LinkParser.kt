package app.parvaz.vpn.data

import android.net.Uri
import android.util.Base64
import org.json.JSONObject

data class Parsed(
    val protocol: String,          // vless | vmess | trojan | ss
    val name: String,
    val host: String,
    val port: Int,
    val secret: String,            // uuid or password
    val method: String = "",       // ss cipher / vmess security
    val aid: Int = 0,
    val net: String = "tcp",       // tcp | ws | grpc | httpupgrade
    val security: String = "none", // none | tls | reality
    val sni: String = "",
    val hostHeader: String = "",
    val path: String = "",
    val fp: String = "",
    val alpn: String = "",
    val pbk: String = "",
    val sid: String = "",
    val spx: String = "",
    val flow: String = "",
    val serviceName: String = "",
    val allowInsecure: Boolean = false,
)

object LinkParser {

    fun parse(raw: String): Parsed? {
        val link = raw.trim()
        return try {
            when {
                link.startsWith("vless://", true) -> vlessOrTrojan(link, "vless")
                link.startsWith("trojan://", true) -> vlessOrTrojan(link, "trojan")
                link.startsWith("vmess://", true) -> vmess(link)
                link.startsWith("ss://", true) -> shadowsocks(link)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Extracts valid proxy links from plain text or a base64 subscription body. */
    fun extract(text: String): List<String> {
        fun lines(s: String) = s.split('\n', '\r', ' ', '\t').map { it.trim() }.filter { it.contains("://") }
        val direct = lines(text).filter { parse(it) != null }
        if (direct.isNotEmpty()) return direct
        return try {
            lines(b64(text)).filter { parse(it) != null }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun b64(s: String): String =
        String(Base64.decode(s.trim().replace('-', '+').replace('_', '/'), Base64.DEFAULT), Charsets.UTF_8)

    private class U(val user: String, val host: String, val port: Int, val q: Map<String, String>, val frag: String)

    private fun uri(link: String): U {
        val body = link.substringAfter("://")
        val frag = Uri.decode(body.substringAfter('#', ""))
        val noFrag = body.substringBefore('#')
        val query = noFrag.substringAfter('?', "")
        val main = noFrag.substringBefore('?')
        val user = if (main.contains('@')) Uri.decode(main.substringBeforeLast('@')) else ""
        val hp = main.substringAfterLast('@')
        val host = if (hp.startsWith("[")) hp.substring(1, hp.indexOf(']')) else hp.substringBeforeLast(':')
        val port = hp.substringAfterLast(':').trim('/').toInt()
        val q = query.split('&')
            .filter { it.contains('=') }
            .associate { it.substringBefore('=') to Uri.decode(it.substringAfter('=')) }
        return U(user, host, port, q, frag)
    }

    private fun vlessOrTrojan(link: String, proto: String): Parsed {
        val u = uri(link)
        val q = u.q
        return Parsed(
            protocol = proto,
            name = u.frag.ifBlank { u.host },
            host = u.host,
            port = u.port,
            secret = u.user,
            net = q["type"] ?: "tcp",
            security = q["security"] ?: if (proto == "trojan") "tls" else "none",
            sni = q["sni"] ?: q["peer"] ?: "",
            hostHeader = q["host"] ?: "",
            path = q["path"] ?: "",
            fp = q["fp"] ?: "",
            alpn = q["alpn"] ?: "",
            pbk = q["pbk"] ?: "",
            sid = q["sid"] ?: "",
            spx = q["spx"] ?: "",
            flow = q["flow"] ?: "",
            serviceName = q["serviceName"] ?: "",
            allowInsecure = q["allowInsecure"] == "1" || q["insecure"] == "1",
        )
    }

    private fun vmess(link: String): Parsed {
        val j = JSONObject(b64(link.substringAfter("://").substringBefore('#')))
        val net = j.optString("net", "tcp").ifBlank { "tcp" }
        return Parsed(
            protocol = "vmess",
            name = j.optString("ps").ifBlank { j.optString("add") },
            host = j.getString("add"),
            port = j.get("port").toString().toInt(),
            secret = j.getString("id"),
            method = j.optString("scy").ifBlank { "auto" },
            aid = j.optString("aid", "0").toIntOrNull() ?: 0,
            net = net,
            security = if (j.optString("tls") == "tls") "tls" else "none",
            sni = j.optString("sni"),
            hostHeader = j.optString("host"),
            path = j.optString("path"),
            fp = j.optString("fp"),
            alpn = j.optString("alpn"),
            serviceName = if (net == "grpc") j.optString("path") else "",
        )
    }

    private fun shadowsocks(link: String): Parsed {
        val body = link.substringAfter("://")
        val frag = Uri.decode(body.substringAfter('#', ""))
        val main = body.substringBefore('#').substringBefore('?')
        val cred: String
        val hp: String
        if (main.contains('@')) {
            val ui = main.substringBeforeLast('@')
            cred = if (ui.contains(':')) Uri.decode(ui) else b64(ui)
            hp = main.substringAfterLast('@')
        } else {
            val dec = b64(main)
            cred = dec.substringBeforeLast('@')
            hp = dec.substringAfterLast('@')
        }
        val host = if (hp.startsWith("[")) hp.substring(1, hp.indexOf(']')) else hp.substringBeforeLast(':')
        val port = hp.substringAfterLast(':').trim('/').toInt()
        return Parsed(
            protocol = "ss",
            name = frag.ifBlank { host },
            host = host,
            port = port,
            secret = cred.substringAfter(':'),
            method = cred.substringBefore(':'),
        )
    }
}
