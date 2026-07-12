---
title: Signing and Secrets
description: How Android signing material is stored in 1Password and loaded into GitHub Actions at runtime, so no keystore or password is ever kept in GitHub.
---

# Signing and Secrets

This repository never stores signing material in GitHub. The release and debug
keystores, their passwords, and the key aliases all live in **1Password**, and
CI fetches them at runtime with a read-only service-account token. The reusable
building block that does this is the composite action
[`.github/actions/op-load-signing-secrets`](https://github.com/GlycemicGPT/glycemicgpt-android-unofficial/blob/develop/.github/actions/op-load-signing-secrets/action.yml),
which the signed-release and `dev-latest` debug-signed build jobs consume.

## The model

- **Signing material lives in 1Password** -- vault `github`, item
  `android-signing`. This is the single source of truth for both the release and
  debug signing identities.
- **The only GitHub secret is `ANDROID_ACTIONS_SERVICE_ACCOUNT`** -- a read-only
  1Password service-account token scoped to that vault. Nothing else
  signing-related is stored in GitHub. Callers map it into the variable the
  1Password tooling reads:

  ```yaml
  env:
    OP_SERVICE_ACCOUNT_TOKEN: ${{ secrets.ANDROID_ACTIONS_SERVICE_ACCOUNT }}
  ```

- If the token leaks, it can only *read* the `android-signing` item; it cannot
  write to 1Password or reach any other vault. Rotating it is a one-line change
  to a single GitHub secret, with no keystore re-issuance.

Creating the service account, minting its read-only token, and setting the
`ANDROID_ACTIONS_SERVICE_ACCOUNT` secret are one-time maintainer tasks covered by
the maintainer signing runbook (kept out of this repository, alongside the
signing identity itself). This page documents only how CI *consumes* what that
runbook provisions.

## Item layout: file attachments vs text fields

The `android-signing` item holds two kinds of value, and each kind is loaded a
different way:

| Kind | Names | How CI loads it |
|------|-------|-----------------|
| **File attachments** | `release.jks`, `debug.keystore` | `op read --out-file` (the op CLI) |
| **Text fields** | `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, `DEBUG_KEYSTORE_PASSWORD`, `DEBUG_KEY_ALIAS`, `DEBUG_KEY_PASSWORD` | `1password/load-secrets-action` |
| **Canary** (non-sensitive) | `canary` = `"ok"` | plumbing check only (see below) |

Each value's `op://` reference is `op://github/android-signing/<name>` -- for
example `op://github/android-signing/release.jks` or
`op://github/android-signing/RELEASE_KEY_ALIAS`.

### Why two mechanisms

`1password/load-secrets-action` resolves `op://` references that point at
**text fields** and exports them as masked environment variables -- ideal for
passwords and aliases. It **cannot** resolve a **file attachment**. The keystores
are binary file attachments, so they are materialized separately with the op CLI
(`op read op://github/android-signing/release.jks --out-file ...`), which writes
the bytes straight to disk without ever passing them through a log or an
environment variable.

## The env-var contract

`app/build.gradle.kts` reads the signing configuration entirely from the
environment. The composite action exports exactly the names it expects, so the
consuming job needs no renaming:

| `keystore` input | Env vars exported | `signingConfigs` field |
|------------------|-------------------|------------------------|
| `release` | `RELEASE_KEYSTORE_FILE` | `storeFile` |
| | `RELEASE_KEYSTORE_PASSWORD` | `storePassword` |
| | `RELEASE_KEY_ALIAS` | `keyAlias` |
| | `RELEASE_KEY_PASSWORD` | `keyPassword` |
| `debug` | `DEBUG_KEYSTORE_FILE` | `storeFile` |
| | `DEBUG_KEYSTORE_PASSWORD` | `storePassword` |
| | `DEBUG_KEY_ALIAS` | `keyAlias` |
| | `DEBUG_KEY_PASSWORD` | `keyPassword` |

The password/alias names in 1Password already match the gradle env-var names, so
they are exported 1:1. The `*_KEYSTORE_FILE` name is the one thing the action
synthesizes: it points at the keystore materialized in `$RUNNER_TEMP`, and is
also returned as the `keystore-path` output so a later step can reference the
file directly -- for example, the `if: always()` cleanup step that removes it.

## Consuming the action

A signing job maps the token, then calls the composite action once with
`keystore: release` (or `debug`) before the Gradle build:

```yaml
jobs:
  build-release:
    runs-on: ubuntu-latest
    # Least privilege: this job only needs to read the repo to check out and build.
    # Bump to `contents: write` ONLY if the job itself creates a GitHub Release.
    permissions:
      contents: read
    env:
      # Map the only GitHub secret into the variable 1Password tooling reads.
      OP_SERVICE_ACCOUNT_TOKEN: ${{ secrets.ANDROID_ACTIONS_SERVICE_ACCOUNT }}
    steps:
      - uses: actions/checkout@9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0 # v7.0.0
        with:
          persist-credentials: false

      - name: Load release signing secrets from 1Password
        id: signing
        uses: ./.github/actions/op-load-signing-secrets
        with:
          keystore: release

      # RELEASE_KEYSTORE_FILE / _PASSWORD, RELEASE_KEY_ALIAS / _PASSWORD are now in
      # the (masked) job environment; Gradle's signingConfigs read them directly.
      - name: Build signed release
        run: ./gradlew assembleRelease

      # $RUNNER_TEMP is wiped with the ephemeral runner, but on self-hosted
      # runners delete the materialized keystore explicitly.
      - name: Remove materialized keystore
        if: always()
        run: rm -f "${{ steps.signing.outputs.keystore-path }}"
```

The debug variant is identical with `keystore: debug`; it exports the
`DEBUG_*` names for the shared-debug-keystore build.

The job declares `permissions: contents: read` -- least privilege for a build
that only checks out and compiles. Raise it to `contents: write` only when the
job itself creates a GitHub Release (for example, attaching the signed APK as a
release asset); a build-and-sign job that merely uploads a workflow artifact does
not need write access.

## Verifying the plumbing

[`.github/workflows/secrets-plumbing-check.yml`](https://github.com/GlycemicGPT/glycemicgpt-android-unofficial/blob/develop/.github/workflows/secrets-plumbing-check.yml)
is a `workflow_dispatch` smoke test that authenticates with the service-account
token and resolves **only** the non-sensitive `canary` field. It never touches a
keystore or password. Run it after rotating the token, or to confirm the
1Password -> Actions path before the signing jobs depend on it. A green run prints
`1Password plumbing OK`.

Note: GitHub only exposes the **Run workflow** control (and the `gh workflow run`
API) once a `workflow_dispatch` workflow reaches the repo's default branch
(`main`), so this check becomes dispatchable after `develop` is promoted, not
while it lives only on `develop`.

## Guarantees

- No keystore, password, alias, or token byte exists anywhere in this repository
  -- only `op://` references and the wiring around them.
- Keystores are materialized to `$RUNNER_TEMP` with `chmod 600` and are never
  echoed; passwords and aliases live only in masked environment variables that
  `load-secrets-action` registers with the runner's secret masker.
- Every third-party action is pinned to a commit SHA with a version comment.
