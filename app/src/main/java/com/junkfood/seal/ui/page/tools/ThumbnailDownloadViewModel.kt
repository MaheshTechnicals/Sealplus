package com.junkfood.seal.ui.page.tools

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.ThumbnailFormat
import com.junkfood.seal.util.ThumbnailQuality
import com.junkfood.seal.util.ThumbnailUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Backs the "Thumbnail Downloader" tool: given a YouTube URL, resolves the highest-resolution
 * thumbnail YouTube has for that video, previews it with basic video info, and lets the user
 * save it locally after converting it to their chosen output format/quality.
 *
 * Resolution is intentionally NOT a user choice — the highest resolution YouTube provides
 * (checked via `maxresdefault.jpg`, falling back gracefully) is always used. Users only pick
 * the *output* format/quality of the final saved file.
 */
class ThumbnailDownloadViewModel : ViewModel() {

    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    private var debounceJob: Job? = null
    private var fetchJob: Job? = null

    // Small in-memory cache keyed by video ID so re-entering/re-pasting the same URL in the
    // same session (e.g. after navigating away and back) is instant instead of re-running
    // yt-dlp + the maxres HEAD check again. Coil (AsyncImageImpl) separately caches the actual
    // decoded bitmap, so this covers the metadata side of "cache thumbnail previews".
    private val previewCache = LinkedHashMap<String, PreviewData>()
    private val maxCacheSize = 20

    // Raw (pre-conversion) thumbnail bytes for the current preview, kept around so the
    // download-summary file-size estimate can be recomputed instantly whenever the user
    // changes format/quality, without re-downloading from YouTube each time.
    private var rawThumbnailBytes: ByteArray? = null
    private var sizeEstimateJob: Job? = null

    data class PreviewData(
        val title: String,
        val channelName: String,
        val thumbnailUrl: String,
    )

    data class ViewState(
        val url: String = "",
        val isUrlValid: Boolean = false,
        val isFetching: Boolean = false,
        val videoId: String? = null,
        val title: String? = null,
        val channelName: String? = null,
        val thumbnailUrl: String? = null,
        val errorMessage: String? = null,
        val format: ThumbnailFormat = ThumbnailFormat.JPG,
        val quality: ThumbnailQuality = ThumbnailQuality.ORIGINAL,
        val fileName: String = "",
        val isDownloading: Boolean = false,
        val downloadSuccess: Boolean = false,
        val savedFilePath: String? = null,
        /** Size in bytes of the raw (pre-conversion) thumbnail once fetched — shown in the
         *  download summary so the user knows roughly what they're about to save. */
        val fileSizeBytes: Long? = null,
    ) {
        val hasPreview: Boolean get() = thumbnailUrl != null
    }

    /**
     * Called on every keystroke/paste. Debounces so we don't spam a yt-dlp fetch on every
     * character, and cancels any in-flight fetch for a now-stale URL — this is what prevents
     * duplicate/overlapping requests when the user edits the URL while a fetch is running.
     */
    fun updateUrl(url: String) {
        debounceJob?.cancel()
        fetchJob?.cancel()
        val trimmed = url.trim()
        val videoId = ThumbnailUtil.extractVideoId(trimmed)
        val isValid = videoId != null

        mutableViewStateFlow.update {
            it.copy(
                url = url,
                isUrlValid = isValid,
                errorMessage = null,
                isFetching = false,
            )
        }

        if (!isValid) {
            clearPreview()
            return
        }

        // Cache hit — show instantly, no network round trip, no loading skeleton.
        previewCache[videoId]?.let { cached ->
            mutableViewStateFlow.update {
                it.copy(
                    videoId = videoId,
                    title = cached.title,
                    channelName = cached.channelName,
                    thumbnailUrl = cached.thumbnailUrl,
                    fileName = it.fileName.ifBlank { cached.title },
                    errorMessage = null,
                )
            }
            return
        }

        debounceJob = viewModelScope.launch {
            delay(550L)
            fetchPreview(trimmed, videoId)
        }
    }

