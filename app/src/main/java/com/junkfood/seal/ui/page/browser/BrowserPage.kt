@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.junkfood.seal.ui.page.browser

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.Formatter
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.App
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.makeToast
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val HOME_URL = "https://www.google.com"
private const val SEARCH_URL = "https://www.google.com/search?q="

/** Values the WebView client reads from its own thread, so they must be safe to share. */
private class PageContext {
    @Volatile var pageUrl: String = HOME_URL
    @Volatile var userAgent: String = ""
}

/** Reads the rendered page HTML from the WebView and passes it to [MediaSniffer] off the UI thread. */
private fun scanPage(view: WebView, pageContext: PageContext) {
    view.evaluateJavascript("(function(){return document.documentElement.outerHTML})()") { raw ->
        // Parsing and regex scanning a large page must not run on the UI thread (it caused ANR).
        App.applicationScope.launch(Dispatchers.Default) {
            val html = runCatching { org.json.JSONArray("[$raw]").optString(0) }.getOrDefault("")
            if (html.isNotEmpty()) {
                MediaSniffer.scanPageSource(html, pageContext.pageUrl, pageContext.userAgent)
            }
        }
    }
}

/**
 * In-app browser. Media files that load while a page is open are collected by [MediaSniffer]
 * and listed from the download icon in the top bar, so they can be downloaded without cookies.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val downloader: DownloaderV2 = koinInject()
    val detected by MediaSniffer.items.collectAsStateWithLifecycle()

    val pageContext = remember { PageContext() }
    var addressText by rememberSaveable { mutableStateOf(HOME_URL) }
    var isEditingAddress by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var showNoMediaDialog by remember { mutableStateOf(false) }
    var showDetectedSheet by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<SniffedMedia?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                // Sites often open ad popups. Block them so the page stays on the same tab.
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                allowFileAccess = false
                allowContentAccess = false
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            // Use a plain Chrome-like User-Agent. WebView's default "wv" marker makes some sites
            // hide their media. The same string is sent to yt-dlp for matching downloads.
            val chromeLikeUa = settings.userAgentString.replace(Regex("\\swv\\b"), "")
            settings.userAgentString = chromeLikeUa
            pageContext.userAgent = chromeLikeUa

            webViewClient =
                object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        // Only web pages stay in the browser. Other schemes (intent:, market:,
                        // app deep links) are ignored so ads cannot open other apps.
                        val scheme = request?.url?.scheme.orEmpty()
                        return scheme != "http" && scheme != "https"
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        if (request != null && request.method == "GET") {
                            val url = request.url.toString()
                            val kind =
                                MediaSniffer.classify(
                                    url,
                                    request.requestHeaders?.get("Accept"),
                                )
                            if (kind != null) {
                                MediaSniffer.offer(
                                    url = url,
                                    pageUrl = pageContext.pageUrl,
                                    pageUserAgent = pageContext.userAgent,
                                    kind = kind,
                                )
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        pageContext.pageUrl = url.orEmpty()
                        if (!isEditingAddress && url != null) addressText = url
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        canGoBack = view?.canGoBack() == true
                        // Read the page's own source for media URLs, now and again after the
                        // player has had time to set up its qualities.
                        view?.let { v ->
                            scanPage(v, pageContext)
                            v.postDelayed({ scanPage(v, pageContext) }, 6_000)
                        }
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean,
                    ) {
                        canGoBack = view?.canGoBack() == true
                    }
                }

            webChromeClient =
                object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        progress = newProgress
                    }
                }

            loadUrl(addressText)
        }
    }

    DisposableEffect(webView) { onDispose { webView.stopLoading(); webView.destroy() } }

    BackHandler(enabled = canGoBack) { webView.goBack() }

    fun navigateTo(input: String) {
        val url = toLoadableUrl(input)
        addressText = url
        isEditingAddress = false
        focusManager.clearFocus()
        webView.loadUrl(url)
    }

    fun startDownload(media: SniffedMedia) {
        if (enqueueSniffed(context, downloader, media)) {
            context.makeToast(R.string.browser_download_started)
            pendingDownload = null
            showDetectedSheet = false
            // Back to Home, where the new task shows in "Recent downloads".
            onNavigateBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (canGoBack) webView.goBack() else onNavigateBack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            AddressField(
                modifier = Modifier.weight(1f),
                value = addressText,
                onValueChange = { addressText = it },
                onFocusChanged = { isEditingAddress = it },
                onGo = { navigateTo(addressText) },
            )

            IconButton(onClick = { webView.reload() }) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            IconButton(
                onClick = {
                    if (detected.isEmpty()) showNoMediaDialog = true else showDetectedSheet = true
                }
            ) {
                BadgedBox(
                    badge = {
                        if (detected.isNotEmpty()) {
                            Badge { Text(text = detected.size.toString()) }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = stringResource(R.string.browser_detected_title),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (progress in 0..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AndroidView(
            factory = { webView },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }

    if (showNoMediaDialog) {
        SealDialog(
            onDismissRequest = { showNoMediaDialog = false },
            confirmButton = { ConfirmButton { showNoMediaDialog = false } },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text(text = stringResource(R.string.browser_no_media_title)) },
            text = { Text(text = stringResource(R.string.browser_no_media_tips)) },
        )
    }

    pendingDownload?.let { media ->
        SniffedDownloadDialog(
            media = media,
            onConfirm = { startDownload(media) },
            onDismiss = { pendingDownload = null },
        )
    }

    if (showDetectedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetectedSheet = false },
            sheetState = sheetState,
        ) {
            DetectedFilesContent(
                items = detected,
                onOpen = { media -> pendingDownload = media },
                onCopyLink = { media ->
                    clipboard.setText(AnnotatedString(media.url))
                    context.makeToast(R.string.link_copied)
                },
                onRemove = { media -> MediaSniffer.remove(media.url) },
                onClearAll = { MediaSniffer.clear() },
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showDetectedSheet = false
                    }
                },
            )
        }
    }
}

@Composable
private fun AddressField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onGo: () -> Unit,
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
            )
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.browser_address_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private enum class SortOrder { Quality, Size, Name }

private enum class KindFilter { All, Video, Audio, Stream }

private fun SniffedKind.toFilter(): KindFilter =
    when (this) {
        SniffedKind.Video -> KindFilter.Video
        SniffedKind.Audio -> KindFilter.Audio
        SniffedKind.Stream -> KindFilter.Stream
    }

@Composable
private fun DetectedFilesContent(
    items: List<SniffedMedia>,
    onOpen: (SniffedMedia) -> Unit,
    onCopyLink: (SniffedMedia) -> Unit,
    onRemove: (SniffedMedia) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
) {
    var sort by remember { mutableStateOf(SortOrder.Quality) }
    var filter by remember { mutableStateOf(KindFilter.All) }
    var sortMenu by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }

    // Items are already sorted by quality in MediaSniffer; the other orders are applied here.
    val visible =
        items
            .filter { filter == KindFilter.All || it.kind.toFilter() == filter }
            .let { list ->
                when (sort) {
                    SortOrder.Quality -> list
                    SortOrder.Size -> list.sortedByDescending { it.sizeBytes ?: -1L }
                    SortOrder.Name -> list.sortedBy { it.fileName.lowercase() }
                }
            }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.browser_detected_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = visible.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Box {
                IconButton(onClick = { filterMenu = true }) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                    KindFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text =
                                        when (option) {
                                            KindFilter.All -> stringResource(R.string.browser_filter_all)
                                            KindFilter.Video -> stringResource(R.string.browser_kind_video)
                                            KindFilter.Audio -> stringResource(R.string.browser_kind_audio)
                                            KindFilter.Stream -> stringResource(R.string.browser_kind_stream)
                                        }
                                )
                            },
                            onClick = {
                                filter = option
                                filterMenu = false
                            },
                        )
                    }
                }
            }
        }

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.browser_no_media_tips),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(items = visible, key = { it.url }) { media ->
                    DetectedRow(
                        media = media,
                        onOpen = { onOpen(media) },
                        onCopyLink = { onCopyLink(media) },
                        onRemove = { onRemove(media) },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TextButton(onClick = onClose) { Text(text = stringResource(R.string.close)) }
            Spacer(modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) {
                TextButton(onClick = onClearAll) { Text(text = stringResource(R.string.browser_clear_all)) }
                Box {
                    TextButton(onClick = { sortMenu = true }) {
                        Text(text = stringResource(R.string.browser_sort))
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortOrder.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text =
                                            when (option) {
                                                SortOrder.Quality -> stringResource(R.string.browser_sort_quality)
                                                SortOrder.Size -> stringResource(R.string.browser_sort_size)
                                                SortOrder.Name -> stringResource(R.string.browser_sort_name)
                                            }
                                    )
                                },
                                onClick = {
                                    sort = option
                                    sortMenu = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Confirmation for one detected file. Confirm starts the download and returns to Home. */
