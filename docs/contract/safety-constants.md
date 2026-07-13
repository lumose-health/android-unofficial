---
title: Cross-repo safety constants
description: The three safety constants duplicated between this Android repo and the backend platform repo, their canonical owners, and every duplication site the drift guard covers.
---

# Cross-repo safety constants

The Android/Wear apps were extracted from the
[GlycemicGPT platform repo](https://github.com/GlycemicGPT/GlycemicGPT) and now
ship on a cadence independent of the backend. Three safety-relevant constants are
duplicated across the two repos. If a copy silently desyncs, glucose is
mis-converted or mis-validated — a patient-safety failure. This page is the
single source of truth for those constants and their duplication sites; the
`SafetyConstantDriftGuardTest` unit test enforces it.

When any of these values legitimately changes, it must change in **both repos in
the same coordinated release**, and this page plus the guard's enumeration must be
updated in the same PR.

## The three constants

| Constant | Canonical value | Backend owner (platform repo) |
|---|---|---|
| mmol ↔ mg/dL factor | `18.0156` | `apps/api/src/core/units.py` `MGDL_PER_MMOL` |
| glucose-validity bound | `20..500` mg/dL | The 20–500 mg/dL platform invariant (PR #729; enforced by the CodeRabbit **Medical-Safety** gate in `.coderabbit.yaml`). Backend rejects outside this range at every ingestion mapper and on `GlucoseReading` (`ge=20`). |
| Tandem epoch offset | `1199145600` s (2008-01-01 UTC) | `apps/api/src/core/tandem_regions.py` `TANDEM_EPOCH_OFFSET_SECONDS = 1_199_145_600` |

Canonical glucose storage is mg/dL everywhere; mmol/L is display-only (`÷ 18.0156`,
round last). Alert/SSE numerics stay mg/dL.

## Android duplication sites (shipped)

These are the sites the drift guard covers. Paths are repo-relative.

### `18.0156` — mmol↔mg/dL factor (public `const val MGDL_PER_MMOL`)

- `app/src/main/java/com/glycemicgpt/mobile/domain/format/GlucoseFormat.kt` — the client-side conversion choke-point.
- `wear-device/src/main/java/com/glycemicgpt/weardevice/util/GlucoseDisplayUtils.kt` — wear mirror (wear cannot depend on `:app`).

### `20..500` — glucose-validity bound (no single owner; scattered)

Named constants:

- `plugins/pump-driver-api/.../domain/pump/SafetyLimits.kt` — `DEFAULT_MIN_GLUCOSE=20` / `DEFAULT_MAX_GLUCOSE=500` and the `ABSOLUTE_*` aliases. The closest thing to a canonical Android owner; `SafetyLimitsStore` defaults derive from it.
- `plugins/pump-driver-api/.../domain/model/PumpModels.kt` — `CgmReading.MIN_MG_DL=20` / `MAX_MG_DL=500`.
- `app/.../domain/compute/DashboardComputations.kt` — private `VALID_GLUCOSE_MIN=20` / `VALID_GLUCOSE_MAX=500`.
- `app/.../presentation/home/HomeViewModel.kt` — private `MIN_THRESHOLD=20` / `MAX_THRESHOLD=500`.

Inline `20..500` literals:

- `plugins/pump-driver-api/.../domain/model/BgmReading.kt` (`require`), `.../domain/plugin/events/PluginEvent.kt` (`require`).
- `app/.../domain/model/EnrichedBolusEvent.kt`, `app/.../data/local/AlertThresholdStore.kt`, `app/.../data/repository/AuthRepository.kt` (×2, plus off-by-one `20..499`/`21..500`), `app/.../presentation/settings/SettingsViewModel.kt` (×2), `app/.../data/local/dao/PumpDao.kt` (SQL `BETWEEN 20 AND 500`).
- `wear-device/.../presentation/AlertsActivity.kt`, `wear-device/.../util/GlucoseDisplayUtils.kt` (`isValidGlucose` + urgent-low/high `coerceIn` clamps).

### `1199145600` — Tandem epoch offset (private `TANDEM_EPOCH_OFFSET`)

- `plugins/shipped/tandem/.../ble/messages/StatusResponseParser.kt`.

## Not covered (documented exclusions)

- **Example plugins** (`plugins/example/**` — `DemoGlucometerPlugin.kt`, `ReadingSimulator.kt`) encode `20..500` but are sample code, not shipped, so they are intentionally outside the guard.
- **Comment-only mentions** (e.g. `plugins/shipped/medtronic/.../CgmReader.kt`, `NightscoutDataMapper.kt`) are not code sites; the guard strips comments before counting.

## Known limitation / follow-up

The `20..500` bound is genuinely scattered with no single owner. Consolidating it
into one shared `object` (e.g. `SafetyLimits` as the sole source, with every call
site referencing it) would let the guard read one constant instead of scanning
sites, and is recorded as a follow-up in
`docs/adr/0003-contract-drift-strategy.md`. That refactor touches safety
validators, so it is deliberately **not** bundled with this guard.
