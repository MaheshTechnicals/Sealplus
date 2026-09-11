package com.junkfood.seal.util

import com.yausername.youtubedl_android.YoutubeDL
import org.junit.Assert.assertEquals
import org.junit.Test

class YtdlpUpdateChannelTest {
    @Test
    fun `stable preference uses stable channel`() {
        assertEquals(
            YoutubeDL.UpdateChannel.STABLE.apiUrl,
            UpdateUtil.getYtDlpUpdateChannel(YT_DLP_STABLE).apiUrl,
        )
    }

    @Test
    fun `nightly preference uses nightly channel`() {
        assertEquals(
            YoutubeDL.UpdateChannel.NIGHTLY.apiUrl,
            UpdateUtil.getYtDlpUpdateChannel(YT_DLP_NIGHTLY).apiUrl,
        )
    }

    @Test
    fun `RedGifs preference uses patched releases endpoint`() {
        assertEquals(
            "https://api.github.com/repos/thirteenth13/ytdlnis-redgifs-yt-dlp/releases/latest",
            UpdateUtil.getYtDlpUpdateChannel(YT_DLP_REDGIFS).apiUrl,
        )
    }

    @Test
    fun `unknown preference falls back to RedGifs channel`() {
        assertEquals(
            "https://api.github.com/repos/thirteenth13/ytdlnis-redgifs-yt-dlp/releases/latest",
            UpdateUtil.getYtDlpUpdateChannel(Int.MAX_VALUE).apiUrl,
        )
    }
}
