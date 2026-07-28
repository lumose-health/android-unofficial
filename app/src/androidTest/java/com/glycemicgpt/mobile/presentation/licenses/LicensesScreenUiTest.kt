// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Josh Engelbrecht
package com.glycemicgpt.mobile.presentation.licenses

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glycemicgpt.mobile.presentation.theme.GlycemicGptTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end proof that the licence text reaches the screen out of the packaged assets. The
 * JVM-side [LicenseAssetsTest] compares the merged assets against the repository documents;
 * this runs the composable against a real [android.content.res.AssetManager], so a screen that
 * failed to read them would surface here as the Unavailable state rather than as a green build.
 */
@RunWith(AndroidJUnit4::class)
class LicensesScreenUiTest {

    @get:Rule
    val compose = createComposeRule()

    private var backPresses = 0

    private fun setContent() {
        compose.setContent {
            GlycemicGptTheme {
                LicensesScreen(onBack = { backPresses++ })
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun readsTheBundledAssets_ratherThanFallingBackToUnavailable() {
        setContent()

        // Only the Loaded branch carries this tag; Loading and Unavailable render a spinner
        // and an error message instead, so its presence is the read succeeding.
        compose.onNodeWithTag("licenses_content").assertIsDisplayed()
        compose.onNodeWithText("could not be read", substring = true).assertDoesNotExist()
    }

    @Test
    fun rendersTheLicenseTextItself_notJustAContainer() {
        setContent()

        // The GPL body renders as Compose text (the markdown blocks above it are an
        // AndroidView the semantics tree cannot see into), so this is the assertion that
        // proves real license content out of the asset reached the screen.
        compose.onNodeWithTag("licenses_content")
            .performScrollToNode(hasText("GNU GENERAL PUBLIC LICENSE", substring = true))
        compose.onNodeWithText("GNU GENERAL PUBLIC LICENSE", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun contentIsScrollable() {
        setContent()

        assertTrue(
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun backNavigationIsLabelledAndWired() {
        setContent()

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backPresses)
    }
}
