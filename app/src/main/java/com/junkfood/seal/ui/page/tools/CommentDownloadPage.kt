package com.junkfood.seal.ui.page.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.junkfood.seal.database.objects.SavedCommentSet
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.util.makeToast
import org.koin.androidx.compose.koinViewModel

/** How many comments to show inline in the fetch-result preview before saving. */
private const val COMMENT_PREVIEW_LIMIT = 10

@Composable
fun CommentDownloadPage(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: CommentDownloadViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val palette = rememberToolPalette()

    val state by viewModel.viewStateFlow.collectAsStateWithLifecycle()
    val savedCommentSets by viewModel.savedCommentSetsFlow.collectAsStateWithLifecycle()

    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Int>() }

    androidx.compose.runtime.LaunchedEffect(savedCommentSets.isEmpty()) {
        if (savedCommentSets.isEmpty()) {
            isSelectionMode = false
            selectedIds.clear()
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
                            text = stringResource(R.string.comment_download),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.comment_download_subtitle),
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

            CommentUrlInputCard(
                palette = palette,
                url = state.url,
                isLoading = state.isLoading,
                fetchLimit = state.fetchLimit,
                sortByTop = state.sortByTop,
                onUrlChange = viewModel::updateUrl,
                onPaste = {
                    clipboardManager.getText()?.let {
                        viewModel.updateUrl(it.toString().trim())
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onFetchLimitSelected = viewModel::updateFetchLimit,
                onSortChanged = viewModel::updateSortByTop,
                onFetch = { viewModel.fetchComments() },
            )

            state.errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                CommentErrorCard(palette = palette, message = error)
            }

            AnimatedVisibility(
                visible = state.hasResult,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    CommentResultCard(
                        palette = palette,
                        title = state.videoTitle.orEmpty(),
                        uploader = state.uploader.orEmpty(),
                        thumbnailUrl = state.thumbnailUrl,
                        topLevelCount = state.topLevelCount,
                        replyCount = state.replyCount,
                        showRepliesOnly = state.showRepliesOnly,
                        onToggleRepliesOnly = viewModel::toggleShowRepliesOnly,
                        comments = state.filteredComments,
                        isSaved = state.isSaved,
                        onDismiss = { viewModel.clearResult() },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = palette.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.saved_comment_sets),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                )
                if (savedCommentSets.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.chipSelectedBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = savedCommentSets.size.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.primary,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (isSelectionMode) {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(
                            onClick = { showBulkDeleteDialog = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_selected),
                                tint = palette.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = palette.primary,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    isSelectionMode = false
                                    selectedIds.clear()
                                },
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (savedCommentSets.isEmpty()) {
                EmptySavedCommentsState(palette = palette)
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
                    items(savedCommentSets, key = { it.id }) { commentSet ->
                        SavedCommentSetCard(
                            palette = palette,
                            commentSet = commentSet,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(commentSet.id),
                            onClick = {
                                if (isSelectionMode) {
                                    if (selectedIds.contains(commentSet.id)) selectedIds.remove(commentSet.id)
                                    else selectedIds.add(commentSet.id)
                                } else {
                                    onNavigateToDetail(commentSet.id)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!isSelectionMode) isSelectionMode = true
                                if (!selectedIds.contains(commentSet.id)) selectedIds.add(commentSet.id)
                            },
                            onDeleteRequest = { pendingDeleteId = commentSet.id },
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
            icon = { Icon(Icons.Outlined.Chat, null, tint = palette.primary) },
            title = { Text(stringResource(R.string.delete_saved_comments)) },
            text = { Text(stringResource(R.string.delete_saved_comments_msg)) },
            confirmButton = {
                ConfirmButton {
                    viewModel.deleteSavedCommentSetById(id)
                    pendingDeleteId = null
                }
            },
            dismissButton = { DismissButton { pendingDeleteId = null } },
        )
    }

    if (showBulkDeleteDialog) {
        SealDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Delete, null, tint = palette.error) },
            title = { Text(stringResource(R.string.delete_selected_comments)) },
            text = {
                Text(stringResource(R.string.delete_selected_comments_msg).format(selectedIds.size))
            },
            confirmButton = {
                ConfirmButton {
                    viewModel.deleteSavedCommentSets(selectedIds.toSet())
                    selectedIds.clear()
                    isSelectionMode = false
                    showBulkDeleteDialog = false
                }
            },
            dismissButton = { DismissButton { showBulkDeleteDialog = false } },
        )
    }
}

@Composable
private fun CommentUrlInputCard(
    palette: ToolPalette,
    url: String,
    isLoading: Boolean,
    fetchLimit: CommentFetchLimit,
    sortByTop: Boolean,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onFetchLimitSelected: (CommentFetchLimit) -> Unit,
    onSortChanged: (Boolean) -> Unit,
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

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.fetch_limit),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CommentFetchLimit.entries.forEach { limit ->
                    val selected = limit == fetchLimit
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onFetchLimitSelected(limit) },
                            ),
                        shape = RoundedCornerShape(9.dp),
                        color = if (selected) palette.chipSelectedBg else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (selected) palette.chipSelectedBorder else palette.chipUnselectedBorder,
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = if (limit == CommentFetchLimit.ALL) "All" else limit.count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selected) palette.primary else palette.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (sortByTop) stringResource(R.string.sort_by_top)
                    else stringResource(R.string.sort_by_newest),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.sort_by_top),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sortByTop) palette.primary else palette.textSecondary,
                    )
                    Switch(
                        checked = !sortByTop,
                        onCheckedChange = { onSortChanged(!it) },
                        modifier = Modifier.padding(horizontal = 6.dp),
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = palette.primary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.sort_by_newest),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!sortByTop) palette.primary else palette.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPaste,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceVariant),
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
                                text = if (isLoading) stringResource(R.string.fetching_comments)
                                else stringResource(R.string.fetch_comments),
                                color = if (isLoading) palette.textSecondary else Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentErrorCard(palette: ToolPalette, message: String) {
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
private fun CommentResultCard(
    palette: ToolPalette,
    title: String,
    uploader: String,
    thumbnailUrl: String?,
    topLevelCount: Int,
    replyCount: Int,
    showRepliesOnly: Boolean,
    onToggleRepliesOnly: () -> Unit,
    comments: List<com.junkfood.seal.util.Comment>,
    isSaved: Boolean,
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
                            .size(width = 72.dp, height = 54.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(10.dp))
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountChip(palette, stringResource(R.string.comment_count_label), topLevelCount)
                CountChip(palette, stringResource(R.string.reply_count_label), replyCount)
                Spacer(Modifier.weight(1f))
                if (isSaved) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.BookmarkAdded,
                            contentDescription = null,
                            tint = palette.success,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.success,
                        )
                    }
                }
            }

            if (replyCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onToggleRepliesOnly,
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (showRepliesOnly) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (showRepliesOnly) palette.primary else palette.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.show_replies_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = palette.border.copy(alpha = 0.3f))
            Spacer(Modifier.height(10.dp))

            if (comments.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_comments_available),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    comments.take(COMMENT_PREVIEW_LIMIT).forEach { comment ->
                        CommentRow(palette = palette, comment = comment)
                    }
                    if (comments.size > COMMENT_PREVIEW_LIMIT) {
                        Text(
                            text = "+ ${comments.size - COMMENT_PREVIEW_LIMIT} more comments (view full list after saving)",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountChip(palette: ToolPalette, label: String, count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.primary,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun CommentRow(palette: ToolPalette, comment: com.junkfood.seal.util.Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (comment.isReply) 20.dp else 0.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.chipSelectedBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = comment.author.take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = palette.primary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.authorIsUploader) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.creator_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = palette.primary,
                        )
                    }
                }
                if (comment.authorIsVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary.copy(alpha = 0.9f),
            )
            comment.likeCount?.takeIf { it > 0 }?.let { likes ->
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = likes.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySavedCommentsState(palette: ToolPalette) {
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
                    Icons.Outlined.Chat,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.no_saved_comments),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.no_saved_comments_desc),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedCommentSetCard(
    palette: ToolPalette,
    commentSet: SavedCommentSet,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) palette.primary else palette.border.copy(alpha = 0.4f),
        ),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                if (commentSet.thumbnailUrl.isNotBlank()) {
                    AsyncImageImpl(
                        model = commentSet.thumbnailUrl,
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
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (isSelected) palette.primary else Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.Black.copy(alpha = 0.45f)),
                    )
                } else {
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
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${commentSet.commentCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = commentSet.videoTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (commentSet.uploader.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = commentSet.uploader,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
