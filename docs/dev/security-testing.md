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
| Android Gate | `android.yml` | Build/unit-test/lint (not a security gate, listed for context). |

Two more layers run outside these workflows and are intentionally **not** duplicated here:

- **CodeRabbit** (`.coderabbit.yaml`) runs `gitleaks`, `semgrep`, `osvScanner`, `detekt`,
  `checkov`, `actionlint`, and `shellcheck` on every PR, plus the medical-safety, BLE-protocol,
  and hardcoded-secret custom checks.
- **GitGuardian** runs org-wide as a separate push-time secret scanner.

### What was intentionally dropped vs the monorepo

The platform monorepo's `security-scan.yml` is multi-language and multi-stage. Only the mobile
SAST lane applies to this repository; the rest target services that do not exist here and would
be perpetually red, so they were **not** ported:

- **Semgrep Python / TypeScript** -- no backend or web code in this repo.
- **DAST / ZAP / nuclei / the Docker test stack** -- there is no HTTP service to scan. BLE
  protocol fuzzing would require pump hardware and is out of scope for CI.
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

## SAST (Static Analysis)

**Tool:** [Semgrep](https://semgrep.dev/) with the `p/kotlin`, `p/java`, and `p/secrets`
rulesets, over `app/`, `wear-device/`, `watchface/`, and `plugins/`.

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

The gate blocks only on ERROR-severity results; a clean tree reports "0 findings".

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

That gradle resolution succeeds with locking enabled (and that lockfiles are complete) can be
confirmed with any resolving task, e.g. `./gradlew help` or `./gradlew resolveAndLockAll
--write-locks` (which should produce no lockfile diff on an unchanged tree).
