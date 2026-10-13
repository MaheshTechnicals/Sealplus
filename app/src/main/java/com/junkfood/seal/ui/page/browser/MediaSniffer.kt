package com.junkfood.seal.ui.page.browser

import android.net.Uri
import com.junkfood.seal.App
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

enum class SniffedKind {
    Video,
    Audio,
    /** HLS / DASH manifest. yt-dlp merges the segments, so the manifest is the download target. */
    Stream,
}

/**
 * A media file found while a page in the in-app browser was loading.
 *
 * [pageUrl] and [pageUserAgent] are sent back to yt-dlp as Referer and User-Agent, which many
 * sites require before they serve the file. No cookies are used.
 */
data class SniffedMedia(
    val url: String,
    val pageUrl: String,
    val pageUserAgent: String,
    val kind: SniffedKind,
    val sizeBytes: Long? = null,
) {
    val fileName: String
        get() = Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() } ?: url

    val host: String
        get() = Uri.parse(url).host.orEmpty()
}

/**
 * Collects media URLs seen by the browser's WebView.
 *
 * Detection works on request URLs and the Accept header, so it finds progressive files and
 * HLS/DASH manifests. Streams that the page assembles in memory (blob: URLs, MSE) cannot be
 * seen this way and are not listed.
 */
object MediaSniffer {
    private val videoExtensions = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp", "flv")
    private val audioExtensions = setOf("mp3", "m4a", "aac", "ogg", "oga", "opus", "wav", "flac")
    private val streamExtensions = setOf("m3u8", "mpd")

    private val _items = MutableStateFlow<List<SniffedMedia>>(emptyList())
    val items: StateFlow<List<SniffedMedia>> = _items.asStateFlow()

    // Size probes are best effort, so keep the timeouts short and never block the browser.
    private val probeClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /** Returns the media kind for a request, or null when the request is not media. */
    fun classify(url: String, accept: String?): SniffedKind? {
        val uri = Uri.parse(url)
        if (uri.scheme != "http" && uri.scheme != "https") return null
        val ext = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return when {
            ext in streamExtensions -> SniffedKind.Stream
            ext in videoExtensions -> SniffedKind.Video
            ext in audioExtensions -> SniffedKind.Audio
            accept?.contains("video/", ignoreCase = true) == true -> SniffedKind.Video
            accept?.contains("audio/", ignoreCase = true) == true -> SniffedKind.Audio
            else -> null
        }
    }

    /** Adds [url] if it is new. Safe to call from any thread (WebView calls it off the UI thread). */
    fun offer(url: String, pageUrl: String, pageUserAgent: String, kind: SniffedKind) {
        val added = _items.updateAndGetAdded {
            if (it.any { media -> media.url == url }) null
            else it + SniffedMedia(url, pageUrl, pageUserAgent, kind)
        }
        if (added && kind != SniffedKind.Stream) probeSize(url, pageUrl, pageUserAgent)
    }

    fun remove(url: String) = _items.update { list -> list.filterNot { it.url == url } }

    fun clear() = _items.update { emptyList() }

    private fun probeSize(url: String, pageUrl: String, pageUserAgent: String) {
        App.applicationScope.launch(Dispatchers.IO) {
            val size =
                runCatching {
                        val request =
                            Request.Builder()
                                .url(url)
                                .head()
                                .header("Referer", pageUrl)
                                .header("User-Agent", pageUserAgent.ifBlank { "Mozilla/5.0" })
                                .build()
                        probeClient.newCall(request).execute().use { response ->
                            response.header("Content-Length")?.toLongOrNull()
                        }
                    }
                    .getOrNull()
            if (size != null && size > 0) {
                _items.update { list ->
                    list.map { if (it.url == url) it.copy(sizeBytes = size) else it }
                }
            }
        }
    }

    /** Runs [transform] atomically and reports whether the list actually changed. */
    private fun MutableStateFlow<List<SniffedMedia>>.updateAndGetAdded(
        transform: (List<SniffedMedia>) -> List<SniffedMedia>?
    ): Boolean {
        var added = false
        update { current ->
            val next = transform(current)
            added = next != null
            next ?: current
        }
        return added
    }
}
