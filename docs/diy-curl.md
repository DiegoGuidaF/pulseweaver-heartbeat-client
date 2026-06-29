# ⚙️ DIY — curl

The whole client is one HTTP request. If a device can run `curl` (or any tool that sends an HTTP
POST) on a schedule, it can heartbeat — **any OS works**: Linux with cron or a systemd timer, macOS
with launchd, Windows with Task Scheduler. The only requirements are `curl` and a scheduler. Best
for headless boxes where you want full control and zero install.

This client has no pairing flow, so create the device's API key on the server first
(see [Prerequisites](../README.md#-prerequisites)).

## The request

```bash
curl -sf -X POST https://pw-device.example.com/api/v1/heartbeat \
     -H "X-API-Key: wdk_..."
```

- Point it at an endpoint that **bypasses the forward-auth gate** — a device on a new network has
  no registered IP yet, so a gated endpoint would reject the heartbeat. The server guide covers the
  [dedicated device domain](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#the-endpoints-must-be-reachable-without-the-gate).
- `-f` makes `curl` exit non-zero on a 401/404/429, so your scheduler can notice a failing heartbeat
  instead of silently reporting success.
- **Don't inline the key in a real unit file.** The server guide shows systemd `LoadCredential` and
  the macOS Keychain.

## Scheduling it

Run the request on a timer with whatever your OS already has. The PulseWeaver server guide carries
the maintained, copy-pasteable recipes so they live in one place:

- **Linux** — [systemd timer](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#linux--systemd-timer)
- **macOS** — [launchd agent](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#macos--launchd-agent)
- **Windows** — Task Scheduler running the same `curl` command on an interval (no recipe yet; the request above is all it runs)
- **cron** — a line that runs the request on your chosen interval

## Verify it's working

Don't trust the timer alone — confirm the **server** sees the heartbeat: open the device in the
PulseWeaver dashboard and check that its **address history** gains a fresh entry (updated last-seen
time and IP) each interval. See
[Verifying a device is connected](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#verifying-a-device-is-connected).

## How often, and the server-side lease

Whether the device stays allowed depends on the server's **address lease** (the per-device TTL that
retires an address when heartbeats stop) relative to your interval — keep the lease comfortably
longer than the interval. A box on a fixed address can use a long lease, or
[register its IP manually](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#manual--static-ip-devices)
and skip heartbeating entirely. The full rule:
[Recommended settings for roaming devices](https://github.com/DiegoGuidaF/PulseWeaver/blob/main/docs/Connecting-Devices.md#recommended-settings-for-roaming-devices).
