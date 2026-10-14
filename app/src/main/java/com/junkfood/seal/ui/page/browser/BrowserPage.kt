@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.junkfood.seal.ui.page.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.LocalWindowWidthState
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
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
    @Volatile var pageTitle: String = ""
    @Volatile var pageThumbnail: String = ""
    /** Delayed second scan of the page. Cancelled when the browser leaves, so it never runs on a destroyed WebView. */
    var rescan: Runnable? = null
}

/** Reads the rendered page HTML from the WebView and passes it to [MediaSniffer] off the UI thread. */
private fun scanPage(view: WebView, pageContext: PageContext) {
    view.evaluateJavascript("(function(){return document.documentElement.outerHTML})()") { raw ->
        // Parsing and regex scanning a large page must not run on the UI thread (it caused ANR).
        App.applicationScope.launch(Dispatchers.Default) {
            val html = runCatching { org.json.JSONArray("[$raw]").optString(0) }.getOrDefault("")
            if (html.isNotEmpty()) {
                pageContext.pageThumbnail = findPageThumbnail(html)
                MediaSniffer.scanPageSource(html, pageContext.pageUrl, pageContext.userAgent)
            }
        }
    }
}

/**
 * Shows a page's own fullscreen video (HTML5 fullscreen) inside the browser window.
 *
 * The status bar and navigation bar are NOT hidden: the video sits between them, so the clock and
 * battery stay visible while a video plays. Their icons switch to light while the black video
 * background is behind them, and are restored when fullscreen ends.
 */
