package com.junkfood.seal.ui.page.browser

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R

/** One action in the bottom bar (phones) or the side rail (tablets and landscape). */
internal data class BrowserNavAction(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean,
    val badge: Int = 0,
    val onClick: () -> Unit,
)

/** Icon with an optional count. The badge sits inside the icon box, so it is never cut off. */
@Composable
private fun NavIcon(icon: ImageVector, badge: Int) {
    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = null)
        if (badge > 0) {
            Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                Text(text = if (badge > 99) "99+" else badge.toString())
            }
        }
    }
}

@Composable
internal fun BrowserNavigationBar(actions: List<BrowserNavAction>) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        actions.forEach { action ->
            NavigationBarItem(
                selected = false,
                onClick = action.onClick,
                enabled = action.enabled,
                icon = { NavIcon(action.icon, action.badge) },
                label = { Text(text = action.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
internal fun BrowserNavigationRail(actions: List<BrowserNavAction>) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        actions.forEach { action ->
            NavigationRailItem(
                selected = false,
                onClick = action.onClick,
                enabled = action.enabled,
                icon = { NavIcon(action.icon, action.badge) },
                label = { Text(text = action.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                alwaysShowLabel = true,
            )
        }
    }
}

/** Rounded address field. Shows a lock for https pages and a reload or stop button at the end. */
@Composable
internal fun AddressBar(
    value: String,
    loading: Boolean,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onGo: () -> Unit,
    onReloadOrStop: () -> Unit,
) {
    val secure = value.startsWith("https://", ignoreCase = true)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).height(52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (secure) Icons.Outlined.Lock else Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint =
                    if (secure) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onGo() }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
                )
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.browser_address_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onReloadOrStop) {
                Icon(
                    imageVector = if (loading) Icons.Outlined.Close else Icons.Outlined.Refresh,
                    contentDescription =
                        stringResource(if (loading) R.string.browser_stop else R.string.browser_reload),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private val adHosts = listOf("adtng.com", "trafficjunky", "trafficfactory", "exoclick", "juicyads", "vcmdiawe")

/**
 * True for the real video, false for previews and ad creatives. Known sizes under 2 MB are
 * previews. Streams (HLS/DASH) have no size yet, so they are kept.
 */
internal fun isMainMedia(media: SniffedMedia): Boolean {
    if (adHosts.any { media.host.contains(it, ignoreCase = true) }) return false
    if (media.fileName.contains("_fb", ignoreCase = true)) return false
    val size = media.sizeBytes
    if (media.kind != SniffedKind.Stream && size != null && !media.sizeIsEstimate && size < 2_000_000L) return false
    return true
}

@Composable
private fun SniffedKind.label(): String =
    when (this) {
        SniffedKind.Video -> stringResource(R.string.browser_kind_video)
        SniffedKind.Audio -> stringResource(R.string.browser_kind_audio)
        SniffedKind.Stream -> stringResource(R.string.browser_kind_stream)
    }


/**
 * The list of detected files. Tap a card to open the download dialog; nothing downloads until the
 * user confirms there.
 */
@Composable
internal fun DetectedFilesContent(
    items: List<SniffedMedia>,
    onOpen: (SniffedMedia) -> Unit,
    onCopyLink: (SniffedMedia) -> Unit,
    onRemove: (SniffedMedia) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
) {
    // Only real files are listed. Small previews and ad clips are hidden.
    val visible = items.filter { isMainMedia(it) }
    // Half the screen height at most, so the list stays usable in landscape and on small screens.
    val listMaxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.5f).coerceAtMost(440.dp)

    Column(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.browser_detected_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    text = visible.size.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = stringResource(R.string.close))
            }
        }

        if (visible.isEmpty()) {
            DetectedEmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(max = listMaxHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visible, key = { it.url }) { media ->
                    DetectedCard(
                        media = media,
                        onOpen = { onOpen(media) },
                        onCopyLink = { onCopyLink(media) },
                        onRemove = { onRemove(media) },
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                TextButton(onClick = onClearAll) { Text(text = stringResource(R.string.browser_clear_all)) }
            }
        }
    }
}

@Composable
private fun DetectedEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.VideoFile,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.browser_no_media_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.browser_no_media_tips),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One detected file. Tapping the card opens the download dialog. */
@Composable
private fun DetectedCard(
    media: SniffedMedia,
    onOpen: () -> Unit,
    onCopyLink: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val sizeText =
        media.sizeBytes?.let { (if (media.sizeIsEstimate) "~" else "") + Formatter.formatShortFileSize(context, it) }
            ?: stringResource(R.string.browser_size_unknown)
    val partsText = media.segments?.let { stringResource(R.string.browser_parts, it) }
    val qualityText = media.resolutionLabel ?: media.bitrateLabel ?: media.kind.label()
    val details = listOfNotNull(media.host, sizeText, partsText, media.durationLabel).joinToString(" · ")

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = kindIcon(media.kind),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = qualityText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.download)) },
                        onClick = {
                            menuOpen = false
                            onOpen()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.copy_link)) },
                        onClick = {
                            menuOpen = false
                            onCopyLink()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.remove)) },
                        onClick = {
                            menuOpen = false
                            onRemove()
                        },
                    )
                }
            }
        }
    }
}

/** Confirmation for one detected file. Confirm starts the download and returns to Home. */
@Composable
internal fun SniffedDownloadDialog(media: SniffedMedia, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sizeText =
        media.sizeBytes?.let { (if (media.sizeIsEstimate) "~" else "") + Formatter.formatShortFileSize(context, it) }
            ?: stringResource(R.string.browser_size_unknown)
    com.junkfood.seal.ui.component.SealDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            com.junkfood.seal.ui.component.ConfirmButton(text = stringResource(R.string.download)) { onConfirm() }
        },
        dismissButton = {
            com.junkfood.seal.ui.component.DismissButton(text = stringResource(R.string.cancel)) { onDismiss() }
        },
        icon = {
            Icon(
                imageVector = kindIcon(media.kind),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(text = media.fileName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                listOfNotNull(media.resolutionLabel ?: media.bitrateLabel, media.durationLabel)
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        Text(
                            text = it.joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                Text(
                    text = "${media.host} · $sizeText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

internal fun kindIcon(kind: SniffedKind) =
    when (kind) {
        SniffedKind.Video -> Icons.Outlined.VideoFile
        SniffedKind.Audio -> Icons.Outlined.AudioFile
        SniffedKind.Stream -> Icons.Outlined.LiveTv
    }
