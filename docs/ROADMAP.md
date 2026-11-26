# PocketPanel Development Roadmap

## Status Legend
- [ ] Not started
- [~] In progress
- [x] Completed and verified
- [!] Blocked

Milestones must be completed sequentially.
No milestone may be started without explicit authorization.

---

## M0a — Technical Research and Foundation Documentation
Status: [x] Completed 2026-11-25 (research only; no project generated)

### Objectives
- Verify the current stable Android toolchain.
- Verify Kotlin, Gradle, AGP, JDK and Compose compatibility.
- Identify SDK installation requirements.
- Define the minimum supported Android version.
- Establish the initial project architecture.
- Establish build and test procedures.

### Acceptance Criteria
- [x] Compatibility matrix documented (docs/DECISIONS.md D005).
- [x] Selected toolchain verified against official sources.
- [x] SDK level policy decided (D006: compileSdk 37, targetSdk 36,
      minSdk 26; compileSdk later corrected to 37.1 in D006/D013 after
      CI exposed the Compose 1.13.0 AAR metadata requirement).
- [x] Overlay, foreground service and monitoring APIs verified
      (D008, D009).
- [x] Architecture documented (docs/ARCHITECTURE.md).
- [x] Decisions documented (docs/DECISIONS.md D005–D012).
- [x] Testing strategy updated (docs/TESTING.md).
- [x] Risks, limitations and blockers recorded.

### Verification Performed
- AGP 9.5.2, Gradle 9.6.0, JDK 17, Kotlin 2.4.21 and Compose BOM
  2026.11.01 checked against official release notes and Maven metadata.
- AGP 9.6.0 confirmed to be prerelease only (alpha06).
- Overlay permission, `TYPE_APPLICATION_OVERLAY` and Android 14+
  foreground service types confirmed against Android documentation.
- Battery, memory and thermal API availability confirmed per API level.
- GPU monitoring confirmed to have no public Android API.
- Shizuku 13.1.5 confirmed on Maven Central and reviewed for viability.
- No package was installed, configured or generated during this
  milestone.

### Explicitly Not Verified
- No build, compilation or test has been run; no toolchain is
  installed.
- Effective Kotlin version from AGP built-in Kotlin is unknown until
  M0b runs `buildEnvironment`.
- The Termux aarch64 build path is unproven (see D012).

### Open Items Carried Into M0b
- Choose a build and verification environment (D012).
- Confirm the built-in Kotlin version and align the Compose compiler
  plugin to it.
- Finalize the application ID and package structure.

Resolution: the build environment is D012 (GitHub Actions
`ubuntu-24.04`) and the effective Kotlin/Compose versions are recorded
in D013. The application ID is `dev.pocketpanel`, already used by the
M0b scaffold. The "no build run" and "Termux aarch64 unproven" notes
above describe the state at the end of M0a and remain factually true for
that milestone; the build itself was verified in M0b.

---

## M0b — Project Generation and Build Verification
Status: [~] Build verified 2026-11-25; launch smoke test deferred (D016)

### Objectives
- Create the native Android project with the pinned toolchain.
- Record the effective Kotlin/KGP version from the build.
- Generate and verify the Gradle wrapper at 9.6.0. (Resolved in M1;
  see Remaining below.)
- Establish the initial project architecture from
  docs/ARCHITECTURE.md.
- Produce a verified debug build.

### Acceptance Criteria
- [x] Project scaffolding generated with AGP 9.5.2, Gradle 9.6.0,
      JDK 17. (Temporary verification scaffold only; no features. The
      wrapper and the real project structure were delivered in M1.)
- [x] Version catalog contains only the versions recorded in D005,
      with `compileSdk` corrected to 37.1 per D006 and D013.
- [x] Build classpath recorded, confirming Kotlin 2.4.21 and the
      Compose compiler plugin 2.4.21 (CI run 45621953748).
- [x] Project builds successfully with real, observed output
      (`assembleDebug` success; APK 12,884,656 bytes, sha256
      `da26cee2…`).
- [ ] Basic application launches. Not tested: no emulator or device is
      provisioned and device testing is suspended (D016).
- [x] No prerelease dependency resolved.
- [x] Verification method recorded in docs/TESTING.md and
      docs/DECISIONS.md (D012, D013).

### Verification Performed
- Run 45620269443: failed at the SDK-install step (`sdkmanager` not on
  the runner PATH). The runner image already contains the required SDK,
  so the step was removed.
- Run 45620584351: Kotlin/Compose compilation succeeded; packaging
  failed the AAR metadata check because Compose 1.13.0 requires
  `compileSdk` 37.1 or later.
