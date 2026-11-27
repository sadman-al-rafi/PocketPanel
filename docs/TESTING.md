# PocketPanel Testing Strategy

Testing must reflect what was actually executed. Any unrun or
unavailable test is reported as such, never implied to have passed.

## 1. CI Authorization

CI runs on every push to `main` (AGENTS.md section 13, D024).

- `.github/workflows/verify-toolchain.yml` triggers on `push` to `main`
  and on `workflow_dispatch`. It must not be triggered by
  `pull_request` or `schedule`.
- A push starts the build, lint and JVM tests automatically. Results
  are published to the `ci-results` branch.
- Do not push solely to discover whether the code compiles. Run
  `scripts/check-static.sh`, review the change and diagnose locally
  first.
- A failed run is diagnosed from its logs first. Do not retry
  repeatedly; push the smallest fix.
- CI is not a substitute for local static checks (section 2), which
  cover what they can without a JDK or Android SDK.

The debug APK is produced by CI, and every reported result must come
from a run that actually completed.

## 2. Local Static Verification

There is no local JDK, Android SDK, Gradle or Kotlin compiler in the
Termux environment. `scripts/check-static.sh` performs the checks that
do not need them:

- Required project files exist (wrapper, manifest, version catalog,
  theme, screens).
- Every XML file is well formed (tag balance and quoting, checked with
  `awk`/`grep` because no XML parser is installed).
- Every `R.string`, `R.drawable`, `R.style` and `@string`/`@drawable`/
  `@style`/`@color` reference resolves to a declared resource.
- No duplicate resource names in a `values` file.
- Every version catalog alias used in a Gradle build file exists.
- No hardcoded `Color(0x...)` outside `ui/theme`.
- Every `TestTags` constant is used and every `testTag(...)` argument
  exists in `TestTags`.
- Manifest declares only the permissions listed in
  docs/ARCHITECTURE.md section 8.
- The overlay service is declared, not exported, uses
  `specialUse` with the subtype property, and has a source file.
- The overlay window uses `TYPE_APPLICATION_OVERLAY`,
  `FLAG_NOT_FOCUSABLE` and `START_NOT_STICKY`.
- No `BOOT_COMPLETED` receiver and no legacy `TYPE_PHONE`/
  `TYPE_SYSTEM_ALERT` window type exist.
- No `TODO`/`FIXME` markers left in application sources.

As of M2 the script runs 75 checks.

These checks are structural. They cannot prove that the code compiles
or that the UI renders; only an authorized CI run can do that.

## 3. Build Verification (authorized runs only)

The committed Gradle wrapper selects Gradle 9.6.0 (D011). An authorized
run of `.github/workflows/verify-toolchain.yml` executes, in order:

    ./gradlew --version                     # wrapper + launcher JVM
    ./gradlew buildEnvironment              # resolved plugins, KGP version
    ./gradlew :app:dependencies --configuration debugCompileClasspath
    ./gradlew :app:assembleDebug            # APK
    ./gradlew :app:lintDebug                # Android lint
    ./gradlew :app:testDebugUnitTest        # JVM unit tests

Requirements for a build to count as verified:
- The output is observed in a completed workflow run, not assumed.
- AGP, Gradle, JDK and Kotlin versions in the output match
  docs/DECISIONS.md D005 and D013.
- No prerelease dependency appears in the resolved graph.
- A command that reports `NO-SOURCE`, `SKIPPED` or `UP-TO-DATE` is not
  counted as a passing test.

Results are published to the `ci-results` branch (`status.txt`,
`versions.log`, filtered dependency and build-environment logs, test
reports, lint report, APK size and sha256), and full logs plus the debug
APK are uploaded as workflow artifacts. Job console logs require GitHub
authentication and are not used as the sole evidence.

Current status (2026-11-26):
- M0b: `assembleDebug` succeeded on `ubuntu-24.04` with AGP 9.5.2,
  Gradle 9.6.0, Temurin JDK 17.0.21 and Kotlin 2.4.21 (runs 45621464769
  and 45621953748).
- M1: authorized manual run 45669463189 (2026-11-25, commit `ba95fb4`,
  run 10) succeeded. `assembleDebug`, `lintDebug` (0 errors, 5
  warnings) and `testDebugUnitTest` (5 tests, 0 failures, 0 ignored)
  all passed. Debug APK: 13,903,982 bytes, sha256
  `d29438c6719868a1bc879a650a00dd77970e862bf853b6a16420b254b6b02bb3`.
