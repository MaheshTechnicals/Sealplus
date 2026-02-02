# Proxy Settings Feature - Implementation Guide

## Overview

A comprehensive proxy settings feature has been successfully implemented for the Sealplus Android app. This feature enables users to route all download traffic through proxy servers, with support for both free proxies and custom proxy configurations.

## Features Implemented

### 1. **Proxy Manager (`ProxyManager.kt`)**
- Centralized proxy configuration management
- Support for multiple proxy types: HTTP, HTTPS, SOCKS4, SOCKS5
- Integration with ProxyScrape API for free proxies
- Configuration persistence using MMKV
- Automatic proxy application to network requests

**Supported Countries for Free Proxies:**
- United States (US)
- United Kingdom (GB)
- Singapore (SG)
- Germany (DE)
- Switzerland (CH)

### 2. **Proxy Validator (`ProxyValidator.kt`)**
- **Connection Testing**: Validates proxy connectivity with HEAD requests to google.com/bing.com
- **Speed Testing**: Comprehensive performance metrics including:
  - Latency measurement (with quality rating: Excellent/Good/Fair/Poor)
  - Download speed calculation in Mbps
  - Upload latency testing
- **Network Availability Checking**: Uses NetworkCapabilities
- **Quick Ping**: Lightweight validation for fast checks

### 3. **Proxy Settings UI (`ProxySettingsPage.kt`)**
Material Design 3 Compose interface with:

**Main Sections:**
- **Global Proxy Toggle**: Master switch to enable/disable proxy
- **Connection Status Card**: Real-time validation results with color-coded indicators
- **Speed Test Results Card**: Detailed performance metrics
- **Proxy Type Selector**: Toggle between Free and Custom proxies

**Free Proxy Section:**
- Country dropdown selector
- Proxy list fetching with loading indicator
- Proxy selection dialog
- Selected proxy display

**Custom Proxy Section:**
- Host/IP input field
- Port number input
- Proxy protocol selector (HTTP/HTTPS/SOCKS4/SOCKS5)
- Configuration save button

**Action Buttons:**
- Test Connection: Validates proxy with real-time status
- Run Speed Test: Comprehensive performance analysis

### 4. **Navigation Integration**
- Added route: `Route.PROXY_SETTINGS`
- Accessible from: Settings → Sealplus Extras → Proxy Settings
- Full navigation flow implemented in `AppEntry.kt`

### 5. **Global OkHttpClient Integration**
All network clients now support proxy configuration:
- **DownloadUtil**: yt-dlp proxy integration via `--proxy` flag
- **UpdateUtil**: App update downloads through proxy
- **SponsorUtil**: GitHub API requests through proxy
- **FormatValidator**: Format URL validation through proxy
- **ProxyManager**: Free proxy fetching through proxy (with fallback)
- **ProxyValidator**: Connection and speed tests

### 6. **Preference Management**
New preference keys added to `PreferenceUtil.kt`:
```kotlin
PROXY_ENABLED           // Boolean - Global proxy toggle
PROXY_USE_FREE          // Boolean - Use free proxy (true) or custom (false)
PROXY_FREE_COUNTRY      // String - Selected country code
PROXY_FREE_ADDRESS      // String - Selected free proxy address
PROXY_CUSTOM_HOST       // String - Custom proxy host/IP
PROXY_CUSTOM_PORT       // Int - Custom proxy port
PROXY_CUSTOM_TYPE       // String - Proxy protocol type
PROXY_LAST_VALIDATED    // Long - Last validation timestamp
PROXY_IS_WORKING        // Boolean - Last validation result
```

## How It Works

### Proxy Configuration Flow

1. **User Configuration**:
   - User navigates to Settings → Sealplus Extras → Proxy Settings
   - Enables proxy toggle
   - Chooses Free or Custom proxy
   - For Free: Selects country and fetches proxy list
   - For Custom: Enters host, port, and protocol
   - Tests connection to validate proxy
   - Optionally runs speed test

2. **Proxy Validation**:
   ```kotlin
   // Connection test
   val result = ProxyValidator.validateProxyConnection(proxy)
   // Returns: Success(latencyMs) or Failed(error)
   
   // Speed test
   val speedResult = ProxyValidator.performSpeedTest(proxy)
   // Returns: SpeedTestResult with latency, download speed, upload latency
   ```

3. **Configuration Persistence**:
   ```kotlin
   ProxyManager.saveProxyConfig(config)
   // Saves all settings to MMKV
   ```

4. **Automatic Application**:
   - All OkHttpClient instances check `ProxyManager.getActiveProxy()`
   - DownloadUtil passes proxy to yt-dlp via `--proxy` option
   - Proxy is only applied if enabled and validated as working

### API Integration

**ProxyScrape API**:
```
https://api.proxyscrape.com/v2/?request=getproxies&protocol=http&country={CODE}&ssl=all&anonymity=anonymous&timeout=5000
```

Returns newline-separated list of proxies in format: `host:port`

### Connection Testing

**Primary Test URL**: `https://www.google.com` (HEAD request)  
**Fallback URL**: `https://www.bing.com` (HEAD request)  
**Timeout**: 10 seconds

**Speed Test URL**: `https://www.google.com/robots.txt` (GET request)

### Validation States

