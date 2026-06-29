<picture>
  <source media="(prefers-color-scheme: dark)" srcset=".github/assets/wordmark-dark.svg">
  <img src=".github/assets/wordmark-light.svg" alt="PulseWeaver" height="48">
</picture>

# PulseWeaver Heartbeat Client

Keep your devices reachable on your [PulseWeaver](https://github.com/DiegoGuidaF/PulseWeaver) server as their network address changes — automatically.

> **Got a pairing code from your admin?** You only need the app. Jump straight to
> **[set up PulseWeaver Companion in 4 steps](docs/app.md#device-pairing)** — you can
> skip the rest of this page. Everything below is for self-hosters choosing how to run a client.

A device only stays allowed through PulseWeaver while it keeps proving its current address is live — by sending a periodic **heartbeat** (`POST /api/v1/heartbeat`). Stop heartbeating and the device drops off, so this one job has to keep running reliably on whatever the device is. That's why there isn't a single client: this repository ships **three** ways to send a heartbeat so there's a good fit for every device, from a phone to a headless box. Pick one:

- **[PulseWeaver Companion](docs/app.md)** — the multiplatform app for phones and desktops. Runs in the background, re-announces a new address the moment you change networks, and pairs from a single code. **The recommended client, and the only one a non-technical user should need.**
- **[Docker](docs/docker.md)** — a tiny container that heartbeats on a timer. For a server or NAS already running Docker.
- **[DIY curl](docs/diy-curl.md)** — one `curl` driven by your own scheduler (cron, systemd, Task Scheduler…). For headless boxes where you want full control and zero install.

> New to the model? The server's [Connecting devices](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md) guide is the end-to-end walkthrough — how a device gets its key, how to expose the endpoint, and the settings that make roaming behave. The client docs here link back to it rather than restating it.

## Which client?

**Most people want the app.** It's the recommended setup and the only one that re-announces a
new address the instant you change networks. Reach for Docker or curl on headless servers.

|                  | [📱 PulseWeaver Companion](docs/app.md) ✅ recommended                       | [🐳 Docker](docs/docker.md)                              | [⚙️ DIY — curl](docs/diy-curl.md)        |
|------------------|------------------------------------------------------------------------------|----------------------------------------------------------|-------------------------------------------|
| **Best for**     | Phones, and any laptop/desktop that changes networks. The pick for non-technical users. | Servers/NAS already running Docker (otherwise use curl)  | Any headless box, with full control       |
| **Platforms**    | Android, Linux, Windows, macOS                                               | Any Docker host                                          | Any OS with `curl` + a scheduler          |
| **Scheduling**   | OS-native (WorkManager, JVM timer)                                           | Internal loop on the interval (restart policy only recovers crashes/reboots) | Your scheduler (cron, systemd, launchd, Task Scheduler) |
| **Network-aware**| Yes — reconnects immediately on link change                                  | No¹                                                      | No¹                                       |
| **Requirements** | None to install (on Android, allow the battery-optimization exemption)       | Docker                                                   | `curl` and a scheduler                    |

¹ Not network-aware: after the device's address changes, the old one stays announced until
the next scheduled heartbeat, so access can lapse until then. Fine for a server on a fixed
address — pick the app for anything that roams.

---

## 🔑 Prerequisites

Every client authenticates with a device **API key**. There are two ways to get one onto a device.

**Recommended — device pairing (Companion app only):** your PulseWeaver admin creates a pairing
code (Devices → the device → create a pairing) and shares it with you. Paste it into PulseWeaver
Companion and the device and its API key are created automatically when the code is claimed — no
manual key handling. See [Companion → Device pairing](docs/app.md#device-pairing).

**Manual setup (any client):** in the PulseWeaver dashboard, create the device
(**Devices → New device**), then generate its **API key** (starts with `wdk_…`) from
**Devices → the device → Settings**. Each device needs its own key. Docker and curl always use
this path.

---

## 🩺 Pairing error codes

Pairing is a PulseWeaver Companion feature, so its failure codes are specific to
the app (Docker and curl don't pair). If activation fails, the app shows a
plain-language message and a short diagnostic code (e.g. `PWC-PAIR-EXPIRED`) you
can read back to your administrator — it points to the exact cause regardless of
the underlying HTTP status. The full list and what to do for each is in
**[Companion → Pairing error codes](docs/app.md#pairing-error-codes)**.

## 🔌 API reference

All three clients call the same endpoint. No server-side changes are needed beyond generating an API key.

```
POST {server_url}/api/v1/heartbeat
Header: X-API-Key: wdk_...
Body:   (empty — the server reads the caller's IP)

200 — IP updated
201 — New IP registered
401 — Invalid API key
404 — Device not found
429 — Rate limited
```

The endpoint must be reachable **without** passing through the forward-auth gate — a device on a
new network has no registered IP yet, so a gated endpoint would reject its heartbeat. The server
guide covers the [dedicated device domain](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#the-endpoints-must-be-reachable-without-the-gate) pattern.

## Contributing

Contributions are welcome. The project follows a spec-driven workflow — features are specified before implementation starts.

## Security

Found a vulnerability? See [SECURITY.md](SECURITY.md) for how to report it privately. The clients are open source, so their network behavior, permissions, and key handling are auditable — see [Permissions, privacy & key storage](docs/app.md#permissions-privacy--key-storage).

## License

[AGPL-3.0](LICENSE)
