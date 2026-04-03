package com.junkfood.seal.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalWindowWidthState
import com.junkfood.seal.ui.common.Route
import com.junkfood.seal.ui.common.ThemedIconColors
import com.junkfood.seal.ui.page.downloadv2.DownloadPageImplV2
import com.junkfood.seal.ui.page.security.LockScreen
import com.junkfood.seal.util.AuthenticationManager
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.launch

@Composable
fun NavigationDrawer(
    modifier: Modifier = Modifier,
    drawerState: DrawerState,
    windowWidth: WindowWidthSizeClass = LocalWindowWidthState.current,
    currentRoute: String? = null,
    currentTopDestination: String? = null,
    showQuickSettings: Boolean = true,
    onNavigateToRoute: (String) -> Unit,
    onDismissRequest: suspend () -> Unit,
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showHiddenContentAuthScreen by remember { mutableStateOf(false) }
    var hiddenContentAuthDone by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    when (windowWidth) {
        WindowWidthSizeClass.Compact,
        WindowWidthSizeClass.Medium -> {
            ModalNavigationDrawer(
                gesturesEnabled = gesturesEnabled,
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(drawerState = drawerState, modifier = modifier.width(360.dp)) {
                        NavigationDrawerSheetContent(
                            modifier = Modifier,
                            currentRoute = currentRoute,
                            showQuickSettings = showQuickSettings,
                            onNavigateToRoute = onNavigateToRoute,
                            onDismissRequest = onDismissRequest,
                            onShowHiddenContentAuth = {
                                hiddenContentAuthDone = false
                                showHiddenContentAuthScreen = true
                            },
                        )
                    }
                },
                content = content,
            )
        }
        WindowWidthSizeClass.Expanded -> {
            ModalNavigationDrawer(
                gesturesEnabled = drawerState.isOpen,
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(drawerState = drawerState, modifier = modifier.width(360.dp)) {
                        NavigationDrawerSheetContent(
                            modifier = Modifier,
                            currentRoute = currentRoute,
                            showQuickSettings = showQuickSettings,
                            onNavigateToRoute = onNavigateToRoute,
                            onDismissRequest = onDismissRequest,
                            onShowHiddenContentAuth = {
                                hiddenContentAuthDone = false
                                showHiddenContentAuthScreen = true
                            },
                        )
                    }
                },
            ) {
                Row {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.zIndex(1f),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight().systemBarsPadding().width(92.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(8.dp))
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Icon(Icons.Outlined.Menu, null, tint = ThemedIconColors.primary)
                            }
                            Spacer(Modifier.weight(1f))
                            NavigationRailContent(
                                modifier = Modifier,
                                currentTopDestination = currentTopDestination,
                                onNavigateToRoute = onNavigateToRoute,
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    content()
                }
            }
        }
    }

    // Full-screen LockScreen overlay — rendered outside the narrow ModalDrawerSheet
    // so it covers the entire display.
    if (showHiddenContentAuthScreen && !hiddenContentAuthDone) {
        Box(modifier = Modifier.fillMaxSize()) {
            LockScreen(
                onUnlocked = {
                    hiddenContentAuthDone = true
                    showHiddenContentAuthScreen = false
                    scope.launch { onDismissRequest() }
                        .invokeOnCompletion { onNavigateToRoute(Route.HIDDEN_CONTENT) }
                },
                useBiometric = AuthenticationManager.useBiometric()
            )
        }
    }
    } // end outer Box
}

