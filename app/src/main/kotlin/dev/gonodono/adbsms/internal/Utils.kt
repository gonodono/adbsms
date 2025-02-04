package dev.gonodono.adbsms.internal

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_SMS
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import dev.gonodono.adbsms.BuildConfig
import dev.gonodono.adbsms.R

internal fun Activity.checkShowIntro(
    savedInstanceState: Bundle?,
    onFinished: () -> Unit
) {
    if (savedInstanceState == null && !appPreferences().hideIntro) {
        AlertDialog.Builder(this)
            .setOnDismissListener { onFinished() }
            .setView(R.layout.dialog_intro)
            .setPositiveButton("Close", null)
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

internal fun TextView.setIcon(@DrawableRes drawable: Int) =
    setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, 0, 0, 0)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun Context.hasPostNotificationsPermission(): Boolean =
    checkSelfPermission(POST_NOTIFICATIONS) == PERMISSION_GRANTED

internal fun Context.hasReadSmsPermission(): Boolean =
    checkSelfPermission(READ_SMS) == PERMISSION_GRANTED

internal fun Context.getDefaultSmsPackage(): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val manager = getSystemService(RoleManager::class.java)
        if (manager.isRoleHeld(RoleManager.ROLE_SMS)) return packageName
    }
    return Telephony.Sms.getDefaultSmsPackage(this)
}

internal fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", packageName, null))
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        if (BuildConfig.DEBUG) Log.d("adbsms", "Error opening Settings", e)
        Toast.makeText(this, R.string.error_settings, LENGTH_SHORT).show()
    }
}