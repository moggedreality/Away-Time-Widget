# Contributing to Away Time

Thank you for your interest in contributing to Away Time! This document provides guidelines for contributing to this project.

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## How Can I Contribute?

### Reporting Bugs

Before creating a bug report, please check the [issue tracker](https://github.com/moggedreality/Away-Time-Widget/issues) to see if the issue has already been reported.

When creating a bug report, please include:
- **Clear title** describing the issue
- **Steps to reproduce** the problem
- **Expected behavior** vs **actual behavior**
- **Device information** (model, Android version)
- **App version** (from Settings → About)
- **Screenshots** if applicable
- **Logs** if relevant (adb logcat)

### Suggesting Enhancements

Enhancement suggestions are welcome! Please:
- Check existing issues first
- Provide a clear description of the feature
- Explain the use case and why it would be valuable
- Consider if it fits the app's scope (widget-focused, privacy-first)

### Pull Requests

1. **Fork** the repository
2. **Create a branch** from `main`: `git checkout -b feature/your-feature-name`
3. **Make your changes** following the guidelines below
4. **Test thoroughly** on device/emulator
5. **Run lint checks** locally (see Development Setup)
6. **Submit a PR** with a clear description

## Development Setup

### Prerequisites
- **JDK 17+** (required for Gradle 8.14+)
- **Android Studio** (latest stable) or **IntelliJ IDEA** with Android plugin
- **Android SDK** with API 36 (Android 16)

### Building the Project

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/Away-Time-Widget.git
cd Away-Time-Widget

# Build debug APK
./gradlew assembleDebug

# Run all checks (lint, detekt, ktlint)
./gradlew check

# Format code with ktlint
./gradlew ktlintFormat
```

### Code Style

We use **ktlint** with the official Kotlin style guide:

```bash
# Check formatting
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat
```

We also use **detekt** for static analysis:

```bash
# Run detekt
./gradlew detekt
```

**Before committing**, run `./gradlew check` to ensure all checks pass.

### Commit Guidelines

- Use **conventional commits** format:
  - `feat:` new feature
  - `fix:` bug fix
  - `docs:` documentation changes
  - `style:` formatting (no code change)
  - `refactor:` code restructuring
  - `test:` adding tests
  - `chore:` maintenance tasks

Example:
```
feat: add adaptive theme support for widget

- Implement light/dark theme switching based on system theme
- Add adaptive theme option to Design tab
- Update WidgetRenderer for dynamic colors
```

### Pull Request Guidelines

- **One feature/fix per PR** — keep changes focused
- **Update documentation** if you change user-facing behavior
- **Add tests** for new functionality
- **Keep PR description clear** — explain what, why, and how
- **Link related issues** using `Fixes #123` or `Relates to #123`

## Project Structure

```
Away-Time-Widget/
├── app/                          # Main app module
│   ├── src/main/
│   │   ├── java/com/stxrs/awaytime/
│   │   │   ├── AwayTimeCalculator.kt
│   │   │   ├── AwayTimeWidget.kt
│   │   │   ├── WidgetRenderer.kt
│   │   │   ├── WidgetSettings.kt
│   │   │   ├── Themes.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── HomeScreen.kt
│   │   │   ├── DesignScreen.kt
│   │   │   ├── PresetsScreen.kt
│   │   │   ├── SettingsScreen.kt
│   │   │   ├── AppPicker.kt / Apps.kt
│   │   │   ├── Motion.kt / Sheets.kt / NavBar.kt / Pickers.kt / Ui.kt
│   │   │   ├── FontStore.kt
│   │   │   ├── Battery.kt
│   │   │   └── BootReceiver.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml        # Version catalog
│   └── wrapper/
├── .github/workflows/            # CI/CD pipelines
├── build.gradle.kts              # Root build file
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── SECURITY.md
└── CHANGELOG.md
```

## Architecture Notes

- **Single module** (`:app`) — all code in one module for simplicity
- **No external UI libraries** — pure View-based UI built in code
- **UsageStatsManager** — reads usage events (no background collection)
- **No internet permission** — fully offline, privacy-first
- **Kotlin 2.3.20**, **AGP 8.13.2**, **Gradle 8.14.3**
- **minSdk 28**, **targetSdk 36**

## Testing

- **Unit tests**: `./gradlew test` (JVM tests)
- **Instrumented tests**: `./gradlew connectedAndroidTest` (device/emulator)

## Release Process

Releases are automated via GitHub Actions:
1. Tag a release: `git tag v2.3.0 && git push origin v2.3.0`
2. CI builds signed AAB/APK
3. GitHub Release created with artifacts

## Questions?

Feel free to open an issue for any questions about contributing!