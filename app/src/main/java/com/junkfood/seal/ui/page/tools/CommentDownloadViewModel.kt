package com.junkfood.seal.ui.page.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.SavedCommentSet
import com.junkfood.seal.util.Comment
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

/** How many top-level+reply comments to request from yt-dlp per fetch. */
enum class CommentFetchLimit(val label: String, val count: Int) {
    SMALL("100 comments", 100),
    MEDIUM("500 comments", 500),
    LARGE("1000 comments", 1000),
    ALL("All comments", Int.MAX_VALUE),
}

/**
 * Backs the "Comment Download" tool: fetches a YouTube video's comments via yt-dlp, lets the
 * user preview and filter them (top-level vs replies, sort by top/newest), and saves the set
 * as a card the user can revisit and export (TXT/JSON/CSV) later — same shape as
 * [VideoInfoDownloadViewModel] for the sibling "Video Info Download" tool.
 */
class CommentDownloadViewModel : ViewModel() {

    private val mutableViewStateFlow = MutableStateFlow(ViewState())
    val viewStateFlow = mutableViewStateFlow.asStateFlow()

    val savedCommentSetsFlow =
        DatabaseUtil.getSavedCommentSetFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class ViewState(
        val url: String = "",
        val isLoading: Boolean = false,
        val videoTitle: String? = null,
        val uploader: String? = null,
        val thumbnailUrl: String? = null,
        val comments: List<Comment> = emptyList(),
        val errorMessage: String? = null,
        val fetchLimit: CommentFetchLimit = CommentFetchLimit.MEDIUM,
        val sortByTop: Boolean = true,
        val showRepliesOnly: Boolean = false,
        val isSaved: Boolean = false,
    ) {
        val hasResult: Boolean get() = videoTitle != null
        val topLevelCount: Int get() = comments.count { !it.isReply }
        val replyCount: Int get() = comments.count { it.isReply }

        /** Comments filtered for display per [showRepliesOnly], preserving fetch order. */
        val filteredComments: List<Comment>
            get() = if (showRepliesOnly) comments.filter { it.isReply } else comments
    }

    fun updateUrl(url: String) {
        mutableViewStateFlow.update { it.copy(url = url, errorMessage = null) }
    }

    fun updateFetchLimit(limit: CommentFetchLimit) {
        mutableViewStateFlow.update { it.copy(fetchLimit = limit) }
    }

    fun updateSortByTop(sortByTop: Boolean) {
        mutableViewStateFlow.update { it.copy(sortByTop = sortByTop) }
    }

    fun toggleShowRepliesOnly() {
        mutableViewStateFlow.update { it.copy(showRepliesOnly = !it.showRepliesOnly) }
    }

    fun fetchComments() {
        val url = viewStateFlow.value.url.trim()
        if (url.isBlank()) {
            mutableViewStateFlow.update { it.copy(errorMessage = "Please enter a video URL") }
            return
        }
        if (!isYoutubeVideoUrl(url)) {
            mutableViewStateFlow.update {
                it.copy(
                    errorMessage = App.context.getString(R.string.invalid_youtube_url),
                    comments = emptyList(),
                    videoTitle = null,
                )
            }
            return
        }
        val state = viewStateFlow.value
        mutableViewStateFlow.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                comments = emptyList(),
                videoTitle = null,
                isSaved = false,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            DownloadUtil.fetchCommentsFromUrl(
                    url = url,
                    maxComments = state.fetchLimit.count,
                    sortByTop = state.sortByTop,
                )
                .onSuccess { info -> applyResult(info, url) }
                .onFailure { th ->
                    mutableViewStateFlow.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = th.message
                                ?: App.context.getString(R.string.comments_fetch_failed),
                        )
                    }
                }
        }
    }

    // Auto-saves on a successful fetch (no extra "Save" tap required) — matches the same
    // pattern already used by the "Video Info Download" tool for a consistent feel across
    // the More Tools family.
    private suspend fun applyResult(info: VideoInfo, requestUrl: String) {
        val comments = info.comments.orEmpty()
        val title = info.title.ifBlank { "Untitled video" }
        val uploader = (info.uploader ?: info.channel).orEmpty()
        val thumbnailUrl = info.thumbnail.orEmpty()

        if (comments.isNotEmpty()) {
            DatabaseUtil.insertSavedCommentSet(
                SavedCommentSet(
                    videoTitle = title,
                    uploader = uploader,
                    videoUrl = requestUrl,
                    thumbnailUrl = thumbnailUrl,
                    commentsJson = SavedCommentSet.encodeComments(comments),
                    commentCount = comments.size,
                    savedAtMillis = System.currentTimeMillis(),
                )
            )
        }

        mutableViewStateFlow.update {
            it.copy(
                isLoading = false,
                videoTitle = title,
                uploader = uploader,
                thumbnailUrl = thumbnailUrl,
                comments = comments,
                isSaved = comments.isNotEmpty(),
                errorMessage = if (comments.isEmpty())
                    App.context.getString(R.string.no_comments_found)
                else null,
            )
        }
    }

    fun deleteSavedCommentSetById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { DatabaseUtil.deleteSavedCommentSetById(id) }
    }

    fun deleteSavedCommentSets(ids: Set<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id -> DatabaseUtil.deleteSavedCommentSetById(id) }
        }
    }

    fun clearResult() {
        mutableViewStateFlow.update {
            it.copy(
                comments = emptyList(),
                videoTitle = null,
                uploader = null,
                thumbnailUrl = null,
                errorMessage = null,
                isSaved = false,
            )
        }
    }
}
