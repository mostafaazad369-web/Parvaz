package app.parvaz.vpn.data

import android.content.Context
import org.json.JSONArray

data class Node(val link: String, val p: Parsed) {
    val id: String get() = link.hashCode().toString()
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("parvaz", Context.MODE_PRIVATE)

    private fun list(key: String): List<String> {
        val a = JSONArray(sp.getString(key, "[]"))
        return (0 until a.length()).map { a.getString(it) }
    }

    private fun put(key: String, v: List<String>) {
        sp.edit().putString(key, JSONArray(v).toString()).apply()
    }

    var links: List<String>
        get() = list("links")
        set(v) = put("links", v)

    var subs: List<String>
        get() = list("subs")
        set(v) = put("subs", v)

    var selected: String?
        get() = sp.getString("selected", null)
        set(v) = sp.edit().putString("selected", v).apply()

    /** 0 = system, 1 = light, 2 = dark */
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()
}
