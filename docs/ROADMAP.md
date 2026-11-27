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
  explicitly (D019). Run 13 compiled and verified this.
- Authorized manual run 45756276354 (run 13, commit `c254da9`)
  succeeded: build, lint (0 errors), 5/5 JVM tests and the debug APK.
  Runtime overlay behavior remains unverified (D016).

### Deferred Beyond M2 Acceptance
- Outside-tap collapse (a non-focusable overlay cannot receive outside
  touches).
- Screen-off collapse.
- Boot restore (no boot receiver by design, D008/D018).
- Permission-loss detection while the app is backgrounded.
- Expanded-panel sizing on unusually small displays.

---

## M3 — Basic System Monitoring
Status: [✓] Implemented 2026-11-27 (commit `ca9ba8a`). Implementation
was rewritten after authorized runs 14-19 failed on
invalid placeholder code (not a toolchain incompatibility). Authorized
manual run 45824780276 (run 20, commit `ca9ba8a`) succeeded: build,
lint 0 errors / 5 pre-existing warnings, 15/15 JVM tests and the debug
APK. Runtime monitoring behavior remains unverified because no device
or emulator test was authorized at that time (D016).

2026-11-27 bug fix (before M7): the CPU row was empty on every test
device because `/proc/stat` is unreadable by apps on API 26+. D025 adds
a clearly labelled cpufreq-based fallback. CI run `45964971185`
(run 26, commit `56212e1`) passed build, lint and 45/45 JVM tests,
including 7 new `CpuUsageMathTest` cases. Device verification of the
CPU row is pending.

### Objectives
- CPU usage monitoring.
- RAM monitoring.
- Battery information.
- Monitoring state management.
- Configurable monitoring intervals.

### Acceptance Criteria
- [✓] Supported readings are real. Memory, battery and CPU values come
      from documented platform APIs; CPU usage uses `/proc/stat` deltas
      where available and an explicitly labelled estimate otherwise
      (D025).
- [✓] Unsupported readings are clearly identified. Null values render
      as "Not available".
- [~] Monitoring updates correctly. Implemented with interval polling;
      needs device verification.
- [~] Monitoring can be stopped. Implemented with the monitoring
      switch; needs device verification.
- [✓] No fake production readings.
- [✓] Build and tests pass (run 20).

### Implementation Notes
- `DeviceInfo` reads memory through `ActivityManager.MemoryInfo`,
  battery level/temperature/health through the sticky
  `ACTION_BATTERY_CHANGED` broadcast, and CPU usage through
  best-effort `/proc/stat` deltas or a labelled cpufreq estimate when
  `/proc/stat` is blocked (D025).
- `SystemInfoRepository` samples on `Dispatchers.Default` only while
  the UI observes the state.
- `PocketPanelViewModel` persists monitoring state and interval through
  `SettingsRepository` (DataStore) and exposes them in
  `PocketPanelUiState`.
- Unsupported values remain null and are shown as "Not available".
- Local static checks pass 92/92. Run 26 verified compilation, lint,
  45/45 JVM tests and the APK after the D025 CPU fallback.

---

## M4 — Temperature and Hardware Discovery
Status: [✓] Implemented 2026-11-27 (commit `ca9ba8a`). Battery
temperature is implemented through the M3 monitoring pipeline and run
45824780276 (run 20) verified build, lint and tests. Runtime reading
remains unverified because no device test is authorized (D016).

### Objectives
- Discover available temperature information.
- Identify supported temperature sources.
- Implement capability detection.
- Provide clear unsupported states.

### Acceptance Criteria
- [✓] Thermal sources are discovered safely. Battery temperature is
      read from the documented sticky battery broadcast; an absent
      extra yields null.
- [~] Available readings are displayed correctly. Implemented in the
      monitoring card; needs device verification.
- [✓] Unsupported sources do not crash the app.
- [✓] No fabricated readings.
- [✓] Build and tests pass (run 20).

