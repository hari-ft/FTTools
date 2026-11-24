package com.pdns.switcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

object Grants {
    const val SECURE_GRANT_COMMAND =
        "adb shell pm grant com.pdns.switcher android.permission.WRITE_SECURE_SETTINGS"

    fun hasSecureSettings(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun canWriteSystemSettings(ctx: Context) =
        Settings.System.canWrite(ctx) || hasSecureSettings(ctx)

    fun systemSettingsAccessIntent(ctx: Context) =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
}
