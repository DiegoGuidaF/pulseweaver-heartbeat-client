# Kotlin App — Full Documentation

The Kotlin Multiplatform app is the full-featured heartbeat client for phones and desktops. It runs in the background,
handles network changes, and requires no manual intervention after setup.

## Features

- **Background heartbeats** — configurable interval, runs without user interaction
- **Network-aware** — detects connectivity changes and sends a heartbeat immediately when back online
- **Biometric lock** (Android) — optionally require fingerprint/face to view or change settings, with a 60-second grace
  period
- **System tray** (Desktop) — lives in the tray with a state-aware icon; no window needed after setup
- **Light / Dark / Auto theme** — follows your system preference or pick manually
- **Minimal permissions, no telemetry** — no accounts, and the only network destination is the PulseWeaver server you configure (details in [Permissions, privacy & key storage](#permissions-privacy--key-storage))

> **Tip for mobile / dynamic IP devices**
>
> Enable two per-device rules on the PulseWeaver server so roaming stays clean:
> * **address lease** — a TTL that must sit above your worst-case heartbeat gap. A ~1 hour lease is a good default for
>   phones and laptops; under Android Doze the effective gap can reach ~30 minutes, so don't set the lease shorter than
>   that or it will flap.
> * **max active addresses** — cap it at 2. Roaming expires old IPs via the lease, but during travel (e.g. a moving car)
>   the IP can change faster, and keeping 2 avoids dropping mid-switch.
>
> Full reasoning: [Recommended settings for roaming devices](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#recommended-settings-for-roaming-devices).

> **Android: turn off battery optimization**
>
> Android's Doze / App Standby pauses background apps and can defer the heartbeat by hours, letting the device's
> access expire. On first run the app shows a reliability prompt — tap **Open settings**, find PulseWeaver in the
> battery-optimization list, and turn optimization **off**. This is required for the background schedule to run on
> time; without it, a device that isn't opened regularly will keep losing access. Admins: this is the first thing to
> check when a user reports their access dropping.


## Permissions, privacy & key storage

**Android permissions.** The app requests only what the heartbeat needs:

| Permission | Why |
|---|---|
| `INTERNET` | Send the heartbeat POST |
| `ACCESS_NETWORK_STATE` | Detect connectivity changes and re-heartbeat immediately when back online |
| `RECEIVE_BOOT_COMPLETED` | Resume the schedule after a reboot |
| `USE_BIOMETRIC` | The optional fingerprint/face lock — only used if you enable it |

No location, contacts, storage, or background-location permissions are requested.

**No telemetry.** The app talks to exactly one network destination: the server URL you configure (the heartbeat, plus a one-time pairing claim during setup). There is no analytics or crash-reporting backend. Because the app is open source ([AGPL-3.0](../LICENSE)), you can verify this yourself — see [Building from source](#building-from-source).

**Where your API key is stored.** The key lives in the platform's app-private configuration store; the app does not separately encrypt it:

- **Android** — Jetpack DataStore in app-private internal storage. On Android 10+ this is covered by the OS file-based encryption; on older versions it's protected by the app sandbox.
- **Desktop** — the Java user-preferences store under your OS user profile. This is **not** encrypted, so rely on your OS account and disk encryption to protect it.

The **biometric lock gates the app UI only** (viewing and changing settings) — it does **not** encrypt the stored key. Someone with filesystem or backup access is bounded by the platform storage protection above, not by the lock.

## Supported platforms

| Platform | Status     | Background scheduling       |
|----------|------------|-----------------------------|
| Android  | ✅ Ready    | WorkManager (survives doze) |
| Linux    | ✅ Ready    | JVM timer + system tray     |
| Windows  | ✅ Ready    | JVM timer + system tray     |
| macOS    | ✅ Ready    | JVM timer + system tray     |
| iOS      | 🚧 Planned | BGAppRefreshTask            |

The app is pre-1.0: the platforms marked ✅ are functional and tested, but expect rough edges and breaking changes between releases.

## Installing

Download the latest release for your platform
from [GitHub Releases](https://github.com/DiegoGuidaF/pulseweaver-heartbeat-client/releases):

| Platform | Artifact                   |
|----------|----------------------------|
| Android  | `.apk`                     |
| Linux    | `.deb` / `.rpm` / AppImage |
| Windows  | `.msi`                     |
| macOS    | `.dmg`                     |

On Android you'll need to allow installing the `.apk` from your browser or files app ("install unknown apps").

**Verifying your download.** Android release APKs are signed with the project's release key — inspect the certificate with `apksigner verify --print-certs <file>.apk`, and note that Android refuses to install an update signed by a different key. The desktop installers (`.deb` / `.dmg` / `.msi`) are not yet signed or notarized and no checksums are published; if you need stronger assurance, [build from source](#building-from-source).

## Device pairing

The recommended way to set up the app. The PulseWeaver administrator creates a pairing code and shares it with the
user (QR code or text string). The pairing code is single-use.

Steps the user needs to do:

1. Open the app — on first launch it goes straight to the **Setup screen**.
2. **Paste** the pairing code the administrator sent you.
3. Tap **Activate**. The app contacts the PulseWeaver server, registers the device, and auto-configures everything:
   server URL, API key, heartbeat interval, and security settings.
4. Done — the heartbeat starts immediately. The main screen shows the switch on and the time of the last heartbeat. If it doesn't activate, double-check the code with your admin — each pairing code is single-use.

If the administrator enabled **Lock all app settings**, all settings are read-only with the only exception of appearance
settings (theme) which remain editable. This is not meant as a security measure, but it is intended to simplify user
interaction (and errors) since users might have big thumbs :)

To re-pair (e.g. after a server migration), the user can press **Enter a setup code** — the same kind of pairing code
the administrator generates. The app will ask for confirmation before replacing the current configuration. This removes all app configuration and allows the user to
reconfigure it (either via code or manually).

> For how the server side of pairing works (creating pairings, proxy setup, the pairing code format), see the
> [PulseWeaver server documentation](https://github.com/DiegoGuidaF/PulseWeaver#device-pairing).

## Manual configuration

If you don't have a pairing code, tap **Configure manually** on the setup screen and fill in:

- **Server URL** — your PulseWeaver instance, on an endpoint that bypasses the forward-auth gate (e.g. `https://pw-device.example.com`)
- **API Key** — the `wdk_...` key from the PulseWeaver dashboard
- **Interval** — how often to send heartbeats (default: 60 seconds on desktop; on Android the minimum is 15 minutes)

Flip the switch to start. On desktop the app moves to the system tray.

## Building from source

Requires **JDK 17+** and the Android SDK (for Android builds).

```bash
# Run tests
./gradlew shared:jvmTest

# Desktop app (current OS)
./gradlew shared:run

# Android APK
./gradlew androidApp:assembleRelease

# Native desktop installers
./gradlew shared:packageDmg          # macOS
./gradlew shared:packageMsi          # Windows
./gradlew shared:packageDeb          # Linux .deb
./gradlew shared:packageRpm          # Linux .rpm
```

## Tech stack

| Layer    | Technology                                                             |
|----------|------------------------------------------------------------------------|
| Language | Kotlin 2.3.20                                                          |
| UI       | Compose Multiplatform 1.10.3                                           |
| HTTP     | Ktor 3.4.2                                                             |
| Build    | Gradle with version catalog, AGP 9.1.0                                 |
| Tests    | kotlin-test, kotlinx-coroutines-test, Ktor MockEngine, compose-ui-test |