### Implementation Notes
- Battery temperature (`BatteryManager.EXTRA_TEMPERATURE`) is the
  primary source because it is broadly available across Android
  devices.
- A missing or `Integer.MIN_VALUE` reading is shown as "Not
  available"; no temperature is estimated or fabricated.
- CPU/SoC thermal-zone sysfs paths are not used: access is not
  guaranteed and is not an SDK contract.

---

## M5 — Network Monitor
Status: [✓] Implemented 2026-11-27 (commit `37fab11`). Authorized manual
run 45939285640 (run 22) succeeded: build, lint 0 errors / 5
pre-existing warnings, 25/25 JVM tests and the debug APK. The user
confirmed that network data and the ping row work on a test device.

### Objectives
- Network throughput monitoring.
- Basic connection information.
- Network monitoring UI.
- Ping latency to 8.8.8.8 (added on request).

### Acceptance Criteria
- [✓] Readings are accurate to the supported source. Throughput is
      device-wide from `TrafficStats`; the user confirmed the values
      and the ping reading display on a test device.
- [~] Interface handles network changes. The transport is re-read on
      every sample; a network-switch scenario has not been tested
      separately.
- [~] Monitoring lifecycle is respected. Sampling is tied to the
      monitoring switch and interval; stop/start has not been tested
      separately on a device.
- [✓] Build and tests pass (run 22).

### Implementation Notes
- `NetworkMonitor` reads the active transport from `ConnectivityManager`
  and `NetworkCapabilities`, and derives device-wide download/upload
  rates from successive `TrafficStats` total counters.
- The first sample carries the transport only; a rate needs two samples.
  Unsupported counters or a negative delta yield null ("Not available").
- Sampling shares the monitoring interval and stops with monitoring.
- The network section is part of the monitoring card and is labelled as
  device-wide, because `TrafficStats` exposes no per-app attribution.
- `ACCESS_NETWORK_STATE` is declared (normal permission, no prompt).
- Ping is measured with the platform `ping` command against 8.8.8.8,
  cached for five seconds and exposed in `NetworkInfo` so the overlay
  side panel can reuse it later. `INTERNET` is declared for the ping
  process. A blocked or missing `ping` binary yields "Not available".

### Verification Performed
- Authorized manual run 45936405957 (run 21, commit `50735e4`)
  succeeded: build, lint 0 errors / 5 pre-existing warnings, 20/20 JVM
  tests and a 14,008,773-byte debug APK.
- The user reported that the app shows the network data on a local
  device test.
- Authorized manual run 45939285640 (run 22, commit `37fab11`)
  succeeded: build, lint 0 errors / 5 pre-existing warnings, 25/25 JVM
  tests and a 14,025,981-byte debug APK.
- The user reported that the ping row works on a test device.

---

## M6 — WireGuard VPN Client
Status: [✓] Implemented and verified 2026-11-27. CI run 24
verified build, lint and tests; the project owner confirmed the VPN
works normally on a test device.

### Objectives
- Add an embedded WireGuard client using the official
  `com.wireguard.android:tunnel` library.
- Import and validate a WireGuard configuration.
- Connect and disconnect through Android `VpnService` with explicit
  user consent.
- Show real connection state and clear error states.
- Expose a connect/disconnect control and observable state that the
  overlay side panel can reuse in a later milestone.
- Keep the feature optional: no root or Shizuku is required, and the
  rest of the app works while the VPN is off.

### Deliverables
- `vpn/` package: config parser/validator, encrypted profile storage,
  backend adapter, tunnel handle and state model. The `VpnService` is
  provided by the library AAR.
- In-app VPN card: import configuration, connect, disconnect and show
  status.
- The library AAR declares `GoBackend$VpnService` with
  `BIND_VPN_SERVICE` and the `android.net.VpnService` intent filter;
  the app does not duplicate that declaration.
