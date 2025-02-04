# adbsms

A simple Android app that exposes an unprotected `ContentProvider` that relays
operations to the SMS Provider, allowing messages to be queried and modified
over adb without causing a `SecurityException` due to missing permissions.

<p align="center">
<img src="images/screenshots.png"
alt="Screenshots of the app in light and dark modes."
width="20%" />
</p>

<br />

> [!CAUTION]
> All SMS Provider operations are guarded by dangerous permissions. This app
> effectively bypasses the security and privacy measures provided by that
> mechanism, and could potentially be exploited by a malicious actor. It should
> be disabled or uninstalled when not in use. <b>Use at your own risk!</b>

<br />

## Overview

The app offers two levels of access to the SMS Provider:

+ Read-only, by acquiring the `READ_SMS` permission.

  This option is the most straightforward of the two. However, on Marshmallow
  and above, you will be able to view only _inbox_ and _sent_ messages.

+ Full access, by assuming the default SMS app role.

  This option will grant you full read and write access on each applicable
  version, but all messaging functionality will be broken while adbsms is the
  default. This means that any incoming messages will just be lost, though it
  would be possible to implement some sort of fallback logging or persistence in
  [the relevant component classes][stubs], should you really need it.

After enabling the desired option, queries can be made just as they usually are
over adb by replacing the authority in any `content://sms` URI with `adbsms`.

### Examples

You can consult adb's documentation for further details, but these few examples
should at least clarify the proper usage.

To list the number and text for all (viewable) messages:

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

If you've set adbsms as the default SMS app, you can also delete messages:

```
adb shell content delete --uri content://adbsms/137
```

Or update an existing one:

```
adb shell content update --uri content://adbsms/137 --bind body:s:"Updated\ text"
```

Or insert a new one:

```
adb shell content insert --uri content://adbsms --bind body:s:"Draft\ text" --bind type:i:3
```

The `type` column corresponds to the `MESSAGE_TYPE_*` values from the
[`Telephony.TextBasedSmsColumns`][columns] contract, which are summarized in the
following table.

### Message types

| Type   | Value |
|--------|:-----:|
| All    |   0   |
| Inbox  |   1   |
| Sent   |   2   |
| Draft  |   3   |
| Outbox |   4   |
| Failed |   5   |
| Queued |   6   |

<br />

## Notes

+ Don't get your hopes up if you're using the default SMS app option looking for
  those other message types. Though most apps use the _inbox_ and _sent_ types
  appropriately, it seems that many don't use _draft_ and/or the others at all.
  I guess they save that data to internal storage instead, for some reason. Just
  a heads up.

+ I haven't implemented every possible `ContentProvider` operation in the
  [`AdbSmsProvider`][provider] class, but it does cover the required overrides.
  I _think_ that should be sufficient for everything that adb can do, but if you
  find that I've missed something, please [file an issue][issue] for it.

[stubs]: app/src/main/kotlin/dev/gonodono/adbsms/DefaultSmsAppStubs.kt

[columns]: https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns

[provider]: app/src/main/kotlin/dev/gonodono/adbsms/AdbSmsProvider.kt

[issue]: https://github.com/gonodono/adbsms/issues/new