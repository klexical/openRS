package com.openrs.dash.ui

import android.content.Context
import androidx.core.content.edit

/**
 * Thin SharedPreferences wrapper for user-configurable app settings.
 * All reads/writes are synchronous and safe to call from any thread.
 */
object AppSettings {

    private const val PREFS   = "openrs_settings"
    private const val KEY_HOST = "wican_host"
    private const val KEY_PORT = "wican_port"

    const val DEFAULT_HOST = "192.168.80.1"
    const val DEFAULT_PORT = 3333

    fun getHost(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST

    fun getPort(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_PORT, DEFAULT_PORT)

    fun save(ctx: Context, host: String, port: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_HOST, host.trim())
            putInt(KEY_PORT, port)
        }
    }
}
