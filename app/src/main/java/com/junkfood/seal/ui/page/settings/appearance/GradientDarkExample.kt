package com.junkfood.seal.ui.page.settings.appearance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.junkfood.seal.ui.component.*
import com.junkfood.seal.ui.theme.GradientBrushes

/**
 * Example implementation showing how to use Gradient Dark Premium Components
 * 
 * This file demonstrates the proper usage of:
 * - PremiumGlassCard for card-based layouts
 * - PremiumGradientButton for action buttons
 * - PremiumSectionHeader for section titles
 * - PremiumInfoCard for informational content
 * - AnimatedCardContainer for smooth animations
 * 
 * Usage:
 * Simply call GradientDarkExamplePage() in your navigation graph
 * or replace existing content with these premium components.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientDarkExamplePage(
    onNavigateBack: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Gradient Dark Demo") },
                navigationIcon = { BackButton(onClick = onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            item {
                AnimatedCardContainer(delayMillis = 0) {
                    PremiumSectionHeader(
                        title = "Premium Features",
                        icon = Icons.Outlined.Star
                    )
                }
            }
            
            // Info Card
            item {
                AnimatedCardContainer(delayMillis = 50) {
                    PremiumInfoCard(
                        text = "Gradient Dark mode is now active! Enjoy the premium glassmorphism effects.",
                        icon = Icons.Outlined.Info
                    )
                }
            }
            
            // Feature Cards
            item {
                AnimatedCardContainer(delayMillis = 100) {
                    PremiumGlassCard(
                        title = "Downloads",
                        description = "Manage your video and audio downloads with premium styling",
                        icon = Icons.Outlined.Download,
                        onClick = { /* Navigate to downloads */ }
                    ) {
                        // Additional content can go here
                    }
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 150) {
                    PremiumGlassCard(
                        title = "Settings",
                        description = "Configure app preferences with glassmorphism cards",
                        icon = Icons.Outlined.Settings,
                        onClick = { /* Navigate to settings */ }
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 200) {
                    PremiumGlassCard(
                        title = "Quality Selection",
                        description = "Choose video quality with vibrant gradient accents",
                        icon = Icons.Outlined.HighQuality,
                        onClick = { /* Navigate to quality settings */ }
                    )
                }
            }
            
            // Gradient Buttons
            item {
                AnimatedCardContainer(delayMillis = 250) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumSectionHeader(
                            title = "Actions",
                            icon = Icons.Outlined.TouchApp
                        )
                        
                        PremiumGradientButton(
                            text = "Start Download",
                            icon = Icons.Outlined.Download,
                            onClick = { /* Start download action */ },
                            brush = GradientBrushes.Primary
                        )
                        
                        PremiumGradientButton(
                            text = "View History",
                            icon = Icons.Outlined.History,
                            onClick = { /* View history action */ },
                            brush = GradientBrushes.Secondary
                        )
                        
                        PremiumGradientButton(
                            text = "Share",
                            icon = Icons.Outlined.Share,
                            onClick = { /* Share action */ },
                            brush = GradientBrushes.Accent
                        )
                    }
                }
            }
            
            // Stats Card Example
            item {
                AnimatedCardContainer(delayMillis = 300) {
                    PremiumGlassCard(
                        title = "Statistics",
                        icon = Icons.Outlined.BarChart,
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatRow("Total Downloads", "127")
                            StatRow("This Month", "23")
                            StatRow("Storage Used", "4.2 GB")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * INTEGRATION GUIDE:
 * 
 * To integrate Gradient Dark theme into existing screens:
 * 
 * 1. Replace standard Card components with PremiumGlassCard:
 *    Before: Card { ... }
 *    After:  PremiumGlassCard { ... }
 * 
 * 2. Replace standard Button components with PremiumGradientButton:
 *    Before: Button(onClick = { ... }) { Text("Action") }
 *    After:  PremiumGradientButton(text = "Action", onClick = { ... })
 * 
 * 3. Add section headers with PremiumSectionHeader:
 *    PremiumSectionHeader(title = "Section Title", icon = Icons.Outlined.Icon)
 * 
 * 4. Wrap cards in AnimatedCardContainer for entrance animations:
 *    AnimatedCardContainer(delayMillis = 100) {
 *        PremiumGlassCard { ... }
 *    }
 * 
 * 5. Use PremiumInfoCard for informational messages:
 *    PremiumInfoCard(
 *        text = "Information message",
 *        icon = Icons.Outlined.Info
 *    )
 * 
 * The components automatically detect if Gradient Dark mode is enabled
 * and adjust their appearance accordingly. When disabled, they fall back
 * to standard Material 3 styling.
 */
