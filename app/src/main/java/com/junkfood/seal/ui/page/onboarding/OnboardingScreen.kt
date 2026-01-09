package com.junkfood.seal.ui.page.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.GradientDarkColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Onboarding data class for each page
 */
data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val gradient: Brush
)

/**
 * Main Onboarding Screen with ViewPager-style navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.VideoLibrary,
            title = "Welcome to Seal Plus",
            description = "Your ultimate video downloader powered by yt-dlp. Download videos, audio, and playlists from your favorite platforms with ease.",
            gradient = GradientBrushes.Primary
        ),
        OnboardingPage(
            icon = Icons.Filled.Download,
            title = "Powerful Downloads",
            description = "Download in multiple formats and quality options. Choose from video, audio-only, or custom formats with advanced configuration.",
            gradient = GradientBrushes.Secondary
        ),
        OnboardingPage(
            icon = Icons.Filled.Settings,
            title = "Highly Customizable",
            description = "Extensive settings to personalize your experience. Control download directory, network preferences, themes, and much more.",
            gradient = GradientBrushes.Accent
        ),
        OnboardingPage(
            icon = Icons.Filled.Security,
            title = "Privacy & Security",
            description = "Your downloads are private and secure. Optional app lock with PIN or biometric authentication keeps your content safe.",
            gradient = GradientBrushes.Vibrant
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDarkTheme) GradientDarkColors.Background
                else MaterialTheme.colorScheme.background
            )
    ) {
        // Animated background gradient blobs
        AnimatedBackgroundGradient(
            currentPage = pagerState.currentPage,
            isDarkTheme = isDarkTheme
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button at top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pages.size - 1)
                            }
                        }
                    ) {
                        Text(
                            text = "Skip",
                            color = if (isDarkTheme) 
                                GradientDarkColors.OnSurface.copy(alpha = 0.7f)
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // ViewPager for onboarding pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    isDarkTheme = isDarkTheme
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Page indicators
            PageIndicators(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                isDarkTheme = isDarkTheme
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Navigation buttons
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = if (isDarkTheme)
                                    GradientDarkColors.GradientPrimaryEnd
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Back", fontSize = 16.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // Next/Get Started button
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onFinish()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (pagerState.currentPage > 0) 16.dp else 0.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme)
                                GradientDarkColors.GradientPrimaryEnd
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Individual onboarding page content with animations
 */
@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    isDarkTheme: Boolean
) {
    // Animation states
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(page) {
        visible = false
        delay(100)
        visible = true
    }
    
    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(contentAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with gradient background and glow effect
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(iconScale),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .alpha(0.4f)
                    .blur(20.dp)
                    .background(
                        brush = page.gradient,
                        shape = CircleShape
                    )
            )
            
            // Icon container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDarkTheme)
                            GradientDarkColors.SurfaceContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(brush = page.gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title
        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isDarkTheme)
                GradientDarkColors.OnBackground
            else
                MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = page.description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            color = if (isDarkTheme)
                GradientDarkColors.OnSurface.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

/**
 * Page indicators (dots)
 */
@Composable
fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    isDarkTheme: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            val isSelected = page == currentPage
            
            val width by animateDpAsState(
                targetValue = if (isSelected) 32.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "indicatorWidth"
            )
            
            val alpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.3f,
                animationSpec = tween(durationMillis = 300),
                label = "indicatorAlpha"
            )
            
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .alpha(alpha)
                    .background(
                        if (isDarkTheme)
                            GradientDarkColors.GradientPrimaryEnd
                        else
                            MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}

/**
 * Animated background gradient that changes with pages
 */
@Composable
fun AnimatedBackgroundGradient(
    currentPage: Int,
    isDarkTheme: Boolean
) {
    if (!isDarkTheme) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "backgroundGradient")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.15f)
    ) {
        // Gradient blob top right
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 150.dp, y = (-100).dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GradientDarkColors.GradientPrimaryEnd.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Gradient blob bottom left
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 100.dp)
                .blur(70.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GradientDarkColors.GradientAccentEnd.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Center gradient blob (moves with pages)
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .offset(
                    x = (currentPage * 50 - 100).dp,
                    y = 0.dp
                )
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GradientDarkColors.GradientSecondaryEnd.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}
