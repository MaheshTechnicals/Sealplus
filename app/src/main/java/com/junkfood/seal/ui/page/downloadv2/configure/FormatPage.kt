package com.junkfood.seal.ui.page.downloadv2.configure

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.TaskFactory
import com.junkfood.seal.ui.component.ClearButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.FormatItem
import com.junkfood.seal.ui.component.FormatSubtitle
import com.junkfood.seal.ui.component.FormatVideoPreview
import com.junkfood.seal.ui.component.PreferenceInfo
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.ui.component.SealSearchBar
import com.junkfood.seal.ui.component.SuggestedFormatItem
import com.junkfood.seal.ui.component.TextButtonWithIcon
import com.junkfood.seal.ui.component.VideoFilterChip
import com.junkfood.seal.ui.page.download.VideoClipDialog
import com.junkfood.seal.ui.page.download.VideoSelectionSlider
import com.junkfood.seal.ui.page.settings.general.DialogCheckBoxItem
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.ui.theme.generateLabelColor
import com.junkfood.seal.util.EXTRACT_AUDIO
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.MERGE_MULTI_AUDIO_STREAM
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.SUBTITLE
import com.junkfood.seal.util.SUBTITLE_LANGUAGE
import com.junkfood.seal.util.SubtitleFormat
import com.junkfood.seal.util.VIDEO_CLIP
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.toHttpsUrl
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.pow
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val TAG = "FormatPage"

/**
 * Extracts resolution (width x height) from format string.
 * Format strings typically look like: "616 - 1920x1080 (Premium)" or "2480x1920"
 * Returns a Pair of (width, height) or null if no resolution found.
 */
private fun extractResolution(format: Format): Pair<Int, Int>? {
    // Try to extract from format string first (e.g., "1920x1080")
    val formatString = format.format ?: ""
    val resolutionRegex = """(\d{3,4})x(\d{3,4})""".toRegex()
    val resolutionMatch = resolutionRegex.find(formatString)
    if (resolutionMatch != null) {
        val width = resolutionMatch.groupValues[1].toIntOrNull()
        val height = resolutionMatch.groupValues[2].toIntOrNull()
        if (width != null && height != null && width > 0 && height > 0) {
            return Pair(width, height)
        }
    }
    
    // Try to extract resolution from height if available (common in YouTube formats)
    // YouTube format strings often contain height in format like "1080p"
    val heightPattern = """(\d{3,4})p""".toRegex()
    val heightMatch = heightPattern.find(formatString)
    if (heightMatch != null) {
        val height = heightMatch.groupValues[1].toIntOrNull()
        if (height != null && height > 0) {
            // Estimate width based on common aspect ratios (16:9 is most common)
            val width = (height * 16.0 / 9.0).toInt()
            return Pair(width, height)
        }
    }
    
    // Use fileSizeApprox as a fallback indicator (larger file = higher quality)
    // This is not ideal but better than nothing when resolution can't be extracted
    return null
}

/**
 * Calculate a quality score based on resolution.
 * Higher score = better quality.
 */
private fun getQualityScore(format: Format): Int {
    val resolution = extractResolution(format)
    if (resolution != null) {
        // Use area (width × height) as quality score
        return resolution.first * resolution.second
    }
    
    // Fallback: use fileSizeApprox if available
    val fileSize = format.fileSizeApprox ?: format.fileSize ?: 0.0
    return fileSize.toInt()
}

private data class FormatConfig(
    val formatList: List<Format>,
    val videoClips: List<VideoClip>,
    val splitByChapter: Boolean,
    val newTitle: String,
    val selectedSubtitles: List<String>,
    val selectedAutoCaptions: List<String>,
)

