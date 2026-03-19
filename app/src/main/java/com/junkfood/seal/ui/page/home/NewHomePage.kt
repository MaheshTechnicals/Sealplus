package com.junkfood.seal.ui.page.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.alerts.DownloadDetailsDialog
import com.junkfood.seal.ui.alerts.RecentDownloadDetailsDialog
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.component.ActiveDownloadCard
import com.junkfood.seal.ui.component.card.RecentDownloadCard
import com.junkfood.seal.ui.component.URLInputField
import com.junkfood.seal.ui.icons.PlusIcon
import com.junkfood.seal.ui.page.downloadv2.UiAction
import com.junkfood.seal.ui.page.downloadv2.configure.Config
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialog
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.ui.page.downloadv2.configure.FormatPage
import com.junkfood.seal.ui.page.downloadv2.configure.PlaylistSelectionPage
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.junkfood.seal.util.SPONSOR_DIALOG_FREQUENCY
import com.junkfood.seal.util.SPONSOR_DIALOG_LAST_SHOWN
import com.junkfood.seal.util.SPONSOR_FREQ_OFF
import com.junkfood.seal.util.SPONSOR_FREQ_WEEKLY
import com.junkfood.seal.util.getErrorReport
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.matchUrlFromClipboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomePage(
    modifier: Modifier = Modifier,
    onMenuOpen: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    dialogViewModel: DownloadDialogViewModel,
    downloader: DownloaderV2 = koinInject(),
) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val activity = context as? Activity

    var showExitDialog by remember { mutableStateOf(false) }
    var urlText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Pre-fill URL from share intent
    val sharedUrl by dialogViewModel.sharedUrlFlow.collectAsState()
    LaunchedEffect(sharedUrl) {
        if (sharedUrl.isNotBlank()) {
            urlText = sharedUrl
            dialogViewModel.consumeSharedUrl()
        }
    }

    // Get lifecycle owner
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // State to track lifecycle and force refresh
    var lifecycleRefreshTrigger by remember { mutableIntStateOf(0) }

    // Monitor lifecycle events to trigger refresh when screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleRefreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission states
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var permissionsChecked by remember { mutableStateOf(false) }
    var showSponsorDialog by remember { mutableStateOf(false) }

    // Check notification permission
    val hasNotificationPermission = remember(lifecycleRefreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed below Android 13
        }
    }

    // Check battery optimization
    val isBatteryOptimizationDisabled = remember(lifecycleRefreshTrigger) {
        val pm = context.getSystemService(PowerManager::class.java)
        pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    // Notification permission launcher - tries system permission first
    // Notification settings launcher - opens app notification settings
    val notificationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Permission state will be checked on resume */ }

    // Battery optimization launcher
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Result handled by remembering state */ }


    // Check permissions on first load
    LaunchedEffect(Unit) {
        if (!permissionsChecked) {
            permissionsChecked = true
            if (!hasNotificationPermission) {
                showNotificationPermissionDialog = true
            } else if (!isBatteryOptimizationDisabled) {
                showBatteryOptimizationDialog = true
            }
        }
        // Sponsor support dialog — delay slightly so permissions dialogs get priority
        delay(600L)
        val frequency = SPONSOR_DIALOG_FREQUENCY.getInt()
        if (frequency != SPONSOR_FREQ_OFF) {
            val lastShown = SPONSOR_DIALOG_LAST_SHOWN.getLong()
            val intervalMs = if (frequency == SPONSOR_FREQ_WEEKLY)
                7L * 24 * 60 * 60 * 1000
            else
                30L * 24 * 60 * 60 * 1000
            val now = System.currentTimeMillis()
            if (lastShown == 0L || now - lastShown >= intervalMs) {
                showSponsorDialog = true
            }
        }
    }

    // Monitor permission state changes to show next dialog when user returns from settings
    LaunchedEffect(hasNotificationPermission, isBatteryOptimizationDisabled) {
        if (permissionsChecked) {
            // If notification dialog was shown and is now dismissed
            if (!showNotificationPermissionDialog && hasNotificationPermission && !isBatteryOptimizationDisabled) {
                // Show battery optimization dialog after notification permission is granted
                showBatteryOptimizationDialog = true
            }
        }
    }

    // Get recent downloads from database - will refresh when lifecycleRefreshTrigger changes
    val recentDownloads by remember(lifecycleRefreshTrigger) {
        DatabaseUtil.getDownloadHistoryFlow()
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Get recent 5 downloads (remove duplicates by video URL and path to prevent duplicate cards)
    val recentFiveDownloads = remember(recentDownloads) {
        recentDownloads
            .distinctBy { it.videoUrl + it.videoPath } // Use both URL and path to ensure uniqueness
            .takeLast(5)
            .reversed()
    }

    // Get active downloads with proper state observation for real-time updates
    val taskStateMap = downloader.getTaskStateMap()

    // Create a comprehensive set of identifiers from recent downloads to avoid duplicates
    val recentDownloadIdentifiers = remember(recentFiveDownloads) {
        recentFiveDownloads.flatMap { download ->
            listOf(
                download.videoUrl,
                download.videoPath,
                "${download.videoUrl}|${download.videoPath}" // Combined key for extra safety
            )
        }.toSet()
    }

    // Filter active downloads - only show non-completed tasks OR completed tasks not in recent downloads
    val activeDownloads by remember(taskStateMap, recentDownloadIdentifiers) {
        derivedStateOf {
            taskStateMap.toList().filter { (task, state) ->
                val downloadState = state.downloadState
                val isCompleted = downloadState is Task.DownloadState.Completed

                when {
                    // If completed, check if it's already in recent downloads
                    isCompleted && downloadState is Task.DownloadState.Completed -> {
                        val filePath = downloadState.filePath
                        val taskUrl = task.url

                        // Don't show if URL, path, or combined key is in recent downloads
                        val isInRecent = recentDownloadIdentifiers.contains(taskUrl) ||
                                recentDownloadIdentifiers.contains(filePath) ||
                                recentDownloadIdentifiers.contains("$taskUrl|$filePath")

                        !isInRecent // Show only if NOT in recent downloads
                    }
                    // Show all non-completed tasks (running, canceled, error, etc.)
                    else -> true
                }
            }
        }
    }

    // Handle back press to show exit confirmation
    BackHandler {
        showExitDialog = true
    }

    // Notification Permission Dialog
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showNotificationPermissionDialog = false
                if (!isBatteryOptimizationDisabled) {
                    showBatteryOptimizationDialog = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.notification_permission_required),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.notification_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Open notification settings directly
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            notificationSettingsLauncher.launch(intent)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.grant_permission),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        if (!isBatteryOptimizationDisabled) {
                            showBatteryOptimizationDialog = true
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Battery Optimization Dialog
    if (showBatteryOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.battery_configuration),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.battery_configuration_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.battery_settings_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryOptimizationDialog = false
                        val intent =
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                        batteryOptimizationLauncher.launch(intent)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.open_settings),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatteryOptimizationDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Sponsor support dialog
    if (showSponsorDialog) {
        SponsorSupportDialog(
            onDismiss = {
                showSponsorDialog = false
                SPONSOR_DIALOG_LAST_SHOWN.updateLong(System.currentTimeMillis())
            },
            onSupport = {
                showSponsorDialog = false
                SPONSOR_DIALOG_LAST_SHOWN.updateLong(System.currentTimeMillis())
                onNavigateToSupport()
            },
        )
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            title = { Text(stringResource(R.string.exit_app_title)) },
            text = { Text(stringResource(R.string.exit_app_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text(stringResource(R.string.exit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuOpen) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSupport) {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = "Support Developer",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onNavigateToDownloads) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = stringResource(R.string.downloads_history),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Seal+ Branding with animated glowing "+"
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Seal",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        PlusIcon()
                    }
                }
            }

            // URL Input Field with Download Button
            item {
                URLInputField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    onDownloadClick = {
                        if (urlText.isNotBlank()) {
                            view.slightHapticFeedback()
                            dialogViewModel.postAction(Action.ShowSheet(listOf(urlText)))
                            urlText = ""
                            keyboardController?.hide()
                        } else {
                            context.makeToast(R.string.url_empty)
                        }
                    },
                    onPasteClick = {
                        val clipText = clipboardManager.getText()?.text
                        if (clipText != null) {
                            context.matchUrlFromClipboard(clipText).let { url ->
                                urlText = url
                                context.makeToast(R.string.paste_msg)
                            }
                        }
                    }
                )
            }

            // Recent Downloads Section - combines both active and completed
            if (taskStateMap.isNotEmpty() || recentFiveDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.recent_downloads),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Show active downloads first
            if (activeDownloads.isNotEmpty()) {
                items(
                    items = activeDownloads,
                    key = { (task, _) -> task.id }
                ) { (task, state) ->
                    var showDetailsDialog by remember { mutableStateOf(false) }
                    var detailsTask by remember { mutableStateOf<Task?>(null) }
                    var detailsState by remember { mutableStateOf<Task.State?>(null) }

                    ActiveDownloadCard(
                        task = task,
                        state = state,
                        onAction = { action ->
                            view.slightHapticFeedback()
                            when (action) {
                                UiAction.Pause -> downloader.pause(task)
                                UiAction.Cancel -> downloader.cancel(task)
                                UiAction.Delete -> downloader.remove(task)
                                UiAction.Resume -> downloader.resume(task)
                                UiAction.Retry -> downloader.restart(task)
                                is UiAction.CopyErrorReport -> {
                                    clipboardManager.setText(
                                        AnnotatedString(getErrorReport(action.throwable, task.url))
                                    )
                                    context.makeToast(R.string.error_copied)
                                }

                                is UiAction.CopyVideoURL -> {
                                    clipboardManager.setText(AnnotatedString(task.url))
                                    context.makeToast(R.string.link_copied)
                                }

                                UiAction.ShowDetails -> {
                                    detailsTask = task
                                    detailsState = state
                                    showDetailsDialog = true
                                }

                                is UiAction.OpenFile -> {
                                    action.filePath?.let {
                                        FileUtil.openFile(path = it) {
                                            context.makeToast(R.string.file_unavailable)
                                        }
                                    }
                                }

                                is UiAction.OpenThumbnailURL -> {
                                    uriHandler.openUri(action.url)
                                }

                                is UiAction.OpenVideoURL -> {
                                    uriHandler.openUri(action.url)
                                }

                                is UiAction.ShareFile -> {
                                    val shareTitle = context.getString(R.string.share)
                                    FileUtil.createIntentForSharingFile(action.filePath)?.let {
                                        context.startActivity(Intent.createChooser(it, shareTitle))
                                    }
                                }
                            }
                        }
                    )

                    if (showDetailsDialog && detailsTask != null && detailsState != null) {
                        DownloadDetailsDialog(
                            task = detailsTask!!,
                            state = detailsState!!,
                            onDismiss = { showDetailsDialog = false }
                        )
                    }
                }
            }

            // Show recent completed downloads
            if (recentFiveDownloads.isNotEmpty()) {
                items(
                    items = recentFiveDownloads,
                    key = { it.id }
                ) { downloadInfo ->
                    var showRecentDetailsDialog by remember { mutableStateOf(false) }

                    RecentDownloadCard(
                        downloadInfo = downloadInfo,
                        onClick = {
                            FileUtil.openFile(downloadInfo.videoPath) {
                                context.makeToast(R.string.file_unavailable)
                            }
                        },
                        onShare = {
                            view.slightHapticFeedback()
                            val shareTitle = context.getString(R.string.share)
                            FileUtil.createIntentForSharingFile(downloadInfo.videoPath)?.let {
                                context.startActivity(Intent.createChooser(it, shareTitle))
                            }
                        },
                        onCopyLink = {
                            view.slightHapticFeedback()
                            clipboardManager.setText(AnnotatedString(downloadInfo.videoUrl))
                            context.makeToast(R.string.link_copied)
                        },
                        onShowDetails = {
                            view.slightHapticFeedback()
                            showRecentDetailsDialog = true
                        }
                    )

                    if (showRecentDetailsDialog) {
                        RecentDownloadDetailsDialog(
                            downloadInfo = downloadInfo,
                            onDismiss = { showRecentDetailsDialog = false }
                        )
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Download Dialog
    var preferences by remember {
        mutableStateOf(DownloadUtil.DownloadPreferences.createFromPreferences())
    }
    val sheetValue by dialogViewModel.sheetValueFlow.collectAsStateWithLifecycle()
    val dialogState by dialogViewModel.sheetStateFlow.collectAsStateWithLifecycle()
    val selectionState = dialogViewModel.selectionStateFlow.collectAsStateWithLifecycle().value

    var showDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(sheetValue) {
        if (sheetValue == DownloadDialogViewModel.SheetValue.Expanded) {
            showDialog = true
        } else {
            launch { sheetState.hide() }.invokeOnCompletion { showDialog = false }
        }
    }

    if (showDialog) {
        DownloadDialog(
            state = dialogState,
            sheetState = sheetState,
            config = Config(),
            preferences = preferences,
            onPreferencesUpdate = { preferences = it },
            onActionPost = { dialogViewModel.postAction(it) },
        )
    }

    when (selectionState) {
        is DownloadDialogViewModel.SelectionState.FormatSelection ->
            FormatPage(
                state = selectionState,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) },
            )

        is DownloadDialogViewModel.SelectionState.PlaylistSelection -> {
            PlaylistSelectionPage(
                state = selectionState,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) },
            )
        }

        DownloadDialogViewModel.SelectionState.Idle -> {}
    }
}










