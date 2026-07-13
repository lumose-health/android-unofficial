---
title: "ADR 0003: Contract-drift detection strategy (smoke test over full DTO↔spec diff)"
description: Why the Android contract guard is a Retrofit/Moshi smoke test plus a safety-constant source scan rather than a full DTO-to-OpenAPI structural diff, and the safety-constant consolidation follow-up.
---

# ADR 0003: Contract-drift detection strategy

**Status:** Accepted · **Context:** GLY-92 / Epic 56.9

## Context

The app pins the backend contract as a vendored `contract/openapi.json` (see
[ADR 0001](0001-api-versioning-and-compat-signal.md)). We need CI to fail on
*incompatible* drift while tolerating *additive* drift, without over-investing.

## Decision 1 — a smoke test, not a full DTO↔spec structural diff

A full, automated DTO-to-OpenAPI structural diff (walk every DTO field, resolve
its `$ref` in the spec, compare types/nullability/enum members) is
**disproportionate** for this app:

- It requires a Kotlin-reflection + JSON-Schema resolver that must model Moshi's
  `@Json(name=...)` renames, defaulted/nullable semantics, `Instant` adapters, and
  polymorphism — a substantial amount of test infrastructure to maintain.
- The genuine risk is narrow: a field the app *reads* disappearing or changing
  type, or an endpoint the app *calls* being dropped.

Instead, `ContractSmokeTest`:

- round-trips representative, spec-conforming JSON payloads through the **real**
  Retrofit interface and Moshi DTOs (golden path);
- asserts unknown/extra fields are ignored (**additive tolerated**);
- asserts a renamed/removed consumed field or a type change **fails** to parse
  (**incompatible fails**);
- asserts every endpoint the app's Retrofit interface calls still exists in the
  pinned spec's `paths` (a dropped route is caught when the pin is refreshed).

This gives the compatibility signal we need at a fraction of the cost.

**Known blind spot:** the smoke test detects the *removal of a required
(non-null, no-default) consumed field* because Moshi then throws. If a consumed
field is nullable or has a Kotlin default, Moshi tolerates its absence, so the
backend removing it would be classified as additive rather than incompatible.
Today the safety-relevant consumed fields (glucose bounds, auth tokens) are
non-null and undefaulted, so their removal *is* caught; keep it that way — do not
add a default to a field whose absence must be treated as breaking.

### Updating the pin

When the backend contract changes, refresh `contract/openapi.json` from the
platform repo's `apps/api/contract/openapi.json` (byte-for-byte), update
`contract/CONTRACT_VERSION` to match, and reconcile any smoke-test fixtures the
change affects. If the endpoint-presence or golden round-trip test then fails, the
change is incompatible and needs a coordinated app change.

## Decision 2 — safety constants: guard now, consolidate later

The three cross-repo safety constants (see
[safety-constants.md](../contract/safety-constants.md)) are guarded by
`SafetyConstantDriftGuardTest` via direct constant reads, behavioral boundary
checks, and a comment-stripped source scan of every shipped duplication site.

The `20..500` **mg/dL** glucose-validity bound is genuinely scattered with **no
single owner** (the `18.0156` constant is unrelated — it is only the mmol/L
display-conversion factor, not a bound). The
guard therefore asserts *every* current duplication site rather than one constant.
Consolidating the bound into a single shared `object` (all call sites referencing
it) is the right end state and is recorded here as a **follow-up**: it is not
bundled with this guard because it edits safety validators (`require`/`init`
blocks in `BgmReading`, `PluginEvent`, `EnrichedBolusEvent`, `CgmReading`,
`SafetyLimits`) and would enlarge the blast radius of a guard-only change.

## Consequences

- Contract drift and safety-constant desync are CI-enforced from the client side.
- The source scan carries a maintenance cost: adding a new duplication site
  requires updating the guard's enumeration and `safety-constants.md` in the same
  PR. Consolidation would retire that cost.
