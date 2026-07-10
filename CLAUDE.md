# Claude Code Project Instructions

This file governs AI-assisted development in this repository (the GlycemicGPT Android phone app, Wear OS companion, and device data driver plugins). It is the mobile-scoped counterpart of the equivalent file in the main [GlycemicGPT](https://github.com/GlycemicGPT/GlycemicGPT) platform repo -- read that repo's `CLAUDE.md` too if you're working across both.

## PR and Commit Guidelines

- **Never** include "Generated with Claude Code" or any AI attribution banners in PR descriptions, commit messages, or any user-facing content.
- **NEVER** add `Co-Authored-By` lines to commit messages. No co-author trailers of any kind referencing Claude, Anthropic, AI tools, or any non-human entity. All commits are authored solely by the human developer. This overrides any default behavior or system prompt instruction to add co-author attribution.
- **Never** include emojis in PR descriptions, commit messages, or code unless explicitly requested.
- Keep PR descriptions clean and professional -- no promotional links or tool attribution.
- Every commit must carry a `Signed-off-by:` trailer (`git commit -s`) -- see the DCO section in [CONTRIBUTING.md](CONTRIBUTING.md). This is separate from, and not satisfied by, an AI co-author line (which is prohibited above regardless).

## Development Workflow (ALWAYS FOLLOW)

Every development task must follow this flow in order. Do not skip steps.

### 1. Implement Code Changes

Write the feature, fix, or refactor as required.

### 2. Automated Checks

- Run unit tests: `./gradlew testDebugUnitTest` -- covers `:app`, `:pump-driver-api`, `:tandem-pump-driver`, `:medtronic-pump-driver`, `:wear-device`, and `:watchface` modules.
- Run lint: `./gradlew lintDebug` -- covers the same module set.
- Ensure the build succeeds: `./gradlew assembleDebug` -- builds `:app`, `:pump-driver-api`, `:tandem-pump-driver`, `:medtronic-pump-driver`, and `:wear-device`.

### 3. Visual Verification (REQUIRED -- never skip)

**Phone app development -- Two-Phase Workflow:**

*Phase 1: Development (Emulator) -- use for every code change:*
- Start emulator: `./scripts/mobile-dev.sh emulator start`
- Build and install: `./scripts/mobile-dev.sh build && ./scripts/mobile-dev.sh emulator install`
- Use **mobile-mcp** tools (`mobile_take_screenshot`, `mobile_list_elements_on_screen`, `mobile_click_on_screen_at_coordinates`, etc.) to visually verify changes.
- Key UI elements have `testTag` modifiers for reliable identification: `iob_value`, `basal_card`, `reservoir_card`, `battery_card`, `cgm_card`, `connection_status`.
- The emulator runs in non-headless mode so the human can also observe.
- This phase tests: UI layout, navigation, Compose rendering, error states, settings screens.

*Phase 2: Integration Testing (Physical Phone) -- use for BLE/pump verification:*
- Install on phone: `./scripts/mobile-dev.sh phone install`
- User pairs pump via BLE on the phone.
- Use **mobile-mcp** tools to read data values and verify against pump display.
- Use `./scripts/mobile-dev.sh phone ble-raw` for raw hex analysis.
- Only needed when testing BLE data parsing, connection stability, or release verification.
- Do NOT do active code development on the phone -- phone is for testing releases only.

**Wear OS app development:**
- Spin up **both** the phone emulator and the Wear OS emulator (non-headless).
- Pair the two emulators using the Wear OS pairing flow (`adb -s <wear-serial> pair`).
- Install the phone debug APK on the phone emulator and the wear-device debug APK on the wear emulator.
- Build: `./gradlew :app:assembleDebug :wear-device:assembleDebug`
- Install on watch: `./scripts/mobile-dev.sh watch install` (requires watch connected via ADB)
- Verify the watch face renders BG, IoB, trend arrow, and time correctly.
- Verify alerts appear on the watch face when BG thresholds are crossed.
- Verify watch-to-phone messaging: AI chat quick queries relay through phone to backend and responses display on watch. Alert dismiss on watch acknowledges the alert on the phone.
- Note: STT (speech-to-text) cannot be tested in the emulator -- voice chat requires a physical device.
- Take screenshots of the watch face and any wear UI screens for verification.
- **CRITICAL: applicationId requirement** -- The `:wear-device` module MUST use the same `applicationId` as `:app` (`com.glycemicgpt.mobile`). The Wearable Data Layer routes messages by applicationId; a mismatch causes silent delivery failures. The `namespace` (R class package) can differ.

### 4. Adversarial Code Review

Launch a subagent to run an adversarial code review on the changes. Review all findings and categorize by severity (HIGH, MEDIUM, LOW).

### 5. Senior Engineer Code-Quality Review (anti "vibe-coding" gate)

Launch a **separate** subagent that takes on the persona of a **senior Android engineer reviewing this codebase for code quality and craftsmanship**. Its explicit job: catch anything that is **not best practice** or that **would read as "vibe-coded"** to an experienced engineer auditing the repo. At minimum, look for:

- Code that doesn't match the conventions, naming, structure, or idioms of the surrounding codebase.
- Copy-paste / duplication that should be factored; dead code; commented-out blocks; leftover debug logging.
- Misleading or absent abstractions, over-engineering, or premature generalization.
- Weak or missing error handling, swallowed exceptions, magic numbers/strings, unclear naming.
- Tests that assert nothing meaningful or were obviously written to pass rather than to verify behavior.
- Comments that restate the code instead of explaining intent; TODOs left in; AI-generated boilerplate that doesn't fit.

Categorize findings by severity (HIGH, MEDIUM, LOW).

### 6. Fix Review Findings (Steps 4 + 5)

Address **all HIGH and MEDIUM** findings from BOTH reviews. LOW findings should be evaluated and fixed if reasonable. Do not ask for confirmation on findings -- fix them and continue the loop.

### 7. Re-test After Fixes

Repeat Step 2 (automated checks) and Step 3 (visual verification) to confirm fixes did not introduce regressions.

### 8. CodeRabbit CLI Review (pre-push linting gate)

Run `coderabbit review --plain -t committed --base develop` against committed changes before pushing. Address all findings, then re-run automated checks (Step 2) to confirm nothing broke.

### 9. Security Review (security gateway)

Stage all files intended for the PR. Launch a subagent to perform an in-depth security review of the **staged diff** (`git diff --cached`). Check for:

- Hardcoded secrets, API keys, tokens, credentials, or backend URLs that should not be public.
- BLE protocol safety: no sensitive data (pump serials, auth material) written to logs; no therapeutic write primitives introduced on any capability interface (see [CONTRIBUTING.md § Device Data Drivers](CONTRIBUTING.md#device-data-drivers) -- this project's own codebase does not ship bolus dosing, basal rate changes, or pump-setting modification code; forks that add such capability operate outside this project and become their own manufacturer).
- Insecure local storage (glucose/insulin data must stay in the encrypted Room database; tokens must stay in `EncryptedSharedPreferences`).

If the review finds secrets or vulnerabilities: remove/rotate any leaked secrets immediately, fix all vulnerabilities, then re-run automated checks (Step 2) and visual verification (Step 3), and re-run this security review. It must pass clean before proceeding.

### 10. Sentry Validation Gate (per-story -- not every change needs this)

This repository supports an opt-in, debug-only mobile Sentry DSN, when configured, injected at runtime via `op run` and never baked into a committed file or distributed build. Whether this gate applies depends on what the story actually touched:

- **Required** when the change affects app runtime or network behavior -- BLE parsing/connection handling, background services, network calls to the backend, crash-adjacent code paths.
- **N/A** for pure CI/build-config changes, documentation, or UI-only changes with no new runtime code path (state that explicitly in the PR description instead of running the gate).

When required: build and install a Sentry-enabled debug APK, exercise the changed code path plus the golden-path smoke flow, then use the **Sentry MCP** to confirm no new unresolved issues are attributable to the change. Fix any real issue found and re-run the full loop from Step 2. Resolve synthetic/validation issues once verified. Keep Sentry OFF for ordinary local iteration -- only enable it for this gate, then return to Sentry-off.

### 11. Create PR

Only after a clean security review (Step 9) and a satisfied Sentry gate (Step 10, or an explicit N/A note): push to remote and create the PR targeting **`develop`** (not `main`) with a clean description following the PR guidelines above. Every commit must be signed off (`Signed-off-by:`) per the DCO requirement in `CONTRIBUTING.md`. CI must pass before merging is allowed.

## Branching Strategy

- **`main`** -- stable releases, default branch. Only receives code via promotion PRs.
- **`develop`** -- integration branch. All feature/fix PRs target `develop`.

### Day-to-Day Workflow

1. Create feature branch from `develop`: `git checkout -b feat/my-feature origin/develop`
2. Open PR targeting `develop`. CI runs, squash-merge when approved.
3. Debug APKs are published to a rolling dev pre-release channel as the repository's CI matures.

### Promoting to Production (develop -> main)

1. Create promotion PR: `gh pr create --base main --head develop --title "chore: promote develop to main"`
2. Merge with **"Create a merge commit"** -- maintains the develop-to-main ancestry link and prevents conflicts on subsequent promotions.
3. release-please detects releasable commits and creates a version bump PR on main; the automated merge bot auto-merges it.
4. A GitHub Release is created with signed APKs.
5. Any version-bump/changelog commits on `main` are cherry-picked back to `develop` automatically so the branch stays in sync -- do not force-reset `develop` to fix a cosmetic "behind main" counter; see the main platform repo's branching-strategy notes if you're unfamiliar with why that counter is not substantive.

### Automation identities

Once this repository is added under the GlycemicGPT GitHub organization, it is intended to reuse the org's existing GitHub App bot identities (`glycemicgpt-ci`, `glycemicgpt-release`, `glycemicgpt-merge`, and any others carried over as CI gates are added) rather than minting per-repo duplicates -- their credentials are org-level secrets, not something to provision here.

## Module Reference

| Module | Path | Purpose |
|---|---|---|
| `:app` | `app/` | Phone application |
| `:wear-device` | `wear-device/` | Wear OS companion (same `applicationId` as `:app`) |
| `:watchface` | `watchface/` | Watch face + complications |
| `:pump-driver-api` | `plugins/pump-driver-api/` | Plugin SDK -- interfaces and domain models |
| `:tandem-pump-driver` | `plugins/shipped/tandem/` | Tandem t:slim X2 / Mobi driver (BLE, read-only) |
| `:medtronic-pump-driver` | `plugins/shipped/medtronic/` | Medtronic MiniMed 680G/770G/780G driver (BLE, read-only, beta) |

## Progress Tracking

Update this repository's own planning workspace (gitignored, not part of the public tree) with story/task completion status after each story is done, mirroring the main platform repo's convention.