- Run 45621464769: full success after switching to API 37.1; APK
  produced.
- Run 45621953748: full success with root buildscript evidence,
  confirming AGP 9.5.2, KGP 2.4.21 and Compose plugin 2.4.21.
- `testDebugUnitTest` reported `NO-SOURCE`; no tests exist yet, so no
  test-pass claim is made.

### Remaining Before This Milestone Closes
- Launch smoke test on an emulator or physical device. Suspended by
  D016: no emulator is provisioned and no device test is authorized.
  This criterion is **not** satisfied; it stays open until device
  testing is reintroduced.
- Gradle wrapper: resolved in M1. `gradlew`, `gradlew.bat` and
  `gradle/wrapper/` are committed (D011) and CI uses `./gradlew`.
- Temporary verification scaffold: replaced by the M1 project structure.

---

## M1 — Application Foundation and Material You
Status: [~] Implementation and build verified; runtime verification
blocked by the suspended device-testing policy (D016)

### Objectives
- Establish Material 3 design system.
- Implement System, Light and Dark theme settings.
- Implement dynamic color support where available.
- Create the application settings screen.
- Create the main application screen.

### Prerequisites (requested with M1 authorization)
- [x] Generate the Gradle wrapper at 9.6.0 and commit it (generated
      by the temporary `bootstrap/wrapper` workflow, run evidence in
      that branch; wrapper files merged into main).
- [x] CI uses the committed wrapper (`gradle/actions/setup-gradle` with
      no version override; `./gradlew` used for every command).
- [x] Emulator smoke test: withdrawn at the user's direction after three
      environmental failures (D016). No emulator job exists.
- [x] Workflow kept minimal: one manual `workflow_dispatch` job that
      builds, lints and runs JVM unit tests.

### Acceptance Criteria
- [x] App uses Material 3 (`MaterialTheme` with a Material 3 color
      scheme, typography and shapes; no decorative styling).
- [~] Theme switching is implemented for System / Light / Dark and
      resolves through `ThemeMode.resolveDarkTheme` (5 JVM unit tests
      pass), but the UI behavior has not been observed on a device.
- [~] Theme selection persists through DataStore; the persistence test
      is written but has not run, so persistence is not yet verified at
      runtime.
- [x] UI is responsive by construction: a 640 dp content column, 16/32
      dp adaptive padding, scrollable screens, edge-to-edge insets.
      Visual confirmation at other widths is still pending.
- [x] No unnecessary decorative styling: no gradients, glassmorphism,
      custom icon packs or decorative animation.
- [x] Dynamic color is implemented for API 31+ with a coherent blue
      fallback palette below it, and the setting is disabled with an
      explanation when unavailable. Runtime appearance is unverified.
- [x] Build, lint and JVM tests pass: run 45669463189 (commit
      `ba95fb4`), `assembleDebug` + `lintDebug` (0 errors) +
      `testDebugUnitTest` (5/5). Instrumented tests are not executed
      (D016).

### Verification Performed
- Authorized manual run 45669463189 (2026-11-25, run 10, commit
  `ba95fb4`): success. APK 13,903,982 bytes, sha256
  `d29438c6719868a1bc879a650a00dd77970e862bf853b6a16420b254b6b02bb3`.
  Resolved: AGP 9.5.2, Gradle 9.6.0, JDK 17.0.21, Kotlin 2.4.21,
  Compose BOM 2026.11.01 / Compose 1.13.0 / Material3 1.5.0.
- Lint: 0 errors, 5 warnings. Two warnings were fixed afterwards
  (obsolete `mipmap-anydpi-v26` qualifier, missing data-extraction
  rules; D017). The remaining three are intentional: Gradle 9.9.2 and
  AGP 9.5.3 are not adopted without approval, and `targetSdk 36` is the
  D006 policy.
- Static checks: `scripts/check-static.sh`, 49 checks passing
  (2026-11-26). Structural only; no compiler involved.
- Instrumented tests: written, never executed. No result is claimed.
- Emulator launch: not attempted; suspended by D016.

### Outstanding
- Runtime confirmation of launch, theme switching, persistence and
  dynamic color. This needs either a physical device (for example
  installing the CI APK manually) or a reintroduced, authorized device
  test; M1 cannot be marked `[x]` before that.
- The post-run warning fixes (D017) are not yet covered by an
  authorized build; they are resource/manifest-only and will be
  verified by the next authorized run.

---

