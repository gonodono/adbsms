package dev.gonodono.adbsms.internal

import android.content.Context
import android.content.SharedPreferences

internal fun Context.appSettings(): AppSettings =
    AppSettings(getSharedPreferences(packageName, Context.MODE_PRIVATE))

@JvmInline
internal value class AppSettings(private val sp: SharedPreferences) {

    var hideIntro: Boolean
        get() = sp.getBoolean(HideIntro, false)
        set(value) = sp.edit().putBoolean(HideIntro, value).apply()

    var showStatus: Boolean
        get() = sp.getBoolean(ShowStatus, true)
        set(value) = sp.edit().putBoolean(ShowStatus, value).apply()

    var logReceipts: Boolean
        get() = sp.getBoolean(LogReceipts, false)
        set(value) = sp.edit().putBoolean(LogReceipts, value).apply()

    var notifyReceipts: Boolean
        get() = sp.getBoolean(NotifyReceipts, true)
        set(value) = sp.edit().putBoolean(NotifyReceipts, value).apply()

    var storeReceivedSms: Boolean
        get() = sp.getBoolean(StoreReceivedSms, true)
        set(value) = sp.edit().putBoolean(StoreReceivedSms, value).apply()

    var originalDefault: String?
        get() = sp.getString(OriginalDefault, null)
        set(value) = sp.edit().putString(OriginalDefault, value).apply()

    fun nextSmsAppNotificationId(): Int {
        val next = sp.getInt(SmsAppNotificationId, 10)
        sp.edit().putInt(SmsAppNotificationId, next + 1).apply()
        return next
    }
}

private const val HideIntro = "hide_intro"
private const val ShowStatus = "show_status"
private const val LogReceipts = "log_receipts"
private const val NotifyReceipts = "notify_receipts"
private const val StoreReceivedSms = "store_received_sms"
private const val OriginalDefault = "original_default"
private const val SmsAppNotificationId = "sms_app_notification_id"