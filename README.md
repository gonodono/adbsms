# adbsms

A simple Android app that exposes an unprotected `ContentProvider` that relays
operations to the SMS Provider, allowing messages to be queried and modified
over adb without causing a `SecurityException` due to missing permissions.

> [!CAUTION]
> All SMS Provider operations are guarded by dangerous permissions. This app
> effectively bypasses the security and privacy measures provided by that
> mechanism, and could potentially be exploited by a malicious actor. It should
> be disabled or uninstalled when not in use. <b>Use at your own risk!</b>

## Overview

The app offers two options for enabling access to the SMS Provider:

+ Requesting the `READ_SMS` permission directly.

  This read-only option is the most straightforward of the two. On Marshmallow
  and above, however, you'll be able to view only _inbox_ and _sent_ messages.

+ Temporarily setting adbsms as the default SMS app.

  This option will grant you full read and write access on all applicable
  versions, but all messaging functionality will be broken while adbsms is the
  default. This means that any incoming messages will just be lost, though it
  should be possible to implement some sort of fallback logging or persistence
  in [the relevant component classes][stubs], if you really need it.

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
adb shell content insert --uri content://adbsms --bind address:s:0123456789 --bind body:s:"Draft\ text" --bind type:i:3
```

The `type` column corresponds to the `MESSAGE_TYPE_*` values from the
[`Telephony.TextBasedSmsColumns`][columns] contract, which are summarized in the
following table.

## Message types

| Type   | Value |
|--------|:-----:|
| All    |   0   |
| Inbox  |   1   |
| Sent   |   2   |
| Draft  |   3   |
| Outbox |   4   |
| Failed |   5   |
| Queued |   6   |

## Notes

+ Don't get your hopes up if you're using the default SMS app option looking for
  those other message types. Though most apps use the _inbox_ and _sent_ types
  appropriately, it seems that many don't use _draft_ and/or the others at all.
  I guess they save that data to internal storage instead, for some reason. Just
  a heads up.

+ I haven't implemented all of the possible `ContentProvider` operations in the
  [`AdbSmsProvider`][provider] class, but it does cover the required overrides.
  I _think_ that should be sufficient for everything that adb can do, but if you
  find that I've missed something, please [file an issue][issue] for it.

[stubs]: app/src/main/kotlin/dev/gonodono/adbsms/DefaultSmsAppStubs.kt

[columns]: https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns

[provider]: app/src/main/kotlin/dev/gonodono/adbsms/AdbSmsProvider.kt

[issue]: https://github.com/gonodono/adbsms/issues/new