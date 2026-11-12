# Away Time

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-8.13.2-3DDC84.svg?logo=android)](https://developer.android.com/studio/releases/gradle-plugin)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.3-02303A.svg?logo=gradle)](https://gradle.org)
[![minSdk](https://img.shields.io/badge/minSdk-28-green.svg)](https://developer.android.com/about/dashboards)
[![targetSdk](https://img.shields.io/badge/targetSdk-36-blue.svg)](https://developer.android.com/about/versions/16)

[![CI](https://github.com/moggedreality/Away-Time-Widget/actions/workflows/ci.yml/badge.svg)](https://github.com/moggedreality/Away-Time-Widget/actions/workflows/ci.yml)
[![Code Quality](https://img.shields.io/badge/code%20quality-ktlint%20%7C%20detekt%20%7C%20spotless-blue)](https://github.com/moggedreality/Away-Time-Widget/actions/workflows/ci.yml)
[![Dependabot](https://img.shields.io/badge/dependabot-enabled-brightgreen.svg)](https://github.com/moggedreality/Away-Time-Widget/blob/main/.github/dependabot.yml)

A privacy-first Android home-screen widget that displays your daily **time away from your phone** — time since midnight minus screen/app usage.

> **No internet permission · No analytics · No tracking · Fully offline**

![Away Time Widget](time%20away.png)

## Features

### Widget
- **10 themes** — including adaptive light/dark that follows system theme
- **Custom fonts** — load any `.ttf`/`.otf` from storage
- **Configurable greetings** — dynamic text with time/date placeholders
- **Layout controls** — line spacing, alignment, margins
- **Per-line styling** — font, size, color, weight per line
- **Auto-refresh** — configurable intervals (15min, 30min, 1hr, etc.)
- **Multiple sizes** — resizable widget (2×1 to 4×4)

### Data Modes
| Mode | Description |
|------|-------------|
| **App Usage** | Like Digital Wellbeing — reads `UsageStatsManager` events |
| **Screen On** | Counts only when screen is actually on |

### Standby Apps
Mark apps as "standby" (e.g., music player, navigation) — their usage counts as **away time** instead of screen time.

### App (4 Tabs)
| Tab | Purpose |
|-----|---------|
| **Home** | Live widget preview + quick settings |
| **Design** | Theme, font, layout, colors customization |
| **Presets** | Save/load/delete widget configurations |
| **Settings** | Data mode, standby apps, permissions, battery exemption |

### Privacy & Permissions
| Permission | Required | Purpose |
|------------|----------|---------|
| `PACKAGE_USAGE_STATS` | ✅ | Read usage events (user grants via Settings) |
| `RECEIVE_BOOT_COMPLETED` | ✅ | Re-arm widget timer after reboot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Optional | Exempt from Doze/Battery Saver |

**Zero network access** — the app declares no `INTERNET` permission.

## Screenshots

| Home | Design | Presets | Settings |
|------|--------|---------|----------|
| ![Home](screenshots/home.png) | ![Design](screenshots/design.png) | ![Presets](screenshots/presets.png) | ![Settings](screenshots/settings.png) |

## Download

| Platform | Link |
|----------|------|
| **GitHub Releases** | [Latest APK/AAB](https://github.com/moggedreality/Away-Time-Widget/releases/latest) |
| **Play Store** | Coming soon |

## Building

### Prerequisites
- **JDK 17+** (required for Gradle 8.14+)
- **Android Studio** (Koala 2024.1.2+) or **IntelliJ IDEA** with Android plugin
- **Android SDK** with API 36 (Android 16)

### Quick Start
```bash
# Clone the repository
git clone https://github.com/moggedreality/Away-Time-Widget.git
cd Away-Time-Widget

# Build debug APK
./gradlew assembleDebug

# Run all checks (lint, detekt, ktlint, spotless)
./gradlew check

# Auto-format code
./gradlew format

# Build release AAB (requires key.properties)
./gradlew bundleRelease
```

### Release Signing
For local release builds, create `key.properties` (see `key.properties.example`):

```properties
storeFile=../release.keystore
storePassword=your_keystore_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

**Never commit `key.properties` or `.keystore` files!**

CI/CD uses GitHub Secrets for signing.

## Project Structure

```
Away-Time-Widget/
├── .github/
│   ├── workflows/ci.yml          # CI/CD pipeline
│   └── dependabot.yml            # Auto dependency updates
├── app/
│   ├── src/main/
│   │   ├── java/com/stxrs/awaytime/
│   │   │   ├── AwayTimeCalculator.kt   # Usage events → away time
│   │   │   ├── AwayTimeWidget.kt       # Widget provider & refresh timer
│   │   │   ├── WidgetRenderer.kt       # Draws widget as bitmap
│   │   │   ├── WidgetSettings.kt       # Options, presets, built-in looks
│   │   │   ├── Themes.kt               # Themes & Adaptive logic
│   │   │   ├── MainActivity.kt         # Single activity, insets, back handling
│   │   │   ├── HomeScreen.kt           # UI tabs
│   │   │   ├── DesignScreen.kt
│   │   │   ├── PresetsScreen.kt
│   │   │   ├── SettingsScreen.kt
│   │   │   ├── AppPicker.kt / Apps.kt  # Standby apps picker
│   │   │   ├── Motion.kt / Sheets.kt / NavBar.kt / Pickers.kt / Ui.kt
│   │   │   ├── FontStore.kt            # Custom font loader
│   │   │   ├── Battery.kt              # Battery Saver exemption
│   │   │   └── BootReceiver.kt         # Re-arms timer after boot
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── config/detekt/detekt.yml
├── gradle/
│   ├── libs.versions.toml        # Version catalog
│   └── wrapper/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── key.properties.example
├── suppression.xml
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── SECURITY.md
└── CHANGELOG.md
```

## Tech Stack

| Component | Version |
|-----------|---------|
| **Kotlin** | 2.3.20 |
| **Android Gradle Plugin** | 8.13.2 |
| **Gradle** | 8.14.3 |
| **compileSdk / targetSdk** | 36 (Android 16) |
| **minSdk** | 28 (Android 9) |
| **JVM Target** | 17 |
| **UI** | Plain `View` (no Compose, no AppCompat) |
| **Architecture** | Single module (`:app`) |

### Code Quality
| Tool | Purpose |
|------|---------|
| **ktlint** | Kotlin formatting (official style) |
| **detekt** | Static analysis (bugs, complexity, style) |
| **spotless** | Format enforcement + license headers |
| **Android Lint** | Android-specific checks |

Run all: `./gradlew check`

## Contributing

We welcome contributions! Please see:

- [Contributing Guide](CONTRIBUTING.md) — development setup, PR guidelines
- [Code of Conduct](CODE_OF_CONDUCT.md) — community standards
- [Security Policy](SECURITY.md) — vulnerability reporting

### Good First Issues
- Add new widget themes
- Improve font loading performance
- Add unit tests for `AwayTimeCalculator`
- Improve accessibility (content descriptions, touch targets)

## License

**MIT License** — see [LICENSE](LICENSE) for details.

```
MIT License

Copyright (c) 2024 Away Time Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

## Acknowledgments

- Android `UsageStatsManager` for usage data
- Kotlin & AndroidX teams for excellent tooling
- Contributors and testers

---

**Made with ❤️ for digital wellbeing**

[Report a Bug](https://github.com/moggedreality/Away-Time-Widget/issues/new?template=bug_report.md) · [Request Feature](https://github.com/moggedreality/Away-Time-Widget/issues/new?template=feature_request.md) · [Security](SECURITY.md)