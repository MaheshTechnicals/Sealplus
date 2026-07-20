package com.junkfood.seal.ui.page.tools

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.SavedCommentSet
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.util.Comment
import com.junkfood.seal.util.CommentExportUtil
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.makeToast

/** Caps the number of comments rendered on this page to keep large (2-3k+) comment sets smooth. */
private const val COMMENT_DETAIL_PREVIEW_LIMIT = 10

@Composable
fun CommentDetailPage(
    commentSetId: Int,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val palette = rememberToolPalette()

    var commentSet by remember { mutableStateOf<SavedCommentSet?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(commentSetId) {
        isLoading = true
        commentSet = DatabaseUtil.getSavedCommentSetById(commentSetId)
        isLoading = false
    }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onNavigateBack)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.comment_detail),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    commentSet?.let { current ->
                        IconButton(onClick = {
                            if (current.videoUrl.isNotBlank()) {
                                context.startActivity(
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, current.videoUrl)
                                    }.let { Intent.createChooser(it, context.getString(R.string.share)) }
                                )
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = palette.primary,
                            )
                        }
                    }
                }
                HorizontalDivider(color = palette.border.copy(alpha = 0.4f))
            }
        },
    ) { paddingValues ->
        val current = commentSet
        if (isLoading || current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (!isLoading && current == null) {
                    Text(
                        text = stringResource(R.string.no_saved_comments),
                        modifier = Modifier.padding(24.dp),
                        color = palette.textSecondary,
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            if (current.thumbnailUrl.isNotBlank()) {
                AsyncImageImpl(
                    model = current.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(14.dp))
            }

            Text(
                text = current.videoTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary,
            )
            if (current.uploader.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = current.uploader,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${current.commentCount} ${stringResource(R.string.comment_count_label)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                )
                IconButton(
                    onClick = {
                        val allText = current.comments.joinToString("\n\n") { c ->
                            "${c.author}${if (c.isReply) " (reply)" else ""}: ${c.text}"
                        }
                        clipboardManager.setText(AnnotatedString(allText))
                        context.makeToast("Comments copied to clipboard")
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.copy_link),
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            if (current.comments.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_comments_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    current.comments.take(COMMENT_DETAIL_PREVIEW_LIMIT).forEach { comment ->
                        FullCommentRow(palette = palette, comment = comment)
                    }
                }
                val remaining = current.comments.size - COMMENT_DETAIL_PREVIEW_LIMIT
                if (remaining > 0) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.more_comments_export_hint, remaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            ExportSection(
                palette = palette,
                onExportTxt = {
                    CommentExportUtil.exportAsTxt(current)
                        .onSuccess { path -> shareExportedFile(context, path) }
                        .onFailure { context.makeToast(context.getString(R.string.export_failed)) }
                },
                onExportCsv = {
                    CommentExportUtil.exportAsCsv(current)
                        .onSuccess { path -> shareExportedFile(context, path) }
                        .onFailure { context.makeToast(context.getString(R.string.export_failed)) }
                },
                onExportJson = {
                    CommentExportUtil.exportAsJson(current)
                        .onSuccess { path -> shareExportedFile(context, path) }
                        .onFailure { context.makeToast(context.getString(R.string.export_failed)) }
                },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun shareExportedFile(context: android.content.Context, path: String) {
    context.makeToast(context.getString(R.string.exported_successfully))
    FileUtil.createIntentForSharingFile(path)?.let {
        context.startActivity(Intent.createChooser(it, context.getString(R.string.share)))
    }
}

@Composable
private fun FullCommentRow(palette: ToolPalette, comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (comment.isReply) 24.dp else 0.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(palette.chipSelectedBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = comment.author.take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.primary,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (comment.authorIsUploader) {
                    Spacer(Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(palette.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.creator_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
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
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
                color = palette.textPrimary.copy(alpha = 0.92f),
            )
            Spacer(Modifier.height(4.dp))
            comment.likeCount?.takeIf { it > 0 }?.let { likes ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
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
private fun ExportSection(
    palette: ToolPalette,
    onExportTxt: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
) {
    Column {
        Text(
            text = "Export",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(10.dp))
        // Stacked vertically (one full-width button per row) instead of 3-across — at 3-across
        // each button was too narrow for its label ("Export as TXT"/"CSV"/"JSON") to fit
        // without clipping, so it always rendered as a truncated "Export as…". Full width
        // gives every label room to show completely.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onExportTxt,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
            ) {
                Text(
                    stringResource(R.string.export_as_txt),
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
            OutlinedButton(
                onClick = onExportCsv,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
            ) {
                Text(
                    stringResource(R.string.export_as_csv),
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
            Button(
                onClick = onExportJson,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
            ) {
                Text(
                    stringResource(R.string.export_as_json),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}
