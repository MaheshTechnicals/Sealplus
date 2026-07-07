package com.junkfood.seal.ui.page.settings.network

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.USER_AGENT_STRING

private const val TAG = "WebViewPage"

// ---------------------------------------------------------------------------
// Anti-bot-detection script
//
// Facebook and Instagram (Meta) run JavaScript bot-detection checks on their
// LOGIN pages — specifically:
//   1. Does window.chrome exist?  (Chrome has it; plain Android WebView does not)
//   2. Is navigator.webdriver true?  (automation flag)
//
// When these checks fail, Meta's React code deliberately mounts an empty root
// and renders nothing — giving a blank white/dark page even though the HTML
// arrived and the document title was set.
//
// We inject this script using WebViewCompat.addDocumentStartJavaScript() which
// runs BEFORE any page JavaScript, so the shim is in place before Meta's
// detection code executes. A fallback evaluateJavascript() call is made from
// onPageStarted for devices where addDocumentStartJavaScript is not supported.
// ---------------------------------------------------------------------------
private val ANTI_DETECTION_SCRIPT = """
(function() {
  'use strict';
  try {

    // 1. window.chrome — Meta checks for this object.
    //    Android WebView omits it; real Chrome always has it.
    if (typeof window.chrome === 'undefined' || window.chrome === null) {
      var chromeDef = {
        app: {
          isInstalled: false,
          InstallState: {
            DISABLED: 'disabled',
            INSTALLED: 'installed',
            NOT_INSTALLED: 'not_installed'
          },
          RunningState: {
            CANNOT_RUN: 'cannot_run',
            READY_TO_RUN: 'ready_to_run',
            RUNNING: 'running'
          }
        },
        runtime: {
          connect: function() {
            return { postMessage: function() {}, disconnect: function() {},
                     onMessage: { addListener: function() {}, removeListener: function() {} },
                     onDisconnect: { addListener: function() {}, removeListener: function() {} } };
          },
          sendMessage: function() {},
          onMessage: {
            addListener: function() {},
            removeListener: function() {},
            hasListener: function() { return false; }
          },
          onConnect: { addListener: function() {}, removeListener: function() {} },
          onConnectExternal: { addListener: function() {}, removeListener: function() {} },
          id: undefined
        },
        loadTimes: function() {
          return {
            requestTime: 0, startLoadTime: 0, commitLoadTime: 0,
            finishDocumentLoadTime: 0, finishLoadTime: 0,
            firstPaintTime: 0, firstPaintAfterLoadTime: 0,
            navigationType: 'Other', wasFetchedViaSpdy: false,
            wasNpnNegotiated: false, npnNegotiatedProtocol: '',
            wasAlternateProtocolAvailable: false, connectionInfo: 'unknown'
          };
        },
        csi: function() {
          return { startE: Date.now(), onloadT: Date.now(), pageT: 0, tran: 15 };
        }
      };
      try {
        Object.defineProperty(window, 'chrome', {
          value: chromeDef,
          writable: true,
          enumerable: true,
          configurable: true
        });
      } catch (e) {
        window.chrome = chromeDef;
      }
    }

    // 2. navigator.webdriver — must be false/undefined, not true.
    try {
      Object.defineProperty(navigator, 'webdriver', {
        get: function() { return false; },
        configurable: true
      });
    } catch (e) {}

    // 3. navigator.languages — empty array is a WebView signal.
    try {
      if (!navigator.languages || navigator.languages.length === 0) {
        Object.defineProperty(navigator, 'languages', {
          get: function() { return ['en-US', 'en']; },
          configurable: true
        });
      }
    } catch (e) {}

  } catch (globalErr) {
    // Never crash the host page.
  }
})();
""".trimIndent()

// ---------------------------------------------------------------------------
// Cookie data model
// ---------------------------------------------------------------------------

data class Cookie(
    val domain: String = "",
    val name: String = "",
    val value: String = "",
    val includeSubdomains: Boolean = false,
    val path: String = "/",
    val secure: Boolean = true,
    val expiry: Long = 0L,
    val isHttpOnly: Boolean = false,
) {
    constructor(
        url: String,
        name: String,
        value: String,
    ) : this(domain = url.toDomain(), name = name, value = value)

    fun toNetscapeCookieString(): String {
        // Netscape HTTP Cookie File format: exactly 7 tab-delimited fields, no filtering.
        // Using connectWithDelimiter() is WRONG here because it silently strips blank/empty
        // fields — legitimate cookies with empty values (e.g. opt-out flags, logout tokens)
        // would produce a 6-field line that yt-dlp rejects as malformed.
        return buildString {
            append(domain)
            append('\t')
            append(includeSubdomains.toNetscapeFlag())
            append('\t')
            append(path)
            append('\t')
            append(secure.toNetscapeFlag())
            append('\t')
            append(if (expiry > 0L) expiry.toString() else "0")
            append('\t')
            append(name)
            append('\t')
            append(value) // intentionally kept even when empty — empty value is valid
        }
    }

    fun isExpired(): Boolean = expiry > 0L && expiry < System.currentTimeMillis() / 1000L
}

