# Security Policy

## Supported Versions

We release security updates for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 2.2.x   | :white_check_mark: |
| < 2.2   | :x:                |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue, please report it responsibly.

### How to Report

**Please do NOT report security vulnerabilities via public GitHub issues.**

Instead, email us at: **security@awaytime.app**

Include the following information:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Any suggested fixes (optional)

### What to Expect

1. **Acknowledgment** — We'll acknowledge receipt within 48 hours
2. **Assessment** — We'll assess the vulnerability and determine severity
3. **Fix Timeline** — We'll provide an estimated timeline for a fix
4. **Coordination** — We'll coordinate disclosure with you

### Disclosure Policy

- We aim to release fixes within **30 days** for critical issues
- We'll credit reporters (unless anonymity is requested)
- We'll publish a security advisory after the fix is released

## Security Features

Away Time is designed with privacy and security in mind:

- **No internet permission** — The app cannot transmit any data
- **No analytics/tracking** — Zero data collection
- **UsageStatsManager only** — Reads system-provided usage events (requires explicit user grant)
- **Local storage only** — All settings stored locally via DataStore/SharedPreferences
- **No background services** — Widget updates via `AlarmManager`/`WorkManager` only

## Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| `PACKAGE_USAGE_STATS` | Read app/screen usage events | Yes (user-granted via Settings) |
| `RECEIVE_BOOT_COMPLETED` | Re-arm widget timer after reboot | Yes |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Exempt from Doze/Battery Saver | Optional (user-granted) |

## Building Securely

- Release builds are signed with a dedicated keystore
- Keystore credentials are **never** committed (stored in CI secrets)
- Dependencies are scanned for vulnerabilities via GitHub Dependabot
- Version catalog ensures reproducible builds

## Contact

For security concerns: **security@awaytime.app**

For general questions: [GitHub Issues](https://github.com/moggedreality/Away-Time-Widget/issues)