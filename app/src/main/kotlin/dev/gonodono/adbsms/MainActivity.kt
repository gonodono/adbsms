package dev.gonodono.adbsms

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_SMS
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SMS
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT
import android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME
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
import dev.gonodono.adbsms.internal.getDefaultSmsPackage
import dev.gonodono.adbsms.internal.hasPostNotificationsPermission
import dev.gonodono.adbsms.internal.hasReadSmsPermission
import dev.gonodono.adbsms.internal.openAppSettings
import dev.gonodono.adbsms.internal.setIcon
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
        val item = menu.findItem(R.id.option_notification)
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPostNotificationsPermission()
        item.isChecked = canPost && appPreferences().showNotification
        item.isEnabled = canPost
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // In case things get stuck due to bad timing with a launched op.
            R.id.option_refresh -> {
                Toast.makeText(this, R.string.refreshed, LENGTH_SHORT).show()
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
            setCheckedActual(hasRead)
            isEnabled = !isDefault
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
            setCheckedActual(isDefault)
        }

        updateNotification(
            this@MainActivity,
            appPreferences().showNotification,
            hasRead,
            isDefault
        )
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