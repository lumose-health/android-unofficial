---
title: Security Testing
description: How security testing works for the GlycemicGPT mobile/Wear OS app in CI, and how to extend it.
---

# Security Testing

How security testing works for this repository's Kotlin codebase (`app/`, `wear-device/`,
`watchface/`, `plugins/`), and how to extend it as the app grows. Backend, web, and AI-sidecar
security testing (SAST for Python/TypeScript, DAST, auth pentests, API fuzzing, ZAP, nuclei)
lives in the main platform repo's docs -- see
[GlycemicGPT/GlycemicGPT security-testing.md](https://github.com/GlycemicGPT/GlycemicGPT/blob/main/docs/dev/security-testing.md).

### Medical Device Context

GlycemicGPT reads glucose data and insulin pump telemetry over Bluetooth. Security failures in
this context can have health consequences. The CI gates enforce a baseline: every PR must pass
security checks before merging.

## SAST (Static Analysis Security Testing)

**Tool:** [Semgrep](https://semgrep.dev/) with language-specific rulesets.

| Language | Rulesets | Scanned Paths |
|----------|----------|---------------|
| Kotlin | `p/kotlin`, `p/android`, `p/secrets` | `app/`, `wear-device/`, `watchface/`, `plugins/` |

Semgrep catches hardcoded secrets, injection patterns, and OWASP Top 10 issues at the code
level. In the full-suite workflow, SARIF results are uploaded to the GitHub Security tab for
centralized vulnerability tracking.

## Dependency Vulnerability Scanning (Gradle)

**Tool:** [Google OSV-Scanner](https://google.github.io/osv-scanner/), triggered on dependency
file changes plus a weekly schedule.

Coverage comes from [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html):
every module in this repo's build commits a `gradle.lockfile` that OSV-Scanner reads (it does
not parse `build.gradle.kts` or `libs.versions.toml`). Renovate itself runs in a container
without the Android SDK and cannot regenerate the Android lockfiles, so a
`regen-gradle-lockfiles.yml` workflow refreshes them on Renovate's Gradle PRs and pushes the
result back to the PR branch. After a manual dependency change, regenerate with
`./gradlew resolveAndLockAll --write-locks`. Two configuration families are deliberately
excluded from locking (see the comment in `build.gradle.kts`): AGP-internal Unified Test
Platform tooling classpaths (versions pinned by AGP itself, never shipped, only movable via an
AGP bump) and Kotlin `*DependenciesMetadata` views (not real classpaths).

Notes: the `:watchface` lockfile is legitimately empty -- it is a resource-only Watch Face
Format module with no dependencies. And because androidx/AGP-managed artifacts are scanned, a
CVE fixable only by an AGP bump can turn the gate red; the escape hatch while the bump lands is
an `osv-scanner.toml` suppression with a written reason.

### Handling findings

| Severity | Action | Timeline |
|----------|--------|----------|
| Critical / High | Block merge, fix immediately | Same PR or hotfix |
| Medium | Create issue, fix in current sprint | 1-2 weeks |
| Low | Triage -- fix if easy, suppress if not exploitable | Best effort |

### Suppressing false positives

Add entries to `osv-scanner.toml` in the repo root:

```toml
[[IgnoredVulns]]
id = "GHSA-xxxx-yyyy-zzzz"
reason = "Not exploitable -- only affects feature X which we don't use"
```

Every suppression must include a reason. Review suppressions quarterly.

## Mobile Security

The Android app has these security measures, verified through different mechanisms:

| Measure | Verification |
|---------|-------------|
| SQLCipher database encryption | Unit tests + CodeRabbit review |
| EncryptedSharedPreferences for tokens | Unit tests + CodeRabbit review |
| HTTPS enforcement (network_security_config) | Android Lint + CodeRabbit review |
| No sensitive data in logs | CodeRabbit BLE Protocol Safety check |
| Dependency vulnerabilities | OSV-Scanner (recursive Gradle scan) |
| Code quality / safety | CodeRabbit Medical Safety Review check |
| Hardcoded secrets, insecure patterns | Semgrep SAST (`p/kotlin`, `p/android`, `p/secrets`) |

No DAST scanning for mobile -- BLE protocol fuzzing would require hardware and is out of scope
for CI.

## Adding Security Tests for New Plugins

**Auto-covered** in most cases:

- **Android Gate**: Plugin code under `plugins/**` triggers the Android build/test/lint
  pipeline.
- **SAST**: Kotlin plugin code is scanned by Semgrep with `p/kotlin` and `p/android` rulesets.
- **Dependency scan**: If the plugin adds dependencies to the Gradle version catalog, the
  recursive Gradle sweep picks them up.

Only manual action needed: if a plugin introduces a new auth pattern (e.g. a new pairing or
credential flow), add coverage for it in that plugin's own test suite -- there is no shared
auth-flow test harness in this repo the way there is for the backend API.

## Running Locally

### SAST only

```bash
pip install semgrep
semgrep scan --config p/kotlin --config p/android --config p/secrets app/ wear-device/ watchface/ plugins/
```

### Dependency scan only

```bash
go install github.com/google/osv-scanner/v2/cmd/osv-scanner@v2.3.3
osv-scanner scan --recursive --config=osv-scanner.toml .
```
