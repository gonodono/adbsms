package dev.gonodono.adbsms.internal

import android.content.Context
import android.content.SharedPreferences

internal fun Context.appPreferences(): AppPreferences =
    AppPreferences(getSharedPreferences(packageName, Context.MODE_PRIVATE))

@JvmInline
internal value class AppPreferences(private val sp: SharedPreferences) {

    var hideIntro: Boolean
        get() = sp.getBoolean(HIDE_INTRO, false)
        set(value) = sp.edit().putBoolean(HIDE_INTRO, value).apply()

    var showStatus: Boolean
        get() = sp.getBoolean(SHOW_STATUS, true)
        set(value) = sp.edit().putBoolean(SHOW_STATUS, value).apply()

    var smsAppLog: Boolean
        get() = sp.getBoolean(SMS_APP_LOG, false)
        set(value) = sp.edit().putBoolean(SMS_APP_LOG, value).apply()

    var smsAppNotify: Boolean
        get() = sp.getBoolean(SMS_APP_NOTIFY, true)
        set(value) = sp.edit().putBoolean(SMS_APP_NOTIFY, value).apply()

    var smsAppStoreSms: Boolean
        get() = sp.getBoolean(SMS_APP_STORE_SMS, true)
        set(value) = sp.edit().putBoolean(SMS_APP_STORE_SMS, value).apply()

    var originalDefaultSmsApp: String?
        get() = sp.getString(SMS_APP_ORIGINAL, null)
        set(value) = sp.edit().putString(SMS_APP_ORIGINAL, value).apply()

    fun nextSmsAppNotificationId(): Int {
        val next = sp.getInt(SMS_APP_NOTIFICATION_ID, 10)  // Avoids status ID.
        sp.edit().putInt(SMS_APP_NOTIFICATION_ID, next + 1).apply()
        return next
    }
}

private const val HIDE_INTRO = "hide_intro"
private const val SHOW_STATUS = "show_status"
private const val SMS_APP_LOG = "sms_app_log"
private const val SMS_APP_NOTIFY = "sms_app_notify"
private const val SMS_APP_STORE_SMS = "sms_app_store_sms"
private const val SMS_APP_ORIGINAL = "sms_app_original"
private const val SMS_APP_NOTIFICATION_ID = "sms_app_notification_id"