- Lint warnings in that run: Gradle 9.9.2 available (pinned 9.6.0),
  AGP 9.5.3 available (pinned 9.5.2), `targetSdk 36` older than
  `compileSdk 37.1` (D006 policy), an obsolete `-v26` mipmap qualifier
  and missing data-extraction rules. The last two were fixed after the
  run (D017); the first three are intentional.
- Runtime behavior (launch, theme switching, persistence, dynamic
  color) remains unverified because no device or emulator test is
  authorized (D016).
- M2: authorized manual run 45753602748 (run 11, commit `ef88643`)
  failed at `assembleDebug` with
  `Unresolved reference 'ViewTreeLifecycleOwner'` in
  `OverlayWindowController.kt`. Lifecycle 2.11.1 no longer exposes that
  class; the supported API is the `View.setViewTreeLifecycleOwner`
  extension. Lint, unit tests and the APK did not run.
- M2: authorized manual run 45754823414 (run 12, commit `5ec59d8`)
  failed at `assembleDebug` with a JVM signature clash: the private
  `expanded` property generated `setExpanded(Z)V`, which collided with
  `private fun setExpanded(Boolean)`. The function was renamed to
  `applyExpanded`. Lint, unit tests and the APK did not run.
- Static review of the Compose 1.13 source found that `ComposeView`
  throws when no `ViewTreeSavedStateRegistryOwner` is present, even
  though the overlay restores nothing. `OverlayLifecycleOwner` now
  implements `SavedStateRegistryOwner` and attaches an empty registry;
  `androidx.savedstate:savedstate:1.5.0` is declared explicitly (D019).
- **Run 45756276354 (run 13, commit `c254da9`) succeeded**: build
  successful, lint 0 errors/5 warnings, 5/5 JVM tests passed, APK
  13,961,294 bytes, sha256
  `49d058a479459b6507e3477e5f54fcc6bc70bae248100bd574c23fd769ec4838`.
  M2 acceptance criteria are satisfied from a compilation/lint/test
  perspective. Runtime overlay behavior remains unverified because no
  device or emulator test is authorized (D016).
- **M3: six authorized runs (14-19) all failed at `assembleDebug`.**
  - Run 45770033796 (run 14) failed with
    `Unresolved reference 'Context'` in `DeviceInfo.kt`. The status
    file recorded commit `bd9abdd`; the import fix was pushed as
    `2a900ea`.
  - Run 45770401046 (run 15, commit `2a900ea`) failed with
    `Unresolved reference 'isLevelKnown'` on `BatteryManager`.
  - Run 45771495631 (run 16, commit `d124481`) failed with
    `Unresolved reference 'getInt'` on `BatteryManager`.
  - Run 45771970812 (run 17, commit `609cfe6`) failed with the same
    `getInt` error.
  - Run 45772367516 (run 18, commit `609cfe6`) failed with
    `Unresolved reference 'memoize'`.
  - Run 45822071489 (run 19, commit `609cfe6`, finished
    2026-11-26T12:42:32Z) failed with the same `memoize` error.
  - Root cause found during this session: the M3 source files were
    invalid placeholder code, not a toolchain incompatibility. They
    contained an invented `androidx.compose.runtime.memoize` import,
    invented `androidx.compose.ui.*` imports, `kotlin.run(?)`,
    `Arrangement.space(...)`, reassignment of a `val` parameter, a
    `StringBuilder` helper that emitted XML, and a reference to a
    non-existent `com.github.lin7xv.library.cpuinfo` library. Fixing
    one reported error only exposed the next one.
- **M3/M4 rewrite**: the system package was rewritten using documented
  framework APIs only: `ActivityManager.MemoryInfo` for memory, the
  sticky `ACTION_BATTERY_CHANGED` broadcast for
  level/temperature/health, and `/proc/stat` deltas for best-effort CPU
  usage. Monitoring state and interval are persisted in DataStore.
  Unsupported values remain null and are rendered as "Not available".
  No new dependencies were added. `./scripts/check-static.sh` passes
  75/75.
