package com.junkfood.seal.download

import android.app.PendingIntent
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.PowerManager
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.download.Task.DownloadState
import com.junkfood.seal.download.Task.DownloadState.Canceled
import com.junkfood.seal.download.Task.DownloadState.Completed
import com.junkfood.seal.download.Task.DownloadState.Error
import com.junkfood.seal.download.Task.DownloadState.FetchingInfo
import com.junkfood.seal.download.Task.DownloadState.Idle
import com.junkfood.seal.download.Task.DownloadState.Paused
import com.junkfood.seal.download.Task.DownloadState.ReadyWithInfo
import com.junkfood.seal.download.Task.DownloadState.Running
import com.junkfood.seal.download.Task.RestartableAction.Download
import com.junkfood.seal.download.Task.RestartableAction.FetchInfo
import com.junkfood.seal.download.Task.TypeInfo
import com.junkfood.seal.download.Task.PauseReason
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.MAX_CONCURRENT_DOWNLOADS
import com.junkfood.seal.util.NotificationUtil
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.VideoInfo
import com.yausername.youtubedl_android.YoutubeDL
import kotlin.collections.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

private const val TAG = "DownloaderV2"

interface DownloaderV2 {
    fun getTaskStateMap(): SnapshotStateMap<Task, Task.State>

    fun cancel(task: Task): Boolean

    fun cancel(taskId: String): Boolean {
        return getTaskStateMap().keys.find { it.id == taskId }?.let { cancel(it) } ?: false
    }

    fun pause(task: Task): Boolean

    fun pause(taskId: String): Boolean {
        return getTaskStateMap().keys.find { it.id == taskId }?.let { pause(it) } ?: false
    }

    fun resume(task: Task): Boolean

    fun resume(taskId: String): Boolean {
        return getTaskStateMap().keys.find { it.id == taskId }?.let { resume(it) } ?: false
    }

    fun restart(task: Task)

    /** Enqueue a [Task] with an empty [Task.State] */
    fun enqueue(task: Task)

    fun enqueue(task: Task, state: Task.State)

    fun enqueue(taskWithState: TaskFactory.TaskWithState) {
        val (task, state) = taskWithState
        enqueue(task, state)
    }

    fun remove(task: Task): Boolean

    fun cleanup() {}
}

internal object FakeDownloaderV2 : DownloaderV2 {
    override fun getTaskStateMap(): SnapshotStateMap<Task, Task.State> {
        return mutableStateMapOf()
    }

    override fun cancel(task: Task): Boolean {
        return false
    }

    override fun pause(task: Task): Boolean {
        return false
    }

    override fun resume(task: Task): Boolean {
        return false
    }

    override fun restart(task: Task) {}

    override fun enqueue(task: Task) {}

    override fun enqueue(task: Task, state: Task.State) {}

    override fun remove(task: Task): Boolean {
        return true
    }
}

/**
 * TODO:
 *     - Notification
 *     - Custom commands
 *     - States for ViewModels
 */
@OptIn(FlowPreview::class)
class DownloaderV2Impl(private val appContext: Context) : DownloaderV2, KoinComponent {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val taskStateMap = mutableStateMapOf<Task, Task.State>()
    private val resumedProgressMap = java.util.concurrent.ConcurrentHashMap<String, Float>()
    // Tracks how many auto-retries have been attempted for each task (keyed by task ID).
    // Cleared on success or after MAX_AUTO_RETRIES exhausted.
    private val retryCountMap = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val waitingForNetwork = java.util.concurrent.ConcurrentHashMap<String, Task.RestartableAction>()
    @Volatile private var networkPauseJob: Job? = null
    @Volatile private var networkDegradedAtMs: Long = 0L

    // Held only while at least one task is Running/FetchingInfo. Without this, Doze mode can
    // suspend the CPU mid-download even while the foreground service notification is showing —
    // the yt-dlp process gets frozen (not killed) and the download stalls or eventually errors
    // out. Reopening the app wakes the CPU again, which is why "resume" appears to fix it: the
    // real fix is holding the CPU awake for the download's duration in the first place.
    private val wakeLock: PowerManager.WakeLock by lazy {
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SealPlus::DownloadWakeLock")
            .apply { setReferenceCounted(false) }
    }