## M2 — Floating Overlay Foundation
Status: [✓] Implemented 2026-11-26 (commit `c254da9`); M2 authorized manual
  run 45756276354 (run 13): build successful (0 lint errors, 5 warnings),
  5/5 JVM tests passed, APK 13,961,294 bytes, sha256 `49d058a479459b6507e3477e5f54fcc6bc70bae248100bd574c23fd769ec4838`.

### Objectives
- Implement overlay permission handling.
- Create floating edge handle.
- Implement panel opening and closing.
- Implement drag positioning.
- Implement overlay lifecycle handling.

### Deliverables
- `overlay/OverlayPermission.kt`: `canDrawOverlays` check and settings
  intent with a graceful failure path.
- `overlay/OverlayService.kt`: `specialUse` foreground service,
  `START_NOT_STICKY`, notification with a Stop action, settings
  collection, explicit stop on permission or window failure.
- `overlay/OverlayWindowController.kt`: single
  `TYPE_APPLICATION_OVERLAY` window, usable-area placement, drag and
  snap, synchronous removal.
- `overlay/OverlayContent.kt`: handle and panel Compose UI, fixed dp
  sizes, screen-coordinate drag gesture, accessibility semantics.
- `overlay/OverlayLifecycleOwner.kt`: service-scoped lifecycle and
  saved-state owners for the overlay `ComposeView`.
- `overlay/OverlayGeometry.kt`: pure placement and snap math.
- `settings/HandleEdge.kt` and DataStore keys for `overlayEnabled`,
  edge and vertical fraction; `setHandlePosition` writes both atomically.
- Main screen overlay card, permission button and notification caveat;
  manifest permissions and service declaration.
- JVM tests `OverlayGeometryTest` and `HandleEdgeTest`.

### Acceptance Criteria
- [~] Overlay permission is handled correctly. Implemented; needs
      device verification of grant, denial and revocation.
- [~] Floating handle appears when enabled. Implemented; needs device
      verification.
- [~] Panel opens and closes. Implemented via handle tap and Hide
      button; needs device verification.
- [~] Handle positioning works. Implemented with screen-coordinate
      drag, edge snap and persistence; geometry covered by JVM tests.
      Needs device verification.
- [~] Permission denial is handled. Implemented; needs device
      verification.
- [~] Overlay survives expected lifecycle transitions. Implemented for
      rotation and process death (restored on app resume); no boot
      restore by design. Needs device verification.
- [ ] No unacceptable resource consumption. Not measured; device
      testing is suspended (D016).
- [ ] Build and tests pass. Not yet run for M2; requires an authorized
      CI run.

### Verification Performed
- `scripts/check-static.sh`: 75 checks passed locally (structure,
  resources, manifest wiring, workflow policy).
- JVM overlay geometry and handle-edge tests are written but not run
  locally; no JDK is available.
- Authorized manual run 45753602748 (run 11, commit `ef88643`) failed
  at `assembleDebug` with `Unresolved reference
  'ViewTreeLifecycleOwner'` in `OverlayWindowController.kt`. Lifecycle
  2.11.1 no longer exposes that class; the supported API is the
  `View.setViewTreeLifecycleOwner` extension.
- Authorized manual run 45754823414 (run 12, commit `5ec59d8`) then
  failed at `assembleDebug` with a JVM signature clash:
  `private var expanded` generates `setExpanded(Z)V`, which collided
  with `private fun setExpanded(Boolean)`. The function is now
  `applyExpanded`.
- Static review of the Compose 1.13 source found that `ComposeView`
  throws when no `ViewTreeSavedStateRegistryOwner` is present, even
  when nothing is restored. `OverlayLifecycleOwner` now provides both
  owners and `androidx.savedstate:savedstate:1.5.0` is declared
  explicitly (D019). This has not been compiled yet.
- No successful M2 build, lint, unit-test or device result exists yet.

### Deferred Beyond M2 Acceptance
- Outside-tap collapse (a non-focusable overlay cannot receive outside
  touches).
- Screen-off collapse.
- Boot restore (no boot receiver by design, D008/D018).
- Permission-loss detection while the app is backgrounded.
- Expanded-panel sizing on unusually small displays.

---

## M3 — Basic System Monitoring
Status: [~] In Progress 2026-11-26; CI authorized run pending on commit `d124481` (run 16 pending after `getInt` fix).

### Objectives
- CPU usage monitoring.
- RAM monitoring.
- Battery information.
- Monitoring state management.
- Configurable monitoring intervals.

