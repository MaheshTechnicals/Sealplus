package com.junkfood.seal.ui.integration

/**
 * INTEGRATION EXAMPLES FOR EXISTING SEAL APP PAGES
 * 
 * This file provides specific examples of how to integrate Gradient Dark theme
 * into the existing Seal app pages. Copy and adapt these examples to update
 * your existing screens.
 */

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
 * EXAMPLE 1: Settings Page Integration
 * 
 * Update SettingsPage.kt to use premium components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSettingsPageExample() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings Section
            item {
                AnimatedCardContainer(delayMillis = 0) {
                    PremiumSectionHeader(
                        title = "General",
                        icon = Icons.Outlined.Settings
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 50) {
                    PremiumGlassCard(
                        title = "Download Location",
                        description = "/storage/emulated/0/Download",
                        icon = Icons.Outlined.Folder,
                        onClick = { /* Navigate to download location */ }
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 100) {
                    PremiumGlassCard(
                        title = "Format Selection",
                        description = "Choose video and audio formats",
                        icon = Icons.Outlined.VideoSettings,
                        onClick = { /* Navigate to format selection */ }
                    )
                }
            }
            
            // Appearance Section
            item {
                AnimatedCardContainer(delayMillis = 150) {
                    PremiumSectionHeader(
                        title = "Appearance",
                        icon = Icons.Outlined.Palette
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 200) {
                    PremiumGlassCard(
                        title = "Look & Feel",
                        description = "Customize app appearance",
                        icon = Icons.Outlined.Palette,
                        onClick = { /* Navigate to appearance */ }
                    )
                }
            }
            
            // Network Section
            item {
                AnimatedCardContainer(delayMillis = 250) {
                    PremiumSectionHeader(
                        title = "Network",
                        icon = Icons.Outlined.CloudDownload
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 300) {
                    PremiumGlassCard(
                        title = "Network Preferences",
                        description = "Configure proxy, rate limit, and cookies",
                        icon = Icons.Outlined.Settings,
                        onClick = { /* Navigate to network settings */ }
                    )
                }
            }
        }
    }
}

/**
 * EXAMPLE 2: Download History Integration
 * 
 * Update download history screen with premium cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumDownloadHistoryExample() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Downloads") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Card
            item {
                AnimatedCardContainer(delayMillis = 0) {
                    PremiumGlassCard(
                        title = "Statistics",
                        icon = Icons.Outlined.BarChart,
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatRow("Total Downloads", "127")
                            StatRow("Completed", "115")
                            StatRow("In Progress", "12")
                            StatRow("Storage Used", "4.2 GB")
                        }
                    }
                }
            }
            
            // Recent Downloads Header
            item {
                AnimatedCardContainer(delayMillis = 50) {
                    PremiumSectionHeader(
                        title = "Recent",
                        icon = Icons.Outlined.History
                    )
                }
            }
            
            // Download Items (loop with staggered animation)
            items(10) { index ->
                AnimatedCardContainer(delayMillis = 100 + (index * 30)) {
                    PremiumGlassCard(
                        title = "Video Title $index",
                        description = "1080p • 45 MB • Completed",
                        icon = Icons.Outlined.VideoLibrary,
                        onClick = { /* Open video */ }
                    )
                }
            }
        }
    }
}

/**
 * EXAMPLE 3: Format Selection Dialog Integration
 */
@Composable
fun PremiumFormatSelectionExample(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PremiumSectionHeader(
            title = "Select Quality",
            icon = Icons.Outlined.HighQuality
        )
        
        // Quality Options
        PremiumGlassCard(
            title = "1080p Full HD",
            description = "Best quality • ~200 MB",
            icon = Icons.Outlined.HighQuality,
            onClick = { /* Select 1080p */ }
        )
        
        PremiumGlassCard(
            title = "720p HD",
            description = "Good quality • ~120 MB",
            icon = Icons.Outlined.Hd,
            onClick = { /* Select 720p */ }
        )
        
        PremiumGlassCard(
            title = "480p",
            description = "Standard quality • ~60 MB",
            icon = Icons.Outlined.Sd,
            onClick = { /* Select 480p */ }
        )
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumGradientButton(
                text = "Download",
                icon = Icons.Outlined.Download,
                onClick = { /* Start download */ },
                modifier = Modifier.weight(1f),
                brush = GradientBrushes.Primary
            )
        }
    }
}