    private fun fetchPreview(url: String, videoId: String) {
        fetchJob?.cancel()
        mutableViewStateFlow.update { it.copy(isFetching = true, errorMessage = null) }
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            DownloadUtil.fetchVideoInfoFromUrl(url = url)
                .onSuccess { info ->
                    val bestThumbnailUrl =
                        ThumbnailUtil.resolveBestThumbnailUrl(videoId, info.thumbnail)
                    val title = info.title.ifBlank { "Untitled video" }
                    val channelName = (info.uploader ?: info.channel).orEmpty()

                    previewCache[videoId] = PreviewData(title, channelName, bestThumbnailUrl)
                    if (previewCache.size > maxCacheSize) {
                        previewCache.remove(previewCache.keys.first())
                    }

                    // Guard against a stale response landing after the user has since typed a
                    // different URL — only apply the result if it still matches.
                    if (mutableViewStateFlow.value.videoId != videoId &&
                        mutableViewStateFlow.value.url.trim() != url
                    ) {
                        return@onSuccess
                    }

                    mutableViewStateFlow.update {
                        it.copy(
                            isFetching = false,
                            videoId = videoId,
                            title = title,
                            channelName = channelName,
                            thumbnailUrl = bestThumbnailUrl,
                            fileName = it.fileName.ifBlank { title },
                            errorMessage = null,
                        )
                    }
                    refreshFileSizeEstimate(bestThumbnailUrl)
                }
                .onFailure { th ->
                    mutableViewStateFlow.update {
                        it.copy(
                            isFetching = false,
                            errorMessage = th.message
                                ?: App.context.getString(R.string.thumbnail_fetch_failed),
                        )
                    }
                }
        }
    }

    private fun clearPreview() {
        sizeEstimateJob?.cancel()
        rawThumbnailBytes = null
        mutableViewStateFlow.update {
            it.copy(
                videoId = null,
                title = null,
                channelName = null,
                thumbnailUrl = null,
                downloadSuccess = false,
                savedFilePath = null,
                fileSizeBytes = null,
            )
        }
    }

    fun updateFormat(format: ThumbnailFormat) {
        mutableViewStateFlow.update { it.copy(format = format, downloadSuccess = false) }
        recomputeSizeEstimate()
    }

    fun updateQuality(quality: ThumbnailQuality) {
        mutableViewStateFlow.update { it.copy(quality = quality, downloadSuccess = false) }
        recomputeSizeEstimate()
    }

    /** Downloads the raw thumbnail once so the summary can show an initial file-size estimate. */
    private fun refreshFileSizeEstimate(thumbnailUrl: String) {
        sizeEstimateJob?.cancel()
        sizeEstimateJob = viewModelScope.launch(Dispatchers.IO) {
            ThumbnailUtil.downloadThumbnailBytes(thumbnailUrl).onSuccess { bytes ->
                rawThumbnailBytes = bytes
                applySizeEstimateFromCache()
            }
        }
    }

    /** Re-encodes the already-downloaded raw bytes at the current format/quality to refresh
     *  the estimate — cheap since thumbnails are small images, and avoids a network round trip
     *  every time the user taps a different format/quality chip. */
    private fun recomputeSizeEstimate() {
        val raw = rawThumbnailBytes ?: return
        sizeEstimateJob?.cancel()
        sizeEstimateJob = viewModelScope.launch(Dispatchers.Default) {
            applySizeEstimateFromCache(raw)
        }
    }

    private suspend fun applySizeEstimateFromCache(raw: ByteArray? = rawThumbnailBytes) {
        val bytes = raw ?: return
        val state = mutableViewStateFlow.value
        ThumbnailUtil.encodeThumbnail(bytes, state.format, state.quality).onSuccess { encoded ->
            mutableViewStateFlow.update { it.copy(fileSizeBytes = encoded.size.toLong()) }
        }
    }

    fun updateFileName(name: String) {
        mutableViewStateFlow.update { it.copy(fileName = name) }
    }

    fun dismissDownloadSuccess() {
        mutableViewStateFlow.update { it.copy(downloadSuccess = false) }
    }

    fun clearAll() {
        debounceJob?.cancel()
        fetchJob?.cancel()
        sizeEstimateJob?.cancel()
        rawThumbnailBytes = null
        mutableViewStateFlow.value = ViewState()
    }

    /**
     * Downloads, converts, and saves the current preview's thumbnail. When [targetUri] is
     * provided (from a system "Save As" picker) the file is written there; otherwise it's
     * auto-saved under `Downloads/SealPlus/Thumbnails/`.
     */
    fun downloadThumbnail(targetUri: Uri? = null) {
        val state = mutableViewStateFlow.value
        val thumbnailUrl = state.thumbnailUrl ?: return
        if (state.isDownloading) return // prevent duplicate concurrent downloads

        mutableViewStateFlow.update {
            it.copy(isDownloading = true, downloadSuccess = false, errorMessage = null)
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Reuse the raw bytes already fetched for the file-size estimate when available,
            // instead of re-downloading the same thumbnail from YouTube a second time.
            val rawBytesResult = rawThumbnailBytes?.let { Result.success(it) }
                ?: ThumbnailUtil.downloadThumbnailBytes(thumbnailUrl)
            rawBytesResult
                .mapCatching { rawBytes ->
                    ThumbnailUtil.encodeThumbnail(rawBytes, state.format, state.quality).getOrThrow()
                }
                .mapCatching { encodedBytes ->
                    if (targetUri != null) {
                        App.context.contentResolver.openOutputStream(targetUri)?.use {
                            it.write(encodedBytes)
                        } ?: throw IllegalStateException("Could not open destination file")
                        targetUri.toString()
                    } else {
                        val fileName = state.fileName.ifBlank { state.title ?: "thumbnail" }
                        ThumbnailUtil.saveThumbnailToDownloads(encodedBytes, fileName, state.format)
                            .getOrThrow()
                    }
                }
                .onSuccess { savedPath ->
                    mutableViewStateFlow.update {
                        it.copy(
                            isDownloading = false,
                            downloadSuccess = true,
                            savedFilePath = savedPath,
                        )
                    }
                }
                .onFailure { th ->
                    mutableViewStateFlow.update {
                        it.copy(
                            isDownloading = false,
                            errorMessage = th.message
                                ?: App.context.getString(R.string.thumbnail_download_failed),
                        )
                    }
                }
        }
    }
}
