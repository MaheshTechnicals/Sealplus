package com.junkfood.seal.ui.page.settings.network

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.USER_AGENT_STRING

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
        return com.junkfood.seal.util.connectWithDelimiter(
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

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewPage(cookiesViewModel: CookiesViewModel, onDismissRequest: () -> Unit) {

    val state by cookiesViewModel.stateFlow.collectAsStateWithLifecycle()
    Log.d(TAG, state.editingCookieProfile.url)

    val cookieManager = remember { CookieManager.getInstance() }
    val rawUrl = state.editingCookieProfile.url
    val websiteUrl = remember(rawUrl) {
        when {
            rawUrl.isEmpty() -> rawUrl
            rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
            else -> "https://$rawUrl"
        }
    }
    var pageTitle by remember { mutableStateOf("") }
    var mainWebView by remember { mutableStateOf<WebView?>(null) }

    cookieManager.setAcceptCookie(true)

    if (websiteUrl.isEmpty()) {
        onDismissRequest()
        return
    }

    BackHandler {
        if (mainWebView?.canGoBack() == true) {
            mainWebView?.goBack()
        } else {
            onDismissRequest()
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
        AndroidView(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    mainWebView = this
                    setBackgroundColor(AndroidColor.WHITE)
                    settings.run {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setSupportMultipleWindows(false)
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        allowFileAccess = false
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                    USER_AGENT_STRING.updateString(userAgentString)
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            cookieManager.flush()
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: android.webkit.WebResourceRequest,
                        ): Boolean {
                            return request.url?.scheme?.startsWith("http") != true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView, title: String?) {
                            pageTitle = title ?: ""
                        }
                    }
                    loadUrl(websiteUrl)
                }
            },
            onRelease = {
                mainWebView = null
                it.destroy()
            },
        )
    }
}
