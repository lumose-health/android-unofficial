package com.glycemicgpt.weardevice.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wear arm of the cross-repo safety-constant drift guard (GLY-92).
 *
 * The main guard (`:app` `SafetyConstantDriftGuardTest`) cannot import the wear
 * module, so it can only *scan the source* for the wear copies of the safety
 * constants. This test gives the wear copies a compiled, refactor-tolerant
 * backstop -- a value/behavioral assertion that catches any drift the source
 * scan cannot express (e.g. the constant rewritten as an expression), independent
 * of the display-formatting tests.
 *
 * Canonical values and full inventory: docs/contract/safety-constants.md.
 */
class SafetyConstantWearGuardTest {

    @Test
    fun `wear mmol factor equals canonical 18_0156`() {
        assertEquals(18.0156, GlucoseDisplayUtils.MGDL_PER_MMOL, 0.0)
    }

    @Test
    fun `wear glucose-validity bound is 20 to 500`() {
        assertTrue(GlucoseDisplayUtils.isValidGlucose(20))
        assertTrue(GlucoseDisplayUtils.isValidGlucose(500))
        assertFalse(GlucoseDisplayUtils.isValidGlucose(19))
        assertFalse(GlucoseDisplayUtils.isValidGlucose(501))
    }
}
