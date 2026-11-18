# DNS Switcher

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
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Install

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant the permission (one time)

Android only lets an app change Private DNS if it holds `WRITE_SECURE_SETTINGS`, which can't be granted from the phone itself. With USB debugging on, run:

```sh
adb shell pm grant com.pdns.switcher android.permission.WRITE_SECURE_SETTINGS
```

The grant survives app updates. Run it again after uninstalling or on a new phone. Without it, the app shows this command with a copy button.

## Providers

Private DNS uses DNS-over-TLS, so it accepts **hostnames only**. An IP address such as `1.1.1.1` won't work, and the app rejects one with a suggested hostname.

Built-in defaults:

| Provider | Hostname | Blocks |
|---|---|---|
| AdGuard | `dns.adguard-dns.com` | Ads, trackers, phishing |
| Cloudflare (1.1.1.1 / 1.0.0.1) | `one.one.one.one` | Nothing |
| Google (8.8.8.8 / 8.8.4.4) | `dns.google` | Nothing |

Other hostnames you can add:

| Provider | Hostname | Blocks |
|---|---|---|
| AdGuard, unfiltered | `unfiltered.adguard-dns.com` | Nothing |
| AdGuard Family | `family.adguard-dns.com` | Ads, trackers, adult sites |
| Quad9 | `dns.quad9.net` | Malware, phishing |
| Cloudflare Security | `security.cloudflare-dns.com` | Malware |
| Cloudflare Family | `family.cloudflare-dns.com` | Malware, adult sites |
| Mullvad | `dns.mullvad.net` | Nothing |
| Mullvad, ad blocking | `adblock.dns.mullvad.net` | Ads |
| CleanBrowsing Family | `family-filter-dns.cleanbrowsing.org` | Adult sites |
| NextDNS | `<your-id>.dns.nextdns.io` | Your own configuration |

If an ad-blocking provider breaks a link, login or payment page, switch to an unfiltered one for that task.

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
