---
title: "ADR 0001: API versioning posture and app-compatibility signal"
description: Decision to leave the backend endpoint-versioning scheme as-is, and the design (deferred) for an App-requires-API≥X compatibility signal with a fail-open fallback for self-hosted backends.
---

# ADR 0001: API versioning posture and app-compatibility signal

**Status:** Accepted · **Context:** GLY-92 / Epic 56.9 (cross-repo contract discipline, pre-split)

## Context

The Android/Wear apps now ship independently of the backend platform repo. A user
may point the app at a backend of any age via a user-set base URL
(`app/src/main/java/com/glycemicgpt/mobile/data/remote/BaseUrlInterceptor.kt`),
including a self-hosted instance that predates a given app build. Two questions:

1. Should the backend's `/api/*` endpoint versioning be normalized?
2. How should the app detect and respond to an incompatible backend?

## Decision 1 — endpoint versioning: **LEAVE AS-IS, document**

Today only `/api/v1/devices` (`routers/device_registration.py`) and `/api/v1/alerts`
(`routers/alert_api.py`, `alert_stream.py`) are version-prefixed; the rest of the
surface is unversioned (`/api/auth/...`, `/api/settings/...`, `/api/ai/...`,
`/api/food-records`, `/api/integrations/...`).

We will **not** normalize to `/api/v1` now. Rationale:

- Normalization is a **runtime change** to the backend, out of scope for this
  guard-only work, and it would break every unversioned path the already-shipped
  app calls unless done with parallel routing — a large, risky change.
- The [contract pin + drift gate](0003-contract-drift-strategy.md) already makes
  the *whole* surface (versioned or not) explicit and CI-enforced, which is the
  property we actually needed. Path-level versioning adds little on top.

If a future breaking change to an unversioned path is unavoidable, introduce a
`/v2` sibling and keep the old path until the shipped app fleet has aged out — do
not mutate an unversioned path in place.

## Decision 2 — app-compatibility signal: **design now, implement later**

Design (deferred to a follow-up, not implemented here):

- The backend exposes its contract version — the `x-contract-version` already
  stamped into the pinned `openapi.json` — via a stable runtime channel, e.g. an
  `X-Contract-Version` response header on `/health` or a field in the `/health`
  body. (Neither is added now: this work is guard-only and adds no runtime path.)
- The app pins the minimum contract version it requires (`CONTRACT_VERSION` it was
  built and tested against, vendored at `contract/CONTRACT_VERSION`).
- On first contact with a backend, the app compares.

**Version ordering.** `CONTRACT_VERSION` is a **monotonically increasing integer**
(starts at `1`), not a semantic or opaque string; comparison is numeric. A value
that fails to parse as an integer, or a signal the app cannot read, is treated as
**absent** (the "older / unknown backend" branch below) — never silently as
"compatible."

### Fallback for a self-hosted / older / unknown backend: scoped fail-open-with-warning

A user-pointed self-hosted backend may predate the signal entirely (no header/field
at all). The app **must not hard-refuse** in that case — that would brick a
legitimate self-hoster on upgrade. But fail-open is **scoped**, not blanket:

- **Signal present and app-required ≤ backend version:** proceed normally.
- **Signal present and backend older than the app requires, OR signal
  absent/unparseable:** proceed on **read/monitoring** paths with a one-time,
  dismissible warning that the backend is older/unknown and some features may not
  work. The backend [tolerant reader](0002-backend-tolerant-reader-compat.md) and
  the app's additive-tolerant DTOs keep the common read path working; individual
  calls that fail are handled per-call, not by blocking the app.

**Scope of fail-open.** This app is monitoring-only — it issues **no therapeutic /
device-control commands** (no bolus, no pump writes; the AI never issues device
commands — a project-wide invariant). Fail-open therefore applies to reading and
displaying data. Were a therapeutic or otherwise safety-critical *write* surface
ever added, it must **not** inherit blanket fail-open: such an operation must be
refused when backend compatibility is unknown, independent of this monitoring
fail-open default. Hard-refuse of the whole app is reserved for a future,
explicitly-declared hard-incompatibility epoch, if one ever exists.

## Consequences

- No runtime change ships in this PR; the compatibility signal is a recorded,
  designed follow-up.
- The drift gate is a **build-time** guard against the *vendored pin*, not runtime
  protection: it catches incompatibility only once `contract/openapi.json` is
  refreshed from the backend, and it says nothing about an arbitrary
  user-configured backend at runtime. The runtime compatibility signal above is
  what would cover that case; until it ships, the app is simply built and tested
  against a known contract version, with incompatible drift on covered surfaces
  caught by the [contract smoke test](0003-contract-drift-strategy.md).
