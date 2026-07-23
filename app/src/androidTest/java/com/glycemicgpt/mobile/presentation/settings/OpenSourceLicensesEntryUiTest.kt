// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Josh Engelbrecht
package com.glycemicgpt.mobile.presentation.settings

import android.content.Intent
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.glycemicgpt.mobile.presentation.theme.GlycemicGptTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The About card's licensing entry was once a bare [androidx.compose.material3.Text] styled in
 * the primary colour: it looked like a link and did nothing. These pin the two properties that
 * fix requires -- a real click action, and a destination actually wired to it -- because the
 * regression is invisible to a screenshot and the callback has a no-op default.
 */
@RunWith(AndroidJUnit4::class)
class OpenSourceLicensesEntryUiTest {

    @get:Rule
    val compose = createComposeRule()

    private var navigations = 0

    private fun setContent() {
        compose.setContent {
            GlycemicGptTheme {
                AboutSection(
                    state = SettingsUiState(),
                    onCheckForUpdate = {},
                    onDownloadUpdate = { _, _ -> },
                    onGetInstallIntent = { Intent() },
                    onDismissUpdate = {},
                    onNavigateToLicenses = { navigations++ },
                )
            }
        }
    }

    @Test
    fun licensesEntry_isInteractive_notJustStyledText() {
        setContent()

        compose.onNodeWithTag("open_source_licenses_button")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun licensesEntry_tapReachesItsDestination() {
        setContent()

        compose.onNodeWithTag("open_source_licenses_button").performClick()

        assertEquals(1, navigations)
    }
}
