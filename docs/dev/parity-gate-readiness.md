# GLY-93 Parity-Gate — Readiness Checklist

> One-page go/no-go front door to the parity-gate runbook. Each line is keyed to a
> runbook step ID (`docs/dev/parity-gate-runbook.md`) — this sheet decides *whether*
> to proceed; the runbook says *how*.
>
> **One-way door.** Nothing in the monorepo is touched destructively (GLY-94) until
> this gate PASSES. Accountable human: the maintainer (gate sign-off).

## Section A — Pre-event prep (all ✅ before the event is scheduled)

Three owner items (A1–A3) are what actually gate the schedule; A4 is non-gating hygiene and the rest is P1 hardening.

| # | Item | Owner | Runbook | Status |
|---|---|---|---|---|
| A1 | **Physical phone staged** — v0.13.0 *release* build installed, pump paired, **≥1 day of real pump-sourced data** accrued, synthetic seed rows recorded. Real pump-sourced data is **local-only readiness input and must never be posted to GLY-93** — evidence posted to the tracker uses synthetic known values on the dedicated test account, with identifiers redacted from screenshots and logs (Evidence policy). | Maintainer | P1.5 | long pole — needs wall-clock day |
| A2 | **Approver roster** — sequential release approvers enumerated and available for the window | Maintainer | P1.7 | open |
| A3 | **Version plan in writing** — `Release-As:` bump so android's first stable **strictly exceeds** the bridging release; monorepo develop→main **freeze** window agreed | Maintainer | P1.8 | ruling made; numbers not written |
| A4 | Park the `GlycemicGPT` org name — **non-gating hygiene, not a parity-gate precondition.** The rename to `lumose-health` already released the old name (`orgs/GlycemicGPT` → 404, old URLs still 301) and the compiled dependency was removed in #31; residual risk is impersonation only. Sequenced under **GLY-194**: repoint the remaining doc/README slugs first, *then* park the name — registering `GlycemicGPT` makes it a live account and breaks the auto-redirects. A pre-public-stable task, tracked outside this gate. | Maintainer | — | non-gating; tracked under GLY-194 |
| A5 | Dev-channel RUN offset in `dev-pre-release.yml` (`DEV_RUN_NUMBER_OFFSET=500`) | dev | P1.1 | ✅ landed |
| A6 | Release `apksigner` cert assertion in `release.yml` | dev | P1.1 | ✅ landed |
| A7 | In-app updater repointed to canonical slug | dev | P1.1 | ✅ landed |
| A8 | **API-36 Wear AVD** created + ambient string-fit rehearsed on a **clean checkout** (prior PASS ran against an uncommitted `shell.nix` edit) | dev | P1.2 | PASS-qualified; needs clean-checkout re-run |
| A9 | Phone↔wear pairing rehearsed (port-forward 5601 path) | dev | P1.3 | verify current state |
| A10 | Monorepo `develop` red gates triaged | dev | P1.6 | open (blocks bridging release, not this gate) |
| A11 | Renovate confirmed **not live** during the window; recovery path pre-staged | Maintainer/dev | P1.9–P1.10 | open |

## Section B — Pre-publish HARD gates (on the still-draft release, before publishing, any device work, or announcement)

A failure here is an abort, not a workaround. Runbook P3.3 / P3.5.

- [ ] **B1 — Signer.** `apksigner verify --print-certs` on the downloaded **phone AND wear** APKs both equal SHA-256 `55f0d0cd…342990`. Mismatch → `gh release delete` + STOP. *(Both repos silently fall back to debug signing if `RELEASE_KEYSTORE_FILE` is unset.)*
- [ ] **B2 — 4-APK inventory.** All four release artifacts present. A wear-build failure is **gate-blocking**, not `continue-on-error` — a phone-only publish silently unfulfils the wear leg.
- [ ] **B3 — versionCode > 130000.** Strictly greater than the real installed v0.13.0 baseline.
- [ ] **Abort ladder:** delete the release, **keep the tag, never re-tag a burned version** (a re-released equal version is invisible to updaters) → next attempt is a patch bump. Declining the APK-build approval is the last free abort.

## Section C — AC evidence (sign-off artifact; runbook Phase 4 → P5.1)

Sign-off (runbook P5.1) = a single GLY-93 comment, posted before GLY-94 unblocks, that:

- confirms the freeze held — `git rev-parse origin/develop` re-run in **both** repositories; this
  repo's head still equals `$PIN`, and if the monorepo head moved past `$MONO_PIN`, every monorepo
  evidence item cites `$MONO_PIN` — the pins are what the evidence anchors to, and the sign-off
  records both final heads either way;
- enumerates every gate leg, **AC1–AC7**, with a link to each item's captured evidence comment;
- carries the two standing notes: the parity-plan review requirement is satisfied by this runbook's
  own reviewed PR, and the Sentry validation gate is **N/A** by ruling (backend/web/sidecar-scoped;
  mobile release builds compile the Sentry DSN to `""`).

AC1–AC7 evidence links alone are not sufficient — without the freeze/pin confirmation, GLY-94 could
unblock without proof the freeze-and-pin loop happened.

- [ ] **AC1** — Signed new-repo APK installs **in place** over the monorepo build (no uninstall, no `-r` cert override); Room data + login + **pump BLE pairing** survive. Verified in **airplane mode against pre-seeded known rows** (a wiped DB self-masks). **Evidence posted to GLY-93 uses synthetic known values on the dedicated test account only** — never the real pump-sourced data staged in A1 — with identifiers redacted from screenshots and logs, per the runbook's Evidence policy. ⛔ **An install failure is a FAIL — never uninstall to proceed.** (Step 4.1)
- [ ] **AC2** — Phone↔watch Wearable Data Layer round-trips in the version-skew config: chat quick-query relay + alert-dismiss. (Step 4.2)
- [ ] **AC3** — In-app updater discovers the new-repo release **from an installed new-repo build**, both channels. *(Monorepo-cohort discovery is GLY-94 AC2, NOT this gate.)* (Step 4.6)
- [ ] **AC4** — Committed in-app WFF assets **byte-identical to the v0.13.0 tag** + `apksigner verifies`; ambient string-fit on the **physical watch (non-gating)**. ⛔ **Do NOT regenerate WFF assets during the event** (regen changes the signer, destroys the evidence). (Step 4.4)
- [ ] **AC5** — Pump-driver **structural equivalence** (accepted): re-pin tree hashes `e7f2eaa8` (api) / `f53a8169` (shipped) at the promotion SHA + matching toolchain identity (Gradle/AGP/Kotlin + all seven lockfile hash pairs) + dual-green `testDebugUnitTest` at the pin. Any divergence = STOP. (Step 4.5)
- [ ] **AC6** — **Human go/no-go** posted to GLY-93 before GLY-94's block clears.
- [ ] **AC7** — Parity builds green; adversarial + senior review of the runbook; CodeRabbit on scripts; security review (no signing material leaked). **Sentry N/A** (mobile release DSN compiles to `""`).

---

*The gate can be scheduled the moment Section A is fully green. Sections B and C are
event-day execution, fully specified in the runbook; this sheet is the go/no-go index
into them.*
