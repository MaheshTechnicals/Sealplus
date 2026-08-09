package com.junkfood.seal.ui.page.downloadv2.configure

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.component.OutlinedDismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.util.DownloadType.Audio
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.PlaylistEntry
import com.junkfood.seal.util.PlaylistResult
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class YtdlpSearchProvider(
    val prefix: String,
    val displayName: String,
    @StringRes val labelRes: Int,
) {
    YouTube(
        prefix = "ytsearch10",
        displayName = "YouTube",
        labelRes = R.string.ytdlp_search_provider_youtube,
    ),
    SoundCloud(
        prefix = "scsearch10",
        displayName = "SoundCloud",
        labelRes = R.string.ytdlp_search_provider_soundcloud,
    ),
}

@Composable
fun YtdlpSearchDialog(
    initialQuery: String = "",
    initialProvider: YtdlpSearchProvider = YtdlpSearchProvider.YouTube,
    config: Config,
    preferences: DownloadUtil.DownloadPreferences,
    onDismissRequest: () -> Unit,
    onActionPost: (Action) -> Unit,
) {
    val providers = remember { YtdlpSearchProvider.entries }
    val scope = rememberCoroutineScope()

    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var providerIndex by
        rememberSaveable(initialProvider) { mutableIntStateOf(initialProvider.ordinal) }
    var results by remember { mutableStateOf<List<PlaylistEntry>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeSearchId by remember { mutableStateOf<String?>(null) }
    var activeSearchJob by remember { mutableStateOf<Job?>(null) }

    val selectedProvider = providers[providerIndex]

    fun cancelActiveSearch() {
        activeSearchId?.let { YoutubeDL.destroyProcessById(it) }
        activeSearchJob?.cancel()
        activeSearchId = null
        activeSearchJob = null
        loading = false
    }

    fun dismiss() {
        cancelActiveSearch()
        onDismissRequest()
    }

    fun search() {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty() || loading) return

        val searchUrl = "${selectedProvider.prefix}:$trimmedQuery"
        activeSearchId = searchUrl
        activeSearchJob =
            scope.launch {
                loading = true
                errorMessage = null
                hasSearched = true

                val result =
                    withContext(Dispatchers.IO) {
                        DownloadUtil.getPlaylistOrVideoInfo(
                            playlistURL = searchUrl,
                            downloadPreferences = preferences.copy(extractAudio = false),
                            showToast = false,
                        )
                    }

                result
                    .onSuccess { info ->
                        results = (info as? PlaylistResult)?.entries.orEmpty()
                    }
                    .onFailure {
                        results = emptyList()
                        errorMessage = it.message
                    }

                if (activeSearchId == searchUrl) {
                    activeSearchId = null
                    activeSearchJob = null
                    loading = false
                }
            }
    }

    LaunchedEffect(initialQuery, initialProvider) {
        if (initialQuery.isNotBlank()) search()
    }

    SealDialog(
        onDismissRequest = ::dismiss,
        icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        title = { Text(stringResource(R.string.ytdlp_search_title)) },
        confirmButton = null,
        dismissButton = { OutlinedDismissButton(onClick = ::dismiss) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "query") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.ytdlp_search_query)) },
                    )
                }

                item(key = "provider") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.ytdlp_search_service),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            providers.forEachIndexed { index, provider ->
                                FilterChip(
                                    selected = providerIndex == index,
                                    enabled = !loading,
                                    onClick = {
                                        providerIndex = index
                                        results = emptyList()
                                        hasSearched = false
                                        errorMessage = null
                                    },
                                    label = { Text(stringResource(provider.labelRes)) },
                                )
                            }
                        }
                    }
                }

                item(key = "search") {
                    Button(
                        onClick = ::search,
                        enabled = query.isNotBlank() && !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                        Text(
                            text = stringResource(R.string.ytdlp_search_title),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                if (loading) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                errorMessage?.let { message ->
                    item(key = "error") {
                        val fallbackMessage = stringResource(R.string.fetch_info_error_msg)
                        Text(
                            text = if (message.isBlank()) fallbackMessage else message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (!loading && errorMessage == null && hasSearched && results.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.ytdlp_search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (results.isNotEmpty()) {
                    item(key = "hint") {
                        Text(
                            text = stringResource(R.string.ytdlp_search_tap_thumbnail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    itemsIndexed(
                        items = results,
                        key = { index, item -> "${item.id ?: item.url ?: item.title}:$index" },
                    ) { _, entry ->
                        SearchResultRow(
                            entry = entry,
                            provider = selectedProvider,
                            onThumbnailClick = { url ->
                                onActionPost(
                                    Action.DownloadWithPreset(
                                        urlList = listOf(url),
                                        preferences =
                                            preferences.copy(extractAudio = config.downloadType == Audio),
                                    )
                                )
                                dismiss()
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SearchResultRow(
    entry: PlaylistEntry,
    provider: YtdlpSearchProvider,
    onThumbnailClick: (String) -> Unit,
) {
    val downloadUrl = remember(entry, provider) { entry.resolveDownloadUrl(provider) }
    val thumbnailUrl = remember(entry) { entry.bestThumbnailUrl() }
    val duration = remember(entry.duration) { formatDuration(entry.duration) }
    val metadata = listOfNotNull(entry.uploader ?: entry.channel, duration).joinToString(" • ")

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.width(128.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(enabled = downloadUrl != null) {
                        downloadUrl?.let(onThumbnailClick)
                    },
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnailUrl != null) {
                AsyncImageImpl(
                    model = thumbnailUrl,
                    contentDescription = entry.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.VideoFile,
                    contentDescription = entry.title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (metadata.isNotBlank()) {
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun PlaylistEntry.bestThumbnailUrl(): String? {
    val available = thumbnails.orEmpty().filter { it.url.isNotBlank() }
    return available.maxByOrNull { it.width * it.height }?.url ?: thumbnail
}

private fun PlaylistEntry.resolveDownloadUrl(provider: YtdlpSearchProvider): String? {
    val candidate = (webpageUrl ?: url)?.trim().orEmpty()
    if (candidate.startsWith("https://") || candidate.startsWith("http://")) return candidate

    return when (provider) {
        YtdlpSearchProvider.YouTube ->
            (id?.takeIf { it.isNotBlank() } ?: candidate.takeIf { it.isNotBlank() })?.let {
                "https://www.youtube.com/watch?v=$it"
            }
        YtdlpSearchProvider.SoundCloud -> null
    }
}

private fun formatDuration(seconds: Double?): String? {
    val totalSeconds = seconds?.toLong()?.takeIf { it >= 0 } ?: return null
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val remainingSeconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    } else {
        "%d:%02d".format(minutes, remainingSeconds)
    }
}
