package dev.gonodono.adbsms.internal

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_SMS
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckBox
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import dev.gonodono.adbsms.BuildConfig
import dev.gonodono.adbsms.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal const val TAG = "adbsms"

internal fun Activity.checkShowIntro(
    savedInstanceState: Bundle?,
    onFinished: () -> Unit
) {
    if (savedInstanceState == null && !appPreferences().hideIntro) {
        AlertDialog.Builder(this)
            .setView(R.layout.dialog_intro)
            .setPositiveButton("Close", null)
            .setOnDismissListener { onFinished() }
            .show()
            .findViewById<CheckBox>(R.id.hide_welcome)
            ?.setOnCheckedChangeListener { _, isChecked ->
                appPreferences().hideIntro = isChecked
            }
    } else {
        onFinished()
    }
}

internal fun View.applyInsetsListener() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updateLayoutParams<MarginLayoutParams> {
            leftMargin = bars.left
            topMargin = bars.top
            rightMargin = bars.right
            bottomMargin = bars.bottom
        }
        insets
    }
}

internal fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", packageName, null))
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Error opening Settings", e)
        Toast.makeText(this, R.string.error_settings, LENGTH_SHORT).show()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun Context.hasPostNotificationsPermission(): Boolean =
    checkSelfPermission(POST_NOTIFICATIONS) == PERMISSION_GRANTED

internal fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPostNotificationsPermission()

internal fun Context.hasReadSmsPermission(): Boolean =
    checkSelfPermission(READ_SMS) == PERMISSION_GRANTED

internal fun BroadcastReceiver.doAsync(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
) {
    val pendingResult = goAsync()
    val scope = CoroutineScope(coroutineContext)
    scope.launch {
        try {
            try {
                coroutineScope(block)
            } catch (e: CancellationException) {
                // No rethrow. scope is cancelled anyway.
            } catch (e: Throwable) {
                onError(e)
            } finally {
                scope.cancel()
            }
        } finally {
            pendingResult.finish()
        }
    }
}