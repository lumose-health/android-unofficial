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
}
