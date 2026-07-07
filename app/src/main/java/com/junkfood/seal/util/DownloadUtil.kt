package com.junkfood.seal.util

import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import androidx.annotation.CheckResult
import com.junkfood.seal.App
import java.io.File
import com.junkfood.seal.BuildConfig
import com.junkfood.seal.App.Companion.audioDownloadDir
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.App.Companion.videoDownloadDir
import com.junkfood.seal.Downloader
import com.junkfood.seal.Downloader.onProcessEnded
import com.junkfood.seal.Downloader.onProcessStarted
import com.junkfood.seal.Downloader.onTaskEnded
import com.junkfood.seal.Downloader.onTaskError
import com.junkfood.seal.Downloader.onTaskStarted
import com.junkfood.seal.Downloader.toNotificationId
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.page.settings.network.Cookie
import com.junkfood.seal.util.FileUtil.getArchiveFile
import com.junkfood.seal.util.FileUtil.getConfigFile
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.FileUtil.getExternalTempDir
import com.junkfood.seal.util.FileUtil.getFileName
import com.junkfood.seal.util.FileUtil.getSdcardTempDir
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.FileUtil.moveFilesToSdcard
import com.junkfood.seal.util.PreferenceUtil.COOKIE_HEADER
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

object DownloadUtil {

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    // -------------------------------------------------------------------------
    // Manual cookie content parsing
    //
    // When a CookieProfile has non-empty `content`, the user pasted cookies
    // manually (Netscape / JSON / name=value format). These are parsed here and
    // used directly instead of reading from CookieManager.
    // -------------------------------------------------------------------------

    /** JSON shape produced by "Cookie-Editor" and "EditThisCookie" browser extensions. */
    @Serializable
    private data class CookieJson(
        val name: String = "",
        val value: String = "",
        val domain: String = "",
        val path: String = "/",
        val secure: Boolean = false,
        @SerialName("httpOnly") val httpOnly: Boolean = false,
        // Both extension names in the wild:
        val expirationDate: Double = 0.0,
        val expires: Double = 0.0,
        val session: Boolean = true,
    )

