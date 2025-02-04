package dev.gonodono.adbsms.internal

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
import androidx.core.app.NotificationCompat
import dev.gonodono.adbsms.MainActivity
import dev.gonodono.adbsms.R
import dev.gonodono.adbsms.getDefaultSmsPackage

private const val REQUEST_CODE_READ = 0
private const val REQUEST_CODE_FULL = 1
private const val NOTIFICATION_ID = 137
private const val CHANNEL_ID = "alerts"
private const val CHANNEL_NAME = "Alerts"

internal fun updateNotification(
    context: Context,
    isEnabled: Boolean,
    hasRead: Boolean,
    isDefault: Boolean
) {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (isEnabled && (hasRead || isDefault)) {
        postNotification(context, manager, isDefault)
    } else {
        checkActivityIntent(context, REQUEST_CODE_READ)?.cancel()
        checkActivityIntent(context, REQUEST_CODE_FULL)?.cancel()
        manager.cancel(NOTIFICATION_ID)
    }
}

private fun postNotification(
    context: Context,
    manager: NotificationManager,
    isDefault: Boolean
) {
    val requestCode = if (isDefault) REQUEST_CODE_FULL else REQUEST_CODE_READ
    if (checkActivityIntent(context, requestCode) != null) return

    val otherCode = if (isDefault) REQUEST_CODE_READ else REQUEST_CODE_FULL
    checkActivityIntent(context, otherCode)?.cancel()

    ensureNotificationChannel(manager)

    val textId =
        if (isDefault) R.string.status_full_access
        else R.string.status_read_enabled
    val iconId =
        if (isDefault) R.drawable.ic_warn_full
        else R.drawable.ic_warn_read
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentIntent(createActivityIntent(context, requestCode))
        .setDeleteIntent(createDeleteIntent(context))
        .setContentText(context.getText(textId))
        .setContentTitle("Alert!")
        .setSmallIcon(iconId)
        .setOngoing(true)
        .build()
    manager.notify(NOTIFICATION_ID, notification)
}

private fun checkActivityIntent(
    context: Context,
    requestCode: Int
): PendingIntent? = getPendingIntent(context, requestCode, FLAG_NO_CREATE)

private fun createActivityIntent(
    context: Context,
    requestCode: Int
): PendingIntent? = getPendingIntent(context, requestCode, 0)

private fun getPendingIntent(
    context: Context,
    requestCode: Int,
    extraFlags: Int
): PendingIntent? =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java),
        FLAG_IMMUTABLE or extraFlags
    )

private fun createDeleteIntent(context: Context): PendingIntent? =
    PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NotificationDeleteReceiver::class.java),
        FLAG_IMMUTABLE
    )

class NotificationDeleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        checkActivityIntent(context, REQUEST_CODE_READ)?.cancel()
        checkActivityIntent(context, REQUEST_CODE_FULL)?.cancel()
        updateNotification(
            context,
            context.appPreferences().showAlerts,
            context.hasReadSmsPermission(),
            context.packageName == context.getDefaultSmsPackage()
        )
    }
}

private fun ensureNotificationChannel(manager: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_HIGH)
    manager.createNotificationChannel(channel)
}