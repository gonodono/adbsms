package dev.gonodono.adbsms

import android.Manifest.permission.READ_SMS
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.database.Cursor
import android.net.Uri

class AdbSmsProvider : ContentProvider() {

    override fun onCreate(): Boolean =
        context?.checkSelfPermission(READ_SMS) == PERMISSION_GRANTED

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? =
        requireContentResolver().query(
            uri.toSmsUri(),
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

    override fun getType(uri: Uri): String? =
        requireContentResolver().getType(uri.toSmsUri())

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        requireContentResolver().insert(uri.toSmsUri(), values)

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int =
        requireContentResolver().delete(
            uri.toSmsUri(),
            selection,
            selectionArgs
        )

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int =
        requireContentResolver().update(
            uri.toSmsUri(),
            values,
            selection,
            selectionArgs
        )

    private fun requireContentResolver(): ContentResolver =
        checkNotNull(context?.contentResolver) { "Cannot find ContentResolver" }
}

private fun Uri.toSmsUri(): Uri =
    Uri.Builder()
        .scheme(scheme)
        .authority("sms")
        .path(path)
        .query(query)
        .fragment(fragment)
        .build()