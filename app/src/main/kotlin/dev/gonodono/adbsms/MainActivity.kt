package dev.gonodono.adbsms

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_SMS
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SMS
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT
import android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.buildSpannedString
import dev.gonodono.adbsms.databinding.ActivityMainBinding
import dev.gonodono.adbsms.internal.appPreferences
import dev.gonodono.adbsms.internal.applyInsetsListener
import dev.gonodono.adbsms.internal.configureAsTabsFor
import dev.gonodono.adbsms.internal.getDefaultSmsPackage
import dev.gonodono.adbsms.internal.hasReadSmsPermission
import dev.gonodono.adbsms.internal.openAppSettings
import dev.gonodono.adbsms.internal.updateNotification

// Most everything here isn't strictly necessary, apart from needing to launch
// MainActivity once to bring the app out of the stopped state. After that, you
// could grant the SMS permission manually through the device Settings, or
// change the default SMS app just like you would normally.

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding

    private val requestPermission: (String) -> Unit =
        registerForActivityResult(RequestPermission()) { updateUi() }::launch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val ui = ActivityMainBinding.inflate(layoutInflater).also { ui = it }
        ui.root.applyInsetsListener()
        setContentView(ui.root)

        supportActionBar?.setDisplayShowCustomEnabled(true)

        ui.basic.requestRead.setOnClickListener { requestPermission(READ_SMS) }
        ui.basic.openSettings.setOnClickListener { openAppSettings() }
        ui.full.setDefault.setOnClickListener { setSelfAsDefault() }
        ui.full.revertDefault.setOnClickListener { revertDefault() }
        ui.tabs.configureAsTabsFor(ui.switcher)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED
        ) {
            requestPermission(POST_NOTIFICATIONS)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu ?: return false
        menuInflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu ?: return false
        appPreferences().run {
            menu.findItem(R.id.option_banner).isChecked = showBanner
            menu.findItem(R.id.option_notification).isChecked = showNotification
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // In case things get stuck due to bad timing with a launched op.
            R.id.option_refresh -> {
                Toast.makeText(this, R.string.refreshed, LENGTH_SHORT).show()
            }
            R.id.option_banner -> {
                appPreferences().apply { showBanner = !showBanner }
            }
            R.id.option_notification -> {
                appPreferences().apply { showNotification = !showNotification }
            }
        }
        updateUi()
        return true
    }

    // This is excessive, but some of the launched ops don't play nice.
    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi() = ui.let { ui ->
        val hasRead = hasReadSmsPermission()
        val default = getDefaultSmsPackage()
        val isDefault = packageName == default

        ui.basic.basicInfo.isEnabled = !hasRead && !isDefault
        ui.basic.requestRead.isEnabled = !hasRead && !isDefault
        ui.basic.basicRevertInfo.isEnabled = hasRead && !isDefault
        ui.basic.openSettings.isEnabled = hasRead && !isDefault

        ui.full.fullInfo.isEnabled = !isDefault
        ui.full.setDefault.isEnabled = !isDefault
        ui.full.fullRevertInfo.isEnabled = isDefault
        ui.full.revertDefault.isEnabled = isDefault

        ui.full.currentDefault.text = buildSpannedString {
            appendLine(getText(R.string.label_current_default))
            append(default.toString())
        }

        if (isDefault) {
            ui.tabBasic.isEnabled = false
            ui.tabFull.isChecked = true
        } else {
            ui.tabBasic.isEnabled = true
        }

        val preferences = appPreferences()
        updateBanner(
            preferences.showBanner,
            hasRead,
            isDefault
        )
        updateNotification(
            this@MainActivity,
            preferences.showNotification,
            hasRead,
            isDefault
        )
    }

    private fun updateBanner(
        isEnabled: Boolean,
        hasRead: Boolean,
        isDefault: Boolean
    ) {
        val actionBar = supportActionBar ?: return
        when {
            !isEnabled || !isDefault && !hasRead -> actionBar.customView = null
            isDefault -> actionBar.setCustomView(R.layout.ab_full_access)
            else -> actionBar.setCustomView(R.layout.ab_read_enabled)
        }
    }

    private val launchForResult: (Intent) -> Unit =
        registerForActivityResult(StartActivityForResult()) { updateUi() }::launch

    private fun setSelfAsDefault() {
        appPreferences().originalDefault = getDefaultSmsPackage()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            val intent = manager.createRequestRoleIntent(ROLE_SMS)
            launchForResult(intent)
        } else {
            changeDefaultOldMethod(packageName)
        }
    }

    private fun revertDefault() {
        val original = appPreferences().originalDefault
        when {
            original == null -> {
                Toast.makeText(this, R.string.error_revert, LENGTH_SHORT).show()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val intent = packageManager.getLaunchIntentForPackage(original)
                if (intent != null) launchForResult(intent)
            }
            else -> changeDefaultOldMethod(original)
        }
    }

    private fun changeDefaultOldMethod(original: String) {
        val intent = Intent(ACTION_CHANGE_DEFAULT)
            .putExtra(EXTRA_PACKAGE_NAME, original)
        launchForResult(intent)
    }
}