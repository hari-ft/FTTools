# PDNSapp

An Android app that switches the system **Private DNS** provider with one tap, without going through Settings → Network → Private DNS.

- One on/off slider per saved provider; turning one on turns the others off.
- Turning off the active provider sets Private DNS to **Off**.
- A settings screen to add, edit and delete providers.
- Shows the phone's real current Private DNS state, including changes made in system Settings.

Requires Android 9 (API 28) or newer.

## Setup

### 1. Build

Requires JDK 17+ and the Android SDK (compileSdk 36).

```sh
./gradlew assembleDebug assembleRelease
```

Each APK is copied into a `build` folder at the project root:

| Build | File | Signed with |
|---|---|---|
| Debug | `build/debug/PDNSapp-<version>-debug.apk` | `keys/debug.keystore` |
| Release | `build/release/PDNSapp-<version>-release.apk` | `keys/release.jks` |

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
adb install -r build/release/PDNSapp-<version>-release.apk
```

Use the release APK on your phone. The debug and release APKs are signed with different keys, so one can't be installed over the other without uninstalling.

### 3. Grant the permission (once per phone)

Android only lets an app change Private DNS if it holds `WRITE_SECURE_SETTINGS`, which can't be granted from the phone itself. With USB debugging on, run:

```sh
adb shell pm grant com.pdns.switcher android.permission.WRITE_SECURE_SETTINGS
```

Then close and reopen the app. Without the permission, the app shows a red "One-time setup needed" card with this command and a copy button.

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

Private DNS is in the last level on purpose. An app that can change DNS can route all web traffic through a server it controls. The "Modify system settings" switch (`WRITE_SETTINGS`) only covers basic settings such as brightness, not Private DNS.

Ways to avoid a PC that aren't built yet:

- **Shizuku:** a free app, started on the phone with Wireless debugging, that could let PDNSapp grant itself. It needs restarting after each reboot, but only for the grant itself.
- **Root:** the app could grant itself on a rooted phone.

## Providers

Private DNS uses DNS-over-TLS, so it accepts **hostnames only**. An IP address such as `1.1.1.1` won't work, and the app rejects one with a suggested hostname.

Built-in defaults:

| Provider | Hostname | Blocks |
|---|---|---|
| AdGuard | `dns.adguard-dns.com` | Ads, trackers, phishing |
| Cloudflare (1.1.1.1 / 1.0.0.1) | `one.one.one.one` | Nothing |
| Google (8.8.8.8 / 8.8.4.4) | `dns.google` | Nothing |

### Other providers you can add

Add any of these from the app's settings screen. Any service that publishes a DNS-over-TLS hostname will work; this isn't a complete list.

**No filtering:** privacy and speed only; every site, ad and tracker loads normally.

| Provider | Hostname |
|---|---|
| Cloudflare (1.1.1.1 / 1.0.0.1) | `one.one.one.one` |
| Google (8.8.8.8 / 8.8.4.4) | `dns.google` |
| Quad9, unfiltered | `dns10.quad9.net` |
| AdGuard, unfiltered | `unfiltered.adguard-dns.com` |
| Mullvad | `dns.mullvad.net` |
| Control D, unfiltered | `p0.freedns.controld.com` |
| DNS.SB | `dot.sb` |

**Malware and phishing blocking**

| Provider | Hostname |
|---|---|
| Quad9 | `dns.quad9.net` |
| Cloudflare Security | `security.cloudflare-dns.com` |
| CleanBrowsing Security | `security-filter-dns.cleanbrowsing.org` |
| Control D, malware | `p1.freedns.controld.com` |

**Ad and tracker blocking:** sites still load, but ad and tracker domains are blocked.

| Provider | Hostname |
|---|---|
| AdGuard | `dns.adguard-dns.com` |
| Mullvad, ads | `adblock.dns.mullvad.net` |
| Mullvad, ads + trackers + malware | `base.dns.mullvad.net` |
| Control D, ads + malware | `p2.freedns.controld.com` |

**Family:** blocks adult content and forces safe search.

| Provider | Hostname |
|---|---|
| AdGuard Family | `family.adguard-dns.com` |
| Cloudflare Family | `family.cloudflare-dns.com` |
| CleanBrowsing Family | `family-filter-dns.cleanbrowsing.org` |
| CleanBrowsing Adult | `adult-filter-dns.cleanbrowsing.org` |
| Mullvad Family | `family.dns.mullvad.net` |
| Control D Family | `family.freedns.controld.com` |

**Custom:** you choose what gets blocked in the provider's dashboard. Sign up for a free account to get your ID.

| Provider | Hostname |
|---|---|
| NextDNS | `<your-id>.dns.nextdns.io` |
| AdGuard DNS personal | `<your-id>.d.adguard-dns.com` |
| Control D custom | `<your-id>.dns.controld.com` |

**Not supported:** OpenDNS, Comodo, plain IP addresses and your ISP's own DNS don't offer DNS-over-TLS, so Android can't use them for Private DNS.

### Ad blocking side effects

Ad blocking can stop sponsored search results, deal or email tracking links, and "watch an ad" rewards from working. Rarely, it breaks a login or payment page. When that happens, switch to an unfiltered provider for that task, then switch back. It can't block YouTube, Facebook or Instagram ads, because those come from the same domain as the content.

## How it works

The app writes the same system settings the Private DNS screen uses:

| Setting (`Settings.Global`) | Value |
|---|---|
| `private_dns_mode` | `off`, `opportunistic` (Auto) or `hostname` |
| `private_dns_specifier` | The provider hostname |

The saved provider list is kept in the app's SharedPreferences.

## Project layout

```
app/src/main/java/com/pdns/switcher/
├── MainActivity.kt   Compose UI: home screen, settings screen, edit dialog
├── PrivateDns.kt     Reads/writes system settings, validates hostnames
└── DnsStore.kt       Saved provider list and defaults
```
