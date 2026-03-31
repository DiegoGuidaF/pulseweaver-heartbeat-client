# PulseWeaver Heartbeat Client

Keep your devices authorized on your [PulseWeaver](https://github.com/DiegoGuidaF/PulseWeaver) server — automatically.

PulseWeaver is a self-hosted, IP-based dynamic firewall. It sits as an authorization middleware in front of your reverse proxy (e.g. Caddy) and decides whether to allow or deny requests based on the caller's IP. For an IP to be allowed, a device must **send periodic heartbeats** to prove it's still active.

This app does exactly that. Install it on a phone, laptop, or desktop and it keeps your IP registered in the background — no browser tabs, no Tasker hacks, no manual steps.

## Platforms

| Platform | Status | Background scheduling                                                                                 |
|----------|--------|-------------------------------------------------------------------------------------------------------|
| Android  | ✅ Ready | WorkManager (survives doze)                                                                           |
| Linux    | ✅ Ready | JVM timer + system tray — or [systemd timer](#linux-headless-alternative-systemd) for headless setups |
| Windows  | ✅ Ready | JVM timer + system tray                                                                               |
| macOS    | ✅ Ready | JVM timer + system tray — or [launchd agent](#macos-headless-alternative-launchd) for headless setups |
| iOS      | 🚧 Planned | BGAppRefreshTask                                                                                      |

Single codebase — built with Kotlin Multiplatform and Compose Multiplatform.

## Features

- **Background heartbeats** — configurable interval, runs without user interaction
- **Network-aware** — detects connectivity changes and sends a heartbeat immediately when back online
- **Biometric lock** (Android) — optionally require fingerprint/face to view or change settings, with a 60-second grace period
- **System tray** (Desktop) — lives in the tray with a state-aware icon; no window needed after setup
- **Light / Dark / Auto theme** — follows your system preference or pick manually
- **Minimal permissions** — no accounts, no telemetry, no internet access beyond your own server

## Screenshots

<!-- Replace these with actual screenshots -->

| Android | Desktop |
|---------|---------|
| ![Android screenshot](docs/screenshot-android.png) | ![Desktop screenshot](docs/screenshot-desktop.png) |

## Getting started

### 1. Generate an API key

In the PulseWeaver dashboard, create a **device key** (starts with `wdk_...`). Each device needs its own key.

### 2. Install the app

Download the latest release for your platform from [GitHub Releases](https://github.com/DiegoGuidaF/pulseweaver-heartbeat-client/releases):

| Platform | Artifact |
|----------|----------|
| Android  | `.apk` |
| Linux    | `.deb` / `.rpm` / AppImage |
| Windows  | `.msi` |
| macOS    | `.dmg` |

### 3. Configure

Open the app and fill in:

- **Server URL** — your PulseWeaver instance (e.g. `https://pw.example.com`)
- **API Key** — the `wdk_...` key from step 1
- **Interval** — how often to send heartbeats (default: 60 seconds)

Flip the switch to start. On desktop the app moves to the system tray.

> **Tip for mobile / dynamic IP devices:** Enable the **address lease** on the PulseWeaver server for that device and set the TTL slightly above the heartbeat interval (e.g. 20 min TTL for a 15 min interval). This way, if a heartbeat is delayed by a network switch or doze cycle, the IP doesn't expire prematurely.

## Linux headless alternative (systemd)

If you're running on a headless Linux box (server, Raspberry Pi, VPS…) you don't need the full GUI app. A systemd **timer + oneshot service** using `curl` does the job with zero dependencies.

#### 1. Create the service

`/etc/systemd/system/pulseweaver-heartbeat.service`

```ini
[Unit]
Description=PulseWeaver heartbeat ping
Wants=network-online.target
After=network-online.target

[Service]
Type=oneshot
ExecStart=curl -sf -X POST -H "X-API-Key: wdk_YOUR_KEY_HERE" https://pw.example.com/api/v1/heartbeat
```

#### 2. Create the timer

`/etc/systemd/system/pulseweaver-heartbeat.timer`

```ini
[Unit]
Description=Send PulseWeaver heartbeat every 5 minutes

[Timer]
OnBootSec=30s
OnUnitActiveSec=5min
AccuracySec=30s

[Install]
WantedBy=timers.target
```

#### 3. Enable and start

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now pulseweaver-heartbeat.timer

# Verify it's scheduled
systemctl list-timers pulseweaver-heartbeat.timer

# Test a manual run
sudo systemctl start pulseweaver-heartbeat.service
journalctl -u pulseweaver-heartbeat.service -n 5
```

> **Security note:** The example above inlines the API key in the unit file for simplicity. For production use, consider systemd's built-in credential management (`LoadCredential=` / `LoadCredentialEncrypted=`, available since systemd 247) which injects secrets at runtime via `$CREDENTIALS_DIRECTORY` — keeping them out of unit files, environment variables, and process listings. See the [systemd credentials docs](https://systemd.io/CREDENTIALS/) for details.

## macOS headless alternative (launchd)

On a Mac mini server or any headless macOS machine, a **launchd agent** with `curl` replaces the full app.

#### 1. Create the plist

`~/Library/LaunchAgents/com.pulseweaver.heartbeat.plist`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.pulseweaver.heartbeat</string>

    <key>ProgramArguments</key>
    <array>
        <string>curl</string>
        <string>-sf</string>
        <string>-X</string>
        <string>POST</string>
        <string>-H</string>
        <string>X-API-Key: wdk_YOUR_KEY_HERE</string>
        <string>https://pw.example.com/api/v1/heartbeat</string>
    </array>

    <key>StartInterval</key>
    <integer>300</integer>

    <key>RunAtLoad</key>
    <true/>

    <key>StandardErrorPath</key>
    <string>/tmp/pulseweaver-heartbeat.err</string>
</dict>
</plist>
```

#### 2. Load and start

```bash
launchctl load ~/Library/LaunchAgents/com.pulseweaver.heartbeat.plist

# Verify it's loaded
launchctl list | grep pulseweaver

# Test a manual run
launchctl start com.pulseweaver.heartbeat
cat /tmp/pulseweaver-heartbeat.err
```

To stop and unload: `launchctl unload ~/Library/LaunchAgents/com.pulseweaver.heartbeat.plist`

> **Security note:** The API key is inlined in the plist for simplicity. For better secret handling, store the key in the macOS Keychain and retrieve it with `security find-generic-password` in a wrapper script, or use an environment variable sourced from a file with restricted permissions (`chmod 600`).

## API

The client calls a single endpoint. No server-side changes are needed.

```
POST {server_url}/api/v1/heartbeat
Header: X-API-Key: wdk_...
Body:   (empty — server reads IP from the request)

200 — IP updated
201 — New IP registered
401 — Invalid API key
404 — Device not found
429 — Rate limited
```

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

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3.20 |
| UI | Compose Multiplatform 1.10.3 |
| HTTP | Ktor 3.4.2 |
| Build | Gradle with version catalog, AGP 9.1.0 |
| Tests | kotlin-test, kotlinx-coroutines-test, Ktor MockEngine, compose-ui-test |

## Contributing

Contributions are welcome. The project follows a spec-driven workflow — feature specs live in the [planning workspace](https://github.com/DiegoGuidaF/PulseWeaver) and are written before implementation starts.

## License

[AGPL-3.0](LICENSE)
