package com.pdns.switcher

import android.content.Context
import android.provider.Settings

object ScreenTimeout {
    val PRESETS_MS = listOf(15_000, 30_000, 60_000, 120_000, 300_000, 600_000, 1_800_000)
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
