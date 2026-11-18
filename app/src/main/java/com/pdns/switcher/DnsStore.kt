package com.pdns.switcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DnsEntry(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val hostname: String,
)

/** Persists the user's list of Private DNS providers in SharedPreferences as JSON. */
class DnsStore(context: Context) {
    private val prefs = context.getSharedPreferences("dns_store", Context.MODE_PRIVATE)

    fun load(): List<DnsEntry> {
        val raw = prefs.getString(KEY, null) ?: return DEFAULTS.also { save(it) }
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                DnsEntry(o.getString("id"), o.getString("label"), o.getString("hostname"))
            }
        }.getOrDefault(DEFAULTS)
    }

    fun save(list: List<DnsEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("label", it.label).put("hostname", it.hostname))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "entries"

        // Android Private DNS needs a hostname (DNS-over-TLS), not an IP.
        val DEFAULTS = listOf(
            DnsEntry("adguard", "AdGuard", "dns.adguard-dns.com"),
            DnsEntry("cloudflare", "Cloudflare (1.1.1.1 / 1.0.0.1)", "one.one.one.one"),
            DnsEntry("google", "Google (8.8.8.8 / 8.8.4.4)", "dns.google"),
        )
    }
}
