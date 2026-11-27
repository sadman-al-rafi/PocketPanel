# PocketPanel Architecture

Architecture decisions must be based on verified Android platform
capabilities and compatible stable technologies.

This document was populated during M0 (research phase) on 2026-11-25.
M1 implements the foundation subset described here: Material 3 theme,
settings persistence and the main/settings screens. Overlay, monitoring
and the VPN client remain proposed until their milestones.

Toolchain versions and verification sources are recorded in
docs/DECISIONS.md. Platform constraints are recorded in sections below.

## 1. Architectural Principles

- Single application module until there is a demonstrated need to split.
- Compose for all application UI, including the overlay panel surface.
- Monitoring is provider-based: every metric is a swappable provider
  that declares its own availability.
- Unavailable data is represented as unavailable, never as zero, a
  placeholder value or an estimate.
- Standard Android access is the baseline. Shizuku and root are optional
  upgrades behind the same provider interfaces.
- Failures are handled explicitly and surfaced in the UI state.
- No background polling that is not justified by a visible feature.

## 2. Source Layout

    app/
      src/main/
        AndroidManifest.xml
        java/dev/pocketpanel/
          PocketPanelApplication.kt   Owns the settings repository
          MainActivity.kt             Single Compose host activity
          settings/
            ThemeMode.kt              SYSTEM / LIGHT / DARK
            UserPreferences.kt        Immutable settings snapshot
            SettingsRepository.kt     Repository contract
            DataStoreSettingsRepository.kt
          ui/
            PocketPanelApp.kt         Screen switch + back handling
            PocketPanelViewModel.kt   Screen state + settings writes
            Labels.kt                 Mode -> string resource mapping
            TestTags.kt               Stable tags for UI tests
            theme/                    Color.kt, Type.kt, Theme.kt
            main/                     MainScreen.kt
            settings/                 SettingsScreen.kt
          overlay/                    M2+
          monitoring/                 M3+
          vpn/                        M6+
          permissions/                M2+
          service/                    M2+
        res/                         Themes, strings, icons, adaptive launcher
                                     icon, data-extraction rules (D017)
      src/test/                      JVM unit tests
      src/androidTest/               Instrumented tests (written, not executed; D016)

    scripts/
      check-static.sh                Local structural checks (no JDK/SDK)

The application ID is `dev.pocketpanel`.

Screens are deliberately few: M1 has a single `PocketPanelViewModel`
that exposes the current screen and the persisted preferences. Separate
screen view models are introduced only when a screen gains state that is
not shared.

## 3. UI Architecture

- Single `MainActivity`, Compose-only content. No Navigation component
  is required for two screens; a sealed `AppScreen` in
  `PocketPanelViewModel` selects main vs. settings. Introduce Navigation
  only if the screen count grows.
- Material 3 theming centralized in `ui/theme`. Composables consume
  `MaterialTheme.colorScheme` and `MaterialTheme.typography` tokens only.
  No hardcoded colors in screens.
- Theme mode (System / Light / Dark) and dynamic color flag live in user
  settings and are applied at the top of the composition.
- Dynamic color uses `dynamicDarkColorScheme`/`dynamicLightColorScheme`
  when `Build.VERSION.SDK_INT >= 31`; otherwise a defined Material 3
  fallback palette.
- Animations limited to panel expansion/collapse and standard Material
  state transitions.

## 4. State Management

- `ViewModel` + `StateFlow` for screen state; collected with
  lifecycle-aware collection in Compose.
- Monitoring data is exposed as a single immutable UI state object:

      data class MonitoringUiState(
          val cpu: MetricUi,
          val memory: MetricUi,
          val battery: MetricUi,
          val thermal: MetricUi,
          val network: MetricUi,
          val gpu: MetricUi,
      )

  where `MetricUi` carries value, unit, timestamp and availability
  (Available / Unavailable / PermissionMissing / NotSupported).
- Provider failures update availability state; they do not throw into
  the composition.
- Settings are a cold `Flow` from DataStore, converted to state where the
  UI needs it. No duplicate in-memory source of truth.

## 5. Overlay Implementation

Verified platform facts:

- Requires `SYSTEM_ALERT_WINDOW`, granted by the user through
  `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`; checked with
  `Settings.canDrawOverlays(context)`.
- The permission can be revoked at any time, including while the service
  runs. It must be re-checked before creating a window and on resume.
- `TYPE_APPLICATION_OVERLAY` exists from API 26. At `minSdk 26` this is
  the only overlay type needed, so no legacy `TYPE_PHONE` path exists.
