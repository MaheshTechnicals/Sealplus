package com.junkfood.seal.util

import android.util.Log
import com.junkfood.seal.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

private const val TAG = "FormatValidator"

/**
 * Validates video/audio formats before displaying them to the user.
 * Filters out formats that are:
 * - Video-only or audio-only (when not needed)
 * - DRM-protected
 * - Have expired URLs
 * - Have unsupported codecs
 * - Return fetch errors
 */
object FormatValidator {

    private fun getClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
        
        ProxyManager.getActiveProxy()?.let { proxy ->
            builder.proxy(proxy)
        }
        
        return builder.build()
    }

    // Known unsupported codecs that may cause issues
    private val unsupportedVideoCodecs = setOf(
        "av01",  // AV1 - not widely supported on older devices
    )

    private val unsupportedAudioCodecs = setOf<String>(
        // Most audio codecs are supported, but we can add problematic ones here
    )

    // DRM indicators in format strings
    private val drmIndicators = listOf(
        "drm", "encrypted", "widevine", "playready", "fairplay", "protected"
    )

    /**
     * Data class to hold validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val reason: String? = null,
        val hasValidUrl: Boolean = true,
        val hasSupportedCodec: Boolean = true,
        val isDrmFree: Boolean = true,
        val isAccessible: Boolean = true
    )

    /**
     * Validates a single format comprehensively
     */
    suspend fun validateFormat(format: Format, checkUrlAccessibility: Boolean = false): ValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check 1: DRM protection
                if (!isDrmFree(format)) {
                    return@withContext ValidationResult(
                        isValid = false,
                        reason = "DRM-protected content",
                        isDrmFree = false
                    )
                }

                // Check 2: Has valid URL
                if (format.url.isNullOrBlank()) {
                    return@withContext ValidationResult(
                        isValid = false,
                        reason = "No download URL available",
                        hasValidUrl = false
                    )
                }

                // Check 3: Codec support
                if (!hasSupportedCodecs(format)) {
                    return@withContext ValidationResult(
                        isValid = false,
                        reason = "Unsupported codec",
                        hasSupportedCodec = false
                    )
                }

                // Check 4: URL accessibility (optional, can be slow)
                if (checkUrlAccessibility && !isUrlAccessible(format.url)) {
                    return@withContext ValidationResult(
                        isValid = false,
                        reason = "URL not accessible or expired",
                        isAccessible = false
                    )
                }

                // All checks passed
                ValidationResult(isValid = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error validating format ${format.formatId}: ${e.message}")
                ValidationResult(
                    isValid = false,
                    reason = "Validation error: ${e.message}"
                )
            }
        }
    }

    /**
     * Validates a list of formats and returns only valid ones
     */
    suspend fun filterValidFormats(
        formats: List<Format>,
        checkUrlAccessibility: Boolean = false
    ): List<Format> {
        return withContext(Dispatchers.IO) {
            formats.filter { format ->
                val result = validateFormat(format, checkUrlAccessibility)
                if (!result.isValid) {
                    Log.d(TAG, "Filtered out format ${format.formatId}: ${result.reason}")
                }
                result.isValid
            }
        }
    }

    /**
     * Checks if format is DRM-protected
     */
    private fun isDrmFree(format: Format): Boolean {
        val formatString = format.format?.lowercase() ?: ""
        val formatNote = format.formatNote?.lowercase() ?: ""
        
        return !drmIndicators.any { indicator ->
            formatString.contains(indicator) || formatNote.contains(indicator)
        }
    }

    /**
     * Checks if format has supported codecs
     */
    private fun hasSupportedCodecs(format: Format): Boolean {
        val vcodec = format.vcodec?.lowercase() ?: "none"
        val acodec = format.acodec?.lowercase() ?: "none"

        // Check video codec
        if (vcodec != "none") {
            // Check if it's in unsupported list (partial match)
            if (unsupportedVideoCodecs.any { vcodec.startsWith(it) }) {
                return false
            }
        }

        // Check audio codec
        if (acodec != "none") {
            if (unsupportedAudioCodecs.any { acodec.startsWith(it) }) {
                return false
            }
        }

        return true
    }

    /**
     * Checks if URL is accessible (HEAD request)
     * This is an expensive operation and should be used sparingly
     */
    private suspend fun isUrlAccessible(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .build()

                getClient().newCall(request).execute().use { response ->
                    val code = response.code
                    // Accept 200 OK and 206 Partial Content (for streaming)
                    code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking URL accessibility: ${e.message}")
                false
            }
        }
    }

    /**
     * Deduplicates formats by resolution, keeping the best quality for each resolution
     */
    fun deduplicateByResolution(formats: List<Format>): List<Format> {
        // Group formats by resolution
        val groupedByResolution = formats.groupBy { format ->
            extractResolutionKey(format)
        }

        // For each resolution, keep the format with highest bitrate/filesize
        return groupedByResolution.mapNotNull { (resolutionKey, formatsAtResolution) ->
            if (resolutionKey == null) {
                // Keep formats without clear resolution info as-is
                formatsAtResolution
            } else {
                // Pick the best quality format for this resolution
                listOf(selectBestFormat(formatsAtResolution))
            }
        }.flatten()
    }

    /**
     * Extracts a resolution key for grouping (e.g., "1920x1080", "1280x720")
     */
    private fun extractResolutionKey(format: Format): String? {
        // Try explicit width/height first
        if (format.width != null && format.height != null && 
            format.width > 0 && format.height > 0) {
            return "${format.width.toInt()}x${format.height.toInt()}"
        }

        // Try to extract from format string
        val formatString = format.format ?: format.resolution ?: ""
        val resolutionRegex = """(\d{3,4})x(\d{3,4})""".toRegex()
        val match = resolutionRegex.find(formatString)
        if (match != null) {
            return "${match.groupValues[1]}x${match.groupValues[2]}"
        }

        // Try height-only pattern (e.g., "1080p")
        val heightPattern = """(\d{3,4})p""".toRegex()
        val heightMatch = heightPattern.find(formatString)
        if (heightMatch != null) {
            val height = heightMatch.groupValues[1]
            val width = (height.toInt() * 16 / 9) // Assume 16:9
            return "${width}x${height}"
        }

        return null
    }

    /**
     * Selects the best format from a list based on quality indicators
     */
    private fun selectBestFormat(formats: List<Format>): Format {
        return formats.maxByOrNull { format ->
            // Calculate a quality score
            val bitrateScore = (format.tbr ?: format.vbr ?: 0.0) * 1000
            val filesizeScore = (format.fileSizeApprox ?: format.fileSize ?: 0.0) / 1000
            val resolutionScore = ((format.width ?: 0.0) * (format.height ?: 0.0)) / 1000
            
            // Prefer formats with explicit audio+video
            val combinedBonus = if (format.containsAudio() && format.containsVideo()) 1000.0 else 0.0
            
            bitrateScore + filesizeScore + resolutionScore + combinedBonus
        } ?: formats.first()
    }

    /**
     * Groups formats by resolution with a label (e.g., "1080p", "720p", "4K")
     */
    fun groupByResolutionWithLabel(formats: List<Format>): Map<String, List<Format>> {
        return formats.groupBy { format ->
            extractResolutionLabel(format) ?: "Unknown"
        }
    }

    /**
     * Extracts a human-readable resolution label
     */
    private fun extractResolutionLabel(format: Format): String? {
        val height = format.height?.toInt() ?: run {
            // Try to extract from format string
            val formatString = format.format ?: ""
            val heightPattern = """(\d{3,4})p""".toRegex()
            heightPattern.find(formatString)?.groupValues?.get(1)?.toIntOrNull()
        }

        return when {
            height == null -> null
            height >= 2160 -> "4K (${height}p)"
            height >= 1440 -> "2K (${height}p)"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            height >= 360 -> "360p"
            else -> "${height}p"
        }
    }

    /**
     * Checks if network is available
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = App.connectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability: ${e.message}")
            false
        }
    }
}