private fun Boolean.toNetscapeFlag(): String = if (this) "TRUE" else "FALSE"

private fun String.toDomain(): String {
    val candidate = if (this.contains("://")) this else "https://$this"
    return runCatching { Uri.parse(candidate).host }.getOrNull()
        ?: this.substringAfter("://").substringBefore("/")
}

private val NAVIGABLE_SCHEMES = setOf("http", "https")

// ---------------------------------------------------------------------------
// WebView page composable
// ---------------------------------------------------------------------------

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewPage(cookiesViewModel: CookiesViewModel, onDismissRequest: () -> Unit) {

    val state by cookiesViewModel.stateFlow.collectAsStateWithLifecycle()

    val cookieManager = remember { CookieManager.getInstance() }
    val rawUrl = state.editingCookieProfile.url
    val websiteUrl = remember(rawUrl) {
        when {
            rawUrl.isEmpty() -> rawUrl
            rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
            else -> "https://$rawUrl"
        }
    }

    // Tracks whether addDocumentStartJavaScript was registered for this WebView.
    // If the feature is not supported we fall back to evaluateJavascript in onPageStarted.
    var documentStartScriptRegistered by remember { mutableStateOf(false) }

    var pageTitle by remember { mutableStateOf("") }
    var mainWebView by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    cookieManager.setAcceptCookie(true)

    if (websiteUrl.isEmpty()) {
        onDismissRequest()
        return
    }

    BackHandler {
        val webView = mainWebView
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else {
            // Mirror what the "Done" button does: flush first so any cookies written
            // by JavaScript after the last onPageFinished (common on Facebook/Instagram
            // SPAs) are persisted before we leave the browser.
            cookieManager.flush()
            onDismissRequest()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(pageTitle.ifEmpty { websiteUrl }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Flush before dismissing so any cookies written by JS after
                        // the last onPageFinished are persisted — same as the Done button.
                        cookieManager.flush()
                        onDismissRequest()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(id = androidx.appcompat.R.string.abc_action_mode_done),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            cookieManager.flush()
                            onDismissRequest()
                        }
                    ) {
                        Text(text = stringResource(id = androidx.appcompat.R.string.abc_action_mode_done))
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        mainWebView = this

                        // Transparent background so dark-themed sites (Instagram) do not
                        // show a white flash while the page is loading.
                        setBackgroundColor(AndroidColor.TRANSPARENT)

                        settings.run {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            // Must be true so Facebook/Instagram popup windows (window.open,
                            // target="_blank") are delivered to onCreateWindow instead of
                            // being silently dropped, which caused blank pages.
                            setSupportMultipleWindows(true)
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            allowFileAccess = false
                            allowContentAccess = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            blockNetworkImage = false
                            loadsImagesAutomatically = true
                            // Allow JS to open windows without user gesture (e.g. post-login
                            // programmatic redirects in Facebook/Instagram auth flows).
                            // This is safe because every new-window request is intercepted by
                            // onCreateWindow and redirected into the same visible WebView —
                            // no uncontrolled windows can actually appear.
                            javaScriptCanOpenWindowsAutomatically = true
                            mediaPlaybackRequiresUserGesture = false
                            // Strip the " wv" suffix that older Android WebViews appended to
                            // the UA string so sites see a standard Chrome UA.
                            val chromeLikeUA = userAgentString.replace(Regex("\\swv\\b"), "")
                            setUserAgentString(chromeLikeUA)
                            USER_AGENT_STRING.updateString(chromeLikeUA)
                        }
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView,
                                url: String?,
                                favicon: android.graphics.Bitmap?,
                            ) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                loadError = null

                                // Fallback injection for devices where WebView does not
                                // support addDocumentStartJavaScript (WebView < 102).
                                // evaluateJavascript from onPageStarted runs before the
                                // page's own script bundle is executed on most devices.
                                if (!documentStartScriptRegistered) {
                                    view.evaluateJavascript(ANTI_DETECTION_SCRIPT, null)
                                }
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                cookieManager.flush()
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val scheme = request.url?.scheme?.lowercase()
                                if (scheme in NAVIGABLE_SCHEMES) return false
                                return try {
                                    val intent =
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            request.url,
                                        )
                                    view.context.startActivity(intent)
                                    true
                                } catch (e: Exception) {
                                    Log.w(TAG, "No handler for scheme=$scheme", e)
                                    true
                                }
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request.isForMainFrame) {
                                    isLoading = false
                                    loadError = error.description?.toString()
                                    Log.w(TAG, "Load error on main frame: ${error.errorCode} ${error.description}")
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView,
                                handler: SslErrorHandler,
                                error: android.net.http.SslError,
                            ) {
                                Log.w(TAG, "SSL error: ${error.primaryError}")
                                loadError = "Connection is not secure"
                                handler.cancel()
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView, title: String?) {
                                pageTitle = title ?: ""
                            }

                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                loadProgress = newProgress
                            }

                            override fun onPermissionRequest(request: PermissionRequest) {
                                request.deny()
                            }

                            // Handle window.open() / target="_blank" links.
                            // Facebook, Instagram and other social sites open their login/OAuth
                            // flows in popup windows. Without this override those requests are
                            // silently dropped and the user sees a blank page. We redirect the
                            // new-window URL into the existing WebView so the login proceeds
                            // normally inside the same in-app browser.
                            override fun onCreateWindow(
                                view: WebView,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?,
                            ): Boolean {
                                // A temporary, off-screen WebView is required to receive the
                                // WebViewTransport object that carries the new window's first URL.
                                //
                                // IMPORTANT: shouldOverrideUrlLoading does NOT fire for the
                                // *first* navigation of a transport window — the WebView loads
                                // that initial URL itself directly. Only subsequent navigations
                                // trigger the override. We must also intercept onPageStarted so
                                // the very first URL (e.g. the Facebook/Instagram consent page)
                                // is redirected into the main visible WebView.
                                val helper = WebView(view.context)
                                helper.webViewClient = object : WebViewClient() {

                                    // Catches the FIRST URL loaded into the helper window
                                    // (the one carried by the transport message).
                                    override fun onPageStarted(
                                        view: WebView,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?,
                                    ) {
                                        if (!url.isNullOrEmpty() && url != "about:blank") {
                                            mainWebView?.loadUrl(url)
                                        }
                                        // The helper has served its purpose — stop loading and
                                        // destroy it to avoid a memory leak. We post the destroy
                                        // so it runs after this callback returns (calling
                                        // destroy() synchronously inside a WebViewClient
                                        // callback is undefined behaviour).
                                        view.post {
                                            view.stopLoading()
                                            view.destroy()
                                        }
                                    }

                                    // Catches any subsequent navigations inside the helper
                                    // before the post-destroy executes.
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean {
                                        val url = request.url?.toString()
                                        if (!url.isNullOrEmpty()) {
                                            mainWebView?.loadUrl(url)
                                        }
                                        view.post {
                                            view.stopLoading()
                                            view.destroy()
                                        }
                                        return true
                                    }
                                }
                                val transport =
                                    resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                transport.webView = helper
                                resultMsg.sendToTarget()
                                return true
                            }
                        }

                        // -------------------------------------------------------
                        // Register the anti-detection script to run at document
                        // start — BEFORE any page JavaScript executes.
                        //
                        // This MUST be called after webViewClient/webChromeClient
                        // are set and BEFORE loadUrl() so the first page load gets
                        // the script too.
                        //
                        // WebViewFeature.DOCUMENT_START_SCRIPT requires WebView 102+
                        // (available on all devices running Android 5+ with a modern
                        // WebView APK, which covers >99% of active devices in 2026).
                        // -------------------------------------------------------
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                            WebViewCompat.addDocumentStartJavaScript(
                                this, ANTI_DETECTION_SCRIPT, setOf("*")
                            )
                            documentStartScriptRegistered = true
                            Log.d(TAG, "Anti-detection script registered via addDocumentStartJavaScript")
                        } else {
                            // Older WebView: fall back to evaluateJavascript in onPageStarted
                            Log.d(TAG, "addDocumentStartJavaScript not supported; using evaluateJavascript fallback")
                        }

                        loadUrl(websiteUrl)
                    }
                },
                onRelease = {
                    mainWebView = null
                    it.destroy()
                },
            )

            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            loadError?.let { message ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = message,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
            }
        }
    }
}
