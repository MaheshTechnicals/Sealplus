package com.junkfood.seal.ui.page.settings.network

import android.content.res.Configuration
import android.webkit.CookieManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GeneratingTokens
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.CookieProfile
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.common.booleanState
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DialogSwitchItem
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.HelpDialog
import com.junkfood.seal.ui.component.PasteFromClipBoardButton
import com.junkfood.seal.ui.component.PreferenceItemVariant
import com.junkfood.seal.ui.component.PreferenceSwitchWithContainer
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.ui.component.TextButtonWithIcon
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.ui.theme.generateLabelColor
import com.junkfood.seal.util.COOKIES
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.DownloadUtil.toCookiesFileContent
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.USER_AGENT
import com.junkfood.seal.util.matchUrlFromClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieProfilePage(
    cookiesViewModel: CookiesViewModel,
    navigateToCookieGeneratorPage: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    val cookies = cookiesViewModel.cookiesFlow.collectAsState(emptyList()).value
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by cookiesViewModel.stateFlow.collectAsStateWithLifecycle()
    var showClearCookieDialog by remember { mutableStateOf(false) }
    var isCookieEnabled by remember { mutableStateOf(COOKIES.getBoolean()) }
    val cookieManager = CookieManager.getInstance()
    var showHelpDialog by remember { mutableStateOf(false) }
    val view = LocalView.current

    var cookieList by remember { mutableStateOf(listOf<Cookie>()) }

    var cookieRefreshKey by remember { mutableIntStateOf(0) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManualCookieDialog by remember { mutableStateOf(false) }

    fun refreshCookies() {
        scope.launch {
            // Do the heavy IO work (CookieManager read + file write) on IO dispatcher,
            // then assign the result back on the main thread where Compose state lives.
            val list = withContext(Dispatchers.IO) {
                DownloadUtil.getCookieListFromDatabase().getOrNull()?.also { result ->
                    // writeContentToFile uses File.writeText() which can throw IOException
                    // (e.g. disk full). Wrapping in runCatching prevents the exception
                    // from propagating out of this supervised coroutine — an unhandled
                    // exception in rememberCoroutineScope reaches the thread's uncaught
                    // exception handler and can trigger a crash dialog.
                    runCatching {
                        FileUtil.writeContentToFile(result.toCookiesFileContent(), context.getCookiesFile())
                    }
                }
            }
            // State mutation on main thread — satisfies Compose snapshot threading contract.
            list?.let { cookieList = it }
        }
    }

    LaunchedEffect(cookieRefreshKey) {
        val list = withContext(Dispatchers.IO) {
            DownloadUtil.getCookieListFromDatabase().getOrNull()?.also { result ->
                runCatching {
                    FileUtil.writeContentToFile(result.toCookiesFileContent(), context.getCookiesFile())
                }
            }
        }
        // Back on main (LaunchedEffect dispatcher) — safe to mutate Compose state.
        list?.let { cookieList = it }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshCookies()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(cookieList.toCookiesFileContent().toByteArray())
                    }
                }
            }
        }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(modifier = Modifier, text = stringResource(id = R.string.cookies)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.how_does_it_work),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.show_more_actions),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        var userAgent by USER_AGENT.booleanState
                        fun toggleUserAgent(boolean: Boolean = !userAgent) {
                            expanded = false
                            userAgent = boolean
                            USER_AGENT.updateBoolean(boolean)
                        }
                        DropdownMenuItem(
                            modifier =
                                Modifier.toggleable(
                                    value = userAgent,
                                    onValueChange = ::toggleUserAgent,
                                ),
                            leadingIcon = {
                                Checkbox(
                                    checked = userAgent,
                                    onCheckedChange = null,
                                    modifier = Modifier.clearAndSetSemantics {},
                                )
                            },
                            text = { Text(stringResource(id = R.string.ua_header)) },
                            onClick = ::toggleUserAgent,
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.FileCopy, null, tint = MaterialTheme.colorScheme.secondary) },
                            text = { Text(stringResource(id = R.string.export_to_file)) },
                            enabled = cookieList.isNotEmpty(),
                            onClick = {
                                expanded = false
                                exportLauncher.launch(
                                    "cookies_exported${System.currentTimeMillis()}.txt"
                                )
                            },
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.tertiary) },
                            text = { Text(stringResource(id = R.string.clear_all_cookies)) },
                            onClick = {
                                expanded = false
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                showClearCookieDialog = true
                            },
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier, contentPadding = paddingValues) {
            item {
                PreferenceSwitchWithContainer(
                    title = stringResource(R.string.use_cookies),
                    icon = null,
                    isChecked = isCookieEnabled,
                    onClick = {
                        if (isCookieEnabled) {
                            isCookieEnabled = false
                            COOKIES.updateBoolean(false)
                        } else if (
                            // Disable help gate when any profile has manual cookies,
                            // because those are always available without CookieManager.
                            (cookies.isEmpty() || (!cookieManager.hasCookies() && cookies.none { it.content.isNotEmpty() })) && !isCookieEnabled
                        ) {
                            showHelpDialog = true
                        } else {
                            isCookieEnabled = true
                            COOKIES.updateBoolean(true)
                        }
                    },
                )
            }
            itemsIndexed(cookies) { _, item ->
                PreferenceItemVariant(
                    modifier = Modifier.padding(vertical = 4.dp),
                    title = item.url,
                    // Show badge when the profile has manually pasted cookies so the
                    // user knows this profile does not rely on the in-app browser.
                    description = if (item.content.isNotEmpty())
                        stringResource(R.string.manual_cookies_active) else null,
                    onClick = {
                        cookiesViewModel.setEditingProfile(item)
                        showEditDialog = true
                    },
                    onClickLabel = stringResource(id = R.string.edit),
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        cookiesViewModel.setEditingProfile(item)
                        showDeleteDialog = true
                    },
                    onLongClickLabel = stringResource(R.string.remove),
                )
            }

            item {
                PreferenceItemVariant(
                    title = stringResource(id = R.string.generate_new_cookies),
                    icon = Icons.Outlined.Add,
                ) {
                    cookiesViewModel.setEditingProfile()
                    showEditDialog = true
                }
            }
            item {
                androidx.compose.material3.HorizontalDivider()
                val cookiesCount = cookieList.size
                val siteCount = cookieList.distinctBy { it.domain }.size
                Text(
                    text = stringResource(R.string.cookies_in_database, cookiesCount, siteCount),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (showEditDialog) {
        CookieGeneratorDialog(
            cookiesViewModel = cookiesViewModel,
            navigateToCookieGeneratorPage = {
                showEditDialog = false
                cookiesViewModel.updateCookieProfile()
                navigateToCookieGeneratorPage()
            },
            onPasteCookiesManually = {
                showEditDialog = false
                showManualCookieDialog = true
            },
        ) {
            showEditDialog = false
            cookieRefreshKey++
        }
    }

    if (showManualCookieDialog) {
        ManualCookieInputDialog(
            cookiesViewModel = cookiesViewModel,
            onDismissRequest = {
                showManualCookieDialog = false
                cookieRefreshKey++
            },
        )
    }

    if (showDeleteDialog) {
        DeleteCookieDialog(cookiesViewModel) { showDeleteDialog = false }
    }

    if (showHelpDialog) {
        HelpDialog(
            text = stringResource(id = R.string.cookies_usage_msg),
            onDismissRequest = { showHelpDialog = false },
        )
    }
    if (showClearCookieDialog) {
        ClearCookiesDialog(onDismissRequest = { showClearCookieDialog = false }) {
            view.slightHapticFeedback()
            scope
                .launch(Dispatchers.IO) { CookieManager.getInstance().removeAllCookies(null) }
                .invokeOnCompletion { cookieRefreshKey++ }
        }
    }
}

@Composable
fun CookieGeneratorDialog(
    cookiesViewModel: CookiesViewModel,
    navigateToCookieGeneratorPage: () -> Unit = {},
    onPasteCookiesManually: () -> Unit = {},
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by cookiesViewModel.stateFlow.collectAsStateWithLifecycle()
    val profile = state.editingCookieProfile
    val url = profile.url
    val hasManualCookies = profile.content.isNotEmpty()

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { CookieManager.getInstance().flush() } }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Cookie, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.cookies)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    value = url,
                    label = { Text("URL") },
                    onValueChange = { cookiesViewModel.updateUrl(it) },
                    trailingIcon = {
                        PasteFromClipBoardButton {
                            cookiesViewModel.updateUrl(context.matchUrlFromClipboard(it))
                        }
                    },
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )

                // Option 1: generate via the in-app WebView browser
                TextButtonWithIcon(
                    onClick = { navigateToCookieGeneratorPage() },
                    icon = Icons.Outlined.GeneratingTokens,
                    text = stringResource(id = R.string.generate_new_cookies),
                )

                // Option 2: paste cookies from an external browser (Firefox, Kiwi…)
                TextButtonWithIcon(
                    onClick = { onPasteCookiesManually() },
                    icon = Icons.Outlined.ContentPaste,
                    text = stringResource(id = R.string.paste_cookies_manually),
                )

                // If the profile already has manual cookies, offer a clear action.
                if (hasManualCookies) {
                    TextButtonWithIcon(
                        onClick = {
                            cookiesViewModel.updateContent("")
                            scope.launch(Dispatchers.IO) {
                                cookiesViewModel.updateCookieProfile(
                                    profile.copy(content = "")
                                )
                            }
                            onDismissRequest()
                        },
                        icon = Icons.Outlined.DeleteForever,
                        text = stringResource(id = R.string.clear_manual_cookies),
                    )
                }
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        confirmButton = {
            ConfirmButton(enabled = url.isNotEmpty()) {
                cookiesViewModel.updateCookieProfile()
                onDismissRequest()
            }
        },
    )
}

