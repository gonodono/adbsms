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

    var checkCaller: Boolean
        get() = sp.getBoolean(CHECK_CALLER, true)
        set(value) = sp.edit().putBoolean(CHECK_CALLER, value).apply()

    var logReceipts: Boolean
        get() = sp.getBoolean(LOG_RECEIPTS, false)
        set(value) = sp.edit().putBoolean(LOG_RECEIPTS, value).apply()

    var notifyReceipts: Boolean
        get() = sp.getBoolean(NOTIFY_RECEIPTS, true)
        set(value) = sp.edit().putBoolean(NOTIFY_RECEIPTS, value).apply()

    var storeReceivedSms: Boolean
        get() = sp.getBoolean(STORE_RECEIVED_SMS, true)
        set(value) = sp.edit().putBoolean(STORE_RECEIVED_SMS, value).apply()

    var originalDefault: String?
        get() = sp.getString(ORIGINAL_DEFAULT, null)
        set(value) = sp.edit().putString(ORIGINAL_DEFAULT, value).apply()

    fun nextSmsAppNotificationId(): Int {
        val next = sp.getInt(SMS_APP_NOTIFICATION_ID, 10)  // Avoids status ID.
        sp.edit().putInt(SMS_APP_NOTIFICATION_ID, next + 1).apply()
        return next
    }
}

private const val HIDE_INTRO = "hide_intro"
private const val SHOW_STATUS = "show_status"
private const val CHECK_CALLER = "check_caller"
private const val LOG_RECEIPTS = "log_receipts"
private const val NOTIFY_RECEIPTS = "notify_receipts"
private const val STORE_RECEIVED_SMS = "store_received_sms"
private const val ORIGINAL_DEFAULT = "original_default"
private const val SMS_APP_NOTIFICATION_ID = "sms_app_notification_id"