- Configuration import through the Storage Access Framework, so no
  storage permission is required.
- Unit tests for config parsing/validation, state mapping and secret
  redaction.
- Documentation and a verification record.

### Acceptance Criteria
- [ ] A valid WireGuard configuration establishes a real tunnel; no
      connected state is shown without an established tunnel.
- [ ] `VpnService.prepare()` consent is requested before the first
      connection; denial, revocation and `onRevoke()` are handled
      without a crash or a stale connected state.
- [ ] Only one VPN connection is active; disconnect stops the tunnel and
      clears the system VPN indicator.
- [ ] Private keys are encrypted at rest with an Android Keystore key
      and never written to logs, plain DataStore or crash output.
- [ ] Invalid or incomplete configurations are rejected with a clear
      error; no partial or fabricated connection state.
- [x] The app adds no foreground service type or notification channel for
      the VPN; the library service is managed by the system.
- [ ] Standard mode remains functional with the VPN off; no root or
      Shizuku dependency.
- [x] The connect/disconnect state is exposed through a reusable
      repository surface for a future overlay button.
- [x] Build, lint and JVM tests pass in CI (D024).

### Out of Scope for M6
- The overlay side-panel on/off button. M6 exposes the control
  surface; the button is added in a later milestone (tracked in M10).
- Always-on VPN, per-app routing, kill switch, multiple profiles,
  subscriptions, account services and non-WireGuard protocols.

### Technology Validation (before adding the dependency)
- Official library: `com.wireguard.android:tunnel:1.0.20260102`
  (Apache-2.0), verified on Maven Central on 2026-11-27.
- AAR metadata: `minCompileSdk=1`,
  `minAndroidGradlePluginVersion=1.0.0`, compatible with the pinned
  AGP 9.5.2 / compileSdk 37.1.
- AAR dependencies: `androidx.annotation:1.9.1` and
  `androidx.collection:1.5.0`.
- Native ABIs: arm64-v8a, armeabi-v7a, x86 and x86_64. The AAR is about
  5.8 MB, so the APK will grow; measure the exact delta in CI.
- Android `VpnService` requirements: `BIND_VPN_SERVICE` service
  permission, `android.net.VpnService` intent filter and user consent via
  `VpnService.prepare()`. The system shows the active-VPN indicator.
- Verified from the library sources and the official WireGuard app on
  2026-11-27: `GoBackend$VpnService` does not call `startForeground()` and
  the app declares no VPN notification channel. The AAR manifest already
  contains the service declaration, so the app must not add a duplicate.
- No app foreground service type is required for the VPN. The overlay
  keeps `specialUse` for its own service (D009).
- Core library desugaring is enabled with `desugar_jdk_libs:2.1.5`: the
  library uses `List.of` and `java.time`, which are not available on all
  supported API levels without it. The first M6 CI run confirms
  Kotlin/AGP compatibility and native packaging.

### Verification Record (2026-11-27)
- CI run 45962542309 (run 24), commit `5ecbc25`: `assembleDebug`,
  `lintDebug` and `testDebugUnitTest` all succeeded.
- JVM tests: 38 tests, 0 failures. `WireGuardConfigParserTest` adds 13
  cases; the 25 earlier tests still pass.
- Lint: 0 errors, 5 warnings, the same categories as M5 (OldTargetApi,
  AndroidGradlePluginVersion, DataExtractionRules and UseKtx). No new
  VPN lint warning.
- Debug APK: 27,989,171 bytes; sha256
  `c21ade4cbbdab100943eb25b2eb8ae64cc61a4211567095a8bb5508e89900fac`.
  The M5 APK was 14,025,981 bytes, so M6 adds 13,963,190 bytes (about
  13.3 MiB), consistent with the four native ABIs and desugared classes.
- CI run `45962342278` (run 23, commit `b972f56`) failed to compile:
  `VpnTunnel` used `override val name` for the Java `Tunnel.getName()`
  method. The fix implements `override fun getName()`.
