# adbsms

A simple Android app that relays SMS content operation requests from adb to the
SMS Provider, allowing messages to be queried and modified over the debug bridge
without causing a `SecurityException` due to missing permissions.

<p align="center">
<img src="images/screenshots.png"
alt="Screenshots of the app in light and dark modes."
width="25%" />
</p>

<br />

## Overview

The app offers two levels of access to the SMS Provider:

+ Read-only, by acquiring the `READ_SMS` permission.

  This option is the most straightforward of the two. However, on Marshmallow
  and above, you will be able to view only `inbox` and `sent` messages.

+ Full access, by assuming the default SMS app role.

  This one will grant you full read and write access on each applicable version,
  but your messaging will be largely nonfunctional while adbsms is the default.
  The only fallback facility currently provided is (optional) incoming SMS
  processing and storage. Nothing else is handled, aside from some very
  simplistic event logs, and there is no way to send anything out.

After enabling the desired option, queries can be made as they usually are over
adb by replacing the authority in any `content://sms` URI with `adbsms`.

### Examples

You can check adb's documentation for further details, but these few examples
should at least clarify the proper usage.

To list the number and text for all (viewable) messages:

```
adb shell content query --uri content://adbsms --projection address:body
```

Or just the `sent` messages:

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

The `type` column corresponds to the `MESSAGE_TYPE_*` constants from the
[`Telephony.TextBasedSmsColumns`][columns] contract, the values for which are
given in the following table.

### Message types

| Type     | Value |
|----------|:-----:|
| `all`    |   0   |
| `inbox`  |   1   |
| `sent`   |   2   |
| `draft`  |   3   |
| `outbox` |   4   |
| `failed` |   5   |
| `queued` |   6   |

<br /> 

## Notes

+ By default, all Provider operation requests are guarded by a check against
  the calling process, ensuring that they're coming from the system's `shell`
  user. If your environment is somehow different than expected, and this check
  ends up preventing valid calls, it can be disabled through an item in the main
  options menu.

  However, this will effectively leave open an unsecured endpoint, which could
  potentially be exploited by malicious actors. If you're running this on an
  actual user device, that check should be disabled only if absolutely
  necessary, and then re-enabled as soon as possible.

+ If you plan to use the Full access option in order to get at the hidden
  message types, you should know that not all SMS apps utilize each one. Though
  most use `inbox` and `sent` consistently, it seems that many apps simply don't
  use `draft` and/or the others at all. I'm guessing that they save those
  messages to internal storage instead, for some reason. Just a heads up.

+ If you need to use the Full access option on Lollipop, API levels 21 and 22,
  you will need to un-comment the `WRITE_SMS` permission in the manifest (and
  probably suppress a warning or two, as well).

+ I haven't implemented every possible `ContentProvider` operation in
  [`AdbSmsProvider`][provider], but it does cover all of the required overrides.
  I _think_ that should be sufficient for everything that adb can do, but if you
  find something I've missed, please [file an issue][issue] for it.

[columns]: https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns

[provider]: app/src/main/kotlin/dev/gonodono/adbsms/AdbSmsProvider.kt

[issue]: https://github.com/gonodono/adbsms/issues/new