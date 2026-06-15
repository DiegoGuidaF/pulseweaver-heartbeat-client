# Client patterns

Conventions specific to the Heartbeat client that are hard to derive from any single
file. Each entry names the canonical examples so new code can copy the shape.

## Availability-gated platform capability

**Use when:** adding an optional device capability that only some targets support
(biometric lock, camera/QR scan, NFC, share sheet, …).

**Examples:** `platform/BiometricAuth.kt`, `platform/QrScanner.kt`.

A capability is an `expect object` in `commonMain/platform/` that exposes:

- `fun isAvailable(): Boolean` — cheap, synchronous, safe to call from a composable.
- the capability's `suspend` operation (e.g. `authenticate(...)`, `scan()`), returning a
  plain result (`Boolean`, `String?`) — never throwing for "unsupported".

Rules:

1. **The real implementation lives only in the `androidMain` actual.** `jvmMain` and
   `iosMain` actuals are no-op stubs: `isAvailable()` returns `false` and the operation
   returns a safe default (`false` / `null`). Returning `false` rather than throwing
   `NotImplementedError` is what lets the UI gate cleanly — don't skip it.
2. **The UI gates on availability, never on platform.** Read it once
   (`val canX = remember { Cap.isAvailable() }`) and conditionally render the affordance.
   No `if (Platform.isAndroid)` branching leaks into common UI; unsupported targets
   degrade by hiding the control.

Adding a new capability is then a fill-in-the-template exercise: one `expect`, one Android
actual, two stubs, one `isAvailable()`-gated control.

### Reaching Android Context from `:shared`

Android actuals don't take a `Context` through common signatures. They pull it from the
process-scoped holders in `androidMain`:

- `ApplicationContextHolder.context` — app context, initialized in `PulseWeaverApp.onCreate`.
- `ActivityHolder.get()` — weak ref to the resumed `FragmentActivity` (set in
  `MainActivity.onResume`, cleared in `onPause`); needed by APIs that must show UI from an
  activity. Prefer it when present, fall back to the app context.

### Note: GMS Code Scanner needs no `CAMERA` permission

`QrScanner` (Android) uses Google Play Services' code scanner
(`play-services-code-scanner`), which runs the camera out-of-process inside Play Services.
It deliberately requires **no `CAMERA` permission** in the manifest — do not add one. The
trade-off is a one-time on-demand module download and a dependency on Play Services being
present; `isAvailable()` only checks for a camera, so a scan can still fail on a
Play-Services-less device (it returns `null`, and the setup flow stays usable via paste).
