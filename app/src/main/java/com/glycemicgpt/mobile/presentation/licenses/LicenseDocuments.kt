// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Josh Engelbrecht
package com.glycemicgpt.mobile.presentation.licenses

import android.content.res.AssetManager

/**
 * The license documents bundled into the APK by the `generateLicenseAssets` Gradle task.
 *
 * Each is an exact copy of its source -- `LICENSE`, `docs/THIRD_PARTY_LICENSES.md`, and the
 * `## License` section excerpted from `README.md` -- regenerated on every build, so the viewer
 * always shows what the repository actually says. They ship inside the APK, which is what
 * makes the screen readable with no connectivity.
 */
internal object LicenseDocuments {
    const val PROJECT_NOTICE = "licenses/project_license_notice.md"
    const val THIRD_PARTY = "licenses/third_party_licenses.md"
    const val FULL_LICENSE = "licenses/gpl-3.0.txt"

    fun read(assets: AssetManager, path: String): String =
        assets.open(path).bufferedReader().use { it.readText() }
}

/**
 * Reduces links whose target is a repository path -- `[LICENSE](../LICENSE)` -- to their label
 * text. Such a path addresses a file that does not exist on the device, so the link can only
 * render as an affordance that does nothing when tapped.
 *
 * Targets carrying a URI scheme are left alone even when
 * [com.glycemicgpt.mobile.presentation.common.AppMarkdownText] declines to open them: a
 * `mailto:` address is information a reader may need out of a licensing document, and removing
 * the target would delete it from the page entirely. Image syntax is left for that composable's
 * own sanitizer to remove.
 */
internal fun stripUnresolvableLinks(markdown: String): String =
    markdown.replace(REPOSITORY_RELATIVE_LINK, "$1")

private val REPOSITORY_RELATIVE_LINK =
    Regex("""(?<!!)\[([^\]]+)]\((?![a-zA-Z][a-zA-Z0-9+.\-]*:)[^)]*\)""")
