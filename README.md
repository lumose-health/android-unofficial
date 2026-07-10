# glycemicgpt-android-unofficial

Android phone app and Wear OS companion for [GlycemicGPT](https://github.com/GlycemicGPT/GlycemicGPT), an open source diabetes platform with AI-powered analysis at its core.

> **This is the mobile half of GlycemicGPT, split into its own repository.** The backend API, web dashboard, and AI sidecar live in the main platform repository: [GlycemicGPT/GlycemicGPT](https://github.com/GlycemicGPT/GlycemicGPT). This repo owns the phone app, the Wear OS watch face and companion, and the device data driver plugins (Tandem, Medtronic) that read from insulin pumps over Bluetooth.

---

> **IMPORTANT SAFETY WARNING**
>
> This software is **NOT** designed to replace your endocrinologist or healthcare provider. GlycemicGPT provides AI-generated suggestions only and should be used as a supplementary tool alongside professional medical care.
>
> *(This repository's own `MEDICAL-DISCLAIMER.md` is separate, in-progress work -- see [Open Questions / In Progress](#open-questions--in-progress) below. Until it lands, the main platform repo's [MEDICAL-DISCLAIMER.md](https://github.com/GlycemicGPT/GlycemicGPT/blob/main/MEDICAL-DISCLAIMER.md) applies to this codebase as well.)*

---

## Overview

This app connects directly to your CGM and insulin pump over Bluetooth (and, for supported devices, their cloud APIs) to give you real-time glucose monitoring, insulin-on-board tracking, and an AI-powered daily brief -- all on your phone and your wrist. It talks to a self-hosted GlycemicGPT backend (see the [main platform repo](https://github.com/GlycemicGPT/GlycemicGPT)) for AI analysis, alerting, and long-term data storage; the phone app itself also runs a useful subset of functionality without a backend configured (local BLE monitoring, on-device Room storage).

**Currently supported devices:**

| Device | Type | Connection | Status |
|--------|------|------------|--------|
| Tandem t:slim X2 | Insulin Pump | BLE (direct) | Verified |
| Tandem Mobi | Insulin Pump | BLE (direct) | Protocol-compatible (unverified on hardware) |
| Medtronic MiniMed 680G / 770G / 780G | Insulin Pump + CGM | BLE (direct) | Beta, read-only, unverified on hardware |

This app is a **monitoring and analysis platform**. It reads glucose, insulin-on-board, basal, and bolus data from supported pumps -- it does not, and will not, issue therapeutic writes (no bolus dosing, no basal rate changes, no pump-setting modifications). See [Device Data Drivers](CONTRIBUTING.md#device-data-drivers) in `CONTRIBUTING.md` for what the plugin SDK does and does not expose, and the safety framing below.

## Sideload Installation

This is unofficial, community-maintained software distributed outside the Google Play Store. Once this repository's release automation and signing keystore are provisioned, signed release APKs will be published on this repository's [GitHub Releases](../../releases) page. Until then, see [CONTRIBUTING.md](CONTRIBUTING.md) for building the app from source. When signed releases are available, installation will look like:

1. Download the latest signed APK from Releases
2. Enable "Install unknown apps" for your browser or file manager (Android will prompt you the first time)
3. Install the APK
4. Pair your pump and/or point the app at your self-hosted GlycemicGPT backend

Because this will be a sideloaded app, you will be responsible for verifying you're installing a build signed with the project's release key -- only install APKs from this repository's official Releases page once that pipeline exists.

## Architecture

| Component | Technology |
|-----------|------------|
| Phone app | Kotlin, Jetpack Compose, BLE |
| Wear OS companion | Kotlin, Wear Compose, Watch Face |
| Device data drivers | Community plugin architecture (Tandem, Medtronic) |

The phone app and Wear OS companion share an `applicationId` (`com.glycemicgpt.mobile`) so the Wearable Data Layer can route messages between them -- see `CLAUDE.md` before touching either module's manifest.

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) for full build setup, branching strategy, and code style.

A `shell.nix` at the repo root provisions the complete toolchain (JDK 17, Gradle, Android SDK platforms 34/35/36, emulator system images) -- run `nix-shell` from the repo root if you use Nix. Otherwise install the prerequisites listed in CONTRIBUTING.md.

```bash
# Build debug APKs (phone + Wear OS)
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug
```

## Contributing

We welcome contributions, especially new device data drivers -- see [Device Data Drivers](CONTRIBUTING.md#device-data-drivers) in `CONTRIBUTING.md`. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request, including the sign-off requirement (DCO) and the safety rules for anything that touches pump communication.

- Bug reports and feature requests: [Issues](../../issues)
- Questions and discussion: use the main platform repo's [Discussions](https://github.com/GlycemicGPT/GlycemicGPT/discussions) and [Discord](https://discord.gg/QbyhCQKDBs) -- this repo doesn't run a separate community channel

## Governance

See [GOVERNANCE.md](GOVERNANCE.md) for roles, decision-making, and how the project is run.

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See [LICENSE](LICENSE) for details. By contributing, you agree that your contributions will be licensed under the same terms -- see the DCO section in [CONTRIBUTING.md](CONTRIBUTING.md).

## Open Questions / In Progress

A few legal and community-health documents for this repository -- `MEDICAL-DISCLAIMER.md`, `PRIVACY.md`, SPDX license headers, and this project's `funding.json` entry -- are tracked as separate, in-progress work and are not yet present in this repository. Until they land, treat the main platform repo's equivalent documents as authoritative for this codebase. (`THIRD_PARTY_LICENSES.md` arrived with the extracted mobile tree and is present at the repo root.)

## Disclaimer

> **USE AT YOUR OWN RISK.** This software is experimental, is not approved by the FDA or any regulatory body for medical use, and does not control any medical device. AI-generated suggestions can be wrong. Always verify insulin dosing, carb ratio, or correction factor suggestions with your healthcare team before acting on them. See the safety warning at the top of this page, and the main platform repo's [MEDICAL-DISCLAIMER.md](https://github.com/GlycemicGPT/GlycemicGPT/blob/main/MEDICAL-DISCLAIMER.md) for the complete terms.
