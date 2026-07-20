package com.junkfood.seal.ui.page.tools

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.common.ThemedIconColors
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.makeToast

private data class ToolItem(
    val id: Int,
    val titleRes: Int,
    /** One-line summary shown directly on the card. Keep this short so it never wraps/clips. */
    val shortDescRes: Int,
    /** Full description shown in the info dialog (tap the (i) icon or long-press the card). */
    val descRes: Int,
    val icon: ImageVector,
    val isComingSoon: Boolean = true,
)

/** Each tool cycles through one of these theme-derived gradient pairs for its icon badge. */
private enum class AccentStyle { PRIMARY, SECONDARY, TERTIARY }

// Display order: Batch URL Import, Thumbnail Download, Video Info Download, Comment Download.
// IDs are stable identifiers used for click routing (see the `when (tool.id)` dispatch below) —
// they intentionally stay unchanged here; only this list's ordering (i.e. display order) moved.
private val tools = listOf(
    ToolItem(
        id = 1,
        titleRes = R.string.batch_url_import,
        shortDescRes = R.string.batch_url_import_short_desc,
        descRes = R.string.batch_url_import_desc,
        icon = Icons.Outlined.PlaylistAdd,
        isComingSoon = false,
    ),
    ToolItem(
        id = 4,
        titleRes = R.string.thumbnail_download,
        shortDescRes = R.string.thumbnail_download_short_desc,
        descRes = R.string.thumbnail_download_desc,
        icon = Icons.Outlined.Image,
        isComingSoon = false,
    ),
    ToolItem(
        id = 2,
        titleRes = R.string.video_info_download,
        shortDescRes = R.string.video_info_download_short_desc,
        descRes = R.string.video_info_download_desc,
        icon = Icons.Outlined.Description,
        isComingSoon = false,
    ),
    ToolItem(
        id = 3,
        titleRes = R.string.comment_download,
        shortDescRes = R.string.comment_download_short_desc,
        descRes = R.string.comment_download_desc,
        icon = Icons.Outlined.Chat,
        isComingSoon = false,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreToolsPage(
    onNavigateBack: () -> Unit,
    onNavigateToBatchUrlImport: (() -> Unit)? = null,
    onNavigateToVideoInfoDownload: (() -> Unit)? = null,
    onNavigateToThumbnailDownload: (() -> Unit)? = null,
    onNavigateToCommentDownload: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    val useGradientColors = isGradientDark && isDarkTheme
    var infoDialogTool by remember { mutableStateOf<ToolItem?>(null) }

    val backgroundColor = if (useGradientColors) {
        GradientDarkColors.Background
    } else {
        MaterialTheme.colorScheme.background
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.more_tools)) },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = if (useGradientColors) {
                        GradientDarkColors.Background
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
            )
        },
        containerColor = backgroundColor,
    ) { paddingValues ->
        // Adaptive column count: scales from a single column on narrow phones up to several
        // columns on tablets/foldables/landscape, instead of a hardcoded fixed count.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 176.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 4.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeroBanner(useGradientColors = useGradientColors)
            }

            tools.forEachIndexed { index, tool ->
                item {
                    ToolCard(
                        tool = tool,
                        index = index,
                        accentStyle = AccentStyle.entries[index % AccentStyle.entries.size],
                        useGradientColors = useGradientColors,
                        onClick = {
                            when (tool.id) {
                                1 -> onNavigateToBatchUrlImport?.invoke()
                                2 -> onNavigateToVideoInfoDownload?.invoke()
                                3 -> onNavigateToCommentDownload?.invoke()
                                4 -> onNavigateToThumbnailDownload?.invoke()
                                else -> {
                                    context.makeToast(
                                        "${context.getString(tool.titleRes)} — ${context.getString(R.string.feature_unavailable)}"
                                    )
                                }
                            }
                        },
                        onLongClick = { infoDialogTool = tool },
                        onInfoClick = { infoDialogTool = tool },
                    )
                }
            }
        }
    }

    infoDialogTool?.let { tool ->
        SealDialog(
            onDismissRequest = { infoDialogTool = null },
            icon = { Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(tool.titleRes)) },
            text = { Text(stringResource(tool.descRes)) },
            confirmButton = {
                ConfirmButton(text = stringResource(R.string.got_it)) { infoDialogTool = null }
            },
        )
    }
}

