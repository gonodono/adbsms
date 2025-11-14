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

private const val StatusChannelId = "status_alerts"
private const val StatusChannelName = "Status alerts"
private const val StatusRequestCodeRead = 0
private const val StatusRequestCodeFull = 1
private const val StatusNotificationId = 0

internal fun updateStatusNotification(context: Context) {
    val isDefault = context.packageName == getDefaultSmsPackage(context)
    val manager = context.getSystemService(NotificationManager::class.java)

    if (context.appSettings().showStatus &&
        (context.hasReadSmsPermission() || isDefault)
    ) {
        postStatusNotification(context, manager, isDefault)
    } else {
        manager.cancel(StatusNotificationId)
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
        if (isDefault) StatusRequestCodeFull
        else StatusRequestCodeRead
    if (checkActivityIntent(context, requestCode) != null) return

    val otherCode =
        if (isDefault) StatusRequestCodeRead
        else StatusRequestCodeFull
    checkActivityIntent(context, otherCode)?.cancel()

    manager.ensureChannel(StatusChannelId, StatusChannelName)

    val textId =
        if (isDefault) R.string.status_full_access
        else R.string.status_read_enabled
    val notification = createNotificationBuilder(context, StatusChannelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getText(R.string.notification_title_status))
        .setContentText(context.getText(textId))
        .setContentIntent(createActivityIntent(context, requestCode))
        .setDeleteIntent(createStatusDeleteIntent(context))
        .setOngoing(true)
        .build()
    manager.notify(StatusNotificationId, notification)
}

private fun cancelStatusActivityIntents(context: Context) {
    checkActivityIntent(context, StatusRequestCodeRead)?.cancel()
    checkActivityIntent(context, StatusRequestCodeFull)?.cancel()
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

private const val SmsAppChannelId = "sms_app_alerts"
private const val SmsAppChannelName = "SMS app alerts"
private const val SmsAppRequestCode = 10

internal fun postSmsAppNotification(context: Context, text: CharSequence) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.ensureChannel(SmsAppChannelId, SmsAppChannelName)

    val notification = createNotificationBuilder(context, SmsAppChannelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getText(R.string.notification_title_app))
        .setContentText(text)
        .setContentIntent(createActivityIntent(context, SmsAppRequestCode))
        .setAutoCancel(true)
        .build()
    val id = context.appSettings().nextSmsAppNotificationId()
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