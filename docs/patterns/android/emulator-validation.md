# On-device / emulator validation

Automated tests (`commonTest`, `jvmTest`) cover business logic, HTTP, and the
desktop UI. They cannot exercise the Android-only machinery — WorkManager,
Doze, the battery-optimization exemption, the system network callback, or
DataStore writes from a background worker. That behaviour is validated by
hand on an emulator or device, driven entirely through `adb`. This is the
recipe.

## Setup

```bash
./gradlew androidApp:assembleDebug
APK=androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb devices                       # emulator (Android Studio) or device must be listed
adb install -r "$APK"             # package: com.pulseweaver.heartbeat
adb shell monkey -p com.pulseweaver.heartbeat -c android.intent.category.LAUNCHER 1
```

## Driving the Compose UI from adb

Compose `testTag`s are **not** exposed to `uiautomator` (the app does not set
`testTagsAsResourceId`), so target nodes by visible text or by pixel bounds
read from a dump — not by tag.

```bash
adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml   # find bounds="[x1,y1][x2,y2]"
adb shell input tap <cx> <cy>                                               # tap a node's centre
adb exec-out screencap -p > shot.png                                        # screenshot to inspect
```

- **Text entry:** focus the field (`input tap`), then `adb shell input text 'https://host.example.org'`. URLs and API keys survive; avoid shell-special characters (`& | ; < > ( ) " '`).
- **Clear a field:** `adb shell input keyevent 123` (MOVE_END), then repeat `adb shell input keyevent 67` (DEL) for the field length.

## Battery-optimization exemption

```bash
adb shell dumpsys deviceidle whitelist | grep pulseweaver   # absent before grant
```

Tapping the in-app "Allow background activity" button fires
`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, which opens the system dialog
`com.android.settings.fuelgauge.RequestIgnoreBatteryOptimizations` (confirm
with `adb shell dumpsys window | grep mCurrentFocus`). Tap **Allow**, then:

```bash
adb shell dumpsys deviceidle whitelist | grep pulseweaver   # -> user,com.pulseweaver.heartbeat,<uid>
```

The in-app card disappears on its own — the screen polls `isExempt()` until
granted.

## Forcing Doze

```bash
adb shell dumpsys deviceidle force-idle      # "Now forced in to deep idle mode"
adb shell dumpsys deviceidle get deep        # IDLE
adb shell dumpsys jobscheduler | grep -A12 HeartbeatWorker
```

For a whitelisted app the job prints
`exempted ... [RUN_ANY_IN_BACKGROUND allowed] RUNNABLE WHITELISTED` — direct
proof the battery exemption keeps periodic work eligible during Doze (without
it, the job is deferred to maintenance windows). Exit with
`adb shell dumpsys deviceidle unforce`.

> **Gotcha (Android 14+):** WorkManager's jobs are namespaced
> (`androidx.work.systemjobscheduler:u0aNNN/<id>`). `adb shell cmd jobscheduler
> run -f <pkg> <id>` cannot address a namespaced job and prints "Could not find
> job". To force an actual background send, use the network-change path below
> instead of triggering the periodic job directly.

## Triggering the network-change heartbeat

The emulator's default link is CELLULAR over NAT, so `svc wifi/data` and
`cmd connectivity airplane-mode` frequently do **not** drop it. Toggle the
radio through the emulator console instead:

```bash
adb emu gsm data off ; sleep 5 ; adb emu gsm data on
```

Restoring connectivity fires the registered `ConnectivityManager` callback,
which enqueues a one-time worker recorded with `trigger=network_change`.

## Reading persisted state (debug build)

Debug builds permit `run-as`, so DataStore Preferences files can be read
directly — useful to confirm a *background worker* (not the open UI) wrote a
result:

```bash
adb shell run-as com.pulseweaver.heartbeat cat files/datastore/heartbeat_result.preferences_pb | strings
adb shell run-as com.pulseweaver.heartbeat cat files/datastore/heartbeat_config.preferences_pb  | strings
```

A `trigger: network_change` in the result store proves the background path
ran; if the open screen then updates to match, `ResultStore.observe()` is
wired correctly (a worker write reaching the foreground UI).

## Server round-trip gotcha (TLS / SNI)

If heartbeats fail on HTTPS with `TLSV1_ALERT_INTERNAL_ERROR` from *every*
client — including the device's BoringSSL — suspect a wildcard-certificate
SNI mismatch, not the app. A `*.example.org` certificate matches
`host.example.org` but **not** `www.host.example.org`: wildcards match a
single label only. Diagnose from any machine:

```bash
echo | openssl s_client -connect <ip>:443 -servername <host> 2>/dev/null | openssl x509 -noout -subject
```

A subject of `CN=*.example.org` with a two-label-deep host explains the
handshake abort; drop the extra label from the configured URL.

## What this cannot cover

App-Standby RARE/RESTRICTED buckets (apps unused for days or weeks) and
OEM-specific background killers are inherently multi-day and not reproducible
live on an emulator or device in one session. `force-idle` simulates Doze, not
long-term bucket demotion.

---
**Verified against:** Android emulator API 37, debug APK, full validation run
**Applies to:** Android background-reliability work (WorkManager, Doze, battery optimization, network callbacks, DataStore)
**Known gaps:** App-Standby buckets and OEM killers not reproducible in a single session
**Last verified:** 2026-06-10
