package com.junkfood.seal.download

import android.app.PendingIntent
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
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
import kotlinx.coroutines.SupervisorJob
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

    companion object {
        private const val MAX_AUTO_RETRIES = 3
        private const val RETRY_DELAY_MS = 5_000L
        private val NETWORK_ERROR_KEYWORDS = listOf(
            "Unable to connect", "Connection reset", "timed out", "connect timed out",
            "HTTP Error 5", "Network is unreachable", "nodename nor servname",
            "Failed to establish", "RemoteDisconnected", "SSLError"
        )
    }
    private val snapshotFlow = snapshotFlow { taskStateMap.toMap() }

    init {
        // Re-trigger doYourWork() when suitable network becomes available
        // (e.g. WiFi reconnects after the restriction blocked queued tasks).
        App.connectivityManager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = doYourWork()
            }
        )

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
                .collect { if (it > 0) App.startService() else App.stopService() }
        }

        scope.launch(Dispatchers.IO) {
            // don't write before we read
            enqueueFromBackup()

            // Only write backup when a structurally important state changes.
            // Strip volatile Running.progress/progressText before comparing so that the constant
            // stream of progress callbacks (~5/sec) does NOT trigger a full MMKV serialization.
            snapshotFlow
                .map { map ->
                    map
                        .filter { (_, state) -> state.downloadState !is Completed }
                        .mapValues { (_, state) ->
                            state.copy(
                                downloadState = when (val ds = state.downloadState) {
                                    is Running -> ds.copy(progress = -1f, progressText = "")
                                    else -> ds
                                }
                            )
                        }
                }
                .distinctUntilChanged()
                .collect { snapshot ->
                    snapshot.forEach { (_, state) -> Log.d(TAG, state.viewState.title) }
                    // Write back original map (with real progress) so paused-on-kill tasks
                    // restore with the last known progress value.
                    val original = taskStateMap
                        .filter { (_, state) -> state.downloadState !is Completed }
                    PreferenceUtil.encodeTaskListBackup(original)
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
                            is FetchingInfo,
                            Idle -> {
                                Canceled(action = FetchInfo)
                            }
                            is Running -> {
                                Paused(action = Download, progress = preState.progress)
                            }

                            ReadyWithInfo -> {
                                Paused(action = Download, progress = null)
                            }
                            is Paused -> {
                                // Keep paused state on restart
                                preState
                            }
                            else -> {
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

    private var Task.state: Task.State
        get() = taskStateMap[this]!!
        set(value) {
            taskStateMap[this] = value
        }

    private var Task.downloadState: DownloadState
        get() = state.downloadState
        set(value) {
            val prevState = state
            taskStateMap[this] = prevState.copy(downloadState = value)
        }

    private var Task.info: VideoInfo?
        get() = state.videoInfo
        set(value) {
            val prevState = state
            taskStateMap[this] = prevState.copy(videoInfo = value)
        }

    private var Task.viewState: Task.ViewState
        get() = state.viewState
        set(value) {
            val prevState = state
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
                when (state.downloadState) {
                    Idle -> task.prepare()
                    ReadyWithInfo -> task.download()
                    else -> {
                        throw IllegalStateException()
                    }
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
                DownloadUtil.downloadVideo(
                        videoInfo = info,
                        taskId = id,
                        downloadPreferences = preferences,
                        progressCallback = { progressPercentage, _, text ->
                            val progress = progressPercentage / 100f
                            // Strip yt-dlp's "[download] " prefix so progressText stored
                            // in the Running state is clean for any UI that displays it.
                            val cleanText = text
                                .removePrefix("[download] ")
                                .removePrefix("[download]")
                                .trim()
                            when (val preState = downloadState) {
                                is Running -> {
                                    downloadState =
                                        preState.copy(progress = progress, progressText = cleanText)
                                    NotificationUtil.notifyProgress(
                                        notificationId = notificationId,
                                        progress = progressPercentage.toInt(),
                                        text = cleanText,
                                        title = viewState.title,
                                        taskId = id,
                                    )
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
                    }
                    .onFailure { throwable ->
                        if (throwable is YoutubeDL.CanceledException) {
                            return@onFailure
                        }
                        val retries = retryCountMap.getOrDefault(id, 0)
                        val isNetworkError = throwable.message?.let { msg ->
                            NETWORK_ERROR_KEYWORDS.any { msg.contains(it, ignoreCase = true) }
                        } ?: false

                        if (isNetworkError && retries < MAX_AUTO_RETRIES) {
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
        when (val preState = downloadState) {
            is DownloadState.Cancelable -> {
                val res = YoutubeDL.destroyProcessById(preState.taskId)
                if (res) {
                    preState.job.cancel()
                    val progress = if (preState is Running) preState.progress else null
                    NotificationUtil.updateNotification(
                        notificationId = notificationId,
                        title = viewState.title,
                        text = appContext.getString(R.string.status_paused),
                    )
                    downloadState =
                        DownloadState.Paused(action = preState.action, progress = progress)
                }
                return res
            }
            else -> {
                return false
            }
        }
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
                if (res) {
                    preState.job.cancel()
                    val progress = if (preState is Running) preState.progress else null
                    NotificationUtil.cancelNotification(notificationId)
                    downloadState =
                        DownloadState.Canceled(action = preState.action, progress = progress)
                }
                return res
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
        when (val preState = downloadState) {
            is DownloadState.Restartable -> {
                downloadState =
                    when (preState.action) {
                        Download -> ReadyWithInfo
                        FetchInfo -> Idle
                    }
            }
            else -> {
                throw IllegalStateException()
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
                DownloadUtil.executeCustomCommandTask(url, id, template, preferences) {
                        progressPercentage,
                        _,
                        text ->
                        val progress = progressPercentage / 100f
                        when (val preState = downloadState) {
                            is Running -> {
                                downloadState =
                                    preState.copy(progress = progress, progressText = text)
                                NotificationUtil.makeNotificationForCustomCommand(
                                    notificationId = notificationId,
                                    taskId = id,
                                    progress = progressPercentage.toInt(),
                                    templateName = template.name,
                                    taskUrl = url,
                                    text = text,
                                )
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
