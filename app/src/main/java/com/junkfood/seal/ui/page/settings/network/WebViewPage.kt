package com.junkfood.seal.ui.page.settings.network

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Message
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.USER_AGENT_STRING
import com.junkfood.seal.util.connectWithDelimiter

private const val TAG = "WebViewPage"

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
        return connectWithDelimiter(
            domain,
            includeSubdomains.toNetscapeFlag(),
            path,
            secure.toNetscapeFlag(),
            if (expiry > 0L) expiry.toString() else "0",
            name,
            value,
            delimiter = "\u0009",
        )
    }

    fun isExpired(): Boolean = expiry > 0L && expiry < System.currentTimeMillis() / 1000L
}

private fun Boolean.toNetscapeFlag(): String = if (this) "TRUE" else "FALSE"

private val domainRegex = Regex("""https?://([\w-]+\.)?|/.*""")

private fun String.toDomain(): String {
    return this.replace(domainRegex, "").trimEnd('/')
}

private fun makeCookie(url: String, cookieString: String): Cookie {
    cookieString.split("=").run {
        return Cookie(url = url, name = first(), value = last())
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewPage(cookiesViewModel: CookiesViewModel, onDismissRequest: () -> Unit) {

    val state by cookiesViewModel.stateFlow.collectAsStateWithLifecycle()
    Log.d(TAG, state.editingCookieProfile.url)

    val cookieManager = remember { CookieManager.getInstance() }
    val websiteUrl = state.editingCookieProfile.url
    var pageTitle by remember { mutableStateOf("") }
    // Reference to the main WebView so the system back button can navigate the login flow
    // (multi-step logins) instead of immediately leaving the screen.
    var mainWebView by remember { mutableStateOf<WebView?>(null) }
    // Holds a popup/child WebView opened by the page via window.open() / target="_blank".
    // Many login flows (Google, OAuth) open the login form in a NEW window. Without
    // multi-window support that produced a black screen.
    var popupWebView by remember { mutableStateOf<WebView?>(null) }

    // Accept cookies globally before any page loads, otherwise the login session is never
    // persisted and the SQLite extraction finds nothing.
    cookieManager.setAcceptCookie(true)

    // Back navigation: close the popup first, then walk back through the login history,
    // and finally leave the screen.
    BackHandler {
        when {
            popupWebView != null -> popupWebView = null
            mainWebView?.canGoBack() == true -> mainWebView?.goBack()
            else -> onDismissRequest()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(pageTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { onDismissRequest() }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            stringResource(id = androidx.appcompat.R.string.abc_action_mode_done),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onDismissRequest) {
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
                    // White background avoids a black flash before the first paint.
                    setBackgroundColor(AndroidColor.WHITE)
                    settings.run {
                        javaScriptCanOpenWindowsAutomatically = true
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        // Required so the page can open login popups (window.open / target=_blank).
                        setSupportMultipleWindows(true)
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        USER_AGENT_STRING.updateString(userAgentString)
                    }
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            // Persist cookies as they arrive so the SQLite extraction
                            // (DownloadUtil.getCookieListFromDatabase) sees the latest session.
                            cookieManager.flush()
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            // Let the WebView load all http(s) navigations itself; only block
                            // non-http schemes (intent://, market://, mailto:, etc.).
                            return request.url?.scheme?.startsWith("http") != true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView, title: String?) {
                            pageTitle = title ?: ""
                        }

                        // Route a user-initiated popup window into a real child WebView shown as
                        // an overlay, so the user can complete the login. Cookies set during the
                        // popup flow land in the shared app cookie store and are captured.
                        override fun onCreateWindow(
                            view: WebView,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message,
                        ): Boolean {
                            // Ignore automatic popups (ads/trackers). Honouring them would cover
                            // the real login page with a blank overlay — the "black screen".
                            if (!isUserGesture) return false
                            val popup =
                                WebView(view.context).apply {
                                    setBackgroundColor(AndroidColor.WHITE)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.javaScriptCanOpenWindowsAutomatically = true
                                    settings.setSupportMultipleWindows(true)
                                    settings.userAgentString = view.settings.userAgentString
                                    cookieManager.setAcceptThirdPartyCookies(this, true)
                                    webViewClient =
                                        object : WebViewClient() {
                                            override fun onPageFinished(v: WebView, url: String?) {
                                                super.onPageFinished(v, url)
                                                cookieManager.flush()
                                            }
                                        }
                                    webChromeClient =
                                        object : WebChromeClient() {
                                            override fun onReceivedTitle(v: WebView, title: String?) {
                                                if (!title.isNullOrEmpty()) pageTitle = title
                                            }

                                            override fun onCloseWindow(window: WebView) {
                                                popupWebView = null
                                            }
                                        }
                                }
                            popupWebView = popup
                            (resultMsg.obj as WebView.WebViewTransport).webView = popup
                            resultMsg.sendToTarget()
                            return true
                        }

                        override fun onCloseWindow(window: WebView) {
                            popupWebView = null
                        }
                    }
                    loadUrl(websiteUrl)
                }
            },
            // NOTE: deliberately NO reload here. Reloading on recomposition (which happens on
            // every title change / popup toggle) restarted the page on each redirect and caused
            // the blank/black login screen. The factory loads the URL exactly once.
            onRelease = {
                mainWebView = null
                it.destroy()
            },
        )

        // Login popup overlay (e.g. Google "Log in" window).
        popupWebView?.let { popup ->
            key(popup) {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { popup },
                    onRelease = { it.destroy() },
                )
                IconButton(
                    onClick = { popupWebView = null },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription =
                            stringResource(id = androidx.appcompat.R.string.abc_action_mode_done),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            }
        }
      }
    }
}
