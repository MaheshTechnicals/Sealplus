@file:OptIn(ExperimentalMaterialApi::class)

package com.junkfood.seal.ui.page.videolist

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import android.text.format.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.component.SealModalBottomSheetM2
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.makeToast

private fun formatDownloadTime(millis: Long): String {
    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatAverageSpeed(bytesPerSec: Long): String {
    val mb = 1024L * 1024L
    val kb = 1024L
    return when {
        bytesPerSec >= mb -> "%.1f MB/s".format(bytesPerSec.toDouble() / mb)
        bytesPerSec >= kb -> "${bytesPerSec / kb} KB/s"
        else -> "$bytesPerSec B/s"
    }
}

@Composable
fun VideoDetailDrawer(
    sheetState: ModalBottomSheetState,
    info: DownloadedVideoInfo,
    isFileAvailable: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    BackHandler(sheetState.targetValue == ModalBottomSheetValue.Expanded) { onDismissRequest() }

    val onReDownload =
        remember(info) {
            {
                context.startActivity(
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        setPackage(context.packageName)
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, info.videoUrl)
                    }
                )
            }
        }

    val shareTitle = stringResource(id = R.string.share)
    with(info) {
        VideoDetailDrawerImpl(
            sheetState = sheetState,
            title = videoTitle,
            author = videoAuthor,
            url = videoUrl,
            thumbnailUrl = thumbnailUrl,
            videoPath = videoPath,
            extractor = extractor,
            isFileAvailable = isFileAvailable,
            onReDownload = onReDownload,
            onDismissRequest = onDismissRequest,
            downloadTimeMillis = downloadTimeMillis,
            averageSpeedBytesPerSec = averageSpeedBytesPerSec,
            onDelete = {
                view.slightHapticFeedback()
                onDismissRequest()
                onDelete()
            },
            onOpenLink = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismissRequest()
                uriHandler.openUri(videoUrl)
            },
            onShareFile = {
                view.slightHapticFeedback()
                FileUtil.createIntentForSharingFile(videoPath)?.runCatching {
                    context.startActivity(Intent.createChooser(this, shareTitle))
                }
            },
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DrawerPreview() {
    SealTheme {
        VideoDetailDrawerImpl(
            sheetState =
                ModalBottomSheetState(
                    ModalBottomSheetValue.Expanded,
                    density = LocalDensity.current,
                ),
            onReDownload = {},
            downloadTimeMillis = 134000L,
            averageSpeedBytesPerSec = 16_000_000L,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailDrawerImpl(
    sheetState: ModalBottomSheetState =
        ModalBottomSheetState(ModalBottomSheetValue.Hidden, density = LocalDensity.current),
    title: String = stringResource(id = R.string.video_title_sample_text),
    author: String = stringResource(id = R.string.video_creator_sample_text),
    url: String = "https://www.example.com",
    thumbnailUrl: String = "",
    videoPath: String = "",
    extractor: String = "",
    onDismissRequest: () -> Unit = {},
    isFileAvailable: Boolean = true,
    onReDownload: (() -> Unit) = {},
    onDelete: () -> Unit = {},
    onOpenLink: () -> Unit = {},
    onShareFile: () -> Unit = {},
    downloadTimeMillis: Long = -1L,
    averageSpeedBytesPerSec: Long = -1L,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    // File facts (size, resolution, duration, first frame) are read off the UI thread.
    val localInfo by
        produceState<LocalMediaInfo?>(initialValue = null, key1 = videoPath) {
            value = withContext(Dispatchers.IO) { readLocalMediaInfo(videoPath) }
        }

    SealModalBottomSheetM2(
        sheetState = sheetState,
        contentPadding = PaddingValues(horizontal = 0.dp),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {

                // ── Title & Author ───────────────────────────────────────────
                SelectionContainer {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (author.isNotBlank() && author != "playlist" && author != "null") {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── Thumbnail ────────────────────────────────────────────────
                if (thumbnailUrl.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        AsyncImageImpl(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                // Site did not give a thumbnail: use a frame from the downloaded file.
                if (thumbnailUrl.isBlank()) {
                    localInfo?.frame?.let { frame ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Image(
                                bitmap = frame.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }

                // ── Source URL card ──────────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    ),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(url))
                        context.makeToast(R.string.link_copied)
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(18.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.source_url),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        IconButton(
                            onClick = onOpenLink,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = stringResource(R.string.open_url),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // ── Download stats row ───────────────────────────────────────
                if (downloadTimeMillis > 0L || averageSpeedBytesPerSec > 0L) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (downloadTimeMillis > 0L) {
                            StatCard(
                                icon = Icons.Outlined.Timer,
                                label = stringResource(R.string.download_time),
                                value = formatDownloadTime(downloadTimeMillis),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (averageSpeedBytesPerSec > 0L) {
                            StatCard(
                                icon = Icons.Outlined.Speed,
                                label = stringResource(R.string.average_speed),
                                value = formatAverageSpeed(averageSpeedBytesPerSec),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── File facts (size, resolution, duration, location) ────────
                localInfo?.let { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.file_size),
                            value = Formatter.formatShortFileSize(context, info.sizeBytes),
                            modifier = Modifier.weight(1f),
                        )
                        info.resolutionLabel?.let {
                            StatCard(
                                icon = Icons.Outlined.HighQuality,
                                label = stringResource(R.string.resolution),
                                value = it,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        info.durationMs?.takeIf { it > 0 }?.let {
                            StatCard(
                                icon = Icons.Outlined.Schedule,
                                label = stringResource(R.string.duration),
                                value = formatDurationMs(it),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (videoPath.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        onClick = {
                            clipboardManager.setText(AnnotatedString(videoPath))
                            context.makeToast(R.string.link_copied)
                        },
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(
                                text = stringResource(R.string.file_path),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = videoPath,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // ── Action buttons ───────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.remove),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    if (isFileAvailable) {
                        Button(
                            onClick = onShareFile,
                            modifier = Modifier
                                .height(52.dp)
                                .weight(2f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.share),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    } else {
                        Button(
                            onClick = onReDownload,
                            modifier = Modifier
                                .height(52.dp)
                                .weight(2f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.redownload),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
