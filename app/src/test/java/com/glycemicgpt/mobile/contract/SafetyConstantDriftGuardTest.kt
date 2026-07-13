package com.glycemicgpt.mobile.contract

import com.glycemicgpt.mobile.domain.format.GlucoseFormat
import com.glycemicgpt.mobile.domain.model.BgmReading
import com.glycemicgpt.mobile.domain.model.BolusType
import com.glycemicgpt.mobile.domain.model.CgmReading
import com.glycemicgpt.mobile.domain.model.CgmTrend
import com.glycemicgpt.mobile.domain.model.EnrichedBolusEvent
import com.glycemicgpt.mobile.domain.plugin.events.PluginEvent
import com.glycemicgpt.mobile.domain.pump.SafetyLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

/**
 * Cross-repo safety-constant drift guard (GLY-92 / 56.9, AC3 + AC6).
 *
 * Three constants are duplicated between this app and the backend monorepo. If
 * they silently desync across the independent release cadences, glucose is
 * mis-converted or mis-validated -- a patient-safety failure. This guard fails
 * the build the moment any Android-side copy drifts from its canonical value.
 *
 * | Constant | Canonical | Backend owner (monorepo) |
 * |---|---|---|
 * | mmol<->mg/dL factor | 18.0156 | `src/core/units.py` MGDL_PER_MMOL |
 * | glucose-validity bound | 20..500 mg/dL | 20-500 platform invariant (PR #729, CodeRabbit Medical-Safety gate) |
 * | Tandem epoch offset (s) | 1199145600 | `core/tandem_regions.py` TANDEM_EPOCH_OFFSET_SECONDS |
 *
 * Full duplication-site inventory: docs/contract/safety-constants.md.
 *
 * The guard works three ways, strongest first:
 *  1. **Direct constant reads** of the public `const val`s the code actually uses.
 *  2. **Behavioral boundary** checks at the real `require`/`init` validators.
 *  3. **Source scan** of every remaining shipped duplication site (private consts,
 *     inline `20..500` literals, the wear/tandem copies another module's test
 *     cannot import) -- comment-stripped so only code counts and so a single
 *     changed occurrence in a multi-occurrence file is still caught.
 *
 * The 20..500 bound has no single owner on Android today; consolidating it is a
 * follow-up (see the ADR). Until then this guard is the desync backstop.
 */
class SafetyConstantDriftGuardTest {

    private val now: Instant = Instant.EPOCH
    private val mgdlPerMmol = 18.0156
    private val glucoseMin = 20
    private val glucoseMax = 500

    // --- 1. Direct constant reads (public const vals the code uses) ---

    @Test
    fun `mmol factor constant equals canonical`() {
        assertEquals(mgdlPerMmol, GlucoseFormat.MGDL_PER_MMOL, 0.0)
    }

    @Test
    fun `SafetyLimits glucose bounds equal canonical`() {
        assertEquals(glucoseMin, SafetyLimits.DEFAULT_MIN_GLUCOSE)
        assertEquals(glucoseMax, SafetyLimits.DEFAULT_MAX_GLUCOSE)
        assertEquals(glucoseMin, SafetyLimits.ABSOLUTE_MIN_GLUCOSE)
        assertEquals(glucoseMax, SafetyLimits.ABSOLUTE_MAX_GLUCOSE)
    }

    @Test
    fun `CgmReading bounds equal canonical`() {
        assertEquals(glucoseMin, CgmReading.MIN_MG_DL)
        assertEquals(glucoseMax, CgmReading.MAX_MG_DL)
    }

    // --- 2. Behavioral boundary at the real validators ---

    @Test
    fun `BgmReading enforces the 20-500 bound`() {
        BgmReading(glucoseMin, now) // accepted
        BgmReading(glucoseMax, now) // accepted
        assertThrows(IllegalArgumentException::class.java) { BgmReading(glucoseMin - 1, now) }
        assertThrows(IllegalArgumentException::class.java) { BgmReading(glucoseMax + 1, now) }
    }

