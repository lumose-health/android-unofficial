---
title: Parity-Gate Runbook
description: Event-day procedure for proving signed, in-place-upgradeable phone + wear + watchface builds from this repository before the monorepo's mobile tree is removed.
---

# Parity-Gate Runbook

This is the ordered, event-day procedure for the **parity-validation gate**: the proof that this
repository's first signed stable release upgrades existing monorepo-built installs **in place** --
preserving encrypted Room data, login, and pump BLE pairing -- before anything destructive happens
to the monorepo's mobile tree. It is the last non-destructive gate in the repository split; the
destructive removal (tracked as GLY-94) stays blocked until this gate has a recorded PASS.

The gate is executed by **one release maintainer plus one development session**, in order, in one
sitting for Phases 2--5 (Phase 1 is preparation, days before). Every step states what to **do**,
what constitutes **PASS**, what constitutes **FAIL**, and what to **capture**. A FAIL routes to the
abort ladder in Phase 3 -- never to improvised recovery, because on the user-data steps the
"obvious" recovery moves (uninstall, regenerate, re-tag) are exactly the ones that destroy the
evidence or the data.

## Evidence policy

All evidence is captured as comments on the parity tracking issue (**GLY-93**), posted
**immediately as each step completes** -- not batched at the end. The 2026-07-23 readiness pass's
evidence lived in an ephemeral working directory and is gone; issue comments are the durable record
that the final sign-off links to. Each capture should include the exact command run, its full
output (or a screenshot for on-device steps), and a timestamp. Steps below mark their minimum
capture set with **Capture:**.

**Privacy rule -- no health or account data in issue comments.** GLY-93 is an ordinary tracking
issue, not an approved health-data store. Every row used for verification MUST be a **synthetic
known value on the dedicated test account** (P1.5) -- never a real person's glucose, bolus, or
basal history. Synthetic marker values may be posted verbatim (that is what makes verification
exact); everything else is captured as hashes, row counts, version screens, and screenshots
**redacted of account identifiers** (email, tokens, device IDs). Logs are trimmed to the relevant
lines with identifiers removed -- never attached whole. If a sensitive artifact is ever
unavoidable, it goes into restricted storage (1Password) with only a link and SHA-256 in the
issue comment.

## Global abort rules

These apply at every step and override any instinct to "just fix it and keep going":

1. ⛔ **Never uninstall the phone app to recover from a failed install.** Uninstalling destroys the
   Android-Keystore-wrapped SQLCipher passphrase, the stored tokens, and the pump BLE pairing --
   converting a loud, diagnosable FAIL into a silent fake PASS on the test device, and modeling the
   exact action that would cause unrecoverable data loss on a real user's device. An install
   failure is a gate FAIL; capture it and go to the abort ladder.
2. ⛔ **Never regenerate the committed WFF watch-face assets during the event.** Regeneration
   re-signs them, which destroys the byte-identity parity evidence they exist to provide. An asset
   mismatch is a FAIL finding to investigate, not something to fix in place.
3. ⛔ **Never re-tag or re-release a burned version.** The in-app updater compares versions; a
   re-released equal version is invisible to every installed client. A deleted release keeps its
   tag, and the next attempt is a patch bump.
4. **A wear-build failure at release time is gate-blocking.** `release.yml` marks the
   `:wear-device` and `:watchface` release builds `continue-on-error`, so a phone-only publish is
   mechanically legal -- this runbook treats it as a FAIL regardless (Phase 3, check V1).
5. **No publishing, no announcement, and no device work until the verification block (Phase 3)
   has passed against the draft.** The release does not become discoverable until its signature,
   asset inventory, and version have been verified against the values below.

## Reference values

Verified 2026-07-23 against the shipped monorepo v0.12.0/v0.13.0 release APKs, the
1Password-stored keystore, and both repositories' `develop` heads. Phase 2 **recomputes** the
recomputable ones at the pinned SHAs -- a drift from this table is a finding to investigate before
proceeding, never something to wave through. The release-signer digest is additionally
**re-derived independently on event day with a second person (P2.6)** -- this table is a
cross-check, never the sole authority on the signer.

| Value | Expected |
|-------|----------|
| Release signer SHA-256 | `55f0d0cdabf20b398ad30a0ce3e998e3192666e5c15e6d378b86e1c8de342990` |
| Debug signer SHA-256 (shared, dev channel) | `b04eacc89b84f0a30ace14b6cd1b0cdb919f6688fe13a572ca0c3af6565ea311` |
| `applicationId` (phone AND wear, both repos) | `com.glycemicgpt.mobile` |
| versionCode formula | `major * 1_000_000 + minor * 10_000 + patch` (see `app/build.gradle.kts`) |
| Installed stable baseline | v0.13.0 = versionCode **130000** (monorepo-built) |
| First stable from this repo | versionCode **> 130000** AND strictly greater than the bridging release (see Phase 5) |
| `plugins/pump-driver-api` tree hash | `e7f2eaa87bae92da2997dac18b69154d5399aeb9` |
| `plugins/shipped` tree hash (tandem + medtronic) | `f53a8169b25c54ec5149dc6f76d29ad12961b664` |
| `app/src/main/java/com/glycemicgpt/mobile/data/local` tree hash | `f9594efc421f8803608b9613ffb1039a8460953b` |
| `app/schemas` (exported Room schemas) tree hash | `0f67e0c3326431ec1f0272626322d404f21bc0c9` |
| `wear-device/src/main/AndroidManifest.xml` blob hash | `73ce603b58cf22c64f8ca3cdffdcc5a2392f521d` |
| WFF asset `glycemicgpt-watchface-digitalFull.apk` blob hash | `1439f2a9b0ddf0a507f84d5b4b3846b263640a14` |
| WFF asset `glycemicgpt-watchface-analogMechanical.apk` blob hash | `71c107ea983f7a81fc97b00c864963281b014c6e` |
| Toolchain (identical in both repos) | Gradle 8.12 · AGP 8.7.3 · Kotlin 2.1.0 · JDK 17 |

