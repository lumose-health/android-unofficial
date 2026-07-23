// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Josh Engelbrecht
package com.glycemicgpt.mobile.presentation.licenses

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Drift gate for the in-app license viewer.
 *
 * The screen renders assets, not source-embedded strings, and `generateLicenseAssets`
 * regenerates those assets from the repository's documents on every build. These tests read
 * the assets back out of the built variant and compare them against the documents on disk, so
 * a licensing edit that never reaches the app -- or an asset that stops being packaged at all
 * -- fails the suite instead of shipping. The unit-test task declares the three documents as
 * inputs (see `app/build.gradle.kts`), so editing one re-runs the gate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LicenseAssetsTest {

    private val assets = ApplicationProvider.getApplicationContext<Application>().assets

    private fun asset(path: String) = LicenseDocuments.read(assets, path)

    /** Repo-root-relative: Gradle runs unit tests with the module directory as the working dir. */
    private fun repoFile(path: String) = File("../$path").readText()

    /**
     * Parses the license-text asset into identifier -> body. Each section is a separator line, the
     * identifier, a separator line, then the text up to the next separator, so splitting on the
     * separator yields the preamble followed by alternating identifier/body tokens.
     */
    private fun licenseBodies(texts: String): Map<String, String> {
        val tokens = texts.split(Regex("\\n={10,}\\n")).map { it.trim() }
        return (1 until tokens.size step 2).associate { i ->
            tokens[i] to tokens.getOrElse(i + 1) { "" }
        }
    }

    /** The lines of a single component's entry in the component list, heading excluded. */
    private fun componentEntry(components: String, coordinates: String): String {
        val lines = components.lines()
        val start = lines.indexOfFirst { it.startsWith("- `$coordinates") }
        assertTrue("Component $coordinates is not in the list", start >= 0)
        val rest = lines.drop(start + 1).takeWhile { !it.startsWith("- ") && !it.startsWith("## ") }
        return (listOf(lines[start]) + rest).joinToString("\n")
    }

    @Test
    fun `full license asset is the repository LICENSE verbatim`() {
        assertEquals(repoFile("LICENSE"), asset(LicenseDocuments.FULL_LICENSE))
    }

    @Test
    fun `third-party asset is the repository attributions document verbatim`() {
        assertEquals(
            repoFile("docs/THIRD_PARTY_LICENSES.md"),
            asset(LicenseDocuments.THIRD_PARTY),
        )
    }

    /**
     * The equality tests above compare an asset against the file it was copied from in the same
     * build, so they hold just as well when that file is empty -- and an empty asset renders as
     * a blank section rather than any kind of error. These pin content the documents cannot lose
     * while still being the documents they claim to be.
     */
    @Test
    fun `each asset carries the anchors that make it the document it claims to be`() {
        val fullLicense = asset(LicenseDocuments.FULL_LICENSE)
        assertTrue(fullLicense.contains("GNU GENERAL PUBLIC LICENSE"))
        assertTrue(fullLicense.contains("Version 3, 29 June 2007"))
        assertTrue("GPL-3.0 is ~35 KB; a shorter one is truncated", fullLicense.length > 30_000)

        val thirdParty = asset(LicenseDocuments.THIRD_PARTY)
        assertTrue(thirdParty.contains("# Third-Party Licenses"))
        assertTrue("Attributions document is truncated", thirdParty.length > 10_000)

        assertTrue(asset(LicenseDocuments.PROJECT_NOTICE).length > 200)
    }

    /**
     * The redistributed-component assets have no repository document to compare against -- they
     * are generated from the resolved dependency graph. What can be pinned is that the graph
     * reached the asset: the components this app is built from must appear in it by name. A
     * generator that silently produced an empty or partial document fails here.
     */
    @Test
    fun `redistributed components asset lists what this build actually ships`() {
        val components = asset(LicenseDocuments.RUNTIME_DEPENDENCIES)
        assertTrue(components.startsWith("# Redistributed Components"))
        listOf(
            "com.squareup.okhttp3:okhttp",
            "com.squareup.retrofit2:retrofit",
            "androidx.room:room-runtime",
            "com.jakewharton.timber:timber",
            "io.sentry:sentry-android",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core",
        ).forEach { coordinates ->
            assertTrue(
                "$coordinates ships in the APK but is missing from the attribution",
                components.contains(coordinates),
            )
        }
        // A wear-only component: it is on :wear-device's runtime graph and not :app's, so its
        // presence proves the attribution spans every module, not only :app. Five of the six
        // reproduced NOTICE files are reachable only through that module.
        assertTrue(
            "Wear-only components are missing; the attribution covers only :app",
            components.contains("androidx.wear.watchface:watchface-push"),
        )
    }

    @Test
    fun `licence text asset carries the terms and notices those components ship under`() {
        val texts = asset(LicenseDocuments.RUNTIME_DEPENDENCY_LICENSES)
        assertTrue(texts.contains("Apache License"))
        assertTrue(texts.contains("Version 2.0, January 2004"))
        // Structural: the notices section exists and holds at least one per-component block,
        // rather than pinning an incidental transitive that a dependency bump could remove.
        val notices = texts.substringAfter("UPSTREAM NOTICES", "")
        assertTrue("Apache-2.0 section 4(d) notices are not reproduced", notices.isNotBlank())
        assertTrue("No per-component notice block under UPSTREAM NOTICES", notices.contains("\n--- "))
        // jakarta.inject-api is pulled in by Hilt (a core dependency) and ships a NOTICE, so it
        // is a stable witness that a real notice is reproduced beneath its heading.
        assertTrue(notices.contains("jakarta.inject:jakarta.inject-api"))
    }

    /**
     * The two components that declare no SPDX license and ship no license text in their artifact
     * (BouncyCastle, java-cup) are the ones the section 4(a) "shipped without its terms" gap most
     * threatens. Their canonical text is bundled under docs/licenses and must be reproduced;
     * proprietary-SDK components (Play services) are instead noted with where their terms live.
     */
    @Test
    fun `non-SPDX components are attributed with a bundled text or an external-terms note`() {
        val components = asset(LicenseDocuments.RUNTIME_DEPENDENCIES)
        val texts = asset(LicenseDocuments.RUNTIME_DEPENDENCY_LICENSES)
        assertTrue(components.contains("org.bouncycastle:bcprov-jdk18on"))
        assertTrue("BouncyCastle license text is empty", licenseBodies(texts)["BouncyCastle"].orEmpty().length > 200)
        assertTrue(components.contains("edu.princeton.cup:java-cup"))
        assertTrue("CUP license text is empty", licenseBodies(texts)["CUP"].orEmpty().length > 200)
        // A specific external-terms component, not just the group heading: its own entry must
        // carry the note, so the heading cannot be present for some other component while this
        // one is unannotated.
        val playServices = componentEntry(components, "com.google.android.gms:play-services-base")
        assertTrue(
            "play-services-base is not noted as externally-licensed",
            playServices.contains("Terms: Android Software Development Kit License"),
        )
    }

    /**
     * Every license family a redistributed component is grouped under must resolve to a real,
     * non-empty text section (or be a known non-SPDX/external category), and every SPDX family
     * this project bundles a text for must be present. A component shipped under a license whose
     * text was never bundled -- or bundled empty -- is exactly the section 4(a) gap this
     * attribution exists to close, and it would be invisible on the screen.
     */
    @Test
    fun `every licence family in the component list resolves to real bundled text`() {
        val texts = asset(LicenseDocuments.RUNTIME_DEPENDENCY_LICENSES)
        val bodies = licenseBodies(texts)

        // Each SPDX family this project bundles a text for is present with a substantive body.
        SPDX_FAMILIES_WITH_TEXT.forEach { identifier ->
            val body = bodies[identifier]
            assertTrue("No license text section for $identifier", body != null)
            assertTrue("License text for $identifier is empty or truncated", body!!.length > 200)
        }

        // Every heading in the component list is an expected category: an SPDX family with text,
        // a known non-SPDX license bundled by name, or the external-terms group. A new, unbundled
        // family (say MPL-2.0) would surface here rather than pass silently.
        val headingFamilies = asset(LicenseDocuments.RUNTIME_DEPENDENCIES)
            .lines()
            .filter { it.startsWith("## ") }
            .map { it.removePrefix("## ").trim() }
            .toSet()
        assertTrue("No license families were found in the component list", headingFamilies.isNotEmpty())
        val unexpected = headingFamilies - SPDX_FAMILIES_WITH_TEXT - NON_SPDX_HEADINGS
        assertTrue("Unexpected license family with no bundled text: $unexpected", unexpected.isEmpty())
        // Every SPDX-family heading resolves to a non-empty text section.
        headingFamilies.filter { it in SPDX_FAMILIES_WITH_TEXT }.forEach { family ->
            assertTrue("Component grouped under $family but its text is empty", bodies[family].orEmpty().length > 200)
        }
    }

    @Test
    fun `component sections split into one item per licence family`() {
        val components = asset(LicenseDocuments.RUNTIME_DEPENDENCIES)
        val sections = splitIntoSections(components)
        // The preamble plus one section per family, and nothing dropped in the split.
        assertEquals(
            components.lines().count { it.startsWith("## ") } + 1,
            sections.size,
        )
        assertTrue(sections.first().startsWith("# Redistributed Components"))
        sections.drop(1).forEach { assertTrue(it.startsWith("## ")) }
    }

    @Test
    fun `project notice is the README License section verbatim`() {
        val notice = asset(LicenseDocuments.PROJECT_NOTICE)
        assertTrue(
            "Project notice is not a verbatim excerpt of README.md",
            repoFile("README.md").contains(notice.trimEnd()),
        )
        assertTrue(notice.startsWith("## License"))
    }

    @Test
    fun `project notice stops at the next README section`() {
        // Its own heading is line 1; any later one means the extraction over-captured and the
        // screen is showing unrelated README prose as the project's licensing notice.
        val afterOwnHeading = asset(LicenseDocuments.PROJECT_NOTICE).substringAfter('\n')
        assertFalse(afterOwnHeading.contains("\n## "))
    }

    @Test
    fun `project notice carries the copyright holder and SPDX identifier`() {
        val notice = asset(LicenseDocuments.PROJECT_NOTICE)
        assertTrue(notice.contains("Copyright (C) 2026 Josh Engelbrecht"))
        assertTrue(notice.contains("GPL-3.0-only"))
    }

    private companion object {
        /**
         * SPDX families present in the release graph, each with a canonical text under
         * `docs/licenses`. A dependency bump that introduces a new family fails
         * `generateDependencyAttribution` before it reaches this test; this set keeps the
         * expectation visible in the suite as well.
         */
        val SPDX_FAMILIES_WITH_TEXT = setOf(
            "Apache-2.0",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "ICU",
            "MIT",
            "SAX-PD",
            "SAX-PD-2.0",
        )

        /**
         * Component-list headings that are not SPDX families: licenses bundled by name because
         * their POM declares no SPDX identifier, and the group for components whose terms are
         * noted rather than reproduced (proprietary SDKs, and licenses reproduced elsewhere).
         */
        val NON_SPDX_HEADINGS = setOf(
            "BouncyCastle",
            "CUP",
            "License terms available elsewhere",
        )
    }
}
