---
title: SPDX Header Policy
description: The SPDX identifier and header form for this repository, which files are in scope, and the upstream-derived trees a header sweep must never stamp.
---

# SPDX Header Policy

SPDX license headers in this repository use `GPL-3.0-only`. This policy defines the
header form, which files are in scope, and which trees must never be stamped. The
sweep that applies headers is separate work, executed against this document.

Per-file headers are how this repository expresses its license grant in source.
`LICENSE` itself is never edited: the GNU GPL is a verbatim document ("changing it is
not allowed"), and the "How to Apply These Terms" appendix at its end is an
instruction to attach a notice to source files, not a template to fill in inside the
license document. `README.md` carries the program-level copyright notice; source
files carry the block below.

## The identifier

```text
GPL-3.0-only
```

Not `GPL-3.0-or-later`, and not the deprecated bare `GPL-3.0`. This repository is
distributed under version 3 of the GNU General Public License and does not extend
the "or any later version" option to downstream recipients. The full license text is
at [`LICENSE`](../../LICENSE).

The copyright holder is the individual, not the GitHub organization:

```text
Copyright (C) 2026 Josh Engelbrecht
```

"Lumose Health" and "GlycemicGPT" are organization and project names, not legal
entities, and must never appear as the copyright holder in a notice.

## The header form

This is the canonical notice block. Stamp it verbatim -- do not paraphrase it, add to
it, or regenerate it per file. For Kotlin, Java, and Gradle Kotlin DSL files it is the
first thing in the file, above the `package` declaration:

```kotlin
// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Josh Engelbrecht
```

The same two lines, with `#` comment markers, are the canonical block for shell
scripts and YAML:

```sh
# SPDX-License-Identifier: GPL-3.0-only
# Copyright (C) 2026 Josh Engelbrecht
```

For shell scripts the block goes immediately after the shebang; for YAML, at the top
of the file. The identifier line must match byte for byte -- the verification commands
below match on it literally.

A file that already opens with a descriptive block comment keeps that comment; the
SPDX lines go above it, not instead of it. Nothing in this policy authorizes
deleting, reordering, or rewording an existing header.

## In scope

Stamp exactly these globs:

```text
app/src/**/*.kt
wear-device/src/**/*.kt
watchface/src/**/*.kt
plugins/pump-driver-api/src/**/*.kt
plugins/example/src/**/*.kt
plugins/shipped/tandem/src/**/*.kt
build.gradle.kts
settings.gradle.kts
app/build.gradle.kts
wear-device/build.gradle.kts
watchface/build.gradle.kts
plugins/pump-driver-api/build.gradle.kts
plugins/example/build.gradle.kts
plugins/shipped/tandem/build.gradle.kts
scripts/*.sh
.github/workflows/*.yml
```

**Where an in-scope glob and an excluded tree both match a file, the exclusion
wins.** The Gradle build files are listed individually rather than as
`**/*.gradle.kts` for exactly this reason: a recursive glob would otherwise reach
`plugins/shipped/medtronic/build.gradle.kts` and the spike's build files, all of
which are excluded.

**Anything not matched by an in-scope glob is not stamped.** That is the whole rule.
It is deliberately conservative: resource XML, `AndroidManifest.xml`, ProGuard rules,
`gradle.properties`, `shell.nix`, `renovate.json5`, `osv-scanner.toml`, `zizmor.yml`,
`gradle/libs.versions.toml`, `contract/openapi.json`, `.githooks/*`, lockfiles,
generated output under any `build/` directory, binary assets, and Markdown are all
out of scope. Prose and config carry their license by way of the repository
`LICENSE`, not a per-file header.

Note that the three in-scope globs under `plugins/` name `pump-driver-api`,
`example`, and `shipped/tandem` individually, so `plugins/shipped/medtronic/` is
skipped by construction -- see below.

## Never stamp the GPL header here

Some code here is derived from upstream projects that carry their own copyright and
license terms. Adding a GlycemicGPT copyright header to a file whose provenance is
upstream overstates authorship: it is an attribution defect, not housekeeping.

"Not the GPL header" does not always mean "no header". One file in this section --
`EcJpake.kt` -- must receive an **Apache-2.0** header instead. Read that subsection
before treating this whole section as a skip list.

The list below is normative. **A pull request that adds an upstream-derived file must
add it to this list in the same pull request.** If a file is not listed and you are
still unsure, the objective test is:

> A file is excluded if upstream code was **copied or ported into it**. A file is in
> scope if upstream work was only **studied** and reimplemented.

Citing an upstream project is not by itself grounds for exclusion, and neither is
naming an upstream file as a reference. Several Tandem files cite `jwoglom/pumpX2`
as a protocol or test-vector reference -- `ble/protocol/TandemProtocol.kt`,
`ble/auth/JpakeAuthenticator.kt`, `ble/messages/StatusResponseParser.kt`,
`ble/crypto/Hkdf.kt`, and `ble/crypto/HmacSha256Util.kt`. All five are original
implementations written from published protocol research, import no upstream code,
and **are in scope**. Contrast `ble/crypto/EcJpake.kt`, which says "Ported from" --
that is a port, and it is excluded. Keep the reference comments intact either way.

When the test and the list disagree, treat the file as excluded and fix the list.

### The OpenMinimed-derived Medtronic driver

```text
plugins/shipped/medtronic/**
```

The whole module tree, main and test sources alike. The Medtronic MiniMed 700-series
read-only driver is a direct port of OpenMinimed's readers into Kotlin, not an
independent reimplementation, and its in-source headers carry upstream copyright and
cite the specific upstream file each port derives from. Those headers are the record
of a relicensing granted specifically to this project.

The exclusion is stated at directory level deliberately. A few files under `di/` and
`plugin/` in this module are original work, but drawing the line file-by-file inside
a ported module invites exactly the misattribution this policy exists to prevent.
Skip the module.

Full attribution record:
[`docs/THIRD_PARTY_LICENSES.md`](../THIRD_PARTY_LICENSES.md#medtronic-minimed-ble-protocol-implementation-openminimed),
which is authoritative if it and this policy disagree.

### The Particle-derived EC-JPAKE implementation (Apache-2.0, stamp required)

```text
plugins/shipped/tandem/src/main/java/com/glycemicgpt/mobile/ble/crypto/EcJpake.kt
```

Ported from `io.particle.crypto.EcJpake`, Apache-2.0, Copyright 2022 Particle
Industries, Inc. Stamping it `GPL-3.0-only` would assert a license its upstream
author did not grant it under.

This file is an **open attribution gap, not a settled exclusion.** Its current header
records the port in prose but carries no copyright notice and no license identifier,
while the Apache-2.0 notice lives only in `docs/THIRD_PARTY_LICENSES.md`. Apache-2.0
requires the notice travel with the file. The sweep must therefore not skip this file
silently -- it must give it the correct header:

```kotlin
// SPDX-License-Identifier: Apache-2.0
// Copyright 2022 Particle Industries, Inc.
```

This is the only file under `plugins/shipped/tandem/` that is itself a port. Its
siblings that cite `jwoglom/pumpX2` do so as a protocol or test-vector reference, not
as a source they were copied from; per the test above those are references, and those
files are in scope. The Tandem implementation as a whole is original work informed by
studying MIT-licensed protocol documentation, with no upstream code imported.

### The spike harness

```text
tools/medtronic-ble-spike/**
```

A standalone throwaway Gradle project with its own `LICENSE`, not part of any shipped
artifact, and partly derived from OpenMinimed work. Out of scope for the sweep
entirely.

### Vendored Gradle wrappers

```text
gradlew
gradlew.bat
tools/medtronic-ble-spike/gradlew
tools/medtronic-ble-spike/gradlew.bat
gradle/wrapper/**
```

Gradle's own files, Apache-2.0. Leave them exactly as they are. `gradlew` and the
spike's `gradlew` and `gradlew.bat` already carry
`SPDX-License-Identifier: Apache-2.0`; the rest carry the Apache-2.0 notice without
an identifier line. Do not add one.

## Verifying a sweep

All six must hold when the sweep is done. Set `SWEEP_BASE` and `SWEEP_HEAD` to the
revisions the sweep itself spans, so unrelated commits are not attributed to it.

Judge these checks by their **output**, not their exit status: `grep -L` and `grep -l`
exit non-zero when nothing matches, which is the passing case here. Each command below
ends in `|| true` so a passing check does not abort a `set -e` script.

Every GPL-scoped Kotlin file carries the header. `EcJpake.kt` is filtered out on
purpose: it is in-tree but Apache-2.0, and is checked separately below. Must print
nothing:

```sh
grep -rL 'SPDX-License-Identifier: GPL-3.0-only' \
  --include='*.kt' \
  app/src wear-device/src watchface/src \
  plugins/pump-driver-api/src plugins/example/src plugins/shipped/tandem/src \
  | grep -v 'ble/crypto/EcJpake.kt' || true
```

Every in-scope non-Kotlin file carries the header too. The Gradle files are listed
explicitly rather than globbed, so the excluded medtronic and spike build files are
never selected (must print nothing):

```sh
grep -L 'SPDX-License-Identifier: GPL-3.0-only' $(
  git ls-files \
    'build.gradle.kts' 'settings.gradle.kts' \
    'app/build.gradle.kts' 'wear-device/build.gradle.kts' \
    'watchface/build.gradle.kts' \
    'plugins/pump-driver-api/build.gradle.kts' \
    'plugins/example/build.gradle.kts' \
    'plugins/shipped/tandem/build.gradle.kts' \
    'scripts/*.sh' '.github/workflows/*.yml'
) || true
```

No excluded tree was stamped (must print nothing):

```sh
grep -rl 'GPL-3.0-only' plugins/shipped/medtronic tools/medtronic-ble-spike || true
```

`EcJpake.kt` carries the Apache-2.0 header, not the GPL one (must print
`SPDX-License-Identifier: Apache-2.0`):

```sh
grep -o 'SPDX-License-Identifier: [A-Za-z0-9.-]*' \
  plugins/shipped/tandem/src/main/java/com/glycemicgpt/mobile/ble/crypto/EcJpake.kt
```

The sweep did not touch `LICENSE` (must print nothing):

```sh
git diff --name-only "$SWEEP_BASE".."$SWEEP_HEAD" -- LICENSE
```

That check only proves the sweep left the file alone. Confirm license detection
itself separately -- it must report exactly one license, `GPL-3.0`:

```sh
gh api "repos/lumose-health/android-unofficial/license?ref=$SWEEP_HEAD" \
  --jq '{spdx: .license.spdx_id, path: .path}'
```
