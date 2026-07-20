package com.junkfood.seal.ui.page.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.page.downloadv2.UiAction
import com.junkfood.seal.ui.page.downloadv2.configure.Config
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialog
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.ui.page.downloadv2.configure.FormatPage
import com.junkfood.seal.ui.page.downloadv2.configure.PlaylistSelectionPage
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import java.io.File
import com.junkfood.seal.util.toFileSizeText
import com.junkfood.seal.util.getErrorReport
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.matchUrlFromClipboard
import com.junkfood.seal.util.SPONSOR_DIALOG_FREQUENCY
import com.junkfood.seal.util.SPONSOR_DIALOG_LAST_SHOWN
import com.junkfood.seal.util.SPONSOR_FREQ_OFF
import com.junkfood.seal.util.SPONSOR_FREQ_WEEKLY
import com.junkfood.seal.util.BATTERY_DIALOG_DISMISSED
import com.junkfood.seal.util.BATTERY_LAST_KNOWN_DISABLED
import com.junkfood.seal.util.BatteryUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateLong
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    
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
    var lifecycleRefreshTrigger by remember { mutableStateOf(0) }
    
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
    // NOTE: BATTERY_DIALOG_DISMISSED is a one-time "don't show again" flag. On its own it would
    // stay `true` forever once set, even if the user later flips the OS setting from "No
    // restrictions" back to "Optimized"/"Restricted" — a real regression that can happen
    // accidentally (OS update, user exploring settings, restoring a backup, etc). We detect that
    // regression below by comparing against the last known-good state (BATTERY_LAST_KNOWN_DISABLED)
    // and re-arm BATTERY_DIALOG_DISMISSED so the dialog is shown again — this must work
    // identically on every OEM/Android version since it is pure app-level state, independent of
    // any manufacturer-specific quirk.
    var batteryDialogDismissed by remember { mutableStateOf(BATTERY_DIALOG_DISMISSED.getBoolean()) }
    val isBatteryOptimizationDisabled = remember(lifecycleRefreshTrigger) {
        BatteryUtil.isIgnoringBatteryOptimizations(context)
    }
    LaunchedEffect(isBatteryOptimizationDisabled) {
        val wasDisabledBefore = BATTERY_LAST_KNOWN_DISABLED.getBoolean()
        if (wasDisabledBefore && !isBatteryOptimizationDisabled) {
            // Regression detected: battery optimization was off before, now it's back on.
            // Re-arm the dialog so the user is prompted again.
            BATTERY_DIALOG_DISMISSED.updateBoolean(false)
            batteryDialogDismissed = false
        }
        BATTERY_LAST_KNOWN_DISABLED.updateBoolean(isBatteryOptimizationDisabled)
    }
    val dismissBatteryDialog = {
        BATTERY_DIALOG_DISMISSED.updateBoolean(true)
        batteryDialogDismissed = true
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
            } else if (!isBatteryOptimizationDisabled && !batteryDialogDismissed) {
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
    // NOTE: keyed on batteryDialogDismissed too — this is required for the regression re-arm
    // above to reliably trigger this effect regardless of coroutine launch ordering.
    LaunchedEffect(hasNotificationPermission, isBatteryOptimizationDisabled, batteryDialogDismissed) {
        if (permissionsChecked) {
            // If notification dialog was shown and is now dismissed
            if (!showNotificationPermissionDialog && hasNotificationPermission && !isBatteryOptimizationDisabled && !batteryDialogDismissed) {
                // Show battery optimization dialog after notification permission is granted
                showBatteryOptimizationDialog = true
            }
        }
    }
    
    // Always-on collection: LaunchedEffect is tied to the composition lifetime (not Android
    // lifecycle), so Room emissions are NEVER missed — not when on the back stack, not when
    // the app is backgrounded, not during navigation transitions. This prevents the stale-card
    // bug where a deletion on VideoListPage wasn't reflected until the process was killed.
    var recentDownloads by remember { mutableStateOf(emptyList<DownloadedVideoInfo>()) }
    LaunchedEffect(Unit) {
        DatabaseUtil.getVisibleDownloadHistoryFlow().collect { list ->
            recentDownloads = list
        }
    }

    // Tracks IDs that have been hidden this session for instant optimistic UI removal
    var localHiddenIds by remember { mutableStateOf(setOf<Int>()) }

    // Purge stale IDs from localHiddenIds once the DB confirms their removal,
    // so a later re-insertion doesn't get incorrectly suppressed.
    LaunchedEffect(recentDownloads) {
        val currentIds = recentDownloads.map { it.id }.toSet()
        localHiddenIds = localHiddenIds.intersect(currentIds)
    }
    
    // Get recent 5 downloads (remove duplicates by video URL and path to prevent duplicate cards)
    val recentFiveDownloads = remember(recentDownloads) {
        recentDownloads
            .distinctBy { it.videoUrl + it.videoPath } // Use both URL and path to ensure uniqueness
            .takeLast(5)
            .reversed()
    }
    
    // Get active downloads with proper state observation for real-time updates.
    // SnapshotStateMap is a stable reference; derivedStateOf tracks snapshot reads internally.
    val taskStateMap = downloader.getTaskStateMap()

    // Build the set of URLs that currently have an *active* (non-completed) task so we can
    // suppress those entries from the "completed" database section and avoid dual-listing.
    val activeTaskUrls by remember {
        derivedStateOf {
            taskStateMap
                .filter { (_, state) -> state.downloadState !is Task.DownloadState.Completed }
                .keys
                .map { it.url }
                .toSet()
        }
    }

    // Create a comprehensive set of identifiers from recent downloads to avoid duplicates
    val recentDownloadIdentifiers = remember(recentFiveDownloads) {
        recentFiveDownloads.flatMap { download ->
            listOf(
                download.videoUrl,
                download.videoPath,
                "${download.videoUrl}|${download.videoPath}"
            )
        }.toSet()
    }

    // Filter active downloads:
    //   • Always show non-completed tasks (running, queued, paused, canceled, error)
    //   • Show completed tasks only if they haven't yet appeared in the recent-downloads DB section
    // Sort order (strict, stable):
    //   1. Running / FetchingInfo   — actively working right now
    //   2. ReadyWithInfo            — info fetched, waiting for a download slot (more advanced than Idle)
    //   3. Idle                     — just queued, nothing started yet
    //   4. Paused                   — user paused
    //   5. Canceled / Error         — terminal but user-visible
    //   6. Completed                — transition state before DB write, shown below
    // Within each group: newer tasks (higher timeCreated) appear first.
    // IMPORTANT: recentDownloadIdentifiers is a plain Set (not snapshot state), so
    // derivedStateOf cannot track it. We pass it as a key to remember() so the
    // derivedStateOf object is recreated (and re-evaluated) whenever the DB-backed
    // identifiers set changes — e.g. when a completed task is flushed to the DB.
    val activeDownloads by remember(recentDownloadIdentifiers) {
        derivedStateOf {
            taskStateMap
                .filter { (task, state) ->
                    val ds = state.downloadState
                    when {
                        ds is Task.DownloadState.Completed -> {
                            val filePath = ds.filePath
                            val taskUrl  = task.url
                            val isInRecent =
                                recentDownloadIdentifiers.contains(taskUrl) ||
                                recentDownloadIdentifiers.contains(filePath) ||
                                recentDownloadIdentifiers.contains("$taskUrl|$filePath")
                            !isInRecent
                        }
                        else -> true
                    }
                }
                .toList()
                .sortedWith(
                    compareBy<Pair<Task, Task.State>> { (_, state) ->
                        downloadStateSortPriority(state.downloadState)
                    }.thenByDescending { (task, _) -> task.timeCreated }
                )
        }
    }

    // Exclude recent-DB entries whose URL still has a live (non-completed) active task so items
    // don't appear in both sections simultaneously during the Running → Completed transition.
    // Also exclude optimistically hidden items so the card vanishes before the DB Flow re-emits.
    val recentFiveDownloadsFiltered = remember(recentFiveDownloads, activeTaskUrls, localHiddenIds) {
        recentFiveDownloads.filter { it.videoUrl !in activeTaskUrls && it.id !in localHiddenIds }
    }

    // Prune Completed tasks from the in-memory taskStateMap as soon as their DB row is confirmed
    // present. taskStateMap lives inside the process-scoped DownloaderV2 singleton, so a
    // Completed task otherwise lingers there FOREVER (it's only ever removed via the Active
    // Downloads card's own delete button). If left unpruned and the user later deletes/hides
    // that same item from History or Recent Downloads, the DB row disappears but the stale
    // Completed task does not — so it silently satisfies the "not yet synced to DB" condition
    // in activeDownloads below and gets wrongly resurrected as a ghost card. This only became
    // visible after a fresh process start because enqueueFromBackup() never restores Completed
    // tasks, masking the leak on cold boot. Pruning here, right when we know the DB has taken
    // over as source of truth, closes that gap.
    // Checked against the FULL visible download list (not just the last-5 window used for the
    // Recent Downloads cards) so a task that ages out of the top 5 still gets pruned instead of
    // resurfacing indefinitely.
    val allVisibleIdentifiers = remember(recentDownloads) {
        recentDownloads.flatMap { download ->
            listOf(
                download.videoUrl,
                download.videoPath,
                "${download.videoUrl}|${download.videoPath}"
            )
        }.toSet()
    }
    LaunchedEffect(allVisibleIdentifiers) {
        taskStateMap.entries
            .filter { (task, state) ->
                val downloadState = state.downloadState
                downloadState is Task.DownloadState.Completed &&
                    (allVisibleIdentifiers.contains(task.url) ||
                        allVisibleIdentifiers.contains(downloadState.filePath) ||
                        allVisibleIdentifiers.contains("${task.url}|${downloadState.filePath}"))
            }
            .map { it.key }
            .forEach { task -> downloader.remove(task) }
    }

    // Hoisted state for the Spotify-style swipeable download-details sheet: holds the index
    // (within recentFiveDownloadsFiltered) of the card whose details are being viewed, or null
    // when the sheet is closed. Kept as an index (not the item itself) so left/right swipes
    // inside the sheet can move to adjacent list entries.
    var detailsDialogIndex by remember { mutableStateOf<Int?>(null) }
    
    // Handle back press to show exit confirmation
    BackHandler {
        showExitDialog = true
    }
    
    // Notification Permission Dialog
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNotificationPermissionDialog = false
                if (!isBatteryOptimizationDisabled && !batteryDialogDismissed) {
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
                        if (!isBatteryOptimizationDisabled && !batteryDialogDismissed) {
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
            onDismissRequest = {
                showBatteryOptimizationDialog = false
                dismissBatteryDialog()
            },
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
                    val batteryDescResId = remember {
                        BatteryUtil.getBatterySettingsDescResId(BatteryUtil.getManufacturer())
                    }
                    Text(
                        text = stringResource(batteryDescResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryOptimizationDialog = false
                        dismissBatteryDialog()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = BatteryUtil.buildBatterySettingsIntent(context)
                            batteryOptimizationLauncher.launch(intent)
                        }
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
                    onClick = {
                        showBatteryOptimizationDialog = false
                        dismissBatteryDialog()
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
            icon = { Icon(Icons.Outlined.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
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
                        AnimatedGlowingPlus()
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
                            context.matchUrlFromClipboard(clipText)?.let { url ->
                                urlText = url
                                context.makeToast(R.string.paste_msg)
                            } ?: context.makeToast(R.string.paste_fail_msg)
                        }
                    }
                )
            }
            
            // Recent Downloads Section - combines both active and completed.
            // Use activeDownloads (not taskStateMap) so the header hides correctly when all
            // tasks are Completed and already present in the DB-backed section.
            if (activeDownloads.isNotEmpty() || recentFiveDownloadsFiltered.isNotEmpty()) {
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
                    var showActiveDeleteDialog by remember { mutableStateOf(false) }
                    
                    ActiveDownloadCard(
                        task = task,
                        state = state,
                        onAction = { action ->
                            view.slightHapticFeedback()
                            when (action) {
                                UiAction.Pause -> downloader.pause(task)
                                UiAction.Cancel -> downloader.cancel(task)
                                UiAction.Delete -> showActiveDeleteDialog = true
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

                    if (showActiveDeleteDialog) {
                        val deleteTitle = state.viewState.title.ifBlank { task.url }
                        SealDialog(
                            onDismissRequest = { showActiveDeleteDialog = false },
                            title = { Text(text = stringResource(R.string.delete_info)) },
                            icon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            text = {
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    text = stringResource(R.string.delete_info_msg).format(deleteTitle),
                                )
                            },
                            confirmButton = {
                                ConfirmButton {
                                    showActiveDeleteDialog = false
                                    scope.launch(Dispatchers.IO) {
                                        val videoId = state.videoInfo?.id
                                        val baseName =
                                            state.videoInfo?.title?.ifBlank { deleteTitle }
                                                ?: deleteTitle
                                        FileUtil.deleteTempFilesForTask(baseName, videoId)
                                        downloader.remove(task)
                                    }
                                }
                            },
                            dismissButton = { DismissButton { showActiveDeleteDialog = false } },
                        )
                    }
                }
            }
            
            // Show recent completed downloads
            if (recentFiveDownloadsFiltered.isNotEmpty()) {
                items(
                    items = recentFiveDownloadsFiltered,
                    key = { it.id }
                ) { downloadInfo ->
                    var showRecentDeleteDialog by remember { mutableStateOf(false) }
                    
                    RecentDownloadCard(
                        downloadInfo = downloadInfo,
                        refreshKey = lifecycleRefreshTrigger,
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
                            val idx = recentFiveDownloadsFiltered.indexOfFirst { it.id == downloadInfo.id }
                            detailsDialogIndex = if (idx >= 0) idx else 0
                        },
                        onDelete = {
                            view.slightHapticFeedback()
                            showRecentDeleteDialog = true
                        },
                        onHide = {
                            view.slightHapticFeedback()
                            // Optimistically remove from UI immediately, then persist to DB
                            localHiddenIds = localHiddenIds + downloadInfo.id
                            scope.launch(Dispatchers.IO) {
                                DatabaseUtil.hideItem(downloadInfo)
                            }
                        }
                    )
                    
                    if (showRecentDeleteDialog) {
                        SealDialog(
                            onDismissRequest = { showRecentDeleteDialog = false },
                            title = { Text(text = stringResource(R.string.delete_info)) },
                            icon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            text = {
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                    text = stringResource(R.string.delete_info_msg).format(downloadInfo.videoTitle),
                                )
                            },
                            confirmButton = {
                                ConfirmButton {
                                    showRecentDeleteDialog = false
                                    localHiddenIds = localHiddenIds + downloadInfo.id
                                    scope.launch(Dispatchers.IO) {
                                        val baseName =
                                            File(downloadInfo.videoPath)
                                                .nameWithoutExtension
                                                .ifEmpty { downloadInfo.videoTitle }
                                        FileUtil.deleteTempFilesForTask(baseName, downloadInfo.videoId)
                                        DatabaseUtil.deleteInfoList(
                                            infoList = listOf(downloadInfo),
                                            deleteFile = false
                                        )
                                    }
                                }
                            },
                            dismissButton = { DismissButton { showRecentDeleteDialog = false } },
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

    // Spotify-style swipeable details sheet for recent downloads. A single sheet instance is
    // shared across all cards; detailsDialogIndex tracks which item (by position in
    // recentFiveDownloadsFiltered) is currently shown, so left/right swipes just move the index.
    detailsDialogIndex?.let { index ->
        // The underlying list is a live Flow and can shrink while the sheet is open (e.g. the
        // user deletes/hides the item they're viewing). Clamp to the last valid index, or close
        // the sheet entirely if nothing is left to show.
        if (recentFiveDownloadsFiltered.isEmpty()) {
            detailsDialogIndex = null
        } else {
            val safeIndex = index.coerceIn(0, recentFiveDownloadsFiltered.lastIndex)
            RecentDownloadDetailsDialog(
                downloadInfoList = recentFiveDownloadsFiltered,
                initialIndex = safeIndex,
                onDismiss = { detailsDialogIndex = null }
            )
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

@Composable
fun URLInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onDownloadClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    val fullPlaceholder = stringResource(R.string.enter_url_to_download)

    // Typewriter animation: reveal characters one by one
    var displayedLength by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        displayedLength = 0
        for (i in 1..fullPlaceholder.length) {
            delay(50L)
            displayedLength = i
        }
    }

    // Gradient animation for the placeholder text
    val infiniteTransition = rememberInfiniteTransition(label = "placeholderGradient")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "placeholderShift"
    )

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )

    val gradientBrush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(gradientShift, 0f),
        end = Offset(gradientShift + 400f, 0f),
        tileMode = TileMode.Mirror
    )
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        placeholder = {
            Text(
                text = fullPlaceholder.take(displayedLength),
                style = MaterialTheme.typography.bodyLarge.merge(
                    TextStyle(brush = gradientBrush)
                )
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(32.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDownloadClick() }),
        trailingIcon = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (value.isEmpty()) {
                    IconButton(onClick = onPasteClick) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = "Paste",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                FilledIconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 4.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isGradientDark && isDarkTheme) {
                            GradientDarkColors.GradientPrimaryStart
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = stringResource(R.string.download),
                        tint = Color.White
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isGradientDark && isDarkTheme) {
                GradientDarkColors.GradientPrimaryStart
            } else {
                MaterialTheme.colorScheme.primary
            },
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun RecentDownloadCard(
    downloadInfo: DownloadedVideoInfo,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onShowDetails: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit = {},
    refreshKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    var showMenu by remember { mutableStateOf(false) }
    // Use produceState so fileExists is re-checked on every recomposition trigger (refreshKey
    // changes on ON_RESUME), and also whenever the video path itself changes.
    val fileExists by produceState(initialValue = java.io.File(downloadInfo.videoPath).exists(), key1 = downloadInfo.videoPath, key2 = refreshKey) {
        value = java.io.File(downloadInfo.videoPath).exists()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (fileExists) 1f else 0.55f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGradientDark && isDarkTheme) {
                GradientDarkColors.SurfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = downloadInfo.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = downloadInfo.videoTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fileExists) {
                        Text(
                            text = stringResource(R.string.completed),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isGradientDark && isDarkTheme) {
                                Color(0xFF4ADE80)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.BrokenImage,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.file_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // More button with dropdown menu
            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.details)) },
                        onClick = {
                            onShowDetails()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = {
                            onShare()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy_link)) },
                        onClick = {
                            onCopyLink()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.hide)) },
                        onClick = {
                            onHide()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveDownloadCard(
    task: Task,
    state: Task.State,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    var showMenu by remember { mutableStateOf(false) }
    
    val downloadState = state.downloadState
    val progress = when (downloadState) {
        is Task.DownloadState.Running -> downloadState.progress
        is Task.DownloadState.Paused -> downloadState.progress ?: -1f
        is Task.DownloadState.Canceled -> downloadState.progress ?: -1f
        else -> 0f
    }
    
    // Parse progress text to determine download phase
    val progressText = if (downloadState is Task.DownloadState.Running) downloadState.progressText else ""
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Track download state: format info -> video download -> audio download -> merging
    var hasSeenFormatInfo by remember { mutableStateOf(false) }
    var hasSeenVideoComplete by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf("downloading") }
    
    // Determine phase based on progressText patterns
    // NOTE: DownloaderV2 strips the "[download] " prefix before storing progressText,
    // so download progress lines look like "45.3% of 10.00MiB at 2.50MiB/s ETA 00:03"
    // or "100% of 10.00MiB in 00:04". We detect them by looking for the % sign with digits.
    val downloadPhase = when {
        // Merging phase — [Merger] prefix is NOT stripped
        progressText.contains("[Merger]", ignoreCase = true) ||
        progressText.contains("Merging formats", ignoreCase = true) -> {
            currentPhase = "merging"
            hasSeenVideoComplete = false
            hasSeenFormatInfo = false
            "merging"
        }
        // Format info line — [info] prefix is NOT stripped
        progressText.contains("[info]", ignoreCase = true) && progressText.contains("format", ignoreCase = true) -> {
            hasSeenFormatInfo = true
            hasSeenVideoComplete = false
            currentPhase = "downloading"
            "downloading"
        }
        // Download progress lines — "[download]" prefix stripped; match % pattern instead
        progressText.matches(Regex("""^\d+(\.\d+)?%.*""")) || progressText.contains("% of ") -> {
            when {
                // 100% completion — video stream done, audio stream is next
                (progressText.startsWith("100%") || progressText.contains("100% of ")) && !hasSeenVideoComplete -> {
                    hasSeenVideoComplete = true
                    currentPhase = "video"
                    "video"
                }
                // After video complete, any download progress is the audio stream
                hasSeenVideoComplete -> {
                    currentPhase = "audio"
                    "audio"
                }
                // Before any 100% seen, first stream is always video
                hasSeenFormatInfo -> {
                    currentPhase = "video"
                    "video"
                }
                else -> {
                    currentPhase = "downloading"
                    "downloading"
                }
            }
        }
        // Post-download file operations — maintain current phase
        progressText.contains("Deleting original file", ignoreCase = true) ||
        progressText.contains("[Metadata]", ignoreCase = true) ||
        progressText.contains("[MoveFiles]", ignoreCase = true) -> {
            currentPhase
        }
        // yt-dlp re-outputs [youtube] / "Downloading webpage" lines between streams.
        // In Running state we are always downloading (FetchingInfo state handles the fetch phase).
        progressText.contains("[youtube]", ignoreCase = true) ||
        progressText.contains("Downloading webpage", ignoreCase = true) ||
        progressText.contains("Downloading player", ignoreCase = true) -> {
            if (hasSeenVideoComplete) {
                // yt-dlp is initializing the second (audio) stream
                currentPhase = "audio"
                "audio"
            } else {
                currentPhase  // Maintain current phase — never show "fetching" while running
            }
        }
        else -> currentPhase
    }
    
    val statusText = when (downloadState) {
        is Task.DownloadState.Running -> {
            val pct = if (progress >= 0) " ${(progress * 100).toInt()}%" else ""
            when (downloadPhase) {
                "merging" -> stringResource(R.string.status_merging)
                "video"   -> "Downloading video...$pct"
                "audio"   -> "Downloading audio...$pct"
                "fetching" -> stringResource(R.string.fetching_info)
                else -> if (progress >= 0) "Downloading... ${(progress * 100).toInt()}%"
                        else stringResource(R.string.status_downloading)
            }
        }
        is Task.DownloadState.Paused -> if (progress >= 0) stringResource(R.string.status_paused) + " ${(progress * 100).toInt()}%" else stringResource(R.string.status_paused)
        is Task.DownloadState.Canceled -> stringResource(R.string.status_canceled)
        is Task.DownloadState.Error -> stringResource(R.string.download_error)
        is Task.DownloadState.Completed -> stringResource(R.string.completed) + " 100%"
        is Task.DownloadState.FetchingInfo -> stringResource(R.string.fetching_info)
        // Idle = waiting for a download slot to open; ReadyWithInfo = info fetched, waiting to start
        Task.DownloadState.Idle,
        Task.DownloadState.ReadyWithInfo -> stringResource(R.string.queue_status)
        else -> ""
    }
    
    val statusColor = when (downloadState) {
        is Task.DownloadState.Running -> if (isGradientDark && isDarkTheme) {
            GradientDarkColors.GradientPrimaryStart
        } else {
            MaterialTheme.colorScheme.primary
        }
        is Task.DownloadState.Paused -> Color(0xFFFBBF24)
        is Task.DownloadState.Canceled -> Color(0xFFEF4444)
        is Task.DownloadState.Error -> Color(0xFFEF4444)
        is Task.DownloadState.Completed -> Color(0xFF4ADE80)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Parse speed and ETA from yt-dlp progressText.
    // Example line: "45.3% of 10.00MiB at 2.50MiB/s ETA 00:03"
    val speedEtaText = if (downloadState is Task.DownloadState.Running && progressText.isNotEmpty()) {
        val speed = Regex("""at\s+([\d.]+\s*\S+/s)""").find(progressText)?.groupValues?.get(1)
        val eta = Regex("""ETA\s+(\d+:\d+)""").find(progressText)?.groupValues?.get(1)
        when {
            speed != null && eta != null -> "$speed  •  ETA $eta"
            speed != null -> speed
            eta != null -> "ETA $eta"
            else -> null
        }
    } else null
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGradientDark && isDarkTheme) {
                GradientDarkColors.SurfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                state.videoInfo?.thumbnail?.let { thumbnailUrl ->
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = state.viewState.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Show Queue badge for Idle or ReadyWithInfo tasks
                        if (downloadState is Task.DownloadState.Idle || downloadState is Task.DownloadState.ReadyWithInfo) {
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isGradientDark && isDarkTheme) {
                                    GradientDarkColors.GradientSecondaryStart.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.queue_status),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isGradientDark && isDarkTheme) {
                                        GradientDarkColors.GradientSecondaryEnd
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Speed + ETA line — only shown during active download
                    if (speedEtaText != null) {
                        Text(
                            text = speedEtaText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Pause/Resume action button
                if (downloadState is Task.DownloadState.Running) {
                    IconButton(
                        onClick = { onAction(UiAction.Pause) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Pause,
                            contentDescription = stringResource(R.string.pause),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                if (downloadState is Task.DownloadState.Paused) {
                    IconButton(
                        onClick = { onAction(UiAction.Resume) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.resume),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // More button with dropdown menu
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        val downloadState = state.downloadState
                        
                        // Pause option for running downloads
                        if (downloadState is Task.DownloadState.Running) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pause)) },
                                onClick = {
                                    onAction(UiAction.Pause)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Pause,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                        
                        // Resume option for paused downloads
                        if (downloadState is Task.DownloadState.Paused) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.resume)) },
                                onClick = {
                                    onAction(UiAction.Resume)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                        
                        // Retry option for canceled or failed downloads
                        if (downloadState is Task.DownloadState.Canceled || downloadState is Task.DownloadState.Error) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.retry)) },
                                onClick = {
                                    onAction(UiAction.Retry)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                        
                        // Cancel option for running/fetching/paused/queued downloads.
                        // Idle and ReadyWithInfo are queued states — DownloaderV2.cancelImpl()
                        // handles them correctly but the UI must expose the action.
                        if (downloadState is Task.DownloadState.Running ||
                            downloadState is Task.DownloadState.FetchingInfo ||
                            downloadState is Task.DownloadState.Paused ||
                            downloadState == Task.DownloadState.Idle ||
                            downloadState == Task.DownloadState.ReadyWithInfo) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cancel)) },
                                onClick = {
                                    onAction(UiAction.Cancel)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Cancel,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            )
                        }
                        
                        // Copy link option
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy_link)) },
                            onClick = {
                                onAction(UiAction.CopyVideoURL)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        )
                        
                        // Details option (only for completed downloads)
                        if (downloadState is Task.DownloadState.Completed) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.details)) },
                                onClick = {
                                    onAction(UiAction.ShowDetails)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )
                        }
                        
                        // Delete option
                        if (downloadState is Task.DownloadState.Completed || downloadState is Task.DownloadState.Error || downloadState is Task.DownloadState.Canceled) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    onAction(UiAction.Delete)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            // Progress bar for active and paused downloads
            if (downloadState is Task.DownloadState.Running || downloadState is Task.DownloadState.Paused) {
                val barColor = when (downloadState) {
                    is Task.DownloadState.Paused -> Color(0xFFFBBF24)
                    else -> if (isGradientDark && isDarkTheme) GradientDarkColors.GradientPrimaryStart
                            else MaterialTheme.colorScheme.primary
                }
                if (progress >= 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadDetailsDialog(
    task: Task,
    state: Task.State,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFilePathDialog by remember { mutableStateOf(false) }
    
    BackHandler {
        onDismiss()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.download_details),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.viewState.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Thumbnail Card
            state.videoInfo?.thumbnail?.let { thumbnailUrl ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Media Information Section
            Text(
                text = stringResource(R.string.media_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Grid Layout for Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: File Format and File Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.viewState.videoFormats?.firstOrNull()?.ext?.let { ext ->
                        if (ext.isNotBlank()) {
                            DetailCard(
                                icon = Icons.Outlined.VideoFile,
                                label = stringResource(R.string.file_format),
                                value = ext.uppercase(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    val fileSize = state.viewState.fileSizeApprox
                    if (fileSize > 0) {
                        DetailCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.file_size),
                            value = fileSize.toFileSizeText(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 2: Creator and Platform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.viewState.uploader.isNotBlank()) {
                        DetailCard(
                            icon = Icons.Outlined.Person,
                            label = stringResource(R.string.video_creator_label),
                            value = state.viewState.uploader,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (state.viewState.extractorKey.isNotBlank()) {
                        DetailCard(
                            icon = Icons.Outlined.Language,
                            label = stringResource(R.string.platform),
                            value = state.viewState.extractorKey,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 3: File Path and Download Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.downloadState is Task.DownloadState.Completed) {
                        state.downloadState.filePath?.let { path ->
                            DetailCard(
                                icon = Icons.Outlined.Folder,
                                label = stringResource(R.string.file_path),
                                value = path,
                                modifier = Modifier.weight(1f),
                                onClick = { showFilePathDialog = true }
                            )
                        }
                    }
                    
                    DetailCard(
                        icon = Icons.Outlined.CalendarToday,
                        label = stringResource(R.string.download_date),
                        value = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(task.timeCreated)),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Source URL Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(state.viewState.url))
                            context.makeToast(R.string.link_copied)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.source_url),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = state.viewState.url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // File Path Dialog
    if (showFilePathDialog && state.downloadState is Task.DownloadState.Completed) {
        state.downloadState.filePath?.let { path ->
            AlertDialog(
                onDismissRequest = { showFilePathDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.file_path),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    SelectionContainer {
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFilePathDialog = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }
    }
}

/**
 * Spotify-style swipeable download-details sheet. Wraps a [HorizontalPager] over
 * [downloadInfoList] so the user can swipe left/right to move between the details of
 * adjacent recent-download cards without closing and reopening the sheet. When the list
 * has only one item, the pager still mounts but swiping is disabled (no-op, no crash).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecentDownloadDetailsDialog(
    downloadInfoList: List<DownloadedVideoInfo>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val safeInitialIndex = initialIndex.coerceIn(0, (downloadInfoList.size - 1).coerceAtLeast(0))
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = safeInitialIndex
    ) { downloadInfoList.size }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current

    // BUG FIX — continuous up/down flicker after releasing a swipe-up gesture:
    //
    // The sheet's own drag-to-expand/dismiss gesture and each page's inner verticalScroll
    // both live on the same vertical axis. Once a swipe-up reaches the top/bottom of the
    // scrollable content, any velocity/delta the inner scroll can't consume is normally
    // handed UP to the ModalBottomSheet's drag handler (that's how nested scroll works by
    // default: children consume first, parents get the "pre" pass and any leftovers).
    // Because the sheet height is a fixed 85% of the screen, its resting/expanded anchor sits
    // very close to the content's natural bounds on most devices — so that tiny leftover
    // delta/velocity is just enough to nudge the sheet, which nudges the scroll state back,
    // which nudges the sheet again... an unstable feedback loop that never settles. That is
    // the repeating up/down flicker: it does not stop on its own once triggered by a
    // swipe-and-release.
    //
    // Fix: swallow all scroll deltas AND fling velocity in a NestedScrollConnection placed
    // between the pager/content and the ModalBottomSheet, so nothing above this point ever
    // sees "leftover" scroll to react to. The sheet still drags normally from its own drag
    // handle / empty areas — this only isolates the content's internal scrolling.
    val swallowNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                available
        }
    }

    BackHandler {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(swallowNestedScroll),
        ) {
            // Page indicator dots — only shown when there's more than one item to swipe between.
            if (downloadInfoList.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(downloadInfoList.size) { dotIndex ->
                        val isSelected = dotIndex == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }

            // IMPORTANT: use a FIXED height here (not heightIn(max=...)). Each page's intrinsic
            // content height differs (some items have extra detail rows, longer URLs, etc.).
            // With only a max-height constraint, HorizontalPager re-measures its own height to
            // match whichever page is being settled on during a drag, and that continuous
            // height renegotiation while the inner verticalScroll is also tracking its own
            // bounds is what produced the up/down flicker when scrolling a page to its end.
            // Giving every page the exact same fixed height removes that feedback loop entirely.
            val pagerHeight = (configuration.screenHeightDp * 0.85f).dp
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pagerHeight),
                // Disable swipe gestures entirely when there's nothing to swipe to — keeps a
                // single-item list on its one and only details page with no dead gesture area.
                userScrollEnabled = downloadInfoList.size > 1,
            ) { page ->
                RecentDownloadDetailsContent(
                    downloadInfo = downloadInfoList[page],
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun RecentDownloadDetailsContent(
    downloadInfo: DownloadedVideoInfo,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFilePathDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.download_details),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = downloadInfo.videoTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Thumbnail Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                AsyncImage(
                    model = downloadInfo.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Media Information Section
            Text(
                text = stringResource(R.string.media_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Grid Layout for Details
            val file = java.io.File(downloadInfo.videoPath)
            val fileExtension = downloadInfo.videoPath.substringAfterLast(".", "")
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: File Format and File Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (fileExtension.isNotEmpty()) {
                        DetailCard(
                            icon = Icons.Outlined.VideoFile,
                            label = stringResource(R.string.file_format),
                            value = fileExtension.uppercase(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (file.exists()) {
                        val fileSize = file.length()
                        DetailCard(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(R.string.file_size),
                            value = fileSize.toFileSizeText(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Row 2: Resolution and Platform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Extract resolution from video file
                    val resolution = remember(downloadInfo.videoPath) {
                        try {
                            if (file.exists()) {
                                val retriever = android.media.MediaMetadataRetriever()
                                retriever.setDataSource(downloadInfo.videoPath)
                                val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                                val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                                retriever.release()
                                
                                if (width != null && height != null) {
                                    "${width}x${height}"
                                } else {
                                    "N/A"
                                }
                            } else {
                                "N/A"
                            }
                        } catch (e: Exception) {
                            "N/A"
                        }
                    }
                    
                    if (resolution != "N/A") {
                        DetailCard(
                            icon = Icons.Outlined.HighQuality,
                            label = stringResource(R.string.resolution),
                            value = resolution,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    DetailCard(
                        icon = Icons.Outlined.Language,
                        label = stringResource(R.string.platform),
                        value = downloadInfo.extractor,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Row 3: File Path and Download Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailCard(
                        icon = Icons.Outlined.Folder,
                        label = stringResource(R.string.file_path),
                        value = downloadInfo.videoPath,
                        modifier = Modifier.weight(1f),
                        onClick = { showFilePathDialog = true }
                    )
                    
                    val downloadDate = if (file.exists()) {
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(file.lastModified()))
                    } else {
                        "N/A"
                    }
                    DetailCard(
                        icon = Icons.Outlined.CalendarToday,
                        label = stringResource(R.string.download_date),
                        value = downloadDate,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Download Time and Average Speed
                if (downloadInfo.downloadTimeMillis > 0L || downloadInfo.averageSpeedBytesPerSec > 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (downloadInfo.downloadTimeMillis > 0L) {
                            DetailCard(
                                icon = Icons.Outlined.Timer,
                                label = stringResource(R.string.download_time),
                                value = formatDownloadTime(downloadInfo.downloadTimeMillis),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (downloadInfo.averageSpeedBytesPerSec > 0L) {
                            DetailCard(
                                icon = Icons.Outlined.Speed,
                                label = stringResource(R.string.average_speed),
                                value = formatAverageSpeed(downloadInfo.averageSpeedBytesPerSec),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Source URL Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(downloadInfo.videoUrl))
                            context.makeToast(R.string.link_copied)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.source_url),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = downloadInfo.videoUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
    }

    // File Path Dialog
    if (showFilePathDialog) {
        AlertDialog(
            onDismissRequest = { showFilePathDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.file_path),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                SelectionContainer {
                    Text(
                        text = downloadInfo.videoPath,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilePathDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

private fun formatDownloadTime(millis: Long): String {
    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatAverageSpeed(bytesPerSec: Long): String {
    val mb = 1024L * 1024L
    val kb = 1024L
    return when {
        bytesPerSec >= mb -> "%.1f MB/s".format(bytesPerSec.toDouble() / mb)
        bytesPerSec >= kb -> "${bytesPerSec / kb} KB/s"
        else -> "$bytesPerSec B/s"
    }
}

@Composable
private fun DetailCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else Modifier
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Returns a numeric sort priority for a [Task.DownloadState] so that the
 * Recent Downloads list always shows items in the order:
 *
 *   Running                (0)    →  actively downloading NOW
 *   FetchingInfo           (1)    →  actively fetching metadata NOW
 *   ReadyWithInfo          (2)    →  info fetched, waiting for a download slot
 *   Idle                   (3)    →  just queued, nothing started yet
 *   Paused                 (4)    →  user-paused
 *   Canceled               (5)    →  user-canceled
 *   Error                  (6)    →  failed
 *   Completed              (7)    →  transition state before DB flush
 *
 * Lower number = shown closer to the top of the list.
 */
private fun downloadStateSortPriority(state: Task.DownloadState): Int = when (state) {
    is Task.DownloadState.Running       -> 0  // actively downloading right now
    is Task.DownloadState.FetchingInfo  -> 1  // actively fetching metadata right now
    Task.DownloadState.ReadyWithInfo    -> 2  // info fetched, waiting for a download slot — more advanced than Idle
    Task.DownloadState.Idle             -> 3  // just queued, nothing started yet
    is Task.DownloadState.Paused        -> 4  // user-paused
    is Task.DownloadState.Canceled      -> 5  // user-canceled
    is Task.DownloadState.Error         -> 6  // failed
    is Task.DownloadState.Completed     -> 7  // done, transitioning to DB section
}

/**
 * Animated glowing "+" text with continuously cycling gradient colors
 * and a pulsing glow effect that matches the app theme.
 */
@Composable
fun AnimatedGlowingPlus() {
    val infiniteTransition = rememberInfiniteTransition(label = "plusGlow")

    // Animate the gradient offset to make colors flow continuously
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientShift"
    )

    // Animate glow intensity (pulsing alpha for the shadow)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )

    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.6f)

    val gradientBrush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(gradientShift, 0f),
        end = Offset(gradientShift + 500f, 500f),
        tileMode = TileMode.Mirror
    )

    Text(
        text = "+",
        style = MaterialTheme.typography.displayMedium.merge(
            TextStyle(
                brush = gradientBrush,
                shadow = Shadow(
                    color = glowColor,
                    offset = Offset.Zero,
                    blurRadius = 16f * glowAlpha
                )
            )
        ),
        fontWeight = FontWeight.Bold
    )
}