@Composable
fun DrawerHeader(
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    
    // Create gradient based on theme
    val headerGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.surface
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.surface
            )
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(headerGradient)
            .padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo - left side, vertically centered, reduced for balance
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "Seal Plus Logo",
                modifier = Modifier.size(76.dp)
            )
            
            // Fixed spacing between logo and text
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content column - strictly left-aligned text stack
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                // App Name - refined typography with tighter line height
                Text(
                    text = "Seal Plus",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                
                // Reduced spacing between title and tagline
                Spacer(modifier = Modifier.height(4.dp))
                
                // Tagline - enhanced hierarchy with improved opacity
                Text(
                    text = "Download Manager",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1
                )
                
                // Increased spacing before version badge for better hierarchy
                Spacer(modifier = Modifier.height(10.dp))
                
                // Version badge with gradient background - premium pill appearance
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isDarkTheme) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "v${App.packageInfo.versionName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationDrawerSheetContent(
    modifier: Modifier = Modifier,
    currentRoute: String? = null,
    showQuickSettings: Boolean = true,
    onNavigateToRoute: (String) -> Unit,
    onDismissRequest: suspend () -> Unit,
    onShowHiddenContentAuth: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
    ) {
        // Modern gradient header
        DrawerHeader()
        
        Spacer(Modifier.height(16.dp))

        // Group 1: Primary Destinations
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.home)) },
                    icon = { Icon(Icons.Filled.Download, null, tint = ThemedIconColors.primary) },
                    onClick = {
                        scope
                            .launch { onDismissRequest() }
                            .invokeOnCompletion { onNavigateToRoute(Route.HOME) }
                    },
                    selected = false,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.downloads_history)) },
                    icon = { Icon(Icons.Outlined.Subscriptions, null, tint = ThemedIconColors.secondary) },
                    onClick = {
                        scope
                            .launch { onDismissRequest() }
                            .invokeOnCompletion { onNavigateToRoute(Route.DOWNLOADS) }
                    },
                    selected = false,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.hidden_content)) },
                    icon = { Icon(Icons.Outlined.VisibilityOff, null, tint = ThemedIconColors.secondary) },
                    onClick = {
                        if (AuthenticationManager.isSecurityEnabled() && AuthenticationManager.isPinSet()) {
                            onShowHiddenContentAuth()
                        } else {
                            context.makeToast(R.string.hidden_content_requires_app_lock)
                        }
                    },
                    selected = false,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.custom_command)) },
                    icon = { Icon(Icons.Outlined.Terminal, null, tint = ThemedIconColors.tertiary) },
                    onClick = {
                        scope
                            .launch { onDismissRequest() }
                            .invokeOnCompletion { onNavigateToRoute(Route.TASK_LIST) }
                    },
                    selected = false,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        // Divider between groups
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Group 2: Utilities & Support
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    icon = { Icon(Icons.Outlined.Settings, null, tint = ThemedIconColors.primary) },
                    onClick = {
                        scope
                            .launch { onDismissRequest() }
                            .invokeOnCompletion { onNavigateToRoute(Route.SETTINGS) }
                    },
                    selected = false,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.trouble_shooting)) },
                    icon = { Icon(Icons.Rounded.BugReport, null, tint = ThemedIconColors.secondary) },
                    onClick = {
                        scope
                            .launch { onDismissRequest() }
                            .invokeOnCompletion { onNavigateToRoute(Route.TROUBLESHOOTING) }
                    },
                    selected = false,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.sponsor)) },
                    icon = { Icon(Icons.Outlined.VolunteerActivism, null, tint = ThemedIconColors.tertiary) },
                    onClick = {
                        scope
                            .launch { onDismissRequest() }
                            .invokeOnCompletion { onNavigateToRoute(Route.DONATE) }
                    },
                    selected = false,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.about)) },
                    icon = { Icon(Icons.Rounded.Info, null, tint = ThemedIconColors.primary) },
                    onClick = {
                        scope
                            .launch { onDismissRequest() }
                            .invokeOnCompletion { onNavigateToRoute(Route.ABOUT) }
                    },
                    selected = false,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun NavigationRailItemVariant(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit),
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.large)
                .background(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                )
                .selectable(selected = selected, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides
                if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            icon()
        }
    }
}

@Composable
fun NavigationRailContent(
    modifier: Modifier = Modifier,
    currentTopDestination: String? = null,
    onNavigateToRoute: (String) -> Unit,
) {
    Column(
        modifier = modifier.selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val scope = rememberCoroutineScope()
        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.HOME) Icons.Filled.Download
                    else Icons.Outlined.Download,
                    stringResource(R.string.home),
                    tint = ThemedIconColors.primary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.HOME,
            onClick = { onNavigateToRoute(Route.HOME) },
        )

        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.DOWNLOADS) Icons.Filled.Subscriptions
                    else Icons.Outlined.Subscriptions,
                    stringResource(R.string.downloads_history),
                    tint = ThemedIconColors.secondary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.DOWNLOADS,
            onClick = { onNavigateToRoute(Route.DOWNLOADS) },
        )

        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.TASK_LIST) Icons.Filled.Terminal
                    else Icons.Outlined.Terminal,
                    stringResource(R.string.custom_command),
                    tint = ThemedIconColors.tertiary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.TASK_LIST,
            onClick = { onNavigateToRoute(Route.TASK_LIST) },
        )

        NavigationRailItemVariant(
            icon = {
                Icon(
                    if (currentTopDestination == Route.SETTINGS_PAGE) Icons.Filled.Settings
                    else Icons.Outlined.Settings,
                    stringResource(R.string.settings),
                    tint = ThemedIconColors.primary,
                )
            },
            modifier = Modifier,
            selected = currentTopDestination == Route.SETTINGS_PAGE,
            onClick = { onNavigateToRoute(Route.SETTINGS_PAGE) },
        )
    }
}

@Preview(device = "spec:width=673dp,height=841dp")
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
private fun ExpandedPreview() {
    val widthDp = LocalConfiguration.current.screenWidthDp
    var currentRoute = remember { mutableStateOf(Route.HOME) }

    CompositionLocalProvider(
        LocalWindowWidthState provides
            if (widthDp > 480) WindowWidthSizeClass.Expanded
            else if (widthDp > 360) WindowWidthSizeClass.Medium else WindowWidthSizeClass.Compact
    ) {
        Row {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            NavigationDrawer(
                currentRoute = currentRoute.value,
                currentTopDestination = currentRoute.value,
                drawerState = drawerState,
                onNavigateToRoute = { currentRoute.value = it },
                onDismissRequest = {},
            ) {
                DownloadPageImplV2(taskDownloadStateMap = remember { mutableStateMapOf() }) { _, _
                    ->
                }
            }
        }
    }
}
