package dev.gonodono.adbsms

import android.Manifest.permission.READ_SMS
import android.content.ContentProvider
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
    ): Cursor? {
        val resolver = checkNotNull(context?.contentResolver)
        val smsUri = with(uri) {
            Uri.Builder()
                .scheme(scheme)
                .authority("sms")
                .path(path)
                .query(query)
                .fragment(fragment)
                .build()
        }
        return resolver.query(
            smsUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
    }

    override fun getType(uri: Uri): String =
        throw UnsupportedOperationException()

    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException()

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException()
}