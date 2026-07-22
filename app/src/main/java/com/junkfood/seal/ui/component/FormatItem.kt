package com.junkfood.seal.ui.component

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.connectWithBlank
import com.junkfood.seal.util.connectWithDelimiter
import com.junkfood.seal.util.toBitrateText
import com.junkfood.seal.util.toDurationText
import com.junkfood.seal.util.toFileSizeText

/**
 * Returns a short, human-readable title for a [Format] card.
 *
 * Priority:
 * 1. Explicit width × height metadata  → e.g. "1080×1920"
 * 2. `resolution` field (when not "audio only") → e.g. "1280×720"
 * 3. Resolution extracted from the raw `format` string via regex
 * 4. Audio-only formats → formatNote if set, otherwise "Audio"
 * 5. Fallback → formatId
 */
private fun Format.toDisplayTitle(): String {
    // 1. Explicit dimensions
    val w = width?.toInt()
    val h = height?.toInt()
    if (w != null && h != null && w > 0 && h > 0) return "${w}\u00d7${h}"

    // 2. resolution field
    val res = resolution?.trim()
    if (!res.isNullOrBlank() && !res.equals("audio only", ignoreCase = true)) return res

    // 3. Extract "WxH" from the raw format string
    val raw = format ?: ""
    val match = """(\d{3,4})x(\d{3,4})""".toRegex().find(raw)
    if (match != null) return "${match.groupValues[1]}\u00d7${match.groupValues[2]}"

    // 4. Audio-only
    if (isAudioOnly()) return formatNote?.takeIf { it.isNotBlank() } ?: "Audio"

    // 5. Fallback
    return formatId ?: raw.ifBlank { "Unknown" }
}

