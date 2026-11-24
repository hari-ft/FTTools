package com.pdns.switcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

data class TimeoutOption(val ms: Int, val label: String, val fromPhone: Boolean)

object ScreenTimeout {
    private const val DEFAULT_SETTINGS_PACKAGE = "com.android.settings"
    const val MIN_MS = 5_000
    const val MAX_MS = 86_400_000

    fun read(ctx: Context): Int? =
        runCatching { Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT) }
            .getOrNull()

    fun write(ctx: Context, ms: Int): Boolean = try {
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, ms)
    } catch (e: SecurityException) {
        false
    }

    fun phoneOptions(ctx: Context): List<TimeoutOption>? = runCatching {
        val pkg = settingsPackage(ctx)
        val res = ctx.packageManager.getResourcesForApplication(pkg)
        val valuesId = res.getIdentifier("screen_timeout_values", "array", pkg)
        if (valuesId == 0) return null
        val values = res.getStringArray(valuesId).map { it?.toIntOrNull() }
            .takeIf { list -> list.none { it == null } }
            ?.filterNotNull()
            ?: res.getIntArray(valuesId).toList()
        val entriesId = res.getIdentifier("screen_timeout_entries", "array", pkg)
        val entries = if (entriesId != 0) res.getTextArray(entriesId) else emptyArray()
        values.mapIndexed { i, ms ->
            TimeoutOption(ms, entries.getOrNull(i)?.toString() ?: label(ms), fromPhone = true)
        }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    private fun settingsPackage(ctx: Context): String =
        ctx.packageManager
            .resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?: DEFAULT_SETTINGS_PACKAGE

    fun label(ms: Int): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val parts = buildList {
            if (hours > 0) add(countOf(hours, "hour"))
            if (minutes > 0) add(countOf(minutes, "minute"))
            if (seconds > 0 || isEmpty()) add(countOf(seconds, "second"))
        }
        return parts.joinToString(" ")
    }

    private fun countOf(n: Int, unit: String) = if (n == 1) "1 $unit" else "$n ${unit}s"
}

class TimeoutStore(context: Context) {
    private val prefs = context.getSharedPreferences("timeout_store", Context.MODE_PRIVATE)

    fun loadCustom(): List<Int> =
        prefs.getString(KEY, "").orEmpty().split(',').mapNotNull { it.toIntOrNull() }

    fun saveCustom(list: List<Int>) {
        prefs.edit().putString(KEY, list.joinToString(",")).apply()
    }

    private companion object {
        const val KEY = "custom_ms"
    }
}