@Composable
fun FormatPage(
    modifier: Modifier = Modifier,
    videoInfo: VideoInfo,
    downloader: DownloaderV2 = koinInject(),
    onNavigateBack: () -> Unit = {},
) {
    if (videoInfo.formats.isNullOrEmpty()) return
    val audioOnly = EXTRACT_AUDIO.getBoolean()
    val mergeAudioStream = MERGE_MULTI_AUDIO_STREAM.getBoolean()
    val subtitleLanguageRegex = SUBTITLE_LANGUAGE.getString()
    val downloadSubtitle = SUBTITLE.getBoolean()
    val initialSelectedSubtitles =
        if (downloadSubtitle) {
            videoInfo
                .run { subtitles.keys + automaticCaptions.keys }
                .filterWithRegex(subtitleLanguageRegex)
        } else {
            emptySet()
        }

    var showUpdateSubtitleDialog by remember { mutableStateOf(false) }

    var diffSubtitleLanguages by remember { mutableStateOf(emptySet<String>()) }

    FormatPageImpl(
        modifier = modifier,
        videoInfo = videoInfo,
        onNavigateBack = onNavigateBack,
        audioOnly = audioOnly,
        mergeAudioStream = !audioOnly && mergeAudioStream,
        selectedSubtitleCodes = initialSelectedSubtitles,
        isClippingAvailable = VIDEO_CLIP.getBoolean() && (videoInfo.duration ?: .0) >= 0,
    ) { config ->
        with(config) {
            diffSubtitleLanguages =
                (selectedSubtitles + selectedAutoCaptions)
                    .run { this - this.filterWithRegex(subtitleLanguageRegex) }
                    .toSet()

            downloader.enqueue(
                TaskFactory.createWithConfigurations(
                    videoInfo = videoInfo,
                    formatList = formatList,
                    videoClips = videoClips,
                    splitByChapter = splitByChapter,
                    newTitle = newTitle,
                    selectedSubtitles = selectedSubtitles,
                    selectedAutoCaptions = selectedAutoCaptions,
                )
            )

            if (diffSubtitleLanguages.isNotEmpty()) {
                showUpdateSubtitleDialog = true
            } else {
                onNavigateBack()
            }
        }
    }
    if (showUpdateSubtitleDialog) {
        UpdateSubtitleLanguageDialog(
            modifier = Modifier,
            languages = diffSubtitleLanguages,
            onDismissRequest = {
                showUpdateSubtitleDialog = false
                onNavigateBack()
            },
            onConfirm = {
                SUBTITLE_LANGUAGE.updateString(
                    (diffSubtitleLanguages + subtitleLanguageRegex).joinToString(separator = ",") {
                        it
                    }
                )
                showUpdateSubtitleDialog = false
                onNavigateBack()
            },
        )
    }
}

private const val NOT_SELECTED = -1

