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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
 *
 * [width] and [height] are read from the file (MP4 header) or from the stream manifest. They are
 * null when the format does not expose them, and such items are listed last.
 */
data class SniffedMedia(
    val url: String,
    val pageUrl: String,
    val pageUserAgent: String,
    val kind: SniffedKind,
    val sizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    /** Bits per second from the manifest's BANDWIDTH, used when the resolution is unknown. */
    val bitrate: Long? = null,
) {
    val fileName: String
        get() = Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() } ?: url

    val host: String
        get() = Uri.parse(url).host.orEmpty()

    /** "1920×1080", or null when the resolution is unknown. */
    val resolutionLabel: String?
        get() = if (width != null && height != null) "$width×$height" else null

    /** "2.5 Mbps", or null when the bitrate is unknown. */
    val bitrateLabel: String?
        get() = bitrate?.let { java.util.Locale.US.let { l -> String.format(l, "%.1f Mbps", it / 1_000_000.0) } }
}

/**
 * Collects media URLs seen by the browser's WebView.
 *
 * Detection works on request URLs and the Accept header, so it finds progressive files and
 * HLS/DASH manifests. Streams that the page assembles in memory (blob: URLs, MSE) cannot be
 * seen this way and are not listed.
 *
 * The list is always sorted from highest to lowest resolution.
 */
object MediaSniffer {
    private val videoExtensions = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp", "flv")
    private val audioExtensions = setOf("mp3", "m4a", "aac", "ogg", "oga", "opus", "wav", "flac")
    private val streamExtensions = setOf("m3u8", "mpd")

    /** Enough bytes to find the MP4 'tkhd' box in files whose 'moov' box is at the start or end. */
    private const val HEADER_PROBE_BYTES = 2 * 1024 * 1024
    private const val MANIFEST_MAX_BYTES = 512 * 1024

    private val _items = MutableStateFlow<List<SniffedMedia>>(emptyList())
    val items: StateFlow<List<SniffedMedia>> = _items.asStateFlow()

    // Probes are best effort, so keep the timeouts short and never block the browser.
    private val probeClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
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

    /** Adds [url] if it is new, then probes it in the background. Safe to call from any thread. */
    fun offer(url: String, pageUrl: String, pageUserAgent: String, kind: SniffedKind) {
        val added = mutate { list ->
            if (list.any { it.url == url }) null else list + SniffedMedia(url, pageUrl, pageUserAgent, kind)
        }
        if (!added) return
        App.applicationScope.launch(Dispatchers.IO) {
            when (kind) {
                SniffedKind.Stream -> resolveManifest(url, pageUrl, pageUserAgent)
                else -> probeFile(url, pageUrl, pageUserAgent)
            }
        }
    }

    fun remove(url: String) = mutate { list -> list.filterNot { it.url == url } }

    fun clear() = mutate { emptyList() }

    /** Applies [transform] atomically, sorts the result by quality and returns true if it changed. */
    private fun mutate(transform: (List<SniffedMedia>) -> List<SniffedMedia>?): Boolean {
        var changed = false
        _items.update { current ->
            val next = transform(current)
            changed = next != null
            (next ?: current).sortedWith(QUALITY_ORDER)
        }
        return changed
    }

    /** Highest resolution first, then larger file first. Items with no resolution go last. */
    private val QUALITY_ORDER =
        compareByDescending<SniffedMedia> { (it.height ?: -1) }
            .thenByDescending { it.width ?: -1 }
            .thenByDescending { it.bitrate ?: -1L }
            .thenByDescending { it.sizeBytes ?: -1L }

    private fun headers(pageUrl: String, pageUserAgent: String) =
        mapOf("Referer" to pageUrl, "User-Agent" to pageUserAgent.ifBlank { "Mozilla/5.0" })

    /** Size from HEAD, then width and height from the MP4 header when the file is an MP4. */
    private fun probeFile(url: String, pageUrl: String, pageUserAgent: String) {
        val headers = headers(pageUrl, pageUserAgent)
        val length =
            runCatching {
                    val request = Request.Builder().url(url).head().apply {
                        headers.forEach { (k, v) -> header(k, v) }
                    }.build()
                    probeClient.newCall(request).execute().use { it.header("Content-Length")?.toLongOrNull() }
                }
                .getOrNull()
                ?.takeIf { it > 0 }

        val dims = probeMp4Dimensions(url, headers, length)
        mutate { list ->
            list.map {
                if (it.url != url) it
                else it.copy(sizeBytes = length ?: it.sizeBytes, width = dims?.first, height = dims?.second)
            }
        }
    }

    /** Reads width and height from the first video track ('tkhd' box) of an MP4 file. */
    private fun probeMp4Dimensions(url: String, headers: Map<String, String>, length: Long?): Pair<Int, Int>? {
        fetchRange(url, headers, 0, HEADER_PROBE_BYTES - 1L)?.let { parseTkhdDimensions(it) }?.let {
            return it
        }
        // moov box is often at the end of the file (not "fast start").
        if (length != null && length > HEADER_PROBE_BYTES) {
            val start = length - HEADER_PROBE_BYTES
            fetchRange(url, headers, start, length - 1)?.let { return parseTkhdDimensions(it) }
        }
        return null
    }