- **Run 45824780276 (run 20, commit `ca9ba8a`) succeeded**:
  - `assembleDebug`: success.
  - `lintDebug`: 0 errors, 5 warnings. The warnings are the same
    pre-existing ones: `OldTargetApi`, `AndroidGradlePluginVersion`,
    `DataExtractionRules`, and `UseKtx` in `OverlayPermission.kt:29`.
    No new warning comes from the M3/M4 code.
  - `testDebugUnitTest`: 15 tests, 0 failures, 0 ignored
    (`OverlayGeometryTest` 7, `ThemeModeTest` 5, `HandleEdgeTest` 3),
    all at 100%.
  - Debug APK: 14,003,861 bytes, sha256
    `1adee21605bdf0cb7707f89a6e3bb40fb6946303aaa8dde89654b243ea887802`.
  - Runtime monitoring behavior is not covered by CI and remains
    unverified (D016).
- **M5 implementation**: network transport and device-wide throughput
  are read with `ConnectivityManager` / `NetworkCapabilities` and
  `TrafficStats`. `ACCESS_NETWORK_STATE` is added to the manifest.
- **Run 45936405957 (run 21, commit `50735e4`) succeeded**:
  - `assembleDebug`: success.
  - `lintDebug`: 0 errors, 5 warnings (the same pre-existing set).
  - `testDebugUnitTest`: 20 tests, 0 failures, 0 ignored
    (`OverlayGeometryTest` 7, `ThemeModeTest` 5, `HandleEdgeTest` 3,
    `NetworkRateTest` 5), all at 100%.
  - Debug APK: 14,008,773 bytes, sha256
    `8caf28d98ba801bbf68dcb9f967a35f32c1002fff9dbbc83280e5f2024a74580`.
  - The user confirmed that the app shows the network data on a local
    device test.
- **Ping section**: latency to 8.8.8.8 is measured with the platform
  `ping` command, parsed for the `time=... ms` value, and cached for
  five seconds. `INTERNET` is declared for the ping process. A missing
  or blocked `ping` binary yields "Not available".
- **Run 45939285640 (run 22, commit `37fab11`) succeeded**:
  - `assembleDebug`: success.
  - `lintDebug`: 0 errors, 5 warnings (the same pre-existing set).
  - `testDebugUnitTest`: 25 tests, 0 failures, 0 ignored
    (`OverlayGeometryTest` 7, `ThemeModeTest` 5, `HandleEdgeTest` 3,
    `NetworkRateTest` 5, `PingLatencyTest` 5), all at 100%.
  - Debug APK: 14,025,981 bytes, sha256
    `ad377fe0230cc8878fa928c02c76e0cde61cc4c2630459f6660951afbe3f5053`.
  - The user confirmed that the ping row works on a test device.

## 4. Configuration Verification
- `compileSdk 37.1` (API 37 with `minorApiLevel = 1`), `targetSdk 36`,
  `minSdk 26` in the app module.
- JDK 17 toolchain, `jvmTarget = 17`.
- Manifest contains exactly the permissions listed in
  docs/ARCHITECTURE.md section 8 and no others. M2 declares
  `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_SPECIAL_USE` and `POST_NOTIFICATIONS`.
