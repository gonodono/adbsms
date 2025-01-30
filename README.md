A very simple example of a `ContentProvider` that relays queries to the SMS
Provider, allowing queries over adb without adb's process holding the
`READ_SMS` permission.

After installation, that permission needs to be granted to the app, either
through the request launched from `MainActivity`, or manually through the device
Settings.

Example usage:

```kotlin
adb shell content query --uri content ://adbsms --projection address:body
```

> [!CAUTION]
> The SMS permissions are considered dangerous for a reason, and this app
> essentially bypasses the security and privacy offered by that mechanism.
> Use at your own risk!