    @Test
    fun `CgmReading enforces the 20-500 bound`() {
        CgmReading(glucoseMin, CgmTrend.DOUBLE_UP, now)
        CgmReading(glucoseMax, CgmTrend.DOUBLE_UP, now)
        assertThrows(IllegalArgumentException::class.java) {
            CgmReading(glucoseMin - 1, CgmTrend.DOUBLE_UP, now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CgmReading(glucoseMax + 1, CgmTrend.DOUBLE_UP, now)
        }
    }

    @Test
    fun `PluginEvent calibration enforces the 20-500 bound`() {
        PluginEvent.CalibrationRequested("p", glucoseMin, now)
        PluginEvent.CalibrationRequested("p", glucoseMax, now)
        assertThrows(IllegalArgumentException::class.java) {
            PluginEvent.CalibrationRequested("p", glucoseMin - 1, now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PluginEvent.CalibrationRequested("p", glucoseMax + 1, now)
        }
    }

    @Test
    fun `EnrichedBolusEvent enforces the 20-500 bound on bgAtEvent`() {
        enrichedWithBg(glucoseMin)
        enrichedWithBg(glucoseMax)
        enrichedWithBg(null) // bg is optional; null is allowed
        assertThrows(IllegalArgumentException::class.java) { enrichedWithBg(glucoseMin - 1) }
        assertThrows(IllegalArgumentException::class.java) { enrichedWithBg(glucoseMax + 1) }
    }

    private fun enrichedWithBg(bg: Int?) =
        EnrichedBolusEvent(
            units = 1f,
            bolusType = BolusType.MEAL,
            reason = "test",
            correctionUnits = 0f,
            mealUnits = 0f,
            bgAtEvent = bg,
            iobAtEvent = null,
            timestamp = now,
        )

    // --- 3. Source scan of every remaining shipped duplication site ---

    @Test
    fun `every shipped duplication site still uses the canonical constant`() {
        SCAN_SITES.forEach { site ->
            val code = stripComments(ContractFixtures.readRepoFile(site.path))
            val actual = countOccurrences(code, site.token)
            assertEquals(
                "Occurrence count changed in ${site.path}: expected ${site.expected} " +
                    "code occurrence(s) of `${site.token}` (${site.description}), found $actual. " +
                    "Either a cross-repo safety constant drifted (reconcile with the backend owner) " +
                    "or a duplication site was added/removed. Verify the value, then update the " +
                    "SCAN_SITES enumeration and docs/contract/safety-constants.md in the same PR.",
                site.expected,
                actual,
            )
        }
    }

    private data class ScanSite(
        val path: String,
        val token: String,
        val expected: Int,
        val description: String,
    )

    /**
     * Occurrences of [token] in [text], with **digit boundaries** so a widening
     * drift is caught. Plain substring matching would let `20..500` -> `20..5000`
     * or `18.0156` -> `18.01565` slip through (the old token is a prefix of the
     * new) -- exactly the unsafe direction. The lookarounds reject a digit
     * immediately before or after the token so only the exact numeric value matches.
     */
    private fun countOccurrences(text: String, token: String): Int =
        Regex("(?<![0-9])" + Regex.escape(token) + "(?![0-9])").findAll(text).count()

    /**
     * Strip `/* */` block comments (incl. KDoc) and `//` line comments so the scan
     * counts only code -- comment edits must not trip the guard, and a comment
     * mention of a bound must not mask a code-level change.
     */
    private fun stripComments(src: String): String {
        val noBlock = src.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        return noBlock.lines().joinToString("\n") { line ->
            val i = line.indexOf("//")
            if (i >= 0) line.substring(0, i) else line
        }
    }

    private companion object {
        private const val APP = "app/src/main/java/com/glycemicgpt/mobile"
        private const val API = "plugins/pump-driver-api/src/main/java/com/glycemicgpt/mobile"
        private const val WEAR = "wear-device/src/main/java/com/glycemicgpt/weardevice"
        private const val TANDEM = "plugins/shipped/tandem/src/main/java/com/glycemicgpt/mobile"

        // Every shipped production site from docs/contract/safety-constants.md,
        // EXCEPT the importable public consts already covered by the section-1
        // direct reads (GlucoseFormat.MGDL_PER_MMOL, SafetyLimits.DEFAULT_*,
        // CgmReading.MIN/MAX_MG_DL) -- the direct read is strictly stronger and
        // refactor-tolerant, so re-scanning them would only add brittleness.
        // Example-only plugins (plugins/example/**) are excluded as non-shipped
        // (documented in the SoT doc). Expected counts are comment-stripped code
        // occurrences; the match is digit-boundary-anchored (see countOccurrences).
        val SCAN_SITES =
            listOf(
                // -- 18.0156 (wear copy; app copy is a direct read) --
                ScanSite("$WEAR/util/GlucoseDisplayUtils.kt", "18.0156", 1, "wear MGDL_PER_MMOL const"),
                // -- Tandem epoch (private const; no importable accessor) --
                ScanSite("$TANDEM/ble/messages/StatusResponseParser.kt", "1199145600", 1, "TANDEM_EPOCH_OFFSET"),
                // -- 20..500 inline literals --
                ScanSite("$APP/data/local/AlertThresholdStore.kt", "20..500", 1, "threshold validation"),
                ScanSite("$APP/data/repository/AuthRepository.kt", "20..500", 2, "threshold validation x2"),
                ScanSite("$APP/domain/model/EnrichedBolusEvent.kt", "20..500", 1, "bgAtEvent require"),
                ScanSite("$APP/presentation/settings/SettingsViewModel.kt", "20..500", 2, "threshold validation x2"),
                ScanSite("$API/domain/model/BgmReading.kt", "20..500", 2, "require + message"),
                ScanSite("$API/domain/plugin/events/PluginEvent.kt", "20..500", 2, "require + message"),
                ScanSite("$WEAR/presentation/AlertsActivity.kt", "20..500", 1, "alert bg guard"),
                ScanSite("$WEAR/util/GlucoseDisplayUtils.kt", "20..500", 1, "isValidGlucose"),
                // -- 20..500 private named constants (not importable from :app) --
                ScanSite("$APP/domain/compute/DashboardComputations.kt", "VALID_GLUCOSE_MIN = 20", 1, "private min const"),
                ScanSite("$APP/domain/compute/DashboardComputations.kt", "VALID_GLUCOSE_MAX = 500", 1, "private max const"),
                ScanSite("$APP/presentation/home/HomeViewModel.kt", "MIN_THRESHOLD = 20", 1, "private min const"),
                ScanSite("$APP/presentation/home/HomeViewModel.kt", "MAX_THRESHOLD = 500", 1, "private max const"),
                // -- 20 / 500 special forms --
                ScanSite("$APP/data/local/dao/PumpDao.kt", "BETWEEN 20 AND 500", 1, "SQL range filter"),
                ScanSite("$APP/data/repository/AuthRepository.kt", "20..499", 1, "off-by-one min bound"),
                ScanSite("$APP/data/repository/AuthRepository.kt", "21..500", 1, "off-by-one max bound"),
                ScanSite("$WEAR/util/GlucoseDisplayUtils.kt", "coerceIn(20, low)", 1, "urgent-low clamp floor"),
                ScanSite("$WEAR/util/GlucoseDisplayUtils.kt", "coerceIn(high, 500)", 1, "urgent-high clamp ceiling"),
            )
    }
}
