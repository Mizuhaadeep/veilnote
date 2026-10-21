package com.glassnote.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlTextTest {

    @Test
    fun stopsBeforeChineseCharacters() {
        val matches = UrlText.findUrls("www.example.com鹅鹅鹅")

        assertEquals(listOf("www.example.com"), matches.map { it.value })
    }

    @Test
    fun stopsPathBeforeChinese() {
        val matches = UrlText.findUrls("https://example.com/docs中文说明")

        assertEquals(listOf("https://example.com/docs"), matches.map { it.value })
    }

    @Test
    fun keepsAsciiPathQueryAndFragment() {
        val matches = UrlText.findUrls(
            "see https://example.com/a-b_c?x=1&y=2#part now",
        )

        assertEquals(
            listOf("https://example.com/a-b_c?x=1&y=2#part"),
            matches.map { it.value },
        )
    }

    @Test
    fun dropsTrailingPunctuation() {
        val matches = UrlText.findUrls("visit www.example.com/path).")

        assertEquals(
            listOf("www.example.com/path"),
            matches.map { it.value },
        )
    }
}