/**
 * Dialog that lets the user paste cookies manually into a profile.
 *
 * Accepts three formats (auto-detected):
 *  1. Netscape HTTP Cookie File (tab-separated, as produced by cookies.txt exporters)
 *  2. JSON array (as exported by "Cookie-Editor" / "EditThisCookie" browser extensions)
 *  3. Cookie header value ("name=value; name=value; …")
 *
 * The content is stored in [CookieProfile.content] and takes priority over
 * CookieManager when [DownloadUtil.getCookieListFromDatabase] runs.
 */
/**
 * Full-control cookie paste dialog using [Dialog] + [Surface] instead of [AlertDialog].
 *
 * [AlertDialog] allocates its `text` slot with `weight(1f, fill=false)` inside a Box that
 * can take the full dialog height. Any Column placed there (with or without verticalScroll)
 * ends up with a blank gap between the last child and the action buttons because the Box
 * expands to its weight-allocated height.
 *
 * Using a raw [Dialog] wrapping `Box(fillMaxSize, contentAlignment=Center)` → [Surface]
 * (no height modifier) → Column makes the dialog exactly as tall as its content.
 * The Box is required because without it the Dialog's platform window provides full-screen
 * height constraints that would force the Surface to be screen-tall.
 */
@Composable
fun ManualCookieInputDialog(
    cookiesViewModel: CookiesViewModel,
    onDismissRequest: () -> Unit = {},
) {
    val state by cookiesViewModel.stateFlow.collectAsStateWithLifecycle()
    val profile = state.editingCookieProfile
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cookieText by remember { mutableStateOf(profile.content) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.readText()
                    ?: ""
            }.getOrDefault("")
            if (content.isNotEmpty()) {
                withContext(kotlinx.coroutines.Dispatchers.Main) { cookieText = content }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Box(fillMaxSize + contentAlignment=Center) is required so the Surface
        // can size itself to content height naturally. Without this outer Box,
        // the Dialog's platform window provides full-screen height constraints
        // which force the Surface to be screen-tall regardless of wrapContentHeight().
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                // No height modifier — Column child determines height naturally.
                modifier = Modifier.fillMaxWidth(fraction = 0.92f),
                shape = MaterialTheme.shapes.extraLarge,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                ) {
                    // Icon
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp),
                    )

                    // Title
                    Text(
                        text = stringResource(R.string.manual_cookie_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )

                    // Compact format hint
                    Text(
                        text = stringResource(R.string.manual_cookie_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.cookie_account_ban_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    // Cookie text field — fixed 180 dp, maxLines=8 for deterministic height
                    OutlinedTextField(
                        value = cookieText,
                        onValueChange = { cookieText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.manual_cookie_input_hint),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        ),
                        maxLines = 8,
                    )

                    // Action row — [Paste] [Import from file] ····· [🗑 icon]
                    // Three TextButtonWithIcon items overflow on small screens, so the
                    // Clear action uses an icon-only IconButton at the trailing end.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButtonWithIcon(
                            onClick = {
                                val clip = clipboardManager.getText()?.text ?: ""
                                if (clip.isNotEmpty()) cookieText = clip
                            },
                            icon = Icons.Outlined.ContentPaste,
                            text = stringResource(R.string.paste),
                        )
                        TextButtonWithIcon(
                            onClick = {
                                importFileLauncher.launch(
                                    arrayOf("text/plain", "application/json", "*/*")
                                )
                            },
                            icon = Icons.Outlined.FolderOpen,
                            text = stringResource(R.string.import_cookies_from_file),
                        )
                        // Push trash icon to the far right
                        Spacer(modifier = Modifier.weight(1f))
                        if (cookieText.isNotEmpty()) {
                            IconButton(onClick = { cookieText = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteForever,
                                    contentDescription = stringResource(R.string.clear_manual_cookies),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    // Dialog buttons — right-aligned to match app style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        DismissButton { onDismissRequest() }
                        ConfirmButton(
                            enabled = profile.url.trim().length > "https://".length &&
                                      cookieText.isNotBlank(),
                        ) {
                            cookiesViewModel.updateContent(cookieText)
                            scope.launch(Dispatchers.IO) {
                                cookiesViewModel.updateCookieProfile(
                                    profile.copy(content = cookieText)
                                )
                            }
                            onDismissRequest()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteCookieDialog(cookiesViewModel: CookiesViewModel, onDismissRequest: () -> Unit = {}) {
    val state by cookiesViewModel.stateFlow.collectAsState()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.remove)) },
        text = {
            Text(
                stringResource(R.string.remove_cookie_profile_desc)
                    .format(state.editingCookieProfile.url),
                style = LocalTextStyle.current.copy(lineBreak = LineBreak.Paragraph),
            )
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        confirmButton = {
            ConfirmButton {
                cookiesViewModel.deleteCookieProfile()
                onDismissRequest()
            }
        },
        icon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.tertiary) },
    )
}

