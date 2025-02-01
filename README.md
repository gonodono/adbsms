# adbsms

A simple Android app that exposes an unprotected `ContentProvider` that relays
queries to the SMS Provider, allowing messages to be queried over adb without
causing a `SecurityException` due to the missing `READ_SMS` permission.

> [!CAUTION]
> All SMS permissions are classified dangerous. This app effectively bypasses
> the security and privacy measures provided by that mechanism, and could
> potentially be exploited by a malicious actor. It should be disabled or
> uninstalled when not in use. <br /> <b>Use at your own risk!</b>

The app offers two ways to enable read access to the SMS Provider:

+ Requesting the `READ_SMS` permission directly.

  This is the most straightforward option, but only _inbox_ and _sent_ messages
  are available to non-default apps on API level 23 and above.

+ Temporarily setting adbsms as the default SMS app.

  This option allows access to the other message types – e.g., _draft_ and
  _failed_ – but all messaging functionality will be disabled while adbsms is
  the default. This means that any incoming messages will just be lost, though
  it should be possible to implement some sort of fallback log or persistence,
  if necessary.

After enabling the desired option, queries can made by replacing the authority
in the `content://sms` URI for a regular query with `adbsms`. For example, to
list the number and text for all (viewable) messages:

```
adb shell content query --uri content://adbsms --projection address:body
```

Or just the _sent_ messages:

```
adb shell content query --uri content://adbsms/sent --projection address:body
```

Or, to list all columns for the message with ID 137:

```
adb shell content query --uri content://adbsms/137
```


## Notes

+ Don't get your hopes up if you're using the default SMS app option looking for
  those other message types. Though most apps use the _inbox_ and _sent_ types
  appropriately, many don't seem to use _draft_ and/or the others at all. I
  assume that they save those to internal storage instead, for whatever reason.
  Just a heads up.

+ It should be relatively easy to modify the [`AdbSmsProvider`][provider] class
  to handle the other `ContentProvider` operations, if needed. The `WRITE_SMS`
  permission would also need to be added to the manifest and the runtime request.


  [provider]: app/src/main/kotlin/dev/gonodono/adbsms/AdbSmsProvider.kt