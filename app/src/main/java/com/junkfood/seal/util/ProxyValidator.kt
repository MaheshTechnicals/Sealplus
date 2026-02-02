package com.junkfood.seal.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * ProxyValidator handles proxy connection validation and speed testing
 */
object ProxyValidator {
    private const val TAG = "ProxyValidator"
    private const val CONNECTION_TIMEOUT_MS = 10000L
    private const val TEST_URL_PRIMARY = "https://www.google.com"
    private const val TEST_URL_FALLBACK = "https://www.bing.com"
    private const val SPEED_TEST_URL = "https://www.google.com/robots.txt"

    /**
     * Validation result
     */
    sealed class ValidationResult {
        data class Success(val latencyMs: Long, val message: String = "Connected") : ValidationResult()
        data class Failed(val error: String) : ValidationResult()
        data object Testing : ValidationResult()
    }

    /**
     * Speed test result
     */
    data class SpeedTestResult(
        val success: Boolean,
        val latencyMs: Long = 0,
        val downloadSpeedKbps: Double = 0.0,
        val uploadLatencyMs: Long = 0,
        val error: String? = null
    ) {
        fun getSpeedMbps(): Double = downloadSpeedKbps / 1024.0
        
        fun getLatencyDescription(): String {
            return when {
                latencyMs < 100 -> "Excellent"
                latencyMs < 300 -> "Good"
                latencyMs < 600 -> "Fair"
                else -> "Poor"
            }
        }
    }

    /**
     * Validate proxy connection by making HEAD request to test URLs
     * @param proxy The Java Proxy to test
     * @return ValidationResult with status and latency
     */
    suspend fun validateProxyConnection(proxy: Proxy?): ValidationResult = withContext(Dispatchers.IO) {
        if (proxy == null) {
            return@withContext ValidationResult.Failed("Invalid proxy configuration")
        }

        return@withContext withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
            try {
                // Try primary URL first
                val result = testConnection(proxy, TEST_URL_PRIMARY)
                if (result is ValidationResult.Success) {
                    return@withTimeoutOrNull result
                }

                // Fallback to secondary URL
                testConnection(proxy, TEST_URL_FALLBACK)
            } catch (e: Exception) {
                Log.e(TAG, "Connection validation failed", e)
                ValidationResult.Failed(e.message ?: "Connection failed")
            }
        } ?: ValidationResult.Failed("Connection timeout")
    }

    /**
     * Test connection to specific URL
     */
    private suspend fun testConnection(proxy: Proxy, url: String): ValidationResult {
        return try {
            val client = OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = Request.Builder()
                .url(url)
                .head() // HEAD request is lightweight
                .build()

            var latency = 0L
            val response = measureTimeMillis {
                latency = System.currentTimeMillis()
                client.newCall(request).execute()
            }
            
            latency = System.currentTimeMillis() - latency

            if (latency > 0) {
                Log.d(TAG, "Proxy connected successfully. Latency: ${latency}ms")
                ValidationResult.Success(latency, "Connected (${latency}ms)")
            } else {
                ValidationResult.Failed("Connection failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test connection failed for $url", e)
            ValidationResult.Failed(e.message ?: "Connection error")
        }
    }

    /**
     * Perform comprehensive speed test
     * @param proxy The Java Proxy to test
     * @return SpeedTestResult with detailed metrics
     */
    suspend fun performSpeedTest(proxy: Proxy?): SpeedTestResult = withContext(Dispatchers.IO) {
        if (proxy == null) {
            return@withContext SpeedTestResult(
                success = false,
                error = "Invalid proxy configuration"
            )
        }

        return@withContext withTimeoutOrNull(20000L) {
            try {
                val client = OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build()

                // Test 1: Measure latency with HEAD request
                val request = Request.Builder()
                    .url(SPEED_TEST_URL)
                    .head()
                    .build()

                var latency = 0L
                measureTimeMillis {
                    val startTime = System.currentTimeMillis()
                    client.newCall(request).execute().use {
                        latency = System.currentTimeMillis() - startTime
                    }
                }

                // Test 2: Measure download speed with GET request
                val downloadRequest = Request.Builder()
                    .url(SPEED_TEST_URL)
                    .get()
                    .build()

                var downloadTime = 0L
                var bytesDownloaded = 0L
                
                measureTimeMillis {
                    val startTime = System.currentTimeMillis()
                    client.newCall(downloadRequest).execute().use { response ->
                        val body = response.body?.bytes() ?: ByteArray(0)
                        bytesDownloaded = body.size.toLong()
                        downloadTime = System.currentTimeMillis() - startTime
                    }
                }

                // Calculate download speed in Kbps
                val downloadSpeedKbps = if (downloadTime > 0) {
                    (bytesDownloaded * 8.0) / (downloadTime / 1000.0) / 1024.0
                } else {
                    0.0
                }

                // Test 3: Measure upload latency (using HEAD request as proxy)
                val uploadRequest = Request.Builder()
                    .url(TEST_URL_PRIMARY)
                    .head()
                    .build()

                var uploadLatency = 0L
                measureTimeMillis {
                    val startTime = System.currentTimeMillis()
                    client.newCall(uploadRequest).execute().use {
                        uploadLatency = System.currentTimeMillis() - startTime
                    }
                }

                Log.d(TAG, "Speed test completed - Latency: ${latency}ms, Download: ${"%.2f".format(downloadSpeedKbps)} Kbps")

                SpeedTestResult(
                    success = true,
                    latencyMs = latency,
                    downloadSpeedKbps = downloadSpeedKbps,
                    uploadLatencyMs = uploadLatency
                )
            } catch (e: Exception) {
                Log.e(TAG, "Speed test failed", e)
                SpeedTestResult(
                    success = false,
                    error = e.message ?: "Speed test failed"
                )
            }
        } ?: SpeedTestResult(
            success = false,
            error = "Speed test timeout"
        )
    }

    /**
     * Check if network is available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Quick ping test - lightweight validation
     */
    suspend fun quickPing(proxy: Proxy?): Boolean = withContext(Dispatchers.IO) {
        if (proxy == null) return@withContext false

        return@withContext withTimeoutOrNull(5000L) {
            try {
                val client = OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(TEST_URL_PRIMARY)
                    .head()
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Quick ping failed", e)
                false
            }
        } ?: false
    }
}
