package dev.gonodono.adbsms.min;

import static android.Manifest.permission.READ_SMS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Process;

// Skipping androidx.annotations and requireNonNull().
@SuppressWarnings({"DataFlowIssue", "NullableProblems"})
public class AdbSmsProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return Build.VERSION.SDK_INT < 23 || getContext().checkSelfPermission(READ_SMS) == PERMISSION_GRANTED;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        checkCallingProcess();
        return getContext().getContentResolver().query(toSmsUri(uri), projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        checkCallingProcess();
        return getContext().getContentResolver().getType(toSmsUri(uri));
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        checkCallingProcess();
        return getContext().getContentResolver().insert(toSmsUri(uri), values);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        checkCallingProcess();
        return getContext().getContentResolver().delete(toSmsUri(uri), selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        checkCallingProcess();
        return getContext().getContentResolver().update(toSmsUri(uri), values, selection, selectionArgs);
    }

    private static void checkCallingProcess() {
        if (Binder.getCallingUid() != Process.SHELL_UID) throw new SecurityException();
    }

    private static Uri toSmsUri(Uri uri) {
        return new Uri.Builder()
                .scheme(uri.getScheme())
                .authority("sms")
                .path(uri.getPath())
                .query(uri.getQuery())
                .fragment(uri.getFragment())
                .build();
    }
}