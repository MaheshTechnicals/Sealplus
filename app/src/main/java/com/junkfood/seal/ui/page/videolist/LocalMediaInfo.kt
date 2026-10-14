package com.junkfood.seal.ui.page.videolist

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

/**
 * Facts read from a downloaded file itself. Direct (non-yt-dlp) downloads have no metadata from
 * the site, so the details screen fills these in from the file.
 */
data class LocalMediaInfo(
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val frame: Bitmap?,
) {
    /** "1920×1080", or null when the file has no video track. */
    val resolutionLabel: String?
        get() = if (width != null && height != null) "$width×$height" else null
}

/** Reads size, resolution, duration and a first-frame thumbnail. Call off the main thread. */
fun readLocalMediaInfo(path: String): LocalMediaInfo? {
    val file = File(path)
    if (path.isBlank() || !file.isFile) return null
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        // Skip intros and black frames: try a few points in the video and keep the first bright one.
        val frame = if (width != null) pickRepresentativeFrame(retriever, duration) else null
        LocalMediaInfo(
            sizeBytes = file.length(),
            width = width,
            height = height,
            durationMs = duration,
            frame = frame,
        )
    } catch (e: Exception) {
        // Unreadable or unsupported file: still show the size.
        LocalMediaInfo(file.length(), null, null, null, null)
    } finally {
        retriever.release()
    }
}

/** Frame from ~10%, 25%, 50%, 75% of the video that is not almost black. */
private fun pickRepresentativeFrame(retriever: MediaMetadataRetriever, durationMs: Long?): Bitmap? {
    val total = durationMs?.takeIf { it > 0 } ?: 10_000L
    var fallback: Bitmap? = null
    for (fraction in listOf(0.10, 0.25, 0.50, 0.75)) {
        val frame = retriever.getFrameAtTime((total * fraction * 1000).toLong(), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: continue
        if (fallback == null) fallback = frame
        if (averageBrightness(frame) > 24) return frame
    }
    return fallback
}

/** Mean brightness of a sampled grid of pixels, 0..255. */
private fun averageBrightness(bitmap: Bitmap): Int {
    val step = maxOf(1, minOf(bitmap.width, bitmap.height) / 16)
    var sum = 0L
    var n = 0
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val c = bitmap.getPixel(x, y)
            sum += ((c shr 16) and 0xFF) * 3 + ((c shr 8) and 0xFF) * 6 + (c and 0xFF)
            n++
            x += step
        }
        y += step
    }
    return if (n == 0) 0 else (sum / (n * 10)).toInt()
}

/** "1:02:03" or "4:05". */
fun formatDurationMs(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(java.util.Locale.US, "%d:%02d", m, s)
}

/**
 * Writes a first-frame JPEG for a downloaded video into [cacheDir] and returns its `file://` URI,
 * or null when the file has no video frame. Used as the history thumbnail for direct downloads.
 */
fun saveLocalThumbnail(cacheDir: File, videoPath: String): String? {
    val frame = readLocalMediaInfo(videoPath)?.frame ?: return null
    val dir = File(cacheDir, "thumbnails").apply { mkdirs() }
    val out = File(dir, "${videoPath.hashCode().toUInt()}.jpg")
    return try {
        out.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        "file://${out.absolutePath}"
    } catch (e: Exception) {
        null
    }
}
