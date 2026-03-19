package com.junkfood.seal.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.junkfood.seal.R
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.page.downloadv2.UiAction
import com.junkfood.seal.ui.theme.GradientDarkColors

@Composable
fun ActiveDownloadCard(
    task: Task,
    state: Task.State,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    var showMenu by remember { mutableStateOf(false) }

    val downloadState = state.downloadState
    val progress = when (downloadState) {
        is Task.DownloadState.Running -> downloadState.progress
        is Task.DownloadState.Paused -> downloadState.progress ?: 0f
        is Task.DownloadState.Canceled -> downloadState.progress ?: 0f
        else -> 0f
    }

    // Parse progress text to determine download phase
    val progressText =
        if (downloadState is Task.DownloadState.Running) downloadState.progressText else ""
    val context = androidx.compose.ui.platform.LocalContext.current

    // Track download state: format info -> video download -> audio download -> merging
    var hasSeenFormatInfo by remember { mutableStateOf(false) }
    var hasSeenVideoComplete by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf("downloading") }

    // Determine phase based on progressText patterns
    val downloadPhase = when {
        // Merging phase
        progressText.contains("[Merger]", ignoreCase = true) ||
                progressText.contains("Merging formats", ignoreCase = true) -> {
            currentPhase = "merging"
            hasSeenVideoComplete = false
            hasSeenFormatInfo = false
            "merging"
        }
        // Format info line - indicates download will start
        progressText.contains("[info]", ignoreCase = true) && progressText.contains(
            "format",
            ignoreCase = true
        ) -> {
            hasSeenFormatInfo = true
            hasSeenVideoComplete = false
            currentPhase = "fetching"
            "downloading"
        }
        // Download progress lines
        progressText.contains("[download]", ignoreCase = true) -> {
            when {
                // First 100% completion - video is done, audio is next
                progressText.contains("100%") && !hasSeenVideoComplete -> {
                    hasSeenVideoComplete = true
                    currentPhase = "video"
                    "video"
                }
                // After video complete, any download progress is audio
                hasSeenVideoComplete -> {
                    currentPhase = "audio"
                    "audio"
                }
                // Before any completion, it's video (first download is always video)
                hasSeenFormatInfo -> {
                    currentPhase = "video"
                    "video"
                }

                else -> "downloading"
            }
        }
        // Post-download file operations - maintain current phase
        progressText.contains("Deleting original file", ignoreCase = true) ||
                progressText.contains("[Metadata]", ignoreCase = true) ||
                progressText.contains("[MoveFiles]", ignoreCase = true) -> {
            currentPhase
        }
        // Fetching info phase
        progressText.contains("[youtube]", ignoreCase = true) ||
                progressText.contains("Downloading webpage", ignoreCase = true) ||
                progressText.contains("Downloading player", ignoreCase = true) -> {
            "fetching"
        }

        else -> currentPhase
    }

    val statusText = when (downloadState) {
        is Task.DownloadState.Running -> {
            when (downloadPhase) {
                "merging" -> stringResource(R.string.status_merging)
                "video" -> "Downloading video... ${(progress * 100).toInt()}%"
                "audio" -> "Downloading audio... ${(progress * 100).toInt()}%"
                "fetching" -> stringResource(R.string.fetching_info)
                else -> "Downloading... ${(progress * 100).toInt()}%"
            }
        }

        is Task.DownloadState.Paused -> stringResource(R.string.status_paused) + " ${(progress * 100).toInt()}%"
        is Task.DownloadState.Canceled -> stringResource(R.string.status_canceled)
        is Task.DownloadState.Error -> stringResource(R.string.download_error)
        is Task.DownloadState.Completed -> stringResource(R.string.completed) + " 100%"
        is Task.DownloadState.FetchingInfo -> stringResource(R.string.fetching_info)
        else -> ""
    }

    val statusColor = when (downloadState) {
        is Task.DownloadState.Running -> if (isGradientDark && isDarkTheme) {
            GradientDarkColors.GradientPrimaryStart
        } else {
            MaterialTheme.colorScheme.primary
        }

        is Task.DownloadState.Paused -> Color(0xFFFBBF24)
        is Task.DownloadState.Canceled -> Color(0xFFEF4444)
        is Task.DownloadState.Error -> Color(0xFFEF4444)
        is Task.DownloadState.Completed -> Color(0xFF4ADE80)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGradientDark && isDarkTheme) {
                GradientDarkColors.SurfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                state.videoInfo?.thumbnail?.let { thumbnailUrl ->
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = state.viewState.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )

                        // Show Queue badge for Idle or ReadyWithInfo tasks
                        if (downloadState is Task.DownloadState.Idle || downloadState is Task.DownloadState.ReadyWithInfo) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isGradientDark && isDarkTheme) {
                                    GradientDarkColors.GradientSecondaryStart.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.queue_status),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isGradientDark && isDarkTheme) {
                                        GradientDarkColors.GradientSecondaryEnd
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Pause/Resume action button
                if (downloadState is Task.DownloadState.Running) {
                    IconButton(
                        onClick = { onAction(UiAction.Pause) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = stringResource(R.string.pause),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (downloadState is Task.DownloadState.Paused) {
                    IconButton(
                        onClick = { onAction(UiAction.Resume) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.resume),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // More button with dropdown menu
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        val downloadState = state.downloadState

                        // Pause option for running downloads
                        if (downloadState is Task.DownloadState.Running) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pause)) },
                                onClick = {
                                    onAction(UiAction.Pause)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Pause,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }

                        // Resume option for paused downloads
                        if (downloadState is Task.DownloadState.Paused) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.resume)) },
                                onClick = {
                                    onAction(UiAction.Resume)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }

                        // Retry option for canceled or failed downloads
                        if (downloadState is Task.DownloadState.Canceled || downloadState is Task.DownloadState.Error) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.retry)) },
                                onClick = {
                                    onAction(UiAction.Retry)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }

                        // Cancel option for running/fetching/paused downloads
                        if (downloadState is Task.DownloadState.Running ||
                            downloadState is Task.DownloadState.FetchingInfo ||
                            downloadState is Task.DownloadState.Paused
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cancel)) },
                                onClick = {
                                    onAction(UiAction.Cancel)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Cancel,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            )
                        }

                        // Copy link option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy_link)) },
                            onClick = {
                                onAction(UiAction.CopyVideoURL)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        )

                        // Details option (only for completed downloads)
                        if (downloadState is Task.DownloadState.Completed) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.details)) },
                                onClick = {
                                    onAction(UiAction.ShowDetails)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )
                        }

                        // Delete option
                        if (downloadState is Task.DownloadState.Completed || downloadState is Task.DownloadState.Error || downloadState is Task.DownloadState.Canceled) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    onAction(UiAction.Delete)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Progress bar for active and paused downloads
            if (downloadState is Task.DownloadState.Running || downloadState is Task.DownloadState.Paused) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = when (downloadState) {
                        is Task.DownloadState.Paused -> Color(0xFFFBBF24)
                        else -> if (isGradientDark && isDarkTheme) {
                            GradientDarkColors.GradientPrimaryStart
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
