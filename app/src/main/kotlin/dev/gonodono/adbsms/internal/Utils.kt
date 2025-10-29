package dev.gonodono.adbsms.internal

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_SMS
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowInsets
import android.widget.CheckBox
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import dev.gonodono.adbsms.BuildConfig
import dev.gonodono.adbsms.R
import java.util.concurrent.Executors

internal const val Tag = "adbsms"

internal fun debugLog(message: String, throwable: Throwable? = null) {
    if (BuildConfig.DEBUG) Log.d(Tag, message, throwable)
}

internal fun View.applyInsetsListener() {
    if (Build.VERSION.SDK_INT < 35) return

    this.setOnApplyWindowInsetsListener { v, insets ->
        val bars = insets.getInsets(WindowInsets.Type.systemBars())
        (v.layoutParams as? MarginLayoutParams)?.apply {
            leftMargin = bars.left
            topMargin = bars.top
            rightMargin = bars.right
            bottomMargin = bars.bottom
        }
        insets
    }
}

internal fun Activity.checkShowIntro(
    savedInstanceState: Bundle?,
    onFinished: () -> Unit
) {
    if (savedInstanceState == null && !this.appPreferences().hideIntro) {
        AlertDialog.Builder(this)
            .setView(R.layout.dialog_intro)
            .setPositiveButton(R.string.label_close, null)
            .setOnDismissListener { onFinished() }
            .show()
            .findViewById<CheckBox>(R.id.hide_intro)
            ?.setOnCheckedChangeListener { _, isChecked ->
                this.appPreferences().hideIntro = isChecked
            }
    } else {
        onFinished()
    }
}

internal fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", this.packageName, null))
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        debugLog("Error opening Settings", e)
        Toast.makeText(this, R.string.error_settings, LENGTH_SHORT).show()
    }
}

@RequiresApi(33)
internal fun Context.hasPostNotificationsPermission(): Boolean =
    this.checkSelfPermission(POST_NOTIFICATIONS) == PERMISSION_GRANTED

internal fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < 33 || this.hasPostNotificationsPermission()

internal fun Context.hasReadSmsPermission(): Boolean =
    this.checkSelfPermission(READ_SMS) == PERMISSION_GRANTED

internal fun BroadcastReceiver.doAsync(
    onError: ((Throwable) -> Unit)? = null,
    block: () -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()
    val pendingResult = this.goAsync()
    executor.submit {
        try {
            block()
        } catch (e: Throwable) {
            onError?.invoke(e)
        } finally {
            executor.shutdown()
            pendingResult.finish()
        }
    }
}