@Composable
private fun SniffedDownloadDialog(media: SniffedMedia, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sizeText =
        media.sizeBytes?.let { (if (media.sizeIsEstimate) "~" else "") + Formatter.formatShortFileSize(context, it) }
            ?: stringResource(R.string.browser_size_unknown)
    SealDialog(
        onDismissRequest = onDismiss,
        confirmButton = { ConfirmButton(text = stringResource(R.string.download)) { onConfirm() } },
        dismissButton = { DismissButton(text = stringResource(R.string.cancel)) { onDismiss() } },
        icon = {
            Icon(
                imageVector = kindIcon(media.kind),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(text = media.fileName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                listOfNotNull(media.resolutionLabel ?: media.bitrateLabel, media.durationLabel)
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        Text(
                            text = it.joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                Text(
                    text = "${media.host} · $sizeText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun DetectedRow(
    media: SniffedMedia,
    onOpen: () -> Unit,
    onCopyLink: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val sizeText =
        media.sizeBytes?.let { (if (media.sizeIsEstimate) "~" else "") + Formatter.formatShortFileSize(context, it) }
            ?: stringResource(R.string.browser_size_unknown)
    val partsText =
        media.segments?.let { count ->
            val parts = stringResource(R.string.browser_parts, count)
            media.durationLabel?.let { "$parts · $it" } ?: parts
        }
    val kindLabel =
        when (media.kind) {
            SniffedKind.Video -> stringResource(R.string.browser_kind_video)
            SniffedKind.Audio -> stringResource(R.string.browser_kind_audio)
            SniffedKind.Stream -> stringResource(R.string.browser_kind_stream)
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpen)
                .padding(vertical = 6.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = kindIcon(media.kind),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.fileName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(media.resolutionLabel ?: media.bitrateLabel ?: kindLabel, kindLabel.takeIf { media.resolutionLabel != null || media.bitrateLabel != null }).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(media.host, sizeText, partsText).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.download)) },
                    onClick = {
                        menuOpen = false
                        onOpen()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.copy_link)) },
                    onClick = {
                        menuOpen = false
                        onCopyLink()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.remove)) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
    }
}

