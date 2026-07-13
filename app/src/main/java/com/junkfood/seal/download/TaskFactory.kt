package com.junkfood.seal.download

import androidx.annotation.CheckResult
import com.junkfood.seal.download.Task.DownloadState.Idle
import com.junkfood.seal.download.Task.DownloadState.ReadyWithInfo
import com.junkfood.seal.util.DownloadUtil.DownloadPreferences
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.Format.Companion.getEffectiveLanguage
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
        selectedAudioLanguages: List<String> = emptyList(),
        downloadAllAudio: Boolean = false,
        overridePreferences: DownloadPreferences? = null,
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

        fun Format.safeId(): String = formatId ?: ""

        // Multi-audio: when specific languages selected or all audio requested, build language-filtered format
        val hasMultiAudio = selectedAudioLanguages.isNotEmpty() || downloadAllAudio
        val mergeAudioStream = when {
            hasMultiAudio -> true
            else -> audioOnlyFormats.size > 1
        }
        val formatId = when {
            downloadAllAudio -> {
                // Group ALL available audio-only formats by language, pick best quality per language
                val allAudioFormats = videoInfo.formats
                    ?.filter { it.isAudioOnly() && !it.formatId.isNullOrBlank() }
                    ?: emptyList()
                val audioByLanguage = allAudioFormats
                    .mapNotNull { format ->
                        val lang = (getEffectiveLanguage(format) ?: return@mapNotNull null).lowercase()
                        lang to format
                    }
                    .groupBy { it.first }
                    .mapValues { (_, entries) -> entries.map { it.second } }

                val bestPerLanguage = audioByLanguage.values
                    .mapNotNull { formats -> formats.maxByOrNull { it.tbr ?: it.abr ?: 0.0 } }

                if (bestPerLanguage.isNotEmpty() && videoFormats.isNotEmpty()) {
                    val videoIds = videoFormats.mapNotNull { it.safeId().takeIf(String::isNotEmpty) }
                        .joinToString("+")
                    val audioIds = bestPerLanguage.mapNotNull { it.safeId().takeIf(String::isNotEmpty) }
                        .joinToString("+")
                    if (videoIds.isEmpty()) audioIds else "$videoIds+$audioIds"
                } else if (bestPerLanguage.isNotEmpty()) {
                    bestPerLanguage.mapNotNull { it.safeId().takeIf(String::isNotEmpty) }
                        .joinToString("+")
                } else {
                    formatList.mapNotNull { it.safeId().takeIf(String::isNotEmpty) }
                        .joinToString(separator = "+")
                }
            }
            selectedAudioLanguages.isNotEmpty() && videoFormats.isNotEmpty() -> {
                // Find best audio format per requested language from ALL available formats
                val allAudioFormats = videoInfo.formats
                    ?.filter { it.isAudioOnly() && !it.formatId.isNullOrBlank() }
                    ?: emptyList()
                val videoIds = videoFormats.mapNotNull { it.safeId().takeIf(String::isNotEmpty) }
                    .joinToString("+")
                val audioParts = selectedAudioLanguages.mapNotNull { lang ->
                    allAudioFormats
                        .filter { (getEffectiveLanguage(it) ?: "").lowercase() == lang.lowercase() }
                        .maxByOrNull { it.tbr ?: it.abr ?: 0.0 }
                        ?.safeId()?.takeIf(String::isNotEmpty)
                }.joinToString("+")
                if (audioParts.isEmpty()) videoIds else "$videoIds+$audioParts"
            }
            else -> formatList.mapNotNull { it.safeId().takeIf(String::isNotEmpty) }
                .joinToString(separator = "+")
        }
        
        // Check if we're merging video and audio (common for high-quality downloads)
        val isMergingVideoAudio = videoFormats.isNotEmpty() && audioOnlyFormats.isNotEmpty()

        val subtitleLanguage =
            (selectedSubtitles + selectedAutoCaptions).joinToString(separator = ",")

        val preferences =
            (overridePreferences ?: DownloadPreferences.createFromPreferences())
                .run {
                    // When merging video+audio, force MP4 output (not MKV)
                    // This ensures high-quality merged videos are in MP4 container
                    val shouldUseMp4 = if (isMergingVideoAudio) false else this.mergeToMkv
                    
                    copy(
                        formatIdString = formatId,
                        videoClips = videoClips,
                        splitByChapter = splitByChapter,
                        newTitle = newTitle,
                        mergeAudioStream = mergeAudioStream,
                        extractAudio = extractAudio || audioOnly,
                        mergeToMkv = if (hasMultiAudio) true else shouldUseMp4,
                        selectedAudioLanguages = selectedAudioLanguages,
                        downloadAllAudio = downloadAllAudio,
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
