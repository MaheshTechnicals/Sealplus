package com.junkfood.seal.util

import android.content.Context
import android.util.Log
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.junkfood.seal.util.PreferenceUtil.updateString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * ProxyManager handles all proxy-related operations including
 * fetching free proxies, managing custom proxies, and configuration
 */
object ProxyManager {
    private const val TAG = "ProxyManager"
    private const val PROXYSCRAPE_API = "https://api.proxyscrape.com/v2/"
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Supported proxy types
     */
    enum class ProxyType(val displayName: String, val protocolName: String) {
        HTTP("HTTP", "http"),
        HTTPS("HTTPS", "https"),
        SOCKS4("SOCKS4", "socks4"),
        SOCKS5("SOCKS5", "socks5");

        companion object {
            fun fromString(value: String): ProxyType {
                return entries.find { it.name == value } ?: HTTP
            }
        }
    }

    /**
     * Supported countries for free proxy
     */
    enum class ProxyCountry(val displayName: String, val code: String) {
        USA("United States", "US"),
        UK("United Kingdom", "GB"),
        SINGAPORE("Singapore", "SG"),
        GERMANY("Germany", "DE"),
        SWITZERLAND("Switzerland", "CH");

        companion object {
            fun fromCode(code: String): ProxyCountry? {
                return entries.find { it.code == code }
            }
        }
    }

    /**
     * Proxy configuration data class
     */
    @Serializable
    data class ProxyConfig(
        val enabled: Boolean = false,
        val useFreeProxy: Boolean = true,
        val freeProxyCountry: String = ProxyCountry.USA.code,
        val freeProxyAddress: String = "",
        val customProxyHost: String = "",
        val customProxyPort: Int = 0,
        val customProxyType: String = ProxyType.HTTP.name,
        val lastValidated: Long = 0L,
        val isWorking: Boolean = false
    ) {
        fun isValid(): Boolean {
            return if (useFreeProxy) {
                freeProxyAddress.isNotEmpty()
            } else {
                customProxyHost.isNotEmpty() && customProxyPort in 1..65535
            }
        }

        fun getProxyAddress(): String {
            return if (useFreeProxy) {
                freeProxyAddress
            } else {
                "$customProxyHost:$customProxyPort"
            }
        }

        fun toJavaProxy(): Proxy? {
            if (!isValid()) return null

            return try {
                val (host, port) = if (useFreeProxy) {
                    val parts = freeProxyAddress.split(":")
                    if (parts.size != 2) return null
                    parts[0] to (parts[1].toIntOrNull() ?: return null)
                } else {
                    customProxyHost to customProxyPort
                }

                val type = if (useFreeProxy) {
                    Proxy.Type.HTTP
                } else {
                    when (ProxyType.fromString(customProxyType)) {
                        ProxyType.HTTP, ProxyType.HTTPS -> Proxy.Type.HTTP
                        ProxyType.SOCKS4, ProxyType.SOCKS5 -> Proxy.Type.SOCKS
                    }
                }

                Proxy(type, InetSocketAddress.createUnresolved(host, port))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create proxy", e)
                null
            }
        }
    }

    /**
     * Fetch free proxies from ProxyScrape API
     * @param country The country code to fetch proxies for
     * @return List of proxy addresses in format "host:port"
     */
    suspend fun fetchFreeProxies(country: ProxyCountry): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val url = buildString {
                append(PROXYSCRAPE_API)
                append("?request=getproxies")
                append("&protocol=http")
                append("&country=${country.code}")
                append("&ssl=all")
                append("&anonymity=anonymous")
                append("&timeout=5000")
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch proxies: ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            
            // Parse the response - ProxyScrape returns newline-separated list
            val proxies = body.trim()
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.contains(":") }
                .distinct()
                .take(50) // Limit to 50 proxies

            if (proxies.isEmpty()) {
                return@withContext Result.failure(Exception("No proxies available for ${country.displayName}"))
            }

            Log.d(TAG, "Fetched ${proxies.size} proxies for ${country.displayName}")
            Result.success(proxies)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching free proxies", e)
            Result.failure(e)
        }
    }

    /**
     * Save proxy configuration to preferences
     */
    fun saveProxyConfig(config: ProxyConfig) {
        PROXY_ENABLED.updateBoolean(config.enabled)
        PROXY_USE_FREE.updateBoolean(config.useFreeProxy)
        PROXY_FREE_COUNTRY.updateString(config.freeProxyCountry)
        PROXY_FREE_ADDRESS.updateString(config.freeProxyAddress)
        PROXY_CUSTOM_HOST.updateString(config.customProxyHost)
        PROXY_CUSTOM_PORT.updateInt(config.customProxyPort)
        PROXY_CUSTOM_TYPE.updateString(config.customProxyType)
        PROXY_LAST_VALIDATED.updateLong(config.lastValidated)
        PROXY_IS_WORKING.updateBoolean(config.isWorking)
    }

    /**
     * Load proxy configuration from preferences
     */
    fun loadProxyConfig(): ProxyConfig {
        return ProxyConfig(
            enabled = PROXY_ENABLED.getBoolean(),
            useFreeProxy = PROXY_USE_FREE.getBoolean(),
            freeProxyCountry = PROXY_FREE_COUNTRY.getString(),
            freeProxyAddress = PROXY_FREE_ADDRESS.getString(),
            customProxyHost = PROXY_CUSTOM_HOST.getString(),
            customProxyPort = PROXY_CUSTOM_PORT.getInt(),
            customProxyType = PROXY_CUSTOM_TYPE.getString(),
            lastValidated = PROXY_LAST_VALIDATED.getLong(),
            isWorking = PROXY_IS_WORKING.getBoolean()
        )
    }

    /**
     * Clear all proxy settings
     */
    fun clearProxyConfig() {
        saveProxyConfig(ProxyConfig())
    }

    /**
     * Get the current active proxy for OkHttpClient
     */
    fun getActiveProxy(): Proxy? {
        val config = loadProxyConfig()
        return if (config.enabled && config.isValid()) {
            config.toJavaProxy()
        } else {
            null
        }
    }

    /**
     * Check if proxy is currently enabled and working
     */
    fun isProxyActive(): Boolean {
        val config = loadProxyConfig()
        return config.enabled && config.isValid() && config.isWorking
    }
}
