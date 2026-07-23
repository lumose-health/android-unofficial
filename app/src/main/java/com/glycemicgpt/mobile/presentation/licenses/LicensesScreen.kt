// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Josh Engelbrecht
package com.glycemicgpt.mobile.presentation.licenses

import android.content.res.AssetManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.glycemicgpt.mobile.presentation.common.AppMarkdownText
import com.glycemicgpt.mobile.presentation.detail.DetailScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

/**
 * The app's in-product licensing and attribution surface: the project's own notice, the
 * third-party attributions, and the full GPL-3.0 text.
 *
 * The wording comes from repository documents bundled into the APK (see [LicenseDocuments]);
 * the screen states nothing of its own about how the project is licensed, and needs no
 * connectivity to show any of it. Display does normalise the markup around that wording:
 * repository-relative links lose their targets ([stripUnresolvableLinks]), images are dropped
 * ([AppMarkdownText]), and the plain-text license is re-spaced into paragraphs.
 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val assets = LocalContext.current.assets
    var state by remember { mutableStateOf<LicensesUiState>(LicensesUiState.Loading) }
    // Hoisted above the loading branch so a rotation -- which sends the screen back through
    // Loading while the assets are re-read -- returns the reader to where they were rather
    // than to the top of a 35 KB document.
    val listState = rememberLazyListState()
    LaunchedEffect(assets) {
        state = withContext(Dispatchers.IO) { readLicenseDocuments(assets) }
    }

    DetailScaffold(title = "Open Source Licenses", onBack = onBack) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (val current = state) {
            LicensesUiState.Loading -> Centered(contentModifier) { CircularProgressIndicator() }

            LicensesUiState.Unavailable -> Centered(contentModifier) {
                Text(
                    text = "License text could not be read from this build.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // One container around the whole list, not one per item: selection has to be able
            // to cross paragraph boundaries for copying a clause to be of any use.
            is LicensesUiState.Loaded -> SelectionContainer {
                LazyColumn(
                    modifier = contentModifier.testTag("licenses_content"),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // linkifyMask = 0 because these documents name files, not sites -- see
                    // AppMarkdownText. They render into a view SelectionContainer cannot reach,
                    // hence isTextSelectable for their own copy support.
                    item { AppMarkdownText(current.projectNotice, linkifyMask = 0, isTextSelectable = true) }
                    item { HorizontalDivider() }
                    item { AppMarkdownText(current.thirdParty, linkifyMask = 0, isTextSelectable = true) }
                    item { HorizontalDivider() }
                    // One item per paragraph rather than one 35 KB Text, so the list measures
                    // only the paragraphs on screen instead of all ~670 lines of the license.
                    items(current.fullLicenseParagraphs) { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

private fun readLicenseDocuments(assets: AssetManager): LicensesUiState = try {
    LicensesUiState.Loaded(
        projectNotice = stripUnresolvableLinks(
            LicenseDocuments.read(assets, LicenseDocuments.PROJECT_NOTICE),
        ),
        thirdParty = stripUnresolvableLinks(
            LicenseDocuments.read(assets, LicenseDocuments.THIRD_PARTY),
        ),
        // Plain text, not markdown: the GPL is not a markdown document, and a renderer would
        // read its indented lines as code blocks and its section numbers as list markers.
        fullLicenseParagraphs = LicenseDocuments
            .read(assets, LicenseDocuments.FULL_LICENSE)
            .split(PARAGRAPH_BREAK),
    )
} catch (e: IOException) {
    Timber.e(e, "License assets are missing from this build")
    LicensesUiState.Unavailable
}

private sealed interface LicensesUiState {
    data object Loading : LicensesUiState

    /** The assets are generated into every build, so this is a packaging failure, not a user state. */
    data object Unavailable : LicensesUiState

    data class Loaded(
        val projectNotice: String,
        val thirdParty: String,
        val fullLicenseParagraphs: List<String>,
    ) : LicensesUiState
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
}

private val PARAGRAPH_BREAK = Regex("""\n\s*\n""")
