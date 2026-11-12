# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial open source preparation (CI/CD, linting, documentation)

## [2.2.0] - 2024-XX-XX

### Added
- 10 widget themes (including adaptive light/dark)
- Custom font support
- Configurable greetings and layout controls
- Per-line text styling
- Auto-refresh intervals
- "App usage" and "Screen on" data modes
- Standby apps feature (apps that count as away time)
- Four-tab app UI: Home, Design, Presets, Settings
- Live widget preview in app
- Preset management (save/load/delete)
- Spring animations
- Battery saver exemption prompt
- Usage access permission handling
- Boot receiver for timer re-arm

### Changed
- Target SDK 36 (Android 16)
- Kotlin 2.3.20
- AGP 8.13.2
- Gradle 8.14.3

### Fixed
- Widget update reliability
- Theme persistence across reboots
- Font loading edge cases

## [2.1.0] - 2024-XX-XX

### Added
- Basic widget functionality
- Theme system
- Settings screen

### Changed
- Initial release

---

## Release Notes Format

Each release includes:
- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for vulnerability fixes