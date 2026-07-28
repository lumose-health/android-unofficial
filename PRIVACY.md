# Privacy

GlycemicGPT is a privacy-first project. This file covers `android-unofficial` -- the Android phone app, the Wear OS watch face and companion, and the Bluetooth device data driver plugins. The canonical, detailed privacy documentation for the platform as a whole lives in the platform repository at [docs/concepts/privacy.md](https://github.com/lumose-health/GlycemicGPT/blob/main/docs/concepts/privacy.md) and is the single source of truth; where this file and that one ever appear to differ, the canonical doc wins.

## On your devices and your backend: nothing is centralized

Your data -- glucose readings, insulin and pump data, alerts, settings, credentials -- lives on your devices and, if you configure a backend, in the database of the self-hosted GlycemicGPT instance **you** control.

On the phone, glucose and insulin data is held in an encrypted Room database, and tokens and credentials in `EncryptedSharedPreferences`. The watch keeps a small rolling cache of the values its watch face renders -- recent glucose, trend, and insulin on board -- in app-private storage so the face survives a restart. It stores no credentials, and the watch app requests no internet permission.

The app collects no telemetry, and no build the project distributes sends your health data or any usage analytics to the project. The outbound connections a running app makes are the ones you choose, and each is listed below. Two of them necessarily involve a third party -- Google Play Services carries phone-to-watch messages, and GitHub serves update checks and downloads -- so those services see the ordinary connection and request metadata any network call produces. Neither receives your health data.

- **Your pump**, over Bluetooth Low Energy, directly and locally.
- **Your backend**, if you configure one -- the self-hosted GlycemicGPT instance you point the app at, for AI analysis, alerting, and long-term storage. With no backend configured, the app still runs local BLE monitoring, on-device storage, and local threshold alerts, and makes no network calls for them.
- **Your watch**, over the Wear OS Data Layer -- a Google Play Services API, so the transport is provided by Google rather than by this app. The phone sends only the values the watch face renders (glucose, trend, insulin on board, and alerts) and never sends credentials to the watch.
- **GitHub's release API**, and only when you tap "Check for updates" in Settings. This is how the sideloaded app finds a newer APK. It is user-initiated, sends no health data, and never runs on its own. If you then choose to install the update, the APK itself downloads from `github.com` / `objects.githubusercontent.com`.

The one exception is a debug build you compile yourself after supplying your own Sentry DSN. That build sends error reports to *your* Sentry account, never the project's, and no APK the project publishes can be built that way. See below.

Which AI provider processes your data is determined by your backend's configuration, not by this app -- this app does not talk to AI providers directly. See the [platform repository's privacy documentation](https://github.com/lumose-health/GlycemicGPT/blob/main/docs/concepts/privacy.md) for the complete data-flow map.

## Error monitoring: the project's own development only

The project uses [Sentry](https://sentry.io/), donated through [Sentry for Good](https://sentry.io/for/good/), for error monitoring in **its own development and CI environments** -- to catch crashes before they reach a release.

**No build the project distributes phones home.** The Sentry DSN is compiled in only when a developer explicitly supplies it at build time on their own machine. It is **never baked into any published APK -- not the signed release builds, and not the CI-published `dev-latest` debug build.** Release builds compile the DSN in as an empty string, and the build fails outright if a DSN is supplied to a CI build, so the published debug APK cannot carry one either. When the DSN is blank the integration is a no-op. An APK you download from this repository's Releases page reports nothing to the project.

The mobile integration is locked down further than "off by default": error events only -- no performance traces, no profiling, no session or release-health telemetry, no Session Replay, no screenshots or view-hierarchy capture, and no user-interaction tracking. High-risk automatic breadcrumbs (HTTP, navigation, UI, network) are dropped wholesale, every surviving string field is run through a glucose/token/email scrubber, and the reporting IP is replaced with a non-routable placeholder so Sentry cannot geo-locate the connection.

**What an error report contains** (from the project's own environments, or from a developer who builds with their own DSN):

- Stack trace and exception type
- Operating system and runtime versions
- App version and commit hash
- The line of code that triggered the error

**What an error report never contains:**

- Blood glucose readings or any health data
- User identifiers, names, or contact information
- API keys, tokens, or credentials
- Device serial numbers or pump pairing IDs
- Database contents or query parameters
- Local variables captured in error contexts
- HTTP request or response bodies
- Health data or identifiers interpolated into exception or log messages

## Controlling error monitoring

- **Default -- nothing to opt out of.** Distributed builds carry no Sentry DSN, so an app you install from this repository's Releases page reports nothing to anyone; there is no project telemetry to disable.
- **Opt in for your own build.** If you *want* error monitoring, supply your own DSN at build time (`SENTRY_DSN` in the environment, or `-PsentryDsn`) when building a debug APK from source. Reports then go to *your* Sentry account, never the project's.

## Privacy questions

Report privacy concerns the same way as security disclosures, as described in [SECURITY.md](SECURITY.md): use [GitHub's private vulnerability reporting](../../security/advisories/new) for sensitive issues, or open a [GitHub Issue](../../issues/new/choose) for general privacy questions. Privacy is load-bearing for the project; reports here are taken seriously.

---

*Canonical version: the platform repository's [docs/concepts/privacy.md](https://github.com/lumose-health/GlycemicGPT/blob/main/docs/concepts/privacy.md). Last reviewed: 2026-07-23.*
