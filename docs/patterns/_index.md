# Heartbeat Client Pattern Index

> Before implementing a feature, scan this index and read every pattern that applies.
> After implementing, check the [self-improvement protocol](../../project/workflow/WORKFLOW.md#pattern-maintenance).

| Pattern | File | Use when | Avoid when | Refs |
|---------|------|----------|------------|------|
| Expect/actual platform abstraction | `expect-actual.md` | Adding platform-specific behavior (scheduling, biometrics, storage) | Pure business logic that works in commonMain | `platform/BackgroundScheduler.kt`, `config/ConfigStore.kt` |
| Ktor HTTP client | `ktor-http-client.md` | Making HTTP requests, modifying heartbeat logic | UI code or platform scheduling | `service/HeartbeatClient.kt` |
| Testing | `testing.md` | Writing unit tests, HTTP tests, or UI tests | — | `commonTest/`, `jvmTest/` |
