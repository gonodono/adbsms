package dev.gonodono.adbsms.min;

import static android.Manifest.permission.READ_SMS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Process;

import java.io.FileNotFoundException;

// Skipping androidx.annotations and requireNonNull().
@SuppressWarnings({"DataFlowIssue", "NullableProblems"})
public class AdbSmsProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return Build.VERSION.SDK_INT < 23 || getContext().checkSelfPermission(READ_SMS) == PERMISSION_GRANTED;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        checkCallerIsShell();
        return getContext().getContentResolver().query(toSystemUri(uri), projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        checkCallerIsShell();
        return getContext().getContentResolver().getType(toSystemUri(uri));
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        checkCallerIsShell();
        return getContext().getContentResolver().insert(toSystemUri(uri), values);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        checkCallerIsShell();
        return getContext().getContentResolver().delete(toSystemUri(uri), selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        checkCallerIsShell();
        return getContext().getContentResolver().update(toSystemUri(uri), values, selection, selectionArgs);
    }

    @SuppressLint("UseRequiresApi")
    @TargetApi(29)  // <- Avoiding androidx.annotations.
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        checkCallerIsShell();
        return getContext().getContentResolver().openFile(toSystemUri(uri), mode, null);
    }

    private static void checkCallerIsShell() {
        if (Binder.getCallingUid() != Process.SHELL_UID) throw new SecurityException();
    }

    private static Uri toSystemUri(final Uri uri) {
        final String authority = uri.getAuthority();
        return new Uri.Builder()
                .scheme(uri.getScheme())
                .authority(authority.substring(3, authority.length() - 4))
                .path(uri.getPath())
                .query(uri.getQuery())
                .fragment(uri.getFragment())
                .build();
    }
}