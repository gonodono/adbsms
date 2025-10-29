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
            uri.toSmsUri(),
            projection,
            selection,
            selectionArgs,
            sortOrder
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
            uri.toSmsUri(),
            selection,
            selectionArgs
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
            uri.toSmsUri(),
            values,
            selection,
            selectionArgs
        )
    }
}

private fun checkCallingProcess() {
    if (Binder.getCallingUid() != SHELL_UID) throw SecurityException()
}

private val ContentProvider.contentResolver: ContentResolver
    get() = checkNotNull(context) { "Context not found" }.contentResolver

private fun Uri.toSmsUri(): Uri =
    Uri.Builder()
        .scheme(scheme)
        .authority("sms")
        .path(path)
        .query(query)
        .fragment(fragment)
        .build()