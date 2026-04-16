# UI Theming & Component Style

The app uses Material 3 (Compose Multiplatform) with a custom "Navy depth" color scheme. No third-party component library or Tailwind — all UI is standard M3 composables with custom colors.

## Color Scheme

Defined in `App.kt` as `IndigoLight` and `IndigoDark`:

| Role | Light | Dark |
|------|-------|------|
| Primary | `#5C7CFA` (indigo) | `#5C7CFA` (indigo) |
| Background | `#FFFFFF` (white) | `#0D0F1E` (deep navy) |
| Surface | `#FFFFFF` | `#161829` |
| SurfaceVariant | `#F4F4F6` | `#1E2035` |

## Semantic Accent Colors

Defined in `HeartbeatScreen.kt`:

| Color | Hex | Meaning |
|-------|-----|---------|
| `Amber` | `#FFA94D` | Liveness / pulse / active state |
| `StoppedGrey` | `#9E9E9E` | Disabled / stopped |
| `ErrorRed` | `#FA5252` | Error state |
| `WarningYellow` | `#FCC419` | Warning (e.g. rate limited) |

**Rule:** Amber = liveness/pulse, Indigo (primary) = action/structure.

## Theme Mode

`ThemeMode` enum: `AUTO`, `LIGHT`, `DARK`. Resolved in `App.kt`:
```kotlin
val useDark = HeartbeatUtils.shouldUseDarkTheme(themeMode, isSystemInDarkTheme())
```

## Card Pattern

All cards use the same styling:
```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
)
```

## PulseWeaver Branding

The app title in the TopAppBar uses a styled `AnnotatedString`:
- "Pulse" in Amber bold
- "Weaver" in normal weight
- Small amber dot (`8.dp` circle) before the text

## Typography Usage

| Style | Used for |
|-------|----------|
| `titleLarge` | App title |
| `titleSmall` + `SemiBold` | Card headers |
| `headlineSmall` + `SemiBold` | StatusHero state label |
| `bodyLarge` + `Medium` | Toggle labels (e.g. "Heartbeat") |
| `bodyMedium` | Content text, result messages |
| `bodySmall` | Timestamps, hints, supplementary |
| `labelMedium` | Sub-headers (e.g. "Check every", "Theme") |
| `labelSmall` | Status hints, "Saved ✓" |

---
**Verified against:** `App.kt`, `HeartbeatScreen.kt`
**Applies to:** any new UI composable or card
**Known gaps:** none
**Last verified:** 2026-04-16
