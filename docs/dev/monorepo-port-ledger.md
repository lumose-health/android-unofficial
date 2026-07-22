# Monorepo port ledger

While the Android/Wear OS split from the [GlycemicGPT monorepo](https://github.com/lumose-health/GlycemicGPT)
is in progress, the monorepo remains the source of truth users are served from, and external
mobile-tree PRs land there first (see the monorepo's CONTRIBUTING, "Mobile Code During the
Repository Split"). Maintainers then port each merged mobile-tree change to this repository via
`git cherry-pick -x`, preserving the contributor's commit authorship.

This ledger records every monorepo commit touching the extracted paths
(`apps/mobile/`, `plugins/`, `tools/medtronic-ble-spike/`, `scripts/mobile-dev.sh`) since the
extraction point, and its port status here. It is the parity evidence for the final monorepo-removal
step: before the mobile tree is deleted from the monorepo, every row below must reach a terminal
status -- either `ported` (the normal cherry-pick path), or `parallel-landed` (this repo already
had an independent, equivalent fix; the monorepo commit is intentionally not cherry-picked, so
record the monorepo commit once known and both PR links instead).

- Extraction point: monorepo `develop` commit
  [`2ffc3607`](https://github.com/lumose-health/GlycemicGPT/commit/2ffc3607d1fd7676070181d2886d476d7868f268)
  (2026-07-10) — see [history-extraction.md](../history-extraction.md).
- Backfill audit (2026-07-20): `git log 2ffc3607..develop -- <extracted paths>` on the monorepo
  found **no** mobile-tree commits between the extraction point and the two entries below, so the
  ledger starts complete.
- Port mechanics: `git cherry-pick -x -s <monorepo squash SHA>` on a branch off `develop`, PR into
  `develop`. The PR must be merged with a **rebase merge** (not squash) so the cherry-picked
  commits land verbatim and the contributor's Author field survives onto the default branch.
- Status legend: `pending` (not yet cherry-picked) -> `ported` (cherry-pick landed on this repo's
  `develop`); or, for a row whose monorepo commit is intentionally not cherry-picked,
  `parallel-landed` (this repo already carried an independent, equivalent fix -- terminal on
  creation, record the monorepo commit once known and both PR links in the Port status column).

| # | Monorepo commit | Monorepo PR | Description | Author | Port status |
|---|---|---|---|---|---|
| 1 | [`df255978`](https://github.com/lumose-health/GlycemicGPT/commit/df2559788dda952c8376ef12f823955a33803561) | [#906](https://github.com/lumose-health/GlycemicGPT/pull/906) | Fix history timestamps: anchor pump reference time in the device's local zone | @mortenfyhn | ported (this PR) |
| 2 | [`b056cb45`](https://github.com/lumose-health/GlycemicGPT/commit/b056cb4520d9985d908a5606ca8e9e99b6d3b9a7) | [#907](https://github.com/lumose-health/GlycemicGPT/pull/907) | Confirm IOB parsing on a live 780G; drop provisional markers | @mortenfyhn | ported (this PR) |
| 3 | [`aac94ee2`](https://github.com/lumose-health/GlycemicGPT/commit/aac94ee26543f5f65a1cd1665d5879618de868a6) | [GlycemicGPT#921](https://github.com/lumose-health/GlycemicGPT/pull/921) | GLY-170: anchor the phone updater's APK selector to an exact filename shape + version pin, so it can't install the Wear/WatchFace APK over the phone app | @jlengelbrecht96 | parallel-landed -- [android-unofficial#23](https://github.com/lumose-health/android-unofficial/pull/23) |

New rows are appended when a mobile-tree PR merges in the monorepo; the row's port status moves
from `pending` to `ported` when the cherry-pick reaches this repository's `develop`. A row whose
monorepo commit is intentionally not cherry-picked (this repo already had an independent fix) is
recorded as `parallel-landed` instead, and is terminal from the start.

**Note on row 3:** this repository's `AppUpdateChecker.kt` already carried an independent, working
fix for the underlying defect class (a prior PR's prefix-exclusion selector), so there was no live
bug here to port. GlycemicGPT#921's squash commit touches `apps/mobile/`, same as any other row in
this ledger, but it is intentionally NOT cherry-picked here, because android-unofficial#23 already
landed an independently authored, equivalent fix (the same anchored-regex approach), not a port of
#921's commit. Recorded here anyway so the parity check for the eventual monorepo-removal step has
a paper trail for this file, even though it doesn't fit the table's normal cherry-pick columns.