    /**
     * Parses manually-pasted cookie text into a [List<Cookie>].
     * Three input formats are supported:
     *
     *  1. **Netscape / cookies.txt** — tab-separated 7-field lines.
     *  2. **JSON** — array of cookie objects exported by "Cookie-Editor" /
     *     "EditThisCookie" browser extensions.
     *  3. **Header / name=value** — the semicolon-delimited string you see
     *     when you copy a `Cookie:` request header from a browser.
     *
     * @param profileUrl The URL of the [CookieProfile] — used to derive the
     *   domain for format 3, where no domain is available in the text.
     * @param content    The raw text the user pasted.
     */
    fun parseCookieContent(profileUrl: String, content: String): List<Cookie> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        return runCatching {
            when {
                // JSON: starts with [ (array) or { (single object)
                trimmed.startsWith('[') || trimmed.startsWith('{') ->
                    parseJsonCookies(profileUrl, trimmed)

                // Netscape: has at least one line that contains a tab character
                // (comment lines start with # and are fine to skip)
                trimmed.lines().any { it.isNotBlank() && !it.startsWith('#') && '\t' in it } ->
                    parseNetscapeCookies(trimmed)

                // Fallback: treat as Cookie header value  "name=val; name=val"
                else -> parseHeaderCookies(profileUrl, trimmed)
            }
        }.getOrElse { e ->
            Log.w(TAG, "parseCookieContent failed, attempting header fallback: ${e.message}")
            runCatching { parseHeaderCookies(profileUrl, trimmed) }.getOrDefault(emptyList())
        }
    }

    private fun parseJsonCookies(profileUrl: String, json: String): List<Cookie> {
        // Wrap bare object in an array so the decoder always receives a list.
        val normalised = if (json.trimStart().startsWith('{')) "[$json]" else json
        val items = jsonFormat.decodeFromString<List<CookieJson>>(normalised)
        val now = System.currentTimeMillis() / 1000L
        val fallbackDomain = profileUrl.let {
            val url = if (it.startsWith("http")) it else "https://$it"
            "." + (Uri.parse(url).host?.removePrefix("www.") ?: return@let "")
        }
        return items.mapNotNull { c ->
            if (c.name.isEmpty()) return@mapNotNull null
            // Pick whichever expiry field is populated; 0 means session cookie
            val expiry = when {
                c.expirationDate > 0 -> c.expirationDate.toLong()
                c.expires > 0        -> c.expires.toLong()
                else                 -> 0L
            }
            if (expiry > 0L && expiry < now) return@mapNotNull null // expired
            val domain = c.domain.let { d ->
                when {
                    d.isBlank()         -> fallbackDomain
                    d.startsWith('.')   -> d
                    else                -> ".$d"
                }
            }
            Cookie(
                domain            = domain,
                name              = c.name,
                value             = c.value,
                includeSubdomains = true,
                path              = c.path.ifEmpty { "/" },
                secure            = c.secure,
                expiry            = expiry,
                isHttpOnly        = c.httpOnly,
            )
        }
    }

    private fun parseNetscapeCookies(text: String): List<Cookie> {
        val now = System.currentTimeMillis() / 1000L
        val cookies = mutableListOf<Cookie>()
        text.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) return@forEach
            val parts = trimmed.split('\t')
            if (parts.size < 7) return@forEach
            val domain            = parts[0]
            val includeSubdomains = parts[1].uppercase() == "TRUE"
            val path              = parts[2]
            val secure            = parts[3].uppercase() == "TRUE"
            val expiry            = parts[4].toLongOrNull() ?: 0L
            val name              = parts[5]
            // Value may itself contain tabs — re-join the remainder
            val value             = parts.drop(6).joinToString("\t")
            if (name.isEmpty()) return@forEach
            if (expiry > 0L && expiry < now) return@forEach // skip expired
            cookies.add(
                Cookie(
                    domain            = if (domain.startsWith('.')) domain else ".$domain",
                    name              = name,
                    value             = value,
                    includeSubdomains = includeSubdomains,
                    path              = path,
                    secure            = secure,
                    expiry            = expiry,
                    isHttpOnly        = false,
                )
            )
        }
        return cookies
    }

    private fun parseHeaderCookies(profileUrl: String, header: String): List<Cookie> {
        val rawUrl = if (profileUrl.startsWith("http")) profileUrl else "https://$profileUrl"
        val host   = Uri.parse(rawUrl).host ?: ""
        val domain = "." + if (host.startsWith("www.")) host.removePrefix("www.") else host
        val cookies = mutableListOf<Cookie>()
        // Strip a leading "Cookie: " HTTP header prefix if the user copied the whole line
        val cookiePart = header.removePrefix("Cookie:").removePrefix("cookie:").trim()
        cookiePart.split(';').forEach { pair ->
            val eqIdx = pair.indexOf('=')
            if (eqIdx < 0) return@forEach
            val name  = pair.substring(0, eqIdx).trim()
            val value = pair.substring(eqIdx + 1).trim()
            if (name.isEmpty()) return@forEach
            cookies.add(
                Cookie(
                    domain            = domain,
                    name              = name,
                    value             = value,
                    includeSubdomains = true,
                    path              = "/",
                    secure            = false,
                    expiry            = 0L,
                    isHttpOnly        = false,
                )
            )
        }
        return cookies
    }

    private const val TAG = "DownloadUtil"

    const val BASENAME = "%(title).200B"

    const val EXTENSION = ".%(ext)s"

    private const val ID = "[%(id)s]"

    private const val CLIP_TIMESTAMP = "%(section_start)d-%(section_end)d"

    const val OUTPUT_TEMPLATE_DEFAULT = BASENAME + EXTENSION

    const val OUTPUT_TEMPLATE_ID = "$BASENAME $ID$EXTENSION"

    private const val OUTPUT_TEMPLATE_CLIPS = "$BASENAME [$CLIP_TIMESTAMP]$EXTENSION"

    private const val OUTPUT_TEMPLATE_CHAPTERS =
        "chapter:$BASENAME/%(section_number)d - %(section_title).200B$EXTENSION"

    private const val OUTPUT_TEMPLATE_SPLIT = "$BASENAME/$OUTPUT_TEMPLATE_DEFAULT"

    private const val PLAYLIST_TITLE_SUBDIRECTORY_PREFIX = "%(playlist)s/"

    private const val CROP_ARTWORK_COMMAND =
        """--ppa "ffmpeg: -c:v mjpeg -vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\"""""

    @CheckResult
    fun getPlaylistOrVideoInfo(
        playlistURL: String,
        downloadPreferences: DownloadPreferences = DownloadPreferences.createFromPreferences(),
    ): Result<YoutubeDLInfo> =
        YoutubeDL.runCatching {
            App.applicationScope.launch(Dispatchers.Main) {
                context.makeToast(R.string.fetching_playlist_info)
            }
            val request = YoutubeDLRequest(playlistURL)
            with(request) {
                //            addOption("--compat-options", "no-youtube-unavailable-videos")
                addOption("--flat-playlist")
                addOption("--dump-single-json")
                addOption("-o", BASENAME)
                // Info-probe resilience: 1 retry + 5s timeout was too aggressive and made
                // slow/distant servers (common for many non-YouTube sites) fail to even
                // fetch metadata. 3 retries + 15s timeout is far more tolerant while still
                // failing reasonably fast on genuinely dead hosts.
                addOption("-R", "3")
                addOption("--socket-timeout", "15")
                downloadPreferences.run {
                    if (extractAudio) {
                        addOption("-x")
                    }
                    applyFormatSorter(this, toFormatSorter())
                    if (proxy) {
                        enableProxy(proxyUrl)
                    }
                    if (forceIpv4) {
                        addOption("-4")
                    }
                    if (noCheckCertificate) {
                        addOption("--no-check-certificate")
                    }
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                }
            }
            execute(request, playlistURL).out.run {
                val playlistInfo = jsonFormat.decodeFromString<PlaylistResult>(this)
                if (playlistInfo.type != "playlist") {
                    jsonFormat.decodeFromString<VideoInfo>(this)
                } else playlistInfo
            }
        }

    @CheckResult
    private fun getVideoInfo(
        request: YoutubeDLRequest,
        taskKey: String? = null,
    ): Result<VideoInfo> =
        request.runCatching {
            val response: YoutubeDLResponse =
                YoutubeDL.getInstance().execute(request, taskKey, null)
            jsonFormat.decodeFromString(response.out)
        }

    @CheckResult
    fun fetchVideoInfoFromUrl(
        url: String,
        playlistIndex: Int? = null,
        taskKey: String? = null,
        preferences: DownloadPreferences = DownloadPreferences.createFromPreferences(),
    ): Result<VideoInfo> {
        with(preferences) {
            val request =
                YoutubeDLRequest(url).apply {
                    addOption("-o", BASENAME)
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                    if (extractAudio) {
                        addOption("-x")
                    }
                    applyFormatSorter(this@with, toFormatSorter())
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                    if (proxy) {
                        enableProxy(proxyUrl)
                    }
                    if (forceIpv4) {
                        addOption("-4")
                    }
                    if (noCheckCertificate) {
                        addOption("--no-check-certificate")
                    }
                    /*            if (debug) {
                        addOption("-v")
                    }*/
                    if (autoSubtitle) {
                        addOption("--write-auto-subs")
                    }
                    
                    // No player_skip or player_client to get all format types
                    // Format ID consistency will be maintained through caching within the same session
                    if (autoSubtitle && !autoTranslatedSubtitles) {
                        addOption("--extractor-args", "youtube:skip=translated_subs")
                    }
                    
                    if (playlistIndex != null) {
                        addOption("--playlist-items", playlistIndex)
                        addOption("--dump-json")
                    } else {
                        addOption("--dump-single-json")
                        addOption("--no-playlist")
                    }
                    // See getPlaylistOrVideoInfo: 1 retry + 5s timeout caused spurious
                    // "fetch info" failures on slow sites. Use more tolerant values.
                    addOption("-R", "3")
                    addOption("--socket-timeout", "15")
                }
            return getVideoInfo(request, taskKey)
        }
    }

    @Serializable
    data class DownloadPreferences(
        val extractAudio: Boolean,
        val createThumbnail: Boolean,
        val downloadPlaylist: Boolean,
        val subdirectoryExtractor: Boolean,
        val subdirectoryPlaylistTitle: Boolean,
        val commandDirectory: String,
        val downloadSubtitle: Boolean,
        val embedSubtitle: Boolean,
        val keepSubtitle: Boolean,
        val subtitleLanguage: String,
        val autoSubtitle: Boolean,
        val autoTranslatedSubtitles: Boolean,
        val convertSubtitle: Int,
        val concurrentFragments: Int,
        val sponsorBlock: Boolean,
        val sponsorBlockCategory: String,
        val cookies: Boolean,
        val aria2c: Boolean,
        val useCustomAudioPreset: Boolean,
        val audioFormat: Int,
        val audioQuality: Int,
        val convertAudio: Boolean,
        val formatSorting: Boolean,
        val sortingFields: String,
        val audioConvertFormat: Int,
        val videoFormat: Int,
        val formatIdString: String,
        val videoResolution: Int,
        val privateMode: Boolean,
        val rateLimit: Boolean,
        val maxDownloadRate: String,
        val privateDirectory: Boolean,
        val cropArtwork: Boolean,
        val sdcard: Boolean,
        val sdcardUri: String,
        val embedThumbnail: Boolean,
        val videoClips: List<VideoClip>,
        val splitByChapter: Boolean,
        val debug: Boolean,
        val proxy: Boolean,
        val proxyUrl: String,
        val newTitle: String,
        val userAgentString: String,
        val outputTemplate: String,
        val useDownloadArchive: Boolean,
        val embedMetadata: Boolean,
        val restrictFilenames: Boolean,
        val supportAv1HardwareDecoding: Boolean,
        val forceIpv4: Boolean,
        val noCheckCertificate: Boolean,
        val mergeAudioStream: Boolean,
        val mergeToMkv: Boolean,
        val downloadDocs: Boolean = false,
    ) {
        companion object {
            val EMPTY =
                DownloadPreferences(
                    extractAudio = false,
                    createThumbnail = false,
                    downloadPlaylist = false,
                    subdirectoryExtractor = false,
                    subdirectoryPlaylistTitle = false,
                    commandDirectory = "",
                    downloadSubtitle = false,
                    embedSubtitle = false,
                    keepSubtitle = false,
                    subtitleLanguage = "",
                    autoSubtitle = false,
                    autoTranslatedSubtitles = false,
                    convertSubtitle = 0,
                    concurrentFragments = 0,
                    sponsorBlock = false,
                    sponsorBlockCategory = "",
                    cookies = false,
                    aria2c = false,
                    audioFormat = 0,
                    audioQuality = 0,
                    convertAudio = false,
                    formatSorting = false,
                    sortingFields = "",
                    audioConvertFormat = 0,
                    videoFormat = 0,
                    formatIdString = "",
                    videoResolution = 0,
                    privateMode = false,
                    rateLimit = false,
                    maxDownloadRate = "",
                    privateDirectory = false,
                    cropArtwork = false,
                    sdcard = false,
                    sdcardUri = "",
                    embedThumbnail = false,
                    videoClips = emptyList(),
                    splitByChapter = false,
                    debug = false,
                    proxy = false,
                    proxyUrl = "",
                    newTitle = "",
                    userAgentString = "",
                    outputTemplate = "",
                    useDownloadArchive = false,
                    embedMetadata = false,
                    restrictFilenames = false,
                    supportAv1HardwareDecoding = false,
                    forceIpv4 = false,
                    noCheckCertificate = false,
                    mergeAudioStream = false,
                    mergeToMkv = false,
                    useCustomAudioPreset = false,
                    downloadDocs = false,
                )

            fun createFromPreferences(): DownloadPreferences {
                val downloadSubtitle = SUBTITLE.getBoolean()
                val embedSubtitle = EMBED_SUBTITLE.getBoolean()
                return DownloadPreferences(
                    extractAudio = EXTRACT_AUDIO.getBoolean(),
                    createThumbnail = THUMBNAIL.getBoolean(),
                    downloadPlaylist = PLAYLIST.getBoolean(),
                    subdirectoryExtractor = SUBDIRECTORY_EXTRACTOR.getBoolean(),
                    subdirectoryPlaylistTitle = SUBDIRECTORY_PLAYLIST_TITLE.getBoolean(),
                    commandDirectory = COMMAND_DIRECTORY.getString(),
                    downloadSubtitle = downloadSubtitle,
                    embedSubtitle = embedSubtitle,
                    keepSubtitle = KEEP_SUBTITLE_FILES.getBoolean(),
                    subtitleLanguage = SUBTITLE_LANGUAGE.getString(),
                    autoSubtitle = AUTO_SUBTITLE.getBoolean(),
                    autoTranslatedSubtitles = AUTO_TRANSLATED_SUBTITLES.getBoolean(),
                    convertSubtitle = CONVERT_SUBTITLE.getInt(),
                    concurrentFragments = CONCURRENT.getInt(),
                    sponsorBlock = SPONSORBLOCK.getBoolean(),
                    sponsorBlockCategory = PreferenceUtil.getSponsorBlockCategories(),
                    cookies = COOKIES.getBoolean(),
                    aria2c = ARIA2C.getBoolean(),
                    useCustomAudioPreset = USE_CUSTOM_AUDIO_PRESET.getBoolean(),
                    audioFormat = AUDIO_FORMAT.getInt(),
                    audioQuality = AUDIO_QUALITY.getInt(),
                    convertAudio = AUDIO_CONVERT.getBoolean(),
                    formatSorting = FORMAT_SORTING.getBoolean(),
                    sortingFields = SORTING_FIELDS.getString(),
                    audioConvertFormat = PreferenceUtil.getAudioConvertFormat(),
                    videoFormat = PreferenceUtil.getVideoFormat(),
                    formatIdString = "",
                    videoResolution = PreferenceUtil.getVideoResolution(),
                    privateMode = PRIVATE_MODE.getBoolean(),
                    rateLimit = RATE_LIMIT.getBoolean(),
                    maxDownloadRate = PreferenceUtil.getMaxDownloadRate(),
                    privateDirectory = PRIVATE_DIRECTORY.getBoolean(),
                    cropArtwork = CROP_ARTWORK.getBoolean(),
                    sdcard = SDCARD_DOWNLOAD.getBoolean(),
                    sdcardUri = SDCARD_URI.getString(),
                    embedThumbnail = EMBED_THUMBNAIL.getBoolean(),
                    videoClips = emptyList(),
                    splitByChapter = false,
                    debug = DEBUG.getBoolean(),
                    proxy = ProxyManager.isProxyActive() || PROXY.getBoolean(),
                    proxyUrl = if (ProxyManager.isProxyActive()) {
                        val proxyAddress = ProxyManager.loadProxyConfig().getProxyAddress()
                        if (BuildConfig.DEBUG) Log.d("DownloadUtil", "Using new proxy system (address redacted in release)")
                        proxyAddress
                    } else if (PROXY.getBoolean()) {
                        val legacyProxy = PROXY_URL.getString()
                        if (BuildConfig.DEBUG) Log.d("DownloadUtil", "Using legacy proxy (address redacted in release)")
                        legacyProxy
                    } else {
                        ""
                    },
                    newTitle = "",
                    userAgentString =
                        USER_AGENT_STRING.run { if (USER_AGENT.getBoolean()) getString() else "" },
                    outputTemplate = OUTPUT_TEMPLATE.getString(),
                    useDownloadArchive = DOWNLOAD_ARCHIVE.getBoolean(),
                    embedMetadata = EMBED_METADATA.getBoolean(),
                    restrictFilenames = RESTRICT_FILENAMES.getBoolean(),
                    supportAv1HardwareDecoding = checkIfAv1HardwareAccelerated(),
                    forceIpv4 = FORCE_IPV4.getBoolean(),
                    noCheckCertificate = NO_CHECK_CERTIFICATE.getBoolean() && !ProxyManager.isProxyActive(),
                    mergeAudioStream = false,
                    mergeToMkv =
                        (downloadSubtitle && embedSubtitle) || MERGE_OUTPUT_MKV.getBoolean(),
                )
            }
        }
    }

    private fun YoutubeDLRequest.enableCookies(userAgentString: String): YoutubeDLRequest {
        refreshCookiesFile()
        return this.addOption("--cookies", context.getCookiesFile().absolutePath).apply {
            if (userAgentString.isNotEmpty()) {
                addOption("--add-header", "User-Agent:$userAgentString")
            }
        }
    }

    /**
     * Rebuilds the on-disk Netscape cookie file from the current in-memory WebView cookie store.
     * Called automatically before every download when cookies are enabled.
     */
    fun refreshCookiesFile() {
        context.getCookiesFile().let { cookiesFile ->
            getCookieListFromDatabase()
                .mapCatching { it.toCookiesFileContent() }
                // Use mapCatching (not onSuccess) so an IOException from writeText
                // (e.g. disk full) is captured inside the Result and handled by the
                // onFailure below, rather than propagating as an uncaught exception
                // and leaving a partial/corrupt cookies.txt on disk.
                .mapCatching { content -> FileUtil.writeContentToFile(content, cookiesFile) }
                .onFailure { err ->
                    Log.w(TAG, "Failed to refresh cookies file: ${err.message}")
                    if (cookiesFile.exists()) cookiesFile.delete()
                }
        }
    }

    private fun YoutubeDLRequest.enableProxy(proxyUrl: String): YoutubeDLRequest =
        this.addOption("--proxy", proxyUrl)

    private fun YoutubeDLRequest.useDownloadArchive(): YoutubeDLRequest =
        this.addOption("--download-archive", context.getArchiveFile().absolutePath)

    /**
     * Reads cookies for every saved [CookieProfile] URL using the documented
     * [android.webkit.CookieManager] public API.
     *
     * ## Why this replaced the old SQLite approach
     * The old implementation opened the Chromium WebView cookie database file directly with
     * [android.database.sqlite.SQLiteDatabase]. That was fragile in three ways:
     *  1. The WebView process may hold a write lock on the file → SQLITE_BUSY / SQLITE_LOCKED
     *     errors returned no cookies at all.
     *  2. The internal DB path (`app_webview/Default/Cookies` vs `app_webview/Cookies`) varies
     *     across Android / WebView versions and required brittle path detection.
     *  3. The Chromium cookie table schema is an implementation detail that can change without
     *     notice.
     *
     * [CookieManager.getCookie] goes through the same process that owns the cookie store, so
     * there is no locking conflict, no internal paths, and no schema dependency.
     *
     * ## Limitations
     * [CookieManager.getCookie] returns only `name=value` pairs — expiry, Secure, HttpOnly, and
     * Path attributes are not accessible via this API. yt-dlp needs only name and value to
     * authenticate requests, so this is not a practical limitation.
     */
    @CheckResult
    fun getCookieListFromDatabase(): Result<List<Cookie>> = runCatching {
        val manager = CookieManager.getInstance()
        // Flush in-memory cookies to the WebView's persistent store before reading.
        manager.flush()

        // Fetch all saved profile URLs from Room first — we need to know whether any
        // profile uses manual cookies before deciding if hasCookies() matters.
        val profiles = runBlocking(Dispatchers.IO) {
            DatabaseUtil.getCookieProfileList()
        }

        if (profiles.isEmpty()) {
            throw Exception(
                "No cookie profiles configured. " +
                "Please go to Settings → Network → Cookies and add the site you want to download from."
            )
        }

        // Only require WebView cookies when NO profile has manually-pasted content.
        // If at least one profile has manual content, skip this check — those cookies
        // are read from the profile's content field, not from CookieManager.
        if (profiles.none { it.content.isNotEmpty() } && !manager.hasCookies()) {
            throw Exception(
                "No cookies found in the browser. " +
                "Please open Settings → Network → Cookies, tap a profile, " +
                "then tap 'Generate cookies' and log in to the website."
            )
        }

        val seen = HashSet<String>() // deduplication key: "domain|name"
        val cookies = mutableListOf<Cookie>()

        for (profile in profiles) {

            // ------------------------------------------------------------------
            // Manual cookie path: the user pasted cookies directly into the
            // profile. Parse them and skip the CookieManager lookup entirely.
            // ------------------------------------------------------------------
            if (profile.content.isNotEmpty()) {
                val manual = parseCookieContent(profile.url, profile.content)
                if (manual.isNotEmpty()) {
                    manual.forEach { c ->
                        val key = "${c.domain}|${c.name}"
                        if (seen.add(key)) cookies.add(c)
                    }
                    Log.d(TAG, "Profile '${profile.url}': using ${manual.size} manual cookie(s)")
                    continue // skip CookieManager for this profile
                }
                Log.w(TAG, "Profile '${profile.url}': content is set but parsing returned 0 cookies")
            }

            // ------------------------------------------------------------------
            // Automatic path: read cookies from the in-app WebView's CookieManager.
            // ------------------------------------------------------------------
            // Normalise to HTTPS. CookieManager.getCookie() with an http:// URL silently
            // omits all Secure-flagged cookies. Facebook session cookies (c_user, xs),
            // Instagram session cookies (sessionid, csrftoken), and virtually every other
            // social-media auth cookie carry the Secure flag, so an http:// query returns
            // an empty or incomplete cookie string — causing silent auth failures.
            val rawUrl = when {
                profile.url.startsWith("https://") -> profile.url
                profile.url.startsWith("http://")  -> profile.url.replaceFirst("http://", "https://")
                else                               -> "https://${profile.url}"
            }
            val host = Uri.parse(rawUrl).host ?: continue

            // Query both the plain and www-prefixed variants because some sites set their
            // session cookies on "facebook.com" while others use "www.facebook.com".
            val urlVariants = buildList {
                add(rawUrl)
                if (host.startsWith("www.")) {
                    add(rawUrl.replaceFirst("://www.", "://"))
                } else {
                    add(rawUrl.replaceFirst("://", "://www."))
                }
            }

            // Use the registered (base) domain rather than the exact queried hostname.
            // Example: querying "https://www.facebook.com" gives host "www.facebook.com".
            // Facebook sets cookies on ".facebook.com", so writing ".www.facebook.com" in
            // the Netscape file means yt-dlp would NOT send those cookies to subdomains like
            // graph.facebook.com, video.facebook.com, etc., breaking authenticated downloads.
            // Stripping the leading "www." gives ".facebook.com" which covers all subdomains.
            val baseDomain = "." + if (host.startsWith("www.")) host.removePrefix("www.") else host

            for (url in urlVariants) {
                // getCookie returns "name1=val1; name2=val2; ..." or null if nothing is set.
                val raw = manager.getCookie(url) ?: continue
                raw.split(";").forEach rawCookie@{ pair ->
                    val eqIdx = pair.indexOf('=')
                    if (eqIdx < 0) return@rawCookie
                    val name  = pair.substring(0, eqIdx).trim()
                    // Do NOT trim value — some cookies intentionally have leading/trailing
                    // spaces in their value, and we must preserve those exactly.
                    val value = pair.substring(eqIdx + 1)
                    if (name.isEmpty()) return@rawCookie
                    val dedupKey = "$baseDomain|$name"
                    if (seen.add(dedupKey)) {
                        cookies.add(
                            Cookie(
                                domain = baseDomain,
                                name   = name,
                                value  = value,
                                includeSubdomains = true,
                                path   = "/",
                                // CookieManager does not expose Secure, HttpOnly, or expiry —
                                // yt-dlp does not require these for authentication.
                                secure    = false,
                                expiry    = 0L,
                                isHttpOnly = false,
                            )
                        )
                    }
                }
            }
        }

        if (cookies.isEmpty()) {
            throw Exception(
                "No cookies could be retrieved for the saved profiles. " +
                "Please open the cookie browser and log in to each site again."
            )
        }

        Log.d(TAG, "getCookieListFromDatabase: extracted ${cookies.size} cookies for ${cookies.distinctBy { it.domain }.size} domain(s)")
        cookies
    }

    fun List<Cookie>.toCookiesFileContent(): String =
        this.fold(StringBuilder(COOKIE_HEADER)) { acc, cookie ->
                acc.append(cookie.toNetscapeCookieString()).append("\n")
            }
            .toString()

    /** Convenience wrapper: reads cookies and serialises them to Netscape file format. */
    fun getCookiesContentFromDatabase(): Result<String> =
        getCookieListFromDatabase().mapCatching { it.toCookiesFileContent() }

    private fun YoutubeDLRequest.enableAria2c(): YoutubeDLRequest {
        // FIX: addOption() builds a raw argv array — no shell quoting involved.
        // The old value  aria2c:"-x 8 ..."  passes literal " characters to yt-dlp.
        // Python's shlex.split() inside yt-dlp treats the whole quoted block as ONE
        // argument, so aria2c received a single malformed string, ignored all flags,
        // and fell back to single-connection mode → no speed boost + progress = -1.
        // Correct form: no quotes, values separated by spaces, each flag is its own token.
        //
        // PROTOCOL SCOPING: aria2c excels at contiguous HTTP(S)/FTP downloads but adds
        // little for fragmented DASH/HLS (e.g. YouTube high-res, live streams), where
        // yt-dlp's native --concurrent-fragments is more reliable. We therefore restrict
        // aria2c to direct protocols only and let native handle fragmented streams, so
        // both paths can run their respective parallelism (see the call site).
        //
        // aria2c args:
        // -x N  = max N connections per server (user-controlled via ARIA2C_CONNECTIONS)
        // -s N  = split download into N parallel streams
        // -k 1M = minimum chunk size 1 MB (allows aggressive splitting; aria2 default 20M)
        // --file-allocation=none   = no preallocation (faster on Android / SAF temp dirs)
        // --max-tries / --retry-wait = resilience against transient network errors
        // --console-log-level=warn = keep logs clean without breaking progress parsing
        val connections = ARIA2C_CONNECTIONS.getInt()
        return this.addOption("--downloader", "http,https,ftp,ftps:libaria2c.so")
            .addOption(
                "--external-downloader-args",
                "aria2c:-x $connections -s $connections -k 1M " +
                    "--file-allocation=none --max-tries=5 --retry-wait=2 --console-log-level=warn",
            )
    }

    /**
     * Resilience options applied to actual downloads so a single transient failure
     * (network blip, slow extractor, busy storage, rate-limit) retries instead of
     * aborting. Reliable downloads across the many sites yt-dlp supports.
     *
     * NOTE: the retry COUNTS below match yt-dlp's current defaults (retries=10,
     * fragment-retries=10, file-access-retries=3, extractor-retries=3). They are set
     * explicitly so behaviour is stable even if upstream defaults change.
     *
     * The real value-add is the BACK-OFF: by default yt-dlp sleeps 0s between retries,
     * which hammers a struggling/rate-limiting server (HTTP 429) and often escalates
     * into a temporary ban. Exponential back-off (1s, 2s, 4s … capped) lets the server
     * recover and dramatically improves success on rate-limited / flaky sites.
     *
     * Intentionally NOT applied to info-probe requests, which use their own tolerant
     * but faster policy (see getPlaylistOrVideoInfo / fetchVideoInfoFromUrl).
     */
    private fun YoutubeDLRequest.enableRetryOptions(): YoutubeDLRequest =
        this.addOption("--retries", "10")
            .addOption("--fragment-retries", "10")
            .addOption("--extractor-retries", "3")
            .addOption("--file-access-retries", "3")
            // HTTP request back-off: exponential starting at 1s, capped at 120s.
            .addOption("--retry-sleep", "exp=1:120")
            // Fragment back-off: exponential starting at 1s, capped at 60s.
            .addOption("--retry-sleep", "fragment:exp=1:60")

    private fun YoutubeDLRequest.addOptionsForVideoDownloads(
        downloadPreferences: DownloadPreferences
    ): YoutubeDLRequest =
        this.apply {
            downloadPreferences.run {
                addOption("--add-metadata")
                addOption("--no-embed-info-json")
                if (formatIdString.isNotEmpty()) {
                    addOption("-f", formatIdString)
                    if (mergeAudioStream) {
                        addOption("--audio-multistreams")
                    }
                    // When merging video+audio formats (e.g., 303+251), ensure MP4 output
                    // This handles high-quality downloads that need audio merged
                    if (!mergeToMkv && formatIdString.contains("+")) {
                        val formatParts = formatIdString.split("+")
                        if (formatParts.size >= 2) {
                            // Multiple formats means we're merging - ensure MP4 output
                            addOption("--remux-video", "mp4")
                            addOption("--merge-output-format", "mp4")
                        }
                    }
                } else {
                    applyFormatSorter(this, toFormatSorter())
                }
                
                // No player_skip - format IDs selected by user are passed explicitly via -f flag
                // This ensures correct format is downloaded regardless of client selection
                if (downloadSubtitle && autoSubtitle && !autoTranslatedSubtitles) {
                    addOption("--extractor-args", "youtube:skip=translated_subs")
                }
                
                if (downloadSubtitle) {
                    if (autoSubtitle) {
                        addOption("--write-auto-subs")
                    }
                    subtitleLanguage
                        .takeIf { it.isNotEmpty() }
                        ?.let { addOption("--sub-langs", it) }
                    if (embedSubtitle) {
                        addOption("--embed-subs")
                        if (keepSubtitle) {
                            addOption("--write-subs")
                        }
                    } else {
                        addOption("--write-subs")
                    }
                    when (convertSubtitle) {
                        CONVERT_ASS -> addOption("--convert-subs", "ass")
                        CONVERT_SRT -> addOption("--convert-subs", "srt")
                        CONVERT_VTT -> addOption("--convert-subs", "vtt")
                        CONVERT_LRC -> addOption("--convert-subs", "lrc")
                        else -> {}
                    }
                }
                if (mergeToMkv) {
                    addOption("--remux-video", "mkv")
                    addOption("--merge-output-format", "mkv")
                }
                if (embedThumbnail) {
                    addOption("--embed-thumbnail")
                }
                if (videoClips.isEmpty()) addOption("--embed-chapters")
            }
        }

    @CheckResult
    private fun DownloadPreferences.toAudioFormatSorter(): String =
        this.run {
            if (!useCustomAudioPreset) return@run ""
            val format =
                when (audioFormat) {
                    M4A -> "acodec:aac"
                    OPUS -> "acodec:opus"
                    else -> ""
                }
            val quality =
                when (audioQuality) {
                    HIGH -> "abr~192"
                    MEDIUM -> "abr~128"
                    LOW -> "abr~64"
                    else -> ""
                }
            return@run connectWithDelimiter(format, quality, delimiter = ",")
        }

    @CheckResult
    private fun DownloadPreferences.toVideoFormatSorter(): String =
        this.run {
            val format =
                when (videoFormat) {
                    FORMAT_COMPATIBILITY -> "proto,vcodec:h264,ext"
                    FORMAT_QUALITY ->
                        if (supportAv1HardwareDecoding) {
                            "vcodec:av01"
                        } else {
                            "vcodec:vp9.2"
                        }

                    else -> ""
                }
            val res =
                when (videoResolution) {
                    1 -> "res:2160"
                    2 -> "res:1440"
                    3 -> "res:1080"
                    4 -> "res:720"
                    5 -> "res:480"
                    6 -> "res:360"
                    7 -> "+res"
                    else -> ""
                }
            val sorter = if (videoFormat == FORMAT_COMPATIBILITY) {
                connectWithDelimiter(format, res, delimiter = ",")
            } else {
                connectWithDelimiter(res, format, delimiter = ",")
            }
            return@run sorter
        }

    private fun YoutubeDLRequest.applyFormatSorter(
        preferences: DownloadPreferences,
        sorter: String,
    ) =
        preferences.run {
            if (formatSorting && sortingFields.isNotEmpty()) addOption("-S", sortingFields)
            else if (sorter.isNotEmpty()) addOption("-S", sorter) else {}
        }

    @CheckResult
    fun DownloadPreferences.toFormatSorter(): String =
        connectWithDelimiter(
            this.toVideoFormatSorter(),
            this.toAudioFormatSorter(),
            delimiter = ",",
        )

    private fun YoutubeDLRequest.addOptionsForAudioDownloads(
        id: String,
        preferences: DownloadPreferences,
        playlistUrl: String,
    ): YoutubeDLRequest =
        this.apply {
            with(preferences) {
                addOption("-x")
                
                // No player_skip for audio - format ID explicitly passed
                if (downloadSubtitle && autoSubtitle && !autoTranslatedSubtitles) {
                    addOption("--extractor-args", "youtube:skip=translated_subs")
                }
                
                if (downloadSubtitle) {
                    addOption("--write-subs")

                    if (autoSubtitle) {
                        addOption("--write-auto-subs")
                    }
                    subtitleLanguage
                        .takeIf { it.isNotEmpty() }
                        ?.let { addOption("--sub-langs", it) }
                    when (convertSubtitle) {
                        CONVERT_ASS -> addOption("--convert-subs", "ass")
                        CONVERT_SRT -> addOption("--convert-subs", "srt")
                        CONVERT_VTT -> addOption("--convert-subs", "vtt")
                        CONVERT_LRC -> addOption("--convert-subs", "lrc")
                        else -> {}
                    }
                }
                if (formatIdString.isNotEmpty()) {
                    addOption("-f", formatIdString)
                    if (mergeAudioStream) {
                        addOption("--audio-multistreams")
                    }
                } else if (convertAudio) {
                    when (audioConvertFormat) {
                        CONVERT_MP3 -> {
                            addOption("--audio-format", "mp3")
                        }

                        CONVERT_M4A -> {
                            addOption("--audio-format", "m4a")
                        }
                    }
                } else {
                    applyFormatSorter(preferences, toAudioFormatSorter())
                }

                if (embedMetadata) {
                    addOption("--embed-metadata")
                    addOption("--embed-thumbnail")
                    addOption("--convert-thumbnails", "jpg")

                    if (cropArtwork) {
                        val configFile = context.getConfigFile(id)
                        FileUtil.writeContentToFile(CROP_ARTWORK_COMMAND, configFile)
                        addOption("--config", configFile.absolutePath)
                    }
                }
                addOption("--parse-metadata", "%(release_year,upload_date)s:%(meta_date)s")

                if (playlistUrl.isNotEmpty()) {
                    addOption("--parse-metadata", "%(album,playlist,title)s:%(meta_album)s")
                    addOption("--parse-metadata", "%(track_number,playlist_index)d:%(meta_track)s")
                } else {
                    addOption("--parse-metadata", "%(album,title)s:%(meta_album)s")
                }
            }
        }

    @CheckResult
    fun writeDocsTextFile(videoInfo: VideoInfo): Result<String> = runCatching {
        val title = videoInfo.title.ifBlank { "video" }
        val safeTitle = title.replace(Regex("""[<>:"/\\|?*]"""), "_")
            .take(100)
        val docsDir = FileUtil.getDocsDirectory()
        val file = File(docsDir, "$safeTitle - Info.txt")
        val content = buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("  VIDEO INFORMATION")
            appendLine("═══════════════════════════════════════")
            appendLine()
            appendLine("Title:")
            appendLine(videoInfo.title)
            appendLine()
            videoInfo.uploader?.let {
                appendLine("Uploader:")
                appendLine(it)
                appendLine()
            }
            videoInfo.uploadDate?.let {
                appendLine("Upload Date:")
                appendLine(it)
                appendLine()
            }
            videoInfo.duration?.let { sec ->
                val minutes = (sec / 60).toInt()
                val seconds = (sec % 60).toInt()
                appendLine("Duration:")
                appendLine("${minutes}m ${seconds}s")
                appendLine()
            }
            videoInfo.webpageUrl?.let {
                appendLine("URL:")
                appendLine(it)
                appendLine()
            }
            val tags = videoInfo.tags
            if (!tags.isNullOrEmpty()) {
                appendLine("Tags:")
                tags.forEach { tag -> appendLine("  • $tag") }
                appendLine()
            }
            val desc = videoInfo.description
            if (!desc.isNullOrBlank()) {
                appendLine("Description:")
                appendLine(desc)
                appendLine()
            }
            appendLine("═══════════════════════════════════════")
            appendLine("  Generated by Seal Plus")
            appendLine("═══════════════════════════════════════")
        }
        FileUtil.writeContentToFile(content, file)
        context.makeToast(R.string.docs_saved)
        file.absolutePath
    }

    private fun insertInfoIntoDownloadHistory(
        videoInfo: VideoInfo,
        filePaths: List<String>,
        downloadTimeMillis: Long = -1L,
        averageSpeedBytesPerSec: Long = -1L,
    ): List<String> =
        filePaths.onEach {
            DatabaseUtil.insertInfo(
                videoInfo.toDownloadedVideoInfo(
                    videoPath = it,
                    downloadTimeMillis = downloadTimeMillis,
                    averageSpeedBytesPerSec = averageSpeedBytesPerSec,
                )
            )
        }

    private fun VideoInfo.toDownloadedVideoInfo(
        id: Int = 0,
        videoPath: String,
        downloadTimeMillis: Long = -1L,
        averageSpeedBytesPerSec: Long = -1L,
    ): DownloadedVideoInfo =
        this.run {
            DownloadedVideoInfo(
                id = id,
                videoTitle = title,
                videoAuthor = uploader ?: channel ?: uploaderId.toString(),
                videoUrl = webpageUrl ?: originalUrl ?: "",
                thumbnailUrl = thumbnail.toHttpsUrl(),
                videoPath = videoPath,
                videoId = this.id,
                extractor = extractorKey,
                downloadTimeMillis = downloadTimeMillis,
                averageSpeedBytesPerSec = averageSpeedBytesPerSec,
            )
        }

    private fun insertSplitChapterIntoHistory(
        videoInfo: VideoInfo,
        filePaths: List<String>,
        downloadTimeMillis: Long = -1L,
        averageSpeedBytesPerSec: Long = -1L,
    ) =
        filePaths.onEach {
            DatabaseUtil.insertInfo(
                videoInfo
                    .toDownloadedVideoInfo(
                        videoPath = it,
                        downloadTimeMillis = downloadTimeMillis,
                        averageSpeedBytesPerSec = averageSpeedBytesPerSec,
                    )
                    .copy(videoTitle = it.getFileName())
            )
        }

    private fun computeAvgSpeed(videoInfo: VideoInfo, timing: LongArray): Long {
        val elapsed = if (timing[0] > 0L) timing[1] - timing[0] else -1L
        if (elapsed <= 0L) return -1L
        val fileSize = ((videoInfo.fileSize ?: videoInfo.fileSizeApprox) ?: 0.0).toLong()
        return if (fileSize > 0L) fileSize * 1000L / elapsed else -1L
    }

    @CheckResult
    fun downloadVideo(
        videoInfo: VideoInfo? = null,
        playlistUrl: String = "",
        playlistItem: Int = 0,
        taskId: String,
        downloadPreferences: DownloadPreferences,
        progressCallback: ((Float, Long, String) -> Unit)?,
    ): Result<List<String>> {
        if (videoInfo == null)
            return Result.failure(Throwable(context.getString(R.string.fetch_info_error_msg)))

        with(downloadPreferences) {
            val url =
                playlistUrl.ifEmpty {
                    videoInfo.originalUrl
                        ?: videoInfo.webpageUrl
                        ?: return Result.failure(
                            Throwable(context.getString(R.string.fetch_info_error_msg))
                        )
                }
            val request = YoutubeDLRequest(url)
            val pathBuilder = StringBuilder()
            val outputBuilder = StringBuilder()
            // Index 0 = start time ms, index 1 = end time ms
            val downloadTiming = LongArray(2)

            request
                .apply {
                    addOption("--no-mtime")
                    addOption("--continue")
                    enableRetryOptions()
                    //                addOption("-v")
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                    if (proxy) {
                        enableProxy(proxyUrl)
                    }
                    if (forceIpv4) {
                        addOption("-4")
                    }
                    if (noCheckCertificate) {
                        addOption("--no-check-certificate")
                    }
                    if (debug) {
                        addOption("-v")
                    }
                    if (useDownloadArchive) {
                        val archiveFile = context.getArchiveFile()
                        val archiveTarget = "${videoInfo.extractor} ${videoInfo.id}"
                        val alreadyDownloaded = archiveFile.exists() &&
                            archiveFile.bufferedReader().useLines { lines ->
                                lines.any { it.trimEnd() == archiveTarget }
                            }
                        if (alreadyDownloaded) {
                            return Result.failure(
                                YoutubeDLException(
                                    context.getString(R.string.download_archive_error)
                                )
                            )
                        } else {
                            useDownloadArchive()
                        }
                    }

                    if (rateLimit && maxDownloadRate.isNumberInRange(1, 1000000)) {
                        addOption("-r", "${maxDownloadRate}K")
                    }

                    if (playlistItem != 0 && downloadPlaylist) {
                        addOption("--playlist-items", playlistItem)
                        if (subdirectoryPlaylistTitle && !videoInfo.playlist.isNullOrEmpty()) {
                            outputBuilder.append(PLAYLIST_TITLE_SUBDIRECTORY_PREFIX)
                        }
                        //                    addOption("--compat-options",
                        // "no-youtube-unavailable-videos")
                    } else {
                        addOption("--no-playlist")
                    }

                    // aria2c is scoped to contiguous protocols (see enableAria2c), so
                    // concurrent fragments can run alongside it for DASH/HLS streams.
                    if (aria2c) {
                        enableAria2c()
                        if (concurrentFragments > 1) {
                            addOption("--concurrent-fragments", concurrentFragments)
                        }
                    } else if (concurrentFragments > 1) {
                        addOption("--concurrent-fragments", concurrentFragments)
                    }

                    if (extractAudio || (videoInfo.vcodec == "none")) {
                        if (privateDirectory) pathBuilder.append(App.privateDownloadDir)
                        else pathBuilder.append(audioDownloadDir)
                        addOptionsForAudioDownloads(
                            id = videoInfo.id,
                            preferences = downloadPreferences,
                            playlistUrl = playlistUrl,
                        )
                    } else {
                        if (privateDirectory) pathBuilder.append(App.privateDownloadDir)
                        else pathBuilder.append(videoDownloadDir)
                        addOptionsForVideoDownloads(downloadPreferences)
                    }
                    if (sponsorBlock) {
                        addOption("--sponsorblock-remove", sponsorBlockCategory)
                    }

                    if (createThumbnail) {
                        addOption("--write-thumbnail")
                        addOption("--convert-thumbnails", "png")
                    }
                    if (subdirectoryExtractor) {
                        pathBuilder.append("/${videoInfo.extractorKey}")
                    }

                    if (sdcard) {
                        addOption("-P", context.getSdcardTempDir(videoInfo.id).absolutePath)
                    } else {
                        addOption("-P", pathBuilder.toString())
                    }

                    videoClips.forEach {
                        addOption(
                            "--download-sections",
                            "*%d-%d".format(locale = Locale.US, it.start, it.end),
                        )
                    }
                    if (newTitle.isNotEmpty()) {
                        addCommands(listOf("--replace-in-metadata", "title", ".+", newTitle))
                    }
                    if (Build.VERSION.SDK_INT > 23 && !sdcard) {
                        addOption("-P", "temp:" + getExternalTempDir(videoInfo.id))
                    }

                    if (splitByChapter) {
                        addOption("-o", OUTPUT_TEMPLATE_CHAPTERS)
                        addOption("--split-chapters")
                    }

                    val output =
                        if (splitByChapter) {
                            OUTPUT_TEMPLATE_SPLIT
                        } else if (videoClips.isEmpty()) {
                            outputTemplate
                        } else {
                            OUTPUT_TEMPLATE_CLIPS
                        }

                    addOption("-o", outputBuilder.append(output).toString())

                    for (s in request.buildCommand()) Log.d(TAG, s)
                }
                .runCatching {
                    val dlStartTime = System.currentTimeMillis()
                    YoutubeDL.getInstance()
                        .execute(request = this, processId = taskId, callback = progressCallback)
                        .also { downloadTiming[0] = dlStartTime; downloadTiming[1] = System.currentTimeMillis() }
                }
                .onFailure { th ->
                    return if (
                        sponsorBlock &&
                            th.message?.contains("Unable to communicate with SponsorBlock API") ==
                                true
                    ) {
                        th.printStackTrace()
                        onFinishDownloading(
                            preferences = this,
                            videoInfo = videoInfo,
                            downloadPath = pathBuilder.toString(),
                            sdcardUri = sdcardUri,
                            downloadTimeMillis = if (downloadTiming[0] > 0L) downloadTiming[1] - downloadTiming[0] else -1L,
                            averageSpeedBytesPerSec = computeAvgSpeed(videoInfo, downloadTiming),
                        )
                    } else Result.failure(th)
                }
            return onFinishDownloading(
                preferences = this,
                videoInfo = videoInfo,
                downloadPath = pathBuilder.toString(),
                sdcardUri = sdcardUri,
                downloadTimeMillis = if (downloadTiming[0] > 0L) downloadTiming[1] - downloadTiming[0] else -1L,
                averageSpeedBytesPerSec = computeAvgSpeed(videoInfo, downloadTiming),
            )
        }
    }

    private fun onFinishDownloading(
        preferences: DownloadPreferences,
        videoInfo: VideoInfo,
        downloadPath: String,
        sdcardUri: String,
        downloadTimeMillis: Long = -1L,
        averageSpeedBytesPerSec: Long = -1L,
    ): Result<List<String>> =
        preferences.run {
            val fileName =
                preferences.newTitle.ifEmpty {
                    videoInfo.filename
                        ?: videoInfo.requestedDownloads?.firstOrNull()?.filename
                        ?: videoInfo.title
                }

            Log.d(TAG, "onFinishDownloading: $fileName")
            if (sdcard) {
                moveFilesToSdcard(
                        sdcardUri = sdcardUri,
                        tempPath = context.getSdcardTempDir(videoInfo.id),
                    )
                    .onSuccess {
                        if (privateMode) {
                            return Result.success(emptyList())
                        } else if (splitByChapter) {
                            insertSplitChapterIntoHistory(
                                videoInfo, it,
                                downloadTimeMillis = downloadTimeMillis,
                                averageSpeedBytesPerSec = averageSpeedBytesPerSec,
                            )
                        } else {
                            insertInfoIntoDownloadHistory(
                                videoInfo, it,
                                downloadTimeMillis = downloadTimeMillis,
                                averageSpeedBytesPerSec = averageSpeedBytesPerSec,
                            )
                        }
                    }
            } else {
                FileUtil.scanFileToMediaLibraryPostDownload(
                        title = fileName,
                        downloadDir = downloadPath,
                    )
                    .run {
                        if (privateMode) Result.success(emptyList())
                        else
                            Result.success(
                                if (splitByChapter) {
                                    insertSplitChapterIntoHistory(
                                        videoInfo, this,
                                        downloadTimeMillis = downloadTimeMillis,
                                        averageSpeedBytesPerSec = averageSpeedBytesPerSec,
                                    )
                                } else {
                                    insertInfoIntoDownloadHistory(
                                        videoInfo, this,
                                        downloadTimeMillis = downloadTimeMillis,
                                        averageSpeedBytesPerSec = averageSpeedBytesPerSec,
                                    )
                                }
                            )
                    }
            }
        }

    @CheckResult
    fun executeCustomCommandTask(
        urlString: String,
        taskId: String,
        template: CommandTemplate,
        preferences: DownloadPreferences,
        progressCallback: ((Float, Long, String) -> Unit),
    ): Result<YoutubeDLResponse> {
        val urlList = urlString.split(Regex("[\n ]")).filter { it.isNotBlank() }

        val request =
            with(preferences) {
                YoutubeDLRequest(urlList).apply {
                    commandDirectory.takeIf { it.isNotEmpty() }?.let { addOption("-P", it) }
                    addOption("--newline")
                    if (aria2c) {
                        enableAria2c()
                    }
                    if (useDownloadArchive) {
                        useDownloadArchive()
                    }
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                    addOption(
                        "--config-locations",
                        FileUtil.writeContentToFile(template.template, context.getConfigFile())
                            .absolutePath,
                    )
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                    if (noCheckCertificate) {
                        addOption("--no-check-certificate")
                    }
                }
            }

        return runCatching {
            YoutubeDL.getInstance()
                .execute(request = request, processId = taskId, callback = progressCallback)
        }
    }

    suspend fun executeCommandInBackground(
        url: String,
        template: CommandTemplate = PreferenceUtil.getTemplate(),
        downloadPreferences: DownloadPreferences = DownloadPreferences.createFromPreferences(),
    ) {
        downloadPreferences.run {
            val taskId = Downloader.makeKey(url = url, templateName = template.name)
            val notificationId = taskId.toNotificationId()
            val urlList = url.split(Regex("[\n ]")).filter { it.isNotBlank() }

            App.applicationScope.launch(Dispatchers.Main) {
                context.makeToast(R.string.start_execute)
            }
            val request =
                YoutubeDLRequest(urlList).apply {
                    commandDirectory.takeIf { it.isNotEmpty() }?.let { addOption("-P", it) }
                    addOption("--newline")
                    if (aria2c) {
                        enableAria2c()
                    }
                    if (useDownloadArchive) {
                        useDownloadArchive()
                    }
                    if (restrictFilenames) {
                        addOption("--restrict-filenames")
                    }
                    addOption(
                        "--config-locations",
                        FileUtil.writeContentToFile(template.template, context.getConfigFile())
                            .absolutePath,
                    )
                    if (cookies) {
                        enableCookies(userAgentString)
                    }
                    if (noCheckCertificate) {
                        addOption("--no-check-certificate")
                    }
                }

            onProcessStarted()
            withContext(Dispatchers.Main) { onTaskStarted(template, url) }
            runCatching {
                    val response =
                        YoutubeDL.getInstance().execute(request = request, processId = taskId) {
                            progress,
                            _,
                            text ->
                            NotificationUtil.makeNotificationForCustomCommand(
                                notificationId = notificationId,
                                taskId = taskId,
                                progress = progress.toInt(),
                                templateName = template.name,
                                taskUrl = url,
                                text = text,
                            )
                            Downloader.updateTaskOutput(
                                template = template,
                                url = url,
                                line = text,
                                progress = progress,
                            )
                        }
                    onTaskEnded(template, url, response.out + "\n" + response.err)
                }
                .onFailure {
                    it.printStackTrace()
                    if (it is YoutubeDL.CanceledException) return@onFailure
                    it.message.run {
                        if (isNullOrEmpty()) onTaskEnded(template, url)
                        else onTaskError(this, template, url)
                    }
                }
            onProcessEnded()
        }
    }

    private fun checkIfAv1HardwareAccelerated(): Boolean {
        if (PreferenceUtil.containsKey(AV1_HARDWARE_ACCELERATED)) {
            return AV1_HARDWARE_ACCELERATED.getBoolean()
        } else {
            val res =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    false
                } else {
                    MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                        info.supportedTypes.any { it.equals("video/av01", ignoreCase = true) } &&
                            info.isHardwareAccelerated
                    }
                }
            AV1_HARDWARE_ACCELERATED.updateBoolean(res)
            return res
        }
    }
}
