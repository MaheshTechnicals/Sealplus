package com.junkfood.seal.ui.page.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.ThumbnailFormat
import com.junkfood.seal.util.ThumbnailQuality
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.toFileSizeTextAuto
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThumbnailDownloadPage(
    onNavigateBack: () -> Unit,
    viewModel: ThumbnailDownloadViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val palette = rememberToolPalette()
    val scope = rememberCoroutineScope()
    val pageScrollState = rememberScrollState()

    val state by viewModel.viewStateFlow.collectAsStateWithLifecycle()

    // Success toast + haptic tick the moment a download completes, then smoothly scroll the
    // page down to the success card — it renders below the fold (after the format/quality/
    // filename sections) so without this the user would have to manually swipe up to even
    // see that the download finished.
    LaunchedEffect(state.downloadSuccess) {
        if (state.downloadSuccess) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            context.makeToast(context.getString(R.string.thumbnail_downloaded))
            // Let the success card's own enter animation start first so the scroll and the
            // expand/scale-in animation read as one smooth motion instead of a jump-cut.
            kotlinx.coroutines.delay(120L)
            pageScrollState.animateScrollTo(
                value = pageScrollState.maxValue,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            )
        }
    }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onNavigateBack)
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.thumbnail_download),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.thumbnail_download_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(color = palette.border.copy(alpha = 0.4f))
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(pageScrollState)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            ThumbnailUrlInputCard(
                palette = palette,
                url = state.url,
                isUrlValid = state.isUrlValid,
                isFetching = state.isFetching,
                onUrlChange = viewModel::updateUrl,
                onPaste = {
                    clipboardManager.getText()?.let {
                        viewModel.updateUrl(it.toString().trim())
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )

            state.errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                ThumbnailErrorCard(palette = palette, message = error)
            }

            AnimatedVisibility(
                visible = state.isFetching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    ThumbnailPreviewSkeleton(palette = palette)
                }
            }

            AnimatedVisibility(
                visible = state.hasPreview && !state.isFetching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    ThumbnailPreviewCard(
                        palette = palette,
                        thumbnailUrl = state.thumbnailUrl,
                        title = state.title.orEmpty(),
                        channelName = state.channelName.orEmpty(),
                    )

                    Spacer(Modifier.height(16.dp))
                    OutputOptionsSection(
                        palette = palette,
                        selectedFormat = state.format,
                        selectedQuality = state.quality,
                        onFormatSelected = viewModel::updateFormat,
                        onQualitySelected = viewModel::updateQuality,
                    )

                    Spacer(Modifier.height(14.dp))
                    FileNameCard(
                        palette = palette,
                        fileName = state.fileName,
                        onFileNameChange = viewModel::updateFileName,
                    )

                    Spacer(Modifier.height(14.dp))
                    DownloadSummaryCard(
                        palette = palette,
                        format = state.format,
                        quality = state.quality,
                        fileName = state.fileName.ifBlank { state.title.orEmpty() },
                        fileSizeBytes = state.fileSizeBytes,
                    )

                    Spacer(Modifier.height(16.dp))
                    DownloadActionRow(
                        palette = palette,
                        isDownloading = state.isDownloading,
                        onDownloadHere = { viewModel.downloadThumbnail() },
                    )

                    AnimatedVisibility(
                        visible = state.downloadSuccess,
                        enter = fadeIn() + scaleIn(initialScale = 0.85f),
                        exit = fadeOut() + scaleOut(targetScale = 0.9f),
                    ) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            DownloadSuccessCard(
                                palette = palette,
                                filePath = state.savedFilePath,
                                onOpenFile = {
                                    state.savedFilePath?.let { path ->
                                        FileUtil.openFile(path) {
                                            context.makeToast(context.getString(R.string.file_unavailable))
                                        }
                                    }
                                },
                                onShareFile = {
                                    state.savedFilePath?.let { path ->
                                        FileUtil.createIntentForSharingFile(path)?.let { intent ->
                                            context.startActivity(
                                                android.content.Intent.createChooser(
                                                    intent,
                                                    context.getString(R.string.share_thumbnail),
                                                )
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThumbnailUrlInputCard(
    palette: ToolPalette,
    url: String,
    isUrlValid: Boolean,
    isFetching: Boolean,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = palette.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.video_url),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.thumbnail_url_hint),
                        color = palette.textSecondary.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary.copy(alpha = 0.4f),
                    unfocusedBorderColor = palette.border.copy(alpha = 0.3f),
                    cursorColor = palette.primary,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedContainerColor = palette.background,
                    unfocusedContainerColor = palette.background,
                ),
                // Live validation indicator: a small check/loading glyph right in the field,
                // in addition to the paste button, so the user gets feedback without waiting
                // for the preview card to appear.
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            isFetching -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = palette.primary,
                            )
                            url.isNotEmpty() && isUrlValid -> Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = palette.success,
                                modifier = Modifier.size(20.dp),
                            )
                            url.isNotEmpty() && !isUrlValid -> Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = palette.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        if (url.isNotEmpty()) {
                            IconButton(onClick = { onUrlChange("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.clear),
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onPaste,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
            ) {
                Icon(
                    Icons.Outlined.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.paste),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun ThumbnailErrorCard(palette: ToolPalette, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = palette.error.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, palette.error.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = palette.error,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = palette.error,
            )
        }
    }
}

/** A shimmering placeholder shown while the thumbnail/video info is being fetched. */
@Composable
private fun ThumbnailPreviewSkeleton(palette: ToolPalette) {
    val infiniteTransition = rememberInfiniteTransitionCompat()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton_shimmer",
    )
    val shimmerColor = palette.surfaceVariant.copy(alpha = shimmerAlpha)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(shimmerColor),
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerColor),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerColor),
            )
        }
    }
}

