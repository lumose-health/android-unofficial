---
title: Release & Promotion
description: How a develop-to-main promotion turns into a versioned release, a changelog entry, and a sync-back to develop, plus the CI hygiene gates that guard the automation.
---

# Release & Promotion

This repository follows a `develop` -> `main` flow. Feature and fix PRs target
`develop`; feature and fix code reaches `main` only through a **promotion PR**
(the release-please version bump and the changelog update also land on `main`, as
direct results of a promotion). Promotion is what turns accumulated `develop` work
into a signed, versioned release.

The promotion pipeline is three cooperating workflows. Each is triggered by a push
to `main` and does exactly one job, so the pieces stay independently reviewable.

> **Status: built, validated in parts, not yet exercised end-to-end.** The static
> logic below is unit-proven (the changelog promotion gate and the sync-back
> conflict resolver both have local dry-run proofs), but a full live cycle can only
> run at the first real promotion, when `main` and the release/merge GitHub Apps are
> in play. Treat the first promotion as a maintainer-validated run.

## The promotion flow

```
 develop --(promotion PR: "chore: promote develop to main", merge commit)--> main
                                   |
        +--------------------------+--------------------------+
        |                          |                          |
   release.yml               changelog-pr.yml         (nothing else fires on
   version + signed APK       CHANGELOG PR             the feature commit -- no loop)
        |                          |
   "chore: release X"        "[Changelog] Update CHANGELOG.md"
   pushed to main            merged to main
        |                          |
        +------------+-------------+
                     |
          sync-main-to-develop.yml
          cherry-picks the version bump + changelog back to develop
```

1. **Create the promotion PR** (maintainer): `gh pr create --base main --head develop
   --title "chore: promote develop to main"`. Merge it with **Create a merge commit**
   to preserve the develop->main ancestry.

2. **`release.yml`** (on push to `main`): [release-please](https://github.com/googleapis/release-please)
   proposes a version from the conventional commits, or -- when a promotion carries only
   `chore`/`ci`/`docs` commits but changes deployable code -- a **fallback patch release**
   bumps the three module `build.gradle.kts` files and the manifest and cuts a signed-APK
   release. Doc-only promotions produce no release.

3. **`changelog-pr.yml`** (on push to `main`): only when the head commit contains
   `promote develop to main` (and is not itself a `[Changelog]` or `chore: release`
   commit). It collects every PR merged to `develop` since the last
   `<!-- changelog-cutoff:... -->` marker, groups them into mobile-scoped sections
   (see below), opens a `[Changelog] Update CHANGELOG.md` PR as `glycemicgpt-release[bot]`,
   and the second job validates and admin-merges it.

4. **`sync-main-to-develop.yml`** (on push to `main`): fires on the `chore: release X`
   and `[Changelog] Update CHANGELOG.md` commits (never on the promotion feature commit,
   so there is no loop). It cherry-picks those main-only commits back onto `develop`
   via an auto-merged sync PR, keeping the branches from diverging before the next
   promotion.

### Changelog scope taxonomy

`changelog-pr.yml` reads `.github/changelog-pr-config.json` and buckets each merged PR
by its labels: **Mobile** (`mobile`), **Wear OS** (`wear`), **Watch Face** (`watchface`),
**Plugins** (`plugin`), **Security** (`security`), **CI & Build** (`ci`/`build`/`chore`),
**Documentation** (`docs`/`documentation`), and **Dependencies**. Those labels are applied
automatically by [`auto-label.yml`](#auto-label) from the changed file paths and the
Conventional-Commit PR title.

### Sync-back conflict resolution

A cherry-pick onto `develop` usually applies cleanly. When it conflicts, the resolver
takes **main's version for exactly four version files** -- the ones a release bumps:

- `.release-please-manifest.json`
- `app/build.gradle.kts`
- `wear-device/build.gradle.kts`
- `watchface/build.gradle.kts`

Any *other* file left in conflict (including `CHANGELOG.md`) is **not** force-resolved:
the workflow aborts the cherry-pick cleanly, pushes nothing, and leaves a warning for a
manual sync. This is deliberate -- automatic resolution is limited to the version files
whose "main always wins" rule is unambiguous.

## CI hygiene gates

Every PR to `develop` runs a set of automation-hygiene checks alongside the build gates:

- **Workflow Lint** -- `actionlint` (+ shellcheck), a SHA-pin guard, and a composite-action
  secrets-context guard over `.github/workflows/**` and `.github/actions/**`. Lints workflow
  *syntax and shell*.

- <a id="workflow-security"></a>**Workflow Security** -- [zizmor](https://github.com/woodruffw/zizmor)
  static analysis of workflow *security posture* (template injection, dangerous
  `pull_request_target` use, excessive permissions, credential persistence, cache
  poisoning). It **fails on any Medium-or-higher finding** that is not allowlisted.
  Known-safe findings are listed, one at a time with a rationale, in `zizmor.yml` at the
  repo root. To accept a new known-safe finding, add a scoped entry there **with a comment
  explaining why** -- never lower the severity threshold. Informational findings are shown
  in the run log but do not fail the build. This gate runs on `pull_request` and checks out
  the PR to read its workflow files (read-only static analysis -- it never executes them);
  a companion guard in the same job fails any `pull_request_target` workflow that checks out
  PR head code.

- <a id="auto-label"></a>**Auto Label PRs** -- applies path- and title-derived labels
  (`.github/autolabeler-config.json`) so the changelog taxonomy and Renovate auto-merge
  policy have labels to work with.

- **Attribution Check** -- scans commit trailers, changed-file comments, and the PR
  description for AI-tool attribution and prohibited bot co-authors (see
  [CONTRIBUTING.md](../../CONTRIBUTING.md#no-ai-attribution-in-code)). The develop->main
  promotion PR is exempt from its CRITICAL auto-close.

`Attribution Check` and `Auto Label PRs` run on `pull_request_target` so they also work on
fork PRs; neither checks out or executes PR-supplied code (they read PR metadata via the API,
or fetch commits into a remote-only ref for text inspection).
