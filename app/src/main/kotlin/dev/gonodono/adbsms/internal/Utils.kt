package dev.gonodono.adbsms.internal

import android.Manifest.permission.READ_SMS
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import dev.gonodono.adbsms.BuildConfig
import dev.gonodono.adbsms.R

internal fun Context.hasReadSmsPermission(): Boolean =
    checkSelfPermission(READ_SMS) == PERMISSION_GRANTED

internal fun Context.getDefaultSmsPackage(): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val manager = getSystemService(RoleManager::class.java)
        if (manager.isRoleHeld(RoleManager.ROLE_SMS)) return packageName
    }
    return Telephony.Sms.getDefaultSmsPackage(this)
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
        if (BuildConfig.DEBUG) Log.d("adbsms", "Error opening Settings", e)
        Toast.makeText(this, R.string.error_settings, LENGTH_SHORT).show()
    }
}

internal fun RadioGroup.configureAsTabsFor(switcher: DualViewSwitcher) {
    check(childCount == 2) { "Invalid child count for tabs" }

    children.forEach { child ->
        check(child is RadioButton) { "Invalid child for tabs" }
        if (child.isChecked) child.setTabSelected(true)
    }

    setOnCheckedChangeListener { _, checkedId ->
        val checked = findViewById<RadioButton>(checkedId)
        checked.setTabSelected(true)

        val checkedIndex = indexOfChild(checked)
        val otherIndex = if (checkedIndex == 0) 1 else 0
        val other = getChildAt(otherIndex) as RadioButton
        other.setTabSelected(false)

        switcher.displayedChild = checkedIndex
    }
}

private fun RadioButton.setTabSelected(isSelected: Boolean) {
    paint.isUnderlineText = isSelected
    alpha = if (isSelected) 1F else 0.7F
}