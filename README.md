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
