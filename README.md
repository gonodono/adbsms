# adbsms

A simple Android app with a `ContentProvider` that relays requests from adb to
the SMS Provider, allowing messages to be queried and modified over the debug
bridge without causing `SecurityException`s due to missing permissions.

<br />

<p align="center">
<img src="images/screenshots.png"
alt="Screenshots of the app in light and dark modes."
width="30%" />
</p>

<br />

## Overview

The app offers two levels of access to the SMS Provider:

+ Read-only, by acquiring the `READ_SMS` permission.

  This option is the most straightforward of the two. However, on Marshmallow
  and above, you will be able to view only `inbox` and `sent` messages.

+ Full access, by temporarily assuming the default SMS app role.

  This one will grant you full read and write access on each applicable version,
  but your messaging will be largely nonfunctional while adbsms is the default.
  The only fallback facility currently provided is (optional) incoming SMS
  processing and storage to the Provider. Nothing else is handled, aside from
  some very simplistic event logs for a few things, and there is no way to send
  anything out.

After enabling the desired option, queries can be made as they usually are over
adb by replacing the authority in any `content://sms` URI with `adbsms`.

### Examples

You'll have to check adb's documentation for details on all of its available
options, but these few examples should at least clarify the URI modification
necessary to access this app's Provider.

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

+ All Provider operation requests are now guarded by a check against the calling 
  process to ensure that they're coming from the adb daemon. The Caution alert
  formerly found in this README's intro no longer applies.   

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