private class VideoFullscreenHost(
    private val activity: Activity?,
    private val onFullscreenChanged: (Boolean) -> Unit,
) {
    private var customView: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null
    private var previousLightStatusBars: Boolean? = null

    fun show(view: View, cb: WebChromeClient.CustomViewCallback) {
        val window = activity?.window
        val decor = window?.decorView as? FrameLayout
        if (window == null || decor == null) {
            cb.onCustomViewHidden()
            return
        }
        // Only one fullscreen video at a time.
        hide()
        customView = view
        callback = cb
        view.setBackgroundColor(android.graphics.Color.BLACK)
        // Keep the video below the status bar and above the navigation bar, and out of the notch.
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        decor.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val controller = WindowInsetsControllerCompat(window, decor)
        previousLightStatusBars = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        ViewCompat.requestApplyInsets(view)
        onFullscreenChanged(true)
    }

    fun hide() {
        val view = customView ?: return
        val window = activity?.window
        (window?.decorView as? FrameLayout)?.removeView(view)
        ViewCompat.setOnApplyWindowInsetsListener(view, null)
        if (window != null) {
            previousLightStatusBars?.let { WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = it }
        }
        previousLightStatusBars = null
        customView = null
        val done = callback
        callback = null
        onFullscreenChanged(false)
        done?.onCustomViewHidden()
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/**
 * In-app browser. Media files that load while a page is open are collected by [MediaSniffer]
 * and listed in the Downloads sheet, so they can be downloaded without cookies.
 *
 * Phones get a bottom bar; tablets and landscape get a side rail.
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
    val compact = LocalWindowWidthState.current == WindowWidthSizeClass.Compact

    val pageContext = remember { PageContext() }
    var addressText by rememberSaveable { mutableStateOf(HOME_URL) }
    var isEditingAddress by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showDetectedSheet by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<SniffedMedia?>(null) }
    var longPress by remember { mutableStateOf<LongPressTarget?>(null) }
    var popupUrl by remember { mutableStateOf<String?>(null) }
    var inFullscreen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fullscreen = remember {
        VideoFullscreenHost(activity = context.findActivity()) { inFullscreen = it }
    }

    val webView = remember {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                // Sites often open ad popups. Pop-ups are caught in onCreateWindow and the user is asked first.
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                allowFileAccess = false
                allowContentAccess = false
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                // Allow http media on https pages so more sites play and can be detected.
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
                        // Forget the previous page's name and thumbnail so a new download never uses them.
                        pageContext.pageTitle = ""
                        pageContext.pageThumbnail = ""
                        if (!isEditingAddress && url != null) addressText = url
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        canGoBack = view?.canGoBack() == true
                        canGoForward = view?.canGoForward() == true
                        // Read the page's own source for media URLs, now and again after the
                        // player has had time to set up its qualities.
                        view?.let { v ->
                            v.evaluateJavascript("document.title") { raw ->
                                pageContext.pageTitle =
                                    runCatching { org.json.JSONArray("[$raw]").optString(0) }
                                        .getOrDefault("")
                            }
                            scanPage(v, pageContext)
                            pageContext.rescan?.let { v.removeCallbacks(it) }
                            Runnable { scanPage(v, pageContext) }.also {
                                pageContext.rescan = it
                                v.postDelayed(it, 6_000)
                            }
                        }
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean,
                    ) {
                        canGoBack = view?.canGoBack() == true
                        canGoForward = view?.canGoForward() == true
                    }
                }

            webChromeClient =
                object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        progress = newProgress
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        // The official page title; used to name downloaded files.
                        pageContext.pageTitle = title.orEmpty()
                    }

                    // The page's own fullscreen video. Without these the fullscreen request was
                    // ignored or hid the status bar (and the clock) for the whole video.
                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (view == null || callback == null) return
                        fullscreen.show(view, callback)
                    }

                    override fun onHideCustomView() {
                        fullscreen.hide()
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?,
                    ): Boolean {
                        if (resultMsg == null) return false
                        // Read the URL the pop-up wants in a throw-away WebView, then ask the user.
                        val probe = WebView(context)
                        probe.webViewClient =
                            object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    v: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val target = request?.url?.toString().orEmpty()
                                    v?.destroy()
                                    if (target.startsWith("http")) popupUrl = target
                                    return true
                                }
                            }
                        (resultMsg.obj as WebView.WebViewTransport).webView = probe
                        resultMsg.sendToTarget()
                        return true
                    }
                }

            // Long-press on an image or a link opens the download choice.
            setOnLongClickListener { v ->
                val hit = (v as WebView).hitTestResult
                val extra = hit.extra.orEmpty()
                when (hit.type) {
                    WebView.HitTestResult.IMAGE_TYPE,
                    WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE ->
                        if (extra.startsWith("http")) {
                            longPress = LongPressTarget.Image(extra, pageContext.pageUrl, pageContext.userAgent)
                        }
                    WebView.HitTestResult.SRC_ANCHOR_TYPE ->
                        if (extra.startsWith("http")) {
                            longPress = LongPressTarget.Link(extra, pageContext.pageUrl, pageContext.userAgent)
                        }
                }
                longPress != null
            }

            loadUrl(addressText)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            fullscreen.hide()
            pageContext.rescan?.let { webView.removeCallbacks(it) }
            webView.stopLoading()
            webView.destroy()
        }
    }

    BackHandler(enabled = canGoBack) { webView.goBack() }
    // Registered after the page-back handler, so Back leaves fullscreen video first.
    BackHandler(enabled = inFullscreen) { fullscreen.hide() }

    fun navigateTo(input: String) {
        val url = toLoadableUrl(input)
        addressText = url
        isEditingAddress = false
        focusManager.clearFocus()
        webView.loadUrl(url)
    }

    /** Starts downloads for [media] (one or many) and returns to Home. Files get unique names. */
    fun downloadMedia(media: List<SniffedMedia>) {
        if (media.isEmpty()) return
        if (!PreferenceUtil.isNetworkAvailableForDownload()) {
            context.makeToast(PreferenceUtil.getNetworkErrorMessage())
            return
        }
        // Names used by this batch, so two files from one page never collide before they are saved.
        val reserved = mutableSetOf<String>()
        media.forEach { item ->
            enqueueSniffed(downloader, item, pageContext.pageTitle, pageContext.pageThumbnail, reserved)
        }
        context.makeToast(R.string.browser_download_started)
        pendingDownload = null
        showDetectedSheet = false
        // Back to Home, where the new task shows in "Recent downloads".
        onNavigateBack()
    }

    val navActions =
        listOf(
            BrowserNavAction(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                label = stringResource(R.string.browser_nav_back),
                enabled = canGoBack,
                onClick = { webView.goBack() },
            ),
            BrowserNavAction(
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                label = stringResource(R.string.browser_nav_forward),
                enabled = canGoForward,
                onClick = { webView.goForward() },
            ),
            BrowserNavAction(
                icon = Icons.Outlined.Home,
                label = stringResource(R.string.browser_nav_home),
                enabled = true,
                onClick = { navigateTo(HOME_URL) },
            ),
            BrowserNavAction(
                icon = Icons.Outlined.FileDownload,
                label = stringResource(R.string.browser_nav_downloads),
                enabled = true,
                // Same count as the sheet: small previews are not counted.
                badge = detected.count { isMainMedia(it) },
                onClick = { showDetectedSheet = true },
            ),
        )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Scaffold does not pad the top bar for the status bar, so the address bar does it here.
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                AddressBar(
                    value = addressText,
                    loading = progress < 100,
                    onValueChange = { addressText = it },
                    onFocusChanged = { isEditingAddress = it },
                    onGo = { navigateTo(addressText) },
                    onReloadOrStop = { if (progress < 100) webView.stopLoading() else webView.reload() },
                )
                if (progress < 100) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        bottomBar = { if (compact) BrowserNavigationBar(navActions) },
    ) { inner ->
        Row(modifier = Modifier.fillMaxSize().padding(inner)) {
            if (!compact) BrowserNavigationRail(navActions)
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            }
        }
    }

    popupUrl?.let { url ->
        SealDialog(
            onDismissRequest = { popupUrl = null },
            confirmButton = {
                ConfirmButton(text = stringResource(R.string.browser_popup_open)) {
                    webView.loadUrl(url)
                    popupUrl = null
                }
            },
            dismissButton = {
                DismissButton(text = stringResource(R.string.browser_popup_block)) { popupUrl = null }
            },
            title = { Text(text = stringResource(R.string.browser_popup_title)) },
            text = { Text(text = url, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        )
    }

    longPress?.let { target ->
        SealDialog(
            onDismissRequest = { longPress = null },
            confirmButton = {
                ConfirmButton(text = stringResource(R.string.download)) {
                    when (target) {
                        is LongPressTarget.Image -> downloadImage(context, target)
                        is LongPressTarget.Link -> startLinkDownload(context, downloader, target)
                    }
                    longPress = null
                }
            },
            dismissButton = {
                DismissButton(text = stringResource(R.string.cancel)) { longPress = null }
            },
            title = {
                Text(
                    text = if (target is LongPressTarget.Image) stringResource(R.string.browser_download_image)
                    else stringResource(R.string.browser_download_link),
                )
            },
            text = {
                Text(
                    text = target.url,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }

    pendingDownload?.let { media ->
        SniffedDownloadDialog(
            media = media,
            onConfirm = { downloadMedia(listOf(media)) },
            onDismiss = { pendingDownload = null },
        )
    }

    if (showDetectedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetectedSheet = false },
            sheetState = sheetState,
        ) {
            // Centred on tablets; DetectedFilesContent caps its own width.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
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
}

/** Sends a link through the normal downloader (yt-dlp), keeping the page's Referer and User-Agent. */
private fun startLinkDownload(context: Context, downloader: DownloaderV2, target: LongPressTarget.Link) {
    if (!PreferenceUtil.isNetworkAvailableForDownload()) {
        context.makeToast(PreferenceUtil.getNetworkErrorMessage())
        return
    }
    val preferences =
        DownloadUtil.DownloadPreferences.createFromPreferences().copy(
            cookies = false,
            downloadPlaylist = false,
            referer = target.pageUrl,
            pageUserAgent = target.userAgent,
        )
    downloader.enqueue(Task(url = target.url, preferences = preferences))
    context.makeToast(R.string.browser_download_started)
}

/**
 * Starts a download for a sniffed file with the saved preferences, but always without cookies.
 * The page's Referer and User-Agent are forwarded so sites that check them still serve the file.
 *
 * [reserved] collects the names already used by this batch and is updated with the new name.
 */
private fun enqueueSniffed(
    downloader: DownloaderV2,
    media: SniffedMedia,
    pageTitle: String,
    pageThumbnailUrl: String,
    reserved: MutableSet<String>,
) {
    // Name the file after the page title (as 1DM does); fall back to the media file name.
    val wantedBase =
        if (pageTitle.isNotBlank()) cleanBaseName(pageTitle) else cleanBaseName(media.fileName)
    // Same page title again: add " (2)", " (3)"... so the new file does not collide with the old one.
    val baseName = uniqueBaseName(FileUtil.getExternalDownloadDirectory(), wantedBase, reserved)
    reserved += baseName
    val preferences =
        DownloadUtil.DownloadPreferences.createFromPreferences().copy(
            extractAudio = media.kind == SniffedKind.Audio,
            cookies = false,
            downloadPlaylist = false,
            referer = media.pageUrl,
            pageUserAgent = media.pageUserAgent,
            // Use a clean name: the signed URL's query string must not end up in the file name.
            outputTemplate = "$baseName.%(ext)s",
            // The same name is used to find the finished file for Downloads history.
            newTitle = baseName,
            // Browser downloads always go to Downloads history, so they show in the list and in
            // "Recent downloads" even when the global Incognito setting is on.
            privateMode = false,
        )
    // Start the card with the real name too, so it never shows the URL while downloading.
    val task = Task(url = media.url, preferences = preferences)
    downloader.enqueue(
        task,
        Task.State(
            downloadState = Task.DownloadState.Idle,
            videoInfo = null,
            viewState =
                Task.ViewState(
                    url = media.url,
                    title = baseName,
                    thumbnailUrl = pageThumbnailUrl.takeIf { it.isNotBlank() },
                ),
        ),
    )
}

/**
 * Returns [base] if no file in [dir] starts with "base." and [reserved] does not contain it,
 * otherwise "base (2)", "base (3)"...
 */
internal fun uniqueBaseName(dir: java.io.File, base: String, reserved: Collection<String> = emptyList()): String {
    val names = dir.list()?.toList().orEmpty()
    fun taken(candidate: String) =
        candidate in reserved || names.any { it == candidate || it.startsWith("$candidate.") }
    if (!taken(base)) return base
    var n = 2
    while (taken("$base ($n)")) n++
    return "$base ($n)"
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

/** Video poster or og:image from the page HTML: the video's own thumbnail. Empty if none. */
internal fun findPageThumbnail(html: String): String {
    val patterns =
        listOf(
            Regex("""<meta[^>]+property=["']og:image(?::secure_url)?["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]*property=["']og:image["']""", RegexOption.IGNORE_CASE),
            Regex("""<video[^>]+poster=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
        )
    for (p in patterns) {
        val url = p.find(html)?.groupValues?.get(1)?.replace("&amp;", "&").orEmpty()
        if (url.startsWith("http")) return url
    }
    return ""
}

private sealed interface LongPressTarget {
    val url: String
    val pageUrl: String
    val userAgent: String

    data class Image(override val url: String, override val pageUrl: String, override val userAgent: String) :
        LongPressTarget

    data class Link(override val url: String, override val pageUrl: String, override val userAgent: String) :
        LongPressTarget
}

/** Saves one image to Download/SealPlus/Images, sending the page's Referer and User-Agent. */
private fun downloadImage(context: Context, target: LongPressTarget.Image) {
    Thread {
            runCatching {
                val conn = java.net.URL(target.url).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Referer", target.pageUrl)
                conn.setRequestProperty("User-Agent", target.userAgent.ifBlank { "Mozilla/5.0" })
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                val type = conn.contentType.orEmpty()
                val ext =
                    when {
                        type.contains("png") -> "png"
                        type.contains("webp") -> "webp"
                        type.contains("gif") -> "gif"
                        else -> "jpg"
                    }
                val dir = java.io.File(FileUtil.getExternalDownloadDirectory(), "Images").apply { mkdirs() }
                val out = java.io.File(dir, "Image_${System.currentTimeMillis()}.$ext")
                conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
                out.absolutePath
            }
                .onSuccess { path ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        context.makeToast(context.getString(R.string.browser_image_saved, path))
                    }
                }
                .onFailure {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        context.makeToast(R.string.browser_download_failed)
                    }
                }
        }
        .start()
}
