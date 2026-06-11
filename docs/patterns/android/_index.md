# Android Pattern Index

> Android-specific patterns for the heartbeat client. Read alongside the
> top-level [pattern index](../_index.md); these cover behaviour that only
> exists on the Android target (WorkManager scheduling, Doze, on-device validation).

| Pattern | File | Use when | Avoid when | Refs |
|---------|------|----------|------------|------|
| On-device / emulator validation | `emulator-validation.md` | Manually validating Android background or reliability behaviour (Doze, battery optimization, WorkManager, network callbacks) via adb | Pure logic or desktop UI — use [`../testing.md`](../testing.md) | `androidApp/`, `shared/src/androidMain/` |
