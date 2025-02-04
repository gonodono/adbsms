package dev.gonodono.adbsms.internal

import android.content.Context
import android.content.SharedPreferences

internal fun Context.appPreferences(): AppPreferences =
    AppPreferences(getSharedPreferences(packageName, Context.MODE_PRIVATE))

@JvmInline
internal value class AppPreferences(private val sp: SharedPreferences) {

    var hideIntro: Boolean
        get() = sp.getBoolean(PREF_HIDE_INTRO, false)
        set(value) = sp.edit().putBoolean(PREF_HIDE_INTRO, value).apply()

    var showAlerts: Boolean
        get() = sp.getBoolean(PREF_SHOW_ALERTS, true)
        set(value) = sp.edit().putBoolean(PREF_SHOW_ALERTS, value).apply()

    var logIncoming: Boolean
        get() = sp.getBoolean(PREF_LOG_INCOMING, true)
        set(value) = sp.edit().putBoolean(PREF_LOG_INCOMING, value).apply()

    var saveIncoming: Boolean
        get() = sp.getBoolean(PREF_SAVE_INCOMING, true)
        set(value) = sp.edit().putBoolean(PREF_SAVE_INCOMING, value).apply()

    var originalDefault: String?
        get() = sp.getString(PREF_ORIGINAL_DEFAULT, null)
        set(value) = sp.edit().putString(PREF_ORIGINAL_DEFAULT, value).apply()
}

private const val PREF_HIDE_INTRO = "hide_intro"
private const val PREF_SHOW_ALERTS = "show_alerts"
private const val PREF_LOG_INCOMING = "log_incoming"
private const val PREF_SAVE_INCOMING = "save_incoming"
private const val PREF_ORIGINAL_DEFAULT = "original_default"