@Preview
@Composable
fun FormatPagePreview() {
    val captionsMap =
        mapOf(
            "en-en" to listOf(SubtitleFormat(ext = "", url = "", name = "English from English")),
            "ja-en" to listOf(SubtitleFormat(ext = "", url = "", name = "Japanese from English")),
            "zh-Hans-en" to
                listOf(
                    SubtitleFormat(ext = "", url = "", name = "Chinese (Simplified) from English")
                ),
            "zh-Hant-en" to
                listOf(
                    SubtitleFormat(ext = "", url = "", name = "Chinese (Traditional) from English")
                ),
        )

    val subMap = buildMap {
        put("en", listOf(SubtitleFormat(ext = "ass", url = "", name = "English")))
        put("ja", listOf(SubtitleFormat(ext = "ass", url = "", name = "Japanese")))
    }
    val videoInfo =
        VideoInfo(
            formats =
                buildList {
                    repeat(7) { add(Format(formatId = "$it")) }
                    repeat(7) { add(Format(formatId = "$it", vcodec = "avc1", acodec = "none")) }
                    repeat(7) {
                        add(
                            Format(
                                formatId = "$it",
                                acodec = "aac",
                                vcodec = "none",
                                format = "251 - audio only (medium)",
                                fileSizeApprox = 2000000.0,
                                tbr = 128.0,
                            )
                        )
                    }
                },
            subtitles = subMap,
            automaticCaptions = captionsMap,
            requestedFormats =
                buildList {
                    add(
                        Format(
                            formatId = "616",
                            format = "616 - 1920x1080 (Premium)",
                            acodec = "none",
                            vcodec = "vp09.00.40.08",
                            ext = "webm",
                        )
                    )
                    add(
                        Format(
                            formatId = "251",
                            format = "251 - audio only (medium)",
                            acodec = "opus",
                            vcodec = "none",
                            ext = "webm",
                        )
                    )
                },
            duration = 180.0,
        )
    SealTheme {
        FormatPageImpl(
            videoInfo = videoInfo,
            isClippingAvailable = true,
            mergeAudioStream = true,
            selectedSubtitleCodes = setOf("en", "ja-en"),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatPageImpl(
    modifier: Modifier = Modifier,
    videoInfo: VideoInfo = VideoInfo(),
    audioOnly: Boolean = false,
    mergeAudioStream: Boolean = false,
    isClippingAvailable: Boolean = false,
    selectedSubtitleCodes: Set<String>,
    onNavigateBack: () -> Unit = {},
    onDownloadPressed: (FormatConfig) -> Unit = { _ -> },
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    if (videoInfo.formats.isNullOrEmpty()) return
    val videoOnlyFormats =
        videoInfo.formats.filter { it.vcodec != "none" && it.acodec == "none" }.reversed()
    val audioOnlyFormats =
        videoInfo.formats.filter { it.acodec != "none" && it.vcodec == "none" }.reversed()
    // Original video+audio combined formats (usually lower quality like 360p, 480p)
    val videoAudioFormats =
        videoInfo.formats.filter { it.acodec != "none" && it.vcodec != "none" }.reversed()
    
    // Get the best audio format to auto-merge with video-only formats
    val bestAudioFormat = audioOnlyFormats.firstOrNull()
    
    // Create merged video formats: video-only + best audio
    // This allows high-quality videos (720p, 1080p, 4K) to appear in Video section
    val mergedVideoFormats = if (!audioOnly && bestAudioFormat != null) {
        videoOnlyFormats.map { videoFormat ->
            // Store reference to both formats for later merging
            videoFormat.copy(
                // Mark this as requiring audio merge (yt-dlp will handle it)
                format = "${videoFormat.format} + audio",
                // Keep original properties but indicate it will have audio
                acodec = bestAudioFormat.acodec ?: "audio"
            )
        }
    } else emptyList()
    
    // Combine original video+audio formats with new merged formats
    // Sort ALL video formats by quality score (highest resolution first)
    val allVideoFormats = remember(videoInfo, audioOnly, mergedVideoFormats, videoAudioFormats) {
        if (audioOnly) {
            emptyList()
        } else {
            val combined = (mergedVideoFormats + videoAudioFormats).distinctBy { it.formatId }
            // Sort by quality score (highest first) - this ensures best quality appears first
            combined.sortedByDescending { getQualityScore(it) }
        }
    }
    
    // Find highest resolution format from allVideoFormats for suggested section
    // This will be the best quality format based on resolution sorting
    val highestVideoFormat = remember(videoInfo, audioOnly, allVideoFormats) {
        if (!audioOnly && allVideoFormats.isNotEmpty()) {
            allVideoFormats.firstOrNull() // Already sorted by quality, highest quality first
        } else {
            null
        }
    }

    val duration = videoInfo.duration ?: 0.0

    var videoOnlyItemLimit by remember { mutableIntStateOf(6) }
    var audioOnlyItemLimit by remember { mutableIntStateOf(6) }
    // Show all video formats by default (including merged high-quality ones)
    var videoAudioItemLimit by remember { mutableIntStateOf(Int.MAX_VALUE) }

    val isSuggestedFormatAvailable =
        !videoInfo.requestedFormats.isNullOrEmpty() || !videoInfo.requestedDownloads.isNullOrEmpty()

    var isSuggestedFormatSelected by remember { mutableStateOf(isSuggestedFormatAvailable) }

    var selectedVideoAudioFormat by remember { mutableIntStateOf(NOT_SELECTED) }
    var selectedVideoOnlyFormat by remember { mutableIntStateOf(NOT_SELECTED) }
    val selectedAudioOnlyFormats = remember { mutableStateListOf<Int>() }
    val context = LocalContext.current

    val uriHandler = LocalUriHandler.current
    val hapticFeedback = LocalHapticFeedback.current

    fun String?.share() =
        this?.let {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            context.startActivity(
                Intent.createChooser(
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, it)
                    },
                    null,
                ),
                null,
            )
        }

    var isClippingVideo by remember { mutableStateOf(false) }
    var isSplittingVideo by remember { mutableStateOf(false) }
    val isSplitByChapterAvailable = !videoInfo.chapters.isNullOrEmpty()

    val videoDurationRange = 0f..(videoInfo.duration?.toFloat() ?: 0f)
    var showVideoClipDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSubtitleSelectionDialog by remember { mutableStateOf(false) }

    var videoClipDuration by remember { mutableStateOf(videoDurationRange) }
    var videoTitle by remember { mutableStateOf("") }

    val suggestedSubtitleMap: Map<String, List<SubtitleFormat>> =
        videoInfo.subtitles.takeIf { it.isNotEmpty() }
            ?: videoInfo.automaticCaptions.filterKeys { it.endsWith("-orig") }

    val otherSubtitleMap: Map<String, List<SubtitleFormat>> =
        videoInfo.subtitles + videoInfo.automaticCaptions - suggestedSubtitleMap.keys

    LaunchedEffect(isClippingVideo) {
        delay(200)
        videoClipDuration = videoDurationRange
    }

    val lazyGridState = rememberLazyGridState()

    val formatList: List<Format> by remember {
        derivedStateOf {
            mutableListOf<Format>().apply {
                if (isSuggestedFormatSelected) {
                    // Use highest format from allVideoFormats (merged format with audio)
                    if (highestVideoFormat != null && !audioOnly) {
                        // Check if it's a merged format
                        val isMergedFormat = mergedVideoFormats.any { it.formatId == highestVideoFormat.formatId }
                        
                        if (isMergedFormat && bestAudioFormat != null) {
                            // It's a merged format - add video-only + audio
                            val originalVideoFormat = videoOnlyFormats.find { 
                                it.formatId == highestVideoFormat.formatId 
                            }
                            if (originalVideoFormat != null) {
                                add(originalVideoFormat)
                                add(bestAudioFormat)
                            }
                        } else {
                            // It's an original combined format, add as-is
                            add(highestVideoFormat)
                        }
                    } else {
                        // Fallback to original requested formats
                        videoInfo.requestedFormats?.let { addAll(it) }
                            ?: videoInfo.requestedDownloads?.forEach {
                                it.requestedFormats?.let { addAll(it) }
                            }
                    }
                } else {
                    selectedAudioOnlyFormats.forEach { index ->
                        add(audioOnlyFormats.elementAt(index))
                    }
                    // Handle merged video formats (video-only + audio)
                    allVideoFormats.getOrNull(selectedVideoAudioFormat)?.let { selectedFormat ->
                        // Check if this is a merged format by checking if it exists in mergedVideoFormats
                        val isMergedFormat = mergedVideoFormats.any { it.formatId == selectedFormat.formatId }
                        
                        if (isMergedFormat && bestAudioFormat != null) {
                            // This is a merged format - add original video-only + best audio
                            val originalVideoFormat = videoOnlyFormats.find { 
                                it.formatId == selectedFormat.formatId 
                            }
                            if (originalVideoFormat != null) {
                                // Add original video-only format
                                add(originalVideoFormat)
                                // Add best audio format for merging
                                add(bestAudioFormat)
                            }
                        } else {
                            // This is an original combined format, add as-is
                            add(selectedFormat)
                        }
                    }
                    videoOnlyFormats.getOrNull(selectedVideoOnlyFormat)?.let { add(it) }
                }
            }
        }
    }

    val isFabExpanded by remember { derivedStateOf { lazyGridState.firstVisibleItemIndex > 0 } }

    val selectedSubtitles = remember {
        mutableStateListOf<String>().apply { addAll(selectedSubtitleCodes) }
    }

    val selectedAutoCaptions = remember { mutableStateListOf<String>() }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.format_selection),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.close))
                    }
                },
            )
        },
        floatingActionButton = {
            val isFormatSelected = isSuggestedFormatSelected || formatList.isNotEmpty()
            if (isFormatSelected) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onDownloadPressed(
                            FormatConfig(
                                formatList = formatList,
                                videoClips =
                                    if (isClippingVideo) listOf(VideoClip(videoClipDuration))
                                    else emptyList(),
                                splitByChapter = isSplittingVideo,
                                newTitle = videoTitle,
                                selectedSubtitles = selectedSubtitles,
                                selectedAutoCaptions = selectedAutoCaptions,
                            )
                        )
                    },
                    modifier = Modifier.padding(12.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    text = { Text(stringResource(R.string.start_download)) },
                    expanded = isFabExpanded,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { paddingValues ->
        LazyVerticalGrid(
            modifier = Modifier.padding(paddingValues),
            state = lazyGridState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            videoInfo.run {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FormatVideoPreview(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        title = videoTitle.ifEmpty { title },
                        author = uploader ?: channel ?: uploaderId.toString(),
                        thumbnailUrl = thumbnail.toHttpsUrl(),
                        duration = duration.roundToInt(),
                        isClippingVideo = isClippingVideo,
                        isSplittingVideo = isSplittingVideo,
                        isClippingAvailable = isClippingAvailable,
                        isSplitByChapterAvailable = isSplitByChapterAvailable,
                        onClippingToggled = { isClippingVideo = !isClippingVideo },
                        onSplittingToggled = { isSplittingVideo = !isSplittingVideo },
                        onRename = { showRenameDialog = true },
                        onOpenThumbnail = { uriHandler.openUri(thumbnail.toHttpsUrl()) },
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                var shouldUpdateClipDuration by remember { mutableStateOf(false) }

                Column {
                    AnimatedVisibility(visible = isClippingVideo) {
                        Column {
                            val state =
                                remember(isClippingVideo, showVideoClipDialog) {
                                    RangeSliderState(
                                        activeRangeStart = videoClipDuration.start,
                                        activeRangeEnd = videoClipDuration.endInclusive,
                                        valueRange = videoDurationRange,
                                        onValueChangeFinished = { shouldUpdateClipDuration = true },
                                    )
                                }
                            DisposableEffect(shouldUpdateClipDuration) {
                                videoClipDuration = state.activeRangeStart..state.activeRangeEnd
                                onDispose { shouldUpdateClipDuration = false }
                            }

                            VideoSelectionSlider(
                                modifier = Modifier.fillMaxWidth(),
                                state = state,
                                onDiscard = { isClippingVideo = false },
                                onDurationClick = { showVideoClipDialog = true },
                            )
                            androidx.compose.material3.HorizontalDivider()
                        }
                    }

                    AnimatedVisibility(visible = isSplittingVideo) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.split_video_msg,
                                            videoInfo.chapters?.size ?: 0,
                                        ),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                TextButtonWithIcon(
                                    onClick = { isSplittingVideo = false },
                                    icon = Icons.Outlined.Delete,
                                    text = stringResource(id = R.string.discard),
                                    contentColor = MaterialTheme.colorScheme.error,
                                )
                            }
                            androidx.compose.material3.HorizontalDivider()
                        }
                    }
                }
            }

            if (suggestedSubtitleMap.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 16.dp).padding(horizontal = 12.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.subtitle_language),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )

                            ClickableTextAction(
                                visible = true,
                                text =
                                    stringResource(
                                        id =
                                            androidx.appcompat.R.string
                                                .abc_activity_chooser_view_see_all
                                    ),
                            ) {
                                showSubtitleSelectionDialog = true
                            }
                        }

                        LazyRow(
                            modifier = Modifier.padding(top = 8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for ((code, formats) in suggestedSubtitleMap) {
                                item {
                                    VideoFilterChip(
                                        selected = selectedSubtitles.contains(code),
                                        onClick = {
                                            if (selectedSubtitles.contains(code)) {
                                                selectedSubtitles.remove(code)
                                            } else {
                                                selectedSubtitles.add(code)
                                            }
                                        },
                                        label = formats.first().run { name ?: protocol ?: code },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isSuggestedFormatAvailable) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.padding(top = 20.dp).padding(horizontal = 12.dp),
                    ) {
                        FormatSubtitle(
                            text = stringResource(R.string.suggested),
                            modifier = Modifier.padding(vertical = 8.dp),
                            showDivider = true,
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val onClick = {
                        isSuggestedFormatSelected = true
                        selectedAudioOnlyFormats.clear()
                        selectedVideoAudioFormat = NOT_SELECTED
                        selectedVideoOnlyFormat = NOT_SELECTED
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Display the highest format from allVideoFormats (merged format)
                        val displayFormat = if (highestVideoFormat != null && !audioOnly) {
                            // Check if it's a merged format to show proper info
                            val isMergedFormat = mergedVideoFormats.any { it.formatId == highestVideoFormat.formatId }
                            
                            if (isMergedFormat && bestAudioFormat != null) {
                                // Show as merged format (video + audio)
                                val originalVideoFormat = videoOnlyFormats.find { 
                                    it.formatId == highestVideoFormat.formatId 
                                }
                                listOfNotNull(originalVideoFormat, bestAudioFormat)
                            } else {
                                // Original combined format
                                listOf(highestVideoFormat)
                            }
                        } else null
                        
                        SuggestedFormatItem(
                            modifier = Modifier.weight(1f),
                            videoInfo = videoInfo,
                            overrideFormats = displayFormat,
                            selected = isSuggestedFormatSelected,
                            onClick = onClick,
                        )
                    }
                }
            }

            // SECTION 1: Video (with audio) - Now includes high-quality merged formats
            if (allVideoFormats.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 20.dp).padding(horizontal = 12.dp),
                    ) {
                        FormatSubtitle(
                            text = stringResource(R.string.video),
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            showDivider = true,
                        )
                        ClickableTextAction(
                            visible = videoAudioItemLimit < allVideoFormats.size,
                            text = stringResource(R.string.show_all_items, allVideoFormats.size),
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            videoAudioItemLimit = Int.MAX_VALUE
                        }
                    }
                }
                itemsIndexed(
                    allVideoFormats.subList(0, min(videoAudioItemLimit, allVideoFormats.size))
                ) { index, formatInfo ->
                    FormatItem(
                        formatInfo = formatInfo,
                        duration = duration,
                        selected = selectedVideoAudioFormat == index,
                        onLongClick = { formatInfo.url.share() },
                    ) {
                        selectedVideoAudioFormat =
                            if (selectedVideoAudioFormat == index) NOT_SELECTED
                            else {
                                selectedAudioOnlyFormats.clear()
                                selectedVideoOnlyFormat = NOT_SELECTED
                                isSuggestedFormatSelected = false
                                index
                            }
                    }
                }
            }

            // SECTION 2: Audio
            if (audioOnlyFormats.isNotEmpty())
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 20.dp).padding(horizontal = 12.dp),
                    ) {
                        FormatSubtitle(
                            text = stringResource(R.string.audio),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            showDivider = true,
                        )

                        ClickableTextAction(
                            visible = audioOnlyItemLimit < audioOnlyFormats.size,
                            text = stringResource(R.string.show_all_items, audioOnlyFormats.size),
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            audioOnlyItemLimit = Int.MAX_VALUE
                        }
                    }
                }

            itemsIndexed(
                audioOnlyFormats.subList(
                    fromIndex = 0,
                    toIndex = min(audioOnlyItemLimit, audioOnlyFormats.size),
                )
            ) { index, formatInfo ->
                FormatItem(
                    formatInfo = formatInfo,
                    duration = duration,
                    selected = selectedAudioOnlyFormats.contains(index),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    outlineColor = MaterialTheme.colorScheme.secondary,
                    onLongClick = { formatInfo.url.share() },
                ) {
                    if (selectedAudioOnlyFormats.contains(index)) {
                        selectedAudioOnlyFormats.remove(index)
                    } else {
                        if (!mergeAudioStream) {
                            selectedAudioOnlyFormats.clear()
                        }
                        isSuggestedFormatSelected = false
                        selectedAudioOnlyFormats.add(index)
                    }
                }
            }

            // SECTION 3: Video (no audio)
            if (!audioOnly) {
                if (videoOnlyFormats.isNotEmpty())
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 20.dp).padding(horizontal = 12.dp),
                        ) {
                            FormatSubtitle(
                                text = stringResource(R.string.video_only),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                                showDivider = true,
                            )

                            ClickableTextAction(
                                visible = videoOnlyItemLimit < videoOnlyFormats.size,
                                text =
                                    stringResource(R.string.show_all_items, videoOnlyFormats.size),
                            ) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                videoOnlyItemLimit = Int.MAX_VALUE
                            }
                        }
                    }
                itemsIndexed(
                    videoOnlyFormats.subList(0, min(videoOnlyItemLimit, videoOnlyFormats.size))
                ) { index, formatInfo ->
                    FormatItem(
                        formatInfo = formatInfo,
                        duration = duration,
                        selected = selectedVideoOnlyFormat == index,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        outlineColor = MaterialTheme.colorScheme.tertiary,
                        onLongClick = { formatInfo.url.share() },
                    ) {
                        selectedVideoOnlyFormat =
                            if (selectedVideoOnlyFormat == index) NOT_SELECTED
                            else {
                                selectedVideoAudioFormat = NOT_SELECTED
                                isSuggestedFormatSelected = false
                                index
                            }
                    }
                }
            }

            if (!audioOnly && audioOnlyFormats.isNotEmpty() && videoOnlyFormats.isNotEmpty())
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PreferenceInfo(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        text = stringResource(R.string.abs_hint),
                        applyPaddings = false,
                    )
                }
            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
    if (showVideoClipDialog)
        VideoClipDialog(
            onDismissRequest = { showVideoClipDialog = false },
            initialValue = videoClipDuration,
            valueRange = videoDurationRange,
            onConfirm = { videoClipDuration = it },
        )

    if (showRenameDialog)
        RenameDialog(
            initialValue = videoTitle.ifEmpty { videoInfo.title },
            onDismissRequest = { showRenameDialog = false },
        ) {
            videoTitle = it
        }
    if (showSubtitleSelectionDialog)
        SubtitleSelectionDialog(
            suggestedSubtitles = suggestedSubtitleMap,
            autoCaptions = otherSubtitleMap,
            selectedSubtitles = selectedSubtitles,
            onDismissRequest = { showSubtitleSelectionDialog = false },
            onConfirm = { subs, autoSubs ->
                selectedSubtitles.run {
                    clear()
                    addAll(subs)
                }

                showSubtitleSelectionDialog = false
            },
        )
}

