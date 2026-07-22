---
title: Security Testing
description: How security testing works for the GlycemicGPT mobile/Wear OS app in CI, and how to extend it.
---

# Security Testing

How security testing works for this repository's Kotlin/Java codebase (`app/`, `wear-device/`,
`watchface/`, `plugins/`), and how to reproduce each gate locally. Backend, web, and AI-sidecar
security testing (SAST for Python/TypeScript, DAST, auth pentests, API fuzzing, ZAP, nuclei)
lives in the main platform repo's docs -- see
[GlycemicGPT/GlycemicGPT security-testing.md](https://github.com/GlycemicGPT/GlycemicGPT/blob/main/docs/dev/security-testing.md).

## Medical Device Context

GlycemicGPT reads glucose data and insulin pump telemetry over Bluetooth. Security failures in
this context can have health consequences, so every PR must pass the security gates below before
merging.

## What runs in CI

| Gate (required status name) | Workflow | What it does |
|-----------------------------|----------|--------------|
| **Security Scan Gate** | `security-scan.yml` | Semgrep SAST over Kotlin/Java sources; fails on HIGH/ERROR findings. |
| **Dependency Scan Gate** | `dependency-scan.yml` | OSV-Scanner over the committed Gradle lockfiles; fails on any known CVE. |
| **Workflow Lint** | `workflow-lint.yml` | actionlint + shellcheck over workflows; SHA-pin and composite-action guards. |
| **Workflow Security** | `workflow-security.yml` | zizmor (Medium+) over workflows and composite actions; `pull_request_target`-checkout guard. |
| Android Gate | `android.yml` | Build/unit-test/lint (not a security gate, listed for context). |

All four security gates plus the Android Gate are **required** status checks on `develop`
(the "Protect develop" ruleset). Each runs on every PR (`Security Scan Gate`, `Workflow Lint`,
`Workflow Security`) or reports through an always-running aggregation gate that fails closed on a
build/scan failure and passes only when there are no relevant changes (`Android Gate`,
`Dependency Scan Gate`). None of these five gates use `continue-on-error`.

Two more layers run outside these workflows and are intentionally **not** duplicated here:

- **CodeRabbit** (`.coderabbit.yaml`) runs `gitleaks`, `semgrep`, `osvScanner`, `detekt`,
  `checkov`, `actionlint`, and `shellcheck` on every PR, plus the medical-safety, BLE-protocol,
  and hardcoded-secret custom checks.
- **GitGuardian** runs org-wide as a separate push-time secret scanner.

### What was intentionally dropped vs the monorepo

The platform monorepo's security CI (`security-scan.yml`, the full DAST/pentest suite in
`security-full-suite.yml`, and `dependency-scan.yml`) is multi-language and multi-stage. Of those,
only Android SAST (Semgrep), dependency CVE scanning (OSV over Gradle lockfiles), and secret
scanning apply to this repository (workflow hardening -- the `Workflow Lint` and `Workflow
Security` gates in the table above -- is a separate concern, not part of these three). The rest
target a running server/API/web app that does not exist here and would be perpetually red, so they
were **not** ported:

- **Semgrep Python / TypeScript** -- no backend or web code in this repo.
- **DAST / ZAP / nuclei / the Docker test stack** (`security-full-suite.yml`) -- there is no HTTP
  service to scan. BLE protocol fuzzing would require pump hardware and is out of scope for CI.
- **Auth pentests** (`test-auth-flows.py`) and **API fuzzing** (`fuzz-api.py`, self-adapting via
  `/openapi.json`) -- **N/A**: this repo ships no authenticated server or HTTP API surface to
  exercise, so there is nothing for these gates to test.
- **`evaluate-sast.py` + `create-finding-issues.py`** -- the monorepo's issue-filing pipeline
  depends on the org SECURITY GitHub App and a `scripts/security/` Python toolchain that this
  mobile-only repo does not carry. The Security Scan Gate uses a plain
  exit-non-zero-on-HIGH check instead; findings surface on the PR via the run log, the uploaded
  `semgrep-kotlin` artifact, and CodeRabbit.

### Secret scanning decision

The Security Scan Gate already runs Semgrep's `p/secrets` ruleset over `app/`, `wear-device/`,
`watchface/`, and `plugins/`. A **dedicated** secret-scanning gate was deliberately **not** added:
`gitleaks` runs on every PR through CodeRabbit, and **GitGuardian runs org-wide** at push time. A
third pass over the same tree would add maintenance and noise without new coverage. The
`p/secrets` lane stays because it is free within the SAST run and gives an in-repo, PR-blocking
signal independent of the external services.

### Android APK SAST (MobSF) decision -- declined for now

