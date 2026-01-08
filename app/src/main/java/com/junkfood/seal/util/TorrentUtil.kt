package com.junkfood.seal.util

import android.content.Context
import android.util.Log
import com.junkfood.seal.App.Companion.applicationScope
import com.junkfood.seal.App.Companion.context
import com.yausername.aria2c.Aria2c
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object TorrentUtil {
    private const val TAG = "TorrentUtil"
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    /**
     * Check if a string is a magnet link
     */
    fun isMagnetLink(url: String): Boolean {
        return url.startsWith("magnet:", ignoreCase = true)
    }

    /**
     * Check if a string is a torrent file URL
     */
    fun isTorrentUrl(url: String): Boolean {
        return url.endsWith(".torrent", ignoreCase = true) || url.contains(".torrent?")
    }

    /**
     * Check if torrent support is enabled
     */
    fun isTorrentSupportEnabled(): Boolean {
        return with(PreferenceUtil) { TORRENT_SUPPORT.getBoolean() }
    }

    /**
     * Download torrent file from URL to cache directory
     */
    suspend fun downloadTorrentFile(url: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val torrentFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.torrent")
            FileOutputStream(torrentFile).use { output ->
                connection.inputStream.use { input ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Downloaded torrent file: ${torrentFile.absolutePath}")
            torrentFile
        }.onFailure { error ->
            Log.e(TAG, "Failed to download torrent file", error)
        }
    }

    /**
     * Start torrent download using aria2c
     */
    suspend fun startTorrentDownload(
        torrentSource: String, // magnet link or torrent file path
        downloadDir: String,
        progressCallback: (TorrentProgress) -> Unit
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Starting torrent download: $torrentSource")
            
            val outputDir = File(downloadDir)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val aria2cArgs = buildList {
                add(torrentSource)
                add("--dir=$downloadDir")
                add("--seed-time=0") // Don't seed after download
                add("--bt-enable-lpd=true")
                add("--bt-max-peers=50")
                add("--enable-dht=true")
                add("--follow-torrent=mem")
                add("--max-connection-per-server=16")
                add("--min-split-size=1M")
                add("--split=16")
                add("--enable-color=false")
                add("--console-log-level=notice")
                add("--summary-interval=1")
            }

            Log.d(TAG, "Aria2c args: ${aria2cArgs.joinToString(" ")}")

            // Execute aria2c command
            val result = executeAria2cCommand(aria2cArgs.toTypedArray())
            
            // Parse output to get downloaded files
            val downloadedFiles = mutableListOf<String>()
            result.out.lines().forEach { line ->
                if (line.contains("Download complete:")) {
                    val filePath = line.substringAfter("Download complete:").trim()
                    downloadedFiles.add(filePath)
                }
            }

            if (downloadedFiles.isEmpty()) {
                // If we can't parse output, list files in download directory
                outputDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        downloadedFiles.add(file.absolutePath)
                    }
                }
            }

            Log.d(TAG, "Download completed. Files: $downloadedFiles")
            downloadedFiles
        }.onFailure { error ->
            Log.e(TAG, "Torrent download failed", error)
        }
    }

    /**
     * Monitor torrent download progress
     */
    fun monitorTorrentProgress(
        torrentId: String,
        downloadDir: String
    ): Flow<TorrentProgress> = flow {
        var lastProgress = 0f
        var lastSpeed = 0.0
        
        while (lastProgress < 100f) {
            delay(1000) // Update every second

            // Check files in download directory
            val downloadDirFile = File(downloadDir)
            val files = downloadDirFile.listFiles()?.filter { it.isFile } ?: emptyList()
            
            val totalSize = files.sumOf { it.length() }
            val progress = if (totalSize > 0) {
                // Estimate progress (this is approximate)
                minOf((totalSize / 1024.0 / 1024.0 / 10.0 * 100.0).toFloat(), 100f)
            } else {
                lastProgress
            }

            // Calculate speed (rough estimate)
            val speed = if (progress > lastProgress) {
                ((progress - lastProgress) * 1024 * 1024).toDouble() // Rough MB/s estimate
            } else {
                lastSpeed
            }

            lastProgress = progress
            lastSpeed = speed

            emit(
                TorrentProgress(
                    torrentId = torrentId,
                    progress = progress,
                    downloadSpeed = speed.toDouble(),
                    uploadSpeed = 0.0,
                    totalSize = totalSize,
                    downloadedSize = (totalSize * progress / 100).toLong(),
                    numSeeders = 0,
                    numPeers = 0,
                    files = files.map { it.name }
                )
            )

            if (progress >= 100f) break
        }
    }

    /**
     * Execute aria2c command
     */
    private suspend fun executeAria2cCommand(args: Array<String>): Aria2cResult = 
        withContext(Dispatchers.IO) {
            try {
                val output = StringBuilder()
                val error = StringBuilder()

                val processBuilder = ProcessBuilder()
                    .command(context.applicationInfo.nativeLibraryDir + "/libaria2c.so", *args)
                    .redirectErrorStream(true)

                val process = processBuilder.start()
                
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        output.append(line).append("\n")
                        Log.d(TAG, "Aria2c: $line")
                    }
                }

                val exitCode = process.waitFor()
                
                Aria2cResult(
                    exitCode = exitCode,
                    out = output.toString(),
                    err = error.toString()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Aria2c execution failed", e)
                Aria2cResult(
                    exitCode = -1,
                    out = "",
                    err = e.message ?: "Unknown error"
                )
            }
        }

    /**
     * Get torrent download directory
     */
    fun getTorrentDownloadDir(): String {
        return FileUtil.getExternalDownloadDirectory().resolve("Torrents").apply {
            if (!exists()) mkdirs()
        }.absolutePath
    }

    /**
     * Parse magnet link to extract info hash and display name
     */
    fun parseMagnetLink(magnetUrl: String): MagnetInfo? {
        if (!isMagnetLink(magnetUrl)) return null

        return try {
            val dn = Regex("dn=([^&]+)").find(magnetUrl)?.groupValues?.get(1)
                ?.replace("+", " ")
                ?.replace("%20", " ")
            val xt = Regex("xt=urn:btih:([^&]+)").find(magnetUrl)?.groupValues?.get(1)

            MagnetInfo(
                displayName = dn ?: "Unknown Torrent",
                infoHash = xt ?: "",
                magnetUrl = magnetUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse magnet link", e)
            null
        }
    }

    /**
     * Clean up temporary torrent files
     */
    fun cleanupTorrentCache() {
        applicationScope.launchSafely {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".torrent")) {
                    file.delete()
                }
            }
        }
    }
}

/**
 * Data class for torrent progress
 */
@Serializable
data class TorrentProgress(
    val torrentId: String,
    val progress: Float,
    val downloadSpeed: Double, // bytes per second
    val uploadSpeed: Double,
    val totalSize: Long,
    val downloadedSize: Long,
    val numSeeders: Int,
    val numPeers: Int,
    val files: List<String>
)

/**
 * Data class for magnet link info
 */
@Serializable
data class MagnetInfo(
    val displayName: String,
    val infoHash: String,
    val magnetUrl: String
)

/**
 * Result from aria2c command execution
 */
private data class Aria2cResult(
    val exitCode: Int,
    val out: String,
    val err: String
)