@Composable
private fun RenameDialog(
    initialValue: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var filename by remember { mutableStateOf(initialValue) }
    SealDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton {
                onConfirm(filename)
                onDismissRequest()
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        title = { Text(text = stringResource(id = R.string.rename)) },
        icon = { Icon(imageVector = Icons.Outlined.Edit, contentDescription = null) },
        text = {
            Column {
                OutlinedTextField(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text(text = stringResource(id = R.string.title)) },
                    trailingIcon = { if (filename == initialValue) ClearButton { filename = "" } },
                )
            }
        },
    )
}

private fun (Map<String, List<SubtitleFormat>>).filterWithSearchText(
    searchText: String
): Map<String, List<SubtitleFormat>> {
    return this.filter {
        it.run {
            searchText.isBlank() ||
                key.contains(searchText, ignoreCase = true) ||
                value.any { format ->
                    format.name?.contains(searchText, ignoreCase = true) ?: false
                }
        }
    }
}

private fun Map<String, List<SubtitleFormat>>.sortedWithSelection(
    selectedKeys: List<String>
): Map<String, List<SubtitleFormat>> {
    return this.toList()
        .sortedWith { entry1, entry2 ->
            when {
                entry1.first in selectedKeys && entry2.first in selectedKeys ->
                    entry1.compareTo(entry2) // Both in selectedKeys - equal priority
                entry1.first in selectedKeys -> -1 // str1 has priority
                entry2.first in selectedKeys -> 1 // str2 has priority
                else -> entry1.compareTo(entry2)
            }
        }
        .toMap()
}

