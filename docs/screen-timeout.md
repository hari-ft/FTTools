# Screen timeout

An FTTools tool that sets how long the screen stays on without being touched before it turns off and locks. Most phones show this as Settings → Display → **Screen timeout**; some call it **Auto-lock** or **Sleep**.

## Using it

- Tap a duration to apply it straight away. The selected one matches the phone's current setting.
- The durations and their names come from the phone's own Settings app, so the list matches what the phone offers. For example, vivo phones show the same choices as Settings → Display → **Auto-lock**.
- The app has no built-in list. If the phone's list can't be read, the card at the top says so and only custom durations are shown.
- Many phones, including vivo, use Android's standard list (15 seconds, 30 seconds, 1, 2, 5, 10 and 30 minutes), so the tool shows the same seven choices. Phones with a different list show their own.
- **Add custom** adds any duration from 5 seconds to 24 hours, in seconds, minutes or hours. Custom durations are marked "Custom" and have a delete icon; the phone's own durations can't be deleted.
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

The duration list is read from the Settings app's `screen_timeout_values` and `screen_timeout_entries` resources. That's the same list the phone's own screen shows, with names in the phone's language. The manifest declares a `<queries>` entry so the app can see the Settings app on Android 11+.

## Code

| File | Purpose |
|---|---|
| `ScreenTimeout.kt` | Reads and writes the setting, loads the phone's duration list, formats durations, stores custom values |
| `ScreenTimeoutScreen.kt` | Tool screen and custom duration dialog |
