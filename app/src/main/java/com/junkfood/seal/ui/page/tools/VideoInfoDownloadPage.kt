package com.junkfood.seal.ui.page.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.junkfood.seal.database.objects.SavedVideoInfo
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.util.makeToast
import org.koin.androidx.compose.koinViewModel

@Composable
fun VideoInfoDownloadPage(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: VideoInfoDownloadViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val palette = rememberToolPalette()

    val viewState by viewModel.viewStateFlow.collectAsStateWithLifecycle()
    val savedInfoList by viewModel.savedInfoListFlow.collectAsStateWithLifecycle()

    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }

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
                            text = stringResource(R.string.video_info_download),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.video_info_download_desc),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            UrlInputCard(
                palette = palette,
                url = viewState.url,
                isLoading = viewState.isLoading,
                onUrlChange = viewModel::updateUrl,
                onPaste = {
                    clipboardManager.getText()?.let {
                        viewModel.updateUrl(it.toString().trim())
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onFetch = { viewModel.fetchInfo() },
            )

            viewState.errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                ErrorCard(palette = palette, message = error)
            }

            AnimatedVisibility(
                visible = viewState.result != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                viewState.result?.let { info ->
                    Column {
                        Spacer(Modifier.height(12.dp))
                        ResultPreviewCard(
                            palette = palette,
                            title = info.title,
                            uploader = info.uploader ?: info.channel.orEmpty(),
                            thumbnailUrl = info.thumbnail,
                            durationString = info.durationString,
                            viewCount = info.viewCount,
                            likeCount = info.likeCount,
                            tagCount = info.tags?.size ?: 0,
                            hasDescription = !info.description.isNullOrBlank(),
                            isSaved = viewState.isSaved,
                            onSave = {
                                viewModel.saveCurrentResult()
                                context.makeToast(context.getString(R.string.video_info_saved))
                            },
                            onDismiss = { viewModel.clearResult() },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = palette.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.saved_video_info),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                )
                if (savedInfoList.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.chipSelectedBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = savedInfoList.size.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (savedInfoList.isEmpty()) {
                EmptySavedInfoState(palette = palette)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 4000.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    userScrollEnabled = false,
                ) {
                    items(savedInfoList, key = { it.id }) { info ->
                        SavedInfoCard(
                            palette = palette,
                            info = info,
                            onClick = { onNavigateToDetail(info.id) },
                            onDeleteRequest = { pendingDeleteId = info.id },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    pendingDeleteId?.let { id ->
        SealDialog(
            onDismissRequest = { pendingDeleteId = null },
            icon = { Icon(Icons.Outlined.Bookmark, null, tint = palette.primary) },
            title = { Text(stringResource(R.string.delete_saved_info)) },
            text = { Text(stringResource(R.string.delete_saved_info_msg)) },
            confirmButton = {
                ConfirmButton {
                    viewModel.deleteSavedInfo(id)
                    pendingDeleteId = null
                }
            },
            dismissButton = { DismissButton { pendingDeleteId = null } },
        )
    }
}

@Composable
private fun UrlInputCard(
    palette: ToolPalette,
    url: String,
    isLoading: Boolean,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onFetch: () -> Unit,
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
                        stringResource(R.string.video_info_url_hint),
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
                trailingIcon = {
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
                },
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPaste,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = palette.textPrimary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.paste),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Button(
                    onClick = onFetch,
                    modifier = Modifier.weight(1.4f).height(44.dp),
                    enabled = !isLoading,
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
                                if (isLoading) Modifier.background(palette.surfaceVariant)
                                else Modifier.background(ToolGradientBrush)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = palette.textSecondary,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White,
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isLoading) stringResource(R.string.fetching_info)
                                else stringResource(R.string.fetch_info),
                                color = if (isLoading) palette.textSecondary else Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(palette: ToolPalette, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = palette.error.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, palette.error.copy(alpha = 0.3f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = palette.error,
        )
    }
}

@Composable
private fun ResultPreviewCard(
    palette: ToolPalette,
    title: String,
    uploader: String,
    thumbnailUrl: String?,
    durationString: String?,
    viewCount: Long?,
    likeCount: Int?,
    tagCount: Int,
    hasDescription: Boolean,
    isSaved: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.chipSelectedBorder.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row {
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImageImpl(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 96.dp, height = 72.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "Untitled video" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (uploader.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = uploader,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear),
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!durationString.isNullOrBlank()) {
                    StatChip(palette, Icons.Outlined.Timer, durationString)
                }
                if (viewCount != null && viewCount >= 0) {
                    StatChip(palette, Icons.Outlined.Visibility, formatCount(viewCount))
                }
                if (likeCount != null && likeCount >= 0) {
                    StatChip(palette, Icons.Outlined.ThumbUp, formatCount(likeCount.toLong()))
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = palette.border.copy(alpha = 0.3f))
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = palette.textSecondary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (hasDescription) "Description available" else "No description",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.width(12.dp))
                if (tagCount > 0) {
                    Text(
                        text = "· $tagCount tags",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSave,
                enabled = !isSaved,
                modifier = Modifier.fillMaxWidth().height(44.dp),
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
                            if (isSaved) Modifier.background(palette.success.copy(alpha = 0.15f))
                            else Modifier.background(ToolGradientBrush)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSaved) palette.success else Color.White,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isSaved) "Saved" else stringResource(R.string.save_info),
                            color = if (isSaved) palette.success else Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(palette: ToolPalette, icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = palette.textSecondary)
        Spacer(Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    }
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000_000 -> "%.1fB".format(count / 1_000_000_000.0)
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}

@Composable
private fun EmptySavedInfoState(palette: ToolPalette) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.chipSelectedBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Bookmark,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.no_saved_video_info),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.no_saved_video_info_desc),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SavedInfoCard(
    palette: ToolPalette,
    info: SavedVideoInfo,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.4f)),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                if (info.thumbnailUrl.isNotBlank()) {
                    AsyncImageImpl(
                        model = info.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(palette.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                IconButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (info.uploader.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = palette.textSecondary,
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = info.uploader,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (info.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.chipSelectedBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${info.tags.size} tags",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = palette.primary,
                        )
                    }
                }
            }
        }
    }
}
