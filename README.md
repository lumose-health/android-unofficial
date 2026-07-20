<h1 align="center">android-unofficial</h1>

<p align="center">
  <strong>Android phone app, Wear OS watch face, and BLE pump-driver plugins for <a href="https://github.com/GlycemicGPT/GlycemicGPT">GlycemicGPT</a></strong><br/>
  <em>Because no one should manage diabetes alone.</em>
</p>

<p align="center">
  <a href="https://github.com/GlycemicGPT/android-unofficial/actions/workflows/android.yml"><img src="https://img.shields.io/github/actions/workflow/status/GlycemicGPT/android-unofficial/android.yml?branch=develop&style=for-the-badge&labelColor=1e293b&label=Android&logo=githubactions&logoColor=white" alt="Android CI"></a>
  <a href="https://github.com/GlycemicGPT/android-unofficial/actions/workflows/security-scan.yml"><img src="https://img.shields.io/github/actions/workflow/status/GlycemicGPT/android-unofficial/security-scan.yml?branch=develop&style=for-the-badge&labelColor=1e293b&label=Security&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9IndoaXRlIiBzdHJva2Utd2lkdGg9IjIiPjxwYXRoIGQ9Ik0xMiAyMnM4LTQgOC0xMFY1bC04LTMtOCAzdjdjMCA2IDggMTAgOCAxMCIvPjwvc3ZnPg==&logoColor=white" alt="Security Scan"></a>
  <a href="https://github.com/GlycemicGPT/android-unofficial/releases/tag/dev-latest"><img src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fapi.github.com%2Frepos%2FGlycemicGPT%2Fandroid-unofficial%2Freleases%2Ftags%2Fdev-latest&query=%24.name&style=for-the-badge&labelColor=1e293b&label=Dev+Build&color=f59e0b&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9IndoaXRlIiBzdHJva2Utd2lkdGg9IjIiPjxsaW5lIHgxPSI2IiB4Mj0iNiIgeTE9IjMiIHkyPSIxNSIvPjxjaXJjbGUgY3g9IjE4IiBjeT0iNiIgcj0iMyIvPjxjaXJjbGUgY3g9IjYiIGN5PSIxOCIgcj0iMyIvPjxwYXRoIGQ9Ik0xOCA5YTkgOSAwIDAgMS05IDkiLz48L3N2Zz4=&logoColor=white" alt="Dev Build"></a>
  <a href="https://discord.gg/QbyhCQKDBs" target="_blank" rel="noopener noreferrer"><img src="https://img.shields.io/badge/Discord-Join-5865F2?style=for-the-badge&labelColor=1e293b&logo=discord&logoColor=white" alt="Join GlycemicGPT Discord server"></a>
</p>

<p align="center">
  <a href="#overview">Overview</a> •
  <a href="#relationship-to-the-platform">Platform</a> •
  <a href="#installation">Installation</a> •
  <a href="#development">Development</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#license">License</a> •
  <a href="#disclaimer">Disclaimer</a>
</p>

---

> **IMPORTANT SAFETY WARNING**
>
> This software is **NOT** designed to replace your endocrinologist or healthcare provider. GlycemicGPT provides AI-generated suggestions only and should be used as a supplementary tool alongside professional medical care. The platform repo's [MEDICAL-DISCLAIMER.md](https://github.com/GlycemicGPT/GlycemicGPT/blob/main/MEDICAL-DISCLAIMER.md) applies to this codebase.

---

## Overview

This repository is the mobile half of GlycemicGPT, an open source diabetes platform with AI-powered analysis at its core. It owns three things:

