package dev.gonodono.adbsms.internal

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.provider.Telephony.Sms
import android.provider.Telephony.Threads
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.util.Log
import android.util.Patterns
import dev.gonodono.adbsms.R

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

private fun logReceivedSmsError(e: Throwable) =
    Log.e(Tag, "Error processing received SMS", e)

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
    if (log) Log.w(Tag, values.toString())
    if (store) context.contentResolver.insert(Sms.CONTENT_URI, values)
}

private fun SmsMessage.sender(context: Context): String {
    val address = displayOriginatingAddress
    address ?: return context.getString(R.string.unknown)

    if (Patterns.EMAIL_ADDRESS.matcher(address).matches()) return address

    val country = context.resources.configuration.locales[0].country
    val code = if (country.any { it.isDigit() }) "US" else country
    return PhoneNumberUtils.formatNumber(address, code) ?: address
}

private fun Array<SmsMessage>.toContentValues(context: Context): ContentValues =
    ContentValues().also { cv ->
        val message = this.first()
        val address = message.displayOriginatingAddress
        cv.put(Sms.ADDRESS, address)
        cv.put(Sms.BODY, this.joinToString { it.displayMessageBody })
        cv.put(Sms.DATE, System.currentTimeMillis())
        cv.put(Sms.DATE_SENT, message.timestampMillis)
        cv.put(Sms.PROTOCOL, message.protocolIdentifier)
        cv.put(Sms.REPLY_PATH_PRESENT, message.isReplyPathPresent)
        cv.put(Sms.SERVICE_CENTER, message.serviceCenterAddress)
        cv.put(Sms.SUBJECT, message.pseudoSubject.takeIf { it.isNotBlank() })
        cv.put(Sms.THREAD_ID, Threads.getOrCreateThreadId(context, address))
        cv.put(Sms.TYPE, Sms.MESSAGE_TYPE_INBOX)
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

private fun logInvalidBroadcast(intent: Intent, receiver: String) =
    debugLog("Invalid broadcast to $receiver: $intent")

class ComposeSmsActivity : Activity() {

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

private fun notifyMessageEvent(context: Context, event: String) {
    val preferences = context.appPreferences()
    val message = context.getString(R.string.message_event, event)
    if (preferences.logReceipts) Log.w(Tag, message)
    if (preferences.notifyReceipts) postSmsAppNotification(context, message)
}