In the monorepo, the same trees live under the `apps/mobile/` prefix (`plugins/` is at the monorepo
root), e.g. `v0.13.0:apps/mobile/app/schemas` vs this repo's `<PIN>:app/schemas`.

## Phase 1 -- Preconditions (complete days before the event)

Every row must be checked off, with evidence posted, before an event date is set.

### P1.1 Hardening changes landed on `develop`

| Item | Why | Required? |
|------|-----|-----------|
| `release.yml` post-build cert assertion (`apksigner verify --print-certs` == the release signer, phone + wear) | Both repos' release buildTypes **silently fall back to debug signing** when `RELEASE_KEYSTORE_FILE` is unset, and no workflow asserts the produced cert today. `release.yml` has never executed in this repository; the promotion is its first run. | **Required** |
| `dev-pre-release.yml` run-number offset | Dev versions are `<version>-dev.<run_number>`. This repo's run counter (~5) is far below the installed cohort's monorepo lineage (dev.145), so without an offset the dev channel reports "up to date" forever. | **Required** |
| Draft-first release publishing (`release-please-config.json` `"draft": true`, fallback path switched to `gh release create --draft`, plus a documented post-verification publish step) | Today the release is public and reachable by a manual update check the moment it is created -- **before** any APK verification has run. Draft releases are excluded from `releases/latest`, so the verification block (P3.3) runs in containment and publishing becomes an explicit post-verification act (P3.4). | **Required** |
| Recorded decision on the `continue-on-error` wear/watchface release steps | Either remove `continue-on-error` or record why it stays; the runbook treats a missing wear asset as gate-blocking either way (V1). | **Required** (decision), code change recommended |
| Updater slug canonicalization in `AppUpdateChecker` / `WearAppUpdateChecker` | The updater URLs still point at `GlycemicGPT/glycemicgpt-android-unofficial` and rely on GitHub rename redirects. Redirects work today; canonicalizing removes a silent dependency. | Optional |
| Fix dangling `CONTRIBUTING.md` references to `CLAUDE.md` | `CLAUDE.md` does not exist in this repository; contributor docs should not point at it during the event window. | Optional |

**PASS:** each required PR merged to `develop` with green gates. **Capture:** PR links.

### P1.2 API-36 Wear AVD created

The existing wear AVD is API 35; the watchface ambient check (Step 4.4) requires an **API 36**
Wear OS AVD. Create it, boot it, and confirm it renders a watch face.
**PASS:** AVD boots to a rendered watch face. **Capture:** `avdmanager list avd` output + screenshot.

### P1.3 Phone↔wear emulator pairing rehearsed

Pair the phone emulator with the wear AVD (Wear OS pairing flow) end to end once, so pairing
mechanics are not being debugged on event day.
**PASS:** paired; a Data Layer message crosses (any wear feature works). **Capture:** screenshot.

### P1.4 Dev-channel upgrade rehearsal (free dress rehearsal of AC1 mechanics)

On the phone emulator: install the **monorepo-built** `dev.145` debug APK, use the app enough to
create local data, then install this repository's dev build (post-offset numbering) **over the
top**. Both are signed with the shared debug cert (`b04eacc8…`, table above), so this rehearses the
exact in-place path with zero user risk.
**PASS:** package installer treats it as an update (no uninstall, no cert error); app opens with
prior data intact. **FAIL:** any signature/downgrade error -- fix before scheduling the event.
**Capture:** both APK filenames, `apksigner verify --print-certs` on each, before/after screenshots.

