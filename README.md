# adbsms

> [!CAUTION]
> All SMS permissions are classified dangerous. This app effectively bypasses
> the security and privacy measures provided by that mechanism, and could
> potentially be exploited by a malicious actor. <b>Use at your own risk!</b>

The example app contains [a simple `ContentProvider` implementation][provider]
that relays queries to the SMS Provider, allowing us to query messages over adb
without causing a `SecurityException` due to a missing `READ_SMS` permission.

After installation, that permission needs to be granted to the app, either
through the request launched from `MainActivity`, or manually through the device
Settings.

To use, simply replace the authority in the `content://sms…` URI used for a
regular query with `adbsms`. For example, to list the number and text for all
(viewable) messages:

```kotlin
adb shell content query --uri content://adbsms --projection address:body
```

Do note that, since Marshmallow (Android 6, API level 23), non-default apps can
see only inbox and sent messages. The other types – draft, outbox, failed, etc.
– are accessible only by the default SMS app.


  [provider]: app/src/main/kotlin/dev/gonodono/adbsms/AdbSmsProvider.kt