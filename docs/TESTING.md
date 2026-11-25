# PocketPanel Testing Strategy

Testing must reflect what was actually executed. Any unrun or
unavailable test is reported as such, never implied to have passed.

## 1. Build Verification

Commands (run from the project root):

    ./gradlew --version                     # wrapper + JVM actually used
    ./gradlew buildEnvironment              # resolved plugins, KGP version
    ./gradlew :app:assembleDebug            # compilation + APK
    ./gradlew :app:lintDebug                # Android lint
    ./gradlew :app:testDebugUnitTest        # JVM unit tests

Requirements for a build to count as verified:
- The Gradle wrapper is used, not a system Gradle.
- The output is observed, not assumed.
- AGP, Gradle, JDK and Kotlin versions in the output match
  docs/DECISIONS.md D005 and D007.
- No prerelease dependency appears in the resolved graph
  (`./gradlew :app:dependencies`).

Current status:
- Not run. No JDK, Android SDK or Gradle is installed in the Termux
  development environment, and Google does not publish build-tools for
  Linux aarch64.
- The verification environment is an open decision in
  docs/DECISIONS.md D012. Until it is resolved, no build claim may be
  made.

## 2. Configuration Verification

- `compileSdk 37`, `targetSdk 36`, `minSdk 26` in the app module.
- JDK 17 toolchain, `jvmTarget = 17`.
- Manifest contains exactly the permissions listed in
  docs/ARCHITECTURE.md section 8 and no others.
- Overlay service declares `foregroundServiceType="specialUse"` and the
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property.
- No `kotlin-android` plugin applied.
- Dependency versions come from the version catalog only.

## 3. Application Verification

- Cold start reaches the main screen without a crash.
- Theme mode System / Light / Dark applies immediately and after
  restart.
- Dynamic color applies on API 31+; fallback palette is coherent below
  that.
- Settings changes persist across process death.
- No hardcoded colors outside the theme package (review).
- TalkBack labels present on interactive elements; touch targets at
  least 48 dp.

## 4. Overlay Verification

Each case must be tested on a real device or an emulator with the
overlay permission state controlled manually.

| Case | Expected |
|---|---|
| Permission not granted | Explanatory UI, no window attempt, no crash |
| Permission granted, overlay enabled | Handle appears at the saved edge |
| Permission revoked while running | Window removed, service stops, state updates |
| Permission revoked while app is backgrounded | Detected on next start/resume, no crash |
| Drag to each edge | Handle clamps inside bounds, edge persists |
| Panel open/close via handle | Toggles without leaving the app |
| Outside tap while expanded | Panel collapses |
| Screen off / on | Overlay recovers, no duplicate windows |
| Rotation and display size change | Handle remains visible and reachable |
| Device reboot | Overlay stays off until the user enables it |
| Notification permission denied (API 33+) | Service runs, caveat shown in settings |

## 5. Monitoring Verification

For every metric, verify all three states:

1. Available: a plausible value that changes over time.
2. Unavailable: source exists but currently cannot be read; UI shows
   unavailable, not `0`.
3. Not supported: API level or device lacks the capability; UI states
   that explicitly.

Specific checks:
- CPU: value derives from `/proc/stat` deltas; first sample after start
  is not shown as a fabricated percentage.
- RAM: `ActivityManager.MemoryInfo` values match a system reference
  within reasonable tolerance.
- Battery: level, status, temperature and voltage match system
  settings/info; properties returning `Integer.MIN_VALUE` show as
  unavailable.
- Thermal: status enum shown only on API 29+; headroom only on API 30+;
  thresholds only on API 35+.
- Network: throughput changes with real traffic; connection type
  updates on Wi-Fi/mobile switch.
- GPU: never displays a utilization percentage or "FPS".

Polling and lifecycle:
- Sampling stops when the panel is collapsed.
- Sampling stops when the screen is off.
- The configured interval is respected (verify with logging, not
  guesswork).
- No accumulating coroutines, listeners or window leaks after repeated
  open/close cycles.

## 6. API Level Matrix

At minimum, run the smoke suite on:

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

## 7. Resource and Battery Verification

- Compare battery drain over a fixed idle period with the overlay
  enabled and disabled.
- Confirm CPU time used by the app while the panel is collapsed is
  negligible.
- Confirm the overlay view hierarchy is removed, not hidden, when
  disabled.
- Check memory after 50 open/close cycles for growth.

## 8. Elevated Access Verification (future milestones)

- Standard mode fully functional with Shizuku absent and no root.
- Shizuku present but permission denied: metric unavailable, no crash.
- Shizuku stopped while in use: graceful degradation.
- Root granted then revoked: graceful degradation.

## 9. Regression Verification

Previously completed functionality must be retested after relevant
changes. At minimum: overlay permission flow, panel toggle, theme
persistence, and one monitoring sample per metric.

## 10. Reporting

Every test report must state:
- What was executed, with the command or manual steps.
- The environment (device model, API level, build type).
- Result per item: pass, fail, or not run.
- Failures with the actual error output.
- Known limitations and anything that could not be verified.

Never claim tests were performed when they were not.
