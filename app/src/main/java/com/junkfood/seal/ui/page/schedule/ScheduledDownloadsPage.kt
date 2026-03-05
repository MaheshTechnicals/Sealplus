package com.junkfood.seal.ui.page.schedule

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.SignalWifi4Bar
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.ScheduledTask
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.ScheduleNetworkPreference
import com.junkfood.seal.util.ScheduleUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledDownloadsPage(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tasks by DatabaseUtil.getScheduledTasksFlow().collectAsState(initial = emptyList())

    var taskToDelete by remember { mutableStateOf<ScheduledTask?>(null) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scheduled_downloads)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        // Re-check permission on every resume so the banner disappears immediately
        // when the user returns from the system "Schedule Exact Alarm" settings screen.
        var hasExactAlarmPermission by remember { mutableStateOf(ScheduleUtil.canScheduleExactAlarms(context)) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasExactAlarmPermission = ScheduleUtil.canScheduleExactAlarms(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Permission warning banner — shown when SCHEDULE_EXACT_ALARM is not granted
            if (!hasExactAlarmPermission) {
                ExactAlarmPermissionBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onGrantClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                )
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_scheduled_downloads),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tasks, key = { it.id }) { task ->
                        ScheduledTaskItem(
                            task = task,
                            onDeleteClick = { taskToDelete = task },
                        )
                    }
                }
            }
        }
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text(stringResource(R.string.cancel_scheduled_download)) },
            text = {
                Text(
                    text = taskToDelete?.title?.takeIf { it.isNotBlank() }
                        ?: taskToDelete?.url
                        ?: "",
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val task = taskToDelete ?: return@TextButton
                    taskToDelete = null
                    scope.launch {
                        ScheduleUtil.cancelScheduledTask(context, task)
                    }
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ScheduledTaskItem(
    task: ScheduledTask,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            AsyncImage(
                model = task.thumbnailUrl.ifBlank { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )

            Spacer(Modifier.width(12.dp))

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = task.title.ifBlank { task.url },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Scheduled time chip
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = ScheduleUtil.formatScheduledTime(task.scheduledTimeMillis),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                // Network preference chip
                val networkPref = ScheduleNetworkPreference.entries
                    .firstOrNull { it.id == task.networkPreference }
                    ?: ScheduleNetworkPreference.BOTH

                val (netIcon, netLabel) = when (networkPref) {
                    ScheduleNetworkPreference.WIFI_ONLY -> Pair(Icons.Outlined.SignalWifi4Bar, stringResource(R.string.wifi_only))
                    ScheduleNetworkPreference.BOTH -> Pair(Icons.Rounded.NetworkCheck, stringResource(R.string.schedule_network_both))
                    ScheduleNetworkPreference.MOBILE_DATA -> Pair(Icons.Outlined.SignalCellular4Bar, stringResource(R.string.mobile_data))
                }

                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = netLabel,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = netIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cancel_scheduled_download),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Full-width warning card shown at the top of [ScheduledDownloadsPage] when the app has not been
 * granted [android.permission.SCHEDULE_EXACT_ALARM].
 *
 * Without this permission, [android.app.AlarmManager.setAlarmClock] silently falls back to
 * inexact timing on API 31+ devices, meaning scheduled downloads may not start on time.
 */
@Composable
private fun ExactAlarmPermissionBanner(
    modifier: Modifier = Modifier,
    onGrantClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.exact_alarm_permission_required),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.exact_alarm_permission_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onGrantClick) {
                Text(
                    text = stringResource(R.string.grant_permission),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
