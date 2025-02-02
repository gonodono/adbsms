package dev.gonodono.adbsms

import android.Manifest.permission.READ_SMS
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SMS
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT
import android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.buildSpannedString
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import dev.gonodono.adbsms.databinding.ActivityMainBinding
import dev.gonodono.adbsms.internal.DualViewSwitcher

// Most everything here isn't strictly necessary, apart from needing to launch
// MainActivity once to bring the app out of the stopped state. After that, you
// could grant the SMS permission manually through the device Settings, or
// change the default SMS app just like you would normally.

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding

    private val readRequest =
        registerForActivityResult(RequestPermission()) { update() }

    private val resultLauncher =
        registerForActivityResult(StartActivityForResult()) { update() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        ui = ActivityMainBinding.inflate(layoutInflater)
        ui.root.applyInsetsListener()
        setContentView(ui.root)

        ui.buttonRequestRead.setOnClickListener { readRequest.launch(READ_SMS) }
        ui.buttonOpenSettings.setOnClickListener { openAppSettings() }
        ui.buttonSetDefault.setOnClickListener { setSelfAsDefault() }
        ui.buttonRevertDefault.setOnClickListener { revertDefault() }
        ui.textFullRevert.isVisible = Build.VERSION.SDK_INT >= 29
        ui.groupOptions.configureAsTabsFor(ui.switcher)

        update()
    }

    // In case things get stuck/stale, which seems to be possible if the
    // Activity resumes much sooner than a launched operation finishes.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add("Refresh UI")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        update()
        return true
    }

    private fun update() {
        val default = defaultSmsPackage()
        val isDefault = packageName == default

        val hasRead = checkSelfPermission(READ_SMS) == PERMISSION_GRANTED
        ui.buttonRequestRead.isEnabled = !hasRead && !isDefault
        ui.buttonOpenSettings.isEnabled = hasRead && !isDefault
        ui.textBasicRevert.isEnabled = hasRead && !isDefault

        ui.buttonSetDefault.isEnabled = !isDefault
        ui.buttonRevertDefault.isEnabled = isDefault
        ui.textFullRevert.isEnabled = isDefault

        ui.textCurrentDefault.text = buildSpannedString {
            appendLine(getText(R.string.current_default))
            append(default.toString())
        }

        if (isDefault) {
            ui.radioBasic.isEnabled = false
            ui.radioFull.isChecked = true
        } else {
            ui.radioBasic.isEnabled = true
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
        startActivity(intent)
    }

    private fun setSelfAsDefault() {
        originalDefaultSmsPackage = defaultSmsPackage()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            val intent = manager.createRequestRoleIntent(ROLE_SMS)
            resultLauncher.launch(intent)
        } else {
            changeDefaultOldMethod(packageName)
        }
    }

    private fun revertDefault() {
        val original = originalDefaultSmsPackage
        when {
            original == null -> {
                Toast.makeText(this, R.string.revert_error, LENGTH_SHORT).show()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val intent = packageManager.getLaunchIntentForPackage(original)
                resultLauncher.launch(intent ?: return)
            }
            else -> changeDefaultOldMethod(original)
        }
    }

    private var originalDefaultSmsPackage: String?
        get() = getPreferences(Context.MODE_PRIVATE)
            .getString(PREF_ORIGINAL_DEFAULT, null)
        set(value) {
            getPreferences(Context.MODE_PRIVATE).edit()
                .putString(PREF_ORIGINAL_DEFAULT, value).apply()
        }

    private fun changeDefaultOldMethod(original: String) {
        val intent = Intent(ACTION_CHANGE_DEFAULT)
            .putExtra(EXTRA_PACKAGE_NAME, original)
        resultLauncher.launch(intent)
    }
}

private fun View.applyInsetsListener() {
    setOnApplyWindowInsetsListener(this) { v, insets ->
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

private fun RadioGroup.configureAsTabsFor(switcher: DualViewSwitcher) {
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

@Suppress("NOTHING_TO_INLINE")
private inline fun Context.defaultSmsPackage(): String? =
    Telephony.Sms.getDefaultSmsPackage(this)

private const val PREF_ORIGINAL_DEFAULT = "original_default"