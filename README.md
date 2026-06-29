<picture>
  <source media="(prefers-color-scheme: dark)" srcset=".github/assets/wordmark-dark.svg">
  <img src=".github/assets/wordmark-light.svg" alt="PulseWeaver" height="48">
</picture>

# PulseWeaver Heartbeat Client

Keep your devices' current address enabled on your [PulseWeaver](https://github.com/DiegoGuidaF/PulseWeaver) server — automatically.

> **Got a pairing code from your admin?** You only need the app. Jump straight to
> **[Set up the app in 4 steps](docs/app.md#device-pairing)** — no need to read the
> rest of this page. Everything below is for self-hosters choosing how to run the client.

PulseWeaver keeps a device's access working as its network address changes. It's a self-hosted forward-auth gate for reverse proxies — per-user, IP-based access control over which devices reach which services — and for a device's IP to stay allowed it must send periodic heartbeats to prove it's still active. This repository provides three ways to do that.

## Which setup?

**Most people want the app** — it's the recommended setup and the only one that
re-announces a new IP the moment you change networks. Reach for Docker or curl on
headless servers.

|                  | [📱 Multiplatform app](#-kotlin-app) ✅ recommended                          | [🐳 Docker](#-docker)                                   | [⚙️ DIY — curl](#️-diy--curl)    |
|------------------|----------------------------------------------------------------------------|---------------------------------------------------------|-----------------------------------|
| **Best for**     | Phones, and any laptop/desktop that changes networks. The pick for non-techie users. | Servers/NAS already running Docker (otherwise use curl) | Headless Linux/macOS, zero deps   |
| **Platforms**    | Android, Linux, Windows, macOS                                             | Any Docker host                                          | Linux, macOS                      |
| **Scheduling**   | OS-native (WorkManager, JVM timer)                                         | Internal loop on the interval (restart policy only recovers crashes/reboots) | systemd timer / launchd |
| **Network-aware**| Yes — reconnects immediately on link change                                | No¹                                                     | No¹                               |
| **Requirements** | None to install (on Android, allow the battery-optimization exemption)     | Docker                                                   | curl                              |

¹ Not network-aware: after the device's IP changes, the old address stays
announced until the next scheduled heartbeat, so access can lapse until then.
Fine for a server on a fixed IP — pick the app for anything that roams.

---

## 🔑 Prerequisites

**Manual setup:** All three setups need an API key. In the PulseWeaver dashboard, create the device (**Devices → New device**), then generate its **API key** (starts with `wdk_...`) from **Devices → the device → Settings**. Each device needs its own key.

**Recommended — Device pairing:** Your PulseWeaver admin creates a pairing code (Devices → the device → create a pairing) and shares it with you. Paste it in the Kotlin app — the device and API key are created automatically. See [app docs → Device pairing](docs/app.md#device-pairing).

---

## 📱 Kotlin app

The full-featured client for phones and desktops. Runs in the background, handles network changes, lives in the system tray on desktop.

Download the latest release from [GitHub Releases](https://github.com/DiegoGuidaF/pulseweaver-heartbeat-client/releases) and install it (on Android you'll need to allow installing the `.apk` from your browser or files app).

- **If your admin gave you a pairing code** (the recommended path): open the app, paste the code, and tap **Activate** — the server URL, API key, and interval are filled in for you. Full steps: [Device pairing](docs/app.md#device-pairing).
- **No pairing code?** Fill in your server URL, API key, and interval manually, then flip the switch to start.

→ [Full documentation — features, permissions, building from source](docs/app.md)

### Interval & address lease

The client sends a heartbeat at least every chosen interval. The server side of staying authorized is the **address lease** — a per-device TTL that auto-disables an address if no heartbeat refreshes it in time. Match the two so a missed heartbeat or two doesn't drop the device. This applies to every setup (app, Docker, curl):

- **Set the lease longer than your worst-case interval.** A good default for phones and laptops is a **~1 hour lease with max 2 active addresses**; headless servers on a fixed network can use a much shorter lease, or none. The reasoning and the roaming trade-offs are in the server guide: [Recommended settings for roaming devices](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#recommended-settings-for-roaming-devices).
- **Android only — turn off battery optimization for PulseWeaver.** Android batches background work, so the actual delivery can slip — especially on a phone that hasn't been opened in a while. When the app prompts you, tap **Open settings**, find PulseWeaver in Android's battery-optimization list, and turn optimization off. This exempts the app from Doze / App Standby. **If you skip this, heartbeats can be delayed by hours and the device's access will keep expiring** — it's the most common reason a mobile device drops off. Doze can stretch the effective gap to ~30 minutes, so keep the lease above that.

For admins: if a user's device addresses keep expiring, the first thing to check is whether battery optimization is still enabled for the app on their phone — have them turn it off as described above. Persistent expiry after that, together with the max-active-addresses rule, is your signal that something is actually wrong rather than a normal Doze delay.

---

## 🐳 Docker

Lightweight option for servers or NAS devices already running Docker. No JVM, no GUI — just a container that curls your server on a configurable interval.

**Before you start:** create the device's API key on the server first (see [Prerequisites](#-prerequisites)) — the container needs it to authenticate.

```bash
# 1. Copy docker/docker-compose.yml to your host and fill in the env vars
# 2. Start
docker compose up -d
```

Or with `docker run`:

```bash
docker run -d \
  --restart unless-stopped \
  -e HEARTBEAT_URL=https://pw-device.example.com \
  -e HEARTBEAT_API_KEY=wdk_... \
  -e HEARTBEAT_INTERVAL_SECONDS=60 \
  ghcr.io/diegoguidaf/pulseweaver-heartbeat-client:latest
```

| Variable                     | Required | Description                                                                          |
|------------------------------|----------|--------------------------------------------------------------------------------------|
| `HEARTBEAT_URL`              | Yes      | Your PulseWeaver server base URL — must reach the heartbeat endpoint without passing through the forward-auth gate (e.g. `https://pw-device.example.com`). Base URL only; the path comes from `HEARTBEAT_ENDPOINT`. |
| `HEARTBEAT_API_KEY`          | Yes      | Device API key from the dashboard (starts with `wdk_...`)                            |
| `HEARTBEAT_INTERVAL_SECONDS` | Yes      | How often to send a heartbeat, in seconds — must be a positive integer               |
| `HEARTBEAT_ENDPOINT`         | No       | Heartbeat path, defaults to `/api/v1/heartbeat`                                      |

The container exits with an error on startup if any required variable is missing or invalid. A key the server rejects at runtime (401/404) is logged and retried on the next interval — the container keeps running, so a clean startup doesn't by itself mean heartbeats are landing.

**Verify it's working:** `docker logs <container>` should show a heartbeat succeeding (HTTP 200/201) each interval, and the device's last-seen time and IP should update in the PulseWeaver dashboard.

---

## ⚙️ DIY — curl

No dependencies beyond `curl`. Useful for headless Linux or macOS machines where you want full control.

Point this at an endpoint that bypasses the forward-auth gate — a device on a new network has no registered IP yet, so a gated endpoint would reject the heartbeat. The [Full guide](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md) covers the dedicated device domain.

```bash
curl -sf -X POST https://pw-device.example.com/api/v1/heartbeat \
     -H "X-API-Key: wdk_..."
```

`-f` makes curl exit non-zero on a 401/404/429, so a scheduler can catch a failing heartbeat instead of silently reporting success. Don't inline the key in a real unit file — the [Full guide](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md) shows systemd `LoadCredential` and the macOS Keychain.

→ [Full guide — systemd timer, launchd agent, and more](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md)

---

## 🔌 API reference

All three setups call the same endpoint. No server-side changes are needed beyond generating an API key.

```
POST {server_url}/api/v1/heartbeat
Header: X-API-Key: wdk_...
Body:   (empty — server reads the caller's IP)

200 — IP updated
201 — New IP registered
401 — Invalid API key
404 — Device not found
429 — Rate limited
```

## Contributing

Contributions are welcome. The project follows a spec-driven workflow — features are specified before implementation starts.

## Security

Found a vulnerability? See [SECURITY.md](SECURITY.md) for how to report it privately. The app is open source, so its network behavior, permissions, and key handling are auditable — see [Permissions, privacy & key storage](docs/app.md#permissions-privacy--key-storage).

## License

[AGPL-3.0](LICENSE)