- `WindowManager.addView` throws `BadTokenException` /
  `WindowManager.InvalidDisplayException` when permission is missing or
  the display changed; both must be caught and mapped to a state.

Implemented design (M2):

- `OverlayService` owns the `WindowManager` and the single overlay window.
- One window hosts both the handle and the panel. Collapsed it is
  `48 x 96 dp`; expanded it is `320 x 176 dp` (`48 dp` handle plus a
  `272 x 176 dp` panel). The panel height is `176 dp` so the "Hide"
  button fits at every supported density.
- The window is `TYPE_APPLICATION_OVERLAY` with `FLAG_NOT_FOCUSABLE`,
  `PixelFormat.TRANSLUCENT` and `Gravity.TOP or Gravity.START`.
  `layoutInDisplayCutoutMode` is `NEVER` on API 28+.
- The panel is rendered with a `ComposeView` wrapped in a
  `ContextThemeWrapper` so the Material 3 theme resolves. An overlay
  window is not Activity-backed, so the service attaches
  `ViewTreeLifecycleOwner` and `ViewTreeSavedStateRegistryOwner` itself
  using a service-scoped `OverlayLifecycleOwner`. Compose requires the
  saved-state owner even though the overlay restores nothing. View-model
  storage is omitted because it is optional and unused.
- Coordinates are relative to the usable window area (display minus
  system bars), read from `WindowManager.currentWindowMetrics` on API
  30+ and `defaultDisplay` below it. Placement offsets are applied
  directly with `Gravity.TOP or Gravity.START`.
- Drag uses screen coordinates from `getLocationOnScreen` instead of
  Compose-local deltas. Moving the window under the finger would
  otherwise cancel the local deltas and stall the drag.
- On release the window snaps to the nearest horizontal edge; the edge
  and the clamped vertical fraction are persisted atomically through
  `SettingsRepository.setHandlePosition`. The panel opens toward the
  screen interior, and its vertical centre follows the handle so the
  handle does not jump when the panel opens.
- The window is removed synchronously in `onDestroy`; the permission is
  re-checked on resume and immediately before `addView`.
- Deferred beyond M2 acceptance: outside-tap collapse, screen-off
  collapse, boot restore (no boot receiver) and permission-loss
  detection while the app is backgrounded. These are tracked in
  `docs/ROADMAP.md`.

## 6. Service Lifecycle

### 6.1 Is a foreground service required?

Verified from Android documentation:

- `SYSTEM_ALERT_WINDOW` is what authorizes `TYPE_APPLICATION_OVERLAY`.
  No foreground service is an API prerequisite for adding the window.
- A window belongs to the application's process. If that process is
  killed, the window disappears. A process hosting a window that is
  visible to the user is classified at least `IMPORTANCE_VISIBLE` (200),
  which is protected but ranked below a foreground service (125), and no
  Android documentation guarantees that an overlay-only process survives
  memory pressure or background restrictions.
- A foreground service is the documented mechanism for work the user is
  aware of, and `startForeground()` keeps the process at
  foreground-service importance.

Conclusion: a foreground service is not required by the permission model,
but it is required in practice for a dependable always-available overlay.
This is recorded as D008.

### 6.2 Which service type?

Android 14+ requires a declared type, and the available types are
purpose-bound. Checked against the official type list:

- `camera`, `microphone`, `location`, `health`, `mediaPlayback`,
  `mediaProjection`, `mediaProcessing`, `phoneCall`, `remoteMessaging`,
  `dataSync`, `connectedDevice`: all describe a specific activity that
  PocketPanel does not perform.
- `shortService`: limited to roughly three minutes, cannot be sticky, and
  cannot start other foreground services. Not suitable for a persistent
  overlay.
- `systemExempted`: reserved for system apps and specific integrations
  (device/profile owners, device admins, VPN apps configured in system
  settings, emergency-role apps and similar). A normally installed VPN
  client cannot rely on it.
- `specialUse`: the documented fallback for valid cases not covered by
  another type. Google Play describes it as being for "limited
  scenarios", states that all foreground service types are subject to
  review, and requires a Play Console declaration with a functionality
  description, a user-impact statement, a selected use case and a
  demonstration video.

Therefore `specialUse` is the only eligible type for the overlay, but it
is not automatically acceptable for Play distribution. It is a review
risk, not a settled fact. The M6 VPN service also uses `specialUse`; see
section 13.

Fallback if `specialUse` is not accepted: run the overlay without a
foreground service, which means the overlay only lives as long as the app
process does, and the product must present that limitation honestly.
This fallback is not implemented or promised now.