/**
 * Prioritizes comparison of subtitle names (via `getSubtitleName()`) if available, otherwise
 * compares the `key` portion of the pairs.
 *
 * Examples: `zh` (Chinese) should be greater than `en` (English) according to their names
 */
private fun (Pair<String, List<SubtitleFormat>>).compareTo(
    other: (Pair<String, List<SubtitleFormat>>)
): Int {
    val (key, list) = this
    val (otherKey, otherList) = other

    val name = list.getSubtitleName()
    val otherName = otherList.getSubtitleName()

    return if (name != null && otherName != null) {
        name.compareTo(otherName)
    } else {
        key.compareTo(otherKey)
    }
}

private fun (List<SubtitleFormat>).getSubtitleName(): String? = firstOrNull()?.name

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubtitleSelectionDialog(
    suggestedSubtitles: Map<String, List<SubtitleFormat>>,
    autoCaptions: Map<String, List<SubtitleFormat>>,
    selectedSubtitles: List<String>,
    onDismissRequest: () -> Unit = {},
    onConfirm: (subs: List<String>, autoSubs: List<String>) -> Unit = { _, _ -> },
) {
    var searchText by remember { mutableStateOf("") }
    val selectedSubtitles = remember {
        mutableStateListOf<String>().apply { addAll(selectedSubtitles) }
    }
    val selectedAutoCaptions = remember { mutableStateListOf<String>() }

    val suggestedSubtitlesFiltered =
        suggestedSubtitles.filterWithSearchText(searchText).sortedWithSelection(selectedSubtitles)
    val autoCaptionsFiltered =
        autoCaptions.filterWithSearchText(searchText).sortedWithSelection(selectedSubtitles)

    SealDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onDismissRequest = onDismissRequest,
        confirmButton = { ConfirmButton { onConfirm(selectedSubtitles, selectedAutoCaptions) } },
        dismissButton = { DismissButton { onDismissRequest() } },
        title = { Text(text = stringResource(id = R.string.subtitle_language)) },
        icon = { Icon(imageVector = Icons.Outlined.Subtitles, contentDescription = null) },
        text = {
            Column {
                if (autoCaptions.size + suggestedSubtitles.size > 5) {
                    SealSearchBar(
                        text = searchText,
                        placeholderText = stringResource(R.string.search_in_subtitles),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        searchText = it
                    }
                }

                LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                    if (suggestedSubtitlesFiltered.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.suggested),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            )
                        }
                    }
                    for ((code, formats) in suggestedSubtitlesFiltered) {
                        item(key = code) {
                            DialogCheckBoxItem(
                                modifier = Modifier.animateItem(),
                                checked = selectedSubtitles.contains(code),
                                onValueChange = {
                                    if (selectedSubtitles.contains(code)) {
                                        selectedSubtitles.remove(code)
                                    } else {
                                        selectedSubtitles.add(code)
                                    }
                                },
                                text = formats.first().run { name ?: protocol ?: code },
                            )
                        }
                    }

                    if (autoCaptionsFiltered.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.auto_subtitle),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            )
                        }
                        for ((code, formats) in autoCaptionsFiltered) {
                            item(key = code) {
                                DialogCheckBoxItem(
                                    modifier = Modifier.animateItem(),
                                    checked = selectedAutoCaptions.contains(code),
                                    onValueChange = {
                                        if (selectedAutoCaptions.contains(code)) {
                                            selectedAutoCaptions.remove(code)
                                        } else {
                                            selectedAutoCaptions.add(code)
                                        }
                                    },
                                    text = formats.first().run { name ?: protocol ?: code },
                                )
                            }
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider()
            }
        },
    )
}

