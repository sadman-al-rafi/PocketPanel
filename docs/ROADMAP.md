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
      minSdk 26).
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

---

## M0b — Project Generation and Build Verification
Status: [!] Blocked — awaiting authorization and a build environment
decision (D012)

### Objectives
- Create the native Android project with the pinned toolchain.
- Record the effective Kotlin/KGP version from the build.
- Generate and verify the Gradle wrapper at 9.6.0.
- Establish the initial project architecture from
  docs/ARCHITECTURE.md.
- Produce a verified debug build.

### Acceptance Criteria
- [ ] Android project generated with AGP 9.5.2, Gradle 9.6.0,
      JDK 17.
- [ ] Version catalog contains only the versions recorded in D005.
- [ ] `./gradlew buildEnvironment` output recorded, confirming the
      Kotlin version and Compose compiler plugin alignment.
- [ ] Project builds successfully (real, observed build output).
- [ ] Basic application launches.
- [ ] No prerelease dependency is resolved.
- [ ] Verification method recorded in docs/TESTING.md and
      docs/DECISIONS.md.

---

## M1 — Application Foundation and Material You
Status: [ ] Not started

### Objectives
- Establish Material 3 design system.
- Implement System, Light and Dark theme settings.
- Implement dynamic color support where available.
- Create the application settings screen.
- Create the main application screen.

### Acceptance Criteria
- [ ] App uses Material 3.
- [ ] Theme switching works.
- [ ] Theme selection persists.
- [ ] UI is responsive.
- [ ] No unnecessary decorative styling.
- [ ] Build and tests pass.

---

## M2 — Floating Overlay Foundation
Status: [ ] Not started

### Objectives
- Implement overlay permission handling.
- Create floating edge handle.
- Implement panel opening and closing.
- Implement drag positioning.
- Implement overlay lifecycle handling.

### Acceptance Criteria
- [ ] Overlay permission is handled correctly.
- [ ] Floating handle appears when enabled.
- [ ] Panel opens and closes.
- [ ] Handle positioning works.
- [ ] Permission denial is handled.
- [ ] Overlay survives expected lifecycle transitions.
- [ ] No unacceptable resource consumption.
- [ ] Build and tests pass.

---

## M3 — Basic System Monitoring
Status: [ ] Not started

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
Status: [ ] Not started

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
M0b — Project Generation and Build Verification

Status: blocked pending explicit authorization and a build environment
decision (docs/DECISIONS.md D012).

M0a (research and foundation documentation) is complete and recorded
above. Do not start M1 or later without explicit authorization.
