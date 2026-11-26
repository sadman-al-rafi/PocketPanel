# PocketPanel — Agent Instructions

## 1. Project Identity
PocketPanel is a native Android utility application featuring an expandable
floating sidebar, system monitoring, and quick-access tools.

The application must prioritize reliability, native Android behavior,
readability, simplicity, privacy and battery efficiency.

## 2. Mandatory Documentation
Before performing any project work:
- Read this AGENTS.md.
- Read docs/PROJECT.md.
- Read docs/ARCHITECTURE.md.
- Read docs/ROADMAP.md.
- Read docs/TESTING.md.
- Read docs/DECISIONS.md.
- Inspect the existing project and Git status.

Treat these documents as the source of truth.

## 3. Technology Validation — Mandatory
Technology and dependency compatibility must be researched BEFORE
implementation, dependency installation or project generation.

- Verify Android development tools, Android SDK, Gradle, Kotlin,
  Android Gradle Plugin, Jetpack Compose and library compatibility
  using official documentation and release information.
- Prefer current stable releases compatible with one another.
- Do not blindly use versions from old tutorials, code examples,
  previous projects, cached knowledge or generated templates.
- Do not use preview, alpha, beta, RC or experimental versions unless
  explicitly approved.
- Do not automatically select the absolute newest version if it is
  incompatible with other required components.
- Verify required JDK versions against Gradle and Android Gradle Plugin.
- Verify the Android SDK requirements of every selected dependency.
- Verify dependency compatibility before introducing new libraries.
- Prefer official Android libraries and well-maintained libraries.
- Avoid unnecessary third-party dependencies.
- Do not invent package names, APIs, version numbers or dependency
  coordinates.

Before the initial project generation:
1. Research the currently supported stable toolchain.
2. Prepare a compatibility matrix.
3. Record selected versions and official verification sources
   in docs/DECISIONS.md.
4. Explain any compatibility constraints.
5. Only then generate the project.

If web access or reliable verification sources are unavailable,
do not guess. Report the limitation and request approval before
proceeding with an unverified toolchain.

## 4. Development Environment
- Native Android application.
- Kotlin.
- Jetpack Compose.
- Material 3.
- Gradle Kotlin DSL.
- Android SDK.
- No WebView-based primary UI.
- No unnecessary frameworks or abstractions.

## 5. Design Guidelines — Material You
- Use Material 3 and native Android design conventions.
- Prioritize a clean, minimal, functional interface.
- No neon, gaming-style visual effects, excessive gradients,
  decorative glassmorphism or unnecessary animations.
- Use appropriate Material components and restrained elevation.
- Use Material Symbols or standard Material icon resources.
- Avoid custom decorative icon packs.
- Maintain consistent spacing, typography and component sizing.
- Design for accessibility, readability and touch interaction.

### Theme
- Support system default light and dark themes.
- Provide a user setting for Light, Dark and System.
- Provide optional Material You dynamic color support where available.
- If dynamic colors are unavailable, use a cohesive Material 3
  fallback palette.
- Provide an optional user-defined theme/accent customization
  only where it fits the design without compromising consistency.
- Theme changes must apply throughout the application.
- Do not hardcode colors throughout composables.
- Use centralized Material theme tokens and color schemes.

## 6. Android Platform and Permission Requirements
- Respect Android security and permission restrictions.
- Do not bypass platform protections.
- Floating overlays must use appropriate Android APIs and permissions.
- Handle permission denial and revocation gracefully.
- Explain permissions before requesting them where appropriate.
- Do not request permissions that are not required for a feature.
- Do not assume root or Shizuku is available.
- Root and Shizuku integrations must remain optional.
- Detect capabilities at runtime.
- Do not fabricate CPU, GPU, temperature, memory or FPS readings.
- Clearly identify unavailable or unsupported measurements.
- Prefer documented APIs.
- If device-specific interfaces are required, isolate them into
  separate providers and document their limitations.
- Unsupported features must degrade gracefully.

## 7. Architecture and Code Quality
- Keep components modular and maintainable.
- Separate UI, state management, monitoring providers,
  permissions, persistence and settings.
