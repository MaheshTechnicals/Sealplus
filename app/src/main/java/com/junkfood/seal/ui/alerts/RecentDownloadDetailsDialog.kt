package com.junkfood.seal.ui.alerts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.component.DetailCard
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.toFileSizeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentDownloadDetailsDialog(
    downloadInfo: DownloadedVideoInfo,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFilePathDialog by remember { mutableStateOf(false) }

    BackHandler {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.download_details),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = downloadInfo.videoTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Thumbnail Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                AsyncImage(
                    model = downloadInfo.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Media Information Section
            Text(
                text = stringResource(R.string.media_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // Grid Layout for Details
            val file = java.io.File(downloadInfo.videoPath)
            val fileExtension = downloadInfo.videoPath.substringAfterLast(".", "")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: File Format and File Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (fileExtension.isNotEmpty()) {
                        DetailCard(
                            icon = Icons.Outlined.VideoFile,
                            label = stringResource(R.string.file_format),
                            value = fileExtension.uppercase(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (file.exists()) {
                        val fileSize = file.length()
                        DetailCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.file_size),
                            value = fileSize.toFileSizeText(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 2: Resolution and Platform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Extract resolution from video file
                    val resolution = remember(downloadInfo.videoPath) {
                        try {
                            if (file.exists()) {
                                val retriever = android.media.MediaMetadataRetriever()
                                retriever.setDataSource(downloadInfo.videoPath)
                                val width =
                                    retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                                val height =
                                    retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                                retriever.release()

                                if (width != null && height != null) {
                                    "${width}x${height}"
                                } else {
                                    "N/A"
                                }
                            } else {
                                "N/A"
                            }
                        } catch (e: Exception) {
                            "N/A"
                        }
                    }

                    if (resolution != "N/A") {
                        DetailCard(
                            icon = Icons.Outlined.HighQuality,
                            label = stringResource(R.string.resolution),
                            value = resolution,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DetailCard(
                        icon = Icons.Outlined.Language,
                        label = stringResource(R.string.platform),
                        value = downloadInfo.extractor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: File Path and Download Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailCard(
                        icon = Icons.Outlined.Folder,
                        label = stringResource(R.string.file_path),
                        value = downloadInfo.videoPath,
                        modifier = Modifier.weight(1f),
                        onClick = { showFilePathDialog = true }
                    )

                    val downloadDate = if (file.exists()) {
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(file.lastModified()))
                    } else {
                        "N/A"
                    }
                    DetailCard(
                        icon = Icons.Outlined.CalendarToday,
                        label = stringResource(R.string.download_date),
                        value = downloadDate,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Download Time and Average Speed
                if (downloadInfo.downloadTimeMillis > 0L || downloadInfo.averageSpeedBytesPerSec > 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (downloadInfo.downloadTimeMillis > 0L) {
                            DetailCard(
                                icon = Icons.Outlined.Timer,
                                label = stringResource(R.string.download_time),
                                value = formatDownloadTime(downloadInfo.downloadTimeMillis),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (downloadInfo.averageSpeedBytesPerSec > 0L) {
                            DetailCard(
                                icon = Icons.Outlined.Speed,
                                label = stringResource(R.string.average_speed),
                                value = formatAverageSpeed(downloadInfo.averageSpeedBytesPerSec),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Source URL Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(downloadInfo.videoUrl))
                            context.makeToast(R.string.link_copied)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.source_url),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = downloadInfo.videoUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // File Path Dialog
    if (showFilePathDialog) {
        AlertDialog(
            onDismissRequest = { showFilePathDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.file_path),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                SelectionContainer {
                    Text(
                        text = downloadInfo.videoPath,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilePathDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}


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