### 6.3 Implemented behavior (M2)

- `OverlayService` is a foreground service with type `specialUse`,
  declared with `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE`,
  the `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` manifest property, and a
  user-visible notification.
- `POST_NOTIFICATIONS` is requested on API 33+; denial is tolerated
  (the notification is invisible but the service still runs). The app
  shows a caveat on the main screen while the overlay is on and
  notifications are denied.
- `startForeground` is called immediately in `onStartCommand` to satisfy
  the five-second requirement.
- `START_NOT_STICKY` is used: restarting the overlay without a visible
  window or user intent is not desirable and conflicts with Android 15+
  foreground-service start rules.
- Background start: holding `SYSTEM_ALERT_WINDOW` is a documented
  exemption, but on Android 15+ it additionally requires a currently
  visible overlay window. Therefore the service is started only from a
  user gesture (app button or an existing visible overlay), never from
  a boot receiver.
- The saved `overlayEnabled` setting is restored on app resume when the
  permission is still granted. `OverlayService.isRunning` is an
  in-process flag used to avoid redundant restarts; it is not persisted.
- If the permission is revoked, or the window cannot be added, the
  service clears `overlayEnabled` and stops rather than retrying.
- M3/M4 monitoring is exposed on the main screen, not inside the
  overlay panel. Sampling runs in `SystemInfoRepository` on
  `Dispatchers.Default` and is subscribed only while the UI collects
  `PocketPanelUiState` (`SharingStarted.WhileSubscribed`). It therefore
  stops when the app is not visible. Overlay-panel sampling remains a
  later milestone and must additionally stop when the panel is
  collapsed or the screen is off. The service itself continues to exist
  only while the overlay is enabled by the user.

## 7. Monitoring Providers

Contract (conceptual):

    interface MetricProvider {
        val id: MetricId
        fun availability(): Availability
        suspend fun sample(): MetricSample
        fun close() {}
    }

`MonitoringCoordinator` owns a user-configured interval (default
approximately 2 s for CPU/RAM while the panel is open), runs sampling on
`Dispatchers.Default`, and drops overlapping samples.

| Provider | Source | Availability model |
|---|---|---|
| `CpuUsageProvider` | `/proc/stat` deltas (total and per-cluster where readable) | Best-effort. Not an SDK contract. Unreadable file, parse failure, or non-monotonic counters => Unavailable. Never estimate from a missing baseline. |
| `MemoryProvider` | `ActivityManager.MemoryInfo` (`totalMem`, `availMem`, `lowMemory`, `threshold`) | Public API, available on all supported devices. |
| `BatteryProvider` | Sticky `ACTION_BATTERY_CHANGED` plus `BatteryManager.getIntProperty` | Public API. Properties that return `Integer.MIN_VALUE` are reported as Unavailable individually. |
| `ThermalProvider` | `PowerManager.getCurrentThermalStatus()` (API 29+), `getThermalHeadroom()` (API 30+), thresholds (API 35+), headroom listener (API 36+) | Available only when the running API level provides the call; below that the metric is NotSupported. Thermal status is a coarse enum, not a temperature. |
| `NetworkProvider` | `TrafficStats` / `ConnectivityManager` | Implemented in M5 as `NetworkMonitor`. Throughput is device-wide; per-app attribution is not exposed by `TrafficStats`. Connection type is best-effort. |
| `GpuProvider` | None public. Optional vendor sysfs and EGL renderer string only | Always Unavailable for utilization. Renderer string may be shown as informational data, never as load. |

M5 implements the network source as `NetworkMonitor`: the transport
comes from `NetworkCapabilities`, and throughput is the delta between
two `TrafficStats.getTotalRxBytes` / `getTotalTxBytes` samples. A null
rate is shown as "Not available", and the UI states that rates are
device-wide.

Ping latency is measured by `PingProbe` with the platform `ping`
command (`/system/bin/ping`, falling back to `ping` on `PATH`) against
8.8.8.8. The result is cached for five seconds and exposed as
`NetworkInfo.pingMillis` so the overlay side panel can reuse it later.
The command is not an SDK contract: a missing binary, a blocked
executable, a timeout or unparseable output yields null, shown as
"Not available". No latency is estimated.

Explicit non-goals in monitoring: total CPU usage from `ActivityManager`,
per-process CPU, GPU utilization, and any fabricated "FPS" reading.

## 8. Permissions

