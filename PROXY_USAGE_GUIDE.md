## Proxy Settings - Quick Start Guide

### For Users

1. **Navigate to Proxy Settings**:
   - Open Sealplus
   - Go to Settings (⚙️)
   - Select "Sealplus Extras"
   - Tap "Proxy Settings"

2. **Using Free Proxy**:
   - Enable the "Enable Proxy" toggle
   - Ensure "Free Proxy" is selected
   - Choose a country from the dropdown
   - Tap "Fetch Proxies"
   - Select a proxy from the list
   - Tap "Test Connection" to verify
   - Optionally tap "Run Speed Test" for performance metrics

3. **Using Custom Proxy**:
   - Enable the "Enable Proxy" toggle
   - Select "Custom Proxy"
   - Enter your proxy host (e.g., `proxy.example.com` or `192.168.1.1`)
   - Enter the port (e.g., `8080`)
   - Select the proxy type (HTTP, HTTPS, SOCKS4, or SOCKS5)
   - Tap "Save Configuration"
   - Tap "Test Connection" to verify

4. **Disabling Proxy**:
   - Simply toggle off "Enable Proxy"
   - All requests will go directly without proxy

### For Developers

#### Adding Proxy Support to New Network Clients

```kotlin
import com.junkfood.seal.util.ProxyManager
import okhttp3.OkHttpClient

// Method 1: Create client with proxy support
private fun getClient(): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
    
    // Apply proxy if active
    ProxyManager.getActiveProxy()?.let { proxy ->
        builder.proxy(proxy)
    }
    
    return builder.build()
}

// Method 2: Check if proxy is active
if (ProxyManager.isProxyActive()) {
    val proxyAddress = ProxyManager.loadProxyConfig().getProxyAddress()
    // Use proxy address string
}
```

#### Accessing Proxy Configuration

```kotlin
import com.junkfood.seal.util.ProxyManager

// Load current configuration
val config = ProxyManager.loadProxyConfig()

// Check properties
val isEnabled = config.enabled
val useFreeProxy = config.useFreeProxy
val proxyAddress = config.getProxyAddress()
val isValid = config.isValid()
val isWorking = config.isWorking

// Get Java Proxy object for OkHttp
val proxy: Proxy? = config.toJavaProxy()
```

#### Validating a Proxy

```kotlin
import com.junkfood.seal.util.ProxyValidator
import kotlinx.coroutines.launch

// Connection test
scope.launch {
    val result = ProxyValidator.validateProxyConnection(proxy)
    when (result) {
        is ProxyValidator.ValidationResult.Success -> {
            println("Connected! Latency: ${result.latencyMs}ms")
        }
        is ProxyValidator.ValidationResult.Failed -> {
            println("Failed: ${result.error}")
        }
        else -> {}
    }
}

// Speed test
scope.launch {
    val result = ProxyValidator.performSpeedTest(proxy)
    if (result.success) {
        println("Latency: ${result.latencyMs}ms")
        println("Speed: ${result.getSpeedMbps()} Mbps")
        println("Quality: ${result.getLatencyDescription()}")
    }
}

// Quick ping
scope.launch {
    val isAlive = ProxyValidator.quickPing(proxy)
}
```

#### Fetching Free Proxies

```kotlin
import com.junkfood.seal.util.ProxyManager
import kotlinx.coroutines.launch

scope.launch {
    val result = ProxyManager.fetchFreeProxies(
        ProxyManager.ProxyCountry.USA
    )
    
    result.onSuccess { proxies ->
        proxies.forEach { proxy ->
            println("Available proxy: $proxy")
        }
    }.onFailure { error ->
        println("Error: ${error.message}")
    }
}
```

#### Saving Custom Configuration

```kotlin
import com.junkfood.seal.util.ProxyManager

val config = ProxyManager.ProxyConfig(
    enabled = true,
    useFreeProxy = false,
    customProxyHost = "proxy.example.com",
    customProxyPort = 8080,
    customProxyType = ProxyManager.ProxyType.HTTP.name,
    lastValidated = System.currentTimeMillis(),
    isWorking = true
)

ProxyManager.saveProxyConfig(config)
```

#### Adding Proxy to yt-dlp Commands

```kotlin
// In DownloadUtil or similar
val preferences = DownloadPreferences.createFromPreferences()
val request = YoutubeDLRequest(url)

if (preferences.proxy) {
    request.addOption("--proxy", preferences.proxyUrl)
}
```

### API Reference

#### ProxyManager

| Method | Description | Returns |
|--------|-------------|---------|
| `fetchFreeProxies(country)` | Fetch list of free proxies for country | `Result<List<String>>` |
| `saveProxyConfig(config)` | Save configuration to preferences | `Unit` |
| `loadProxyConfig()` | Load configuration from preferences | `ProxyConfig` |
| `getActiveProxy()` | Get current proxy for OkHttp | `Proxy?` |
| `isProxyActive()` | Check if proxy is enabled and working | `Boolean` |
| `clearProxyConfig()` | Reset all proxy settings | `Unit` |

#### ProxyValidator

