# Contributing to glycemicgpt-android-unofficial

Thanks for your interest in contributing! Whether you're fixing a typo, squashing a bug, or building a whole new device data driver -- we appreciate you.

This guide covers the Android phone app and Wear OS companion. **Org-wide policy** -- project roles, the AI-attribution policy, and the security posture -- lives in the [master contributing guide](https://github.com/lumose-health/.github/blob/main/CONTRIBUTING.md); this guide defers to it and focuses on the Android/Wear mechanics. For backend, web dashboard, or AI sidecar contributions, see the [platform repo](https://github.com/lumose-health/GlycemicGPT)'s `CONTRIBUTING.md`.

> **Where to open mobile PRs during the repository split.** The Android and Wear OS apps are being extracted here from the platform repo, but that split isn't finished yet. Until it is: **mobile PRs are accepted in the platform repo today; maintainers port them here with your authorship preserved; once the split completes, mobile PRs move here.** So for now, open mobile contributions against the [platform repo](https://github.com/lumose-health/GlycemicGPT)'s `develop`; a maintainer cherry-picks each merged change into this repo (tracked in [`docs/dev/monorepo-port-ledger.md`](docs/dev/monorepo-port-ledger.md)), so you're credited in both places.

---

## Safety First -- Please Read

**This app talks to real diabetes management hardware over Bluetooth. Incorrect data parsing can directly impact health decisions.**

Before writing any code, please understand these non-negotiable rules:

- **All** AI-generated outputs surfaced by the app must be clearly labeled as **suggestions, not medical advice**.
- **Test thoroughly** -- a wrong number on a glucose chart is not just a UI bug, it's a safety issue.
- Safety limits (glucose range, max bolus, max basal) are enforced by the platform via `SafetyLimits`. Plugins in this repo must validate incoming pump and CGM history values against them; when a reading falls outside the limits, plugins must discard it (do not return, emit, or persist it) and log the rejection with the violated limit.
- **No device control** -- this app is a monitoring and analysis platform, full stop.

### Device Data Drivers

This app is a monitoring and analysis platform. The plugin SDK in `plugins/pump-driver-api/` exists for one purpose: **community-built device data drivers that read from new hardware**. Pumps, CGMs, BGMs, and other diabetes devices all have proprietary protocols, and a plugin SDK is the only realistic way to support the long tail of devices the community uses. Plugins read glucose values, insulin-on-board, basal rates, bolus history, and pump status. They do not control devices.

**This repository does not provide, distribute, document, or solicit plugins that expose any therapeutic write or control surface -- no bolus dosing, no basal rate changes, no pump-setting modifications.** This applies to every official build (debug and signed release APKs) and to every contribution merged into this repository. Pull requests that introduce therapeutic write primitives will not be merged. Non-therapeutic device-management operations that already exist in the SDK (CGM calibration, BLE pair/unpair, connect/disconnect) are session and lifecycle operations -- not therapy -- and remain permitted.

**Forks are not endorsed.** Forks of this project that add device control capabilities operate outside this project. The maintainers do not review them, recommend them, accept liability for them, or accept contributions to this repository whose intent is to enable them. Users who choose to run such forks become the manufacturer of their own personal medical device, consistent with the legal posture of Loop, AndroidAPS, and other DIY diabetes projects.

**Platform safety enforcement.** The plugin SDK has no insulin delivery primitives -- there is no API on any capability interface for issuing a bolus, modifying basal rates, or otherwise writing therapeutic state to a pump. Device-management commands that *do* exist in the SDK (CGM calibration, BLE pair/unpair, connect/disconnect) are session/lifecycle operations, not therapy. Safety constraints (glucose range, max bolus, max basal) are platform-defined and backend-synced; plugins use them to drop implausible readings and cannot bypass them.

**Contributing a data driver:**

1. Pick a device that isn't already supported.
2. Open an issue describing the device, the protocol you intend to use, and the data you'll surface.
3. Submit a PR with a new Gradle module under `plugins/shipped/<device-name>/` (these modules are compiled into official builds), declaring only capabilities from the official read-only enum. Typical capabilities: `GLUCOSE_SOURCE`, `INSULIN_SOURCE`, `PUMP_STATUS`, `BGM_SOURCE`, `CALIBRATION_TARGET`, `BOLUS_CATEGORY_PROVIDER`.
4. Include unit tests, especially for parsing and `SafetyLimits` validation of incoming values.
5. Existing plugins serve as reference implementations. Runtime-loaded plugins (under `plugins/example/`, if present) are a separate, advanced contribution path -- not compiled into official builds.

**Shipped device data drivers:**

| Driver | Module | Transport | Reads | Status |
|---|---|---|---|---|
| Tandem (t:slim X2 / Mobi) | `:tandem-pump-driver` (`plugins/shipped/tandem/`) | BLE (central) | Glucose, IoB, basal, bolus history, pump status | Stable -- reference implementation |
| Medtronic MiniMed (680G / 770G / 780G) | `:medtronic-pump-driver` (`plugins/shipped/medtronic/`) | BLE (peripheral, advertise-and-wait) | Sensor glucose, IoB, basal, bolus history, reservoir, battery | Beta, read-only |

Both drivers are **read-only** -- they read data from the pump and never issue therapeutic writes.

---

## Table of Contents

- [Project Roles](#project-roles)
- [Ways to Contribute](#ways-to-contribute)
- [Development Setup](#development-setup)
- [Branching & Workflow](#branching--workflow)
- [Commit Messages](#commit-messages)
- [Developer Certificate of Origin (DCO)](#developer-certificate-of-origin-dco)
- [Before You Submit](#before-you-submit)
- [Pull Request Process](#pull-request-process)
- [Code Style](#code-style)
- [AI-Assisted Development & Attribution Policy](#ai-assisted-development--attribution-policy)
- [Project Structure](#project-structure)
- [License](#license)
- [Questions?](#questions)

---

## Project Roles

Most people start as contributors -- just open a PR, file an issue, or comment on a discussion. The full role model (Contributor, Committer, Maintainer, Project Lead), how decisions are made, and how branch protection works are documented once, org-wide, in [GOVERNANCE.md in the org `.github` repo](https://github.com/lumose-health/.github/blob/main/GOVERNANCE.md).

---

## Ways to Contribute

- **Report bugs** -- open an [Issue](../../issues/new/choose)
- **Add a device data driver** -- see [Device Data Drivers](#device-data-drivers) above
- **Improve documentation** -- typos, unclear instructions, missing guides
- **Write tests** -- more coverage is always welcome, especially around BLE parsing
- **Review PRs** -- fresh eyes catch things automated checks can't

Before opening an issue, please search existing issues to avoid duplicates.

---

## Development Setup

### Prerequisites

| Component | You Need |
|-----------|----------|
| Phone app | JDK 17, Android SDK Platform 35 (targetSdk 35, minSdk 30) |
| Wear OS | JDK 17, Android SDK Platform 36, Wear OS system image. Requires phone + watch emulators paired via ADB. |

### Quick Start

```bash
# 1. Fork and clone
git clone https://github.com/<your-username>/glycemicgpt-android-unofficial.git
cd glycemicgpt-android-unofficial

# 2. Add upstream remote
git remote add upstream https://github.com/GlycemicGPT/glycemicgpt-android-unofficial.git

# 3. Install the git commit-msg hook (strips prohibited AI-attribution lines,
#    and locally warns if a commit is missing its DCO sign-off)
cp .githooks/commit-msg .git/hooks/commit-msg
chmod +x .git/hooks/commit-msg

# 4. Build debug APKs (phone + Wear OS)
./gradlew assembleDebug

# 5. Run unit tests
./gradlew testDebugUnitTest

# 6. Run lint
./gradlew lintDebug
```

Full BLE integration testing requires a physical phone paired with a supported pump -- see the emulator/physical-device workflow in `CLAUDE.md`.

> **Need to test against a backend?** This app can run in a mobile-only mode (local BLE monitoring, on-device storage) or point at a self-hosted GlycemicGPT backend for AI analysis, alerting, and long-term storage. See the [main platform repo](https://github.com/GlycemicGPT/GlycemicGPT) to stand one up for development.

---

## Branching & Workflow

We use a **develop/main** branching model:

```
feature branch --> squash merge --> develop --> merge --> main
                                      |                     |
                                  dev builds           stable releases
                                  debug APKs           signed APKs
```

- **`develop`** is the integration branch. **All contributor PRs target `develop`.** (During the repository split, mobile contributions currently go to the platform repo -- see the [note at the top](#contributing-to-glycemicgpt-android-unofficial); the mechanics below apply to changes made in this repo, including maintainer ports and post-split contributions.)
- **`main`** is the stable release branch. Do **not** target PRs to `main`.

### Creating a Feature Branch

```bash
git checkout develop && git pull
git checkout -b feat/my-feature
# ... make changes ...
git push -u origin feat/my-feature
# Create PR targeting develop
```

### Branch Naming

| Prefix | Usage |
|--------|-------|
| `feat/` | New features |
| `fix/` | Bug fixes |
| `docs/` | Documentation |
| `refactor/` | Code restructuring |
| `ci/` | CI/CD changes |

---

## Commit Messages

We use [Conventional Commits](https://www.conventionalcommits.org/). This drives automated CHANGELOG generation via [release-please](https://github.com/googleapis/release-please).

| Prefix | Usage | CHANGELOG |
|--------|-------|-----------|
| `feat:` | New features | Visible |
| `fix:` | Bug fixes | Visible |
| `perf:` | Performance improvements | Visible |
| `docs:` | Documentation only | Visible |
| `refactor:` | Code restructuring | Visible |
| `ci:` | CI/CD changes | Visible |
| `chore:` | Maintenance, deps | Hidden |
| `test:` | Adding/updating tests | Hidden |

**Examples:**
```
feat: add reservoir level card to dashboard
fix: prevent BLE reconnect loop on pump timeout
docs: document Medtronic pairing flow
refactor: extract packet parser into separate module
chore(deps): update dependency androidx.compose to 1.8.0
```

---

## Developer Certificate of Origin (DCO)

Every commit in this repository must include a `Signed-off-by:` trailer, certifying that you wrote the code (or otherwise have the right to submit it) under the [Developer Certificate of Origin](https://developercertificate.org).

**Quick start:** add `-s` to your commit command and Git appends the trailer automatically.

```bash
git commit -s -m "fix: prevent BLE reconnect loop on pump timeout"
```

This produces a trailer like:

```
Signed-off-by: Jane Doe <jane@example.com>
```

using the name and email from your Git config (`git config user.name` / `git config user.email`) -- use your real name and a reachable email, not an alias.

If you forgot `-s` on your most recent commit, amend it:

```bash
git commit --amend -s --no-edit
```

For multiple commits on a branch, sign off each one with an interactive rebase, or simply re-commit with `-s` before pushing.

**Why DCO and not a CLA:** the DCO is a lightweight, per-commit provenance attestation -- it certifies you have the right to submit your contribution under this project's license. It does **not** grant the project any relicensing rights beyond that. This repository does not require a Contributor License Agreement (CLA).

PRs with unsigned commits will be blocked from merging until every commit in the branch carries a valid `Signed-off-by:` trailer.

---

## Before You Submit

**Run these checks locally before pushing.**

```bash
./gradlew testDebugUnitTest  # Unit tests (phone + Wear OS)
./gradlew lintDebug          # Lint (phone + Wear OS)
./gradlew assembleDebug      # Build check
```

### Pre-Review with CodeRabbit CLI (Optional but Recommended)

This project uses [CodeRabbit](https://www.coderabbit.ai) for automated AI code review on every PR. You can catch the same issues locally before pushing:

```bash
curl -fsSL https://cli.coderabbit.ai/install.sh | sh
coderabbit auth login
coderabbit review --plain --type uncommitted
```

### Final Checks

- [ ] All tests pass
- [ ] Linting passes with no new warnings
- [ ] No hardcoded secrets, API keys, tokens, or credentials in your code
- [ ] New functionality has tests
- [ ] Commit messages follow [Conventional Commits](#commit-messages) format
- [ ] Every commit is signed off (`Signed-off-by:`) -- see [DCO](#developer-certificate-of-origin-dco) above
- [ ] Your branch is up to date with `develop`

---

## Pull Request Process

1. Push your feature branch to your fork
2. Open a PR **targeting `develop`** (not `main`)
3. Fill out the PR template completely
4. Link related issues using `Fixes #123` or `Relates to #123`

CI runs automatically on every PR; a code owner will review and may ask for changes. Once approved and CI passes, a maintainer will squash-merge your PR.

### Required CI Checks

Every PR must pass these checks before it can be merged:

| Check | What It Validates |
|-------|-------------------|
| Android Gate | Unit tests, lint, debug APK build (`:app`, `:pump-driver-api`, `:tandem-pump-driver`, `:medtronic-pump-driver`, `:wear-device`, `:watchface`) |
| Security Scan Gate | Semgrep SAST on changed Kotlin code |
| Dependency Scan Gate | OSV-Scanner over this repository's Gradle lockfiles |
| Attribution Check | No prohibited AI-attribution lines (see [AI-Assisted Development & Attribution Policy](#ai-assisted-development--attribution-policy)) |
| Workflow Lint | zizmor static analysis of changed GitHub Actions workflows (advisory while it beds in) |

There is no DAST or GitGuardian secret-scanning check defined in this repository's own CI -- see [Security](#device-data-drivers) above for why (no backend/web surface to attack) and `SECURITY.md` for the reporting channel if you find something these checks miss. GitGuardian-equivalent secret scanning is expected to come from GitHub's platform-level Secret Scanning + Push Protection once this repository exists under the GlycemicGPT organization, the same way the main platform repo layers it on top of its own CI secret checks.

### How CI Handles Fork PRs

If you open this PR from your own fork (the normal contributor flow), every required CI check above runs automatically -- you don't need to do anything special, and a maintainer doesn't need to grant you any permissions first.

A few details on how that works, in case you're auditing:

- **The Attribution Check** (and any other workflow that needs write access to post PR comments) runs under `pull_request_target`, which executes in the base-repo context with a writable `GITHUB_TOKEN` even for fork PRs. It never checks out your PR's code into the working tree -- instead it fetches your commits into a remote-only ref and inspects commit messages and diff text only (grep, sed), so it can't accidentally install dependencies from your branch or execute anything you pushed.
- **The Security Scan Gate** runs Semgrep static analysis directly against the diff; it doesn't build or run your code, so it behaves identically for fork and branch PRs.
- **CodeRabbit** has its own review queue. If you push faster than it can keep up you may see stale state on the PR until it catches up -- that's not a CI failure. Comment `@coderabbitai review` to re-trigger if needed.

If a check fails for what looks like an environmental reason rather than a problem in your code, open an issue or ping a maintainer in the PR.

---

## Code Style

### Kotlin (Phone + Wear OS)

- Standard Kotlin conventions
- Jetpack Compose for UI
- Hilt for dependency injection
- Room for local database
- Coroutines + Flow for async operations

---

## AI-Assisted Development & Attribution Policy

**Using AI tools to help write code is completely fine; leaving AI attribution lines in the repo is not.** You own the code you submit -- understand it, make it match our patterns, and test it (AI-generated code is especially prone to subtle bugs in BLE parsing). The full policy -- what the **Attribution Check** CI scans (commit trailers, code comments, and PR descriptions across all major AI tools) and that it fails your PR on a hit -- is org-wide and documented in the [master contributing guide](https://github.com/lumose-health/.github/blob/main/CONTRIBUTING.md#-ai-assisted-development--attribution-policy).

Two mechanics specific to this repo:

- **Local commit-msg hook** -- installed in [Quick Start](#quick-start); it strips prohibited attribution trailers (and warns on a missing DCO sign-off) before they reach the repo. CI enforces the same rules as a backstop.
- **CodeRabbit** runs automated AI review on every PR, posting a summary and inline findings.

---

## Project Structure

```
glycemicgpt-android-unofficial/
├── app/                        # Phone application module
├── wear-device/                # Wear OS companion module
├── watchface/                  # Watch face + complications module
├── plugins/
│   ├── pump-driver-api/        # Plugin SDK (interfaces & domain models)
│   └── shipped/
│       ├── tandem/             # Tandem plugin (t:slim X2 + Mobi)
│       └── medtronic/          # Medtronic MiniMed plugin (680G/770G/780G, BLE, read-only, beta)
├── .github/
│   ├── workflows/              # CI/CD pipelines
│   └── CODEOWNERS              # Code ownership for PR reviews
├── CLAUDE.md                   # AI-assisted development workflow for this repo
└── GOVERNANCE.md                # Roles, decision-making, branch protection
```

---

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE) -- **GPL-3.0 is the inbound license-of-record for this repository: contributions are accepted under GPL-3.0 and are distributed under GPL-3.0 (inbound = outbound)**. By contributing (and signing off per the [DCO](#developer-certificate-of-origin-dco) above), you agree that your contributions will be licensed under the same terms.

---

## Questions?

- **General questions & help** -- use the main platform repo's [Discussions](https://github.com/GlycemicGPT/GlycemicGPT/discussions) or [Discord](https://discord.gg/QbyhCQKDBs)
- **Bug reports** -- open an [Issue](../../issues/new/choose) in this repository

We try to respond to PRs and issues within a few days. If your PR sits without feedback for more than a week, feel free to leave a comment pinging the maintainers.
