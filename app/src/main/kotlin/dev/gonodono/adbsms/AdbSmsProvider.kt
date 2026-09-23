package dev.gonodono.adbsms

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process.SHELL_UID
import dev.gonodono.adbsms.internal.appSettings
import dev.gonodono.adbsms.internal.hasReadSmsPermission
import dev.gonodono.adbsms.internal.updateStatusNotification
import kotlin.reflect.KMutableProperty0

class AdbSmsProvider : ContentProvider() {

    override fun onCreate(): Boolean =
        context?.hasReadSmsPermission() == true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        checkCallerIsShell()
        return contentResolver.query(
            /* uri = */ uri.toSystemUri(),
            /* projection = */ projection,
            /* selection = */ selection,
            /* selectionArgs = */ selectionArgs,
            /* sortOrder = */ sortOrder
        )
    }

    override fun getType(uri: Uri): String? {
        checkCallerIsShell()
        return contentResolver.getType(uri.toSystemUri())
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        checkCallerIsShell()
        return contentResolver.insert(uri.toSystemUri(), values)
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        checkCallerIsShell()
        return contentResolver.delete(
            /* url = */ uri.toSystemUri(),
            /* where = */ selection,
            /* selectionArgs = */ selectionArgs
        )
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        checkCallerIsShell()
        return contentResolver.update(
            /* uri = */ uri.toSystemUri(),
            /* values = */ values,
            /* where = */ selection,
            /* selectionArgs = */ selectionArgs
        )
    }

    @SuppressLint("UseRequiresApi")
    @TargetApi(29)  // <- Avoiding androidx.annotations.
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        checkCallerIsShell()
        return contentResolver.openFile(uri.toSystemUri(), mode, null)
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        checkCallerIsShell()
        return processCall(checkContext(), method, arg)
    }
}

private fun checkCallerIsShell() {
    if (Binder.getCallingUid() != SHELL_UID) throw SecurityException()
}

private val ContentProvider.contentResolver: ContentResolver
    get() = this.checkContext().contentResolver

private fun ContentProvider.checkContext(): Context =
    checkNotNull(this.context) { "Context not found" }

private fun Uri.toSystemUri(): Uri =
    Uri.Builder()
        .scheme(this.scheme)
        .authority(this.authority!!.substring(3))
        .path(this.path)
        .query(this.query)
        .fragment(this.fragment)
        .build()

private fun processCall(
    context: Context,
    method: String,
    arg: String?
): Bundle {
    val settings = context.appSettings()
    return when (method) {
        "notifyStatus" -> {
            processBooleanCall(method, arg, settings::notifyStatus)
        }
        "notifyReceipts" -> {
            processBooleanCall(method, arg, settings::notifyReceipts)
        }
        "logReceipts" -> {
            processBooleanCall(method, arg, settings::logReceipts)
        }
        "storeReceivedSms" -> {
            processBooleanCall(method, arg, settings::storeReceivedSms)
        }
        "originalDefault" -> {
            if (arg == null) {
                Bundle().apply { putString(method, settings.originalDefault) }
            } else {
                settings.originalDefault = arg
                Bundle.EMPTY
            }
        }
        "updateStatus" -> {
            updateStatusNotification(context)
            Bundle.EMPTY
        }
        else -> {
            error("Unknown method: $method")
        }
    }
}

private fun processBooleanCall(
    method: String,
    arg: String?,
    setting: KMutableProperty0<Boolean>
): Bundle =
    if (arg == null) {
        Bundle().apply { putBoolean(method, setting.get()) }
    } else {
        setting.set(arg.toBooleanStrict())
        Bundle.EMPTY
    }