/**
 * Compact hero banner introducing the page. Uses the same gradient language as the rest of
 * the app (GradientBrushes in Gradient Dark mode, theme container colors otherwise) so it
 * feels native rather than bolted-on.
 */
@Composable
private fun HeroBanner(useGradientColors: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "hero_alpha",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha)
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (useGradientColors) {
                        GradientBrushes.Primary
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        )
                    }
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (useGradientColors) Color.White.copy(alpha = 0.16f)
                            else Color.White.copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = if (useGradientColors) {
                            GradientDarkColors.OnPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.more_tools),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (useGradientColors) {
                            GradientDarkColors.OnPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // No maxLines/ellipsis here on purpose — the previous 2-line cap clipped
                    // the tail of the description on narrower phones and in longer-translation
                    // locales. The hero banner's height isn't fixed, so letting the text wrap
                    // to as many lines as it needs shows the full sentence on every screen size.
                    Text(
                        text = stringResource(R.string.more_tools_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (useGradientColors) {
                            GradientDarkColors.OnPrimary.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Resolves the same colorful, theme-aware tint used for icons in the navigation drawer
 * ([ThemedIconColors]) for a given accent slot, so a tool's icon color reads as consistent
 * with the rest of the app (primary/secondary/tertiary role colors) instead of a bespoke
 * gradient invented just for this page.
 */
@Composable
private fun accentColor(style: AccentStyle): Color = when (style) {
    AccentStyle.PRIMARY -> ThemedIconColors.primary
    AccentStyle.SECONDARY -> ThemedIconColors.secondary
    AccentStyle.TERTIARY -> ThemedIconColors.tertiary
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolCard(
    tool: ToolItem,
    index: Int,
    accentStyle: AccentStyle,
    useGradientColors: Boolean,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val entranceSpec = tween<Float>(
        durationMillis = 380,
        delayMillis = index * 60,
        easing = FastOutSlowInEasing,
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = entranceSpec,
        label = "card_alpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 20.dp,
        animationSpec = tween(
            durationMillis = 380,
            delayMillis = index * 60,
            easing = FastOutSlowInEasing,
        ),
        label = "card_offset",
    )
    // Spring-based press feedback reads as noticeably smoother/snappier than a linear tween.
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "card_scale",
    )

    LaunchedEffect(Unit) { visible = true }

    val borderColor = if (useGradientColors) {
        GradientDarkColors.GlassWhiteBorder
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    val surfaceColor = if (useGradientColors) {
        GradientDarkColors.GlassSurface.copy(alpha = 0.06f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val titleColor = if (useGradientColors) GradientDarkColors.OnSurface else MaterialTheme.colorScheme.onSurface
    val subtleColor = if (useGradientColors) {
        GradientDarkColors.OnSurface.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .offset(y = offsetY)
            .graphicsLayer(alpha = alpha)
            .fillMaxWidth()
            .heightIn(min = 172.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                val iconColor = accentColor(accentStyle)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        // Soft tinted container behind a full-strength colored icon — the same
                        // colorful primary/secondary/tertiary language used for icons in the
                        // navigation drawer, applied consistently here instead of the page's
                        // own one-off gradient badges.
                        .background(iconColor.copy(alpha = if (useGradientColors) 0.18f else 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp),
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(tool.titleRes),
                        tint = subtleColor,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(tool.titleRes),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.sp,
                ),
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(tool.shortDescRes),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = subtleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(8.dp))

            if (tool.isComingSoon) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (useGradientColors) {
                                GradientDarkColors.GradientPrimaryStart.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.coming_soon_placeholder),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = if (useGradientColors) {
                            GradientDarkColors.GradientPrimaryEnd
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.open),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
