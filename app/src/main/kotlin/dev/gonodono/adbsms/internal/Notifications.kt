package dev.gonodono.adbsms.internal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_NO_CREATE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony.Sms.getDefaultSmsPackage
import dev.gonodono.adbsms.MainActivity
import dev.gonodono.adbsms.R

const val STATUS_CHANNEL_ID = "status_alerts"
const val STATUS_CHANNEL_NAME = "Status alerts"
const val STATUS_REQUEST_CODE_READ = 0
const val STATUS_REQUEST_CODE_FULL = 1
const val STATUS_NOTIFICATION_ID = 0

internal fun updateStatusNotification(context: Context) {
    val isDefault = context.packageName == getDefaultSmsPackage(context)
    val manager = context.getSystemService(NotificationManager::class.java)

    if (context.appPreferences().showStatus &&
        (context.hasReadSmsPermission() || isDefault)
    ) {
        postStatusNotification(context, manager, isDefault)
    } else {
        manager.cancel(STATUS_NOTIFICATION_ID)
        cancelStatusActivityIntents(context)
    }
}

internal fun refreshStatusNotification(context: Context) {
    cancelStatusActivityIntents(context)
    updateStatusNotification(context)
}

private fun postStatusNotification(
    context: Context,
    manager: NotificationManager,
    isDefault: Boolean
) {
    if (!context.canPostNotifications()) return

    val requestCode =
        if (isDefault) STATUS_REQUEST_CODE_FULL
        else STATUS_REQUEST_CODE_READ
    if (checkActivityIntent(context, requestCode) != null) return

    val otherCode =
        if (isDefault) STATUS_REQUEST_CODE_READ
        else STATUS_REQUEST_CODE_FULL
    checkActivityIntent(context, otherCode)?.cancel()

    manager.ensureChannel(STATUS_CHANNEL_ID, STATUS_CHANNEL_NAME)

    val textId =
        if (isDefault) R.string.status_full_access
        else R.string.status_read_enabled
    val notification = createNotificationBuilder(context, STATUS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getText(R.string.notification_title_status))
        .setContentText(context.getText(textId))
        .setContentIntent(createActivityIntent(context, requestCode))
        .setDeleteIntent(createStatusDeleteIntent(context))
        .setOngoing(true)
        .build()
    manager.notify(STATUS_NOTIFICATION_ID, notification)
}

private fun cancelStatusActivityIntents(context: Context) {
    checkActivityIntent(context, STATUS_REQUEST_CODE_READ)?.cancel()
    checkActivityIntent(context, STATUS_REQUEST_CODE_FULL)?.cancel()
}

private fun checkActivityIntent(
    context: Context,
    requestCode: Int
): PendingIntent? =
    getActivityIntent(context, requestCode, FLAG_NO_CREATE)

private fun createActivityIntent(
    context: Context,
    requestCode: Int
): PendingIntent? =
    getActivityIntent(context, requestCode, 0)

private fun getActivityIntent(
    context: Context,
    requestCode: Int,
    extraFlags: Int
): PendingIntent? {
    val intent = Intent(context, MainActivity::class.java)
    val flags = FLAG_IMMUTABLE or extraFlags
    return PendingIntent.getActivity(context, requestCode, intent, flags)
}

private fun createStatusDeleteIntent(context: Context): PendingIntent? {
    val intent = Intent(context, StatusDeleteReceiver::class.java)
    return PendingIntent.getBroadcast(context, 0, intent, FLAG_IMMUTABLE)
}

class StatusDeleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) =
        refreshStatusNotification(context)
}

const val SMS_APP_CHANNEL_ID = "sms_app_alerts"
const val SMS_APP_CHANNEL_NAME = "SMS app alerts"
const val SMS_APP_REQUEST_CODE = 10

internal fun postSmsAppNotification(context: Context, text: CharSequence) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.ensureChannel(SMS_APP_CHANNEL_ID, SMS_APP_CHANNEL_NAME)

    val notification = createNotificationBuilder(context, SMS_APP_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getText(R.string.notification_title_app))
        .setContentText(text)
        .setContentIntent(createActivityIntent(context, SMS_APP_REQUEST_CODE))
        .setAutoCancel(true)
        .build()
    val id = context.appPreferences().nextSmsAppNotificationId()
    manager.notify(id, notification)
}

private fun NotificationManager.ensureChannel(id: String, name: String) {
    if (Build.VERSION.SDK_INT < 26 || getNotificationChannel(id) != null) return
    createNotificationChannel(NotificationChannel(id, name, IMPORTANCE_HIGH))
}

private fun createNotificationBuilder(context: Context, channelId: String) =
    if (Build.VERSION.SDK_INT >= 26) {
        Notification.Builder(context, channelId)
    } else {
        @Suppress("DEPRECATION") Notification.Builder(context)
    }