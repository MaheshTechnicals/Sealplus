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
        // A frame a little into the video looks better than a black first frame.
        val frame = if (width != null) retriever.getFrameAtTime(1_000_000L) else null
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

/** "1:02:03" or "4:05". */
fun formatDurationMs(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(java.util.Locale.US, "%d:%02d", m, s)
}
