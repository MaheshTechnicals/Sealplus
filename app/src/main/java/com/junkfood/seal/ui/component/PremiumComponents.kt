package com.junkfood.seal.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.GradientDarkColors

/**
 * Premium Glass Card with gradient dark theme support
 */
@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    icon: ImageVector? = null,
    elevation: Dp = 4.dp,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val isGradientDark = LocalGradientDarkMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "card_alpha"
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .then(
                if (isGradientDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = GradientDarkColors.GlassWhiteBorder,
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isGradientDark) {
                GradientDarkColors.GlassSurface.copy(alpha = 0.05f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isGradientDark) elevation + 2.dp else elevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (icon != null || title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGradientDark) {
                                GradientDarkColors.GradientPurpleBright
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isGradientDark) {
                                GradientDarkColors.OnSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                if (description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isGradientDark) {
                        GradientDarkColors.OnSurface.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (icon != null || title != null || description != null) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

/**
 * Premium Gradient Button with animated effects
 */
@Composable
fun PremiumGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    brush: Brush = GradientBrushes.Primary
) {
    val isGradientDark = LocalGradientDarkMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .then(
                if (isGradientDark) {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(brush)
                } else Modifier
            ),
        enabled = enabled,
        interactionSource = interactionSource,
        colors = if (isGradientDark) {
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = GradientDarkColors.OnPrimary
            )
        } else {
            ButtonDefaults.buttonColors()
        },
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isGradientDark) 8.dp else 2.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Premium Section Header with gradient accent
 */
@Composable
fun PremiumSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val isGradientDark = LocalGradientDarkMode.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null && isGradientDark) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GradientBrushes.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GradientDarkColors.OnPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = if (isGradientDark) {
                GradientDarkColors.OnSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * Premium Info Card with gradient border
 */
@Composable
fun PremiumInfoCard(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val isGradientDark = LocalGradientDarkMode.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isGradientDark) {
                    Modifier.border(
                        width = 1.5.dp,
                        brush = GradientBrushes.Primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isGradientDark) {
            GradientDarkColors.SurfaceVariant
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGradientDark) {
                        GradientDarkColors.GradientCyan
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isGradientDark) {
                    GradientDarkColors.OnSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Animated card container with fade-in effect
 */
@Composable
fun AnimatedCardContainer(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "container_alpha"
    )
    
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 20.dp,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "container_offset"
    )
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Box(
        modifier = modifier
            .offset(y = offsetY)
            .graphicsLayer(alpha = alpha)
    ) {
        content()
    }
}