An Android-specific SAST such as [MobSF](https://github.com/MobSF/Mobile-Security-Framework-MobSF)
(or an APK/AAB scan) was evaluated and **deliberately declined** at this time.

It is **not** simply redundant with Semgrep. MobSF operates on the built artifact and covers a
class that source-level Kotlin/Java SAST does not:

- `AndroidManifest.xml` posture -- exported components, over-broad permissions, `debuggable`,
  `allowBackup`, `usesCleartextTraffic`, weak `network_security_config`.
- Packaging / build configuration -- signing scheme, `minSdk` exposure, insecure library
  bundling.
- Secrets embedded in the **packaged** APK (resources, `strings.xml`, assets, native libraries),
  which never appear in the source tree Semgrep's `p/secrets` lane scans.

It is declined because the cost outweighs the benefit for this repository's current shape:

- This is a **monitoring-only, read-only** app (no therapeutic/write surface) distributed by
  **GitHub side-load**, not through the Play Store, so it faces neither Play's pre-launch security
  report nor a store-review threat model.
- On an app that legitimately requires BLE, location (for BLE scanning), and foreground-service
  permissions, MobSF's manifest/permission heuristics are **high-false-positive** and would
  generate recurring triage noise.
- Running it in CI means building and scanning an APK on every PR (a heavier, slower pipeline)
  plus maintaining a suppression baseline -- maintenance the current threat model does not
  justify.

The manifest/network-security posture is meanwhile covered defensively by Android Lint (in the
Android Gate) and CodeRabbit review. Source-level secret exposure is caught by Semgrep
`p/secrets`, CodeRabbit `gitleaks`, and org-wide GitGuardian, and no signing material is
committed. Secrets embedded only in the built artifact (not in source) are not scanned today --
an accepted residual gap that the revisit trigger below is meant to catch.

**Revisit trigger:** add an APK/manifest SAST gate if this app is ever distributed via the
**Play Store**, or if **any write/therapeutic surface** (bolus, basal, pump-setting, or other
device-command capability) is introduced -- either change raises the threat model enough to
warrant the artifact-level coverage.

## SAST (Static Analysis)

**Tool:** [Semgrep](https://semgrep.dev/) with the `p/kotlin`, `p/java`, and `p/secrets`
rulesets, over `app/`, `wear-device/`, `watchface/`, and `plugins/`.

Non-shipped research spikes under `tools/` (e.g. `tools/medtronic-ble-spike/`) fall outside this
scan scope and outside the `plugins/shipped/**/ble/**` review path rules; they are reviewed as
ordinary Kotlin, not as shipped BLE drivers. CodeRabbit's semantic BLE Protocol Safety check is
expected to apply to any BLE code a PR touches, including such spikes.

**Gate policy:** fail on **HIGH/ERROR-severity** findings only (mirroring the monorepo's
`evaluate-sast.py`). WARNING/INFO findings are printed but do not block. A crashed scanner fails
closed.

`AesEcb.java` is excluded: it is vendored from the OpenMinimed JavaSake library and is
re-vendored, not patched, on upstream bumps (#695/#696), so its intentional legacy AES/ECB usage
must not fail the gate. JavaSake is currently consumed as the Maven artifact
`org.openminimed:javasake` (no vendored source in-tree), so the exclusion is inert today but is
kept for the re-vendor case. `.gradle` (build cache) is also excluded.

## Dependency Vulnerability Scanning (Gradle)

**Tool:** [Google OSV-Scanner](https://google.github.io/osv-scanner/) `v2.3.3`, triggered on
dependency-file changes plus a weekly schedule.

Coverage comes from [Gradle dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html):
every module in this repo's build commits a `gradle.lockfile` that OSV-Scanner reads (it does not
parse `build.gradle.kts` or `libs.versions.toml`). `--recursive` auto-discovers each module
lockfile; the root `settings-gradle.lockfile` has a non-standard basename and needs the
`--lockfile=gradle.lockfile:settings-gradle.lockfile` parse-as hint. Renovate itself runs in a
container without the Android SDK and cannot regenerate the Android lockfiles, so
`regen-gradle-lockfiles.yml` refreshes them on Renovate's Gradle PRs and pushes the result back to
the PR branch. (That path is **unproven** until the first Renovate Gradle PR -- Renovate is not
yet configured on this repository.) After a manual dependency change, regenerate with
`./gradlew resolveAndLockAll --write-locks`.

Two configuration families are deliberately excluded from locking (see the comment in
`build.gradle.kts`): AGP-internal Unified Test Platform tooling classpaths (versions pinned by AGP
itself, never shipped, only movable via an AGP bump) and Kotlin `*DependenciesMetadata` views (not
real classpaths). The `:watchface` lockfile is legitimately empty -- it is a resource-only Watch
Face Format module with no dependencies.

**First-scan triage:** the initial recursive scan across ~1,175 resolved packages reported
**no known vulnerabilities**, so `osv-scanner.toml` ships with no active suppressions. Because
androidx/AGP-managed artifacts are scanned, a CVE fixable only by an AGP bump can turn the gate
red before the bump lands; the escape hatch is an `osv-scanner.toml` suppression with a written
reason.

### Suppressing false positives

Add entries to `osv-scanner.toml` in the repo root:

```toml
[[IgnoredVulns]]
id = "GHSA-xxxx-yyyy-zzzz"
reason = "Not exploitable -- only affects feature X which we don't use"
```

Every suppression must include a reason. Prefer bumping the dependency over suppressing. Review
suppressions quarterly.

## Workflow Lint

**Tool:** [actionlint](https://github.com/rhysd/actionlint) `1.7.7` with its
[shellcheck](https://www.shellcheck.net/) `0.10.0` integration (both installed as
SHA-256-verified pinned binaries), over `.github/workflows/**`. On top of actionlint's
expression/context/shell checks, the gate adds two guards:

- **SHA-pin enforcement** over `.github/workflows/**` and `.github/actions/**`: every third-party
  `uses:` must pin a full 40-hex commit SHA (local `./` actions and `docker://` refs are exempt).
- **Composite-action secrets-context guard**: actionlint cannot lint a composite action
  *definition* (it parses `action.yml` as a malformed workflow), so this flags any `secrets.*`
  reference inside `.github/actions/**` -- the class of bug that broke `op-load-signing-secrets`,
  since composite actions cannot read the `secrets` context and such references resolve to empty.

## Mobile Security Measures

| Measure | Verification |
|---------|-------------|
| SQLCipher database encryption | Unit tests + CodeRabbit review |
| EncryptedSharedPreferences for tokens | Unit tests + CodeRabbit review |
| HTTPS enforcement (network_security_config) | Android Lint + CodeRabbit review |
| No sensitive data in logs | CodeRabbit BLE Protocol Safety check |
| Dependency vulnerabilities | OSV-Scanner (recursive Gradle scan) |
| Hardcoded secrets | Semgrep `p/secrets`, CodeRabbit gitleaks, org-wide GitGuardian |
| Insecure code patterns | Semgrep SAST (`p/kotlin`, `p/java`, `p/secrets`) |
| Medical safety / conversion correctness | CodeRabbit Medical Safety Review check |

No DAST scanning for mobile -- BLE protocol fuzzing would require hardware and is out of scope
for CI.

## Adding Security Coverage for New Plugins

Auto-covered in most cases:

- **Android Gate**: plugin code under `plugins/**` triggers the build/test/lint pipeline.
- **SAST**: Kotlin/Java plugin code is scanned by Semgrep.
- **Dependency scan**: a new plugin's `gradle.lockfile` is picked up by the recursive sweep once
  the module is added to the build (regenerate lockfiles after adding dependencies).

Only manual action needed: if a plugin introduces a new auth or pairing flow, add coverage for it
in that plugin's own test suite -- there is no shared auth-flow test harness in this repo the way
there is for the backend API.

## Reproducing a gate locally

### Security Scan Gate (SAST)

```bash
pip install semgrep==1.169.0
semgrep scan \
  --config p/kotlin --config p/java --config p/secrets \
  --exclude 'AesEcb.java' --exclude '.gradle' \
  app/ wear-device/ watchface/ plugins/
```

This reproduces the scan only. The gate additionally captures the JSON output,
fails on ERROR-severity (HIGH) findings via a `jq` check, and fails closed if
Semgrep itself errors (exit code >= 2). A clean tree reports "0 findings".

### Dependency Scan Gate

```bash
go install github.com/google/osv-scanner/v2/cmd/osv-scanner@v2.3.3
osv-scanner scan \
  --lockfile=gradle.lockfile:settings-gradle.lockfile \
  --recursive --config=osv-scanner.toml .
```

### Workflow Lint

```bash
# actionlint + shellcheck available on PATH (brew, go install, or the pinned
# release binaries the workflow downloads).
actionlint -shellcheck=shellcheck
```

CI runs the same actionlint (with shellcheck) and additionally (a) fails if any
third-party action is not pinned to a full commit SHA and (b) rejects any
`secrets.*` reference inside a composite action; the local `actionlint` command
alone does not check those two guards.

That gradle resolution succeeds with locking enabled AND that the committed lockfiles are
complete is verified with a task that actually resolves the locked classpaths: run
`./gradlew resolveAndLockAll --write-locks`, which must produce no lockfile diff on an unchanged
tree (or inspect a module's resolved graph with `./gradlew :app:dependencies`). `./gradlew help`
prints BUILD SUCCESSFUL without resolving any configuration, so it does not validate lockfile
completeness.
