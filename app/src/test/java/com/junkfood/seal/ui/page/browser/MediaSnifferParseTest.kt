package com.junkfood.seal.ui.page.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaSnifferParseTest {

    private val sixResolutionMaster =
        """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-STREAM-INF:BANDWIDTH=20000000,RESOLUTION=3840x2160,CODECS="avc1.640028,mp4a.40.2"
        2160p/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=10000000,RESOLUTION=2560x1440,CODECS="avc1.640028,mp4a.40.2"
        1440p/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
        1080p/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,CODECS="avc1.640028,mp4a.40.2"
        720p/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=960x540,CODECS="avc1.640028,mp4a.40.2"
        540p/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=854x480,CODECS="avc1.640028,mp4a.40.2"
        480p/index.m3u8
        """.trimIndent()

    @Test
    fun masterPlaylistYieldsEveryResolution() {
        val variants = MediaSniffer.parseHlsVariants(sixResolutionMaster)
        assertEquals(6, variants.size)
        assertEquals(
            listOf("3840x2160", "2560x1440", "1920x1080", "1280x720", "960x540", "854x480"),
            variants.map { "${it.width}x${it.height}" },
        )
        assertEquals("2160p/index.m3u8", variants.first().uri)
    }

    @Test
    fun variantWithoutResolutionKeepsBandwidth() {
        val body = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1200000\nlow/index.m3u8\n"
        val v = MediaSniffer.parseHlsVariants(body).single()
        assertNull(v.width)
        assertNull(v.height)
        assertEquals(1_200_000L, v.bitrate)
    }

    @Test
    fun mp4TkhdReadsVideoDimensions() {
        // Minimal tkhd (version 0) with width 1920 and height 1080 as 16.16 fixed point.
        val payload = ByteArray(76 + 8)
        payload[0] = 0 // version 0
        val width = 1920 shl 16
        val height = 1080 shl 16
        writeBe32(payload, 76, width)
        writeBe32(payload, 80, height)
        val box = byteArrayOf(0, 0, 0, (8 + payload.size).toByte()) + "tkhd".toByteArray() + payload
        assertEquals(1920 to 1080, MediaSniffer.parseTkhdDimensions(box))
    }

    private fun writeBe32(b: ByteArray, at: Int, v: Int) {
        b[at] = (v shr 24).toByte()
        b[at + 1] = (v shr 16).toByte()
        b[at + 2] = (v shr 8).toByte()
        b[at + 3] = v.toByte()
    }
}
