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

+ Read-only, by acquiring the `READ_SMS` permission

  This option is the most straightforward of the two. However, on Marshmallow
  and above, you will be able to view only `inbox` and `sent` messages.

+ Full access, by temporarily assuming the default SMS app role

  This one will grant you full read and write access on each applicable version,
  but your messaging will be largely nonfunctional while adbsms is the default.
  The only fallback facility currently provided is (optional) incoming SMS
  processing and storage to the Provider. Nothing else is handled, aside from
  some very simplistic event logs for a few things, and there is no way to send
  anything out.

After enabling the desired option, queries can be made as they usually are over
adb by replacing the authority in any `content://sms` URI with `adbsms`. (The
app's UI can be closed at this point; it's not involved in Provider operations.)

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

+ New versions will now be accompanied by [a GitHub release][releases] with
  assets that contain an `apk` of a release build variant signed with a debug
  key, which is the current setup in the module's `build.gradle.kts`. This app
  isn't published anywhere, as it's intended to be sort of a homebrew tool for
  developers and power users, and the unusual configuration is used to apply
  ProGuard and whatnot to an "unsigned" build.

  I'm not encouraging anyone to prefer the pre-built APKs; they're simply a
  convenience for users who don't have the setup available to do it themselves,
  or who just want a quick test. They're assembled using GitHub Actions and
  [this local workflow][workflow], so you can be reasonably certain that there
  are no malicious injections or modifications.

  The GitHub releases for automated builds will be created by user
  `github-actions`, whose name links to https://github.com/apps/github-actions,
  which actually redirects elsewhere if followed. Workflow execution details can
  be found on [the Actions tab][actions].

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

<br />

## License

MIT License

Copyright (c) 2025 Mike M.

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


[columns]: https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns

[releases]: https://github.com/gonodono/adbsms/releases

[workflow]: .github/workflows/build_and_release_apk.yaml

[actions]: https://github.com/gonodono/adbsms/actions

[provider]: app/src/main/kotlin/dev/gonodono/adbsms/AdbSmsProvider.kt

[issue]: https://github.com/gonodono/adbsms/issues/new