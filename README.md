## adbsms

> [!CAUTION]
> The SMS permissions are classified dangerous and this app essentially bypasses
> the security and privacy offered by that mechanism.
> <b>Use at your own risk!</b>

The example app contains a simple [`ContentProvider` implementation][provider]
that relays queries to the SMS Provider, allowing message queries over adb
without adb's process holding the `READ_SMS` permission.

After installation, that permission needs to be granted to the app, either
through the request launched from `MainActivity`, or manually through the device
Settings.

Example usage:

```kotlin
adb shell content query --uri content://adbsms --projection address:body
```


  [provider]: app/src/main/kotlin/dev/gonodono/adbsms/AdbSmsProvider.kt