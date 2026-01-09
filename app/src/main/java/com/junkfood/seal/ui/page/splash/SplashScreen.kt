package com.junkfood.seal.ui.page.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.ui.theme.SealTheme
import kotlinx.coroutines.delay

/**
 * Modern Professional Splash Screen for Seal Plus
 * 
 * Features:
 * - Smooth spring animations for logo entrance
 * - Sequential text fade-in animations  
 * - Pulsing glow effect in Gradient Dark mode
 * - Theme-aware colors and gradients
 * - Professional branding with "PLUS" badge
 * - Bottom copyright and powered by text
 * 
 * Timing:
 * - Logo appears at 200ms with bounce effect
 * - App name fades in at 600ms
 * - Tagline appears at 900ms
 * - Total display time: ~2.4 seconds
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val isGradientDark = LocalGradientDarkMode.current
    
    // Animation states
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }
    
    // Logo scale and alpha animations
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    
    // Text slide and fade animations
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "textAlpha"
    )
    
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "taglineAlpha"
    )
    
    // Pulsing glow effect for logo
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    // Trigger animations sequentially
    LaunchedEffect(Unit) {
        delay(200) // Initial delay
        logoVisible = true
        delay(400)
        textVisible = true
        delay(300)
        taglineVisible = true
        delay(1500) // Hold on screen
        onSplashFinished()
    }
    
    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isGradientDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            GradientDarkColors.Background,
                            GradientDarkColors.Surface,
                            GradientDarkColors.SurfaceContainerLow
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Logo with glow effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Glow effect (only in Gradient Dark mode)
                if (isGradientDark && logoVisible) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(logoScale * 1.1f)
                            .alpha(glowAlpha * logoAlpha * 0.4f)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        GradientDarkColors.GradientPrimaryEnd.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                
                // Main Logo
                Image(
                    painter = painterResource(id = R.drawable.splash_logo),
                    contentDescription = "Seal Plus Logo",
                    modifier = Modifier
                        .size(160.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Name with gradient (if Gradient Dark mode)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = if (isGradientDark) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier.alpha(textAlpha)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Advanced Video Downloader",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = if (isGradientDark) {
                    GradientDarkColors.GradientPrimaryEnd
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.alpha(taglineAlpha)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Premium badge (only in Gradient Dark mode)
            if (isGradientDark && taglineVisible) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .alpha(taglineAlpha)
                        .background(
                            brush = GradientBrushes.Primary,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✨ PLUS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
        
        // Bottom branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(taglineAlpha)
        ) {
            Text(
                text = "Powered by yt-dlp",
                style = MaterialTheme.typography.bodySmall,
                color = if (isGradientDark) {
                    GradientDarkColors.OnSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "© 2026 Seal Plus",
                style = MaterialTheme.typography.labelSmall,
                color = if (isGradientDark) {
                    GradientDarkColors.OnSurface.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
        }
    }
}

/**
 * Preview for Light Theme
 */
@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun SplashScreenLightPreview() {
    SealTheme(darkTheme = false) {
        SplashScreen(onSplashFinished = {})
    }
}

/**
 * Preview for Dark Theme
 */
@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun SplashScreenDarkPreview() {
    SealTheme(darkTheme = true) {
        SplashScreen(onSplashFinished = {})
    }
}
