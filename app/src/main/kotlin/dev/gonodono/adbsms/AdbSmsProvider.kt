package dev.gonodono.adbsms

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Process.SHELL_UID
import dev.gonodono.adbsms.internal.hasReadSmsPermission

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
        checkCallingProcess()
        return contentResolver.query(
            /* uri = */ uri.toSmsUri(),
            /* projection = */ projection,
            /* selection = */ selection,
            /* selectionArgs = */ selectionArgs,
            /* sortOrder = */ sortOrder
        )
    }

    override fun getType(uri: Uri): String? {
        checkCallingProcess()
        return contentResolver.getType(uri.toSmsUri())
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        checkCallingProcess()
        return contentResolver.insert(uri.toSmsUri(), values)
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        checkCallingProcess()
        return contentResolver.delete(
            /* url = */ uri.toSmsUri(),
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
        checkCallingProcess()
        return contentResolver.update(
            /* uri = */ uri.toSmsUri(),
            /* values = */ values,
            /* where = */ selection,
            /* selectionArgs = */ selectionArgs
        )
    }
}

private fun checkCallingProcess() {
    if (Binder.getCallingUid() != SHELL_UID) throw SecurityException()
}

private val ContentProvider.contentResolver: ContentResolver
    get() = checkNotNull(this.context) { "Context not found" }.contentResolver

private fun Uri.toSmsUri(): Uri =
    Uri.Builder()
        .scheme(this.scheme)
        .authority("sms")
        .path(this.path)
        .query(this.query)
        .fragment(this.fragment)
        .build()