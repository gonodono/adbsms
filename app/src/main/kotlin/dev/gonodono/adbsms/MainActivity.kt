package dev.gonodono.adbsms

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_SMS
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SMS
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.text.Spanned
import android.text.style.RelativeSizeSpan
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
import dev.gonodono.adbsms.internal.checkShowIntro
import dev.gonodono.adbsms.internal.hasPostNotificationsPermission
import dev.gonodono.adbsms.internal.hasReadSmsPermission
import dev.gonodono.adbsms.internal.openAppSettings
import dev.gonodono.adbsms.internal.setIcon
import dev.gonodono.adbsms.internal.updateNotification

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding

    private val requestPermission: (String) -> Unit =
        registerForActivityResult(RequestPermission()) { updateUi() }::launch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        ui = ActivityMainBinding.inflate(layoutInflater)
        ui.root.applyInsetsListener()
        setContentView(ui.root)

        supportActionBar?.setDisplayShowCustomEnabled(true)

        checkShowIntro(savedInstanceState, ::checkPostNotificationsPermission)
    }

    private fun checkPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPostNotificationsPermission()
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

        val preferences = appPreferences()

        val alerts = menu.findItem(R.id.option_alerts)
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPostNotificationsPermission()
        alerts.isChecked = canPost && preferences.showAlerts
        alerts.isEnabled = canPost

        menu.findItem(R.id.option_log).isChecked = preferences.logIncoming
        menu.findItem(R.id.option_save).isChecked = preferences.saveIncoming

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // In case things get stuck due to bad timing with a launched op.
            R.id.option_refresh -> {
                Toast.makeText(this, R.string.refreshed, LENGTH_SHORT).show()
                updateUi()
            }
            R.id.option_alerts -> {
                appPreferences().apply { showAlerts = !showAlerts }
                updateUi(alertOnly = true)
            }
            R.id.option_log -> {
                appPreferences().apply { logIncoming = !logIncoming }
            }
            R.id.option_save -> {
                appPreferences().apply { saveIncoming = !saveIncoming }
            }
        }
        return true
    }

    // This is excessive, but some of the launched ops don't play nice.
    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi(alertOnly: Boolean = false) {
        val hasRead = hasReadSmsPermission()
        val default = getDefaultSmsPackage()
        val isDefault = packageName == default

        val show = appPreferences().showAlerts
        updateNotification(this, show, hasRead, isDefault)
        if (alertOnly) return

        ui.status.apply {
            when {
                isDefault -> {
                    text = getText(R.string.status_full_access)
                    setIcon(R.drawable.ic_warn_full)
                }
                hasRead -> {
                    text = getText(R.string.status_read_enabled)
                    setIcon(R.drawable.ic_warn_read)
                }
                else -> {
                    text = getText(R.string.status_inactive)
                    setIcon(0)
                }
            }
        }

        ui.readInfo.isEnabled = !isDefault
        ui.readSwitch.apply {
            if (hasRead) {
                setOnClickListener { openAppSettings() }
                contentDescription = getText(R.string.desc_revert_read_only)
            } else {
                setOnClickListener { requestPermission(READ_SMS) }
                contentDescription = getText(R.string.desc_enable_read_only)
            }
            isEnabled = !isDefault
            isChecked = hasRead
        }

        ui.fullInfo.text = buildSpannedString {
            appendLine(getText(R.string.label_full_info))
            append(
                default.toString(),
                RelativeSizeSpan(0.9F),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        ui.fullSwitch.apply {
            if (isDefault) {
                setOnClickListener { revertDefault() }
                contentDescription = getText(R.string.desc_revert_full_access)
            } else {
                setOnClickListener { setSelfAsDefault() }
                contentDescription = getText(R.string.desc_enable_full_access)
            }
            isChecked = isDefault
        }
    }

    private val launchForUpdate: (Intent) -> Unit =
        registerForActivityResult(StartActivityForResult()) { updateUi() }::launch

    private fun setSelfAsDefault() {
        appPreferences().originalDefault = getDefaultSmsPackage()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            val intent = manager.createRequestRoleIntent(ROLE_SMS)
            launchForUpdate(intent)
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
                if (intent != null) launchForUpdate(intent)
            }
            else -> changeDefaultOldMethod(original)
        }
    }

    private fun changeDefaultOldMethod(packageName: String) {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        launchForUpdate(intent)
    }
}