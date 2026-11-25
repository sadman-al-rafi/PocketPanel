# PocketPanel Testing Strategy

Testing must reflect what was actually executed. Any unrun or
unavailable test is reported as such, never implied to have passed.

## 1. Build Verification

Builds run on GitHub-hosted `ubuntu-24.04` x86_64 runners through
`.github/workflows/verify-toolchain.yml` (D012). There is no local JDK,
Android SDK or Gradle in the Termux environment, so a build claim can
only come from a workflow run.

The committed Gradle wrapper selects Gradle 9.6.0 (D011). The workflow
has two jobs:

Build job:

    ./gradlew --version                     # wrapper + launcher JVM
    ./gradlew buildEnvironment              # resolved plugins, KGP version
    ./gradlew :app:dependencies --configuration debugCompileClasspath
    ./gradlew :app:assembleDebug            # APK
    ./gradlew :app:lintDebug                # Android lint
    ./gradlew :app:testDebugUnitTest        # JVM unit tests

Emulator job (after the build job succeeds, D015):

    ./gradlew :app:connectedDebugAndroidTest
    adb shell am start -W -n dev.pocketpanel/.MainActivity
    adb shell pidof dev.pocketpanel
    adb logcat -b crash -d                  # must not contain FATAL EXCEPTION

Requirements for a build to count as verified:
- The output is observed in a completed workflow run, not assumed.
- AGP, Gradle, JDK and Kotlin versions in the output match
  docs/DECISIONS.md D005 and D013.
- No prerelease dependency appears in the resolved graph.
- A command that reports `NO-SOURCE`, `SKIPPED` or `UP-TO-DATE` is not
  counted as a passing test.

Results are published to the `ci-results` branch (`status.txt`,
`versions.log`, filtered dependency and build-environment logs, test
reports, lint report, APK size and sha256, and `emulator/` evidence),
and full logs plus the debug APK are uploaded as workflow artifacts.
Job console logs require GitHub authentication and are not used as the
sole evidence.

Current status (2026-11-25):
- M0b: `assembleDebug` succeeded on `ubuntu-24.04` with AGP 9.5.2,
  Gradle 9.6.0, Temurin JDK 17.0.21 and Kotlin 2.4.21 (runs 45621464769
  and 45621953748).
- M1: results pending in this section; recorded after the M1 workflow
  runs.

## 2. Configuration Verification

- `compileSdk 37.1` (API 37 with `minorApiLevel = 1`), `targetSdk 36`,
  `minSdk 26` in the app module.
- JDK 17 toolchain, `jvmTarget = 17`.
- Manifest contains exactly the permissions listed in
  docs/ARCHITECTURE.md section 8 and no others. M1 declares none; the
  overlay and notification entries become applicable with M2.
- When the overlay service exists (M2+), it must declare
  `foregroundServiceType="specialUse"` and the
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property.
- No `kotlin-android` plugin applied.
- Dependency versions come from the version catalog only.
- M1 dependencies: DataStore Preferences 1.3.0, lifecycle-viewmodel
  compose 2.11.1, JUnit 4.13.2, androidx.test ext-junit 1.3.0 and
  runner 1.7.0 (D014).

## 3. Application Verification

Automated on the API 36 emulator job (D015):
- Cold launch succeeds (`Status: ok`), the process stays alive and the
  crash buffer is empty.
- Main screen renders the app title and welcome card.
- Settings opens and shows all three theme options.
- Selecting Dark updates the selected state and the main-screen
  appearance summary.
- Theme mode is written to disk and read back by a fresh DataStore
  instance (process-death persistence proxy).

Manual or visual review (not asserted by automated tests):
- Colors actually switch between light and dark; dynamic color applies
  on API 31+; both palette variants are coherent.
- Landscape and tablet widths keep content inside a readable column.
- No hardcoded colors outside `ui/theme` (review).
- TalkBack labels present on interactive elements; touch targets at
  least 48 dp.

The remaining API-level matrix (section 6) stays manual until M11.

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