- `scripts/check-static.sh`: 86/86 checks pass.
- Not verified in CI: a real tunnel, the consent dialog, revocation,
  Keystore encryption on a device and the ABI contents of the APK.
- Device verification: the project owner reported on 2026-11-27 that the
  VPN feature works normally on a test device. Individual consent,
  revocation and revoke-during-tunnel cases were not separately reported.

---

## M7 — GPU and Advanced Metrics
Status: [✓] Implemented and build verified 2026-11-27 (CI run 28,
commit `773e6b3`). Device verification of the hardware card pending.

### Objectives
- Investigate GPU monitoring sources.
- Implement supported GPU readings.
- Investigate additional hardware metrics.
- Evaluate optional Shizuku integration.

### Deliverables
- `system/GpuInfoProvider.kt`: reads `GL_RENDERER`, `GL_VENDOR` and
  `GL_VERSION` from a short-lived, off-screen EGL context and caches the
  result. The context is always released.
- `HardwareSnapshot` plus extended `DeviceInfo` for the new readings.
- `ui/main/HardwareCard.kt` showing GPU identity, an explicit unsupported
  state, and a note that GPU utilization and FPS are not available.
- Documentation of the Shizuku evaluation (D027).
- `PowerManager` thermal status/headroom were implemented in M7 but were
  removed in the M10 follow-up; they are no longer part of the app.

### Acceptance Criteria
- [ ] Supported readings are real: GPU identity comes from OpenGL ES
      strings. No percentage is inferred from a name or a vendor string.
- [ ] Unsupported readings are clearly identified: an unreadable GPU
      identity shows "Not available". GPU utilization and FPS are never
      displayed. Thermal status/headroom are not displayed.
- [ ] No elevated access is used: M7 adds no Shizuku or root path; all
      readings come from standard APIs (D027).
- [ ] Standard mode remains functional with no new permission and no
      new dependency. Code review confirms no new permission or
      dependency; runtime check pending.
- [x] Build, lint and JVM tests pass in CI (D024).

### Verification Record
- CI run `45966560679` (run 28, commit `773e6b3`): `assembleDebug`,
  `lintDebug` and `testDebugUnitTest` succeeded.
- 48/48 JVM tests passed at verification time (3 thermal mapping cases
  were included then and were removed with that feature).
- Lint: 0 errors, 5 pre-existing warnings.
- Debug APK: 28,014,367 bytes; sha256
  `0eee340d44ca4a33f2c61644c7bccc80658b41a06252d2c3f55a640c7cf6552f`.
- `scripts/check-static.sh`: 101/101 checks before the push.
- Not verified: GPU identity strings and the unsupported-state label on a
  device. These need device verification.

### Technology Validation
- Android has no public API for GPU utilization or FPS. The documented
  EGL/OpenGL ES string queries are identity information only (D026).
- `PowerManager` thermal APIs were evaluated in M7 but removed in the
  M10 follow-up at the project owner's request; no thermal API is used.
- All new APIs come from the Android SDK. The toolchain is unchanged:
  Gradle 9.6.0, AGP 9.5.2, Kotlin 2.4.21, compileSdk 37.1, minSdk 26.

### Verification Requirements
- Local static checks before pushing.
- CI build, lint and JVM tests; record the run ID and commit.
- Device verification of the hardware card on API 26-28, 29 and 30+.


---

## M10 — Customization and Polish
Status: [✓] Implemented and build verified 2026-11-27 (CI run 40,
commit `c9c52b5`). Device verification pending.

### Objectives
- Panel customization.
- Card arrangement.
- Size and positioning options.
- Monitoring configuration.
- Overlay side-panel VPN on/off control.
- Performance optimization.
- Accessibility improvements.

### Deliverables
- Persisted `HandleSize` (compact/normal) and `MainCardLayout`
  (standard/focus) preferences in DataStore.
