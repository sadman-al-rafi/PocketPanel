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
| Compose BOM | 2026.11.01 | needs compileSdk 37 | BOM POM; BOM mapping page | Compose 1.13.0, Material3 1.5.0 |
| compileSdk | 37 | AGP 9.5 supports 37 | Android 17 page | Required by Compose 1.13 |
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
  AGP 9 and `compileSdk 37`.

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

Decision: `compileSdk 37`, `targetSdk 36`, `minSdk 26`.

Alternatives considered:
- `minSdk 24`: would require legacy `TYPE_PHONE` overlay handling and
  pre-26 window quirks for negligible device coverage gain.
- `minSdk 29`: would make thermal status available unconditionally but
  drops Android 8/9 devices and is not required, since the thermal APIs
  can be gated at runtime.
- `minSdk 31`: rejected, unnecessarily narrow.
- `targetSdk 37`: rejected for now, because Android 17 behavior changes
  are not required and target 36 is the current Play requirement.

Reason: `minSdk 26` gives a single overlay window type and modern
window behavior. `compileSdk 37` is required by Compose 1.13.
`targetSdk 36` satisfies Google Play's requirement for new apps and
updates from 2026-08-31 without adopting Android 17 behavior changes
early.

Compatibility notes: AGP 9.5 max supported API is 37. Thermal and
notification APIs above `minSdk` are gated by `Build.VERSION.SDK_INT`.

Verification sources:
- https://developer.android.com/about/versions/17
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://developer.android.com/build/releases/agp-9-5-0-release-notes

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

### D012 — Build and Verification Environment

Date: 2026-11-25
Status: Proposed — requires user decision.

Decision: Not yet decided. The current development environment is
Termux on aarch64 with no JDK and no Android SDK. Google does not
publish Android SDK build-tools (including `aapt2`) for Linux aarch64,
and the Termux `aapt2` package with `android.aapt2FromMavenOverride`
is an unverified workaround.

Alternatives considered:
- Local Termux build with `openjdk-17` plus a manually assembled SDK and
  the aapt2 override.
- x86_64 CI runner executing `assembleDebug` and tests.
- Building on a separate x86_64 development machine.

Reason: M0b acceptance requires a verified build, which cannot be
claimed without a working toolchain.

Consequences: M0b stays blocked until a build path is approved.
No installation is performed before that approval.