@Preview
@Composable
private fun SubtitleSelectionDialogPreview() {
    val captionsMap =
        mapOf(
            "en-en" to listOf(SubtitleFormat(ext = "", url = "", name = "English from English")),
            "ja-en" to listOf(SubtitleFormat(ext = "", url = "", name = "Japanese from English")),
            "zh-Hans-en" to
                listOf(
                    SubtitleFormat(ext = "", url = "", name = "Chinese (Simplified) from English")
                ),
            "zh-Hant-en" to
                listOf(
                    SubtitleFormat(ext = "", url = "", name = "Chinese (Traditional) from English")
                ),
        )

    val subMap = buildMap {
        put("en", listOf(SubtitleFormat(ext = "ass", url = "", name = "English")))
        put("ja", listOf(SubtitleFormat(ext = "ass", url = "", name = "Japanese")))
    }

    SealTheme {
        SubtitleSelectionDialog(
            suggestedSubtitles = subMap,
            autoCaptions = captionsMap,
            selectedSubtitles = listOf(),
        )
    }
}

@Composable
private fun ClickableTextAction(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AnimatedVisibility(visible = visible, exit = fadeOut(animationSpec = spring())) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelLarge,
            modifier =
                modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onClick)
                    .padding(vertical = 6.dp, horizontal = 12.dp),
        )
    }
}

fun <T : Collection<String>> T.filterWithRegex(subtitleLanguageRegex: String): Set<String> {
    val regexGroup = subtitleLanguageRegex.split(',')
    return filter { language -> regexGroup.any { Regex(it).matchEntire(language) != null } }.toSet()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Preview
fun UpdateSubtitleLanguageDialog(
    modifier: Modifier = Modifier,
    languages: Set<String> = setOf("en", "ja"),
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.update_subtitle_languages),
                textAlign = TextAlign.Center,
            )
        },
        icon = { Icon(imageVector = Icons.Filled.Subtitles, contentDescription = null) },
        text = {
            Column {
                Text(text = stringResource(R.string.update_language_msg))

                Spacer(modifier = Modifier.height(24.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    languages.forEach {
                        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier.padding(end = 8.dp)
                                        .size(16.dp)
                                        .background(
                                            color = it.hashCode().generateLabelColor(),
                                            shape = CircleShape,
                                        )
                                        .clearAndSetSemantics {}
                            ) {}
                            Text(
                                text = it,
                                modifier = Modifier,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(text = stringResource(id = R.string.okay)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.no_thanks))
            }
        },
    )
}
