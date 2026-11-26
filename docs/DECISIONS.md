# PocketPanel Technical Decisions

Record significant technical decisions here.

Each entry should include:

- Date.
- Decision.
- Alternatives considered.
- Reason for selection.
- Compatibility information.
- Verification sources.
- Consequences or limitations.

## Initial Decisions

### D001 — Native Android Application
Decision: Use native Android development.

Status: Accepted (2026-11-25, verified in M0 research; see D005).

### D002 — Material 3 Design
Decision: Use Material 3 / Material You.

Status: Accepted.

### D003 — Optional Elevated Access
Decision: Shizuku and root must remain optional.

Status: Accepted.

### D004 — Stable Verified Toolchain
Decision: Use compatible stable versions verified against
official documentation before project generation.

Status: Accepted.

---

## M0 Research Decisions (2026-11-25)

All versions below were checked against official release notes or Maven
metadata on 2026-11-25. No component was installed or built.

### D005 — Toolchain Baseline

Date: 2026-11-25

Decision:
- Android Gradle Plugin 9.5.2 (stable).
- Gradle wrapper 9.6.0.
- JDK 17, `jvmTarget = 17`.
- Kotlin 2.4.21 via AGP built-in Kotlin.
- Compose Compiler plugin `org.jetbrains.kotlin.plugin.compose` 2.4.21.
- Compose BOM 2026.11.01 (Compose 1.13.0, Material3 1.5.0).
- AndroidX: activity-compose 1.13.0, lifecycle 2.11.1, core-ktx 1.19.1,
  datastore-preferences 1.3.0.

Compatibility matrix:

| Component | Selected | Constraints | Verified against | Notes |
|---|---|---|---|---|
| AGP | 9.5.2 | max API 37; Gradle ≥ 9.6.0; Build Tools 36.0.0; JDK 17 | AGP 9.5 release notes; Google Maven metadata | 9.6.0 is alpha only |
| Gradle (wrapper) | 9.6.0 | JVM 17–27 | Gradle compatibility matrix | AGP 9.5 minimum and default |
| JDK | 17 (LTS) | AGP requires 17 | AGP 9.5 notes | `jvmTarget = 17` |
| Kotlin/KGP | 2.4.21 | Compose plugin must match | kotlinlang.org releases; Maven Central | Supplied through AGP built-in Kotlin |
| Compose Compiler plugin | 2.4.21 | equal to Kotlin version | Compose compiler docs | Confirm effective version in M0b |
| Compose BOM | 2026.11.01 | needs compileSdk 37.1+ | BOM POM; AAR metadata (CI) | Compose 1.13.0, Material3 1.5.0 |
| compileSdk | 37.1 | AGP 9.5 supports API 37 minors | CompileSdkSpec reference; CI AAR metadata check | Compose 1.13.0 AARs require 37.1+ |
| targetSdk | 36 | Play requires ≥ 36 from 2026-08-31 | Play target API policy | Avoids early Android 17 opt-in |
| minSdk | 26 | — | Android API level docs | Single overlay type |
| Build Tools | 36.0.0 | AGP 9.5 min/default | AGP 9.5 notes | NDK not required |
| activity-compose | 1.13.0 | — | Google Maven metadata | Stable channel |
| lifecycle | 2.11.1 | — | Google Maven metadata | 2.12.0 is alpha |
| core-ktx | 1.19.1 | — | Google Maven metadata | 1.20.0 is alpha |
| datastore-preferences | 1.3.0 | — | Google Maven metadata | — |
| Shizuku api/provider | 13.1.5 | optional, M8 only | Maven Central metadata | Not a Google Maven artifact |

No prerelease, experimental or RC component is selected.

Alternatives considered:
- AGP 9.6.0: rejected, only `9.6.0-alpha06` exists on Google Maven.
- Gradle 9.9.2: rejected as the wrapper baseline because AGP 9.5 lists
  9.6.0 as both minimum and default; a newer Gradle is untested by AGP.
