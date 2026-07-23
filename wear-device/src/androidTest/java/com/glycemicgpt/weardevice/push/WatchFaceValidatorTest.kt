// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Josh Engelbrecht
package com.glycemicgpt.weardevice.push

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.wearable.watchface.validator.client.DwfValidatorFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Proves the Watch Face Push validator still produces a validation token after the EPL-1.0
 * XPath 2.0 processor is excluded from the validator dependency (see wear-device/build.gradle.kts).
 *
 * This is the exact path [WatchFaceInstaller.generateValidationToken] runs in production --
 * `DwfValidatorFactory.create().validate(apk, packageName)` over a real, shipped WFF APK -- so an
 * empty token here would mean the validator needs the excluded processor to parse the watch face,
 * and the exclusion is unsafe. The WFF APK bundled as a test asset is the debug digitalFull face,
 * whose package (`com.glycemicgpt.mobile.debug.watchfacepush.glycemicgpt`) is prefixed by this
 * module's debug applicationId, which is what the validator checks.
 */
@RunWith(AndroidJUnit4::class)
class WatchFaceValidatorTest {

    @Test
    fun validatorReturnsTokenWithoutTheExcludedXpath2Processor() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val clientPackageName = instrumentation.targetContext.packageName

        // The validator takes a File, so copy the bundled WFF APK out of the test assets.
        val apk = File.createTempFile("wff-validate", ".apk", instrumentation.targetContext.cacheDir)
        try {
            instrumentation.context.assets.open(WFF_ASSET).use { input ->
                apk.outputStream().use { output -> input.copyTo(output) }
            }

            val result = DwfValidatorFactory.create().validate(apk, clientPackageName)

            val failures = result.failures()
            assertTrue(
                "Validator reported failures without xpath2: " +
                    failures.joinToString { "${it.name()}: ${it.failureMessage()}" },
                failures.isEmpty(),
            )
            val token = result.validationToken()
            android.util.Log.i(
                "WatchFaceValidatorTest",
                "Validation token length without xpath2: ${token?.length ?: 0}",
            )
            assertFalse(
                "Validation token is empty -- the validator needs the excluded xpath2 processor " +
                    "to parse the WFF, so the exclusion is unsafe.",
                token.isNullOrEmpty(),
            )
        } finally {
            apk.delete()
        }
    }

    private companion object {
        const val WFF_ASSET = "glycemicgpt-watchface-digitalFull.apk"
    }
}
