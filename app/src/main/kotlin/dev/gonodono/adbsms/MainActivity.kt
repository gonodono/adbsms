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
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.buildSpannedString
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import dev.gonodono.adbsms.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding

    private val readRequest =
        registerForActivityResult(RequestPermission()) { updateUi() }

    private val defaultRequest =
        registerForActivityResult(StartActivityForResult()) { updateUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        ui.buttonRequestRead.setOnClickListener { readRequest.launch(READ_SMS) }
        ui.buttonOpenSettings.setOnClickListener { openAppSettings() }
        ui.buttonSetDefault.setOnClickListener { setSelfAsDefault() }
        ui.buttonRevertDefault.setOnClickListener { revertDefault() }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi() {
        val hasRead = isReadSmsGranted()
        val isDefault = isDefaultSmsApp()

        ui.buttonRequestRead.isEnabled = !hasRead && !isDefault
        ui.buttonOpenSettings.isEnabled = hasRead && !isDefault
        ui.groupPermission.alpha = if (isDefault) 0.5F else 1F

        ui.textCurrentDefault.text = buildSpannedString {
            appendLine(getText(R.string.current_default))
            append(defaultSmsPackage().toString())
        }
        ui.buttonSetDefault.isEnabled = !isDefault
        ui.buttonRevertDefault.isEnabled = isDefault
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
            defaultRequest.launch(intent)
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
                startActivity(intent)
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
}

private fun Context.changeDefaultOldMethod(original: String) {
    val intent = Intent(ACTION_CHANGE_DEFAULT)
        .putExtra(EXTRA_PACKAGE_NAME, original)
    startActivity(intent)
}

private fun Context.isReadSmsGranted(): Boolean =
    checkSelfPermission(READ_SMS) == PERMISSION_GRANTED

private fun Context.isDefaultSmsApp(): Boolean =
    packageName == defaultSmsPackage()

private fun Context.defaultSmsPackage(): String? =
    Telephony.Sms.getDefaultSmsPackage(this)

private const val PREF_ORIGINAL_DEFAULT = "original_default"