### Acceptance Criteria
- [ ] Supported readings are real.
- [ ] Unsupported readings are clearly identified.
- [ ] Monitoring updates correctly.
- [ ] Monitoring can be stopped.
- [ ] No fake production readings.
- [ ] Build and tests pass.

---

## M4 — Temperature and Hardware Discovery
Status: [~] In Progress 2026-11-26; CI authorized run pending on commit `c254da9`.

### Objectives
- Discover available thermal information.
- Identify supported temperature sources.
- Implement capability detection.
- Provide clear unsupported states.

### Acceptance Criteria
- [ ] Thermal sources are discovered safely.
- [ ] Available readings are displayed correctly.
- [ ] Unsupported sources do not crash the app.
- [ ] No fabricated readings.
- [ ] Build and tests pass.

---

## M5 — Network Monitor
Status: [ ] Not started

### Objectives
- Network throughput monitoring.
- Basic connection information.
- Network monitoring UI.

### Acceptance Criteria
- [ ] Readings are accurate to the supported source.
- [ ] Interface handles network changes.
- [ ] Monitoring lifecycle is respected.
- [ ] Build and tests pass.

---

## M6 — Quick Tools
Status: [ ] Not started

### Objectives
- Timer.
- Stopwatch.
- Quick notes.
- Additional approved tools.

### Acceptance Criteria
- [ ] Tools are functional.
- [ ] UI remains consistent.
- [ ] Tool state behaves appropriately.
- [ ] Build and tests pass.

---

## M7 — GPU and Advanced Metrics
Status: [ ] Not started

### Objectives
- Investigate GPU monitoring sources.
- Implement supported GPU readings.
- Investigate additional hardware metrics.
- Evaluate optional Shizuku integration.

### Acceptance Criteria
- [ ] Supported metrics are real.
- [ ] Unsupported metrics are clearly identified.
- [ ] Optional elevated access is isolated.
- [ ] Standard mode remains functional.
- [ ] Build and tests pass.

---

## M8 — Optional Shizuku Integration
Status: [ ] Not started

### Objectives
- Research supported Shizuku APIs.
- Implement approved optional integration.
- Provide capability detection.
- Handle Shizuku unavailable or revoked states.

### Acceptance Criteria
- [ ] Shizuku is optional.
- [ ] Standard mode remains functional.
- [ ] Integration is permission-aware.
- [ ] Build and tests pass.

---

## M9 — Optional Root Integration
Status: [ ] Not started

### Objectives
- Evaluate useful root-only metrics.
- Implement approved root providers.
- Isolate root-specific functionality.

### Acceptance Criteria
- [ ] Root is optional.
- [ ] Standard mode remains functional.
- [ ] Root access is handled transparently.
- [ ] Build and tests pass.

---

## M10 — Customization and Polish
Status: [ ] Not started

### Objectives
- Panel customization.
- Card arrangement.
- Size and positioning options.
- Monitoring configuration.
- Performance optimization.
- Accessibility improvements.

### Acceptance Criteria
- [ ] Settings persist.
- [ ] UI remains consistent.
- [ ] Accessibility reviewed.
- [ ] Performance reviewed.
- [ ] Build and tests pass.

---

## M11 — Final Verification
Status: [ ] Not started

### Objectives
- Final regression testing.
- Permission testing.
- Lifecycle testing.
- Performance testing.
- Documentation review.
- APK packaging.

### Acceptance Criteria
- [ ] All required milestones verified.
- [ ] Known limitations documented.
- [ ] Final build succeeds.
- [ ] APK generated.
- [ ] Installation tested.
- [ ] Final report produced.

---

## Current Milestone
M2 — Floating Overlay Foundation

Status: implemented 2026-11-26; build and device verification pending.
`scripts/check-static.sh` passes 75 checks. No M2 build, lint, unit-test
or device result exists yet.

M1 — Application Foundation and Material You

Status: implementation and build verified by authorized run 45669463189
(commit `ba95fb4`): build, lint (0 errors) and 5/5 JVM tests pass, APK
produced. M1 stays `[~]` because runtime behavior (launch, theme switch,
persistence, dynamic color) has not been observed on a device, and
device testing is suspended (D016). Two post-run lint fixes were applied
in commit `53dd8d3` and are awaiting the next authorized run.

M0a (research and documentation) is complete. M0b build verification is
complete on the GitHub Actions x86_64 runner; its launch smoke test is
deferred and remains unsatisfied because emulator testing is suspended
(D016).

Do not start M3 or later without explicit authorization, and do not run
CI without explicit authorization for that run. Pushing commits does
not require permission because the workflow is manual-only.
