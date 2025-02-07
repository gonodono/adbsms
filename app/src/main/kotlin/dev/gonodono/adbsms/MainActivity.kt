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
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.buildSpannedString
import dev.gonodono.adbsms.databinding.ActivityMainBinding
import dev.gonodono.adbsms.internal.appPreferences
import dev.gonodono.adbsms.internal.applyInsetsListener
import dev.gonodono.adbsms.internal.canPostNotifications
import dev.gonodono.adbsms.internal.checkShowIntro
import dev.gonodono.adbsms.internal.hasPostNotificationsPermission
import dev.gonodono.adbsms.internal.hasReadSmsPermission
import dev.gonodono.adbsms.internal.openAppSettings
import dev.gonodono.adbsms.internal.setIcon
import dev.gonodono.adbsms.internal.updateStatusNotification

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

        ui.smsAppOptions.setOnClickListener(::showSmsAppOptions)

        checkShowIntro(savedInstanceState, ::checkPostNotificationsPermission)
    }

    private fun checkPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasPostNotificationsPermission()) return
        requestPermission(POST_NOTIFICATIONS)
    }

    // This is excessive, but some of the launched ops don't play nice.
    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi() {
        val hasRead = hasReadSmsPermission()
        val default = getDefaultSmsPackage()
        val isDefault = packageName == default

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
                setOnClickListener { revertDefaultSmsApp() }
                contentDescription = getText(R.string.desc_revert_full_access)
            } else {
                setOnClickListener { setSelfAsDefaultSmsApp() }
                contentDescription = getText(R.string.desc_enable_full_access)
            }
            isChecked = isDefault
        }
        ui.smsAppOptions.isEnabled = isDefault

        updateStatusNotification(this)
    }

    private fun showSmsAppOptions(anchor: View) = PopupMenu(this, anchor).run {
        inflate(R.menu.sms_app_options)
        setOnMenuItemClickListener(::onOptionsItemSelected)

        val preferences = appPreferences()
        val canPost = canPostNotifications()

        val log = menu.findItem(R.id.option_sms_app_log)
        log.isChecked = preferences.smsAppLog

        val notify = menu.findItem(R.id.option_sms_app_notify)
        notify.isChecked = canPost && preferences.smsAppNotify
        notify.isEnabled = canPost

        val store = menu.findItem(R.id.option_sms_app_store_sms)
        store.isChecked = preferences.smsAppStoreSms

        show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu ?: return false
        menuInflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu ?: return false

        val preferences = appPreferences()
        val canPost = canPostNotifications()

        val status = menu.findItem(R.id.option_status)
        status.isChecked = canPost && preferences.showStatus
        status.isEnabled = canPost

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val preferences = appPreferences()
        when (item.itemId) {
            // In case things get stuck due to bad timing with a launched op.
            R.id.option_refresh -> {
                Toast.makeText(this, R.string.refreshed, LENGTH_SHORT).show()
                updateUi()
            }
            R.id.option_status -> {
                preferences.showStatus = !preferences.showStatus
                updateStatusNotification(this)
            }
            R.id.option_sms_app_log -> {
                preferences.smsAppLog = !preferences.smsAppLog
            }
            R.id.option_sms_app_notify -> {
                preferences.smsAppNotify = !preferences.smsAppNotify
            }
            R.id.option_sms_app_store_sms -> {
                preferences.smsAppStoreSms = !preferences.smsAppStoreSms
            }
        }
        return true
    }

    private val launchForUpdate: (Intent) -> Unit =
        registerForActivityResult(StartActivityForResult()) { updateUi() }::launch

    private fun setSelfAsDefaultSmsApp() {
        appPreferences().originalDefaultSmsApp = getDefaultSmsPackage()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            val intent = manager.createRequestRoleIntent(ROLE_SMS)
            launchForUpdate(intent)
        } else {
            changeDefaultSmsAppOldMethod(packageName)
        }
    }

    private fun revertDefaultSmsApp() {
        val original = appPreferences().originalDefaultSmsApp
        when {
            original == null -> {
                Toast.makeText(this, R.string.error_revert, LENGTH_SHORT).show()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val intent = packageManager.getLaunchIntentForPackage(original)
                if (intent != null) launchForUpdate(intent)
            }
            else -> changeDefaultSmsAppOldMethod(original)
        }
    }

    private fun changeDefaultSmsAppOldMethod(packageName: String) {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        launchForUpdate(intent)
    }
}