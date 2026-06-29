# 🐳 Docker client

A lightweight container that sends the heartbeat on a timer — no JVM, no GUI, just a `curl` loop.
Best for a server or NAS that already runs Docker. (No Docker on the box? Use the
[DIY curl](diy-curl.md) setup instead.)

This client has no pairing flow, so you create the device's API key on the server first.

**Before you start:** create the device and its API key on the server (see
[Prerequisites](../README.md#-prerequisites)) — the container needs the key to authenticate.

## Run it

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
| `HEARTBEAT_URL`              | Yes      | Your PulseWeaver server base URL — must reach the heartbeat endpoint **without** passing through the forward-auth gate (e.g. `https://pw-device.example.com`). Base URL only; the path comes from `HEARTBEAT_ENDPOINT`. |
| `HEARTBEAT_API_KEY`          | Yes      | Device API key from the dashboard (starts with `wdk_...`)                            |
| `HEARTBEAT_INTERVAL_SECONDS` | Yes      | How often to send a heartbeat, in seconds — must be a positive integer               |
| `HEARTBEAT_ENDPOINT`         | No       | Heartbeat path, defaults to `/api/v1/heartbeat`                                      |

The container exits with an error on startup if any required variable is missing or invalid. A key
the server rejects at runtime (401/404) is logged and retried on the next interval — the container
keeps running, so a clean startup doesn't by itself mean heartbeats are landing. Always confirm with
the verification step below.

## Verify it's working

Two checks, from the container outward to the server — do both:

1. **Container side:** `docker logs <container>` should show a heartbeat succeeding (HTTP 200/201) once per interval.
2. **Server side (authoritative):** open the device in the PulseWeaver dashboard and confirm a fresh
   entry in its **address history** — its last-seen time and IP should update each interval. This is
   the real proof the server is registering the heartbeat. See
   [Verifying a device is connected](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#verifying-a-device-is-connected).

## How often, and the server-side lease

`HEARTBEAT_INTERVAL_SECONDS` only controls how often the container pings. Whether the device stays
allowed also depends on the server's **address lease** (the per-device TTL that retires an address
when heartbeats stop). Keep the lease comfortably longer than the interval. A server on a fixed
address can use a long lease, or none. The full rule lives in the server guide:
[Recommended settings for roaming devices](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#recommended-settings-for-roaming-devices).