- Settings screen sections for main-screen layout, handle size, handle
  edge and monitoring enable/interval.
- Main screen starts with the always-visible floating-panel switch; the
  welcome and appearance cards were removed. Appearance options live only
  in settings. The `MINIMAL` layout preset was removed.
- Overlay panel shows a 2x2 grid (CPU %, battery temperature, memory %,
  ping) only while monitoring is active, followed by the shared VPN
  status and connect/disconnect button. Consent is requested by
  `VpnToggleActivity`, launched from the overlay and finished after the
  system result.
- Dragging uses the window offsets just written instead of a possibly
  stale `getLocationOnScreen` value, for smoother movement.
- `PowerManager` thermal status and headroom were removed completely.
- Overlay window resizes when the handle size or edge changes.
- JVM tests for the new preference enums.
- Documentation and verification record.

### Acceptance Criteria
- [ ] Settings persist across process restart (DataStore keys).
- [ ] UI remains consistent: all layout presets use the same Material 3
      cards and the main VPN card is always available.
- [ ] Accessibility reviewed: overlay VPN button has a 48dp minimum
      height, settings options are 64dp rows with radio/switch roles,
      and the handle keeps its content description.
- [ ] Performance reviewed: monitoring is sampled only while enabled;
      the overlay starts its sampling job only while monitoring is active
      and the panel service is attached.
- [ ] VPN control from the overlay reflects the real tunnel state and
      respects system consent: consent is launched for result before the
      first connect, and denial leaves the UI disconnected.
- [ ] Build and tests pass in CI (D024).

### Verification Requirements
- Local static checks before pushing.
- CI build, lint and JVM tests; record the run ID and commit.
- Device verification: layout presets, handle size/edge, settings
  persistence, overlay 2x2 grid, smooth dragging and overlay
  connect/disconnect with consent.

### Verification Record
The run below verified the M10 state before the home-screen and overlay
monitoring follow-up; the current tree still needs its own CI run.

- CI run `46008800467` (run 37, commit `dd9ba0e`): `assembleDebug`,
  `lintDebug` and `testDebugUnitTest` succeeded.
- 50/50 JVM tests passed, including 2 new preference-enum cases.
- Lint: 0 errors, 5 pre-existing warnings. An extra `UnusedResources`
  warning in run 36 was fixed by removing the obsolete overlay body
  string.
- Debug APK: 28,038,751 bytes; sha256
  `f85b0c6973663fcad9022ebdf34e971460f66af3803d074261c02eaab5adc08a`.
- `scripts/check-static.sh`: 109/109 checks.
- The follow-up changes were verified by CI run `46016088373` (run 40,
  commit `c9c52b5`): build, lint and 49/49 JVM tests passed, with 2 new
  `MemoryUsageTest` cases and 3 thermal cases removed. Lint had 0 errors
  and 5 pre-existing warnings. The debug APK is 28,031,631 bytes (sha256
  `f34962f3bdf5a259a509bae4200dcdf9c6cc3318a9783d7bad4c1fab4b3559bd`).
  `scripts/check-static.sh` passes 111/111.
- Device test of run 40 reported an immediate crash when enabling the
  floating panel. Cause: `OverlayService` constructed `SystemInfoRepository`
  and `NetworkMonitor` in field initializers, before Android attached the
  service Context. Fixed in run 42 (commit `0f03773`) by creating both in
  `onCreate()`; the owner confirmed the crash was gone.
- On-device testing also showed the overlay VPN button bringing the app to
  the foreground. The connect helper Activity now uses a separate task and
  a separate task and a transparent theme with no content, and disconnect
  is handled directly in the service so no Activity is started. CI run
  `46022577030` (run 47, commit `d6a68d9`) verified this state; on-device
  retest pending. Runs 44 and 46 failed on an intermediate `Theme.NoDisplay`
  choice and an unavailable KTX drawable call; both were corrected.

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
