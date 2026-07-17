package com.junkfood.seal.ui.page.tools

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.makeToast
import androidx.compose.ui.platform.LocalContext

private data class ToolItem(
    val id: Int,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
)

private val tools = listOf(
    ToolItem(
        id = 1,
        titleRes = R.string.batch_url_import,
        descRes = R.string.batch_url_import_desc,
        icon = Icons.Outlined.PlaylistAdd,
    ),
    ToolItem(
        id = 2,
        titleRes = R.string.video_info_download,
        descRes = R.string.video_info_download_desc,
        icon = Icons.Outlined.Description,
    ),
    ToolItem(
        id = 3,
        titleRes = R.string.comment_download,
        descRes = R.string.comment_download_desc,
        icon = Icons.Outlined.Chat,
    ),
    ToolItem(
        id = 4,
        titleRes = R.string.thumbnail_download,
        descRes = R.string.thumbnail_download_desc,
        icon = Icons.Outlined.Image,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreToolsPage(
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isGradientDark = LocalGradientDarkMode.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.more_tools))
                },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    scrolledContainerColor = if (isGradientDark) {
                        GradientDarkColors.Background
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
            )
        },
        containerColor = if (isGradientDark) {
            GradientDarkColors.Background
        } else {
            MaterialTheme.colorScheme.background
        },
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(isGradientDark = isGradientDark)
            }

            tools.forEachIndexed { index, tool ->
                item {
                    ToolCard(
                        tool = tool,
                        index = index,
                        isGradientDark = isGradientDark,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(isGradientDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isGradientDark) GradientBrushes.Primary
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = if (isGradientDark) {
                        GradientDarkColors.OnPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(R.string.more_tools_desc),
                style = MaterialTheme.typography.titleMedium,
                color = if (isGradientDark) {
                    GradientDarkColors.OnSurface.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun ToolCard(
    tool: ToolItem,
    index: Int,
    isGradientDark: Boolean,
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = index * 100,
            easing = FastOutSlowInEasing,
        ),
        label = "card_alpha",
    )

    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = index * 100,
            easing = FastOutSlowInEasing,
        ),
        label = "card_offset",
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "card_scale",
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .offset(y = offsetY)
            .graphicsLayer(alpha = alpha)
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isGradientDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = GradientDarkColors.GlassWhiteBorder,
                        shape = RoundedCornerShape(20.dp),
                    )
                } else Modifier
            )
            .background(
                if (isGradientDark) {
                    GradientDarkColors.GlassSurface.copy(alpha = 0.05f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    context.makeToast(
                        "${context.getString(tool.titleRes)} — ${context.getString(R.string.feature_unavailable)}"
                    )
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isGradientDark) {
                                GradientBrushes.Vibrant
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    ),
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(tool.titleRes),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                    ),
                    color = if (isGradientDark) {
                        GradientDarkColors.OnSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(tool.descRes),
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp,
                    ),
                    color = if (isGradientDark) {
                        GradientDarkColors.OnSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 3,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isGradientDark) {
                                GradientDarkColors.GradientPrimaryStart.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.coming_soon_placeholder),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = if (isGradientDark) {
                            GradientDarkColors.GradientPrimaryEnd
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                }
            }
        }
    }
}
