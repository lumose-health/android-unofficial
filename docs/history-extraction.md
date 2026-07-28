# History extraction from the GlycemicGPT monorepo

This repository's Android/Wear OS code was extracted from the
[GlycemicGPT monorepo](https://github.com/GlycemicGPT/GlycemicGPT) with full
git history. This document records exactly how, so the extraction is
reproducible and auditable.

## Source

- Monorepo: `https://github.com/GlycemicGPT/GlycemicGPT.git`
- Branch: `develop`
- Commit: `2ffc3607d1fd7676070181d2886d476d7868f268`
- Date of extraction: 2026-07-10

## Command

Run on a fresh clone (`git clone -b develop <monorepo-url>`), never on a
working checkout:

```bash
# Pin to the recorded source commit. If develop has moved past it, run
# `git reset --hard 2ffc3607d1fd7676070181d2886d476d7868f268` first and
# pass --force to filter-repo (safe: the clone is a throwaway).
git filter-repo \
  --path apps/mobile/ \
  --path plugins/ \
  --path tools/medtronic-ble-spike/ \
  --path scripts/mobile-dev.sh \
  --path LICENSE \
  --path-rename apps/mobile/:

# filter-repo rewrites (not deletes) tags reachable from retained history;
# strip them all per the tag decision below.
git tag -l | xargs -r git tag -d
```

### Path list rationale

| Path | Why |
|------|-----|
| `apps/mobile/` | The Gradle build root: `:app`, `:wear-device`, `:watchface`, wrapper, version catalog, `shell.nix`, `THIRD_PARTY_LICENSES.md`. Re-rooted to `/`. |
| `plugins/` | `pump-driver-api` (plugin SDK), `shipped/tandem`, `shipped/medtronic` (which consumes JavaSake, `org.openminimed:javasake`, from Maven Central -- the SAKE crypto is no longer vendored), and `example` (third-party plugin template). Kept as a root-level `plugins/` subtree per the epic layout ruling. |
| `tools/medtronic-ble-spike/` | Android BLE research spike; carried for provenance per the epic layout ruling. Standalone Gradle build with its own `settings.gradle.kts`; not part of the app build. |
| `scripts/mobile-dev.sh` | The mobile dev helper script the workflow documents. |
| `LICENSE` | GPL-3.0, identical to the scaffold's copy; carried so license history travels with the code. |

Deliberately excluded: `docs/` (platform documentation stays in the
monorepo), all backend/web/sidecar code, monorepo CI workflows (this repo's
CI is built separately), and monorepo root community files (this repo has its
own reviewed versions).

## Tag and blob decisions

- **All monorepo tags stripped** (123 tags: platform `v0.x`, `changelog-*`,
  `dev-latest`). This repository's releases are seeded fresh by its own
  release tooling; carrying platform version tags would corrupt
  release-please's versioning baseline.
- **No blobs stripped.** `git filter-repo --analyze` on the filtered history
  showed the largest object is a 60 KB launcher PNG; the committed watchface
  APK assets are ~33 KB each. Nothing large enough to warrant history
  surgery.

## Secret scan

`gitleaks git` (v8.30.0) was run over **all refs** of the filtered clone
before anything was pushed: 233 commits, ~5.9 MB scanned, **no leaks found**.

## Reconciliation with the repository scaffold

The filtered history was merged into the bootstrap scaffold on `develop`
with `git merge --allow-unrelated-histories`. Overlapping files:

- `LICENSE` -- byte-identical GPL-3.0 on both sides.
- `.gitignore` -- scaffold version kept (a verified superset of the mobile
  `.gitignore`, including the `!gradle/wrapper/gradle-wrapper.jar` negation).
- `README.md` -- scaffold version kept (written for this repo's layout); a
  note about the root `shell.nix` toolchain was folded in.

## Post-merge build changes

- `settings.gradle.kts`: the three plugin `projectDir` overrides were
  rewritten from `file("../../plugins/...")` (which escaped the monorepo
  `apps/mobile` root) to `file("plugins/...")` under this repo's root.
- `shell.nix`: added `pkgs.zip` to `buildInputs` -- the watchface re-sign
  task shells out to `zip`, previously an undeclared host dependency.
- `scripts/mobile-dev.sh`: `MOBILE_DIR` repointed from `apps/mobile` to the
  repo root; help text updated.
