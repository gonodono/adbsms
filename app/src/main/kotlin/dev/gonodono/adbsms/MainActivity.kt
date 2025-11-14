package dev.gonodono.adbsms

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_SMS
import android.app.Activity
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SMS
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony.Sms
import android.provider.Telephony.Sms.getDefaultSmsPackage
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import dev.gonodono.adbsms.databinding.ActivityMainBinding
import dev.gonodono.adbsms.internal.appSettings
import dev.gonodono.adbsms.internal.applyInsetsListener
import dev.gonodono.adbsms.internal.canPostNotifications
import dev.gonodono.adbsms.internal.checkShowIntro
import dev.gonodono.adbsms.internal.hasPostNotificationsPermission
import dev.gonodono.adbsms.internal.hasReadSmsPermission
import dev.gonodono.adbsms.internal.openSettingsAppPage
import dev.gonodono.adbsms.internal.refreshStatusNotification
import dev.gonodono.adbsms.internal.updateStatusNotification

class MainActivity : Activity() {

    private lateinit var ui: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ui = ActivityMainBinding.inflate(layoutInflater)
        ui.root.applyInsetsListener()
        setContentView(ui.root)

        ui.smsAppOptions.setOnClickListener(::showSmsAppOptions)

        checkShowIntro(savedInstanceState, ::checkPostNotificationsPermission)
    }

    private fun checkPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT < 33) return
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
        val default = getDefaultSmsPackage(this)
        val isDefault = packageName == default

        ui.readInfo.isEnabled = !isDefault
        ui.readSwitch.apply {
            if (hasRead) {
                setOnClickListener { openSettingsAppPage() }
                contentDescription = getText(R.string.desc_revert_read_only)
            } else {
                setOnClickListener { requestPermission(READ_SMS) }
                contentDescription = getText(R.string.desc_enable_read_only)
            }
            isEnabled = !isDefault
            isChecked = hasRead
        }

        ui.fullInfo.text = SpannableStringBuilder().apply {
            appendLine(getText(R.string.label_full_info))
            val shrinkSpan = RelativeSizeSpan(0.9F)
            append(default.toString(), shrinkSpan, SPAN_EXCLUSIVE_EXCLUSIVE)
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

    private fun showSmsAppOptions(anchor: View) =
        PopupMenu(this, anchor).run {
            inflate(R.menu.options_sms_app)
            setOnMenuItemClickListener(::onOptionsItemSelected)

            val settings = appSettings()
            val canPost = canPostNotifications()

            val log = menu.findItem(R.id.option_log_receipts)
            log.isChecked = settings.logReceipts

            val notify = menu.findItem(R.id.option_notify_receipts)
            notify.isChecked = canPost && settings.notifyReceipts
            notify.isEnabled = canPost

            val store = menu.findItem(R.id.option_store_received_sms)
            store.isChecked = settings.storeReceivedSms

            show()
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu ?: return false

        menuInflater.inflate(R.menu.options_main, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu ?: return false

        val canPost = canPostNotifications()
        val status = menu.findItem(R.id.option_show_status)
        status.isChecked = canPost && appSettings().showStatus
        status.isEnabled = canPost

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val settings = appSettings()

        when (item.itemId) {
            // In case things get stuck due to bad timing with a launched op.
            R.id.option_refresh_ui -> {
                updateUi()
                refreshStatusNotification(this)
                Toast.makeText(this, R.string.refreshed, LENGTH_SHORT).show()
            }
            R.id.option_show_status -> {
                settings.showStatus = !settings.showStatus
                updateStatusNotification(this)
            }
            R.id.option_log_receipts -> {
                settings.logReceipts = !settings.logReceipts
            }
            R.id.option_notify_receipts -> {
                settings.notifyReceipts = !settings.notifyReceipts
            }
            R.id.option_store_received_sms -> {
                settings.storeReceivedSms = !settings.storeReceivedSms
            }
        }

        return true
    }

    private fun setSelfAsDefaultSmsApp() {
        appSettings().originalDefault = getDefaultSmsPackage(this)

        if (Build.VERSION.SDK_INT >= 29) {
            val manager = getSystemService(RoleManager::class.java)
            val request = manager.createRequestRoleIntent(ROLE_SMS)
            launchForUpdate(request)
        } else {
            changeDefaultSmsAppOldMethod(packageName)
        }
    }

    private fun revertDefaultSmsApp() {
        val original = appSettings().originalDefault

        when {
            original == null -> showRevertError()

            Build.VERSION.SDK_INT >= 29 -> {
                val launch = packageManager.getLaunchIntentForPackage(original)
                launch?.let { launchForUpdate(it) } ?: showRevertError()
            }

            else -> changeDefaultSmsAppOldMethod(original)
        }
    }

    private fun changeDefaultSmsAppOldMethod(packageName: String) {
        val change = Intent(Sms.Intents.ACTION_CHANGE_DEFAULT)
        change.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        launchForUpdate(change)
    }

    private fun showRevertError() =
        Toast.makeText(this, R.string.error_revert, LENGTH_SHORT).show()

    private fun requestPermission(permission: String) =
        requestPermissions(arrayOf(permission), 0)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) =
        updateUi()

    private fun launchForUpdate(intent: Intent) =
        startActivityForResult(intent, 0)

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) =
        updateUi()
}