private fun kindIcon(kind: SniffedKind) =
    when (kind) {
        SniffedKind.Video -> Icons.Outlined.VideoFile
        SniffedKind.Audio -> Icons.Outlined.AudioFile
        SniffedKind.Stream -> Icons.Outlined.LiveTv
    }

/**
 * Starts a download for a sniffed file with the saved preferences, but always without cookies.
 * The page's Referer and User-Agent are forwarded so sites that check them still serve the file.
 */
private fun enqueueSniffed(context: Context, downloader: DownloaderV2, media: SniffedMedia): Boolean {
    if (!PreferenceUtil.isNetworkAvailableForDownload()) {
        context.makeToast(PreferenceUtil.getNetworkErrorMessage())
        return false
    }
    val preferences =
        DownloadUtil.DownloadPreferences.createFromPreferences().copy(
            extractAudio = media.kind == SniffedKind.Audio,
            cookies = false,
            downloadPlaylist = false,
            referer = media.pageUrl,
            pageUserAgent = media.pageUserAgent,
            // Use a clean name: the signed URL's query string must not end up in the file name.
            outputTemplate = "${cleanBaseName(media.fileName)}.%(ext)s",
            // The same name is used to find the finished file for Downloads history.
            newTitle = cleanBaseName(media.fileName),
            // Browser downloads always go to Downloads history, so they show in the list and in
            // "Recent downloads" even when the global Incognito setting is on.
            privateMode = false,
        )
    downloader.enqueue(Task(url = media.url, preferences = preferences))
    return true
}

/** "240P_1000K_41863125.mp4" -> "240P_1000K_41863125", with characters that break paths removed. */
internal fun cleanBaseName(fileName: String): String {
    val base = fileName.substringBeforeLast('.', fileName)
    return base.replace(Regex("[\\\\/:*?\"<>|%\\[\\]&=]"), "_").trim().take(150).ifBlank { "video" }
}

/** Turns what the user typed into a URL. Text that is not a URL becomes a Google search. */
internal fun toLoadableUrl(input: String): String {
    val text = input.trim()
    if (text.isEmpty()) return HOME_URL
    if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(text)) return text
    val looksLikeHost = !text.contains(' ') && text.contains('.')
    return if (looksLikeHost) "https://$text"
    else SEARCH_URL + URLEncoder.encode(text, "UTF-8")
}
