package dev.gonodono.adbsms

import android.app.Service
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Telephony.Sms
import android.provider.Telephony.Threads
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dev.gonodono.adbsms.internal.TAG
import dev.gonodono.adbsms.internal.appPreferences
import dev.gonodono.adbsms.internal.doAsync
import dev.gonodono.adbsms.internal.postSmsAppNotification

internal fun Context.getDefaultSmsPackage(): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val manager = getSystemService(RoleManager::class.java)
        if (manager.isRoleHeld(RoleManager.ROLE_SMS)) return packageName
    }
    return Sms.getDefaultSmsPackage(this)
}

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Sms.Intents.SMS_DELIVER_ACTION) {
            logInvalidBroadcast(intent, "SmsReceiver")
            return
        }

        doAsync(onError = ::logReceivedSmsError) {
            processReceivedSms(context, intent)
        }
    }
}

private fun logReceivedSmsError(e: Throwable) {
    if (BuildConfig.DEBUG) Log.d(TAG, "Error processing received SMS", e)
}

private fun processReceivedSms(context: Context, intent: Intent) {
    val preferences = context.appPreferences()
    val log = preferences.logReceipts
    val store = preferences.storeReceivedSms
    if (!log && !store) return

    val messages = Sms.Intents.getMessagesFromIntent(intent)
    if (messages.isNullOrEmpty()) return

    val sender = messages.first().sender(context)
    notifyMessageEvent(context, context.getString(R.string.event_sms, sender))

    val values = messages.toContentValues(context)
    if (log) Log.w(TAG, values.toString())
    if (store) context.contentResolver.insert(Sms.CONTENT_URI, values)
}

private fun SmsMessage.sender(context: Context): String {
    val address = displayOriginatingAddress
    address ?: return context.getString(R.string.unknown)
    if (address.contains("@")) return address  // Naive email check
    val country = context.resources.configuration.locales[0].country
    val code = if (country.any { it.isDigit() }) "US" else country
    return PhoneNumberUtils.formatNumber(address, code) ?: address
}

private fun Array<SmsMessage>.toContentValues(context: Context): ContentValues =
    ContentValues().apply {
        val message = first()
        val address = message.displayOriginatingAddress
        put(Sms.ADDRESS, address)
        put(Sms.BODY, joinToString { it.displayMessageBody })
        put(Sms.DATE, System.currentTimeMillis())
        put(Sms.DATE_SENT, message.timestampMillis)
        put(Sms.PROTOCOL, message.protocolIdentifier)
        put(Sms.REPLY_PATH_PRESENT, message.isReplyPathPresent)
        put(Sms.SERVICE_CENTER, message.serviceCenterAddress)
        put(Sms.SUBJECT, message.pseudoSubject.takeIf { it.isNotBlank() })
        put(Sms.THREAD_ID, Threads.getOrCreateThreadId(context, address))
        put(Sms.TYPE, Sms.MESSAGE_TYPE_INBOX)
    }

class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Sms.Intents.WAP_PUSH_RECEIVED_ACTION) {
            logInvalidBroadcast(intent, "MmsReceiver")
            return
        }

        // This is all we're doing for MMS, at least for now, because just
        // parsing the address from a message takes a stupid amount of work.
        notifyMessageEvent(context, context.getString(R.string.event_mms))
    }
}

class ComposeSmsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_sms)
    }
}

class HeadlessSmsSendService : Service() {

    override fun onCreate() =
        notifyMessageEvent(this, getString(R.string.event_respond))

    override fun onBind(intent: Intent?): IBinder? = null
}

private fun logInvalidBroadcast(intent: Intent, receiver: String) {
    if (BuildConfig.DEBUG) Log.d(TAG, "Invalid broadcast to $receiver: $intent")
}

// No DEBUG check here for logs because they're info for the user, and opt-in.
private fun notifyMessageEvent(context: Context, event: String) {
    val preferences = context.appPreferences()
    val message = context.getString(R.string.message_event, event)
    if (preferences.logReceipts) Log.w(TAG, message)
    if (preferences.notifyReceipts) postSmsAppNotification(context, message)
}