# adbsms

<!--suppress HtmlDeprecatedAttribute -->

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" 
  alt="The application icon for the main app"
  align="right" />

A small and simple Android app with a `ContentProvider` that can act as a relay
between `adb` and the system message Providers, allowing for access workarounds
in certain problematic environments. For example:

- The shell may lack some or all of the necessary permissions, leading to
  possible `SecurityException`s and other failure modes.

- On Marshmallow and above, non-default apps can see only `inbox` and `sent`
  messages, and the shell may be constrained to the same restricted view.

Though it's named adb***sms***, the app works with the MMS and MMS-SMS Providers
as well, the former potentially acting as the store for RCS too, depending on
how your messaging client is designed to handle them.

<br />

## Contents

- [**Overview**](#overview)
- [**Providers**](#providers)
- [**Examples**](#examples)
- [**Headless**](#headless)
- [**Minimal**](#minimal)
- [**Download**](#download)
- [**Notes**](#notes)
- [**Wiki**][wiki]
- [**Issues**][issue]

<br />

## Overview

<br />

<!--suppress HtmlDeprecatedAttribute -->
<p align="center">
  <!--suppress CheckImageSize -->
  <img src="images/screenshots.png"
    alt="Screenshots of the main app's UI in light and dark modes"
    width="35%" />
</p>
<div align="center">
  <sup><i>The main app's UI, in light mode and dark.</i></sup>
</div>

<br />

This project actually comprises two apps: adbsms and adbsms.min, also called the
main app and the minimal one. They both offer the same core functionalities –
read/write access to the SMS, MMS, and MMS-SMS Providers – but adbsms.min has no
UI whatsoever; it's meant to be used entirely from the shell.

These apps work by serving up their own Providers with distinct authorities –
the part of the URI immediately following `content://` – and passing requests
directly to the appropriate system Providers after replacing the authorities
with the proper values.

### Access options

Both apps offer two levels of access to the message Providers:

- **Read-only**, by acquiring the `READ_SMS` permission

  This option is the most straightforward of the two. However, on Marshmallow
  and above, you will be able to view only `inbox` and `sent` messages. (To
  clarify, `READ_SMS` does grant access to all three Providers.)

- **Full access**, by temporarily assuming the default SMS app role

  This one will get you full read and write access on each supported version,
  but your messaging will be largely nonfunctional for the duration. The only
  fallback facility currently offered is incoming SMS handling.

After enabling the desired option, queries can be made as they normally are over
`adb`, replacing the authorities in your `content://` URIs with adbsms's unique
variants. See the [Examples](#examples) below.

If you'd rather toggle these access options from the shell, consult the
[Headless](#headless) section.

Should you not need the UI at all, you might prefer the [Minimal](#minimal)
version.

> [!NOTE]
> The app's UI can be closed while running queries. It's not involved in
> Provider operations.

### Main app features

These are exclusive to the main app. They're covered here in order to illustrate
the differences between the two.

- A persistent status notification is available in both access modes. It's meant
  mainly so that you don't forget if you've set the main app as the default on a
  personal device.

While the main app is the default SMS app, these options are also offered:

- Incoming SMS processing and storage to the system Provider. MMS and RCS are
  not handled currently due to complexity and lack of a public API,
  respectively.

- Receipt logs and notifications for SMS and MMS, though the latter are bare
  event notices with no actual message data. Nothing for RCS again because of
  the missing API.

These features are all toggled through user settings that can also be modified
from the shell. The status notification can be updated with a specialized shell
content command as well, allowing for completely headless operation. Refer to
the [Main app features](#main-app-features-1) under that section for details.

If you don't need any of these bells or whistles – e.g., if you intend only for
quick and temporary command line usage, as in a script – the [Minimal](#minimal)
version might be preferable.

<br />

## Providers

Both apps support all three standard message Providers: SMS, MMS, and the
MMS-SMS Provider that deals in conversations/threads, which may contain either
or both formats.

There is no public RCS API yet, and therefore no matching Provider for them
either. However, Google's Messages app – and perhaps other messaging clients as
well – apparently stores RCS in the MMS Provider, though that is its own
particular behavior, as far as I know, not the system's.

### Authorities

This table lists all the system Providers and authorities currently handled,
along with each app's unique authority.

<div align="center">

| Provider | Authority |   Main app   |   Minimal app    |
|:--------:|:---------:|:------------:|:----------------:|
|   SMS    |   `sms`   |   `adbsms`   |   `adbsms.min`   |
|   MMS    |   `mms`   |   `adbmms`   |   `adbmms.min`   |
| MMS-SMS  | `mms-sms` | `adbmms-sms` | `adbmms-sms.min` |

<sup>(RCS, if present, will be in the MMS Provider.)</sup>

</div>

The main app simply prepends `adb` to the system authority. The minimal one both
prepends `adb` and appends `.min`; i.e., it "sandwiches" the system authority
between those affixes.

That last row is admittedly a bit clumsy due to bad planning on my part, mainly
'cause I forgot about that darn hyphen. This project was initially a small,
single-purpose tool, and I chose my naming scheme poorly at an early stage and
didn't realize it until too late.

That said, I might change these at some point, but for now, I'm opting for
consistency, and not breaking things without good reason. If anyone finds these
too unwieldy, please file an [issue][issue] to that effect.

<br />

## Examples

You'll have to check `adb`'s documentation for details on all its possible
options, but these few examples should at least clarify the URI modification
necessary to access the relay Providers.

These examples all target the SMS Provider, but the URI changes are essentially
the same for the other [Authorities](#authorities), though each schema will be
quite different.

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
[`Telephony.TextBasedSmsColumns`][contract] contract, the values for which have
been [collated][wiki-sms-type] in the wiki for convenience.

The [wiki][wiki] has further information about available columns and URIs for
the various Providers, along with links to official source code and
documentation. There are also a few more examples that demonstrate a couple of
queries beyond the basic stuff here.

<br />

## Headless

The app's UI can be bypassed entirely, if desired. As with any app, both the
permission and the default app status can be set through the relevant Settings
pages manually or programmatically.

Alternatively, it can all be handled through the shell, as the rest of this
section demonstrates.

> [!CAUTION]
> The main app's status notification is _not_ directly updated by headless
> operations. That must be done manually with an additional command. See
> [Status notification updates](#status-notification-updates) below.

### Read-only setup

This option requires only a permission grant, and the command is the same on all
supported Android versions.

```
adb shell pm grant dev.gonodono.adbsms android.permission.READ_SMS
```

Should you need to revoke it later, change the `pm` command accordingly.

```
adb shell pm revoke dev.gonodono.adbsms android.permission.READ_SMS
```

### Full access setup

The default SMS app status is handled as a `Role` on API levels 29 (Android 10,
Q) and above, and the applicable `adb` commands change at that same version.

#### API levels 29+

You'll want to reset the previous default once you're done. This command should
return (at most) one application ID.

```
adb shell cmd role get-role-holders android.app.role.SMS
```

To set this app as the default, we use the add command. Since this is an
exclusive role, add acts as a setter.

```
adb shell cmd role add-role-holder android.app.role.SMS dev.gonodono.adbsms
```

Call the add command again with the previous app when finished. If there wasn't
one set, you can use `remove-role-holder` instead, but you might get an
`Exception` if the system tries to do something with the new default without
first checking for a valid value. The remove will likely still work; it may just
complain afterward.

#### API levels < 28

The old method involves fiddling with `Settings.Secure` through `adb`.
Retrieving the current value is straightforward:

```
adb shell settings get secure sms_default_application
```

…as is setting a new value:

```
adb shell settings put secure sms_default_application "dev.gonodono.adbsms"
```

…then call `put` again with the previous default when done, or `delete` if none.

Unfortunately, I'm not sure that this method is reliable everywhere; the
`sms_default_application` key is hidden from the SDK. A potential fallback could
be had by launching the default app change action.

```
adb shell am start -a android.provider.Telephony.ACTION_CHANGE_DEFAULT --es package "dev.gonodono.adbsms"
```

Some sort of UI interaction would be required since this does display a dialog,
though it is relatively simple and amenable to the standard automation tools.

### Main app features

All these features can be accessed and modified through the shell.

The `Boolean` values are all _enabled_ flags; i.e., if `true`, the given
functionality is active.

<div align="center">
  <table>
    <tr>
      <td><code>notifyStatus</code></td>
      <td><code>Boolean</code></td>
      <td>Flag for the persistent status notification</td>
    </tr>
    <tr>
      <td><code>notifyReceipts</code></td>
      <td><code>Boolean</code></td>
      <td>Flag for incoming message notifications</td>
    </tr>
    <tr>
      <td><code>logReceipts</code></td>
      <td><code>Boolean</code></td>
      <td>Flag for incoming message logs</td>
    </tr>
    <tr>
      <td><code>storeReceivedSms</code></td>
      <td><code>Boolean</code></td>
      <td>Flag for incoming SMS message storage</td>
    </tr>
    <tr>
      <td><code>originalDefault</code></td>
      <td><code>String</code></td>
      <td>The original default SMS app package</td>
    </tr>
  </table>
</div>

The `call` command is used for these non-CRUD operations.

```
adb shell content call --uri <URI> --method <METHOD> [--arg <ARG>] [--extra <BINDING> ...]
```

`<URI>` should be a base content URI. Since they all target the same Provider
class, any of the main app's three [Authorities](#authorities) can be used,
e.g., `content://adbsms`.

The feature name is passed through `<METHOD>`, and the optional `<ARG>` has a
dual use as both getter and setter, so to speak. `<BINDING>` isn't used for the
call, but the returned result will be a similar structure.

As for `<ARG>`, if it's omitted, the call acts as a getter, and the result will
be a `Bundle` containing a single value.

If `<ARG>` is included, the call acts as a setter, and the given feature setting
is updated with the new value.

For example, to get the current `notifyStatus` value:

```
adb shell content call --uri content://adbsms --method notifyStatus
```

Which will return something like:

```
Result: Bundle[{notifyStatus=true}]
```

To set `notifyStatus` instead, it's the same command with the `<ARG>` appended:

```
adb shell content call --uri content://adbsms --method notifyStatus --arg false
```

Setters return an empty `Bundle` upon success – or, more accurately, upon not
throwing – since returning `null` can cause some environments to throw
`NullPointerException`s, and a token _success_ value would just be confusing.

```
Result: Bundle[{}]
```

### Status notification updates

It's also possible to trigger updates to the status notification from the shell.
Since these are normally enacted automatically by the UI, this allows for
refreshing headlessly.

If you plan to manage notifications without ever opening the UI, you'll need to
grant the necessary permission from the shell on API levels 33 (Android 13,
Tiramisu) and above.

```
adb shell pm grant dev.gonodono.adbsms android.permission.POST_NOTIFICATIONS
```

To fire, make a `call` to the `updateStatus` method with no `<ARG>`.

```
adb shell content call --uri content://adbsms --method updateStatus
```

As with the setters, this too will result in an empty `Bundle` upon not
throwing.

```
Result: Bundle[{}]
```

If you're handling everything from the shell and wish to keep the status
notification updated, you _must_ call `updateStatus` after every relevant
change, e.g., upon first granting the `READ_SMS` permission, whenever
`notifyStatus` is modified, etc.

<br />

## Minimal

A zero-frills, no-UI version – adbsms.min – is provided in the `:min` module.
It's meant for [Headless](#headless) use in scripts, with agents, etc. The only
class it contains is the relay `ContentProvider`, and its access options must be
handled through Settings or the shell.

> [!CAUTION]
> The minimal app has _**no** status notification_. If you set adbsms.min as the
> default SMS app, you must remember yourself to reset the previous app when
> done.

Since there is no UI, adbsms.min doesn't have a launcher icon either, so you
likely won't see it in the regular app list of your average home screen.

The minimal app does have a distinct application ID – `dev.gonodono.adbsms.min`
– and its own content [Authorities](#authorities), so it can be installed
alongside the main app, if need be.

This version also can assume the default SMS app role for full access, since it
has all the necessary components registered. However, none of the underlying
classes actually exist, and attempts to access any of them will result in
errors.

Consequently, the minimal app does _not_ offer the incoming SMS storage
fallback. Indeed, the default app is completely nonfunctional while adbsms.min
holds the role.

### Command line changes

The only differences here are the application ID and authority substitutions.
For instance, to set up read-only access, amend the application ID in the
command given [above](#read-only-setup).

```
adb shell pm grant dev.gonodono.adbsms.min android.permission.READ_SMS
```

The first query from the SMS [Examples](#examples) would have the authority
changed thusly:

```
adb shell content query --uri content://adbsms.min --projection address:body
```

The second one similarly so:

```
adb shell content query --uri content://adbsms.min/sent --projection address:body
```

Et cetera.

<br />

## Download

Each new version is accompanied by [a GitHub release][latest] with assets that
contain `apk`s of release build variants of both `:app` and `:min` signed with a
debug key, which is the current setup in each module's `build.gradle.kts`. These
apps aren't published anywhere, as they're intended to be sort of homebrew tools
for developers and power users, and the unusual configuration is used to apply
ProGuard and whatnot to "unsigned" builds.

> [!NOTE]
> Signing these APKs with a debug key means that the key is going to change for
> each release. Any existing prior versions must be uninstalled before updating.

I'm not encouraging anyone to prefer the pre-built APKs; they're simply a
convenience for users who don't have the setup available to do it themselves, or
anyone who just wants a quick test. They're assembled using GitHub Actions and
[this local workflow][workflow], so you can be reasonably certain that there are
no malicious injections or modifications.

The GitHub releases for automated builds will be created by user
`github-actions`, whose name links to https://github.com/apps/github-actions,
which actually redirects elsewhere if followed. Workflow execution details can
be found on [the Actions tab][actions].

<br />

## Notes

New items are listed first.

Those with headings ending in question marks are soliciting user feedback.

### Upgrading to 0.0.13

In this version, the apps' Providers have had their authorities expanded to
cover all the standard message Providers. For anyone building this themselves
with a constant key, if you still have a prior version installed, Android
technically should be able to handle the upgrade without issue. If you have any
problems, however, you may need to uninstall.

### Proper APK signing?

I originally avoided properly signing the pre-built APKs just because I didn't
want to have to mess with yet another key. I recently remembered, though, that I
already have one from a separate gonodono project here, so I'm considering it
for adbsms.

If you're savvy enough to be using this in the first place, you're clearly smart
enough to _not_ install this app unless you've downloaded it straight from here.
However, I'm becoming increasingly convinced that it's not a great idea to leave
these unsigned anyway, especially with the rise of agents and their proclivity
for devising novel "attacks".

I don't foresee any problems if I do start signing with a static key, but I'll
hold off for a release or two to give users a chance to [object][issue] if it's
going to cause issues.

### Other system Providers?

Given that this app does little more than relay requests, it would be trivial to
extend this functionality to other Providers too, but I'm not sure if anyone's
having similar trouble with any of them.

- Alarm clock
- Blocked numbers
- Calendar
- Call logs
- Contacts
- Media store
- Settings

These are those that I think can be accessed through `adb`, at least to some
practical effect, anyway. If you might find it useful for adbsms – or a
separate, similar app – to handle any of these, please feel free to file an
[issue][issue] as a request.

### Minimum Android versions

The `minSdk` for the `:app` module is 24 (Nougat). If you need that to be lower
and you're cloning the repo, I'll assume that you can figure out where to add
the necessary checks and such. If anyone really needs the pre-built APK to
support prior versions, [file an issue][issue] as a request for the desired
minimum and I'll see what I can do.

The `minSdk` for the `:min` module is 19 (KitKat), which is the version that
introduced the official SMS API. I doubt that anyone is running anything that
old anymore, but it would be the same code with any newer version too, so might
as well.

### Supported Provider operations

I haven't implemented every possible `ContentProvider` operation in either [the
main `AdbSmsProvider`][provider] or [the minimal one][provider.min], but they do
cover all the required overrides. I _think_ that should be sufficient for
everything that `adb` can do, but if you find something I've missed, please
[file an issue][issue] for it.

### Full access limitations

If you plan to use the **Full access** option in order to get at the hidden
message types, you should know that not all SMS apps utilize each one. Though
most use `inbox` and `sent` consistently, it seems that many apps don't use
`draft` and/or the others at all. Apparently they save those messages to
internal storage instead, for whatever reason. Just a heads-up.

### No more golf

Ever since I added the automated build and release, I've been working to shrink
(golf) the APK by various means, mainly as a selling point, I guess. I won't be
doing that anymore, since there's now a separate module dedicated to a minimal
size implementation. I'll generally try to keep things small, but `adbsms.apk`'s
size may fluctuate in the future, rather than decreasing monotonically.

It should also be noted that `:min` is about as bare-bones as is feasible, so
even small differences between build tool versions may be enough to effect
`adbsms.min.apk`'s size, which is therefore liable to fluctuate as well.

### Bug reports

Please report any bugs or other problems encountered in using this project
[here][issue].

<br />

## License

MIT License

Copyright (c) 2026 Mike M.

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
ead
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

[wiki]: https://github.com/gonodono/adbsms/wiki
[issue]: https://github.com/gonodono/adbsms/issues/new
[contract]:
  https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns
[wiki-sms-type]: https://github.com/gonodono/adbsms/wiki/SMS#type
[latest]: https://github.com/gonodono/adbsms/releases/latest
[workflow]: .github/workflows/build_and_release_apks.yaml
[actions]: https://github.com/gonodono/adbsms/actions
[provider]: app/src/main/kotlin/dev/gonodono/adbsms/AdbSmsProvider.kt
[provider.min]: min/src/main/java/dev/gonodono/adbsms/min/AdbSmsProvider.java