- Kotlin 2.5.0: rejected, only `2.5.0-RC2` is published.
- Keeping an older AGP 8.x line: rejected because Compose 1.13 requires
  AGP 9 and `compileSdk` 37.1.

Reason: Newest stable combination that is mutually documented as
compatible, without prerelease components.

Compatibility notes: AGP 9.5 supports up to API 37, requires Gradle
9.6.0, Build Tools 36.0.0 and JDK 17. Gradle 9.6 accepts JVM 17–27.
KGP 2.4.21 documentation lists full support for Gradle 7.6.3–9.7.0 and
AGP 8.5.2–9.3.1; AGP 9.5 is newer than that table, which is why the
project relies on AGP's built-in Kotlin default rather than a
hand-picked KGP/AGP pairing (see D007).

Verification sources:
- https://developer.android.com/build/releases/agp-9-5-0-release-notes
- https://docs.gradle.org/current/userguide/compatibility.html
- https://kotlinlang.org/docs/releases.html
- https://kotlinlang.org/docs/gradle-configure-project.html
- https://developer.android.com/develop/ui/compose/compiler
- https://developer.android.com/develop/ui/compose/bom/bom-mapping
- https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml
- https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.11.01/compose-bom-2026.11.01.pom
- Google Maven metadata for activity-compose, lifecycle, core-ktx and
  datastore-preferences (stable channel).

Consequences: The exact Kotlin version contributed by AGP built-in
Kotlin must be confirmed in M0b with `./gradlew buildEnvironment` before
the Compose compiler plugin version is finalized. This is an acceptance
item for M0b, not an assumption.

### D006 — SDK Levels

Date: 2026-11-25

Decision: `compileSdk 37.1` (API 37, minor level 1), `targetSdk 36`,
`minSdk 26`.

Alternatives considered:
- `minSdk 24`: would require legacy `TYPE_PHONE` overlay handling and
  pre-26 window quirks for negligible device coverage gain.
- `minSdk 29`: would make thermal status available unconditionally but
  drops Android 8/9 devices and is not required, since the thermal APIs
  can be gated at runtime.
- `minSdk 31`: rejected, unnecessarily narrow.
- `targetSdk 37`: rejected for now, because Android 17 behavior changes
  are not required and target 36 is the current Play requirement.
- `compileSdk 37` (level 37.0): rejected after CI proved it is
  insufficient. Compose 1.13.0 AAR metadata requires 37.1 or later.

Reason: `minSdk 26` gives a single overlay window type and modern
window behavior. `compileSdk 37.1` is required by Compose 1.13.0 AAR
metadata; the first CI run failed AAR metadata checks for
`androidx.compose.foundation:foundation-layout-android:1.13.0`,
`androidx.compose.ui:ui-android:1.13.0` and related artifacts when
compiled against `android-37.0`, and the second run passed after moving
to `android-37.1`. `targetSdk 36` satisfies Google Play's requirement
for new apps and updates from 2026-08-31 without adopting Android 17
behavior changes early.

Compatibility notes: AGP 9.5 supports API 37 including minor levels.
Minor levels are configured with the documented AGP DSL:

    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

Thermal and notification APIs above `minSdk` are gated by
`Build.VERSION.SDK_INT`. The `compileSdk` minor level does not change
runtime behavior; it only exposes newer platform APIs.

Verification sources:
- https://developer.android.com/reference/tools/gradle-api/9.0/com/android/build/api/dsl/CompileSdkSpec
- https://developer.android.com/about/versions/17
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://developer.android.com/build/releases/agp-9-5-0-release-notes
- CI run 45621464769 (`:app:assembleDebug` success against android-37.1)

Consequences: Android 17-specific behavior is not exercised until
`targetSdk` is raised in a later, separately authorized milestone.

### D007 — AGP Built-in Kotlin, No `kotlin-android` Plugin