    private fun fetchRange(url: String, headers: Map<String, String>, from: Long, to: Long): ByteArray? =
        runCatching {
                val request = Request.Builder().url(url).header("Range", "bytes=$from-$to").apply {
                    headers.forEach { (k, v) -> header(k, v) }
                }.build()
                probeClient.newCall(request).execute().use { response ->
                    val stream = response.body?.byteStream() ?: return@use null
                    // Servers that ignore Range would send the whole file, so read at most the limit.
                    val limit = (to - from + 1).coerceAtMost(HEADER_PROBE_BYTES.toLong()).toInt()
                    val buffer = ByteArray(limit)
                    var read = 0
                    while (read < limit) {
                        val n = stream.read(buffer, read, limit - read)
                        if (n <= 0) break
                        read += n
                    }
                    buffer.copyOf(read)
                }
            }
            .getOrNull()

    /**
     * Finds 'tkhd' boxes and returns the first one with a non-zero size. Audio tracks have a zero
     * width, so the first non-zero box is the video track.
     */
    internal fun parseTkhdDimensions(bytes: ByteArray): Pair<Int, Int>? {
        val tag = "tkhd".toByteArray(Charsets.US_ASCII)
        for (i in 0..bytes.size - 4) {
            if (bytes[i] != tag[0] || bytes[i + 1] != tag[1] || bytes[i + 2] != tag[2] || bytes[i + 3] != tag[3]) continue
            val payload = i + 4
            if (payload >= bytes.size) break
            val version = bytes[payload].toInt()
            val widthAt = payload + if (version == 1) 80 else 76
            if (widthAt + 8 > bytes.size) continue
            // width and height are 16.16 fixed point numbers
            val width = be32(bytes, widthAt) ushr 16
            val height = be32(bytes, widthAt + 4) ushr 16
            if (width > 0 && height > 0) return width to height
        }
        return null
    }

    private fun be32(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xFF) shl 24) or
            ((b[at + 1].toInt() and 0xFF) shl 16) or
            ((b[at + 2].toInt() and 0xFF) shl 8) or
            (b[at + 3].toInt() and 0xFF)

    /**
     * HLS master playlists list each quality. Every variant becomes its own item, so the user can
     * pick a resolution. A media playlist (no variants) keeps the original entry.
     */
    private fun resolveManifest(url: String, pageUrl: String, pageUserAgent: String) {
        val headers = headers(pageUrl, pageUserAgent)
        val body = fetchText(url, headers) ?: return
        if (body.contains("#EXTM3U") && body.contains("#EXT-X-STREAM-INF")) {
            val base = url.toHttpUrlOrNull() ?: return
            val variants = parseHlsVariants(body).mapNotNull { v ->
                base.resolve(v.uri)?.toString()?.let { absolute -> v.copy(uri = absolute) }
            }
            if (variants.isEmpty()) return
            mutate { list ->
                val master = list.firstOrNull { it.url == url }
                val pageUrlValue = master?.pageUrl ?: pageUrl
                val agent = master?.pageUserAgent ?: pageUserAgent
                val without = list.filterNot { it.url == url }
                val added =
                    variants.filter { v -> without.none { it.url == v.uri } }
                        .map { v ->
                            SniffedMedia(
                                url = v.uri,
                                pageUrl = pageUrlValue,
                                pageUserAgent = agent,
                                kind = SniffedKind.Stream,
                                width = v.width,
                                height = v.height,
                                bitrate = v.bitrate,
                            )
                        }
                without + added
            }
        } else if (body.contains("<MPD")) {
            val size = parseDashMaxDimensions(body) ?: return
            mutate { list ->
                list.map { if (it.url == url) it.copy(width = size.first, height = size.second) else it }
            }
        }
    }

    internal data class HlsVariant(
        val uri: String,
        val width: Int?,
        val height: Int?,
        val bitrate: Long?,
    )

    /** One entry per EXT-X-STREAM-INF line, with its resolution and BANDWIDTH when present. */
    internal fun parseHlsVariants(body: String): List<HlsVariant> {
        val result = mutableListOf<HlsVariant>()
        var pending: HlsVariant? = null
        for (raw in body.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-STREAM-INF") -> {
                    val res = Regex("RESOLUTION=(\\d+)x(\\d+)").find(line)
                    val bw = Regex("[:,]BANDWIDTH=(\\d+)").find(line)
                    pending =
                        HlsVariant(
                            uri = "",
                            width = res?.groupValues?.get(1)?.toIntOrNull(),
                            height = res?.groupValues?.get(2)?.toIntOrNull(),
                            bitrate = bw?.groupValues?.get(1)?.toLongOrNull(),
                        )
                }
                line.isNotEmpty() && !line.startsWith("#") && pending != null -> {
                    result += pending.copy(uri = line)
                    pending = null
                }
            }
        }
        return result
    }

    /** Largest Representation width and height in a DASH manifest. */
    internal fun parseDashMaxDimensions(body: String): Pair<Int, Int>? {
        val pairs =
            Regex("<Representation[^>]*>").findAll(body).mapNotNull { tag ->
                val w = Regex("width=\"(\\d+)\"").find(tag.value)?.groupValues?.get(1)?.toIntOrNull()
                val h = Regex("height=\"(\\d+)\"").find(tag.value)?.groupValues?.get(1)?.toIntOrNull()
                if (w != null && h != null) w to h else null
            }
        return pairs.maxByOrNull { it.second }
    }

    private fun fetchText(url: String, headers: Map<String, String>): String? =
        runCatching {
                val request = Request.Builder().url(url).apply {
                    headers.forEach { (k, v) -> header(k, v) }
                }.build()
                probeClient.newCall(request).execute().use { response ->
                    response.body?.byteStream()?.use { stream ->
                        val bytes = stream.readNBytesCompat(MANIFEST_MAX_BYTES)
                        String(bytes, Charsets.UTF_8)
                    }
                }
            }
            .getOrNull()

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (out.size() < limit) {
            val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (n <= 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