@Composable
fun FormatVideoPreview(
    modifier: Modifier = Modifier,
    title: String,
    author: String,
    thumbnailUrl: String,
    duration: Int,
    isSplittingVideo: Boolean,
    isClippingVideo: Boolean,
    isClippingAvailable: Boolean = false,
    isSplitByChapterAvailable: Boolean = false,
    onRename: () -> Unit = {},
    onOpenThumbnail: () -> Unit = {},
    onClippingToggled: () -> Unit = {},
    onSplittingToggled: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = false),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier) {
                    MediaImage(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        imageModel = thumbnailUrl,
                        isAudio = false,
                        contentDescription = stringResource(id = R.string.thumbnail),
                    )
                    Surface(
                        modifier = Modifier.padding(4.dp).align(Alignment.BottomEnd),
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        val durationText = duration.toDurationText()
                        Text(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(end = 32.dp),
                    verticalArrangement = Arrangement.Top,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (author != "playlist" && author != "null")
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp).padding(top = 4.dp),
                            text = author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                }
            }
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                Surface(
                    onClick = { expanded = true },
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(id = R.string.show_more_actions),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                DropdownMenu(
                    modifier = Modifier.align(Alignment.TopEnd),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    scrollState = rememberScrollState(),
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                        text = { Text(text = stringResource(id = R.string.rename)) },
                        onClick = {
                            onRename()
                            expanded = false
                        },
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.primary) },
                        text = { Text(text = stringResource(id = R.string.thumbnail)) },
                        onClick = {
                            onOpenThumbnail()
                            expanded = false
                        },
                    )
                    if (isClippingAvailable && !isClippingVideo && !isSplittingVideo) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.ContentCut, null, tint = MaterialTheme.colorScheme.primary) },
                            text = { Text(text = stringResource(id = R.string.clip_video)) },
                            onClick = {
                                onClippingToggled()
                                expanded = false
                            },
                        )
                    }
                    if (isSplitByChapterAvailable && !isClippingVideo && !isSplittingVideo) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.VerticalSplit, null, tint = MaterialTheme.colorScheme.primary) },
                            text = { Text(text = stringResource(id = R.string.split_video)) },
                            onClick = {
                                onSplittingToggled()
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun VideoInfoPreview() {
    SealTheme {
        Surface {
            Column {
                FormatVideoPreview(
                    title = stringResource(id = R.string.video_title_sample_text),
                    author = stringResource(id = R.string.video_creator_sample_text),
                    thumbnailUrl = "",
                    duration = 7890,
                    isSplittingVideo = false,
                    isClippingVideo = false,
                    isSplitByChapterAvailable = true,
                    isClippingAvailable = true,
                )
            }
        }
    }
}

@Composable
fun SuggestedFormatItem(
    modifier: Modifier = Modifier,
    videoInfo: VideoInfo,
    overrideFormats: List<Format>? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val requestedFormats =
        overrideFormats ?:
        videoInfo.requestedFormats
            ?: videoInfo.requestedDownloads?.map { it.toFormat() }
            ?: emptyList()
    val duration = videoInfo.duration ?: 0.0

    val containsVideo = requestedFormats.any { it.containsVideo() }
    val containsAudio = requestedFormats.any { it.containsVideo() }

    val title = requestedFormats.joinToString(separator = " + ") { it.toDisplayTitle() }

    val totalFileSize =
        requestedFormats.fold(initial = 0.0) { acc: Double, format: Format ->
            acc +
                (format.fileSize ?: format.fileSizeApprox ?: (duration * (format.tbr ?: 0.0) * 125))
            // kbps -> bytes 1000/8
        }
    val fileSizeText = totalFileSize.toFileSizeText()

    val totalTbr =
        requestedFormats.fold(initial = 0.0) { acc: Double, format: Format ->
            acc + (format.tbr ?: 0.0)
        }

    val tbrText = totalTbr.toBitrateText()

    val firstLineText = connectWithDelimiter(fileSizeText, tbrText, delimiter = " ")

    val vcodecText = (overrideFormats?.firstOrNull()?.vcodec ?: videoInfo.vcodec)?.substringBefore(delimiter = ".") ?: ""
    val acodecText = (overrideFormats?.lastOrNull()?.acodec ?: videoInfo.acodec)?.substringBefore(delimiter = ".") ?: ""

    val codecText =
        connectWithBlank(vcodecText, acodecText).run { if (isNotBlank()) "($this)" else this }

    val secondLineText = connectWithDelimiter(
        (overrideFormats?.firstOrNull()?.ext ?: videoInfo.ext), 
        codecText, 
        delimiter = " "
    ).uppercase()

    FormatItem(
        modifier = modifier,
        title = title,
        containsAudio = containsAudio,
        containsVideo = containsVideo,
        firstLineText = firstLineText,
        secondLineText = secondLineText,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
fun FormatItem(
    modifier: Modifier = Modifier,
    formatInfo: Format,
    duration: Double,
    selected: Boolean = false,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    listView: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {

    with(formatInfo) {
        val vcodecText = vcodec?.substringBefore(delimiter = ".") ?: ""
        val acodecText = acodec?.substringBefore(delimiter = ".") ?: ""

        val codec =
            connectWithBlank(vcodecText, acodecText).run { if (isNotBlank()) "($this)" else this }

        val tbrText =
            when {
                tbr == null -> "" // i don't care
                tbr < 1024f -> "%.1f Kbps".format(tbr)

                else -> "%.2f Mbps".format(tbr / 1024f)
            }

        val fileSize = fileSize ?: fileSizeApprox ?: (tbr?.times(duration * 125))
        val fileSizeText = fileSize.toFileSizeText()

        val firstLineText = connectWithDelimiter(fileSizeText, tbrText, delimiter = " ")

        val secondLineText = connectWithDelimiter(ext, codec, delimiter = " ").uppercase()

        FormatItem(
            modifier = modifier,
            title = formatInfo.toDisplayTitle(),
            containsAudio = formatInfo.containsAudio(),
            containsVideo = formatInfo.containsVideo(),
            firstLineText = firstLineText,
            secondLineText = secondLineText,
            listViewFileSizeText = fileSizeText,
            listViewBitrateCodecText = connectWithDelimiter(tbrText, codec, delimiter = " "),
            listViewFormatExt = ext?.uppercase() ?: "",
            outlineColor = outlineColor,
            containerColor = containerColor,
            selected = selected,
            listView = listView,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormatItem(
    modifier: Modifier = Modifier,
    title: String = "247 - 1280x720 (720p)",
    containsAudio: Boolean = false,
    containsVideo: Boolean = false,
    firstLineText: String,
    secondLineText: String,
    listViewFileSizeText: String = "",
    listViewBitrateCodecText: String = "",
    listViewFormatExt: String = "",
    selected: Boolean = false,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    listView: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {

    // Determine proper text colors based on container background
    val selectedTextColor = when (containerColor) {
        MaterialTheme.colorScheme.primaryContainer -> MaterialTheme.colorScheme.onPrimaryContainer
        MaterialTheme.colorScheme.secondaryContainer -> MaterialTheme.colorScheme.onSecondaryContainer
        MaterialTheme.colorScheme.tertiaryContainer -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val animatedTitleColor by
        animateColorAsState(
            if (selected) selectedTextColor else MaterialTheme.colorScheme.onSurface,
            animationSpec = tween(150),
            label = "",
        )

    val animatedFirstLineColor by
        animateColorAsState(
            if (selected) selectedTextColor else MaterialTheme.colorScheme.onSurface,
            animationSpec = tween(150),
            label = "",
        )

    val animatedSecondLineColor by
        animateColorAsState(
            if (selected) selectedTextColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(150),
            label = "",
        )

    val animatedIconColor by
        animateColorAsState(
            if (selected) selectedTextColor else outlineColor,
            animationSpec = tween(150),
            label = "",
        )

    val animatedContainerColor by
        animateColorAsState(
            if (selected) containerColor else MaterialTheme.colorScheme.surfaceContainerHigh,
            animationSpec = tween(150),
            label = "",
        )

    val animatedOutlineColor by
        animateColorAsState(
            targetValue = if (selected) outlineColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            animationSpec = tween(150),
            label = "",
        )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by
        animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
            label = "formatItemScale",
        )

    val cardShapeRadius by
        animateDpAsState(
            targetValue = if (selected) 20.dp else 16.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMedium, visibilityThreshold = Dp.VisibilityThreshold),
            label = "formatItemCorner",
        )
    val cardShape = RoundedCornerShape(cardShapeRadius)

    val checkBadge: @Composable BoxScope.() -> Unit = {
        AnimatedVisibility(
            visible = selected,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 0.5f) + fadeOut(),
        ) {
            Box(
                modifier = Modifier.size(20.dp).background(outlineColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = selectedTextColor,
                )
            }
        }
    }

    val mediaIcons: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (containsVideo)
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    tint = animatedIconColor,
                    contentDescription = stringResource(id = R.string.video),
                    modifier = Modifier.size(18.dp),
                )
            if (containsAudio)
                Icon(
                    imageVector = Icons.Rounded.Audiotrack,
                    tint = animatedIconColor,
                    contentDescription = stringResource(id = R.string.audio),
                    modifier = Modifier.size(18.dp),
                )
            if (!containsVideo && !containsAudio) {
                Icon(
                    imageVector = Icons.Rounded.QuestionMark,
                    tint = animatedIconColor,
                    contentDescription = stringResource(id = R.string.unknown),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    if (listView) {
        // New premium card layout: centered, full-width, 3-section vertical design
        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(cardShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    onClick = { onClick() },
                    onLongClick = onLongClick,
                    onLongClickLabel = stringResource(R.string.copy_link),
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = animatedOutlineColor,
                    shape = cardShape,
                )
                .background(animatedContainerColor)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Section: Resolution / title — large, bold, perfectly centered
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = animatedTitleColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Middle Section: Format icon(s) + format text — horizontally centered
                val middleText = listViewFormatExt.ifBlank { secondLineText }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    mediaIcons()
                    if (middleText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = middleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = animatedSecondLineColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Bottom Section: file size (left) | bitrate + codec (right)
                val bottomLeft = listViewFileSizeText.ifBlank { firstLineText }
                val bottomRight = listViewBitrateCodecText
                if (bottomLeft.isNotBlank() || bottomRight.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = bottomLeft,
                            style = MaterialTheme.typography.bodySmall,
                            color = animatedSecondLineColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (bottomRight.isNotBlank()) {
                            Text(
                                text = bottomRight,
                                style = MaterialTheme.typography.bodySmall,
                                color = animatedSecondLineColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            checkBadge()
        }
    } else {
        // Original grid card layout
        Box(
            modifier =
                modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(cardShape)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        onClick = { onClick() },
                        onLongClick = onLongClick,
                        onLongClickLabel = stringResource(R.string.copy_link),
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = animatedOutlineColor,
                        shape = cardShape,
                    )
                    .background(animatedContainerColor)
        ) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.Start) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    minLines = 2,
                    maxLines = 2,
                    color = animatedTitleColor,
                    overflow = TextOverflow.Clip,
                )

                Text(
                    text = firstLineText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                    color = animatedFirstLineColor,
                    maxLines = 2,
                )

                Text(
                    text = secondLineText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                    color = animatedSecondLineColor,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.padding(bottom = 10.dp, end = 10.dp).align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (containsVideo)
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        tint = animatedIconColor,
                        contentDescription = stringResource(id = R.string.video),
                        modifier = Modifier.size(18.dp),
                    )
                if (containsAudio)
                    Icon(
                        imageVector = Icons.Rounded.Audiotrack,
                        tint = animatedIconColor,
                        contentDescription = stringResource(id = R.string.audio),
                        modifier = Modifier.size(18.dp),
                    )
                if (!containsVideo && !containsAudio) {
                    Icon(
                        imageVector = Icons.Rounded.QuestionMark,
                        tint = animatedIconColor,
                        contentDescription = stringResource(id = R.string.unknown),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            checkBadge()
        }
    }
}

@Composable
@Preview(
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "Light")
fun PreviewFormat() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        var selected by remember { mutableStateOf(-1) }
        Surface {
            Column() {
                //                FormatSubtitle(text = stringResource(R.string.video_only))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FormatPreviewContent(selected) { selected = it }
                }
            }
        }
    }
}

fun LazyGridScope.FormatPreviewContent(selected: Int = 0, onClick: (Int) -> Unit = {}) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatSubtitle(
            text = "Suggested",
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).padding(horizontal = 12.dp),
        )
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatItem(
            selected = selected == 1,
            containsAudio = true,
            containsVideo = true,
            firstLineText = "? MB + 16.00 MB, (? + 200) Kbps",
            secondLineText = "MKV (Unknown + OPUS)",
        ) {
            onClick(1)
        }
    }

    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatSubtitle(
            text = stringResource(R.string.audio),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).padding(horizontal = 12.dp),
        )
    }
    for (i in 0..1) {
        item {
            FormatItem(
                selected = selected == i,
                outlineColor = MaterialTheme.colorScheme.tertiary,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                containsVideo = false,
                containsAudio = true,
                firstLineText = "",
                secondLineText = "OPUS (OPUS)",
            ) {
                onClick(i)
            }
        }
    }
    item {
        FormatItem(
            selected = selected == 2,
            outlineColor = MaterialTheme.colorScheme.tertiary,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            containsVideo = false,
            containsAudio = true,
            firstLineText = "",
            secondLineText = "Unknown (Unknown)",
        ) {
            onClick(2)
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatSubtitle(
            text = stringResource(R.string.video_only),
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).padding(horizontal = 12.dp),
        )
    }
    for (i in 0..2) {
        item {
            FormatItem(
                selected = selected == i,
                containsVideo = true,
                containsAudio = false,
                firstLineText = "69.00MB 745.7Kbps",
                secondLineText = "MP4 (AVC1)",
            ) {
                onClick(i)
            }
        }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        FormatSubtitle(
            text = stringResource(R.string.video),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).padding(horizontal = 12.dp),
        )
    }
    for (i in 0..3) {
        item {
            FormatItem(
                selected = selected == i,
                outlineColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                containsVideo = true,
                containsAudio = true,
                firstLineText = "",
                secondLineText = "",
            ) {
                onClick(i)
            }
        }
    }
}

@Composable
fun FormatSubtitle(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    showDivider: Boolean = false,
) {
    Column(modifier = modifier) {
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(4.dp).background(color = color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Preview
@Composable
fun FormatItemPreview() {
    FormatItem(formatInfo = Format(), duration = 20.0)
}