    private fun acquireDownloadWakeLock() {
        runCatching {
            if (!wakeLock.isHeld) {
                // Safety timeout (2h) in case a task gets stuck in Running without transitioning
                // out — prevents an indefinitely-held wake lock from draining the battery.
                wakeLock.acquire(2 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseDownloadWakeLock() {
        runCatching {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    companion object {
        private const val MAX_AUTO_RETRIES = 3
        private const val RETRY_DELAY_MS = 5_000L
        private val NETWORK_ERROR_KEYWORDS = listOf(
            "Unable to connect", "Connection reset", "timed out", "connect timed out",
            "HTTP Error 5", "Network is unreachable", "nodename nor servname",
            "Failed to establish", "RemoteDisconnected", "SSLError"
        )

        // yt-dlp emits a progress line on essentially every stdout flush, which on a fast
        // connection can fire many times per second, with no throttling from this app or the
        // underlying youtubedl-android callback. Writing Compose state (which the Home screen's
        // active-download list observes) and posting a notification (a Binder IPC into
        // system_server) on every single tick can flood the main thread with recomposition/
        // layout/draw work and IPC churn. On some heavily-customized OEM ROMs this is enough to
        // starve touch-input dispatch, making the navigation drawer appear unclickable for as
        // long as a download keeps ticking (see: menu items unresponsive while downloading).
        // Capping both to a bounded cadence keeps the UI/notification smooth while freeing up
        // the main thread to service input in between updates.
        private const val PROGRESS_UI_UPDATE_THROTTLE_MS = 200L
        private const val PROGRESS_NOTIFICATION_THROTTLE_MS = 750L
    }
    private val snapshotFlow = snapshotFlow { taskStateMap.toMap() }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            clearNetworkDegraded()
            waitingForNetwork.clear()
            resumeNetworkPausedTasks()
            doYourWork()
        }

        override fun onLost(network: Network) {
            markNetworkDegraded()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            val validated =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (validated) {
                clearNetworkDegraded()
                waitingForNetwork.clear()
                resumeNetworkPausedTasks()
                doYourWork()
            } else {
                markNetworkDegraded()
            }
        }
    }

    init {
        // Re-trigger doYourWork() when suitable network becomes available
        // (e.g. WiFi reconnects after the restriction blocked queued tasks).
        App.connectivityManager.registerDefaultNetworkCallback(networkCallback)

        scope.launch(Dispatchers.Default) {
            // Only trigger doYourWork() when a state TYPE changes (not on every progress tick).
            // Progress updates change Running.progress/progressText every ~200ms — filtering those
            // out prevents hundreds of redundant doYourWork() calls per second.
            snapshotFlow
                .map { map -> map.mapValues { (_, state) -> state.downloadState::class } }
                .distinctUntilChanged()
                .onEach { doYourWork() }
                .map { it.values.count { cls -> cls == Running::class || cls == FetchingInfo::class } }
                .distinctUntilChanged()
                .collect { activeCount ->
                    if (activeCount > 0) {
                        App.startService()
                        acquireDownloadWakeLock()
                    } else {
                        App.stopService()
                        releaseDownloadWakeLock()
                    }
                }
        }

        scope.launch(Dispatchers.IO) {
            // don't write before we read
            enqueueFromBackup()

            // Only write backup when a structurally important state changes.
            // Strip volatile Running.progress/progressText before comparing so that the constant
            // stream of progress callbacks (~5/sec) does NOT trigger a full MMKV serialization.
            snapshotFlow
                .map { map ->
                    // Filter + strip here for change-detection only.
                    // We use the SAME filtered map as the backup payload (not re-reading
                    // taskStateMap later) to avoid a TOCTOU race where a task completes
                    // between the distinctUntilChanged trigger and the actual write —
                    // previously, a completed task could vanish from the live map before
                    // taskStateMap.toMap() ran in collect{}, causing it to be missing from
                    // the backup entirely and never restored after force-stop.
                    map
                        .filter { (_, state) -> state.downloadState !is Completed }
                        .mapValues { (_, state) ->
                            // Preserve real progress in the backup payload so paused-on-kill
                            // tasks restore with their last known progress value. Strip only
                            // for deduplication comparison (the copy below keeps real values).
                            state
                        }
                }
                .distinctUntilChanged { old, new ->
                    // Custom comparator: compare by structural state class (not progress) so
                    // 200ms progress ticks don't trigger a serialization write.
                    old.size == new.size && old.all { (task, oldState) ->
                        val newState = new[task] ?: return@distinctUntilChanged false
                        oldState.downloadState::class == newState.downloadState::class
                    }
                }
                .collect { snapshot ->
                    snapshot.forEach { (_, state) -> Log.d(TAG, state.viewState.title) }
                    PreferenceUtil.encodeTaskListBackup(snapshot)
                }
        }
    }

    private fun enqueueFromBackup() {
        val taskList =
            PreferenceUtil.decodeTaskListBackup()
                .filter { it.value.downloadState !is Completed }
                .mapValues { (_, state) ->
                    val preState = state.downloadState
                    val downloadState =
                        when (preState) {
                            is FetchingInfo -> {
                                // FetchingInfo had a live coroutine job that died with the
                                // process — the job reference is @Transient and not restored,
                                // so this state is invalid. Mark as Canceled so the user can
                                // tap Retry to re-fetch info.
                                Canceled(action = FetchInfo)
                            }
                            Idle -> {
                                // Idle = "queued, waiting for a download slot". It has NO
                                // associated coroutine job — it is pure data. Restoring it
                                // as Idle lets doYourWork() automatically pick it up and
                                // continue the queue without requiring any user interaction,
                                // matching the expected "reopen and continue" behavior.
                                // Previously, Idle was co-handled with FetchingInfo in the
                                // same branch and incorrectly converted to Canceled, which
                                // caused queued tasks to show as Failed after force-stop.
                                Idle
                            }
                            is Running -> {
                                // Running had a live download coroutine — the process died
                                // mid-download. Restore as Paused so the user can resume.
                                Paused(action = Download, progress = preState.progress)
                            }

                            ReadyWithInfo -> {
                                // Info was fetched but download had not started yet.
                                // Restore as Paused so user can resume.
                                Paused(action = Download, progress = null)
                            }
                            is Paused -> {
                                // Keep paused state on restart — user explicitly paused this.
                                preState
                            }
                            else -> {
                                // Error, Canceled, etc. — keep as-is.
                                preState
                            }
                        }
                    state.copy(downloadState = downloadState)
                }
        taskList.forEach(::enqueue)
    }

    private fun Map<Task, Task.State>.countRunning(): Int = count { (_, state) ->
        state.downloadState is Running || state.downloadState is FetchingInfo
    }

    override fun getTaskStateMap(): SnapshotStateMap<Task, Task.State> {
        return taskStateMap
    }

    override fun enqueue(task: Task) {
        taskStateMap +=
            task to Task.State(Idle, null, Task.ViewState(url = task.url, title = task.url))
    }

    override fun enqueue(task: Task, state: Task.State) {
        taskStateMap += task to state
    }

    /**
     * Noted the caller is responsible for stopping the [task] before removing it
     *
     * @return true if the task was removed
     */
    override fun remove(task: Task): Boolean {
        if (taskStateMap.contains(task)) {
            taskStateMap.remove(task)
            return true
        }
        return false
    }

    override fun cancel(task: Task): Boolean = task.cancelImpl()

    override fun pause(task: Task): Boolean = task.pauseImpl()

    override fun resume(task: Task): Boolean = task.resumeImpl()

    override fun restart(task: Task) {
        task.restartImpl()
    }

    override fun cleanup() {
        runCatching {
            App.connectivityManager.unregisterNetworkCallback(networkCallback)
        }
        releaseDownloadWakeLock()
        // Cancel the supervisor scope to stop all in-flight download coroutines and the
        // backup-write collector. Without this, the scope's SupervisorJob and its children
        // continue running after cleanup() is called (e.g. on low-memory conditions),
        // holding references to the Koin context and leaking resources indefinitely.
        scope.cancel()
    }

    // Safe state getter — returns null if the task has been removed from the map
    // (e.g. user tapped Delete on an active card while a download was in progress).
    // Previously used !! which would crash with NPE on concurrent removal.
    private val Task.stateOrNull: Task.State?
        get() = taskStateMap[this]

    private var Task.state: Task.State
        get() = taskStateMap[this] ?: Task.State(
            downloadState = DownloadState.Canceled(action = Task.RestartableAction.Download),
            videoInfo = null,
            viewState = Task.ViewState(url = url, title = url),
        )
        set(value) {
            // Only write back if the task is still in the map — avoids writing a ghost
            // state after the task has been removed by the user.
            if (taskStateMap.containsKey(this)) {
                taskStateMap[this] = value
            }
        }

    private var Task.downloadState: DownloadState
        get() = stateOrNull?.downloadState ?: DownloadState.Canceled(Task.RestartableAction.Download)
        set(value) {
            val prevState = stateOrNull ?: return  // task was removed — silently ignore
            taskStateMap[this] = prevState.copy(downloadState = value)
        }

    private var Task.info: VideoInfo?
        get() = stateOrNull?.videoInfo
        set(value) {
            val prevState = stateOrNull ?: return
            taskStateMap[this] = prevState.copy(videoInfo = value)
        }

    private var Task.viewState: Task.ViewState
        get() = stateOrNull?.viewState ?: Task.ViewState(url = url, title = url)
        set(value) {
            val prevState = stateOrNull ?: return
            taskStateMap[this] = prevState.copy(viewState = value)
        }

    private val Task.notificationId: Int
        get() = id.hashCode()

    /** Processes pending tasks, prioritizing downloads. */
    private fun doYourWork() {
        // Respect WiFi-only / mobile-only restriction set in Network settings.
        if (!PreferenceUtil.isNetworkAvailableForDownload()) return
        val maxConcurrency = MAX_CONCURRENT_DOWNLOADS.getInt()
        val effectiveLimit = if (maxConcurrency == 0) Int.MAX_VALUE else maxConcurrency
        if (taskStateMap.countRunning() >= effectiveLimit) return

        taskStateMap.entries
            .sortedBy { (_, state) -> state.downloadState }
            .firstOrNull { (_, state) ->
                state.downloadState == ReadyWithInfo || state.downloadState == Idle
            }
            ?.let { (task, state) ->
                // Re-read the task's current state at the point of execution. Between the
                // sortedBy/firstOrNull pass above and this lambda, a concurrent UI action
                // (pause, cancel, delete) may have already changed the state. If the state
                // is no longer Idle/ReadyWithInfo, skip it — doYourWork() will fire again
                // on the next state-change tick and pick the correct next task.
                when (task.stateOrNull?.downloadState ?: return) {
                    Idle -> task.prepare()
                    ReadyWithInfo -> task.download()
                    else -> { /* state changed concurrently — skip, will retry next tick */ }
                }
            }
    }

    private fun Task.prepare() {
        check(downloadState == Idle)
        if (type is TypeInfo.CustomCommand) {
            execute()
        } else {
            fetchInfo()
        }
    }

    private fun isNetworkError(throwable: Throwable): Boolean {
        return throwable.message?.let { msg ->
            NETWORK_ERROR_KEYWORDS.any { msg.contains(it, ignoreCase = true) }
        } ?: false
    }

    private fun scheduleNetworkPause() {
        networkPauseJob?.cancel()
        val startAt = networkDegradedAtMs
        if (startAt == 0L) return
        val pauseDelayMs = PreferenceUtil.getNetworkPauseDelayMs()
        networkPauseJob =
            scope.launch {
                val elapsed = System.currentTimeMillis() - startAt
                val remaining = (pauseDelayMs - elapsed).coerceAtLeast(0L)
                delay(remaining)
                if (!PreferenceUtil.isNetworkAvailableForDownload() && networkDegradedAtMs == startAt) {
                    pauseRunningTasksForNetwork()
                }
            }
    }

    private fun markNetworkDegraded() {
        if (networkDegradedAtMs == 0L) {
            networkDegradedAtMs = System.currentTimeMillis()
        }
        scheduleNetworkPause()
    }

    private fun ensureNetworkDegradedStart() {
        if (networkDegradedAtMs == 0L) {
            networkDegradedAtMs = System.currentTimeMillis()
            scheduleNetworkPause()
        }
    }

    private fun clearNetworkDegraded() {
        networkDegradedAtMs = 0L
        networkPauseJob?.cancel()
    }

    private fun isWithinNetworkGracePeriod(): Boolean {
        val startAt = networkDegradedAtMs
        if (startAt == 0L) return false
        return System.currentTimeMillis() - startAt < PreferenceUtil.getNetworkPauseDelayMs()
    }

    private fun pauseRunningTasksForNetwork() {
        taskStateMap.entries.forEach { (task, state) ->
            val downloadState = state.downloadState
            if (downloadState is DownloadState.Cancelable) {
                task.pauseForReason(downloadState, PauseReason.Network)
            } else {
                if (downloadState is ReadyWithInfo || downloadState is Idle) {
                    val action =
                        when (downloadState) {
                            is ReadyWithInfo -> Download
                            is Idle -> FetchInfo
                            else -> null
                        }
                    if (action != null) {
                        task.downloadState =
                            Paused(action = action, progress = null, reason = PauseReason.Network)
                    }
                }
            }
        }
    }

    private fun resumeNetworkPausedTasks() {
        if (!PreferenceUtil.isNetworkAvailableForDownload()) return
        taskStateMap.entries.forEach { (task, state) ->
            val downloadState = state.downloadState
            if (downloadState is DownloadState.Paused && downloadState.reason == PauseReason.Network) {
                waitingForNetwork.remove(task.id)
                task.downloadState =
                    when (downloadState.action) {
                        Download -> ReadyWithInfo
                        FetchInfo -> Idle
                    }
            }
        }
    }

    private fun Task.fetchInfo() {
        check(downloadState == Idle)
        val task = this
        val taskInfo = task.type
        val playlistIndex = if (taskInfo is TypeInfo.Playlist) taskInfo.index else null
        scope
            .launch(Dispatchers.Default) {
                DownloadUtil.fetchVideoInfoFromUrl(
                        url = url,
                        playlistIndex = playlistIndex,
                        preferences = preferences,
                        taskKey = id,
                    )
                    .onSuccess {
                        info = it
                        downloadState = ReadyWithInfo
                        viewState = Task.ViewState.fromVideoInfo(it)
                    }
                    .onFailure { throwable ->
                        if (throwable is YoutubeDL.CanceledException) {
                            return@onFailure
                        }
                        val networkUnavailable = !PreferenceUtil.isNetworkAvailableForDownload()
                        if (networkUnavailable) {
                            ensureNetworkDegradedStart()
                            if (isWithinNetworkGracePeriod()) {
                                waitingForNetwork[id] = FetchInfo
                                delay(2_000L)
                                if (!PreferenceUtil.isNetworkAvailableForDownload()) {
                                    task.downloadState = Idle
                                }
                                return@onFailure
                            }
                            when (val preState = downloadState) {
                                is FetchingInfo -> {
                                    downloadState =
                                        Paused(action = FetchInfo, progress = null, reason = PauseReason.Network)
                                    NotificationUtil.updateNotification(
                                        notificationId = notificationId,
                                        title = viewState.title,
                                        text = appContext.getString(R.string.status_paused),
                                    )
                                }
                                else -> {}
                            }
                            return@onFailure
                        }
                        task.downloadState = Error(throwable = throwable, action = FetchInfo)
                        NotificationUtil.notifyError(
                            title = viewState.title,
                            textId = R.string.fetch_info_error_msg,
                            notificationId = notificationId,
                            report = throwable.message ?: "Unknown error",
                        )
                    }
            }
            .also { job -> downloadState = FetchingInfo(job = job, taskId = id) }
    }

    private fun Task.download() {
        check(downloadState == ReadyWithInfo && info != null)
        if (type is TypeInfo.CustomCommand) {
            execute()
            return
        }
        scope
            .launch(Dispatchers.Default) {
                // Per-download throttling state for the progress callback below. These are
                // local to this download attempt (a fresh closure is created each time
                // Task.download() runs, e.g. on resume/retry), so no cross-task interference.
                var lastUiUpdateAtMs = 0L
                var lastNotifiedAtMs = 0L
                var lastNotifiedProgress = -1
                DownloadUtil.downloadVideo(
                        videoInfo = info,
                        taskId = id,
                        downloadPreferences = preferences,
                        progressCallback = { progressPercentage, _, text ->
                            val progress = progressPercentage / 100f
                            val progressInt = progressPercentage.toInt()
                            // Strip yt-dlp's "[download] " prefix so progressText stored
                            // in the Running state is clean for any UI that displays it.
                            val cleanText = text
                                .removePrefix("[download] ")
                                .removePrefix("[download]")
                                .trim()
                            val now = System.currentTimeMillis()
                            when (val preState = downloadState) {
                                is Running -> {
                                    // Throttle Compose state writes so the Home screen's
                                    // active-download list (which lives behind the nav
                                    // drawer) doesn't recompose/re-layout on every single
                                    // yt-dlp progress line — see PROGRESS_UI_UPDATE_THROTTLE_MS
                                    // comment above for why this matters. The final jump to
                                    // 100%/Completed is handled separately in onSuccess below,
                                    // so a throttled last tick here is harmless.
                                    if (now - lastUiUpdateAtMs >= PROGRESS_UI_UPDATE_THROTTLE_MS) {
                                        lastUiUpdateAtMs = now
                                        downloadState =
                                            preState.copy(progress = progress, progressText = cleanText)
                                    }
                                    // Throttle notification updates independently (and more
                                    // conservatively, since each is a Binder IPC into
                                    // system_server) and skip redundant calls when the
                                    // integer percentage hasn't actually changed.
                                    if (progressInt != lastNotifiedProgress &&
                                        now - lastNotifiedAtMs >= PROGRESS_NOTIFICATION_THROTTLE_MS
                                    ) {
                                        lastNotifiedAtMs = now
                                        lastNotifiedProgress = progressInt
                                        NotificationUtil.notifyProgress(
                                            notificationId = notificationId,
                                            progress = progressInt,
                                            text = cleanText,
                                            title = viewState.title,
                                            taskId = id,
                                        )
                                    }
                                }
                                else -> {}
                            }
                        },
                    )
                    .onSuccess { pathList ->
                        retryCountMap.remove(id)  // clear retry counter on success
                        downloadState = Completed(pathList.firstOrNull())

                        val text =
                            appContext.getString(
                                if (pathList.isEmpty()) R.string.status_completed
                                else R.string.download_finish_notification
                            )
                        FileUtil.createIntentForOpeningFile(pathList.firstOrNull()).run {
                            NotificationUtil.finishNotification(
                                notificationId,
                                title = viewState.title,
                                text = text,
                                intent =
                                    if (this != null)
                                        PendingIntent.getActivity(
                                            appContext,
                                            0,
                                            this,
                                            PendingIntent.FLAG_IMMUTABLE,
                                        )
                                    else null,
                            )
                        }
                        // Write docs text file if enabled
                        // Capture `info` into a local val before the null check to avoid
                        // a concurrent remove() between the check and the force-unwrap.
                        val videoInfoSnapshot = info
                        if (preferences.downloadDocs && videoInfoSnapshot != null) {
                            DownloadUtil.writeDocsTextFile(videoInfoSnapshot)
                        }
                    }
                    .onFailure { throwable ->
                        if (throwable is YoutubeDL.CanceledException) {
                            return@onFailure
                        }
                        val retries = retryCountMap.getOrDefault(id, 0)
                        val isNetworkError = isNetworkError(throwable)
                        val networkUnavailable = !PreferenceUtil.isNetworkAvailableForDownload()
                        if (networkUnavailable) {
                            ensureNetworkDegradedStart()
                            retryCountMap.remove(id)
                            if (isWithinNetworkGracePeriod()) {
                                waitingForNetwork[id] = Download
                                when (val preState = downloadState) {
                                    is Running -> {
                                        downloadState =
                                            preState.copy(progressText = "Waiting for network...")
                                    }
                                    else -> {}
                                }
                                delay(2_000L)
                                if (downloadState is Running && !PreferenceUtil.isNetworkAvailableForDownload()) {
                                    downloadState = ReadyWithInfo
                                }
                                return@onFailure
                            }
                            when (val preState = downloadState) {
                                is Running -> {
                                    downloadState =
                                        Paused(
                                            action = Download,
                                            progress = preState.progress,
                                            reason = PauseReason.Network,
                                        )
                                    NotificationUtil.updateNotification(
                                        notificationId = notificationId,
                                        title = viewState.title,
                                        text = appContext.getString(R.string.status_paused),
                                    )
                                }
                                else -> {}
                            }
                        } else if (isNetworkError && retries < MAX_AUTO_RETRIES) {
                            val attempt = retries + 1
                            retryCountMap[id] = attempt
                            Log.d(TAG, "Network error — retrying ($attempt/$MAX_AUTO_RETRIES) in ${RETRY_DELAY_MS}ms: ${throwable.message}")
                            // Show retrying status in the running card
                            when (val preState = downloadState) {
                                is Running -> downloadState = preState.copy(
                                    progress = preState.progress,
                                    progressText = "Retrying ($attempt/$MAX_AUTO_RETRIES)..."
                                )
                                else -> {}
                            }
                            delay(RETRY_DELAY_MS)
                            // Only retry if still in Running state (user didn't cancel)
                            if (downloadState is Running) {
                                downloadState = ReadyWithInfo
                            }
                        } else {
                            retryCountMap.remove(id)
                            downloadState = Error(throwable = throwable, action = Download)
                            NotificationUtil.notifyError(
                                title = viewState.title,
                                textId = R.string.download_error_msg,
                                notificationId = notificationId,
                                report = throwable.message ?: "Unknown error",
                            )
                        }
                    }
            }
            .also { job -> 
                // Restore progress if this download was resumed from a paused state
                val initialProgress = resumedProgressMap.remove(id) ?: -1f
                downloadState = Running(job = job, taskId = id, progress = initialProgress)
            }
    }

    private fun Task.pauseImpl(): Boolean {
        return when (val preState = downloadState) {
            is DownloadState.Cancelable -> {
                pauseForReason(preState, PauseReason.User)
            }
            else -> {
                false
            }
        }
    }

    private fun Task.pauseForReason(
        preState: DownloadState.Cancelable,
        reason: PauseReason,
    ): Boolean {
        val res = YoutubeDL.destroyProcessById(preState.taskId)
        preState.job.cancel()
        val progress = if (preState is Running) preState.progress else null
        NotificationUtil.updateNotification(
            notificationId = notificationId,
            title = viewState.title,
            text = appContext.getString(R.string.status_paused),
        )
        downloadState =
            DownloadState.Paused(action = preState.action, progress = progress, reason = reason)
        return true
    }

    private fun Task.resumeImpl(): Boolean {
        when (val preState = downloadState) {
            is DownloadState.Paused -> {
                // Store the paused progress so it can be restored when download resumes
                preState.progress?.let { progress ->
                    resumedProgressMap[id] = progress
                }
                downloadState =
                    when (preState.action) {
                        Download -> ReadyWithInfo
                        FetchInfo -> Idle
                    }
                return true
            }
            else -> {
                return false
            }
        }
    }

    private fun Task.cancelImpl(): Boolean {
        when (val preState = downloadState) {
            is DownloadState.Cancelable -> {
                val res = YoutubeDL.destroyProcessById(preState.taskId)
                preState.job.cancel()
                val progress = if (preState is Running) preState.progress else null
                NotificationUtil.cancelNotification(notificationId)
                downloadState =
                    DownloadState.Canceled(action = preState.action, progress = progress)
                return true
            }
            Idle -> {
                downloadState = DownloadState.Canceled(action = FetchInfo)
            }
            ReadyWithInfo -> {
                downloadState = DownloadState.Canceled(action = Download)
            }
            is DownloadState.Paused -> {
                NotificationUtil.cancelNotification(notificationId)
                downloadState = DownloadState.Canceled(action = preState.action, progress = preState.progress)
                return true
            }

            else -> {
                return false
            }
        }
        return true
    }

    private fun Task.restartImpl() {
        when (val preState = stateOrNull?.downloadState) {
            is DownloadState.Restartable -> {
                downloadState =
                    when (preState.action) {
                        Download -> ReadyWithInfo
                        FetchInfo -> Idle
                    }
            }
            null -> {
                // Task was removed from the map (e.g. deleted while Retry was tapped).
                // Silently ignore rather than crashing — the task no longer exists.
                Log.w(TAG, "restartImpl: task ${id} not in map, ignoring restart")
            }
            else -> {
                // State is not Restartable (e.g. already Running after a rapid double-tap).
                // Silently ignore — crashing the UI thread here is worse than a no-op.
                Log.w(TAG, "restartImpl: task ${id} is in non-restartable state ${preState::class.simpleName}, ignoring")
            }
        }
    }

    /**
     * Execute a custom command task
     *
     * @see Task.TypeInfo.CustomCommand
     */
    private fun Task.execute() {
        check(downloadState == Idle)
        check(type is TypeInfo.CustomCommand)
        val template = type.template
        scope
            .launch {
                // Same unthrottled-callback hazard as Task.download() (see
                // PROGRESS_UI_UPDATE_THROTTLE_MS/PROGRESS_NOTIFICATION_THROTTLE_MS comment
                // above): custom-command execution's yt-dlp process emits progress lines just
                // as frequently, so this closure needs its own local throttling state too —
                // otherwise the drawer-unresponsive-during-download bug reappears whenever a
                // Custom Command template task is running instead of a normal download.
                var lastUiUpdateAtMs = 0L
                var lastNotifiedAtMs = 0L
                var lastNotifiedProgress = -1
                DownloadUtil.executeCustomCommandTask(url, id, template, preferences) {
                        progressPercentage,
                        _,
                        text ->
                        val progress = progressPercentage / 100f
                        val progressInt = progressPercentage.toInt()
                        val now = System.currentTimeMillis()
                        when (val preState = downloadState) {
                            is Running -> {
                                if (now - lastUiUpdateAtMs >= PROGRESS_UI_UPDATE_THROTTLE_MS) {
                                    lastUiUpdateAtMs = now
                                    downloadState =
                                        preState.copy(progress = progress, progressText = text)
                                }
                                if (progressInt != lastNotifiedProgress &&
                                    now - lastNotifiedAtMs >= PROGRESS_NOTIFICATION_THROTTLE_MS
                                ) {
                                    lastNotifiedAtMs = now
                                    lastNotifiedProgress = progressInt
                                    NotificationUtil.makeNotificationForCustomCommand(
                                        notificationId = notificationId,
                                        taskId = id,
                                        progress = progressInt,
                                        templateName = template.name,
                                        taskUrl = url,
                                        text = text,
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                    .onFailure { throwable ->
                        if (throwable is YoutubeDL.CanceledException) {
                            return@onFailure
                        }
                        downloadState = Error(throwable = throwable, action = Download)
                        NotificationUtil.notifyError(
                            title = viewState.title,
                            textId = R.string.download_error_msg,
                            notificationId = notificationId,
                            report = throwable.message ?: "Unknown error",
                        )
                    }
                    .onSuccess {
                        downloadState = Completed(null)

                        val text = appContext.getString(R.string.status_completed)

                        NotificationUtil.finishNotification(
                            notificationId = notificationId,
                            title = viewState.title,
                            text = text,
                            intent = null,
                        )
                    }
            }
            .also { downloadState = Running(job = it, taskId = id) }
    }
}
