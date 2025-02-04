package dev.gonodono.adbsms

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import dev.gonodono.adbsms.internal.TAG
import dev.gonodono.adbsms.internal.appPreferences

internal fun Context.getDefaultSmsPackage(): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val manager = getSystemService(RoleManager::class.java)
        if (manager.isRoleHeld(RoleManager.ROLE_SMS)) return packageName
    }
    return Telephony.Sms.getDefaultSmsPackage(this)
}

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        warn("SmsReceiver has received a broadcast")

        val preferences = context.appPreferences()

        val values = messages.toContentValues(context)

        if (preferences.logIncoming) Log.w(TAG, values.toString())

        if (preferences.saveIncoming) {
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        }
    }
}

private fun Array<SmsMessage>.toContentValues(context: Context): ContentValues {
    val values = ContentValues()
    val first = first()
    val address = first.displayOriginatingAddress
    values.put(Telephony.Sms.ADDRESS, address)
    values.put(Telephony.Sms.BODY, joinToString { it.displayMessageBody })
    values.put(Telephony.Sms.DATE, System.currentTimeMillis())
    values.put(Telephony.Sms.DATE_SENT, first.timestampMillis)
    values.put(Telephony.Sms.PROTOCOL, first.protocolIdentifier)
    values.put(Telephony.Sms.REPLY_PATH_PRESENT, first.isReplyPathPresent)
    values.put(Telephony.Sms.SERVICE_CENTER, first.serviceCenterAddress)
    if (!first.pseudoSubject.isNullOrBlank()) {
        values.put(Telephony.Sms.SUBJECT, first.pseudoSubject)
    }
    val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
    values.put(Telephony.Sms.THREAD_ID, threadId)
    values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
    return values
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
    Log.w(TAG, "$message while adbsms is the default SMS app")
}