| Method | Description | Returns |
|--------|-------------|---------|
| `validateProxyConnection(proxy)` | Test proxy connectivity | `ValidationResult` |
| `performSpeedTest(proxy)` | Run comprehensive speed test | `SpeedTestResult` |
| `quickPing(proxy)` | Fast connectivity check | `Boolean` |
| `isNetworkAvailable(context)` | Check network availability | `Boolean` |

#### Enums

**ProxyType**:
- `HTTP` - Standard HTTP proxy
- `HTTPS` - HTTPS proxy (HTTP with TLS)
- `SOCKS4` - SOCKS version 4
- `SOCKS5` - SOCKS version 5

**ProxyCountry**:
- `USA` (US) - United States
- `UK` (GB) - United Kingdom
- `SINGAPORE` (SG) - Singapore
- `GERMANY` (DE) - Germany
- `SWITZERLAND` (CH) - Switzerland

### Preference Keys

Access via `PreferenceUtil`:

```kotlin
PROXY_ENABLED.getBoolean()
PROXY_USE_FREE.getBoolean()
PROXY_FREE_COUNTRY.getString()
PROXY_FREE_ADDRESS.getString()
PROXY_CUSTOM_HOST.getString()
PROXY_CUSTOM_PORT.getInt()
PROXY_CUSTOM_TYPE.getString()
PROXY_LAST_VALIDATED.getLong()
PROXY_IS_WORKING.getBoolean()
```

### Testing Your Integration

```kotlin
// Test if your OkHttpClient respects proxy
suspend fun testProxyIntegration() {
    // Enable test proxy
    val testConfig = ProxyManager.ProxyConfig(
        enabled = true,
        useFreeProxy = false,
        customProxyHost = "httpbin.org",
        customProxyPort = 80,
        customProxyType = ProxyManager.ProxyType.HTTP.name,
        isWorking = true
    )
    ProxyManager.saveProxyConfig(testConfig)
    
    // Make request with your client
    val client = getClient() // Your implementation
    val request = Request.Builder()
        .url("https://api.ipify.org?format=json")
        .build()
    
    client.newCall(request).execute().use { response ->
        println("Response: ${response.body?.string()}")
        // Should show proxy IP if working correctly
    }
}
```

### Troubleshooting

**Q: My OkHttpClient isn't using the proxy**  
A: Make sure you're calling `ProxyManager.getActiveProxy()` and applying it with `.proxy()` on the builder.

**Q: Speed test always fails**  
A: Check that the proxy supports HTTPS. Also verify network connectivity.

**Q: Free proxies don't load**  
A: ProxyScrape API might be down or rate-limited. Try again later or use custom proxy.

**Q: Downloads fail with proxy enabled**  
A: The proxy might not support the target site. Try testing with different proxy or check yt-dlp compatibility.

### Performance Tips

1. **Cache OkHttpClient instances** - Don't create new clients for every request
2. **Use connection pooling** - Configure OkHttpClient with connection pool
3. **Set reasonable timeouts** - Don't wait forever for slow proxies
4. **Validate before use** - Always test proxy before making important requests
5. **Handle failures gracefully** - Fall back to direct connection if proxy fails

### Security Best Practices

1. **Validate user input** - Check host/port before saving
2. **Use HTTPS when possible** - Encrypt traffic even through proxy
3. **Don't log proxy credentials** - If adding auth support
4. **Test proxy trustworthiness** - Free proxies may be malicious
5. **Inform users** - Make it clear when proxy is active

### Examples

**Example 1: Temporary Proxy for Single Request**

```kotlin
suspend fun fetchWithProxy(url: String): String {
    val tempProxy = Proxy(
        Proxy.Type.HTTP,
        InetSocketAddress("proxy.example.com", 8080)
    )
    
    val client = OkHttpClient.Builder()
        .proxy(tempProxy)
        .build()
    
    val request = Request.Builder().url(url).build()
    return client.newCall(request).execute().body?.string() ?: ""
}
```

**Example 2: Conditional Proxy Based on URL**

```kotlin
private fun getClient(url: String): OkHttpClient {
    val builder = OkHttpClient.Builder()
    
    // Only use proxy for specific domains
    if (url.contains("restrictedsite.com")) {
        ProxyManager.getActiveProxy()?.let { builder.proxy(it) }
    }
    
    return builder.build()
}
```

**Example 3: Retry Without Proxy on Failure**

```kotlin
suspend fun downloadWithFallback(url: String): ByteArray? {
    // Try with proxy first
    if (ProxyManager.isProxyActive()) {
        try {
            return downloadWithProxy(url)
        } catch (e: Exception) {
            Log.w(TAG, "Proxy failed, trying direct", e)
        }
    }
    
    // Fallback to direct connection
    return downloadDirect(url)
}
```

### Contributing

When adding new network functionality:

1. Check if proxy should be supported
2. Use `ProxyManager.getActiveProxy()` pattern
3. Test with both free and custom proxies
4. Handle proxy failures gracefully
5. Update this documentation

### Resources

- [OkHttp Proxy Documentation](https://square.github.io/okhttp/)
- [Java Proxy Documentation](https://docs.oracle.com/javase/8/docs/api/java/net/Proxy.html)
- [ProxyScrape API](https://proxyscrape.com/free-proxy-list)
- [yt-dlp Proxy Support](https://github.com/yt-dlp/yt-dlp#network-options)