- Avoid tightly coupled components.
- Use appropriate lifecycle-aware mechanisms.
- Do not introduce abstractions without a real need.
- Avoid large monolithic files.
- Avoid unnecessary rewrites.
- Preserve working functionality when modifying the project.
- Do not perform unrelated refactoring during bug fixes.
- Handle errors explicitly.
- Do not suppress errors merely to make a build succeed.
- Keep sensitive data local by default.
- Do not add analytics, tracking or telemetry without approval.

## 8. Performance and Battery
- Monitoring intervals must be configurable.
- Avoid unnecessarily frequent polling.
- Avoid unnecessary background services.
- Suspend or reduce monitoring when appropriate.
- Keep overlay rendering lightweight.
- Avoid memory leaks.
- Handle service lifecycle and app lifecycle correctly.
- Do not use excessive animations.
- Monitor resource usage during development when practical.

## 9. Mandatory Build and Dependency Discipline
- Verify the compatibility of dependencies before adding them.
- Do not repeatedly replace libraries because of avoidable
  compatibility mistakes.
- Do not use obsolete APIs when supported alternatives exist.
- Do not blindly upgrade or downgrade dependencies.
- Do not change multiple unrelated dependency versions at once.
- Investigate the actual cause of a failure before changing versions.
- Read relevant compiler, Gradle and runtime error messages.
- Reproduce failures where possible.
- Apply the smallest appropriate fix.
- Do not claim successful builds without actually building.
- Do not claim tests passed without running them.
- Report unavailable tests explicitly.
- Avoid unnecessary clean builds if incremental builds are sufficient.
- Do not delete caches or project files to conceal failures.

## 10. Roadmap Management — Mandatory
docs/ROADMAP.md is the authoritative progress tracker.

- Read it before starting any task.
- Work only on the currently authorized milestone.
- Do not implement future milestones without approval.
- Each milestone must contain objectives, deliverables,
  acceptance criteria and verification requirements.
- Update the roadmap when work starts.
- Update it immediately after completing or abandoning work.
- Mark items complete only after verification.
- Clearly record failed tests, unresolved bugs and blockers.
- Never claim a milestone is complete if its acceptance criteria
  have not been satisfied.
- Do not silently skip milestone requirements.
- If the implementation plan changes, explain why and update
  the roadmap accordingly.
- Preserve completed milestones and their verification history.

## 11. Development Workflow
For each milestone:

1. Read relevant documentation and inspect existing files.
2. Identify the exact milestone and acceptance criteria.
3. Review relevant APIs and technical constraints.
4. Plan the smallest implementation that meets the requirements.
5. Implement only the authorized milestone.
6. Run the local static checks that do not require CI.
7. Build and run automated tests only when a CI run is authorized
   (section 13).
8. Inspect and resolve failures.
9. Review changes for regressions.
10. Update docs/ROADMAP.md.
11. Update docs/DECISIONS.md when important technical decisions
    or architectural changes are made.
12. Update docs/TESTING.md if test procedures change.
13. Report the results and wait for authorization to continue.

## 12. Completion Report
At the end of each task, report:

- Implemented functionality.
- Files created or modified.
- Build results.
- Test results.
- Known limitations.
- Roadmap status.
- The next milestone.

Never start the next milestone without explicit authorization.

## 13. CI Authorization Policy — Mandatory

Remote CI consumes shared time and must be requested, not assumed.

1. NEVER trigger a GitHub Actions workflow automatically.
2. Pushing commits to the repository is allowed and does not require
   permission, because the workflow is manual-only and a push cannot
   start CI. Never create a pull request or take any other action
   specifically intended to trigger CI without explicit permission.
3. Do not use `git push` to find out whether the build works.
4. Local checks that do not trigger remote CI are allowed and expected.
5. Before requesting CI permission, explain exactly what needs testing,
   why it is worth testing now and which workflow will run.
6. Wait for explicit approval before triggering any remote workflow.
7. This policy applies to every milestone and session until the user
   explicitly changes it.

Workflow requirements:
- `.github/workflows/verify-toolchain.yml` is manual-only
  (`workflow_dispatch`). Do not add `push`, `pull_request` or `schedule`
  triggers.
- The workflow performs the build, lint and unit tests. No emulator
  installation, AVD creation or emulator boot is part of CI.
- Do not repeatedly retry a failed run. Diagnose locally, explain the
  change and request a new run.
- Generate a debug APK only through an authorized CI run.
- Report CI results only from a run that actually completed, and state
  the run ID.