@Composable
private fun rememberInfiniteTransitionCompat() =
    androidx.compose.animation.core.rememberInfiniteTransition(label = "thumbnail_skeleton")

@Composable
private fun ThumbnailPreviewCard(
    palette: ToolPalette,
    thumbnailUrl: String?,
    title: String,
    channelName: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.chipSelectedBorder.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp)),
            ) {
                AsyncImageImpl(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Small badge confirming this is the true highest-resolution asset — reassures
                // the user there's no resolution picker because there's nothing higher to pick.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "ORIGINAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title.ifBlank { "Untitled video" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (channelName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = palette.textSecondary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputOptionsSection(
    palette: ToolPalette,
    selectedFormat: ThumbnailFormat,
    selectedQuality: ThumbnailQuality,
    onFormatSelected: (ThumbnailFormat) -> Unit,
    onQualitySelected: (ThumbnailQuality) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = palette.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.output_format),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThumbnailFormat.entries.forEach { format ->
                    SelectableChip(
                        palette = palette,
                        label = format.label,
                        selected = format == selectedFormat,
                        modifier = Modifier.weight(1f),
                        onClick = { onFormatSelected(format) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = palette.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.output_quality),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThumbnailQuality.entries.forEach { quality ->
                    QualityRow(
                        palette = palette,
                        quality = quality,
                        selected = quality == selectedQuality,
                        onClick = { onQualitySelected(quality) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.SmartButton,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = palette.textSecondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.original_resolution_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SelectableChip(
    palette: ToolPalette,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "chip_scale",
    )
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactionSource, onClick = onClick)
            .then(Modifier.graphicsLayerScale(scale)),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.chipSelectedBg else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) palette.chipSelectedBorder else palette.chipUnselectedBorder,
        ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) palette.primary else palette.textSecondary,
            )
        }
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier =
    this.scale(scale)

@Composable
private fun QualityRow(
    palette: ToolPalette,
    quality: ThumbnailQuality,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val description = when (quality) {
        ThumbnailQuality.ORIGINAL -> stringResource(R.string.quality_original_desc)
        ThumbnailQuality.HIGH -> stringResource(R.string.quality_high_desc)
        ThumbnailQuality.MEDIUM -> stringResource(R.string.quality_medium_desc)
        ThumbnailQuality.COMPRESSED -> stringResource(R.string.quality_compressed_desc)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.chipSelectedBg else palette.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            if (selected) palette.chipSelectedBorder else palette.border.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quality.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) palette.primary else palette.textPrimary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun FileNameCard(
    palette: ToolPalette,
    fileName: String,
    onFileNameChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DriveFileRenameOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = palette.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.file_name),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = fileName,
                onValueChange = onFileNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.file_name_hint),
                        color = palette.textSecondary.copy(alpha = 0.5f),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary.copy(alpha = 0.4f),
                    unfocusedBorderColor = palette.border.copy(alpha = 0.3f),
                    cursorColor = palette.primary,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedContainerColor = palette.background,
                    unfocusedContainerColor = palette.background,
                ),
            )
        }
    }
}

@Composable
private fun DownloadSummaryCard(
    palette: ToolPalette,
    format: ThumbnailFormat,
    quality: ThumbnailQuality,
    fileName: String,
    fileSizeBytes: Long?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = palette.chipSelectedBg,
        border = BorderStroke(1.dp, palette.chipSelectedBorder.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.download_summary),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = palette.primary,
            )
            Spacer(Modifier.height(8.dp))
            SummaryRow(palette, stringResource(R.string.file_name), "${fileName.ifBlank { "thumbnail" }}.${format.extension}")
            SummaryRow(palette, stringResource(R.string.output_format), format.label)
            SummaryRow(palette, stringResource(R.string.output_quality), quality.label)
            SummaryRow(
                palette,
                stringResource(R.string.file_size),
                if (fileSizeBytes != null) fileSizeBytes.toFileSizeTextAuto()
                else stringResource(R.string.calculating_size),
            )
        }
    }
}

@Composable
private fun SummaryRow(palette: ToolPalette, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp).weight(1f, fill = false),
        )
    }
}

@Composable
private fun DownloadActionRow(
    palette: ToolPalette,
    isDownloading: Boolean,
    onDownloadHere: () -> Unit,
) {
    Column {
        // "Choose Location" was removed — thumbnails always save under the app's own
        // Downloads/SealPlus/Thumbnails folder (same convention as every other tool in the
        // app), so a destination picker was redundant friction rather than a real choice.
        Button(
            onClick = onDownloadHere,
            enabled = !isDownloading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isDownloading) Modifier.background(palette.surfaceVariant)
                        else Modifier.background(ToolGradientBrush)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = palette.textSecondary,
                        )
                    } else {
                        Icon(
                            Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isDownloading) stringResource(R.string.downloading_thumbnail)
                        else stringResource(R.string.download_thumbnail),
                        color = if (isDownloading) palette.textSecondary else Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        AnimatedVisibility(visible = isDownloading) {
            Column {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = palette.primary,
                    trackColor = palette.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DownloadSuccessCard(
    palette: ToolPalette,
    filePath: String?,
    onOpenFile: () -> Unit,
    onShareFile: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.success.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, palette.success.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.success.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = palette.success,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.thumbnail_downloaded),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.success,
                    )
                    filePath?.let {
                        // Show the full path (no truncation) even if it wraps across
                        // multiple lines — the card is allowed to grow taller.
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                        )
                    }
                }
            }
            if (filePath != null && !filePath.startsWith("content://")) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenFile,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                    ) {
                        Icon(
                            Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = palette.textPrimary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.view_thumbnail),
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Button(
                        onClick = onShareFile,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = palette.textPrimary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.share_thumbnail),
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}
