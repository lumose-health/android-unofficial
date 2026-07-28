package com.glycemicgpt.mobile.presentation.common

import android.content.ActivityNotFoundException
import android.text.util.Linkify
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import dev.jeziellago.compose.markdowntext.MarkdownText
import timber.log.Timber

/**
 * Renders markdown with this app's link policy: images are dropped, and only `http(s)` links
 * open.
 *
 * @param linkifyMask Android [android.text.util.Linkify] mask for auto-detecting links in plain
 *   text. Mirrors the compose-markdown default; callers rendering prose that contains file names
 *   should pass `0`, because `Linkify.WEB_URLS` reads `some-file.md` as a domain and turns it
 *   into a link to a site that does not exist. Markdown links written out in the source are
 *   parsed either way.
 * @param isTextSelectable Lets the user copy the rendered text.
 */
@Composable
fun AppMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    linkifyMask: Int = Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS or Linkify.WEB_URLS,
    isTextSelectable: Boolean = false,
) {
    if (markdown.isBlank()) return

    val uriHandler = LocalUriHandler.current
    val textColor = MaterialTheme.colorScheme.onSurface

    // Strip image markdown and HTML img tags to prevent outbound image fetches
    // from AI-generated content (mirrors web MarkdownContent image suppression)
    val sanitized = markdown
        .replace(Regex("""!\[[^\]]*]\([^)]*\)"""), "")
        .replace(Regex("""!\[[^\]]*]\[[^\]]*]"""), "")
        .replace(Regex("""<img[^>]*>""", RegexOption.IGNORE_CASE), "")

    MarkdownText(
        markdown = sanitized,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        linkifyMask = linkifyMask,
        isTextSelectable = isTextSelectable,
        onLinkClicked = { url ->
            val lower = url.lowercase()
            if (lower.startsWith("https://") || lower.startsWith("http://")) {
                try {
                    uriHandler.openUri(url)
                } catch (e: ActivityNotFoundException) {
                    Timber.w(e, "No activity found to handle URL")
                } catch (e: SecurityException) {
                    Timber.w(e, "Security exception opening URL")
                }
            }
        },
    )
}
