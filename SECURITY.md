# Security Policy

## Scope

This policy covers `glycemicgpt-android-unofficial`: the GlycemicGPT phone app, Wear OS companion, and device data driver plugins (Tandem, Medtronic). For the backend API, web dashboard, or AI sidecar, see the [main platform repo](https://github.com/GlycemicGPT/GlycemicGPT)'s security policy instead.

## Reporting a Vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.** Report them privately so we can investigate and ship a fix before details are public.

- Use [GitHub's private vulnerability reporting](../../security/advisories/new) for this repository, or
- Email <security@glycemicgpt.org>.

Either channel reaches the project lead. If you don't get a response within a few days, they can also be reached through the channels listed in the main platform repo's `README.md`.

Please include:

- A description of the vulnerability and its potential impact
- Steps to reproduce, or a proof-of-concept if you have one
- The app version / commit SHA you tested against
- Whether the issue requires physical proximity to a BLE device to reproduce

We'll acknowledge your report, investigate, and keep you updated on the fix timeline. We ask that you give us a reasonable period to address the issue before any public disclosure.

## What Counts as a Vulnerability Here

This is a monitoring and analysis app with no therapeutic write surface -- it cannot dose insulin or change pump settings, and neither can any plugin built against the SDK in this repository. Given that, the vulnerability classes most relevant to this codebase are:

- **Incorrect BLE parsing that produces a wrong glucose, insulin-on-board, or dosing-history value.** A wrong number displayed to a person managing their diabetes is a safety issue, not just a bug -- please report it through this channel even if it doesn't look like a classic "security" bug.
- **Insecure local storage** -- glucose/insulin data escaping the encrypted Room database, or tokens escaping `EncryptedSharedPreferences`.
- **Sensitive data written to logs** -- pump serials, auth material, or raw BLE payloads that could identify a device or user.
- **Any code path that could be used to introduce a therapeutic write primitive**, even an unintentional one, into a capability interface.
- Standard app-level issues: insecure network calls to a configured backend, dependency vulnerabilities, unsafe deserialization, permission or intent handling bugs.

## Supported Versions

Security fixes are applied to the `develop` branch and released through the normal promotion process (`develop` -> `main`). Only the latest signed release is supported; please update before reporting an issue that may already be fixed.

## Monitoring-Only Posture

This app and every plugin shipped from this repository are read-only with respect to insulin delivery: there is no API on any capability interface for issuing a bolus, changing a basal rate, or otherwise writing therapeutic state to a pump. This is a deliberate safety and legal boundary, not an implementation gap -- see [CONTRIBUTING.md § Device Data Drivers](CONTRIBUTING.md#device-data-drivers). A vulnerability report proposing that such a boundary be treated as a bug to "fix" by adding write capability will be declined; that is a feature request for a different, unendorsed kind of project (see [CONTRIBUTING.md § Forks are not endorsed](CONTRIBUTING.md#device-data-drivers)).
