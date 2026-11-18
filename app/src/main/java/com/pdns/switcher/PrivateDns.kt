package com.pdns.switcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** Reads and writes the same system settings the "Private DNS" screen uses. */
object PrivateDns {
    private const val MODE = "private_dns_mode"
    private const val SPECIFIER = "private_dns_specifier"
    const val MODE_OFF = "off"
    const val MODE_AUTO = "opportunistic"
    const val MODE_HOSTNAME = "hostname"

    const val GRANT_COMMAND =
        "adb shell pm grant com.pdns.switcher android.permission.WRITE_SECURE_SETTINGS"

    data class State(val mode: String, val hostname: String?) {
        /** Hostname currently in use, or null if Private DNS is Off/Auto. */
        val activeHost: String? get() = if (mode == MODE_HOSTNAME) hostname else null
    }

    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun read(ctx: Context): State {
        val cr = ctx.contentResolver
        // An unset mode means the system default, which is Auto.
        val mode = Settings.Global.getString(cr, MODE) ?: MODE_AUTO
        return State(mode, Settings.Global.getString(cr, SPECIFIER))
    }

    /** Sets "Private DNS provider" with [hostname]. Returns false if not permitted. */
    fun enable(ctx: Context, hostname: String): Boolean = write {
        Settings.Global.putString(ctx.contentResolver, SPECIFIER, hostname)
        Settings.Global.putString(ctx.contentResolver, MODE, MODE_HOSTNAME)
    }

    /** Turns Private DNS fully off. Returns false if not permitted. */
    fun disable(ctx: Context): Boolean = write {
        Settings.Global.putString(ctx.contentResolver, MODE, MODE_OFF)
    }

    private inline fun write(block: () -> Unit): Boolean = try {
        block(); true
    } catch (e: SecurityException) {
        false
    }

    private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
    private val HOSTNAME =
        Regex("""^(?=.{1,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z][a-z0-9-]{0,61}[a-z0-9]$""")

    private val IP_HINTS = mapOf(
        "1.1.1.1" to "one.one.one.one", "1.0.0.1" to "one.one.one.one",
        "8.8.8.8" to "dns.google", "8.8.4.4" to "dns.google",
        "9.9.9.9" to "dns.quad9.net", "149.112.112.112" to "dns.quad9.net",
        "94.140.14.14" to "dns.adguard-dns.com", "94.140.15.15" to "dns.adguard-dns.com",
    )

    /** Returns an error message, or null if [input] is a usable Private DNS hostname. */
    fun validate(input: String): String? {
        val h = input.trim().lowercase()
        if (h.isEmpty()) return "Enter a hostname"
        if (IPV4.matches(h) || h.contains(':')) {
            val hint = IP_HINTS[h]?.let { " Use \"$it\" instead." } ?: ""
            return "Private DNS only accepts a hostname, not an IP address.$hint"
        }
        if (!HOSTNAME.matches(h)) return "Not a valid hostname"
        return null
    }
}
