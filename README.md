# PocketPanel

PocketPanel is a native Android utility app built with Kotlin and Jetpack
Compose (Material 3). A small floating handle sits on a screen edge; tapping
it opens a compact side panel with live system readings and a WireGuard VPN
control, without leaving the current app.


## Features

- **Floating side panel** — edge-mounted handle, draggable position,
  expand/collapse, and a persisted on/off setting.
- **Monitoring** — CPU usage, memory, battery level/temperature/health,
  GPU identity (renderer, vendor, OpenGL ES version), network transport,
  device-wide throughput and ping latency.
- **Overlay panel** — CPU %, battery temperature, memory % and ping in a
  2×2 grid shown only while monitoring is enabled, with the VPN control
  below it.
- **WireGuard VPN** — import a `.conf` file through the Storage Access
  Framework, connect/disconnect from the app or the overlay, and see real
  connection state and errors. Android's VPN consent dialog is requested
  before the first connection.
- **Customization** — System/Light/Dark themes, optional Material You
  dynamic colors, compact/normal handle size, left/right edge, and
  Standard/Focus home-screen layouts. Settings persist in DataStore.
- **Privacy** — local-first, no account, no telemetry, no cloud. VPN
  private keys are encrypted with an Android Keystore key before storage;
  backups are disabled.

No root or Shizuku is used. Unsupported readings are shown as
"Not available" rather than estimated or fabricated. See the
[known limitations](#known-limitations).

## Requirements

- JDK 17
- Android SDK with platform `android-37.1` (compileSdk 37 minor 1) and
  Build Tools `36.0.0`
- Android Studio/Gradle setup compatible with Android Gradle Plugin 9.5.2
- The committed Gradle wrapper (9.6.0); no separate Gradle install needed

`minSdk` is 26 (Android 8.0) and `targetSdk` is 36.

## Build

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Dependency versions are pinned in `gradle/libs.versions.toml`: AGP 9.5.2,
Kotlin 2.4.21, Compose BOM 2026.11.01, WireGuard tunnel 1.0.20260102 and
`desugar_jdk_libs` 2.1.5. Verification sources and compatibility decisions
are recorded in `docs/DECISIONS.md`.

## Test

```bash
./scripts/check-static.sh          # structure, resources, policy checks; no JDK needed
./gradlew :app:testDebugUnitTest    # JVM unit tests
./gradlew :app:lintDebug            # Android lint
```

Instrumented tests live under `app/src/androidTest`, but CI does not
provision an emulator, so they are not executed in CI.

## CI

`.github/workflows/verify-toolchain.yml` is manual-only: it is started
with `workflow_dispatch` when the project owner authorizes a run. It
builds the debug APK, runs lint and JVM tests, and publishes results to
the `ci-results` branch and workflow artifacts. Push, pull-request and
schedule triggers are intentionally absent.

## Permissions

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the floating handle over other apps |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the overlay alive with a user-visible notification |
| `POST_NOTIFICATIONS` | Show that service notification on Android 13+ |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Ping probe, network transport and VPN tunnel |
| VPN consent dialog | Granted by Android before the first WireGuard connection |

No permission is requested for Shizuku, root or storage during VPN import
(the Storage Access Framework is used instead).

## Project structure

```
app/src/main/java/dev/pocketpanel/
  ui/          Compose screens, view model, theme and test tags
  overlay/     overlay window, geometry, panel content and foreground service
  system/      metric providers (CPU, memory, battery, GPU, network, ping)
  vpn/         WireGuard config parsing, encrypted storage and repository
  settings/    DataStore-backed preferences
docs/          specification, architecture, decisions, roadmap and testing
scripts/       local static checks
```

## Documentation

- `docs/PROJECT.md` — scope and principles
- `docs/ARCHITECTURE.md` — module boundaries and data flow
- `docs/DECISIONS.md` — verified versions and technical decisions
- `docs/ROADMAP.md` — milestone history and final verification report
- `docs/TESTING.md` — verification procedures and results
- `docs/RELEASING.md` — signing, versioning and publishing procedure
- `AGENTS.md` — contributor rules

## Known limitations

- CI has no emulator, so instrumented tests and the full API 26–36 matrix
  are not covered; device verification was reported on the owner's devices.
- On Android 8+ `/proc/stat` is blocked to apps, so CPU usage is a
  clearly labelled estimate derived from core clock frequencies (D025).
- GPU identity may be unavailable if an EGL context cannot be created.
  GPU utilization and FPS are not reported because Android has no public
  API for them.
- Thermal status/headroom were removed by request; only battery
  temperature is shown.
- Android allows one active VPN per user/profile; starting another VPN
  stops this one, and the UI reflects the system state.
- The overlay consent helper is started from a service and relies on the
  visible-overlay exemption for background activity starts; this was
  confirmed on the owner's test device.
