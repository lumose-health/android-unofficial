package com.glycemicgpt.mobile.data.update

import com.squareup.moshi.Moshi
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearAppUpdateCheckerTest {

    // The wear checker self-updates from the standalone Android repository.

    @Test
    fun `wear release URLs target the android-only repository`() {
        assertTrue(WearAppUpdateChecker.STABLE_RELEASES_URL.startsWith("https://api.github.com/"))
        assertTrue(WearAppUpdateChecker.DEV_RELEASES_URL.startsWith("https://api.github.com/"))
        assertTrue(
            WearAppUpdateChecker.STABLE_RELEASES_URL
                .contains("/repos/GlycemicGPT/glycemicgpt-android-unofficial/"),
        )
        assertTrue(
            WearAppUpdateChecker.DEV_RELEASES_URL
                .contains("/repos/GlycemicGPT/glycemicgpt-android-unofficial/"),
        )
        assertFalse(WearAppUpdateChecker.STABLE_RELEASES_URL.contains("/GlycemicGPT/GlycemicGPT/"))
        assertFalse(WearAppUpdateChecker.DEV_RELEASES_URL.contains("/GlycemicGPT/GlycemicGPT/"))
    }

    @Test
    fun `stable channel discriminates the renamed wear release asset`() {
        // WearAppUpdateChecker.check() selects on WEAR_APK_PREFIX ("GlycemicGPT-Wear-")
        // plus the "-release.apk" suffix. The phone release asset shares the
        // "GlycemicGPT-" prefix but not "GlycemicGPT-Wear-", so the wear selector must
        // reject it; the dev-suffixed wear name must also be rejected on the stable channel.
        fun matchesWearStable(name: String) =
            name.startsWith("GlycemicGPT-Wear-") && name.endsWith("-release.apk")

        assertTrue(matchesWearStable("GlycemicGPT-Wear-0.13.0-release.apk"))
        assertFalse(matchesWearStable("GlycemicGPT-0.13.0-release.apk"))
        assertFalse(matchesWearStable("GlycemicGPT-Wear-0.13.0-dev.42-debug.apk"))
    }

    @Test
    fun `parseDevRunNumber extracts number from wear APK filename`() {
        val name = "GlycemicGPT-Wear-0.1.99-dev.42-debug.apk"
        assertEquals(42, AppUpdateChecker.parseDevRunNumber(name))
    }

    @Test
    fun `parseDevRunNumber returns 0 for non-matching filename`() {
        val name = "GlycemicGPT-Wear-0.1.99-release.apk"
        assertEquals(0, AppUpdateChecker.parseDevRunNumber(name))
    }

    @Test
    fun `parseVersionCode computes correct code from version string`() {
        assertEquals(1_000_000, AppUpdateChecker.parseVersionCode("1.0.0"))
        assertEquals(10_099, AppUpdateChecker.parseVersionCode("0.1.99"))
        assertEquals(2_030_005, AppUpdateChecker.parseVersionCode("2.3.5"))
    }

    @Test
    fun `isAllowedDownloadHost accepts github domains`() {
        assertTrue(
            AppUpdateChecker.isAllowedDownloadHost(
                "https://github.com/GlycemicGPT/glycemicgpt-android-unofficial/releases/download/v1.0.0/test.apk",
            ),
        )
        assertTrue(
            AppUpdateChecker.isAllowedDownloadHost(
                "https://objects.githubusercontent.com/path/to/file",
            ),
        )
    }

    @Test
    fun `isAllowedDownloadHost rejects untrusted domains`() {
        assertTrue(
            !AppUpdateChecker.isAllowedDownloadHost("https://evil.com/malware.apk"),
        )
    }

    @Test
    fun `an https URL to an allowed host passes both wear download guards`() {
        val url =
            "https://github.com/GlycemicGPT/glycemicgpt-android-unofficial/releases/download/v1.0/wear.apk"
        assertTrue(AppUpdateChecker.isHttpsUrl(url))
        assertTrue(AppUpdateChecker.isAllowedDownloadHost(url))
    }

    @Test
    fun `downloadWearApk rejects an insecure http URL even to an allowed host`() = runTest {
        val checker = WearAppUpdateChecker(mockk(relaxed = true), Moshi.Builder().build())
        val result = checker.downloadWearApk("http://github.com/x/wear.apk", "wear.apk", 0L)
        assertTrue(result is DownloadResult.Error)
        assertEquals("Download blocked: insecure URL", (result as DownloadResult.Error).message)
    }

    @Test
    fun `sanitizeFileName removes special characters`() {
        assertEquals(
            "GlycemicGPT-Wear-0.1.99-dev.42-debug.apk",
            AppUpdateChecker.sanitizeFileName("GlycemicGPT-Wear-0.1.99-dev.42-debug.apk"),
        )
        assertEquals(
            "file_with_spaces_.apk",
            AppUpdateChecker.sanitizeFileName("file with spaces .apk"),
        )
    }

    @Test
    fun `sanitizeFileName strips query and fragment`() {
        assertEquals(
            "test.apk",
            AppUpdateChecker.sanitizeFileName("test.apk?token=abc#section"),
        )
    }

    @Test
    fun `wear APK prefix matching picks correct asset`() {
        val wearPrefix = "GlycemicGPT-Wear-"
        val phonePrefix = "GlycemicGPT-"
        val assets = listOf(
            "GlycemicGPT-0.1.99-dev.42-debug.apk",
            "GlycemicGPT-Wear-0.1.99-dev.42-debug.apk",
        )

        val wearAsset = assets.firstOrNull {
            it.startsWith(wearPrefix) && it.endsWith("-debug.apk")
        }
        val phoneAsset = assets.firstOrNull {
            it.startsWith(phonePrefix) && !it.startsWith(wearPrefix) && it.endsWith("-debug.apk")
        }

        assertEquals("GlycemicGPT-Wear-0.1.99-dev.42-debug.apk", wearAsset)
        assertEquals("GlycemicGPT-0.1.99-dev.42-debug.apk", phoneAsset)
    }

    @Test
    fun `version comparison dev channel uses run number not version code`() {
        // Dev channel: remote run 50 > local run 42 -> update available
        val remoteRun = AppUpdateChecker.parseDevRunNumber("GlycemicGPT-Wear-0.1.99-dev.50-debug.apk")
        val localRun = 42
        assertTrue(remoteRun > localRun)

        // Dev channel: remote run 42 <= local run 42 -> up to date
        val sameRun = AppUpdateChecker.parseDevRunNumber("GlycemicGPT-Wear-0.1.99-dev.42-debug.apk")
        assertTrue(sameRun <= localRun)
    }

    @Test
    fun `version comparison stable channel uses version code`() {
        val remote = AppUpdateChecker.parseVersionCode("0.2.0")
        val local = AppUpdateChecker.parseVersionCode("0.1.99")
        assertTrue(remote > local)

        val sameVersion = AppUpdateChecker.parseVersionCode("0.1.99")
        assertTrue(sameVersion <= local)
    }
}