- The overlay service declares `foregroundServiceType="specialUse"`
  and the `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property.
- No `kotlin-android` plugin applied.
- Dependency versions come from the version catalog only.
- M1/M2 dependencies: DataStore Preferences 1.3.0,
  lifecycle-runtime-compose and lifecycle-viewmodel-compose 2.11.1,
  savedstate 1.5.0, JUnit 4.13.2, androidx.test ext-junit 1.3.0 and
  runner 1.7.0 (D014, D019).
- M3/M4 add no new dependencies. They use framework APIs
  (`ActivityManager`, `BatteryManager`) and framework files
  (`/proc/stat`, cpufreq sysfs) with the existing Compose, lifecycle and
  DataStore libraries. D025 added the CPU fallback without a dependency.

## 5. Application Verification

Emulator and device testing are suspended (D016). The instrumented
tests remain in `app/src/androidTest` for later use, but they are not
executed and their results are not claimed.

Written but not yet executed:
- `MainActivitySmokeTest`: launch, settings options, theme selection
  reflected on the main screen.
- `SettingsPersistenceTest`: a written theme survives closing one
  DataStore and opening a fresh one on the same file.

JVM unit tests (executed only in an authorized CI run):
- `ThemeModeTest`: storage mapping, fallback, and dark-theme resolution.
- `OverlayGeometryTest`: edge placement, vertical clamping, snap to
  nearest edge, fraction derivation and degenerate screen sizes.
- `HandleEdgeTest`: storage mapping, fallback and default preferences.
- `NetworkRateTest`: counter deltas, zero delta, unsupported counters,
  backwards counters and non-positive elapsed time.
- `PingLatencyTest`: decimal, whole-number and sub-millisecond latency
  parsing, plus timeout and empty output returning null.

Manual review checklist (no emulator; code and layout review):
- Colors come from `MaterialTheme.colorScheme`; no literal colors
  outside `ui/theme`.
- Theme mode resolves to dark/light correctly for SYSTEM/LIGHT/DARK.
- Dynamic color is used on API 31+ and the switch is disabled below 31.
- Content stays in a readable column at phone and tablet widths.
- Interactive elements have content descriptions and at least 48 dp
  touch targets.
- Edge-to-edge insets are consumed by `Scaffold`.

Visual confirmation of actual colors, dynamic color and landscape/tablet
rendering is not possible without a device and remains unverified.

## 6. Overlay Verification

Each case must be tested on a real device or an emulator with the
overlay permission state controlled manually. M2 implementation status
is noted per case; none of these cases has been executed yet. The first
composition also exercises the service-provided lifecycle and
saved-state owners (D019); a missing saved-state owner would throw
before the handle renders.

| Case | Expected | M2 status |
|---|---|---|
| Permission not granted | Explanatory UI, no window attempt, no crash | Implemented, unverified |
| Permission granted, overlay enabled | Handle appears at the saved edge | Implemented, unverified |
| Permission revoked while running | Window removed, service stops, state updates | Implemented, unverified |
| Permission revoked while app is backgrounded | Detected on next start/resume, no crash | Implemented, unverified |
| Drag to each edge | Handle clamps inside bounds, edge persists | Implemented, unverified |
| Panel open/close via handle | Toggles without leaving the app | Implemented, unverified |
| Outside tap while expanded | Panel collapses | Deferred (D018) |
| Screen off / on | Overlay recovers, no duplicate windows | Deferred (D018) |
| Rotation and display size change | Handle remains visible and reachable | Implemented, unverified |
| Device reboot | Overlay stays off after boot; opening the app restores it from the saved setting | By design (no boot receiver) |
| Notification permission denied (API 33+) | Service runs, caveat shown in settings | Implemented, unverified |
| Service notification Stop action | Overlay stops and setting turns off | Implemented, unverified |

## 7. Monitoring Verification

For every metric, verify all three states:

1. Available: a plausible value that changes over time.
2. Unavailable: source exists but currently cannot be read; UI shows
   unavailable, not `0`.
3. Not supported: API level or device lacks the capability; UI states
   that explicitly.

Specific checks:
- CPU: counter deltas are used when `/proc/stat` is readable; on API 26+
  the UI shows "CPU usage (estimated)" with the explanatory note. The
  estimate comes from cpufreq sysfs values and is clamped to 0-100. If
  neither source is readable the row shows "Not available", not 0%.
- JVM tests cover counter deltas, frequency estimation and `/proc/stat`
  parsing without double-counting guest time. All 7 cases passed in CI
  run 26. The on-device CPU row is still unverified.
- RAM: `ActivityManager.MemoryInfo` values match a system reference
  within reasonable tolerance.
- Battery: level, status, temperature and voltage match system
  settings/info; properties returning `Integer.MIN_VALUE` show as
  unavailable.
- Thermal: status enum shown only on API 29+; below that the card says
  "Not supported on this Android version". Headroom (API 30+) is a
  fraction, with 1.00 meaning severe throttling; NaN shows "Not
  available". Status is never presented as a temperature.
- GPU: renderer, vendor and OpenGL ES strings are shown when the EGL
  context can be created; otherwise "Not available". Utilization and
  FPS are never displayed.
- Network: throughput changes with real traffic; connection type
  updates on Wi-Fi/mobile switch.

Polling and lifecycle:
- Sampling stops when the panel is collapsed.
- Sampling stops when the screen is off.
- The configured interval is respected (verify with logging, not
  guesswork).
- No accumulating coroutines, listeners or window leaks after repeated
  open/close cycles.

## 8. API Level Matrix

When device testing resumes, run the smoke suite on at least:

| API level | Why |
|---|---|
| 26 | Minimum supported; overlay baseline |
| 29 | Thermal status becomes available |
| 30 | Thermal headroom becomes available |
| 34 | Mandatory foreground service types |
| 35 | Headroom thresholds; overlay FGS start rule change |
| 36 | Target SDK behavior; headroom listeners |

`minSdk` behavior must also be checked by code review for every call to
an API newer than 26.

## 9. Resource and Battery Verification

- Compare battery drain over a fixed idle period with the overlay
  enabled and disabled.
- Confirm CPU time used by the app while the panel is collapsed is
  negligible.
- Confirm the overlay view hierarchy is removed, not hidden, when
  disabled.
- Check memory after 50 open/close cycles for growth.

## 10. Elevated Access (not scheduled)

Shizuku and root integration are not on the roadmap; these checks are
retained only as constraints if that ever changes.

- Standard mode fully functional with Shizuku absent and no root.
- Shizuku present but permission denied: metric unavailable, no crash.
- Shizuku stopped while in use: graceful degradation.
- Root granted then revoked: graceful degradation.

## 11. Regression Verification

Previously completed functionality must be retested after relevant
changes. At minimum: overlay permission flow, panel toggle, theme
persistence, and one monitoring sample per metric. After M6: VPN
consent, connect, disconnect and revocation.

## 12. VPN Verification (M6)

Automated verification was executed on 2026-11-27. Full tunnel behavior
cannot be verified in CI because no emulator is provisioned (D016).

Automated (executed):
- CI run `45962542309` (run 24, commit `5ecbc25`) passed
  `assembleDebug`, `lintDebug` and `testDebugUnitTest`.
- 38 JVM tests passed, 0 failures; 13 of them are
  `WireGuardConfigParserTest` cases.
- Lint: 0 errors, 5 pre-existing warnings.
- `scripts/check-static.sh`: 86/86 checks passed, including the new VPN
  dependency, consent and duplicate-service checks.
- Native ABI contents of the APK were not inspected in the published
  results; the build with the library AAR succeeded.
- Run 23 (`45962342278`) failed to compile on `override val name` in
  `VpnTunnel`; the fix was pushed and verified by run 24.

Manual device verification (user-reported):
- First connect shows the system VPN consent dialog; denial leaves the
  UI disconnected.
- A valid configuration establishes a tunnel; Android's system VPN
  indicator appears. The app does not create a VPN notification.
- Disconnect clears the system VPN indicator and restores normal
  networking.
- Revoking the VPN in system settings triggers `onRevoke()` and the app
  shows disconnected, not connected.
- Starting another VPN app stops this one and the UI updates.
- Airplane mode or a network switch does not crash the service, and the
  state remains accurate.
- Private keys are absent from logcat, plaintext DataStore and crash
  output.
- With the VPN off, monitoring and the overlay still work.

## 13. Hardware Verification (M7)

Automated (executed):
- CI run `45966560679` (run 28, commit `773e6b3`) passed build, lint and
  48/48 JVM tests, including 3 `ThermalInfoTest` mapping cases.
- `scripts/check-static.sh`: 101/101 checks.

Manual device verification (user-reported, pending):
- The advanced hardware card shows a GPU renderer, vendor and OpenGL ES
  version where an EGL context can be created.
- No GPU utilization percentage or FPS value is shown anywhere.
- On API 26-28 the thermal status row says "Not supported on this
  Android version"; on API 29+ it shows a coarse status label; on API
  30+ it also shows a headroom fraction.
- With monitoring off, the card states that monitoring must be enabled.
- Standard mode still works with no Shizuku or root setup.

## 14. Customization Verification (M10)

Automated (executed):
- CI run `46008800467` (run 37, commit `dd9ba0e`) passed build, lint and
  50/50 JVM tests, including 2 new preference-enum cases.
- `scripts/check-static.sh`: 109/109 checks, covering persisted keys,
  settings options, the consent Activity and the shared VPN state.

Manual device verification (user-reported, pending):
- Each main-screen layout preset shows the expected cards and persists
  after a process restart.
- Compact and normal handle sizes resize the handle and panel; changing
  the edge moves the handle without resetting its vertical position.
- Monitoring enable/interval changes in settings are saved.
- The overlay VPN row shows the real connection state. First connect
  opens the system consent dialog; denial leaves it disconnected; a
  granted consent starts the tunnel; disconnect works from the overlay.

## 15. Reporting

Every test report must state:
- What was executed, with the command or manual steps.
- The environment (device model, API level, build type).
- Result per item: pass, fail, or not run.
- Failures with the actual error output.
- Known limitations and anything that could not be verified.

Never claim tests were performed when they were not. A workflow that
has not been authorized and run has no result.
