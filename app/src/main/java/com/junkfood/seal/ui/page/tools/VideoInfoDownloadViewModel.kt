package com.junkfood.seal.ui.page.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.SavedVideoInfo
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Matches YouTube video links only (watch/shorts/embed/live/v paths on youtube.com,
 * music.youtube.com, m.youtube.com, or the youtu.be short domain). This tool is scoped to
 * YouTube, so anything else (other sites, playlist-only or channel-only links, etc.) is
 * rejected with a clear message instead of being sent to yt-dlp.
 */
private val YOUTUBE_VIDEO_URL_REGEX = Regex(
    "^(https?://)?(www\\.|m\\.|music\\.)?(youtube\\.com/(watch\\?.*v=|shorts/|embed/|v/|live/)[\\w-]{6,}|youtu\\.be/[\\w-]{6,})",
    RegexOption.IGNORE_CASE,
)

fun isYoutubeVideoUrl(url: String): Boolean = YOUTUBE_VIDEO_URL_REGEX.containsMatchIn(url.trim())

/**
 * Backs the "Video Info Download" tool page: fetches yt-dlp metadata for a URL, lets the
 * user preview it, and saves it as a card the user can revisit later on the same page.
 */
class VideoInfoDownloadViewModel : ViewModel() {

    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    val savedInfoListFlow =
        DatabaseUtil.getSavedVideoInfoFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class ViewState(
        val url: String = "",
        val isLoading: Boolean = false,
        val result: VideoInfo? = null,
        val errorMessage: String? = null,
        val isSaved: Boolean = false,
    )

    fun updateUrl(url: String) {
        mutableViewStateFlow.update { it.copy(url = url, errorMessage = null) }
    }

    fun fetchInfo() {
        val url = viewStateFlow.value.url.trim()
        if (url.isBlank()) {
            mutableViewStateFlow.update { it.copy(errorMessage = "Please enter a video URL") }
            return
        }
        if (!isYoutubeVideoUrl(url)) {
            mutableViewStateFlow.update {
                it.copy(
                    errorMessage = App.context.getString(R.string.invalid_youtube_url),
                    result = null,
                )
            }
            return
        }
        mutableViewStateFlow.update {
            it.copy(isLoading = true, errorMessage = null, result = null, isSaved = false)
        }
        viewModelScope.launch(Dispatchers.IO) {
            DownloadUtil.fetchVideoInfoFromUrl(url = url)
                .onSuccess { info ->
                    // Auto-save immediately on a successful fetch — no extra tap required.
                    DatabaseUtil.insertSavedVideoInfo(info.toSavedVideoInfo())
                    mutableViewStateFlow.update {
                        it.copy(isLoading = false, result = info, errorMessage = null, isSaved = true)
                    }
                }
                .onFailure { th ->
                    mutableViewStateFlow.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = th.message ?: "Failed to fetch video info",
                        )
                    }
                }
        }
    }

    fun deleteSavedInfo(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { DatabaseUtil.deleteSavedVideoInfoById(id) }
    }

    fun deleteSavedInfoList(ids: Set<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id -> DatabaseUtil.deleteSavedVideoInfoById(id) }
        }
    }

    fun clearResult() {
        mutableViewStateFlow.update {
            it.copy(result = null, errorMessage = null, isSaved = false)
        }
    }
}

private fun VideoInfo.toSavedVideoInfo(): SavedVideoInfo =
    SavedVideoInfo(
        title = title.ifBlank { "Untitled video" },
        description = description.orEmpty(),
        tagsJson = SavedVideoInfo.encodeTags(tags.orEmpty()),
        videoUrl = webpageUrl ?: originalUrl.orEmpty(),
        thumbnailUrl = thumbnail.orEmpty(),
        uploader = uploader ?: channel.orEmpty(),
        uploadDate = uploadDate.orEmpty(),
        durationString = durationString ?: duration?.let { formatDuration(it) }.orEmpty(),
        viewCount = viewCount ?: -1L,
        likeCount = likeCount ?: -1,
        extractor = extractor.orEmpty(),
        savedAtMillis = System.currentTimeMillis(),
    )

private fun formatDuration(seconds: Double): String {
    val totalSeconds = seconds.toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs)
    else "%d:%02d".format(minutes, secs)
}
