# FTTools

An Android toolbox for changing system settings quickly. Each tool turns a setting that's buried several screens deep in Android Settings into one tap.

Requires Android 9 (API 28) or newer.

## Tools

| Tool | What it does |
|---|---|
| [Private DNS](docs/private-dns.md) | Switch between saved Private DNS providers, or turn Private DNS off, with one slider per provider |
| [Screen timeout](docs/screen-timeout.md) | Set how long the screen stays on before it turns off and locks |

Open a tool from the FTTools home screen. Each tool's page above covers how to use it and what it changes.

Tools that change protected settings share the [one-time permission grant](#3-grant-the-permission-once-per-phone) below, so one grant covers all of them.

## Setup

### 1. Build

Requires JDK 17+ and the Android SDK (compileSdk 36).

```sh
./gradlew assembleDebug assembleRelease
```

Each APK is copied into a `build` folder at the project root:

| Build | File | Signed with |
|---|---|---|
| Debug | `build/debug/FTTools-<version>-debug.apk` | `keys/debug.keystore` |
| Release | `build/release/FTTools-<version>-release.apk` | `keys/release.jks` |

### Signing keys

Both keys live in a `keys` folder at the project root, which Git ignores:

```
keys/
├── debug.keystore        Debug key (alias androiddebugkey, password android)
├── release.jks           Release key (alias pdns-release)
└── keystore.properties   Release key path, alias and passwords
```

`keystore.properties` format:

```properties
storeFile=release.jks
storePassword=<password>
keyAlias=pdns-release
keyPassword=<password>
```

Back up the `keys` folder outside this PC. If `release.jks` or its password is lost, the installed app can never be updated; it has to be uninstalled and the permission granted again.

### 2. Install

```sh
adb install -r build/release/FTTools-<version>-release.apk
```

Use the release APK on your phone. The debug and release APKs are signed with different keys, so one can't be installed over the other without uninstalling.

### 3. Grant the permission (once per phone)

Many Android settings, including Private DNS, are protected. An app can only change them if it holds `WRITE_SECURE_SETTINGS`, which can't be granted from the phone itself. With USB debugging on, run:

```sh
adb shell pm grant com.pdns.switcher android.permission.WRITE_SECURE_SETTINGS
```

Then close and reopen the app. Without the permission, the app shows a red "One-time setup needed" card with this command and a copy button.

The package ID is still `com.pdns.switcher` from when the app only switched Private DNS. Keeping it lets installed copies update in place and keep their grant.

If several phones are connected, pick one with `adb -s <serial>`; `adb devices -l` lists serials and models.

#### Connecting the phone

- **USB:** turn on Settings → Developer options → **USB debugging**, connect the cable, unlock the phone and tap **Allow** on the "Allow USB debugging?" prompt. On vivo and some other phones, also turn on **USB debugging (Security settings)** if it's there, or the grant can fail with a security error.
- **Wireless (Android 11+, same Wi-Fi):** turn on Developer options → **Wireless debugging**, tap **Pair device with pairing code**, then run:

  ```sh
  adb pair <ip>:<pairing-port>
  adb connect <ip>:<port>
  ```

#### When the grant is lost

The grant **survives app updates**. It is lost only when:

- the app is **uninstalled**, or
- an APK **signed with a different key** is installed, for example switching from the debug build to the release build, which forces an uninstall.

To avoid granting again:

- Always install new versions over the existing app; don't uninstall first.
- Keep using the same build type and key on a given phone.
- Increase `versionCode` in `app/build.gradle.kts` for every release, so the new APK installs as an update.

The grant applies to the main profile only. A copy of the app in Dual Apps, a clone space or a work profile needs its own grant.

#### Why the app can't ask for this permission itself

Android sorts permissions into levels:

| Level | Examples | How an app gets it |
|---|---|---|
| Normal | Internet, vibrate | Automatically at install |
| Dangerous | SMS, camera, location | A popup asking the user |
| Special access | Modify system settings, display over other apps | A switch in Settings |
| Signature / privileged | `WRITE_SECURE_SETTINGS` | No popup or switch; only system apps or an adb grant |

Protected settings are in the last level on purpose. For example, an app that can change DNS can route all web traffic through a server it controls. The "Modify system settings" switch (`WRITE_SETTINGS`) only covers basic settings such as brightness and ringtone, not protected ones.

Ways to avoid a PC that aren't built yet:

- **Shizuku:** a free app, started on the phone with Wireless debugging, that could let FTTools grant itself. It needs restarting after each reboot, but only for the grant itself.
- **Root:** the app could grant itself on a rooted phone.

## Project layout

```
app/src/main/java/com/pdns/switcher/
├── MainActivity.kt          Navigation, home screen, shared UI (scaffold, status and permission cards)
├── Grants.kt                Permission checks and the adb grant command
├── PrivateDns.kt            Private DNS: reads/writes system settings, validates hostnames
├── DnsStore.kt              Private DNS: saved provider list and defaults
├── PrivateDnsScreens.kt     Private DNS: tool screen, provider management, edit dialog
├── ScreenTimeout.kt         Screen timeout: reads/writes the setting, saved custom values
└── ScreenTimeoutScreen.kt   Screen timeout: tool screen, custom value dialog
docs/                        One help page per tool
```

## Adding a tool

A new tool usually needs:

1. A settings helper like `PrivateDns.kt` that reads and writes the setting through `Settings.Global`, `Settings.Secure` or `Settings.System`. Protected settings are covered by the existing `WRITE_SECURE_SETTINGS` grant; `Settings.System` values can instead use the in-app "Modify system settings" permission (see `Grants.kt`).
2. A screen file, a `Screen` entry and a home screen card in `MainActivity.kt`.
3. A help page in `docs/`, linked from the [Tools](#tools) table above.