Also **keep a pre-offset dev APK of this repository** (e.g. today's `dev.5`) on disk: `dev-latest`
is a rolling tag, so once the run-offset PR lands it is replaced -- and Step 4.6 needs a
pre-offset build as its dev-channel instrument.

### P1.5 Physical phone staged and synthetic seed rows recorded

The AC1 device: a physical phone running the **v0.13.0 release build**, logged into the
**dedicated test account**, pump paired over BLE, with at least one day of accumulated data.
Before the event, seed it with **synthetic marker rows** -- manually entered values chosen to be
in-range but distinctive (e.g. a manual bolus of an unusual-but-plausible size, a meal entry with
a recognizable name, readings at memorable timestamps) -- and post the exact synthetic values,
the relevant row counts, and a SHA-256 of the canonical marker list on the tracking issue. These
markers, not any organic history, are what Step 4.1 verifies; because they are synthetic, posting
them verbatim is safe (see the privacy rule) and makes verification exact. An **unseeded** run
self-masks a wipe: the app is designed to re-sync, and `allowBackup="false"` leaves no forensic
trail -- so a wiped database can look full again minutes later and record a PASS on catastrophic
data loss.
**PASS:** phone staged on the test account; synthetic-marker comment posted. **Capture:** the
marker comment (exact values + counts + hash) and a Settings version screenshot showing 0.13.0,
redacted of account identifiers.

### P1.6 Monorepo `develop` red gates triaged

At readiness time, monorepo `develop` head `aac94ee2` had failing **Dependency Scan Gate**,
**OSV-Scanner**, and **Security Scan Gate** check-runs. These block the *bridging* release (Phase
5), not this repository's promotion -- but triage them now, before the freeze, so the bridging
release is not improvised later. Check the known starlette/ecdsa and uv-graph triage classes first,
but **verify rather than assume**.
**PASS:** each red check green or formally dispositioned. **Capture:** check-run links + disposition.

### P1.7 Approvers enumerated and available

Three workflows gate jobs on the `release-gated` environment, each pausing for a
required-reviewer approval: `release.yml` (the release pipeline itself -- full approval topology
in P3.2), `changelog-pr.yml`, and `sync-main-to-develop.yml`. (`dev-pre-release.yml` is
**ungated** and publishes `dev-latest` on qualifying `develop` pushes -- another reason the
develop freeze matters.) List who can approve, confirm at least one approver is available for the
whole event window, and confirm everyone knows two rules from P3.2 in advance: **declining the
APK-build approval is the last free abort**, and the changelog/sync approvals are **left pending
until Phase 4 completes**.
**PASS:** named approvers + availability posted. **Capture:** the list.

### P1.8 Version plan fixed in writing

Both repositories' manifests sit at 0.13.0. Left alone, both would mint 0.13.1/130001 -- a
collision that strands migrated users on "Up to date" forever (or downgrade-blocks them if the
monorepo bumps first). Decide and record, before the event:

- the **bridging release** version (the final monorepo mobile release, e.g. 0.13.1), and
- this repository's first stable, forced **strictly greater** via a `Release-As:` commit footer
  (e.g. 0.14.0 → versionCode 140000).

Also schedule the **monorepo develop→main freeze**, effective from event start until the migration
window closes (Phase 5).
**PASS:** both versions + freeze window posted. **Capture:** the plan comment.

### P1.9 Renovate confirmed not live

Renovate go-live is deferred until after the destructive removal: a bot commit landing on `develop`
mid-window would invalidate the pinned parity SHAs. Confirm the app is not enabled / auto-merge is
dormant.
**PASS:** no Renovate activity possible on `develop`. **Capture:** statement + app-settings screenshot.

### P1.10 Recovery path pre-staged

Once a bad higher version is installed on a device, deleting the release cannot undo it -- the
**only** recovery is publishing an even higher signed version (see the appendix). That path must
be proven **before** the event, not assembled during an incident:

- a **named on-call maintainer** for the event window with release-approval rights;
- **verified signing access**: an `op read` of the release keystore item succeeds for that person
  (the P2.6 signer re-verification doubles as this proof if done by the on-call);
- a written **emergency plan**: the next patch `Release-As` version reserved, and the exact
  command sequence (P2.1 → P3.4) that would ship it;
- a **time bound**: target ≤ 2 hours from FAIL declaration to a published roll-forward release.

**PASS:** all four posted on the tracking issue. **Capture:** the plan comment.

## Phase 2 -- Pin, freeze, and capture (event start)

### P2.1 Land the `Release-As` version commit

Open a PR to `develop` whose **squash commit message** carries the ruled version as a
release-please footer, e.g.:

```text
chore: force first stable release version

Release-As: 0.14.0
```

GitHub's default squash message will not carry the footer by itself -- put it there explicitly,
either by pasting the footer into the merge dialog's extended description or via:

```bash
gh pr merge <pr> --squash --subject "chore: force first stable release version" \
  --body "Release-As: 0.14.0"
```

Verify after merge that the footer survived the squash: `git log -1 origin/develop` must show the
`Release-As:` line. Without it, release-please computes its own (colliding) version. If the footer
is missing, the recovery is simply another PR whose squash commit carries it (release-please
honors the latest `Release-As` in range; the freeze has not started yet).
**PASS:** footer present on the `develop` head commit. **Capture:** the `git log -1` output.

### P2.2 Freeze both repositories

- This repository: no further merges to `develop` (announce it; confirm nothing is queued to
  auto-merge). The freeze holds until the gate PASSes or aborts.
- Monorepo: the develop→main **promotion freeze** from P1.8 starts now.

**PASS:** both freezes announced. **Capture:** the announcements.

### P2.3 Pin the promotion SHA

```bash
git fetch origin
PIN=$(git rev-parse origin/develop)
```

Everything below verifies **this** SHA; it is the commit the promotion PR will carry to `main`.
**Capture:** `$PIN`.

### P2.4 Recompute the parity anchors at the pin

In this repository:

```bash
git rev-parse "$PIN:plugins/pump-driver-api" \
              "$PIN:plugins/shipped" \
              "$PIN:app/src/main/java/com/glycemicgpt/mobile/data/local" \
              "$PIN:app/schemas" \
              "$PIN:wear-device/src/main/AndroidManifest.xml" \
              "$PIN:app/src/release/assets/glycemicgpt-watchface-digitalFull.apk" \
              "$PIN:app/src/release/assets/glycemicgpt-watchface-analogMechanical.apk"
```

In the monorepo, first pin its side too -- monorepo `develop` is not frozen against merges (P2.2
freezes only its develop→main promotion), and the pump-driver-equivalence claim in Step 4.5 is
only coherent if the source-identity evidence and the unit suites use the **same** monorepo SHA:

```bash
git fetch origin
MONO_PIN=$(git rev-parse origin/develop)
```

Then compare (pump-driver trees against `$MONO_PIN`; persistence/wear/WFF anchors against the
**v0.13.0 tag**, the installed baseline):

```bash
git rev-parse "$MONO_PIN:plugins/pump-driver-api" \
              "$MONO_PIN:plugins/shipped" \
              v0.13.0:apps/mobile/app/src/main/java/com/glycemicgpt/mobile/data/local \
              v0.13.0:apps/mobile/app/schemas \
              v0.13.0:apps/mobile/wear-device/src/main/AndroidManifest.xml \
              v0.13.0:apps/mobile/app/src/release/assets/glycemicgpt-watchface-digitalFull.apk \
              v0.13.0:apps/mobile/app/src/release/assets/glycemicgpt-watchface-analogMechanical.apk
```

**PASS:** each pair matches across repositories (and matches the reference table; the pump-driver
re-pin at `$PIN` is the pump-driver-equivalence evidence artifact). **FAIL:** any mismatch is a
**NO-GO -- the event stops.** There is no operator-level waiver: diff the trees
(`git diff v0.13.0:<mono-path> $PIN:<path>`), post the finding, and end the attempt. Intentional
divergence can only ever be authorized by a **PM ruling recorded on the tracking issue before the
event**, with replacement compatibility evidence -- never by an inline judgment call mid-event.
**Capture:** `$MONO_PIN` and both command outputs, verbatim.

### P2.5 Capture green check-runs at the pin

```bash
gh api "repos/lumose-health/android-unofficial/commits/$PIN/check-runs" \
  --jq '.check_runs[] | "\(.name): \(.conclusion)"'
```

**PASS:** all required checks (`Android Gate`, `Security Scan Gate`, `Dependency Scan Gate`,
`Workflow Lint`, `Workflow Security`) concluded `success`. **Capture:** the output.

### P2.6 Independently re-verify the signer anchor (two people)

The `55f0d0cd…` digest in the reference table is a historical verification (2026-07-23). Before
any approval is given, re-derive it **from the sources, on event day**:

1. The on-call maintainer materializes the release keystore from the 1Password `android-signing`
   item just long enough to print its fingerprint, then shreds it (enter the store password from
   the same item when `keytool` prompts):

   ```bash
   umask 077
   op read "op://github/android-signing/release.jks" --out-file /tmp/parity-release.jks
   keytool -list -keystore /tmp/parity-release.jks | grep -i "SHA-256\|SHA256"
   shred -u /tmp/parity-release.jks
   ```

2. A **second person** independently runs `apksigner verify --print-certs` on a shipped monorepo
   v0.13.0 release APK and reads its digest aloud.
3. Both values must equal each other **and** the reference-table digest. Any disagreement is a
   NO-GO before anything has happened -- resolve which value is real before rescheduling.

**PASS:** three-way match, confirmed by two named people. **Capture:** both outputs + both names
on the tracking issue.

## Phase 3 -- Promotion and release

### P3.1 Create and merge the promotion PR

```bash
gh pr create --base main --head develop --title "chore: promote develop to main"
```

Merge with **Create a merge commit** (preserves the develop→main ancestry; see
[Release & Promotion](./release-and-promotion.md)). Confirm the merged head of `main` contains
`$PIN`.

### P3.2 Walk the sequential release approvals

The promotion push starts more than one gated workflow, and `release.yml` itself runs **twice**
(once on the promotion push, again on the release-please squash push). The operator will therefore
see gated approval prompts from **three workflows across two `release.yml` runs** -- approve only
the release-pipeline ones, in order, verifying each job's output before approving the next:

**Run 1 (trigger: the promotion merge commit):**

1. **Release Please** -- must propose a release PR at the `Release-As` version from P2.1
   (e.g. 0.14.0), not a collision-range 0.13.x. Wrong version → do not approve; stop and diagnose.
2. **Auto-merge Release PR** -- squash-merges the release PR onto `main` as `chore: release X`.
3. The **Fallback Patch Release** path must **not** fire when the `Release-As` commit is present;
   if its approval prompt appears, stop -- the version plan has been bypassed.

**Run 2 (trigger: the `chore: release X` push):**

4. **Release Please** (again) -- **this approval is what mints the tag and the GitHub Release**,
   which per the P1.1 draft-first hardening is created as a **draft** (invisible to
   `releases/latest` and to every installed updater). Verify the run shows the expected tag, and
   confirm the release really is a draft (`gh release view "$TAG" --json isDraft`) before
   proceeding -- if it is not a draft, the containment assumption is void: treat any subsequent
   verification failure as a live incident (bundle + delete immediately, abort ladder rung 2).
5. **Build & Upload Release APK** -- ⛔ **this approval is the last free abort.** Declining it
   leaves a tag and an asset-less draft and **no installed user affected** (delete the draft per
   the abort ladder). If anything above looked wrong, decline here.

**Leave pending -- do not approve until Phase 4 completes:**

- **`changelog-pr.yml`** (fires on the promotion push) and **`sync-main-to-develop.yml`** (fires
  on the release push). Approving the sync mid-event pushes the version-bump commit onto `develop`
  **during the declared freeze** and re-triggers the ungated `dev-pre-release.yml`, publishing a
  new `dev-latest` that changes the dev-channel expectation in Step 4.6. Both prompts wait
  harmlessly; approve them after the gate PASSes.

**Capture:** both run URLs and, per approval, the job output you verified before approving.

### P3.3 Verification block (runs against the DRAFT, before anything is discoverable)

Run while the release is still a draft -- **before publishing, before any announcement, and
before any device work**. No wildcard ever touches this block: every check names its file
exactly, because a `GlycemicGPT-*-release.apk` glob also matches the Wear and the
intentionally-unsigned WatchFace APKs. Download every asset to a clean directory:

```bash
TAG=v0.14.0        # the tag just created
VER="${TAG#v}"
gh release download "$TAG" --repo lumose-health/android-unofficial -D "parity-$TAG"
```

**V1 -- exact asset inventory (checked first; missing AND extra assets both fail).**

```bash
printf '%s\n' \
  "GlycemicGPT-${VER}-release.apk" \
  "GlycemicGPT-WatchFace-Analog-${VER}-release.apk" \
  "GlycemicGPT-WatchFace-Digital-${VER}-release.apk" \
  "GlycemicGPT-Wear-${VER}-release.apk" | LC_ALL=C sort > expected-assets.txt
ls -1 "parity-$TAG" | LC_ALL=C sort | diff -u expected-assets.txt -
```

The diff must be **empty** -- the release carries exactly these four APKs, no more, no fewer.
A missing wear or watchface asset means its `continue-on-error` build step failed silently
(gate-blocking, per global rule 4); an unexpected extra (a stray or wrong-version APK) is equally
a FAIL, because installed v0.13.0 selectors pick by unanchored matching. (This check was
validated against a correct set, a phone-only publish, and a stray-extra-asset scenario --
all three behave as stated.)
**FAIL:** any diff output → abort ladder rung 2.

**V2 -- signing certificate, each APK by explicit name.**

```bash
apksigner verify --print-certs "parity-$TAG/GlycemicGPT-${VER}-release.apk"        # phone
apksigner verify --print-certs "parity-$TAG/GlycemicGPT-Wear-${VER}-release.apk"   # wear
```

Both SHA-256 digests must equal the P2.6 re-verified release signer (`55f0d0cd…342990`). The two
WatchFace APKs are **deliberately excluded** from this check -- they are unsigned by long-standing
pre-split behavior, and the artifacts users actually receive are the committed in-app WFF assets
verified in Step 4.4. Do not let any tooling glob them into a signature check in either direction.
**FAIL:** any other digest (the debug cert `b04eacc8…` here means the keystore never materialized
and the build fell back to debug signing) → abort ladder rung 2.

**V3 -- version identity, phone AND wear.**

```bash
aapt dump badging "parity-$TAG/GlycemicGPT-${VER}-release.apk"      | grep '^package:'
aapt dump badging "parity-$TAG/GlycemicGPT-Wear-${VER}-release.apk" | grep '^package:'
```

For **each** of the two: `name` must be `com.glycemicgpt.mobile`; `versionCode` must be
**> 130000** and equal the planned value (e.g. `140000`); `versionName` must match `$VER`.
**FAIL:** any mismatch on either APK → abort ladder rung 2.

**Capture:** all command output above, plus `sha256sum parity-$TAG/*` (the checksums also feed
Step 4.1's install-integrity check and, on a FAIL, the failure bundle).

### P3.4 Publish the release

Only after V1--V3 all PASS:

```bash
gh release edit "$TAG" --repo lumose-health/android-unofficial --draft=false --latest
gh release view "$TAG" --json isDraft,isLatest   # isDraft false, isLatest true
```

Publishing is the go-live act -- from this moment the release is discoverable by every installed
updater. **Capture:** the edit + view output.

### P3.5 Abort ladder

In escalating order -- each rung is complete in itself; never skip down the ladder:

1. **Before APKs exist:** decline the Build & Upload approval. No installed user is affected;
   post the decline reason on the tracking issue, delete the asset-less draft
   (`gh release delete "$TAG"`, tag kept), then fix on `develop` (freeze lifted for the fix only)
   and restart from P2.1 with the next patch `Release-As`.
2. **Verification failed (draft or, worse, published):** ⛔ **persist the failure bundle FIRST,
   delete SECOND.** The bundle -- posted to the tracking issue before any deletion -- is: the
   release asset listing, `sha256sum parity-$TAG/*`, the failing `apksigner`/`aapt`/diff output,
   and confirmation that the local `parity-$TAG` directory is archived and kept (post its
   directory-archive hash). The downloaded copies are the only forensic record of what was almost
   shipped; deleting the release first can strand the diagnosis. While the release is still a
   draft this costs nothing (it is not discoverable); if it was already published, the bundle is
   already on local disk from P3.3 -- posting it takes seconds and still precedes the delete.
   Then `gh release delete "$TAG"` (⛔ **keep the tag** -- see the appendix for why deletion
   genuinely stops the bleed). Fix, then restart from P2.1. ⛔ The burned version number is never
   reused: the next attempt is a patch bump, because a re-released equal versionCode is invisible
   to every installed updater.
3. **After device work has started (Phase 4 FAIL):** same as rung 2, plus post the on-device FAIL
   evidence before touching anything else -- the failed state on the test phone is diagnostic
   evidence; ⛔ do not uninstall, clear data, or "reset and retry" until it has been captured and
   understood.

## Phase 4 -- Device parity

Order is fixed. Each step assumes the previous PASSed.

### Step 4.1 -- Seeded in-place upgrade on the physical phone (AC1)

On the staged phone from P1.5 (v0.13.0, pump paired, seed rows recorded):

1. Put the **phone** release APK on the phone from the copy already verified in P3.3 -- never a
   fresh browser download (the release page lists four similarly named assets, and the Wear APK
   shares the phone app's `applicationId` and cert, so installing the wrong one would consume the
   one-shot seeded upgrade). Then confirm the bytes on-device match the verified copy:

   ```bash
   adb push "parity-$TAG/GlycemicGPT-${VER}-release.apk" /sdcard/Download/
   adb shell sha256sum "/sdcard/Download/GlycemicGPT-${VER}-release.apk"
   sha256sum "parity-$TAG/GlycemicGPT-${VER}-release.apk"   # must match the line above
   ```
2. Enable **airplane mode**, then re-enable **Bluetooth only**. Airplane mode is the point of the
   test: with the network up, the app re-syncs and a wiped database refills itself -- the wipe
   self-masks, and `allowBackup="false"` means there is no backup artifact to autopsy afterward.
3. **Assert the isolation -- do not infer it from the toggle.** A surviving Wi-Fi association,
   cellular data path, or VPN produces exactly the false PASS this step exists to prevent:

   ```bash
   adb shell settings get global airplane_mode_on   # must print 1
   adb shell settings get global wifi_on            # must print 0
   adb shell ping -c 1 -W 2 8.8.8.8                 # must FAIL (network unreachable)
   ```

   All three must hold **before install**, and the ping must be re-run (and still fail)
   **immediately before row validation** in step 5. If any check shows a live network path, fix
   it before installing; if a network was live at any point after install, the row validation is
   void -- the run cannot distinguish surviving data from re-synced data.
4. Install over the top via the package installer. It must present as an **update** to the
   existing app.
5. Open the app -- still isolated (re-run the ping check) -- and verify the **synthetic marker
   rows** against the P1.5 comment.

**PASS (all of):**
- installer offered "Update" and completed without error;
- app opens **logged in** (no login screen);
- every synthetic marker row from the P1.5 comment is present with its exact values + timestamps;
- pump BLE pairing intact -- the pump reconnects without re-pairing;
- Settings shows the new versionName/versionCode;
- the isolation checks held throughout (step 3).

**FAIL (any of):** an install error ("App not installed", signature mismatch, downgrade block), a
login screen, any missing or altered marker row, a dead pairing, or an isolation check that failed
after install (which voids the run rather than passing it).
⛔ **On install failure, never uninstall to proceed** (global rule 1) -- capture the installer
error and `adb logcat` tail, then go to abort-ladder rung 3.

**Capture:** before/after screenshots of the marker rows (redacted of account identifiers), the
isolation-check outputs, the installer prompt, Settings version screen, pump connection state.

### Step 4.2 -- Phone↔watch messaging in the skew configuration (AC2)

Run with the **upgraded phone + the old (monorepo-built) watch app** -- the exact state every
migrating user passes through, since the phone always upgrades first.

1. **Chat quick-query first.** Send a quick query from the watch. Its visible ~30 s timeout is the
   loss-vs-latency discriminator: a timeout is a clean transport FAIL rather than an ambiguous
   hang. A displayed response is phone-mediated by construction -- the watch has no chat path of
   its own, so an answer can only have travelled watch → phone relay → backend → phone → watch --
   but guard the two ways a *stale* artifact could still fake it:
   - use a **data-dependent** quick query (e.g. current glucose), and require the response to
     reflect the device's present state -- a cached or previously-rendered answer cannot;
   - keep a **phone-serial-qualified** log monitor running during the test,
     `adb -s <phone-serial> logcat -v time -s GlycemicGPT`, to catch relay *failure* lines. Note
     the relay's success-path lines (`Received chat request from watch`, `Sent chat response to
     watch`) are debug-level and compiled out of release builds by the WARN-floor logging tree --
     their absence is expected on a healthy run and must not be read as a FAIL; only failure
     lines appear, and any that do appear are a FAIL even if a response rendered.
2. **Alert-dismiss round-trip.** Trigger an alert, dismiss it on the watch, and judge the result
   **phone-side only**: the alert must show **acknowledged in the phone UI within ~10 s**. That UI
   state is the sole PASS instrument on a release build -- `WearChatRelayService`'s success-path
   log lines are debug-level, and the release logging tree drops everything below WARN (and tags
   what survives `GlycemicGPT`, not the class name), so an empty
   `adb logcat -s WearChatRelayService` on a healthy round-trip is expected, not a FAIL. If you
   watch logcat at all, use `adb logcat -s GlycemicGPT` and expect only *failure* lines to appear.

   ⚠️ The watch UI is a **false-pass instrument** for this test: `AlertsActivity` discards the send
   result and clears the alert locally unconditionally, so a dead transport still *looks* dismissed
   on the watch. Watch-side appearance is not evidence.

**PASS:** a data-dependent quick-query response within the timeout with no relay failure lines,
AND phone-side alert acknowledgment within ~10 s.
**FAIL:** quick-query timeout, any relay failure line, or no phone-side acknowledgment
(regardless of what the watch shows).
**Capture:** watch + phone screenshots with timestamps, redacted of account identifiers (plus any
logcat failure lines).

### Step 4.3 -- Wear OTA push (the watch's only upgrade path)

The watch has no updater of its own -- the phone pushes the wear APK over the Data Layer
(`WearApkPusher`, surfaced through the wear-update flow in Settings; `WearAppUpdateChecker`
compares versions). This path appears in no acceptance criterion but is the only way the migrating
cohort's watches ever upgrade, so it is verified here.

1. From the upgraded phone app, run the wear app update flow; it should offer the new version.
2. Push to the watch and let it install **in place**.
3. Confirm the watch app's new version, then re-run one chat quick-query in the now-matched
   (new phone + new watch) configuration.

**PASS:** watch updates without uninstall, opens, and the matched-config quick-query round-trips.
**FAIL:** push fails, or the watch requires an uninstall (same in-place principle as Step 4.1).
**Capture:** watch version screen before/after, quick-query screenshot.

### Step 4.4 -- Watchface parity (AC4, on the API-36 wear AVD)

The user-facing watchface artifacts are the **committed in-app WFF assets** delivered by
`WatchFacePusher` -- not the unsigned release-page WatchFace APKs.

1. **Byte-identity** -- already proven at P2.4 (the two WFF blob hashes match the monorepo v0.13.0
   tag). Re-run the two `git rev-parse` asset lines at the release tag and confirm they still match.
2. **Signature** -- verify the blobs **as committed at the release tag**, not the working tree (a
   locally regenerated asset would otherwise pass here while step 1 reads the tag -- two checks
   silently examining different bytes):

   ```bash
   git show "$TAG:app/src/release/assets/glycemicgpt-watchface-digitalFull.apk" > /tmp/wff-d.apk
   git show "$TAG:app/src/release/assets/glycemicgpt-watchface-analogMechanical.apk" > /tmp/wff-a.apk
   apksigner verify /tmp/wff-d.apk
   apksigner verify /tmp/wff-a.apk
   ```

3. **Ambient string-fit** -- push each face to the **API-36** wear AVD (P1.2), enter ambient mode,
   and confirm BG value, trend arrow, IoB, and time all render without truncation or overlap.

⛔ **Do not regenerate the WFF assets for any reason during the event** (global rule 2). If a hash
or signature check fails, that is a FAIL finding -- investigate; do not rebuild.

**PASS:** hashes match, both `apksigner verifies`, ambient renders clean on both faces.
**Capture:** command outputs + ambient screenshots of both faces.

### Step 4.5 -- Pump-driver equivalence (AC5, structural)

Equivalence is ruled **structural**: identical sources + identical toolchain + passing
captured-frame test suites, which is stronger evidence than an ad-hoc runtime harness invented on
event day. Three parts, all required:

1. **Source identity** -- the P2.4 re-pin of `plugins/pump-driver-api` and `plugins/shipped` at
   `$PIN` (this is the evidence artifact; expected values in the reference table).
2. **Toolchain identity -- verified, not asserted.** Compare the blob hashes of the toolchain
   definition and every module lockfile across the two pins (plugin-module lockfiles are already
   inside the matched plugin trees). All seven pairs were verified identical on 2026-07-23;
   recompute them at the pins -- each pair must match, same NO-GO rule as P2.4:

   ```bash
   # This repo:
   git rev-parse "$PIN:gradle/libs.versions.toml" \
                 "$PIN:gradle/wrapper/gradle-wrapper.properties" \
                 "$PIN:gradle.properties" \
                 "$PIN:settings-gradle.lockfile" \
                 "$PIN:app/gradle.lockfile" \
                 "$PIN:wear-device/gradle.lockfile" \
                 "$PIN:watchface/gradle.lockfile"
   # Monorepo (same seven, under the apps/mobile/ prefix):
   git rev-parse "$MONO_PIN:apps/mobile/gradle/libs.versions.toml" \
                 "$MONO_PIN:apps/mobile/gradle/wrapper/gradle-wrapper.properties" \
                 "$MONO_PIN:apps/mobile/gradle.properties" \
                 "$MONO_PIN:apps/mobile/settings-gradle.lockfile" \
                 "$MONO_PIN:apps/mobile/app/gradle.lockfile" \
                 "$MONO_PIN:apps/mobile/wear-device/gradle.lockfile" \
                 "$MONO_PIN:apps/mobile/watchface/gradle.lockfile"
   ```

   Then capture `./gradlew --version` from **both** pinned checkouts -- Gradle 8.12 and a
   JVM 17 line, byte-comparable across the two outputs (AGP 8.7.3 / Kotlin 2.1.0 are pinned by
   the now-proven-identical `libs.versions.toml`).
3. **Dual green unit suites** -- at the pinned SHAs, in **both** repositories. In this repo, run
   at `$PIN` (the repo root is the Gradle root). In the monorepo, run at a checkout or worktree of
   **`$MONO_PIN`** (captured in P2.4 -- not whatever `develop` has drifted to since), from its
   mobile Gradle root `apps/mobile/`:

   ```bash
   ./gradlew testDebugUnitTest
   ```

   These suites include the real captured-frame decode vectors (Medtronic 249 and 141 mg/dL, IoB
   1.4 IU; Tandem IoB 0.192/0.154 U, basal 1.0 U/hr; the real 780G SAKE trace), so a pass is a
   behavioral statement about real pump bytes, not just compilation.

**PASS:** all tree and toolchain hash pairs match + matching `--version` outputs + both suites
100% green. **FAIL:** any divergence -- **STOP the event**; this is the safety-critical leg and
there is no accepted-residual path around it.
**Capture:** both hash-comparison outputs, both `./gradlew --version` outputs, and both test-run
summaries (repo, SHA, task, result counts).

### Step 4.6 -- Updater discovery from the new build (AC3)

The update channel is a **compile-time build-type field** (`BuildConfig.UPDATE_CHANNEL`: release
builds are `stable`, debug builds are `dev` -- see `app/build.gradle.kts`); there is no runtime
channel switch. Each channel is therefore verified from the build type that actually uses it:

1. **Stable channel -- on the phone upgraded in Step 4.1** (airplane mode off now): Settings →
   check for updates. It must reach this repository's `releases/latest`, parse the new release,
   and report **up to date** at the just-installed version. "Up to date" is only reported after a
   successful fetch + parse + asset match, so it proves URL, parse, and compare all work from the
   shipped binary.
2. **Dev channel -- on the P1.4 emulator**, with a **pre-offset** dev build of this repository
   installed (its `dev.<n>` below the installed cohort's `dev.145` lineage): run its update check.
   It must discover `dev-latest` at the **offset** run numbering from P1.1 and offer the upgrade
   -- proving the dev channel is not stranded on "up to date" by the run-number cliff.

**Explicitly out of scope:** discovery by monorepo-built installs. That is the bridging release's
job (Phase 5 / GLY-94 AC2), not this gate's.

**PASS:** both checks resolve and report the correct verdicts, no errors surfaced.
**Capture:** screenshots of both check results (emulator screenshot to include the installed
dev version so the offset comparison is legible).

## Phase 5 -- Sign-off and the migration window

### P5.1 Gate sign-off

Before posting, close the pinning loop: re-run `git rev-parse origin/develop` in **both**
repositories. This repository's head must still equal `$PIN` (the freeze held); if the monorepo
head has moved past `$MONO_PIN`, confirm every monorepo evidence item cites `$MONO_PIN` -- the
pins are what the evidence anchors to, and the sign-off records both final heads either way.

The accountable maintainer then posts the sign-off comment on the tracking issue: an enumeration
of every gate leg with a link to each captured evidence comment. Two standing notes belong in it:

- the parity-plan review requirement is satisfied by this runbook's own reviewed PR;
- the Sentry validation gate is **N/A** for this gate by ruling -- it is backend/web/sidecar-scoped,
  and mobile release builds compile the Sentry DSN to `""`.

The destructive-removal work (GLY-94) stays blocked until this comment exists.

### P5.2 Bridging release (sequenced here, executed under GLY-94)

The final monorepo mobile release, whose build already points updaters at this repository. It is a
separate event with its own constraints, restated here because this runbook's Phase 1/P1.8
decisions feed it:

- ⛔ **Phone APK only -- asserted, not assumed.** Delete the Wear and WatchFace assets from the
  bridging release, then **verify the surviving asset set before any announcement**: the release
  must carry **exactly one** asset, the phone APK by its exact name, and that file must verify
  against the release signer:

  ```bash
  gh release view <bridging-tag> --repo lumose-health/GlycemicGPT \
    --json assets --jq '.assets[].name'          # exactly one line: the phone APK
  gh release download <bridging-tag> --repo lumose-health/GlycemicGPT -D bridging-check
  apksigner verify --print-certs bridging-check/<phone-apk-name>   # == 55f0d0cd…342990
  ```

  Any Wear or WatchFace asset still present, any extra asset, or a wrong digest → **abort the
  announcement** and fix first. Why this is absolute: installed v0.13.0 devices run the
  pre-anchored `firstOrNull` APK selector, which matches all four release assets and picks the
  phone APK only by asset-ordering luck; the wear APK shares the same `applicationId` and cert,
  so a wear-first ordering would install the **Wear APK over the phone app** -- killing CGM
  display and the local alert floor on a user's primary device.
- **Version-coordinated:** strictly **below** this repository's first stable (P1.8), so migrated
  users are never downgrade-blocked and un-migrated users still see the hop.
- **Monorepo gates green first** (P1.6 triage must be complete).

### P5.3 Soak and window

- The bridging release soaks behind a **recorded soak sign-off** -- a posted decision, not merely
  elapsed time -- before the point-of-no-return removal step.
- Recommended posture: **≤ 14 days** in the half-promoted state; a **30--60 day** migration window
  overall.
- The monorepo develop→main freeze (P2.2) holds until the migration window closes, and the
  monorepo remains **the only patch vehicle for the un-migrated cohort** until the removal lands --
  which is exactly why the freeze must not quietly rot into unreleased monorepo changes.

## Appendix -- Rollback posture (verified)

Why "delete the release" is a real mitigation and what it does not cover:

- Update checks are **manual-only, uncached, and Settings-triggered** -- there is no background
  poller and no CDN/cache layer between the app and the GitHub releases API. Deleting a bad
  release therefore genuinely stops the bleed: the next manual check simply no longer sees it.
  **Residual:** a user mid-flow who has already downloaded the APK can still install it; nothing
  server-side can retract that.
- **Field rollback is roll-forward.** Android will not install a lower versionCode over a higher
  one, so a bad shipped build is superseded by publishing a **higher** versionCode -- realistically
  **1--2 hours** with a maintainer at the keyboard (patch bump through the same gated pipeline).
  This path is pre-staged before the event -- named on-call, verified key access, reserved
  emergency version, time bound -- per P1.10; it is never assembled during the incident.
- Burned version numbers stay burned (global rule 3): the tag remains, the number is never reused,
  and the next attempt is always a patch bump.
