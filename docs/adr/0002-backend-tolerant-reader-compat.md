---
title: "ADR 0002: Backend tolerant reader as the cross-cadence compatibility mechanism"
description: Records the backend's existing tolerant-reader behavior as the mechanism that keeps older mobile builds working across independent backend/app release cadences.
---

# ADR 0002: Backend tolerant reader as the cross-cadence compatibility mechanism

**Status:** Accepted · **Context:** GLY-92 / Epic 56.9

## Context

After the repo split, an older shipped app talks to a newer backend (and vice
versa). Something has to keep the common path working when the two drift by a
release or two. That something already exists on the backend and is documented
here so it is a deliberate, maintained property rather than an accident.

## Decision

Treat the backend's **tolerant-reader** behavior as *the* contract-stability
mechanism across cadences, and preserve it:

- **Legacy-value migration before validation** — the backend upgrades legacy
  enum/shape values in-place before validating, e.g.
  `apps/api/src/schemas/pump.py` `migrate_control_iq_mode` (a pre-validation
  validator that maps older `control_iq_mode` spellings onto the current enum).
  An older app that sends a legacy value is accepted, not 422'd.
- **Loose-typed pass-through fields** — `raw_events` and `pump_info` on the pump
  push schema are intentionally loosely typed (`Any`), with an explicit
  "shape has drifted slightly" note in `schemas/pump.py`. New optional fields the
  app doesn't know about, or fields the app omits, do not break the exchange.

## The client half

The app mirrors this tolerance on the read side, and it is pinned by the
[contract smoke test](0003-contract-drift-strategy.md):

- **Additive drift is tolerated** — the Moshi DTOs ignore unknown JSON keys, so a
  newer backend that adds fields deserializes fine (`ContractSmokeTest` asserts
  this explicitly).
- **Incompatible drift fails** — a field the app *reads* being renamed/removed, or
  a type change, fails to parse. That is the boundary the smoke test draws between
  "safe to ship across cadences" and "requires a coordinated change".

## Supported directions (be explicit)

This mechanism is **directional**, and only two directions are actually
established:

- **Older app → newer backend** (a legacy client sending a legacy value): covered
  by the backend legacy-value migration and loose-typed pass-through fields above.
- **Newer backend → older app** (reading a newer response): covered by Moshi
  ignoring unknown fields on the client.

It does **not** establish **newer app → older backend** compatibility — an app
calling a backend that predates a field/endpoint the app now expects. That case is
handled defensively, not proven-compatible: the scoped fail-open-with-warning and
per-call error handling in
[ADR 0001](0001-api-versioning-and-compat-signal.md), plus the app's on-device
defaults where a setting can't be fetched. Do not treat the tolerant reader as a
blanket cross-cadence guarantee; where a specific operation must be
compatibility-gated, gate it per that ADR rather than assuming tolerance.

## Consequences

- The "additive-OK / incompatible-fail" rule the Android guard enforces is the
  direct client-side counterpart of the backend tolerant reader for the two
  supported directions above; the two are designed together.
- Any future backend change that would remove tolerant-reader behavior (tightening
  `raw_events`/`pump_info`, or dropping a legacy-value migration) is a
  cross-cadence breaking change and must be treated as one — bump the contract
  version and coordinate with the shipped app fleet.
