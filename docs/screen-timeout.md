# Screen timeout

An FTTools tool that sets how long the screen stays on without being touched before it turns off and locks. Most phones show this as Settings → Display → **Screen timeout**; some call it **Auto-lock** or **Sleep**.

## Using it

- Tap a duration to apply it straight away. The selected one matches the phone's current setting.
- Built-in durations: 15 seconds, 30 seconds, 1, 2, 5, 10 and 30 minutes.
- **Add custom** adds any duration from 5 seconds to 24 hours, in seconds, minutes or hours. Custom durations show a delete icon; built-in ones can't be deleted.
- The card at the top shows the current timeout, including changes made in system Settings.

## Permission

This setting isn't protected, so the app can ask for it without a computer:

- If the phone already has the `WRITE_SECURE_SETTINGS` grant from the [README](../README.md#3-grant-the-permission-once-per-phone), it works straight away.
- Otherwise, the tool shows a **Permission needed** card. Tap **Open permission settings** and turn on **Allow modifying system settings** for FTTools.

## Limits

- A work profile or device management policy can cap the maximum timeout; the phone then uses the lower value.
- Some phones only list certain durations in their own Settings screen. A custom value still applies, but that screen may show no option selected.

## How it works

The tool writes `Settings.System.SCREEN_OFF_TIMEOUT` in milliseconds, the same value the system screen-timeout setting uses. Custom durations are kept in the app's SharedPreferences.

## Code

| File | Purpose |
|---|---|
| `ScreenTimeout.kt` | Reads and writes the setting, formats durations, stores custom values |
| `ScreenTimeoutScreen.kt` | Tool screen and custom duration dialog |