- **The Android phone app** -- connects directly to your insulin pump over Bluetooth for real-time glucose monitoring (readings relayed from the pump's linked CGM), insulin-on-board tracking, alerts, and an AI-powered daily brief. It pairs with a self-hosted GlycemicGPT backend for AI analysis, alerting, and long-term storage, and also runs a useful subset without any backend configured (local BLE monitoring, on-device storage, local threshold alerts).
- **The Wear OS watch face and companion** -- at-a-glance glucose, trend arrow, and insulin on board on your wrist, with complications and phone-relayed alerts.
- **The device data driver plugins** -- BLE pump drivers (Tandem, Medtronic) built on a capability-based plugin architecture designed for community contributions.

**Currently supported devices:**

| Device | Type | Connection | Status |
|--------|------|------------|--------|
| Tandem t:slim X2 | Insulin Pump | BLE (direct) | Verified |
| Tandem Mobi | Insulin Pump | BLE (direct) | Protocol-compatible (unverified on hardware) |
| Medtronic MiniMed 680G / 770G / 780G | Insulin Pump + CGM | BLE (direct) | Beta, read-only, unverified on hardware |

This app is a **monitoring and analysis platform**. It reads glucose, insulin-on-board, basal, and bolus data from supported pumps -- it does not, and will not, issue therapeutic writes (no bolus dosing, no basal rate changes, no pump-setting modifications).

## Relationship to the Platform

The backend API, web dashboard, and AI sidecar live in the main platform repository: [GlycemicGPT/GlycemicGPT](https://github.com/GlycemicGPT/GlycemicGPT). That repo is where the self-hosted Docker stack, AI provider configuration, caregiver alerting, and long-term data storage are developed; this repo is everything that runs on your phone and watch. Start with the platform repo's [README](https://github.com/GlycemicGPT/GlycemicGPT#readme) and [get-started guide](https://github.com/GlycemicGPT/GlycemicGPT/blob/main/docs/get-started.md) if you're new to GlycemicGPT.

## Installation

This is unofficial, community-maintained software distributed outside the Google Play Store. Builds are published on this repository's [Releases](../../releases) page; the [`dev-latest`](../../releases/tag/dev-latest) pre-release always carries the newest development build of the phone and watch debug APKs.

Step-by-step install guides:

- [Install the Android app](docs/mobile/install.md) -- downloading the APK, sideloading, and pointing the app at your backend
- [Install the Wear OS watch face](docs/mobile/wear-os.md) -- optional, installed over ADB

Because this is a sideloaded app, only install APKs from this repository's official Releases page.

## Development

A `shell.nix` at the repo root provisions the complete toolchain (JDK 17, Gradle, Android SDK platforms 34/35/36, emulator system images) -- run `nix-shell` from the repo root if you use Nix. Otherwise install the prerequisites listed in [CONTRIBUTING.md](CONTRIBUTING.md).

```bash
# Build debug APKs (phone + Wear OS)
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug
```

The phone app and Wear OS companion share an `applicationId` (`com.glycemicgpt.mobile`) so the Wearable Data Layer can route messages between them -- a mismatch causes silent message-delivery failures, so treat it as load-bearing before touching either module's manifest.

## Contributing

We welcome contributions, especially new device data drivers -- please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request.

- Bug reports and feature requests: [Issues](../../issues)
- Questions and discussion: the main platform repo's [Discussions](https://github.com/GlycemicGPT/GlycemicGPT/discussions) and [Discord](https://discord.gg/QbyhCQKDBs) -- this repo doesn't run a separate community channel
- Roles and decision-making: [GOVERNANCE.md](https://github.com/lumose-health/.github/blob/main/GOVERNANCE.md)

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See [LICENSE](LICENSE) for details. By contributing, you agree that your contributions will be licensed under the same terms.

## Disclaimer

> **USE AT YOUR OWN RISK.** This software is experimental, is not approved by the FDA or any regulatory body for medical use, and does not control any medical device. AI-generated suggestions can be wrong. Always verify insulin dosing, carb ratio, or correction factor suggestions with your healthcare team before acting on them. See the platform repo's [MEDICAL-DISCLAIMER.md](https://github.com/GlycemicGPT/GlycemicGPT/blob/main/MEDICAL-DISCLAIMER.md) for the complete terms.

---

<p align="center">
  <sub>Built with care for the diabetes community. Stay safe.</sub>
</p>
