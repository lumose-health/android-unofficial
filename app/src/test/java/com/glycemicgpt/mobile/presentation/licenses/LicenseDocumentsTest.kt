// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 Josh Engelbrecht
package com.glycemicgpt.mobile.presentation.licenses

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The license documents are bundled verbatim, so the only display-time adjustment the viewer
 * makes is reducing links it cannot follow. These pin that adjustment to exactly that: no
 * wording changes, and no loss of the links that do work.
 */
class LicenseDocumentsTest {

    @Test
    fun `repository-relative links are reduced to their label`() {
        assertEquals(
            "See LICENSE for the full text",
            stripUnresolvableLinks("See [LICENSE](LICENSE) for the full text"),
        )
        assertEquals(
            "the full license text ships at `LICENSE`",
            stripUnresolvableLinks("the full license text ships at [`LICENSE`](../LICENSE)"),
        )
    }

    @Test
    fun `http and https links are left intact`() {
        val markdown = "- [pumpX2](https://github.com/jwoglom/pumpX2) and " +
            "[apache](http://www.apache.org/licenses/)"
        assertEquals(markdown, stripUnresolvableLinks(markdown))
    }

    @Test
    fun `a mailto target survives, because losing the address would lose information`() {
        val link = "[mail us](mailto:nobody@example.org)"
        assertEquals(link, stripUnresolvableLinks(link))
    }

    @Test
    fun `image syntax is left for the markdown image sanitizer`() {
        val image = "![diagram](diagram.png)"
        assertEquals(image, stripUnresolvableLinks(image))
    }

    @Test
    fun `prose around a link is preserved exactly`() {
        assertEquals(
            "Copyright (C) 2026 Josh Engelbrecht -- see docs/dev/spdx-header-policy.md.",
            stripUnresolvableLinks(
                "Copyright (C) 2026 Josh Engelbrecht -- see " +
                    "[docs/dev/spdx-header-policy.md](docs/dev/spdx-header-policy.md).",
            ),
        )
    }

    @Test
    fun `text without links is unchanged`() {
        val plain = "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND [sic]."
        assertEquals(plain, stripUnresolvableLinks(plain))
    }
}
