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
        assertTrue("BouncyCastle license text is not bundled", texts.contains("\nBouncyCastle\n"))
        assertTrue(components.contains("edu.princeton.cup:java-cup"))
        assertTrue("CUP license text is not bundled", texts.contains("\nCUP\n"))
        assertTrue(
            "Play services should be noted as externally-licensed, not dropped",
            components.contains("License terms available elsewhere"),
        )
    }

    /**
     * Every SPDX licence a redistributed component declares must have its full text in the text
     * asset. A component shipped under a licence whose text was never bundled is exactly the
     * section 4(a) gap this attribution exists to close, and it would be invisible on the screen.
     *
     * The generator itself fails the build when an SPDX identifier has no bundled text, so this
     * keeps the expectation visible in the suite and pins that each identifier's own delimited
     * section is present rather than merely mentioned somewhere.
     */
    @Test
    fun `every SPDX licence a component declares has its text bundled`() {
        val texts = asset(LicenseDocuments.RUNTIME_DEPENDENCY_LICENSES)
        SPDX_FAMILIES_WITH_TEXT.forEach { identifier ->
            assertTrue("No licence text bundled for $identifier", texts.contains("\n$identifier\n"))
        }
        // The families that head the component list are the primary licence of each component;
        // every SPDX one among them is part of the set whose text is asserted above.
        val headingFamilies = asset(LicenseDocuments.RUNTIME_DEPENDENCIES)
            .lines()
            .filter { it.startsWith("## ") }
            .map { it.removePrefix("## ").trim() }
        assertTrue("No licence families were found in the component list", headingFamilies.isNotEmpty())
        headingFamilies.filter { it in SPDX_FAMILIES_WITH_TEXT }.forEach { family ->
            assertTrue("Component grouped under $family but its text is absent", texts.contains("\n$family\n"))
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
            "EPL-1.0",
            "ICU",
            "MIT",
            "SAX-PD",
            "SAX-PD-2.0",
        )
    }
}
