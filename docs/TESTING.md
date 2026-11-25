# PocketPanel Testing Strategy

## Build Verification
- Verify Gradle configuration.
- Verify dependency resolution.
- Verify compilation.
- Verify APK generation.

## Application Verification
- Application launch.
- Theme switching.
- Settings persistence.
- Navigation.

## Overlay Verification
- Permission granted.
- Permission denied.
- Handle positioning.
- Panel opening and closing.
- Lifecycle behavior.
- Overlay dismissal.

## Monitoring Verification
- Readings are genuine.
- Unsupported sources are handled.
- Monitoring stops when appropriate.
- No excessive polling.
- No crashes when sources become unavailable.

## Regression Verification
- Previously completed functionality must be retested
  after relevant changes.

## Reporting
Record tests performed, results, failures and limitations.
Never claim tests were performed when they were not.
