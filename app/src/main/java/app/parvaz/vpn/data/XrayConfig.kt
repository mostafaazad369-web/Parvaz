package app.parvaz.vpn.data

import org.json.JSONArray
import org.json.JSONObject

/** Builds an Xray-core JSON config (TUN inbound + one proxy outbound) from a parsed link. */
object XrayConfig {

    private val privateRanges = listOf(
        "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16",
        "127.0.0.0/8", "169.254.0.0/16", "224.0.0.0/4",
    )

    fun build(p: Parsed): String {
        val root = JSONObject()
        root.put("log", JSONObject().put("loglevel", "warning"))
        root.put("dns", JSONObject().put("servers", JSONArray(listOf("1.1.1.1", "8.8.8.8"))))
        root.put(
            "inbounds",
            JSONArray().put(
                JSONObject()
                    .put("tag", "tun")
                    .put("protocol", "tun")
                    .put("settings", JSONObject().put("name", "xray0").put("MTU", 1500).put("userLevel", 0))
                    .put(
                        "sniffing",
                        JSONObject().put("enabled", true)
                            .put("destOverride", JSONArray(listOf("http", "tls", "quic"))),
                    ),
            ),
        )
        root.put(
            "outbounds",
            JSONArray()
                .put(proxy(p))
                .put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
                .put(JSONObject().put("tag", "block").put("protocol", "blackhole")),
        )
        root.put(
            "routing",
            JSONObject().put("domainStrategy", "AsIs").put(
                "rules",
                JSONArray().put(
                    JSONObject().put("type", "field")
                        .put("ip", JSONArray(privateRanges))
                        .put("outboundTag", "direct"),
                ),
            ),
        )
        return root.toString()
    }

    private fun proxy(p: Parsed): JSONObject {
        val o = JSONObject()
            .put("tag", "proxy")
            .put("protocol", if (p.protocol == "ss") "shadowsocks" else p.protocol)
        val settings = when (p.protocol) {
            "vless" -> JSONObject().put(
                "vnext",
                JSONArray().put(
                    JSONObject().put("address", p.host).put("port", p.port).put(
                        "users",
                        JSONArray().put(
                            JSONObject().put("id", p.secret).put("encryption", "none").apply {
                                if (p.flow.isNotBlank()) put("flow", p.flow)
                            },
                        ),
                    ),
                ),
            )
            "vmess" -> JSONObject().put(
                "vnext",
                JSONArray().put(
                    JSONObject().put("address", p.host).put("port", p.port).put(
                        "users",
                        JSONArray().put(
                            JSONObject().put("id", p.secret).put("alterId", p.aid).put("security", p.method),
                        ),
                    ),
                ),
            )
            "trojan" -> JSONObject().put(
                "servers",
                JSONArray().put(JSONObject().put("address", p.host).put("port", p.port).put("password", p.secret)),
            )
            else -> JSONObject().put(
                "servers",
                JSONArray().put(
                    JSONObject().put("address", p.host).put("port", p.port)
                        .put("method", p.method).put("password", p.secret),
                ),
            )
        }
        o.put("settings", settings)
        o.put("streamSettings", stream(p))
        return o
    }

    private fun stream(p: Parsed): JSONObject {
        val st = JSONObject().put("network", p.net).put("security", p.security)
        when (p.security) {
            "tls" -> st.put(
                "tlsSettings",
                JSONObject()
                    .put("serverName", p.sni.ifBlank { p.hostHeader.ifBlank { p.host } })
                    .put("allowInsecure", p.allowInsecure)
                    .apply {
                        if (p.fp.isNotBlank()) put("fingerprint", p.fp)
                        if (p.alpn.isNotBlank()) put("alpn", JSONArray(p.alpn.split(',')))
                    },
            )
            "reality" -> st.put(
                "realitySettings",
                JSONObject()
                    .put("serverName", p.sni)
                    .put("fingerprint", p.fp.ifBlank { "chrome" })
                    .put("publicKey", p.pbk)
                    .put("shortId", p.sid)
                    .put("spiderX", p.spx),
            )
        }
        when (p.net) {
            "ws" -> st.put(
                "wsSettings",
                JSONObject().put("path", p.path.ifBlank { "/" })
                    .put("headers", JSONObject().put("Host", p.hostHeader.ifBlank { p.sni.ifBlank { p.host } })),
            )
            "grpc" -> st.put("grpcSettings", JSONObject().put("serviceName", p.serviceName))
            "httpupgrade" -> st.put(
                "httpupgradeSettings",
                JSONObject().put("path", p.path.ifBlank { "/" }).put("host", p.hostHeader.ifBlank { p.host }),
            )
        }
        return st
    }
}
