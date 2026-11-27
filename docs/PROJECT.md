# PocketPanel Project Specification

## Overview
PocketPanel is a native Android utility application featuring a
floating edge sidebar for system monitoring, network utilities and
quick-access controls.

The app is designed to be accessible from anywhere on the device
without requiring the user to leave the currently active application.

## Core Experience
A small floating handle sits near a screen edge.
The user interacts with the handle to reveal a compact panel.
The panel provides system monitoring information and network controls.
The user can dismiss the panel without switching applications.

## Primary Features

### Floating Sidebar
- Edge-mounted floating handle.
- Expandable and collapsible panel.
- Adjustable position.
- Configurable visibility.
- Lightweight animations.
- Overlay permission management.

### System Monitoring
- CPU usage.
- RAM usage.
- Battery information.
- Battery temperature where supported.
- GPU information where supported.
- Network throughput.
- Additional supported metrics in future milestones.

### VPN
- WireGuard client with configuration import.
- Connect and disconnect from the app.
- Real connection status and clear error states.
- Overlay side-panel on/off control in a later milestone.

### Customization
- System, Light and Dark theme.
- Material You dynamic colors where available.
- Optional user customization.
- Configurable monitoring interval.
- Panel positioning.
- Configurable panel content.

## Design
- Native Android application.
- Material 3 / Material You.
- Clean and minimal interface.
- No unnecessary decorative effects.
- Consistent typography, spacing and component styling.
- Adaptive light and dark themes.
- Accessibility-conscious design.

## Privacy
- Local-first.
- No account requirement.
- No telemetry by default.
- No unnecessary permissions.
- No cloud dependency.
- WireGuard configurations and private keys stay on the device.

## Access Modes
- Standard Android access.
- Optional Shizuku access.
- Optional root access.

Standard mode must remain functional without Shizuku or root. The
WireGuard client uses the standard Android `VpnService` API and does not
require elevated access.

## Non-Goals for Initial Release
- Cloud synchronization.
- Accounts.
- Remote servers.
- Unnecessary background services.
- Gaming optimization claims.
- Automatic modification of system settings.
- Unsupported or fabricated hardware metrics.
- VPN accounts, subscriptions or non-WireGuard protocols.

## Development
Follow AGENTS.md and docs/ROADMAP.md.
Development must proceed milestone by milestone.
