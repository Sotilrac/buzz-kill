# BuzzKill

Android app that powers off the phone after an inactivity timeout, only inside a configurable nightly window. Single-screen, Winamp-style hardware-panel UI.

Primary target: **OnePlus 11 (OxygenOS)**. The code keeps OEM specifics in `app/src/main/kotlin/com/buzzkill/oem/` so adding Pixel, Samsung, Xiaomi, etc. is a data-only change.

## Why this exists

The phone wakes you up. Notifications, the temptation to scroll, the buzz at 2am. BuzzKill kills the phone instead. Inside a window you set (e.g. 23:00–07:00), the app waits for the screen to stay off for N minutes, then triggers the system shutdown via the accessibility service. To come back on in the morning, use the OEM's built-in scheduled power-on (Settings → Additional settings → Scheduled power on/off on OnePlus).

## Host dependencies

You need these on your dev machine before opening the project.

### JDK

Android Gradle Plugin 8.x needs JDK 17. AGP 9.x needs JDK 21. We're on AGP 8.x for now.

```bash
sudo apt install openjdk-17-jdk
java -version   # confirm 17.x
```

Or via [SDKMAN](https://sdkman.io/) if you want to juggle multiple JDKs:

```bash
curl -s "https://get.sdkman.io" | bash
sdk install java 17.0.13-tem
```

### Android Studio

Install the latest stable from <https://developer.android.com/studio>. As of May 2026 that's the Otter / Panda 4 release line. Android Studio bundles a compatible Gradle and Kotlin.

On Linux, the easiest path is the JetBrains Toolbox or the official `.tar.gz`. Snap and Flatpak versions exist but tend to lag.

### Android SDK components

Open Android Studio → SDK Manager and install:

- **SDK Platforms**: Android 15 (API 35) — required, this is our `targetSdk`. Android 16 (API 36) optional.
- **SDK Tools**:
  - Android SDK Build-Tools 35.x
  - Android SDK Platform-Tools (`adb`, `fastboot`)
  - Android SDK Command-line Tools (latest)

Make sure `~/Android/Sdk/platform-tools` is on your `PATH` so `adb` works from a terminal:

```bash
echo 'export PATH="$HOME/Android/Sdk/platform-tools:$PATH"' >> ~/.bashrc
```

### Optional but helpful

- **`scrcpy`** — mirror your phone screen on the desktop while testing the dim-room UI without unlocking constantly:
  ```bash
  sudo apt install scrcpy
  ```
- **`adb` over Wi-Fi** — see device setup below.

## Device setup (OnePlus 11)

1. Open `Settings → About device → Version`. Tap **Build number** seven times. Enter your PIN. Developer Options is now unlocked.
2. `Settings → System → Developer options`:
   - Enable **USB debugging**.
   - Enable **Wireless debugging** if you want to flash without a cable.
3. Plug the phone into your machine over USB. Approve the RSA fingerprint prompt on the phone.
4. Confirm `adb` sees it:
   ```bash
   adb devices
   ```
   You should see one device listed as `device` (not `unauthorized`).

For wireless debugging, on the phone open **Wireless debugging → Pair device with pairing code**, then on the desktop:

```bash
adb pair <ip>:<pair-port>           # use the code shown on the phone
adb connect <ip>:<connect-port>
```

## Project bring-up

Once dependencies are installed:

```bash
git clone <this-repo-url> buzz-off
cd buzz-off
# Open in Android Studio. Let the Gradle sync complete.
# Plug in the OnePlus 11. Click Run.
```

The first sync will download Gradle, the AGP, Kotlin, and the Compose libraries. It can take a few minutes.

## After install: permission walkthrough

The app shows an onboarding checklist on first launch. Each row deep-links to the right system surface and re-verifies when you return.

1. **Accessibility service.** OnePlus puts this at `Settings → Additional settings → Accessibility → Installed services → BuzzKill`. Toggle it on. This is the mechanism the app uses to power off; without it nothing else works.
2. **Disable battery optimization for BuzzKill.** OxygenOS will otherwise kill the foreground service while the screen is off and the timer never fires.
3. **Notification permission.** Android 13+ requirement. The persistent notification while the window is active needs this.
4. **Scheduled power on/off.** This is the **mitigation for the "phone won't come back on" risk**. Set the phone to auto-power-on around your wake time:
   - `Settings → Additional settings → Scheduled power on/off`
   - Enable the power-on schedule. Suggested: 5 minutes after your window-close time.
   - The app cannot configure this on your behalf and cannot verify it; it's a checkbox you confirm yourself. If you forget it, the phone won't wake up by itself.

A first-shutdown confirmation dialog also appears the very first time the inactivity timer fires, so you can't get caught unaware.

## OnePlus and aggressive battery management

OxygenOS is moderately aggressive about killing background work. See <https://dontkillmyapp.com/oneplus> for the canonical advice. In short:

- Disable battery optimization for BuzzKill (covered in onboarding).
- Lock BuzzKill in Recents (long-press in Recents → padlock icon) so OxygenOS doesn't sweep it.
- Don't enable "Deep Optimization" or "Advanced Optimization" battery modes for this app.

## Testing the shutdown mechanism without losing your work

The app includes a **Test trigger** with a 10-second cancellable countdown that runs the shutdown sequence in dry-run mode by default. It opens the system power dialog, finds the matching node, and Toasts what it would have tapped — without actually tapping. Use this after install to verify that the OnePlus power-dialog strings still match.

## Uninstall

```bash
adb uninstall com.buzzkill
```