@Composable
fun ClearCookiesDialog(onDismissRequest: () -> Unit = {}, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.clear_all_cookies)) },
        text = {
            Text(
                stringResource(R.string.clear_all_cookies_desc),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        confirmButton = {
            ConfirmButton {
                onConfirm()
                onDismissRequest()
            }
        },
        icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.tertiary) },
    )
}

@Composable
fun CookiesQuickSettingsDialog(
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {},
    cookieProfiles: List<CookieProfile> = emptyList(),
    onCookieProfileClicked: (CookieProfile) -> Unit = {},
    isCookiesEnabled: Boolean = false,
    onCookiesToggled: (Boolean) -> Unit = {},
) {
    SealDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton(
                text = stringResource(id = androidx.appcompat.R.string.abc_action_mode_done)
            ) {
                onDismissRequest()
                onConfirm()
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Cookie, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(text = stringResource(id = R.string.cookies), textAlign = TextAlign.Center)
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.refresh_cookies_desc),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    //                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn() {
                    items(items = cookieProfiles) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable { onCookieProfileClicked(it) }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier.padding(end = 12.dp)
                                        .size(16.dp)
                                        .background(
                                            color = it.url.hashCode().generateLabelColor(),
                                            shape = CircleShape,
                                        )
                                        .clearAndSetSemantics {}
                            ) {}
                            Text(
                                text = it.url
                                //                                , style =
                                // MaterialTheme.typography.labelLarge
                                ,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                DialogSwitchItem(
                    text = stringResource(id = R.string.use_cookies),
                    value = isCookiesEnabled,
                    onValueChange = onCookiesToggled,
                )
            }
        },
    )
}

@Preview
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CookiesQuickSettingsDialogPreview() {
    SealTheme {
        var isCookiesEnabled by remember { mutableStateOf(false) }
        CookiesQuickSettingsDialog(
            cookieProfiles =
                mutableListOf<CookieProfile>().apply {
                    repeat(4) {
                        add(
                            CookieProfile(id = it, url = "https://www.example$it.com", content = "")
                        )
                    }
                },
            isCookiesEnabled = isCookiesEnabled,
            onCookiesToggled = { isCookiesEnabled = it },
        )
    }
}
