package com.junkfood.seal.ui.page.settings.network

import android.annotation.SuppressLint
import android.os.Message
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
    val includeSubdomains: Boolean = true,
    val path: String = "/",
    val secure: Boolean = true,
    val expiry: Long = 0L,
) {
    constructor(
        url: String,
        name: String,
        value: String,
    ) : this(domain = url.toDomain(), name = name, value = value)

    fun toNetscapeCookieString(): String {
        return connectWithDelimiter(
            domain,
            includeSubdomains.toString().uppercase(),
            path,
            secure.toString().uppercase(),
            expiry.toString(),
            name,
            value,
            delimiter = "\u0009",
        )
    }
}

private val domainRegex = Regex("""http(s)?://(\w*(www|m|account|sso))?|/.*""")

private fun String.toDomain(): String {
    return this.replace(domainRegex, "")
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

    val cookieManager = CookieManager.getInstance()
    val cookieSet = remember { mutableSetOf<Cookie>() }
    val websiteUrl = state.editingCookieProfile.url
    var pageTitle by remember { mutableStateOf("") }
    // Holds a popup/child WebView opened by the page via window.open() / target="_blank".
    // Many login flows (Instagram, Google, OAuth) open the login form in a NEW window.
    // Without multi-window support this produced a black screen.
    var popupWebView by remember { mutableStateOf<WebView?>(null) }

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
                    settings.run {
                        javaScriptCanOpenWindowsAutomatically = true
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        // Required so the page can open login popups (window.open / target=_blank).
                        // Combined with WebChromeClient.onCreateWindow below, this fixes the
                        // black screen seen when tapping "Log in" on Instagram and similar sites.
                        setSupportMultipleWindows(true)
                        USER_AGENT_STRING.updateString(userAgentString)
                    }
                    cookieManager.setAcceptCookie(true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            if (url.isNullOrEmpty()) return
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            return if (request.url?.scheme?.contains("http") == true)
                                super.shouldOverrideUrlLoading(view, request)
                            else true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView, title: String) {
                            pageTitle = title
                        }

                        // Route the page's popup window into a real child WebView shown as an
                        // overlay, so the user can complete the login. Cookies set during the
                        // popup flow land in the shared app cookie store and are captured.
                        override fun onCreateWindow(
                            view: WebView,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message,
                        ): Boolean {
                            val popup =
                                WebView(view.context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.javaScriptCanOpenWindowsAutomatically = true
                                    settings.setSupportMultipleWindows(true)
                                    settings.userAgentString = view.settings.userAgentString
                                    cookieManager.setAcceptThirdPartyCookies(this, true)
                                    webViewClient = WebViewClient()
                                    webChromeClient =
                                        object : WebChromeClient() {
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
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    loadUrl(websiteUrl)
                }
            },
            update = { view ->
                if (view.url != websiteUrl) {
                    view.loadUrl(websiteUrl)
                }
            },
        )

        // Login popup overlay (e.g. Instagram/Google "Log in" window).
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
