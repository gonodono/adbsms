package dev.gonodono.adbsms

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log

// If you're not using the default SMS app option, this can all be removed.

class SmsReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) =
        warn("SmsReceiver has received a broadcast")
}

class MmsReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) =
        warn("MmsReceiver has received a broadcast")
}

class ComposeSmsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_sms)
        warn("ComposeSmsActivity has been launched")
    }
}

class HeadlessSmsSendService : Service() {

    override fun onCreate() = warn("HeadlessSmsSendService has been started")

    override fun onBind(intent: Intent?): IBinder? = null
}

private fun warn(message: String) {
    Log.w("adbsms", "$message while adbsms is the default SMS app")
}