Date: 2026-11-25

Decision: Do not apply `org.jetbrains.kotlin.android`. AGP 9 compiles
Kotlin itself. Apply only the Compose Compiler Gradle plugin, pinned to
the Kotlin version actually used by the build.

Alternatives considered:
- Applying `kotlin-android` as in AGP 8.x: rejected, it conflicts with
  built-in Kotlin.
- Overriding the Kotlin version through the buildscript classpath:
  deferred until M0b shows the AGP default is insufficient.

Reason: Built-in Kotlin removes a whole class of AGP/KGP mismatch
failures, and it is the documented AGP 9 default.

Compatibility notes: The Compose Compiler plugin version must equal the
Kotlin compiler version. With built-in Kotlin that version is supplied
by AGP, so it must be read from the build, not guessed. AGP 9.6 release
notes list KGP minimum 2.2.10 and default 2.4.20, which is consistent
with 2.4.21 being a safe target but is not proof for AGP 9.5.

Verification sources:
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://developer.android.com/develop/ui/compose/compiler
- https://developer.android.com/build/releases/agp-9-6-0-release-notes

Consequences: M0b must record the effective Kotlin version and align
the Compose compiler plugin to it before the first successful build.

### D008 — Overlay and Foreground Service Strategy

Date: 2026-11-25

Decision:
- Overlay via `SYSTEM_ALERT_WINDOW` and `TYPE_APPLICATION_OVERLAY`.
- Overlay hosted by a foreground service with type `specialUse`, as the
  only eligible type, subject to Play review.
- Service started only from an explicit user gesture.

Alternatives considered:
- `TYPE_PHONE`/`TYPE_SYSTEM_ALERT`: deprecated and unnecessary at
  `minSdk 26`.
- Accessibility service to draw overlays: rejected, disproportionate
  permission and policy risk.
- No foreground service (view added from an Activity): rejected, the
  overlay would not survive leaving the app.
- No foreground service at all, relying on process importance: kept as
  the fallback if Play rejects `specialUse`.
- Purpose-specific types (`dataSync`, `connectedDevice`, `shortService`,
  `systemExempted`, and others): rejected, they either misdescribe the
  work or have hard eligibility limits.

Reason: A foreground service is not what authorizes the overlay window,
but it is the documented mechanism for keeping user-aware work alive.
`specialUse` is the only type that is not purpose-bound.

Compatibility notes:
- `SYSTEM_ALERT_WINDOW` alone permits `TYPE_APPLICATION_OVERLAY`; no
  foreground service is an API prerequisite for drawing the window.
- Without a foreground service the process is at best
  `IMPORTANCE_VISIBLE` while the window is visible, and nothing
  guarantees that an overlay-only process survives; hence the service.
