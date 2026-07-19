package com.junkfood.seal.ui.page.tools

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.SavedVideoInfo
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.VideoInfoExportUtil
import com.junkfood.seal.util.makeToast

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoInfoDetailPage(
    videoInfoId: Int,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val palette = rememberToolPalette()

    var info by remember { mutableStateOf<SavedVideoInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(videoInfoId) {
        isLoading = true
        info = DatabaseUtil.getSavedVideoInfoById(videoInfoId)
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
                        text = stringResource(R.string.video_info_detail),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    info?.let { current ->
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
        val current = info
        if (isLoading || current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (!isLoading && current == null) {
                    Text(
                        text = stringResource(R.string.no_saved_video_info),
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

            MetaRow(palette = palette, info = current)

            Spacer(Modifier.height(14.dp))

            CopySection(
                palette = palette,
                icon = Icons.Outlined.Title,
                label = stringResource(R.string.title),
                content = current.title,
                emptyLabel = null,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(current.title))
                    context.makeToast(context.getString(R.string.title_copied))
                },
            )

            Spacer(Modifier.height(12.dp))

            CopySection(
                palette = palette,
                icon = Icons.Outlined.Subject,
                label = stringResource(R.string.description),
                content = current.description,
                emptyLabel = stringResource(R.string.no_description_available),
                onCopy = {
                    clipboardManager.setText(AnnotatedString(current.description))
                    context.makeToast(context.getString(R.string.description_copied))
                },
            )

            Spacer(Modifier.height(12.dp))

            TagsSection(
                palette = palette,
                tags = current.tags,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(current.tags.joinToString(", ")))
                    context.makeToast(context.getString(R.string.tags_copied))
                },
            )

            Spacer(Modifier.height(20.dp))

            ExportSection(
                palette = palette,
                onExportTxt = {
                    VideoInfoExportUtil.exportAsTxt(current)
                        .onSuccess { path -> shareExportedFile(context, path) }
                        .onFailure { context.makeToast(context.getString(R.string.export_failed)) }
                },
                onExportJson = {
                    VideoInfoExportUtil.exportAsJson(current)
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
private fun MetaRow(palette: ToolPalette, info: SavedVideoInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (info.uploader.isNotBlank()) {
                MetaLine(palette, Icons.Outlined.Person, info.uploader)
                Spacer(Modifier.height(6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (info.durationString.isNotBlank()) {
                    MetaLine(palette, Icons.Outlined.Timer, info.durationString)
                }
                if (info.viewCount >= 0) {
                    MetaLine(palette, Icons.Outlined.Visibility, formatMetaCount(info.viewCount))
                }
                if (info.likeCount >= 0) {
                    MetaLine(palette, Icons.Outlined.ThumbUp, formatMetaCount(info.likeCount.toLong()))
                }
            }
            if (info.uploadDate.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                MetaLine(palette, Icons.Outlined.CalendarMonth, info.uploadDate)
            }
        }
    }
}

@Composable
private fun MetaLine(palette: ToolPalette, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.textSecondary)
        Spacer(Modifier.width(5.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
    }
}

private fun formatMetaCount(count: Long): String = when {
    count >= 1_000_000_000 -> "%.1fB".format(count / 1_000_000_000.0)
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}

@Composable
private fun CopySection(
    palette: ToolPalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: String,
    emptyLabel: String?,
    onCopy: () -> Unit,
) {
    val isEmpty = content.isBlank()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textPrimary,
                    )
                }
                if (!isEmpty) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = label,
                            modifier = Modifier.size(16.dp),
                            tint = palette.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isEmpty) emptyLabel.orEmpty() else content,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = if (isEmpty) palette.textSecondary.copy(alpha = 0.6f) else palette.textPrimary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    palette: ToolPalette,
    tags: List<String>,
    onCopy: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Sell,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = palette.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.tags) + if (tags.isNotEmpty()) " (${tags.size})" else "",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textPrimary,
                    )
                }
                if (tags.isNotEmpty()) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy_tags),
                            modifier = Modifier.size(16.dp),
                            tint = palette.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (tags.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_tags_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary.copy(alpha = 0.6f),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.chipSelectedBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportSection(
    palette: ToolPalette,
    onExportTxt: () -> Unit,
    onExportJson: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = palette.textSecondary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Export",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onExportTxt,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
            ) {
                Text(
                    stringResource(R.string.export_as_txt),
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Button(
                onClick = onExportJson,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
            ) {
                Text(
                    stringResource(R.string.export_as_json),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
