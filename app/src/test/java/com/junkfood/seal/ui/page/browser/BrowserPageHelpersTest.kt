package com.junkfood.seal.ui.page.browser

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserPageHelpersTest {

    @Test
    fun toLoadableUrl_keepsFullUrls() {
        assertEquals("https://example.com/a?b=1", toLoadableUrl("https://example.com/a?b=1"))
        assertEquals("http://example.com", toLoadableUrl("http://example.com"))
    }

    @Test
    fun toLoadableUrl_addsSchemeToHosts() {
        assertEquals("https://example.com", toLoadableUrl("example.com"))
        assertEquals("https://example.com/path", toLoadableUrl("  example.com/path  "))
    }

    @Test
    fun toLoadableUrl_turnsTextIntoSearch() {
        assertEquals("https://www.google.com/search?q=hello+world", toLoadableUrl("hello world"))
    }

    @Test
    fun toLoadableUrl_blankGoesHome() {
        assertEquals("https://www.google.com", toLoadableUrl("   "))
    }

    @Test
    fun cleanBaseName_removesExtensionAndPathCharacters() {
        assertEquals("240P_1000K_41863125", cleanBaseName("240P_1000K_41863125.mp4"))
        assertEquals("a_b_c_", cleanBaseName("a/b:c?.mp4"))
    }

    @Test
    fun cleanBaseName_fallsBackToVideo() {
        assertEquals("video", cleanBaseName(""))
        assertEquals("video", cleanBaseName(".mp4"))
    }

    @Test
    fun findPageThumbnail_readsOgImage() {
        val html = """<meta property="og:image" content="https://cdn.example.com/t.jpg">"""
        assertEquals("https://cdn.example.com/t.jpg", findPageThumbnail(html))
    }

    @Test
    fun findPageThumbnail_readsVideoPoster() {
        val html = """<video controls poster="https://cdn.example.com/p.jpg?a=1&amp;b=2"></video>"""
        assertEquals("https://cdn.example.com/p.jpg?a=1&b=2", findPageThumbnail(html))
    }

    @Test
    fun findPageThumbnail_emptyWhenNone() {
        assertEquals("", findPageThumbnail("<html></html>"))
    }

    @Test
    fun uniqueBaseName_addsNumberOnCollision() {
        val dir = createTempDirectory("browser-names").toFile()
        try {
            assertEquals("Movie", uniqueBaseName(dir, "Movie"))
            File(dir, "Movie.mp4").createNewFile()
            assertEquals("Movie (2)", uniqueBaseName(dir, "Movie"))
            File(dir, "Movie (2).mp4").createNewFile()
            assertEquals("Movie (3)", uniqueBaseName(dir, "Movie"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
