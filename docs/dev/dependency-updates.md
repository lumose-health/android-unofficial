---
title: Dependency Updates
description: How Renovate proposes dependency updates for this repository, which ones auto-merge, and the security policy behind it.
---

# Dependency Updates

This repository uses [Renovate](https://docs.renovatebot.com/) to keep its Gradle
dependencies, the version catalog (`gradle/libs.versions.toml`), and its GitHub Actions
pins up to date. Renovate opens PRs against `develop`; a narrow, safe subset of those PRs
merges automatically, and everything else waits for maintainer review.

The config is `renovate.json5` at the repository root. It is validated on every change by the
**Renovate Config Validator** check (`renovate-config-validator.yml`, `--strict`).

> **Status: built, not yet live.** The machinery below is landed and dormant. Auto-merge does
> not happen until the [go-live steps](#go-live-checklist) are completed by a maintainer. Until
> then Renovate is not enabled and no PR merges without a human.

## The label-relay

Renovate itself never merges. Instead it *labels* each PR according to the tier rules in
`renovate.json5`, and a workflow relays a safe subset to GitHub's native auto-merge:

1. **Renovate** opens a PR and applies labels. Only its Tier B rule adds the `automerge` label;
   every manual tier adds a `needs-review`-family label instead (and never `automerge`).
2. **`auto-merge-renovate.yml`** reacts to the label event and *reconciles* auto-merge. If (and
   only if) all of its guards hold, it mints the `glycemicgpt-merge` App token and runs
   `gh pr merge --auto --squash`. `--auto` only *queues* the merge — GitHub still waits for every
   required status check to pass. If a Renovate PR later *loses* eligibility (the `automerge`
   label is removed, or a blocking label such as `security` arrives), the same workflow runs
   `gh pr merge --disable-auto` to retract the queued merge — GitHub native auto-merge is
   otherwise sticky and a label change alone never cancels it.
3. **`renovate-file-scope.yml`** ("Renovate File Scope", a required check) independently
   re-checks, on every branch head, that the PR touches only allowlisted, gate-covered files.

The relay does **not** decide *what* may auto-merge on its own; it trusts Renovate's `automerge`
label for the *intent* and the File Scope check for the *contents*. The two are independent so a
bug or compromise in one does not defeat the other.

### Residual risk: label management is a trusted operation

Auto-merge eligibility is decided from the PR's **current label set**, and the relay fires on the
label event. That means an actor who can *manage labels* (GitHub **Triage** role or higher) could,
in principle, take a manual-tier Renovate PR — say a `security-sensitive` crypto bump — strip its
`needs-review`/`security-sensitive` labels, add `automerge`, and push it through the bypass actor
without the maintainer review that tier exists to force. The required checks (7-day soak, OSV,
Android build) still run, but they would not catch a malicious-yet-not-yet-CVE release.

This is an accepted, bounded residual of the label-relay design (it only affects Renovate-authored,
dependency-file-scoped PRs). It is contained operationally: **Triage and label-management on this
repository must be restricted to trusted maintainers** (see the [go-live checklist](#go-live-checklist)).
Deeper, provenance-based hardening (deriving the tier from the diff rather than the labels) is tracked
with the promotion-automation / PR-provenance work, not this change.

### Auto-merge guards (all required)

`auto-merge-renovate.yml` enables auto-merge only when **all** of these hold:

- the PR author is `glycemicgpt-renovate[bot]` (cannot be impersonated);
- the base branch is `develop` (never `main`);
- the `automerge` label is present; **and**
- **none** of the manual-review labels — `security`, `needs-review`, `major`, `build-toolchain`,
  `security-sensitive`, `prerelease`, `ci-actions` — is present.

The last guard is defense in depth: any one of those labels means a human must merge, so
auto-merge is withheld even if `automerge` was somehow also applied.

## Auto-merge tiers

| Tier | What | Update types | Outcome |
|------|------|--------------|---------|
| **B — auto-merge** | Gradle *library* dependencies (`matchDepTypes: ["dependencies"]`), minus the never-auto-merge set below | patch, minor | `automerge` label → relay → native auto-merge after checks pass |
| **D — manual** | Gradle plugins / build toolchain, the Gradle wrapper, all majors, crypto/cipher libraries, the JitPack markdown renderer, the Wear watchface pre-release alphas, and all GitHub Actions bumps | any | a `needs-review`-family label → a human reviews and merges |

Tier B is expressed as an **all-negative** `matchPackageNames` (every library *except* the
excluded coordinates). If Gradle does not tag catalog libraries with the `dependencies` depType,
Tier B matches nothing and *everything* falls to manual review — a deliberate safe-fail.

### The 7-day soak

`minimumReleaseAge: "7 days"` with `internalChecksFilter: "strict"` means Renovate does **not
even open** a PR until the new version has been public for a week. This is the primary
supply-chain defense: nothing in CI inspects dependency *bytecode*, so the soak is what protects
against a freshly published malicious release. Real CVEs (via `osvVulnerabilityAlerts` +
`vulnerabilityAlerts`) bypass the soak and the rate limits — but they arrive labeled `security`,
which the auto-merge guards treat as manual-only.

## Never auto-merge — and why

These are excluded from Tier B by coordinate and routed to manual review. Auto-merging them
would move risk no gate can catch:

| Package(s) | Label | Why manual |
|------------|-------|------------|
| `org.bouncycastle:bcprov-jdk18on`, `net.zetetic:sqlcipher-android`, `androidx.security:security-crypto`, `org.openminimed:javasake` | `security-sensitive` | Cryptography / cipher / at-rest encryption and the Medtronic Sake crypto lib. A silent behavior change here can weaken encryption without failing a build. |
| `com.android.application`, `com.android.library`, `org.jetbrains.kotlin.*`, `com.google.devtools.ksp`, `com.google.dagger.hilt.android`, the Dagger-Hilt libraries, the Gradle wrapper | `build-toolchain` | AGP / Kotlin / KSP / Hilt / wrapper define *how* everything is built; a bad bump can change bytecode generation across the whole app. |
| `com.github.jeziellago:compose-markdown` | `needs-review` | JitPack (source-built) artifact — no reproducible binary provenance. |
| `androidx.wear.watchface:watchface-push`, `com.google.android.wearable.watchface.validator:validator-push-android` | `prerelease` | Pinned pre-release alphas; version bumps need manual watchface verification. |
| Any **major** update | `major` | Breaking changes by definition. |
| All GitHub Actions bumps (incl. digest re-pins) | `ci-actions` | Workflow `uses:` pins run with repo secrets; every change is maintainer-reviewed (see CODEOWNERS `/.github/workflows/`). |

Grouping rules that can hold an auto-eligible package are all scoped to `["patch","minor"]` and
exclude the manual coordinates by name, so a manual or major dependency can never ride a
groupmate's accumulated `automerge` label.

## The File Scope check

"Renovate File Scope" (`renovate-file-scope.yml`) is a required status check that re-runs on
every branch head. For an `automerge`-labeled Renovate PR it **fails** when a changed file is:

1. **outside the auto-merge file allowlist** —
   `gradle/libs.versions.toml`, `**/build.gradle.kts`, `settings.gradle.kts`, `**/gradle.lockfile`,
   `settings-gradle.lockfile`; or
2. **inside the allowlist but outside gate coverage** — the union of the paths-filters of the
   gates that green-skip (Android Gate + Dependency Scan Gate). A file there would auto-merge
   *unbuilt and unscanned*, because those gates skip (and pass) on paths they do not cover. The
   Security Scan Gate runs on every PR but neither builds nor scans a dependency change, so it
   does not close this gap.

`tools/**` is rejected outright even though it matches `**/build.gradle.kts` / `**/gradle.lockfile`:
it is out of Renovate scope (`ignorePaths`) and is a standalone build the Android Gate does not
build, so it must never auto-merge.

For any non-`automerge` or non-Renovate PR the check is a no-op pass, so it never blocks ordinary
human PRs. Because it re-evaluates on every head, a commit pushed *after* auto-merge is enabled
cannot smuggle an out-of-scope file past the merge.

If Renovate legitimately needs to touch a path outside the allowlist (e.g. a new lockfile
location), extend the allowlist in `renovate-file-scope.yml` **and** confirm a required gate
builds or scans it — in the same PR that adds the manager to `renovate.json5`.

## Regenerating Gradle lockfiles

Renovate runs without an Android SDK, so it cannot refresh the committed `gradle.lockfile` files
when it bumps a Gradle dependency. `regen-gradle-lockfiles.yml` closes that loop on Renovate PRs
with a two-job privilege split: an unprivileged, credential-free job runs the PR-branch build to
regenerate the lockfiles and uploads them as an artifact; a privileged job (Renovate App token,
`contents: write` only, hooks off, pinned to the source SHA) commits them back with `--signoff`.
`tools/medtronic-ble-spike` is out of Renovate scope (`ignorePaths`) and is not regenerated.

Both commits on a Renovate PR must satisfy [DCO](../../CONTRIBUTING.md) (`.github/dco.yml` enforces
a `Signed-off-by` on every commit, with no bot exemption). They are signed in two different places:
Renovate's own version-bump commit via `gitAuthor` + `commitBody` in `renovate.json5` (the
`Signed-off-by` trailer is built from the bot's canonical author identity, so it matches the commit
author GitHub records); the regenerated-lockfile commit via the `--signoff` above. The regen
`--signoff` covers only the lockfile commit — it does **not** sign Renovate's bump commit.

## Reviewing a manual (Tier D) PR

When a PR carries `needs-review`, `build-toolchain`, `security-sensitive`, `major`, `prerelease`,
or `ci-actions`:

1. **Read the release notes / changelog** Renovate links in the PR body. For `major`, look
   specifically for breaking changes and migration steps.
2. **Let the required checks run** — Android Gate (build + unit + lint), Security Scan Gate,
   Dependency Scan Gate (OSV), Workflow Lint. They must be green.
3. **For `security-sensitive`** (crypto/cipher/at-rest): confirm the change is an expected
   upstream release, not a re-published or yanked-and-replaced artifact, and that no encryption
   defaults changed. When in doubt, diff the upstream source.
4. **For `build-toolchain`** (AGP/Kotlin/KSP/Hilt/wrapper): a version catalog bump usually also
   needs a refreshed lockfile — the regen workflow handles that automatically on Renovate PRs.
   Watch for lint-engine or KSP incompatibilities (see the `hilt-navigation-compose` pin note in
   `libs.versions.toml`).
5. **For `prerelease`** (Wear watchface alphas): verify the watch face still renders and the
   push/validator flow works before merging.
6. **Merge manually** once satisfied. Do **not** add the `automerge` label to a Tier D PR — the
   auto-merge guards would still withhold it, and the File Scope check may fail it.

## Staged rollout

1. **This PR** — lands `renovate.json5` and the workflows in a correct-but-dormant state. Nothing
   auto-merges; Renovate is not yet enabled.
2. **Gradle dependency verification** (`verification-metadata.xml`) — a crypto-scoped fast-follow
   in its own PR.
3. **Go-live** — a maintainer performs the checklist below.

### Go-live checklist

These are **maintainer** steps, performed only after this PR merges:

- [ ] Install / enable the Renovate GitHub App on the repository.
- [ ] Enable the repository's **Dependency graph** and **Dependabot alerts** — the GitHub
      vulnerability-alert path that `vulnerabilityAlerts` reads.
- [ ] Separately confirm the **`osvVulnerabilityAlerts`** (OSV-based) path is active and producing
      alerts — it is a distinct mechanism from `vulnerabilityAlerts`.
- [ ] Register **`glycemicgpt-merge`** as a **pull-request-only** bypass actor on `develop`'s
      ruleset, and **verify it cannot bypass required status checks**: a ruleset bypass actor can
      in principle bypass every rule in the ruleset, so confirm (with a deliberately failing-check
      PR) that a merge is still rejected when a required check fails. The intent is that the bypass
      removes only the CODEOWNERS approval requirement; native `--auto` already waits for checks,
      but the ruleset config must not let the actor skip them.
- [ ] Register **"Renovate File Scope"** as a required status check on `develop`.
- [ ] Restrict **Triage / label management** on this repository to trusted maintainers — auto-merge
      eligibility is decided from labels, so relabeling is a security-relevant capability (see
      [Residual risk](#residual-risk-label-management-is-a-trusted-operation)).
- [ ] Confirm the `MERGE_APP_ID` / `MERGE_APP_PRIVATE_KEY` and `RENOVATE_APP_ID` /
      `RENOVATE_APP_PRIVATE_KEY` secrets are present.
- [ ] Watch the **first** real Renovate Gradle bump end-to-end: PR opened → lockfiles
      regenerated + pushed → checks green → (Tier B) auto-merge fires, or (Tier D) waits for review.

Until every box is checked, the whole pipeline stays dormant and safe.
