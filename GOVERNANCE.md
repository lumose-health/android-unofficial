# Project Governance

This document describes the roles, responsibilities, and decision-making process for `glycemicgpt-android-unofficial`. It's designed to be transparent about how this repository is run and how contributors can grow their involvement.

This repository is the Android phone app, Wear OS companion, and device data driver plugins for [GlycemicGPT](https://github.com/GlycemicGPT/GlycemicGPT). It is governed as part of the same project, under the same GitHub organization and the same funding channel -- see [Funding](#funding) below. This document covers the roles and process specific to this repository; the main platform repo's `GOVERNANCE.md` is authoritative for org-wide policy (funding mechanics, sponsor terms) that isn't specific to mobile.

## Medical Context

This repository reads live data from insulin pumps and CGMs over Bluetooth. Code changes here can affect how glucose data is parsed and displayed, and a parsing bug is a safety issue, not just a bug. **Every role carries responsibility for patient safety**, not just code quality.

## Roles

This repository uses the same four-role model as the main platform repo. Each role maps to a GitHub permission level enforced through org teams and CODEOWNERS.

### Permissions

| Permission | Contributor | Committer | Maintainer | Project Lead |
|------------|:-----------:|:---------:|:----------:|:------------:|
| Open issues and PRs | Yes | Yes | Yes | Yes |
| Review code (comments) | Yes | Yes | Yes | Yes |
| Report security vulnerabilities | Yes | Yes | Yes | Yes |
| Push to feature branches | - | Yes | Yes | Yes |
| Approve PRs on develop | - | Yes | Yes | Yes |
| Triage issues (labels, milestones) | - | Yes | Yes | Yes |
| Merge PRs to develop | - | - | Yes | Yes |
| Approve promotion PRs (main) | - | - | Yes | Yes |
| Merge promotion PRs (main) | - | - | - | Yes |
| Publish releases | - | - | Yes | Yes |
| Change governance files | - | - | - | Yes |
| Change security infrastructure | - | - | - | Yes |
| Change branch protection | - | - | - | Yes |

### Contributor

**Who:** Anyone who participates in the project. No special access required.

**How to become one:** Just show up. Open a PR, file an issue, or join a discussion in the main platform repo.

### Committer

**Who:** Trusted contributors with Write access to this repository, typically with demonstrated Kotlin/Compose/BLE expertise or deep familiarity with a specific device data driver.

**What you cannot do:** merge to `develop` or `main`; change governance files (CODEOWNERS, GOVERNANCE.md, CONTRIBUTING.md, LICENSE); modify security infrastructure; publish releases; modify org settings, teams, or branch protection.

### Maintainer

**Who:** Project stewards with Maintain access, responsible for the day-to-day health of this repository.

**What you cannot do:** merge promotion PRs to `main` (project lead only); change governance files (project lead only via CODEOWNERS); change security infrastructure (project lead only via CODEOWNERS); change branch protection rules or org settings.

**Current maintainers:**
- [@jlengelbrecht](https://github.com/jlengelbrecht) (project lead)

### Project Lead

**Who:** The founder and final decision-maker. Org Owner on GitHub with full admin access.

**Current project lead:** [@jlengelbrecht](https://github.com/jlengelbrecht)

This is a standard BDFL (Benevolent Dictator for Life) model, shared with the main platform repo since both live under the same project. The project lead retains authority over governance, security, branch protection, org settings, and maintainer promotions.

## Becoming a Committer

1. Contribute consistently over **3+ months** (no specific PR count -- quality matters more than quantity)
2. Demonstrate understanding of the medical safety requirements in [CONTRIBUTING.md](CONTRIBUTING.md#safety-first----please-read)
3. Follow project conventions without repeated correction
4. Any maintainer nominates you in a Discussion thread (main platform repo)
5. **1-week consensus period** -- the nomination passes if no existing maintainer objects
6. The project lead retains veto power over any nomination
7. On approval: added to the `@GlycemicGPT/committers` org team, which carries Write access on this repository

## Becoming a Maintainer

1. Active committer for **6+ months**
2. Has reviewed PRs and mentored other contributors on this repository
3. Demonstrated sound judgment on safety-critical decisions (BLE parsing correctness, `SafetyLimits` enforcement)
4. Any maintainer nominates in a Discussion thread
5. 1-week consensus period among existing maintainers
6. **Project lead must explicitly approve** (not just absence of objections)
7. On approval: moved to the `@GlycemicGPT/maintainers` org team

## Decision-Making

### Day-to-day decisions

Maintainers make routine decisions: merging PRs, triaging issues, choosing implementation approaches. These don't need formal process.

### Architecture and safety decisions

Major changes that affect this app's architecture or safety properties should be discussed before implementation:

1. Open a Discussion in the main platform repo's Ideas category describing the proposal
2. Tag relevant maintainers and committers
3. Allow at least 7 days for feedback on safety-critical proposals
4. Document the decision in the PR that implements it

Examples of what qualifies: new pump or CGM device support; changes to a device data driver's safety-limit validation; changes to what data the Wearable Data Layer relays between phone and watch; any change touching the applicationId or manifest of `:app` or `:wear-device`.

### Disputes

If contributors disagree on an approach: discuss in the PR or a linked Discussion; if no consensus, maintainers decide; if maintainers disagree, the project lead has final say.

## Branch Protection

- **`main`** (stable releases) -- all changes must go through a pull request; 1 required approving review from a code owner; no force push, no deletion.
- **`develop`** (integration branch) -- all changes must go through a pull request; 1 required approving review from a code owner; squash merge only; required status checks must pass; no force push, no deletion.

### Why project lead approval is required on `main`

The promotion from `develop` to `main` is a release decision: the code has been tested on `develop`, debug APKs have been verified, and no known regressions exist. The project lead takes responsibility for what ships to people managing their diabetes.

## Code Ownership

Code owners are defined in [`.github/CODEOWNERS`](.github/CODEOWNERS). When a PR touches files owned by a specific team or person, GitHub automatically requests their review. Governance files, security infrastructure, and release configuration list the project lead individually alongside the maintainers team, so review is always requested even when the project lead authors the PR.

> **Note:** CODEOWNERS controls who is *requested* for review, not who *must* approve. The requirement that the project lead personally reviews governance, security, and release changes is enforced by process (this document), not by GitHub's code owner mechanism.

## Automation

Once this repository exists under the GlycemicGPT GitHub organization, it will reuse the organization's existing bot identities rather than minting new ones. Their app credentials are org-level secrets, inherited automatically by any repo the corresponding GitHub Apps are installed on:

| Bot | Purpose |
|-----|---------|
| **glycemicgpt-ci** | CI/CD operations |
| **glycemicgpt-release** | Release management (version bump PRs, signed APK uploads) |
| **glycemicgpt-merge** | Automated merging of release-please and changelog PRs |
| **glycemicgpt-renovate** | Dependency update PRs |

This repository's Security Scan Gate (Semgrep Kotlin) runs as a plain CI job with no bot identity of its own -- findings surface in the job log and an uploaded artifact, not as GitHub issues or PR comments. `glycemicgpt-security`, which drives that automated issue/comment lifecycle in the main platform repo, is not used here; adopting it would be a follow-on piece of work, not something this bootstrap sets up.

## Security

- **Suppression decisions** (accepting a known risk) require project lead approval.
- **Security infrastructure changes** require project lead review (enforced via CODEOWNERS).
- **Vulnerability reports** should follow [SECURITY.md](SECURITY.md).

## Funding

This repository does not run its own funding channel. Funding for the whole GlycemicGPT project, including this repository's CI and maintainer costs, flows through [Open Collective](https://opencollective.com/glycemicgpt), fiscally hosted by Open Source Collective, with full public transaction history. See the main platform repo's `GOVERNANCE.md` for the complete funding, stipend, and in-kind sponsorship policy.

## Changes to This Document

This governance document can only be modified by the project lead. Changes require a pull request reviewed by the project lead (enforced via CODEOWNERS). This ensures governance cannot be changed without the founder's explicit approval.
