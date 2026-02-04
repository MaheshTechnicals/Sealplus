package com.junkfood.seal.ui.page.home

import android.app.Activity
import android.content.Intent
import java.io.File
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentCopy
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
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import coil.compose.AsyncImage
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
import com.junkfood.seal.ui.page.videolist.RemoveItemDialog
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.toFileSizeText
import com.junkfood.seal.util.toDurationText
import com.junkfood.seal.util.getErrorReport
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.matchUrlFromClipboard
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewHomePage(
    modifier: Modifier = Modifier,
    onMenuOpen: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
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
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(Icons.Outlined.ExitToApp, contentDescription = null) },
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
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDownloads) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = stringResource(R.string.downloads_history)
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
            // Seal+ Branding
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Seal+",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        placeholder = { 
            Text(
                text = stringResource(R.string.enter_url_to_download),
                style = MaterialTheme.typography.bodyLarge
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
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
                }
            }
            
            // More button with dropdown menu
            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options"
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
                                contentDescription = null
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
                                contentDescription = null
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
                                contentDescription = null
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
        is Task.DownloadState.Paused -> downloadState.progress ?: 0f
        is Task.DownloadState.Canceled -> downloadState.progress ?: 0f
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
    val downloadPhase = when {
        // Merging phase
        progressText.contains("[Merger]", ignoreCase = true) || 
        progressText.contains("Merging formats", ignoreCase = true) -> {
            currentPhase = "merging"
            hasSeenVideoComplete = false
            hasSeenFormatInfo = false
            "merging"
        }
        // Format info line - indicates download will start
        progressText.contains("[info]", ignoreCase = true) && progressText.contains("format", ignoreCase = true) -> {
            hasSeenFormatInfo = true
            hasSeenVideoComplete = false
            currentPhase = "fetching"
            "downloading"
        }
        // Download progress lines
        progressText.contains("[download]", ignoreCase = true) -> {
            when {
                // First 100% completion - video is done, audio is next
                progressText.contains("100%") && !hasSeenVideoComplete -> {
                    hasSeenVideoComplete = true
                    currentPhase = "video"
                    "video"
                }
                // After video complete, any download progress is audio
                hasSeenVideoComplete -> {
                    currentPhase = "audio"
                    "audio"
                }
                // Before any completion, it's video (first download is always video)
                hasSeenFormatInfo -> {
                    currentPhase = "video"
                    "video"
                }
                else -> "downloading"
            }
        }
        // Post-download file operations - maintain current phase
        progressText.contains("Deleting original file", ignoreCase = true) ||
        progressText.contains("[Metadata]", ignoreCase = true) ||
        progressText.contains("[MoveFiles]", ignoreCase = true) -> {
            currentPhase
        }
        // Fetching info phase
        progressText.contains("[youtube]", ignoreCase = true) || 
        progressText.contains("Downloading webpage", ignoreCase = true) ||
        progressText.contains("Downloading player", ignoreCase = true) -> {
            "fetching"
        }
        else -> currentPhase
    }
    
    val statusText = when (downloadState) {
        is Task.DownloadState.Running -> {
            when (downloadPhase) {
                "merging" -> stringResource(R.string.status_merging)
                "video" -> "Downloading video... ${(progress * 100).toInt()}%"
                "audio" -> "Downloading audio... ${(progress * 100).toInt()}%"
                "fetching" -> stringResource(R.string.fetching_info)
                else -> "Downloading... ${(progress * 100).toInt()}%"
            }
        }
        is Task.DownloadState.Paused -> stringResource(R.string.status_paused) + " ${(progress * 100).toInt()}%"
        is Task.DownloadState.Canceled -> stringResource(R.string.status_canceled)
        is Task.DownloadState.Error -> stringResource(R.string.download_error)
        is Task.DownloadState.Completed -> stringResource(R.string.completed) + " 100%"
        is Task.DownloadState.FetchingInfo -> stringResource(R.string.fetching_info)
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // More button with dropdown menu
                Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More options"
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
                                        contentDescription = null
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
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                        
                        // Cancel option for running/fetching/paused downloads
                        if (downloadState is Task.DownloadState.Running || 
                            downloadState is Task.DownloadState.FetchingInfo ||
                            downloadState is Task.DownloadState.Paused) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cancel)) },
                                onClick = {
                                    onAction(UiAction.Cancel)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Cancel,
                                        contentDescription = null
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
                                    contentDescription = null
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
                                        contentDescription = null
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
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            // Progress bar for active and paused downloads
            if (downloadState is Task.DownloadState.Running || downloadState is Task.DownloadState.Paused) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = when (downloadState) {
                        is Task.DownloadState.Paused -> Color(0xFFFBBF24)
                        else -> if (isGradientDark && isDarkTheme) {
                            GradientDarkColors.GradientPrimaryStart
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
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
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
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
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.link_copied),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentDownloadDetailsDialog(
    downloadInfo: DownloadedVideoInfo,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = androidx.compose.ui.platform.LocalContext.current
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
                
                // Row 2: Creator and Platform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (downloadInfo.videoAuthor.isNotEmpty()) {
                        DetailCard(
                            icon = Icons.Outlined.Person,
                            label = stringResource(R.string.video_creator_label),
                            value = downloadInfo.videoAuthor,
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
                
                // Source URL Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(downloadInfo.videoUrl))
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.link_copied),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
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