| Permission | When | Handling |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Enabling the overlay | Special app access. Explain before opening settings; re-check on resume and before adding windows. |
| `FOREGROUND_SERVICE` | Manifest | Required for the overlay service. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Manifest | Required from API 34 with the declared service type. |
| `POST_NOTIFICATIONS` | API 33+, before starting the service | Runtime request; denial does not block the feature, but is shown in settings as a caveat. |
| `ACCESS_NETWORK_STATE` | Manifest, network provider | Normal permission, no prompt. |
| `INTERNET` | Manifest, ping probe and VPN | Normal permission, no prompt. Required for the platform `ping` process and the WireGuard tunnel. |
| `BIND_VPN_SERVICE` | VPN service declaration | Service-level permission; only the system may bind. Not a runtime request. |

No location, storage, contacts, phone state or accessibility permissions
are used. No `QUERY_ALL_PACKAGES`.

## 9. Data Persistence

- DataStore Preferences for: theme mode, dynamic color, monitoring
  interval, overlay enabled, handle edge, handle position, visible
  metric cards, quick tool preferences.
- Implemented in M1: `theme_mode` (enum name) and `dynamic_color`
  (boolean) in the single `settings` DataStore. Keys are private to
  `DataStoreSettingsRepository`; no screen reads raw preference keys.
- Implemented in M3: `monitoring_enabled` (boolean) and
  `monitoring_interval_seconds` (int, clamped to 1-10 s). The interval
  is clamped again when read, so a corrupt value cannot cause a busy
  loop.
  Corrupt or unreadable data falls back to defaults.
- M6 stores non-secret WireGuard profile metadata in DataStore. The
  WireGuard private key is encrypted with an Android Keystore key before
  it is stored; no plaintext secret is written to DataStore, logs,
  crash output or backups (`allowBackup=false`).
- No database is required at this stage. A database is introduced only
  if a demonstrated size or query need justifies Room.

## 10. Optional Elevated Access Boundaries

- Shizuku and root are adapters behind the existing `MetricProvider`
  interface, not alternate code paths in the UI.
- Capability detection is runtime-only: Shizuku binder alive and
  permission granted; root binary present and a test call succeeding.
- The M6 WireGuard client uses the standard `VpnService` API and does
  not depend on Shizuku or root.
- Absence, revocation, or a failed elevated call degrades the affected
  metric to Unavailable and never disables standard mode.
- Elevated providers live in their own package so they can be audited
  and, if needed, excluded at build time.
- Verified constraint: the Shizuku shell UID has limited permissions; it
  is not equivalent to root. Shizuku also requires the separate Shizuku
  application and a user-initiated start (root or Android 11+ wireless
  debugging).

## 11. Testing Strategy

See docs/TESTING.md. Local development uses `scripts/check-static.sh`,
which needs no JDK or Android SDK. Compilation, lint, JVM tests and the
debug APK require an explicitly authorized manual run of GitHub Actions
(D012, D016). Instrumented tests exist but are not executed, and no
emulator is provisioned (D016).

## 12. Explicitly Deferred

- Multiple modules, dependency injection framework, navigation library.
- Room, WorkManager, and any scheduled background work.
- Compose Multiplatform or any non-Android target.
- Cloud sync, accounts, telemetry.
- Always-on VPN, per-app routing, kill switch, multiple VPN profiles
  and non-WireGuard protocols.

## 13. VPN Client (M6)

- The client embeds the official WireGuard tunnel library
  (`com.wireguard.android:tunnel`) instead of implementing the protocol.
  The library is wrapped behind a `VpnBackend` interface so the UI and
  tests never depend on it directly.
- A `VpnService` subclass owns the Android tunnel interface. The service
  is declared with `android.permission.BIND_VPN_SERVICE` and the
  `android.net.VpnService` intent filter, so only the system can bind to
  it.
- `VpnService.prepare()` consent is requested before the first connect.
  Denial or revocation is surfaced as a disconnected state, never a
  stale connected state. `onRevoke()` closes the tunnel.
- The service runs in the foreground with a non-dismissible
  notification. On API 34+ it uses the `specialUse` foreground service
  type; the overlay already declares `FOREGROUND_SERVICE_SPECIAL_USE`.
- Configurations are imported through the Storage Access Framework or
  pasted text, then parsed and validated locally. No storage permission
  is required.
- The WireGuard private key is encrypted with an Android Keystore key
  before storage. Secrets are never logged or included in diagnostics.
- A `VpnRepository` exposes connection state and connect/disconnect
  actions. The app UI and, later, the overlay side panel consume the
  same repository, so the overlay button cannot bypass consent or state
  handling.
- Only one VPN can be active per user or profile. Starting another VPN
  stops this one; the UI reflects the system's actual state.
- No root or Shizuku is required.
