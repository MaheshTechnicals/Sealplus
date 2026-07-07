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
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.USER_AGENT_STRING

private const val TAG = "WebViewPage"

// =============================================================================
// WHY FACEBOOK / INSTAGRAM LOGIN PAGES SHOWED A BLANK PAGE
// =============================================================================
//
// Problem had TWO independent layers — both must be fixed:
//
// LAYER 1 — HTTP request header (server-side check, happens BEFORE any HTML is sent):
//   Android WebView sends:  Sec-CH-UA: "Android WebView";v="136"
//   Real Chrome sends:      Sec-CH-UA: "Google Chrome";v="136"
//   Meta's servers see "Android WebView" and return an empty page — no HTML, no JS,
//   nothing — before our browser even receives anything to render.
//   Fix: WebSettingsCompat.setUserAgentMetadata() (official AndroidX WebKit API) —
//        replaces "Android WebView" with "Google Chrome" in ALL outgoing headers.
//
// LAYER 2 — JavaScript client-side fingerprint (checked by Meta's React login app):
//   Android WebView does not expose window.chrome (real Chrome does).
//   Meta's React login code checks window.chrome.runtime.connect before
//   mounting the login form. If absent → React renders an empty root → blank page.
//   Also: navigator.userAgentData.brands reports "Android WebView" on the JS side.
//   Fix: addDocumentStartJavaScript() injects our shim BEFORE any page JS runs.
//        Fallback: evaluateJavascript() in onPageStarted for older WebViews.
//
// LAYER 1 is the primary cause (server returns nothing).
// LAYER 2 must also be fixed or the login form still won't render even after
// the server serves content.
// =============================================================================

