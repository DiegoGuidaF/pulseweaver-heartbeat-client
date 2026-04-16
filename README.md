# PulseWeaver Heartbeat Client

Keep your devices' current address enabled on your [PulseWeaver](https://github.com/DiegoGuidaF/PulseWeaver) server — automatically.

PulseWeaver is a self-hosted, IP-based dynamic firewall. For an IP to stay allowed, a device must send periodic heartbeats to prove it's still active. This repository provides three ways to do that.

## Which setup?

|                  | [📱 Multiplatform app](#-kotlin-app)                                       | [🐳 Docker](#-docker)                      | [⚙️ DIY — curl](#️-diy--curl)    |
|------------------|----------------------------------------------------------------------------|--------------------------------------------|-----------------------------------|
| **Best for**     | Phones and desktops that need a GUI. Providing access for non-techie users. | Servers/NAS already running Docker         | Any Linux or macOS, zero deps     |
| **Platforms**    | Android, Linux, Windows, macOS                                             | Any Docker host                            | Linux, macOS                      |
| **Scheduling**   | OS-native (WorkManager, JVM timer)                                         | Container restart policy                   | systemd timer / launchd           |
| **Network-aware**| Yes — reconnects immediately on link change                                | No                                         | No                                |
| **Requirements** | None (self-contained installer)                                            | Docker                                     | curl                              |

---

## 🔑 Prerequisites

**Manual setup:** All three setups need an API key. In the PulseWeaver dashboard, create a **device key** (starts with `wdk_...`). Each device needs its own key.

**Recommended — Device provisioning:** Your PulseWeaver admin creates a registration code (Devices → Provisioning) and shares it with you. Paste it in the Kotlin app — the device and API key are created automatically. See [app docs → Device provisioning](docs/app.md#device-provisioning).

---

## 📱 Kotlin app

The full-featured client for phones and desktops. Runs in the background, handles network changes, lives in the system tray on desktop.

Download the latest release from [GitHub Releases](https://github.com/DiegoGuidaF/pulseweaver-heartbeat-client/releases), install it, and fill in your server URL, API key, and interval. Flip the switch to start.

→ [Full documentation — features, screenshots, building from source](docs/app.md)

---

## 🐳 Docker

Lightweight option for servers or NAS devices already running Docker. No JVM, no GUI — just a container that curls your server on a configurable interval.

```bash
# 1. Copy docker/docker-compose.yml to your host and fill in the env vars
# 2. Start
docker compose up -d
```

Or with `docker run`:

```bash
docker run -d \
  --restart unless-stopped \
  -e HEARTBEAT_URL=https://pw.example.com \
  -e HEARTBEAT_API_KEY=wdk_... \
  -e HEARTBEAT_INTERVAL_SECONDS=60 \
  ghcr.io/diegoguidaf/pulseweaver-heartbeat-client:latest
```

| Variable                     | Required | Description                                                            |
|------------------------------|----------|------------------------------------------------------------------------|
| `HEARTBEAT_URL`              | Yes      | Your PulseWeaver server URL (e.g. `https://pw.example.com`)            |
| `HEARTBEAT_API_KEY`          | Yes      | Device API key from the dashboard (starts with `wdk_...`)              |
| `HEARTBEAT_INTERVAL_SECONDS` | Yes      | How often to send a heartbeat, in seconds — must be a positive integer |
| `HEARTBEAT_ENDPOINT`         | No       | Heartbeat path, defaults to `/api/v1/heartbeat`                        |

The container exits with an error on startup if any required variable is missing or invalid.

---

## ⚙️ DIY — curl

No dependencies beyond `curl`. Useful for headless Linux or macOS machines where you want full control.

```bash
curl -s -X POST https://pw.example.com/api/v1/heartbeat \
     -H "X-API-Key: wdk_..."
```

→ [Full guide — systemd timer, launchd agent, and more](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Lightweight-Heartbeat-Clients.md)

---

## 🔌 API reference

All three setups call the same endpoint. No server-side changes are needed beyond generating a device key.

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

Contributions are welcome. The project follows a spec-driven workflow — feature specs live in the [planning workspace](https://github.com/DiegoGuidaF/PulseWeaver) and are written before implementation starts.

## License

[AGPL-3.0](LICENSE)