/**
 * EXAMPLE 4: Home Screen Integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumHomeScreenExample() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Seal Plus") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card
            item {
                AnimatedCardContainer(delayMillis = 0) {
                    PremiumInfoCard(
                        text = "Enter a URL below to start downloading videos and audio",
                        icon = Icons.Outlined.Info
                    )
                }
            }
            
            // URL Input Card
            item {
                AnimatedCardContainer(delayMillis = 100) {
                    PremiumGlassCard(
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = { },
                                label = { Text("Paste URL") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Outlined.Link, null)
                                }
                            )
                            
                            PremiumGradientButton(
                                text = "Start Download",
                                icon = Icons.Outlined.Download,
                                onClick = { /* Start download */ },
                                modifier = Modifier.fillMaxWidth(),
                                brush = GradientBrushes.Primary
                            )
                        }
                    }
                }
            }
            
            // Quick Actions
            item {
                AnimatedCardContainer(delayMillis = 200) {
                    PremiumSectionHeader(
                        title = "Quick Actions",
                        icon = Icons.Outlined.TouchApp
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 250) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumGlassCard(
                            title = "Playlists",
                            icon = Icons.Outlined.PlaylistPlay,
                            onClick = { /* Open playlists */ },
                            modifier = Modifier.weight(1f)
                        )
                        
                        PremiumGlassCard(
                            title = "History",
                            icon = Icons.Outlined.History,
                            onClick = { /* Open history */ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * EXAMPLE 5: About Page Integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumAboutPageExample() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("About") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Card
            item {
                AnimatedCardContainer(delayMillis = 0) {
                    PremiumGlassCard(
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Seal Plus",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "A premium video downloader with gradient dark theme",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Developer Section
            item {
                AnimatedCardContainer(delayMillis = 100) {
                    PremiumSectionHeader(
                        title = "Developer",
                        icon = Icons.Outlined.Code
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 150) {
                    PremiumGlassCard(
                        title = "GitHub",
                        description = "View source code and contribute",
                        icon = Icons.Outlined.Code,
                        onClick = { /* Open GitHub */ }
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 200) {
                    PremiumGlassCard(
                        title = "Report Issue",
                        description = "Found a bug? Let us know",
                        icon = Icons.Outlined.BugReport,
                        onClick = { /* Open issue tracker */ }
                    )
                }
            }
            
            // Legal Section
            item {
                AnimatedCardContainer(delayMillis = 250) {
                    PremiumSectionHeader(
                        title = "Legal",
                        icon = Icons.Outlined.Gavel
                    )
                }
            }
            
            item {
                AnimatedCardContainer(delayMillis = 300) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PremiumGlassCard(
                            title = "License",
                            description = "GPL-3.0",
                            icon = Icons.Outlined.Description,
                            onClick = { /* View license */ }
                        )
                        
                        PremiumGlassCard(
                            title = "Privacy Policy",
                            description = "Your data stays on your device",
                            icon = Icons.Outlined.PrivacyTip,
                            onClick = { /* View privacy policy */ }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper Composable for Statistics Display
 */
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
 * INTEGRATION CHECKLIST:
 * 
 * For each existing screen in your app:
 * 
 * 1. Import premium components:
 *    import com.junkfood.seal.ui.component.*
 *    import com.junkfood.seal.ui.theme.GradientBrushes
 * 
 * 2. Add AnimatedCardContainer wrapper to LazyColumn items:
 *    - Start with delayMillis = 0 for first item
 *    - Increment by 50-100ms for each subsequent item
 * 
 * 3. Replace Card with PremiumGlassCard:
 *    - Add title, description, and icon parameters
 *    - Move click handler to onClick parameter
 * 
 * 4. Replace Button with PremiumGradientButton:
 *    - Specify text and icon
 *    - Choose appropriate gradient brush
 * 
 * 5. Add PremiumSectionHeader for section titles:
 *    - Replaces or enhances existing Text headers
 *    - Adds visual hierarchy
 * 
 * 6. Use PremiumInfoCard for hints and messages:
 *    - Replaces plain text or card-based info displays
 *    - Adds gradient border for emphasis
 * 
 * 7. Test with Gradient Dark both enabled and disabled:
 *    - Components should gracefully fall back to Material 3 styling
 *    - Animations should remain smooth
 */
