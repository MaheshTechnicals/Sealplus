package com.junkfood.seal.download

import androidx.annotation.CheckResult
import com.junkfood.seal.download.Task.DownloadState.Idle
import com.junkfood.seal.download.Task.DownloadState.ReadyWithInfo
import com.junkfood.seal.util.DownloadUtil.DownloadPreferences
import com.junkfood.seal.util.FORMAT_COMPATIBILITY
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.PlaylistResult
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import kotlin.math.roundToInt

object TaskFactory {
    /**
     * @return A [TaskWithState] with extra configurations made by user in the custom format
     *   selection page
     */
    @CheckResult
    fun createWithConfigurations(
        videoInfo: VideoInfo,
        formatList: List<Format>,
        videoClips: List<VideoClip>,
        splitByChapter: Boolean,
        newTitle: String,
        selectedSubtitles: List<String>,
        selectedAutoCaptions: List<String>,
    ): TaskWithState {
        val fileSize =
            formatList.fold(.0) { acc, format ->
                acc + (format.fileSize ?: format.fileSizeApprox ?: .0)
            }

        val info =
            videoInfo
                .run { if (fileSize != .0) copy(fileSize = fileSize) else this }
                .run { if (newTitle.isNotEmpty()) copy(title = newTitle) else this }

        val audioOnlyFormats = formatList.filter { it.isAudioOnly() }
        val videoFormats = formatList.filter { it.containsVideo() }
        val audioOnly = audioOnlyFormats.isNotEmpty() && videoFormats.isEmpty()
        val mergeAudioStream = audioOnlyFormats.size > 1
        val formatId = formatList.joinToString(separator = "+") { it.formatId.toString() }
        
        // Check if we're merging video and audio (common for high-quality downloads)
        val isMergingVideoAudio = videoFormats.isNotEmpty() && audioOnlyFormats.isNotEmpty()
        
        // Get the video format extension and codec to determine desired output container
        val firstVideoFormat = videoFormats.firstOrNull()
        val videoExtension = firstVideoFormat?.ext?.lowercase() ?: ""
        val videoCodec = firstVideoFormat?.vcodec?.lowercase() ?: ""
        val audioCodec = audioOnlyFormats.firstOrNull()?.acodec?.lowercase() ?: ""
        
        // Determine output format based on codec compatibility
        // H.264/AVC1 video can go in MP4, but VP9/AV1 should stay in WEBM
        // Also consider audio codec: OPUS audio is better in WEBM
        // IMPORTANT: Only specify output format if we need to change the container
        val determineOutputFormat: () -> String = {
            when {
                videoExtension.isEmpty() -> ""
                // If format already has correct extension, no need to remux
                isMergingVideoAudio -> {
                    // When merging separate streams, we need to specify output container
                    when {
                        videoCodec.contains("avc") || videoCodec.contains("h264") -> {
                            if (audioCodec.contains("opus") && videoExtension != "mp4") {
                                videoExtension // Keep original if not MP4
                            } else {
                                videoExtension // Use video format's extension
                            }
                        }
                        videoCodec.contains("vp9") || videoCodec.contains("vp09") || videoCodec.contains("av01") -> "webm"
                        else -> videoExtension
                    }
                }
                // Single format with video+audio already combined - check if extension matches codec
                else -> {
                    // For pre-combined formats, only remux if absolutely necessary
                    // MP4 container with AVC1 video - keep as MP4
                    // WEBM container with VP9 video - keep as WEBM
                    when {
                        (videoCodec.contains("avc") || videoCodec.contains("h264")) && videoExtension == "webm" -> "mp4"
                        (videoCodec.contains("vp9") || videoCodec.contains("vp09") || videoCodec.contains("av01")) && videoExtension == "mp4" -> "webm"
                        else -> "" // Format is already correct, don't remux
                    }
                }
            }
        }
        
        val outputFormat = determineOutputFormat()

        val subtitleLanguage =
            (selectedSubtitles + selectedAutoCaptions).joinToString(separator = ",")

        val preferences =
            DownloadPreferences.createFromPreferences()
                .run {
                    // Use the determined output format (considering codec compatibility)
                    val finalOutputFormat = if (!this.mergeToMkv && outputFormat.isNotEmpty()) {
                        outputFormat
                    } else ""
                    
                    copy(
                        formatIdString = formatId,
                        videoClips = videoClips,
                        splitByChapter = splitByChapter,
                        newTitle = newTitle,
                        mergeAudioStream = mergeAudioStream,
                        extractAudio = extractAudio || audioOnly,
                        mergeToMkv = this.mergeToMkv,
                        mergeOutputFormat = finalOutputFormat,
                    )
                }
                .run {
                    if (subtitleLanguage.isNotEmpty()) {
                        copy(
                            downloadSubtitle = true,
                            autoSubtitle = selectedAutoCaptions.isNotEmpty(),
                            subtitleLanguage = subtitleLanguage,
                        )
                    } else {
                        this
                    }
                }

        val task = Task(url = info.originalUrl.toString(), preferences = preferences)
        val state =
            Task.State(
                downloadState = ReadyWithInfo,
                videoInfo = info,
                viewState =
                    Task.ViewState.fromVideoInfo(info = info)
                        .copy(videoFormats = videoFormats, audioOnlyFormats = audioOnlyFormats),
            )

        return TaskWithState(task, state)
    }

    /** @return List of [TaskWithState]s created from playlist items */
    @CheckResult
    fun createWithPlaylistResult(
        playlistUrl: String,
        indexList: List<Int>,
        playlistResult: PlaylistResult,
        preferences: DownloadPreferences,
    ): List<TaskWithState> {
        checkNotNull(playlistResult.entries)
        val indexEntryMap = indexList.associateWith { index -> playlistResult.entries[index - 1] }

        val taskList =
            indexEntryMap.map { (index, entry) ->
                val viewState =
                    Task.ViewState(
                        url = entry.url ?: "",
                        title = entry.title ?: "${playlistResult.title} - $index",
                        duration = entry.duration?.roundToInt() ?: 0,
                        uploader = entry.uploader ?: entry.channel ?: playlistResult.channel ?: "",
                        thumbnailUrl = (entry.thumbnails?.lastOrNull()?.url) ?: "",
                    )
                val task = Task(url = playlistUrl, preferences = preferences, type = Task.TypeInfo.Playlist(index))
                val state =
                    Task.State(downloadState = Idle, videoInfo = null, viewState = viewState)
                TaskWithState(task, state)
            }

        return taskList
    }

    data class TaskWithState(val task: Task, val state: Task.State)
}