// ---------------------------------------------------------------------------
// Anti-detection JavaScript shim — injected before every page's own scripts.
// Fixes LAYER 2 (client-side fingerprinting).
// ---------------------------------------------------------------------------
private val ANTI_DETECTION_SCRIPT = """
(function() {
  'use strict';
  try {

    // ------------------------------------------------------------------
    // 1. window.chrome — Meta checks for this object before showing the
    //    login form. Android WebView omits it; real Chrome always has it.
    // ------------------------------------------------------------------
    if (typeof window.chrome === 'undefined' || window.chrome === null) {
      var chromeDef = {
        app: {
          isInstalled: false,
          InstallState: { DISABLED:'disabled', INSTALLED:'installed', NOT_INSTALLED:'not_installed' },
          RunningState: { CANNOT_RUN:'cannot_run', READY_TO_RUN:'ready_to_run', RUNNING:'running' }
        },
        runtime: {
          connect: function() {
            return {
              postMessage: function() {},
              disconnect: function() {},
              onMessage: { addListener: function() {}, removeListener: function() {} },
              onDisconnect: { addListener: function() {}, removeListener: function() {} }
            };
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
            requestTime:0, startLoadTime:0, commitLoadTime:0,
            finishDocumentLoadTime:0, finishLoadTime:0,
            firstPaintTime:0, firstPaintAfterLoadTime:0,
            navigationType:'Other', wasFetchedViaSpdy:false,
            wasNpnNegotiated:false, npnNegotiatedProtocol:'',
            wasAlternateProtocolAvailable:false, connectionInfo:'unknown'
          };
        },
        csi: function() {
          return { startE: Date.now(), onloadT: Date.now(), pageT: 0, tran: 15 };
        }
      };
      try {
        Object.defineProperty(window, 'chrome', {
          value: chromeDef, writable: true, enumerable: true, configurable: true
        });
      } catch(e) { window.chrome = chromeDef; }
    }

    // ------------------------------------------------------------------
    // 2. navigator.webdriver — must be false/undefined, not true.
    //    Automation / headless browsers set this to true.
    // ------------------------------------------------------------------
    try {
      Object.defineProperty(navigator, 'webdriver', {
        get: function() { return false; }, configurable: true
      });
    } catch(e) {}

    // ------------------------------------------------------------------
    // 3. navigator.userAgentData.brands — JS mirror of the Sec-CH-UA header.
    //    WebView reports "Android WebView"; we patch it to "Google Chrome".
    //    This is a JS-side fallback; the primary fix is setUserAgentMetadata()
    //    which changes the actual HTTP header natively.
    // ------------------------------------------------------------------
    try {
      if (navigator.userAgentData && navigator.userAgentData.brands) {
        var brands = navigator.userAgentData.brands;
        var hasChrome = false;
        var webViewFound = false;
        var chromiumVer = '136';
        for (var i = 0; i < brands.length; i++) {
          if (brands[i].brand === 'Google Chrome')  { hasChrome = true; }
          if (brands[i].brand === 'Chromium')        { chromiumVer = brands[i].version; }
          if (brands[i].brand === 'Android WebView') { webViewFound = true; chromiumVer = brands[i].version; }
        }
        if (!hasChrome || webViewFound) {
          var newBrands = [
            { brand: 'Not/A)Brand',    version: '8'           },
            { brand: 'Chromium',       version: chromiumVer   },
            { brand: 'Google Chrome',  version: chromiumVer   }
          ];
          var patchedUAData = {
            brands: newBrands,
            mobile: true,
            platform: 'Android',
            getHighEntropyValues: function(hints) {
              return Promise.resolve({
                brands: newBrands,
                mobile: true,
                platform: 'Android',
                architecture: 'arm',
                bitness: '64',
                model: '',
                platformVersion: '14.0.0',
                uaFullVersion: chromiumVer + '.0.0.0',
                fullVersionList: newBrands.map(function(b) {
                  return { brand: b.brand, version: chromiumVer + '.0.0.0' };
                }),
                wow64: false
              });
            },
            toJSON: function() {
              return { brands: newBrands, mobile: true, platform: 'Android' };
            }
          };
          try {
            Object.defineProperty(navigator, 'userAgentData', {
              value: patchedUAData, writable: true, configurable: true
            });
          } catch(e2) { /* native setUserAgentMetadata already handled it */ }
        }
      }
    } catch(e) {}

    // ------------------------------------------------------------------
    // 4. navigator.languages — empty array signals a non-browser environment.
    // ------------------------------------------------------------------
    try {
      if (!navigator.languages || navigator.languages.length === 0) {
        Object.defineProperty(navigator, 'languages', {
          get: function() { return ['en-US', 'en']; }, configurable: true
        });
      }
    } catch(e) {}

  } catch(globalErr) { /* never crash the host page */ }
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
        // Netscape HTTP Cookie File format: exactly 7 tab-delimited fields.
        return buildString {
            append(domain); append('\t')
            append(includeSubdomains.toNetscapeFlag()); append('\t')
            append(path); append('\t')
            append(secure.toNetscapeFlag()); append('\t')
            append(if (expiry > 0L) expiry.toString() else "0"); append('\t')
            append(name); append('\t')
            append(value) // preserved even when empty — empty value is valid
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

    // True once addDocumentStartJavaScript is successfully registered.
    // When false, the onPageStarted fallback injects via evaluateJavascript instead.
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
                    TextButton(onClick = {
                        cookieManager.flush()
                        onDismissRequest()
                    }) {
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
                        setBackgroundColor(AndroidColor.TRANSPARENT)

                        settings.run {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
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
                            javaScriptCanOpenWindowsAutomatically = true
                            mediaPlaybackRequiresUserGesture = false

                            // Strip " wv" suffix so User-Agent string looks like real Chrome.
                            val chromeLikeUA = userAgentString.replace(Regex("\\swv\\b"), "")
                            setUserAgentString(chromeLikeUA)
                            USER_AGENT_STRING.updateString(chromeLikeUA)

                            // -----------------------------------------------------------------
                            // CRITICAL FIX — Layer 1: Sec-CH-UA header override
                            //
                            // Android WebView sends `Sec-CH-UA: "Android WebView";v="136"`.
                            // Meta's (Facebook/Instagram) servers check this HTTP header
                            // BEFORE sending HTML. Seeing "Android WebView" they return a
                            // blank page response — before any JavaScript can run.
                            //
                            // WebSettingsCompat.setUserAgentMetadata() is the ONLY way to
                            // change the Sec-CH-UA header. JavaScript injection cannot change
                            // outgoing HTTP request headers.
                            //
                            // Feature requires WebView 112+ / androidx.webkit 1.9.0+.
                            // On older WebViews the JS fallback in ANTI_DETECTION_SCRIPT
                            // overrides navigator.userAgentData as a partial mitigation.
                            // -----------------------------------------------------------------
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
                                runCatching {
                                    val current = WebSettingsCompat.getUserAgentMetadata(this)
                                    // Only patch if "Google Chrome" brand is missing —
                                    // indicating this is a WebView environment.
                                    if (current.brandVersionList.isNotEmpty() &&
                                        current.brandVersionList.none { it.brand == "Google Chrome" }
                                    ) {
                                        val newBrands = current.brandVersionList.map { bv ->
                                            if (bv.brand == "Android WebView") {
                                                // Replace "Android WebView" with "Google Chrome"
                                                // keeping the same version numbers.
                                                UserAgentMetadata.BrandVersion.Builder()
                                                    .setBrand("Google Chrome")
                                                    .setMajorVersion(bv.majorVersion)
                                                    .setFullVersion(
                                                        bv.fullVersion.ifEmpty { bv.majorVersion }
                                                    )
                                                    .build()
                                            } else bv
                                        }
                                        // Rebuild metadata, preserving all existing fields
                                        // except the brands list.
                                        val metaBuilder = UserAgentMetadata.Builder()
                                            .setBrandVersionList(newBrands)
                                            .setMobile(true)
                                        current.platform?.let       { metaBuilder.setPlatform(it) }
                                        current.platformVersion?.let { metaBuilder.setPlatformVersion(it) }
                                        current.architecture?.let   { metaBuilder.setArchitecture(it) }
                                        current.model?.let          { metaBuilder.setModel(it) }
                                        metaBuilder.setBitness(current.bitness)
                                        metaBuilder.setWow64(current.isWow64)
                                        WebSettingsCompat.setUserAgentMetadata(
                                            this, metaBuilder.build()
                                        )
                                        Log.d(TAG, "Sec-CH-UA brands patched: Android WebView → Google Chrome")
                                    }
                                }.onFailure { e ->
                                    Log.w(TAG, "UA brand metadata override failed: ${e.message}")
                                }
                            } else {
                                Log.d(TAG, "USER_AGENT_METADATA not supported; JS fallback will handle userAgentData")
                            }
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
                                // Fallback injection when addDocumentStartJavaScript is
                                // unavailable (WebView < 102). evaluateJavascript from
                                // onPageStarted executes before most page scripts on
                                // Android's single-threaded JS engine.
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
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW, request.url
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
                                    Log.w(TAG, "Load error: ${error.errorCode} ${error.description}")
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

                            // Handle window.open() / target="_blank" popups.
                            // Facebook/Instagram open their login flows in popup windows.
                            // Without this, those windows are silently dropped → blank page.
                            override fun onCreateWindow(
                                view: WebView,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?,
                            ): Boolean {
                                val helper = WebView(view.context)
                                helper.webViewClient = object : WebViewClient() {
                                    // Catches the FIRST URL (transport window).
                                    // shouldOverrideUrlLoading does NOT fire for the initial
                                    // transport URL — only subsequent navigations trigger it.
                                    override fun onPageStarted(
                                        view: WebView,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?,
                                    ) {
                                        if (!url.isNullOrEmpty() && url != "about:blank") {
                                            mainWebView?.loadUrl(url)
                                        }
                                        // Destroy helper after capturing URL — avoid leak.
                                        // post() defers until after this callback returns
                                        // (calling destroy() synchronously inside a WebViewClient
                                        // callback is undefined behaviour).
                                        view.post {
                                            view.stopLoading()
                                            view.destroy()
                                        }
                                    }

                                    // Catches subsequent navigations in the helper.
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean {
                                        request.url?.toString()
                                            ?.let { mainWebView?.loadUrl(it) }
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

                        // ------------------------------------------------------------
                        // Layer 2 fix: inject ANTI_DETECTION_SCRIPT before page JS.
                        // Must be called AFTER webViewClient/webChromeClient are set
                        // and BEFORE loadUrl() so the very first page load gets it.
                        // Requires WebView 102+ (androidx.webkit:DOCUMENT_START_SCRIPT).
                        // ------------------------------------------------------------
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                            WebViewCompat.addDocumentStartJavaScript(
                                this, ANTI_DETECTION_SCRIPT, setOf("*")
                            )
                            documentStartScriptRegistered = true
                            Log.d(TAG, "Anti-detection script registered (document-start)")
                        } else {
                            Log.d(TAG, "DOCUMENT_START_SCRIPT unavailable; using evaluateJavascript fallback")
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