1. **Not Tested**: Initial state, no validation performed
2. **Testing**: Active validation in progress (shows spinner)
3. **Success**: Proxy working (shows green checkmark + latency)
4. **Failed**: Connection failed (shows red error + reason)

## User Experience

### Visual Indicators

- **Enabled State**: Primary container color for toggle card
- **Success**: Green/Secondary color with checkmark icon
- **Error**: Red/Error color with error icon
- **Testing**: Loading spinner with progress message

### Animations

- Smooth expand/collapse animations for sections
- Fade in/out transitions for status cards
- Loading indicators for async operations

### Error Handling

- Network unavailable detection
- Timeout handling (10s for connection, 20s for speed test)
- Graceful fallback for failed API requests
- User-friendly error messages via Toast

## Integration Points

### DownloadUtil
```kotlin
proxy = PROXY.getBoolean() || ProxyManager.isProxyActive(),
proxyUrl = if (ProxyManager.isProxyActive()) {
    ProxyManager.loadProxyConfig().getProxyAddress()
} else {
    PROXY_URL.getString()
}
```

### OkHttpClient Pattern
```kotlin
private fun getClient(): OkHttpClient {
    val builder = OkHttpClient.Builder()
    ProxyManager.getActiveProxy()?.let { proxy ->
        builder.proxy(proxy)
    }
    return builder.build()
}
```

## Testing Checklist

- [ ] Enable proxy toggle works
- [ ] Free proxy list fetches successfully for each country
- [ ] Proxy selection from list updates configuration
- [ ] Custom proxy saves correctly
- [ ] Connection test validates working proxies
- [ ] Connection test fails for invalid proxies
- [ ] Speed test measures latency and speed accurately
- [ ] Downloads route through proxy when enabled
- [ ] App updates use proxy
- [ ] Proxy persists across app restarts
- [ ] Proxy disables cleanly when toggle is off

## Security Considerations

1. **No Credential Storage**: Free proxies don't require authentication
2. **Custom Proxy Privacy**: No password/auth fields (can be added if needed)
3. **HTTPS Support**: SSL/TLS works through HTTP CONNECT tunneling
4. **Validation Required**: Users must test proxy before it's considered "working"

## Performance Optimizations

1. **Lazy Client Creation**: OkHttpClient created on-demand with proxy
2. **Cached Results**: Connection status cached until configuration changes
3. **Background Operations**: All network operations on IO dispatcher
4. **Timeout Limits**: Reasonable timeouts prevent UI blocking
5. **Proxy List Limiting**: Max 50 proxies per fetch to prevent memory issues

## Future Enhancements

Potential additions for future versions:

1. **Proxy Authentication**: Username/password support for authenticated proxies
2. **Proxy Rotation**: Automatic rotation through multiple proxies
3. **Geolocation Display**: Show proxy server location on map
4. **Favorites**: Save frequently used proxies
5. **Auto-Test**: Periodic automatic validation of saved proxies
6. **Custom API Support**: Allow users to add their own proxy API sources
7. **Blacklist**: Automatically blacklist non-working proxies
8. **Performance History**: Track proxy performance over time

## Troubleshooting

### Common Issues

**Issue**: Proxy test fails but downloads work
- **Reason**: Test URLs (google.com/bing.com) may be blocked, but target sites work
- **Solution**: Consider adding custom test URL option

**Issue**: Speed test shows very slow speeds
- **Reason**: Free proxies are often slow and congested
- **Solution**: Try different proxy or use custom proxy

**Issue**: Proxy settings don't persist
- **Reason**: MMKV storage issue
- **Solution**: Check app permissions and storage availability

**Issue**: No proxies available for selected country
- **Reason**: ProxyScrape API may have no proxies for that region
- **Solution**: Try different country or check API status

## Code Structure

```
app/src/main/java/com/junkfood/seal/
├── ui/page/settings/network/
│   └── ProxySettingsPage.kt          # UI composables
├── util/
│   ├── ProxyManager.kt               # Configuration & API
│   ├── ProxyValidator.kt             # Testing engine
│   ├── PreferenceUtil.kt             # Preference keys
│   ├── DownloadUtil.kt               # Download proxy integration
│   ├── UpdateUtil.kt                 # Update proxy integration
│   ├── SponsorUtil.kt                # Sponsor proxy integration
│   └── FormatValidator.kt            # Format proxy integration
└── ui/common/
    └── Route.kt                       # Navigation routes

app/src/main/res/values/
└── strings.xml                        # Localized strings
```

## Dependencies

No new dependencies required! Uses existing:
- OkHttp (already in project)
- Kotlinx Coroutines (already in project)
- Kotlinx Serialization (already in project)
- MMKV (already in project)
- Compose Material3 (already in project)

## Localization

All strings are defined in `strings.xml` for easy translation:
- `proxy_settings`, `enable_proxy`, `free_proxy`, `custom_proxy`
- `connection_status`, `test_connection`, `run_speed_test`
- `proxy_host`, `proxy_port`, `proxy_protocol`
- And 20+ more proxy-related strings

## Conclusion

The proxy settings feature is fully implemented and integrated into the Sealplus app. It provides a user-friendly interface for configuring, testing, and using proxy servers for all network operations. The implementation follows Android best practices, uses Material Design 3 guidelines, and integrates seamlessly with the existing codebase.
