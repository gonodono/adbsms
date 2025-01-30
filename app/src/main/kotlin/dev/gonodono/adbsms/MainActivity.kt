package dev.gonodono.adbsms

import android.Manifest.permission.READ_SMS
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val contract = ActivityResultContracts.RequestPermission()
        val request = registerForActivityResult(contract, ::setUpUi)

        super.onCreate(savedInstanceState)

        if (checkSelfPermission(READ_SMS) != PERMISSION_GRANTED) {
            request.launch(READ_SMS)
        } else {
            setUpUi(true)
        }
    }

    private fun setUpUi(granted: Boolean) {
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        @SuppressLint("SetTextI18n")
        findViewById<TextView>(R.id.text).text =
            "Permission " + if (granted) "granted" else "not granted"
    }
}