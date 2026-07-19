package com.junkfood.seal.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.util.Log
import androidx.annotation.CheckResult
import com.junkfood.seal.App.Companion.context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "ThumbnailUtil"

/** Output image container format for a downloaded thumbnail. */
enum class ThumbnailFormat(val label: String, val extension: String, val mimeType: String) {
    JPG("JPG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WEBP", "webp", "image/webp"),
}

/**
 * Output quality — deliberately NOT a resolution choice (the highest resolution YouTube has
 * for the video is always used). This only controls how much the chosen [ThumbnailFormat] is
 * allowed to compress the image.
 */
enum class ThumbnailQuality(val label: String, val compressionValue: Int) {
    ORIGINAL("Original", 100),
    HIGH("High", 92),
    MEDIUM("Medium", 75),
    COMPRESSED("Compressed", 50),
}

/**
 * Resolves, downloads, converts, and saves YouTube video thumbnails at the highest resolution
 * YouTube provides. Kept separate from [DownloadUtil] because it has nothing to do with
 * yt-dlp/video downloads — this only ever talks to YouTube's static thumbnail CDN
 * (`i.ytimg.com`) and Android's own Bitmap APIs.
 */
object ThumbnailUtil {

    // YouTube's "thumbnail not found" placeholder for maxresdefault.jpg is a small (~1KB) grey
    // image served with a normal 200 OK — there's no dedicated error status to check. A genuine
    // maxres thumbnail is always well above this, so a Content-Length below the threshold means
    // "this video has no maxres thumbnail, fall back to a lower tier".
    private const val MAXRES_MIN_BYTES = 2_000L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val YOUTUBE_VIDEO_ID_REGEX = Regex(
        "(?:youtube(?:-nocookie)?\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/|v/|live/)|youtu\\.be/)([\\w-]{6,})",
        RegexOption.IGNORE_CASE,
    )

    /** Extracts the 11-character (or longer, to be lenient) video ID from any YouTube URL shape. */
    fun extractVideoId(url: String): String? =
        YOUTUBE_VIDEO_ID_REGEX.find(url.trim())?.groupValues?.getOrNull(1)

    /**
     * Determines the best thumbnail URL YouTube has for [videoId]: tries `maxresdefault.jpg`
     * first (YouTube's true highest-resolution thumbnail, usually 1280x720), and only falls
     * back to [fallbackThumbnail] (yt-dlp's own pick, which is reliable but occasionally a
     * lower tier) or a guaranteed-to-exist `hqdefault.jpg` if maxres isn't available for that
     * video (common for older/low-view videos).
     */
    @CheckResult
    suspend fun resolveBestThumbnailUrl(videoId: String, fallbackThumbnail: String?): String =
        withContext(Dispatchers.IO) {
            val maxresUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
            val maxresAvailable = runCatching {
                val request = Request.Builder().url(maxresUrl).head().build()
                httpClient.newCall(request).execute().use { response ->
                    response.isSuccessful &&
                        (response.header("Content-Length")?.toLongOrNull() ?: 0L) > MAXRES_MIN_BYTES
                }
            }.getOrDefault(false)

            when {
                maxresAvailable -> maxresUrl
                !fallbackThumbnail.isNullOrBlank() -> fallbackThumbnail
                else -> "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            }
        }

    /** Downloads the raw bytes of a thumbnail image from [url]. */
    @CheckResult
    suspend fun downloadThumbnailBytes(url: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Failed to download thumbnail (HTTP ${response.code})")
                    }
                    response.body?.bytes()?.takeIf { it.isNotEmpty() }
                        ?: throw IOException("Thumbnail response was empty")
                }
            }
        }

    /**
     * Re-encodes [sourceBytes] (always a JPG straight from YouTube) into the requested
     * [format]/[quality]. When the request is effectively a no-op (Original quality, JPG
     * output), the original bytes are returned untouched — this is the only way to guarantee
     * byte-for-byte fidelity with YouTube's own file, since any decode+re-encode cycle through
     * Android's JPEG encoder is technically lossy even at quality 100.
     */
    @CheckResult
    suspend fun encodeThumbnail(
        sourceBytes: ByteArray,
        format: ThumbnailFormat,
        quality: ThumbnailQuality,
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            if (format == ThumbnailFormat.JPG && quality == ThumbnailQuality.ORIGINAL) {
                return@runCatching sourceBytes
            }
            val bitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size)
                ?: throw IOException("Could not decode thumbnail image")
            try {
                val compressFormat = when (format) {
                    ThumbnailFormat.JPG -> Bitmap.CompressFormat.JPEG
                    ThumbnailFormat.PNG -> Bitmap.CompressFormat.PNG
                    ThumbnailFormat.WEBP -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (quality == ThumbnailQuality.ORIGINAL) {
                                Bitmap.CompressFormat.WEBP_LOSSLESS
                            } else {
                                Bitmap.CompressFormat.WEBP_LOSSY
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            Bitmap.CompressFormat.WEBP
                        }
                    }
                }
                // PNG is always lossless — the quality slider is meaningless for it.
                val compressionValue = if (format == ThumbnailFormat.PNG) 100 else quality.compressionValue
                val output = ByteArrayOutputStream()
                bitmap.compress(compressFormat, compressionValue, output)
                output.toByteArray()
            } finally {
                bitmap.recycle()
            }
        }
    }

    /**
     * Writes [bytes] into `Downloads/SealPlus/Thumbnails/<fileName>.<ext>`, auto-numbering if a
     * file with that name already exists rather than overwriting it. Uses the exact same
     * directory-under-SealPlus + [MediaScannerConnection] pattern already used by
     * [FileUtil.getDocsDirectory] for the video-info text export, so thumbnails show up in the
     * gallery/file manager the same way downloaded videos already do.
     */
    @CheckResult
    fun saveThumbnailToDownloads(
        bytes: ByteArray,
        fileNameWithoutExtension: String,
        format: ThumbnailFormat,
    ): Result<String> = runCatching {
        val dir = File(FileUtil.getExternalDownloadDirectory(), "Thumbnails").apply { mkdirs() }
        val safeName = sanitizeFileName(fileNameWithoutExtension)
        var destFile = File(dir, "$safeName.${format.extension}")
        var counter = 1
        while (destFile.exists()) {
            destFile = File(dir, "$safeName ($counter).${format.extension}")
            counter++
        }
        destFile.writeBytes(bytes)
        runCatching {
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
        }
        destFile.absolutePath
    }.onFailure { Log.e(TAG, "Failed to save thumbnail: ${it.message}") }

    fun sanitizeFileName(name: String): String =
        name.ifBlank { "thumbnail" }
            .replace(Regex("""[<>:"/\\|?*]"""), "_")
            .trim()
            .take(100)
            .ifBlank { "thumbnail" }
}