- From API 34 a foreground service type is mandatory;
  `specialUse` requires `FOREGROUND_SERVICE_SPECIAL_USE` and a
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` manifest property.
- `shortService` is capped at about three minutes and cannot be sticky.
- `systemExempted` requires device owner, profile owner, device admin,
  VPN, emergency-role or similar status; PocketPanel qualifies for none.
- `SYSTEM_ALERT_WINDOW` is a background-start exemption, but on
  Android 15+ the exemption additionally requires a currently visible
  overlay window, so boot-time auto-start is not viable.
- `POST_NOTIFICATIONS` is required for the notification on API 33+.

Verification sources:
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/guide/components/activities/process-lifecycle
- https://developer.android.com/reference/android/app/ActivityManager.RunningAppProcessInfo
- https://support.google.com/googleplay/android-developer/answer/13392821
- https://support.google.com/googleplay/android-developer/answer/13315670
- https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW
- https://developer.android.com/develop/ui/views/notifications/notification-permission

Consequences:
- Play distribution requires an FGS declaration that includes a
  functionality description, a user-impact statement, a selected use
  case and a demonstration video. Google describes `specialUse` as being
  for limited scenarios and reviews all FGS types. Acceptance is not
  guaranteed and must be treated as a release risk, not a settled fact.
- If `specialUse` is rejected, the fallback is to drop the foreground
  service and accept that the overlay only lives with the app process.
  The UI must then state that limitation instead of implying persistence.
- Users must re-enable the overlay manually after a reboot if they had
  it enabled.

### D009 — Monitoring Data Honesty

Date: 2026-11-25

Decision:
- Prefer documented SDK APIs for every metric.
- Use `/proc/stat` only for CPU usage, as an explicitly best-effort
  provider that can report Unavailable.
- Never derive, interpolate or estimate a missing reading.
- GPU utilization is unsupported and must be presented as such.

Alternatives considered:
- Reading `/proc` broadly for memory, thermal and network: rejected
  where a public API exists.
- Hiding unavailable metrics entirely: rejected; users need to know a
  capability is missing on their device.

Reason: The project specification forbids fabricated hardware metrics.

Compatibility notes: `ActivityManager.MemoryInfo` and `BatteryManager`
are public. Thermal status requires API 29, headroom API 30, headroom
thresholds API 35, listeners API 36. No public Android API exposes GPU
load; vendor sysfs nodes are usually unreadable to apps.

Verification sources:
- https://developer.android.com/reference/android/app/ActivityManager.MemoryInfo
- https://developer.android.com/reference/android/os/BatteryManager
- https://developer.android.com/reference/android/os/PowerManager

Consequences: CPU usage may be permanently Unavailable on some OEM
builds; this is accepted and must be visible in the UI and tests.

### D010 — Elevated Access Boundaries

Date: 2026-11-25

Decision: Shizuku and root remain optional adapters behind the
`MetricProvider` interface, implemented only in their authorized
milestones (M8 and M9).

Alternatives considered: none viable; mandatory elevated access
conflicts with the project's standard-mode requirement.

Reason: Standard mode must stay fully functional.

Compatibility notes: Shizuku `dev.rikka.shizuku:api` and `:provider`
13.1.5 are published on Maven Central (not Google Maven) and were last
updated in 2023. Shizuku requires the separate Shizuku app, started by
the user via root or Android 11+ wireless debugging. Its shell UID is
not root and has limited permissions.

Verification sources:
- https://github.com/RikkaApps/Shizuku
- https://shizuku.rikka.app/guide/setup/
- https://repo1.maven.org/maven2/dev/rikka/shizuku/api/maven-metadata.xml

Consequences: No Shizuku or root dependency is added in M0. If added
later, the artifact's age and maintenance status must be reassessed.

### D011 — Version Pinning and Upgrade Discipline

Date: 2026-11-25

Decision:
- The Gradle wrapper is committed and pinned to 9.6.0; the host's
  Gradle installation is never used for builds.
- Dependency versions are declared once in a version catalog.
- No prerelease (`alpha`, `beta`, `rc`, `dev`) component may be selected
  without explicit approval.
- Versions are upgraded one component family at a time, with a recorded
  reason.

Alternatives considered: floating ranges (rejected, unreproducible) and
lockfiles only (insufficient for plugins).

Reason: Reproducible builds and traceable failures.

Consequences: Upgrades are manual and deliberate.

M0b note: the wrapper could not be generated in Termux because no JDK
or Gradle is installed, and the M0b verification therefore used
`gradle/actions/setup-gradle` with a pinned version. Resolved in M1: the
wrapper was generated once by the temporary `bootstrap/wrapper`
workflow, committed (`gradlew`, `gradlew.bat`, `gradle/wrapper/`), and CI
now runs `./gradlew` with the wrapper choosing Gradle 9.6.0.

### D012 — Build and Verification Environment

Date: 2026-11-25
Status: Accepted (user-approved).

Decision: Compilation, test execution and APK generation run on
GitHub-hosted x86_64 Ubuntu runners (`.github/workflows/verify-toolchain.yml`,
`ubuntu-24.04`). OpenCode continues to run locally in Termux. No Android
SDK, JDK or Gradle is installed locally, and no ARM build-tools or
`aapt2` override is used.

Verified by: CI run 45621464769 and run 45621953748, both
`assembleDebug` successes on `ubuntu-24.04` with Temurin JDK 17.0.21 and
Gradle 9.6.0.

Alternatives considered:
- Local Termux build with `openjdk-17` plus a manually assembled SDK and
  the `android.aapt2FromMavenOverride` workaround: rejected, Google does
  not publish Linux aarch64 build-tools and the override is unverified.
- A separate x86_64 development machine: valid but unavailable to the
  current workflow.

Reason: M0b requires a real, observed build. A pinned GitHub-hosted
runner provides it without unreliable local tooling.

Consequences:
- Local commands cannot build the project; every build claim must come
  from a workflow run with captured logs.
- Build results are published to the `ci-results` branch and uploaded as
  workflow artifacts, so results remain inspectable without console
  access.
- Repository secrets are not required; the workflow uses the default
  `GITHUB_TOKEN` with `contents: write` only to push results.
- Emulator or device launch testing is not covered by this environment
  and remains unverified.
- Since D016 the workflow is manual-only (`workflow_dispatch`) and runs
  only when the project owner authorizes a run.

### D013 — Toolchain Verification Results

Date: 2026-11-25

Decision: Accept the verified toolchain recorded in D005 with one
correction: `compileSdk` is API 37.1, not 37.0. M0b build verification
passed on GitHub Actions.

Method: `.github/workflows/verify-toolchain.yml` on `ubuntu-24.04`,
Gradle installed and pinned by `gradle/actions/setup-gradle@v6`
(`gradle-version: 9.6.0`), Temurin JDK 17 via `actions/setup-java@v6`.
The Android SDK is the one preinstalled on the runner image, which
already contains `android-37.1` and `build-tools;36.0.0`. No SDK or JDK
was installed locally.

Resolved and confirmed by the build:

| Component | Version observed |
|---|---|
| Android Gradle Plugin | 9.5.2 (`com.android.tools.build:gradle:9.5.2` on buildscript classpath) |
| Gradle | 9.6.0 |
| JDK | Eclipse Temurin 17.0.21+9 |
| Kotlin Gradle plugin | 2.4.21 (upgraded from AGP's declared 2.2.10) |
| Kotlin compiler | 2.4.21 (daemon classpath, run 45620584351) |
| kotlin-stdlib (app) | 2.4.21 |
| Compose compiler plugin | 2.4.21 (`kotlin-compose-compiler-plugin-embeddable`) |
| Compose BOM | 2026.11.01 |
| Compose UI/foundation/runtime | 1.13.0 |
| Material3 | 1.5.0 |
| activity-compose | 1.13.0 |
| lifecycle-runtime-compose | 2.11.1 |
| core-ktx | 1.19.1 |
| compileSdk | API 37.1 (`android-37.1`) |
| targetSdk | 36 |
| minSdk | 26 |
| Build Tools | 36.0.0 |

Commanded results (run 45621953748, commit 3b2c0b1):

- `gradle buildEnvironment` — success; classpath shows AGP 9.5.2,
  KGP 2.4.21 and compose-compiler-gradle-plugin 2.4.21.
- `gradle :app:dependencies --configuration debugCompileClasspath` —
  success; resolves `kotlin-stdlib:2.4.21`, Compose BOM 2026.11.01,
  Compose 1.13.0 and Material3 1.5.0.
- `gradle :app:compileDebugKotlin --info` — success; Compose compiler
  accepted Kotlin 2.4.21.
- `gradle :app:assembleDebug` — success; `app-debug.apk`,
  12,884,656 bytes,
  sha256 `da26cee260c57aba22603a98602335d3f4ef533d4c7072077e3ae94122324250`.
- `gradle :app:testDebugUnitTest` — `NO-SOURCE`; there are no tests yet,
  so this is not evidence of passing tests.

Findings:

1. Compose 1.13.0 requires `compileSdk` 37.1+. Compiling against 37.0
   failed the AAR metadata check with an explicit requirement for 37.1
   or later. Fixed with the documented `compileSdk { release(37) { minorApiLevel = 1 } }`
   block; see D006.
2. AGP 9.5.2 declares a runtime dependency on KGP 2.2.10, so 2.4.21 is
   not the default. The selected Kotlin version is applied explicitly via
   AGP's documented higher-KGP buildscript classpath, and the resulting
   buildscript classpath resolves to exactly 2.4.21.
3. `gradle --version` reports Kotlin 2.3.21. That is Gradle's embedded
   Kotlin for build scripts and is unrelated to the application compiler.
4. The compose plugin logs "Detected Android Gradle Plugin compose
   compiler configuration", which is expected with AGP 9 built-in
   Kotlin.
5. CI job logs are not readable without GitHub authentication; the
   workflow therefore publishes filtered evidence to the `ci-results`
   branch and full logs plus the APK as workflow artifacts.

Consequences:
- The build toolchain is verified, with the caveat that launch behavior
  has not been tested. M0b's "basic application launches" criterion
  remains open.
- Any future claim of a passing build must cite a workflow run and its
  published results.

### D014 — M1 Foundation Dependencies and Settings Persistence

Date: 2026-11-25

Decision:
- Settings are persisted with DataStore Preferences 1.3.0.
- The single app-level `PocketPanelViewModel` is created through
  `lifecycle-viewmodel-compose` 2.11.1, matching the already selected
  lifecycle version.
- JVM unit tests use JUnit 4.13.2.
- Instrumented tests use `androidx.test.ext:junit` 1.3.0,
  `androidx.test:runner` 1.7.0 and Compose `ui-test-junit4` from BOM
  2026.11.01 (Compose 1.13.0).
- Theme mode is stored as the `ThemeMode` enum name; dynamic color is a
  boolean. Defaults are `SYSTEM` and `true`.
- The fallback palette is a blue Material 3 scheme; dynamic color is
  used on API 31+ only.

Alternatives considered:
- `SharedPreferences`: rejected; architecture D009/§9 already selects
  DataStore for settings and an asynchronous, typed flow is the chosen
  contract.
- Separate `MainViewModel` and `SettingsViewModel`: rejected for M1;
  both would read the same repository and the screen count is two, so a
  single view model with one immutable UI state is smaller and testable.
- Navigation Compose: rejected; docs/ARCHITECTURE.md already defers it
  until the screen count justifies it.
- Adding `material-icons-core`: avoided; settings and back are standard
  Material vector drawables in `res/drawable`, so no extra artifact is
  needed.
- Robolectric for persistence tests: rejected; the emulator job exists
  and real Android DataStore behavior is what must be proven.

Reason: Only official AndroidX and JUnit components, all stable and
already compatible with the pinned toolchain, and no new framework.

Verification sources (Google Maven `maven-metadata.xml`, checked
2026-11-25; Maven Central for JUnit):
- https://dl.google.com/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml (1.3.0 stable)
- https://dl.google.com/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose/maven-metadata.xml (2.11.1 stable)
- https://dl.google.com/android/maven2/androidx/test/ext/junit/maven-metadata.xml (1.3.0 stable)
- https://dl.google.com/android/maven2/androidx/test/runner/maven-metadata.xml (1.7.0 stable)
- https://dl.google.com/android/maven2/androidx/compose/ui/ui-test-junit4/maven-metadata.xml (1.13.0, BOM-managed)
- https://repo1.maven.org/maven2/junit/junit/maven-metadata.xml (4.13.2)

Consequences:
- Disk persistence is proven by an instrumented test that shuts one
  DataStore down and reads the same file through a second instance.
- The effective color values are not asserted by automated tests; theme
  application is covered by manual review and by state-level UI checks.
- Corrupt settings data degrades to defaults instead of crashing.

### D015 — Emulator Verification Environment

Date: 2026-11-25
Status: Withdrawn 2026-11-25 by D016. Emulator and device testing are
suspended. The analysis below is kept as history; the emulator job no
longer exists in the workflow.

Decision: Instrumented tests and a cold-launch check run on an Android
36 (API 36, `google_apis`, x86_64) emulator on `ubuntu-24.04`, with KVM
enabled by the documented udev rule. The emulator, system image and AVD
are created by explicit workflow steps rather than a third-party action,
so every stage has a bounded timeout and its own captured log.

Observed during setup:
- `ReactiveCircus/android-emulator-runner@v2.9.0` fails immediately; it
  uses the retired Node 12 runtime.
- v2.38.0 (Node 24) ran without producing any log or result for over 50
  minutes on the first attempt. Because the action hides its stages and
  the job log is not accessible without authentication, it was replaced
  with the explicit steps below.
- The runner image has no emulator and no system image preinstalled
  (`ANDROID_HOME=/usr/local/lib/android/sdk`, platforms up to
  android-37.2, cmdline-tools 12.0). They must be installed.
- `avdmanager create avd` with `echo no |` hung after
  `Auto-selecting single ABI x86_64` and created no AVD: it reads stdin
  more than once and loops on EOF. Fixed with a fixed multi-line answer
  stream, an explicit `--abi x86_64`, a pre-created `$HOME/.android/avd`
  directory, and a step timeout.

Workflow stages (all logged under `ci-results/emulator/`):
1. Record `ANDROID_HOME`, `/dev/kvm`, CPU virtualization flags and the
   preinstalled SDK.
2. Enable KVM with a udev rule.
3. `sdkmanager` install of `emulator`, `platform-tools` and
   `system-images;android-36;google_apis;x86_64`.
4. `avdmanager create avd` with the `pixel_6` profile.
5. Start the emulator headless (`-no-window -gpu swiftshader_indirect
   -no-snapshot -noaudio -no-boot-anim -no-metrics`).
6. Poll `sys.boot_completed` for at most 15 minutes.
7. Disable animations, dismiss the keyguard, run
   `./gradlew :app:connectedDebugAndroidTest`.
8. Force-stop and cold-start `dev.pocketpanel/.MainActivity`, require
   `Status: ok`, require a live PID, and require an empty crash buffer.
9. On failure, capture the emulator log tail, boot log, device list, AVD
   directory and crash buffer; always stop the emulator and publish.

Alternatives considered:
- The third-party emulator action: kept as the first choice but
  rejected after the two failures above; it also adds an external
  dependency the workflow can avoid.
- Physical device: unavailable to automation.
- Gradle Managed Devices: viable, but adds AGP device orchestration and
  its own provisioning path; explicit steps expose failures more
  directly.
- Multiple API levels: deferred to the M11 matrix; one API level keeps
  the job and runtime minimal, as requested.

Reason: A real install-and-launch check is the only way to verify the
M1 launch and persistence claims. API 36 matches `targetSdk`.

Verification sources:
- https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml
  (confirms `system-images;android-36;google_apis;x86_64`)
- https://docs.github.com/en/actions/reference/runners/github-hosted-runners
  (Linux x64 runners provide hardware acceleration; `ANDROID_HOME`
  preinstalled)
- https://developer.android.com/studio/run/emulator-commandline
- https://github.com/ReactiveCircus/android-emulator-runner/blob/v2.38.0/action.yml
  (node24 runtime; inspected during diagnosis)

Consequences:
- Every push that changes app or build files runs a second, slower job;
  it starts only after the build job succeeds.
- The first run downloads the emulator and system image (~2 GB) and is
  therefore the slowest.
- Emulator evidence is published under `ci-results/emulator/` (status,
  environment, install/AVD/boot/launch logs, test report, logcat,
  diagnostics) and uploaded as an artifact.
- API 26/29/34/35 behavior remains unverified until the M11 matrix.

### D016 — CI Is Manual and Emulator Testing Is Suspended

Date: 2026-11-25
Status: Accepted (user-directed).

Decision:
- `.github/workflows/verify-toolchain.yml` is manual-only
  (`workflow_dispatch`). No `push`, `pull_request` or `schedule`
  trigger exists, and none may be added.
- No workflow is started, and no commit is pushed in order to start
  one, without explicit permission for that run.
- The emulator job is removed. No emulator, AVD, system image or
  instrumented test runs in CI for now. The instrumented tests remain
  in the repository but are not executed.
- Development relies on `scripts/check-static.sh` (structure,
  cross-references, policy checks) plus code review. Build, lint, unit
  tests and the debug APK are produced only in an authorized CI run.
- CI is requested when the app has meaningful functionality to verify,
  not after every change.

Reason: three consecutive emulator attempts failed for environmental
reasons (retired action runtime, an action that produced no logs, and an
`avdmanager` stdin hang). The app is still a foundation shell; repeated
remote runs cost more than they verify. Static checks catch a useful
class of defects locally, and a single authorized run at a meaningful
checkpoint is cheaper and easier to interpret.

Alternatives considered:
- Keep the emulator job but skip it by default: rejected; a dormant job
  still needs maintenance and invites accidental runs.
- Gradle Managed Devices: same cost and provisioning problem, deferred.
- Drop automated checks entirely: rejected; `check-static.sh` is cheap
  and catches missing resources and broken wiring.

Consequences:
- Compilation, lint, unit-test and runtime behavior are unverified
  between authorized runs. Reports must say so explicitly.
- The M1 APK does not exist yet; it is produced by the first authorized
  run.
- `ReactiveCircus/android-emulator-runner`, system-image downloads and
  KVM setup are no longer dependencies of the project.
- Reintroducing device testing requires a new decision and explicit
  authorization.

### D017 — M1 Lint Follow-ups and Data Extraction Policy

Date: 2026-11-26
Status: Accepted.

Context: authorized run 45669463189 produced 5 lint warnings and 0
errors. Two were actionable without touching the approved toolchain.

Decision:
- App data is excluded from both cloud backup and device-to-device
  transfer with `res/xml/data_extraction_rules.xml`, referenced by
  `android:dataExtractionRules` on `<application>`. `allowBackup` stays
  `false`. The attribute is used on API 31+ and ignored below it, which
  is valid with `minSdk 26`.
- The adaptive icon folder is `mipmap-anydpi`, not
  `mipmap-anydpi-v26`: `minSdk` is already 26, so the qualifier was
  redundant (lint `ObsoleteSdkInt`).
- The remaining warnings are accepted deliberately:
  - Gradle 9.9.2 is available but 9.6.0 is pinned (D011).
  - AGP 9.5.3 is available but 9.5.2 is the verified version (D005,
    D013).
  - `targetSdk 36` is older than `compileSdk 37.1` by policy (D006).

Reason: PocketPanel stores user settings locally and should not leak
them through backup or device migration. Fixing the two actionable
warnings keeps lint output meaningful; changing the toolchain or
`targetSdk` to silence the other three would contradict approved
decisions.

Verification: the warnings were observed in run 45669463189.
`scripts/check-static.sh` passes 49 checks after the changes (including
a new `@xml` reference check). The changes are resource/manifest-only
and have not yet been compiled; the next authorized run verifies them.

Consequences:
- Theme and future settings do not migrate to a new device. This is
  acceptable because appearance is device-specific; it can be revisited
  if a future feature stores user-authored data.
- Lint remains at 3 accepted warnings instead of 5.
