# BuzzKill: Implementation Plan

Android app that powers off the phone after an inactivity timeout, only inside a configurable nightly window. Single screen, Winamp-panel aesthetic.

## Stack

Kotlin, Jetpack Compose, Material 3 (heavily restyled), single Activity, ViewModel + StateFlow, DataStore Preferences. No Hilt, no Room, no networking. Min SDK 26, target latest.

## Modules

One app module. Suggested package layout:

```
com.buzzkill/
  ui/           // Compose screen, theme, custom components (LED, 7seg, panel)
  service/      // ShutdownAccessibilityService, ForegroundService
  scheduling/   // AlarmScheduler, BootReceiver, PackageReplacedReceiver
  data/         // SettingsRepository (DataStore wrapper), models
  oem/          // OEM detection, power-on intent resolution, power-dialog string table
```

## Core Mechanism

Shutdown via accessibility service:

1. `performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)`
2. Wait ~500ms
3. Walk `AccessibilityNodeInfo` tree, match locale-aware "Power off" / "Shut down" strings from `oem/PowerDialogStrings.kt`
4. `performAction(ACTION_CLICK)` on match, `dispatchGesture` fallback
5. Repeat walk for confirmation dialog if present

Ship with stock Pixel, recent Samsung, Xiaomi covered. Make the string table easy to extend.

## Scheduling

`AlarmManager.setAlarmClock()` for window-open and window-close (daily). Avoids `SCHEDULE_EXACT_ALARM` and survives Doze. Re-arm on `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.

Inside the window, accessibility service listens to `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` (registered dynamically in `onServiceConnected`). On screen off, schedule one-shot inactivity alarm via `setExactAndAllowWhileIdle`. On screen on, cancel it. If it fires and screen still off and still in window: trigger shutdown.

## Foreground Service

Persistent notification while window is active. Type `specialUse` with justification. Not strictly required for the mechanism but improves OS retention and user trust.

## Permissions / Setup

Onboarding checklist, each row deep-links to the right system surface and re-verifies on `ON_RESUME`:

1. Accessibility service enabled (`Settings.ACTION_ACCESSIBILITY_SETTINGS`, verify via `AccessibilityManager.getEnabledAccessibilityServiceList`)
2. Battery optimization exempted (`Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
3. Notification permission (Android 13+, runtime request)
4. OEM autostart configured (Xiaomi, Oppo/Vivo/Realme, Huawei, sometimes Samsung). Per-OEM intent table, fallback to generic message. No programmatic verification, user-acknowledged.
5. Scheduled power-on configured (user-acknowledged checkbox). See below.

## Scheduled Power-On

The phone cannot wake itself from a fully-off state. There is no AOSP API. Some OEMs expose it in their Clock or Settings app (Samsung, Xiaomi, Huawei, some Oppo). Onboarding branches on `Build.MANUFACTURER`:

- Supported OEM: deep-link to scheduled-power-on settings (try OEM-specific intent, fallback to launching Clock app, fallback to instructional dialog). Pre-fill suggestion: window-close time + 5 minutes.
- Unsupported OEM (Pixel, recent OnePlus): honest dialog explaining the limitation. User can proceed anyway.
- Unknown: treat as unsupported.

This is a user-acknowledged checkbox. App cannot verify it.

First-shutdown confirmation dialog ("Phone will not turn back on automatically unless scheduled. Continue?") with don't-show-again. Fires once, ever.

## State

`SettingsRepository` wraps DataStore. Keys:

- `windowStartMinutes`, `windowEndMinutes` (minutes from midnight)
- `inactivityTimeoutSeconds`
- `enabled` (master toggle)
- `isInWindow`, `isArmed`, `lastTriggerTime`, `countdownStartedAt` (status, written by service, read by UI)
- `permissionAcks` (map of user-acknowledged items)
- `firstShutdownConfirmed`

ViewModel exposes `StateFlow<UiState>` derived from the repository. Service writes status updates; UI observes. One-way. No service binding from Activity.

## UI

Single screen, top to bottom:

1. **Schedule panel.** Master enable toggle (LED switch). Window start time (7-seg, tap to edit). Window end time (7-seg). Inactivity timeout (7-seg + stepper).
2. **Status panel.** If permissions incomplete: checklist with LED indicators and "Fix" buttons. If complete: "Armed. Starts in 3h 24m" or "Active. 4h 12m remaining" with live countdown.
3. **Test trigger.** Big red button. 10-second cancellable countdown, then opens power dialog and stops one step short of the final tap. For verifying the mechanism on the user's specific device.

### Aesthetic

90s hardware panel / Winamp:

- Near-black background with slight warm tint, beveled panel edges, inner shadows on recessed elements
- Time displays in 7-segment LCD font (DSEG, open source). Amber or red on dark. Faintly visible unlit segments behind lit ones.
- Toggles as physical sliders with LED above, lit green when on
- Permission items as vertical row of LEDs, red unlit / green lit
- Buttons styled as physical hardware buttons with pressed state
- Monospace throughout, or sans for labels with mono for numerals
- No Material You dynamic color. Fixed theme.

LED glows via `Modifier.drawBehind` with radial gradient. 7-seg as custom `FontFamily`. Panel beveling via layered backgrounds and borders.

## Manifest

- `BIND_ACCESSIBILITY_SERVICE` (signature, granted by user via settings)
- `RECEIVE_BOOT_COMPLETED`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`
- `POST_NOTIFICATIONS` (runtime, Android 13+)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

Receivers: `BootReceiver` (BOOT_COMPLETED), `PackageReplacedReceiver` (MY_PACKAGE_REPLACED). Both re-arm alarms.

## Build Order

1. Project skeleton, theme, DataStore repository, basic Compose screen with placeholder values
2. Custom UI components: 7-seg display, LED indicator, LED switch, hardware button, panel container
3. Settings panel and status panel wired to ViewModel and repository
4. Permission checklist with deep-link intents and `ON_RESUME` verification
5. AccessibilityService skeleton, screen-state receiver, status writes to DataStore
6. AlarmScheduler with `setAlarmClock` for window bounds, BootReceiver, PackageReplacedReceiver
7. Inactivity alarm and shutdown trigger logic
8. Power-dialog node walking with OEM string table, `dispatchGesture` fallback
9. Test trigger button with stop-before-final-tap mode
10. Foreground service with persistent notification
11. OEM scheduled-power-on onboarding flow
12. First-shutdown confirmation dialog
13. Polish, OEM testing, string table expansion

## Known Risks

- OEM power-dialog string table maintenance. Highest-likelihood breakage. Mitigate with Test trigger and easy-to-extend string resource.
- OEM background-killing on Xiaomi/BBK. Document via dontkillmyapp.com link in onboarding. Foreground service helps but doesn't eliminate.
- Scheduled-power-on cannot be verified. User-acknowledged only. First-shutdown confirmation softens the failure mode.
- Accessibility permission auto-revocation if user doesn't open the app for months. Add a periodic "still active" check and notification.
