package dev.gonodono.adbsms

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import androidx.core.content.ContentProviderCompat
import dev.gonodono.adbsms.internal.appPreferences
import dev.gonodono.adbsms.internal.hasReadSmsPermission

class AdbSmsProvider : ContentProvider() {

    override fun onCreate(): Boolean = context?.hasReadSmsPermission() == true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        checkCallingProcess()
        return requireContentResolver().query(
            uri.toSmsUri(),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
    }

    override fun getType(uri: Uri): String? {
        checkCallingProcess()
        return requireContentResolver().getType(uri.toSmsUri())
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        checkCallingProcess()
        return requireContentResolver().insert(uri.toSmsUri(), values)
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        checkCallingProcess()
        return requireContentResolver().delete(
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
        return requireContentResolver().update(
            uri.toSmsUri(),
            values,
            selection,
            selectionArgs
        )
    }

    private fun checkCallingProcess() {
        val context = ContentProviderCompat.requireContext(this)
        if (!context.appPreferences().checkCaller) return
        if (Binder.getCallingUid() != 2000) throw SecurityException()
        if (callingPackage != "com.android.shell") throw SecurityException()
    }
}

private fun ContentProvider.requireContentResolver(): ContentResolver {
    val context = ContentProviderCompat.requireContext(this)
    return checkNotNull(context.contentResolver) { "Cannot find ContentResolver" }
}

private fun Uri.toSmsUri(): Uri =
    Uri.Builder()
        .scheme(scheme)
        .authority("sms")
        .path(path)
        .query(query)
        .fragment(fragment)
        .build()