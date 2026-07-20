# Monorepo port ledger

Mobile development happens in this repository — mobile PRs should be opened against this repo's
`develop` (see the [GlycemicGPT monorepo](https://github.com/lumose-health/GlycemicGPT)'s
CONTRIBUTING, "Mobile Code During the Repository Split"). The monorepo's mobile tree remains
temporarily while the split completes; if an in-flight mobile-tree change still lands there during
the wind-down, maintainers port it to this repository via `git cherry-pick -x`, preserving the
contributor's commit authorship.

This ledger records every monorepo commit touching the extracted paths
(`apps/mobile/`, `plugins/`, `tools/medtronic-ble-spike/`, `scripts/mobile-dev.sh`) since the
extraction point, and its port status here. It is the parity evidence for the final
monorepo-removal step: before the mobile tree is deleted from the monorepo, every row below must
be `ported`.

- Extraction point: monorepo `develop` commit
  [`2ffc3607`](https://github.com/lumose-health/GlycemicGPT/commit/2ffc3607d1fd7676070181d2886d476d7868f268)
  (2026-07-10) — see [history-extraction.md](../history-extraction.md).
- Backfill audit (2026-07-20): `git log 2ffc3607..develop -- <extracted paths>` on the monorepo
  found **no** mobile-tree commits between the extraction point and the two entries below, so the
  ledger starts complete.
- Port mechanics: `git cherry-pick -x -s <monorepo squash SHA>` on a branch off `develop`, PR into
  `develop`. The PR must be merged with a **rebase merge** (not squash) so the cherry-picked
  commits land verbatim and the contributor's Author field survives onto the default branch.

| # | Monorepo commit | Monorepo PR | Description | Author | Port status |
|---|---|---|---|---|---|
| 1 | [`df255978`](https://github.com/lumose-health/GlycemicGPT/commit/df2559788dda952c8376ef12f823955a33803561) | [#906](https://github.com/lumose-health/GlycemicGPT/pull/906) | Fix history timestamps: anchor pump reference time in the device's local zone | @mortenfyhn | ported (this PR) |
| 2 | [`b056cb45`](https://github.com/lumose-health/GlycemicGPT/commit/b056cb4520d9985d908a5606ca8e9e99b6d3b9a7) | [#907](https://github.com/lumose-health/GlycemicGPT/pull/907) | Confirm IOB parsing on a live 780G; drop provisional markers | @mortenfyhn | ported (this PR) |

New rows are appended when a mobile-tree PR merges in the monorepo; the row's port status moves
from `pending` to `ported` when the cherry-pick reaches this repository's `develop`.
