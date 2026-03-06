package com.junkfood.seal.ui.page.schedule

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.offset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledDownloadsPage(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tasks by DatabaseUtil.getScheduledTasksFlow().collectAsState(initial = emptyList())

    var taskToDelete by remember { mutableStateOf<ScheduledTask?>(null) }
    var taskToEdit by remember { mutableStateOf<ScheduledTask?>(null) }

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
        var isBatteryOptimized by remember { mutableStateOf(ScheduleUtil.isBatteryOptimized(context)) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasExactAlarmPermission = ScheduleUtil.canScheduleExactAlarms(context)
                    // Re-check battery optimization so the banner dismisses immediately
                    // after the user returns from the system settings screen.
                    isBatteryOptimized = ScheduleUtil.isBatteryOptimized(context)
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

            // Battery optimization warning banner — shown when the app is still
            // battery-optimized. OEM optimizers (MIUI, One UI) can silently kill
            // AlarmManager alarms when the app is swiped away, causing scheduled
            // downloads to never start unless the user opens the app manually.
            if (isBatteryOptimized) {
                BatteryOptimizationBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    onDisableClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}"),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                            onEditClick = { taskToEdit = task },
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

    taskToEdit?.let { editTask ->
        EditScheduleDialog(
            task = editTask,
            onDismiss = { taskToEdit = null },
            onSave = { newTimeMillis, newNetworkPref ->
                val task = taskToEdit ?: return@EditScheduleDialog
                taskToEdit = null
                scope.launch {
                    ScheduleUtil.rescheduleTask(context, task, newTimeMillis, newNetworkPref)
                }
            },
        )
    }
}

@Composable
private fun ScheduledTaskItem(
    task: ScheduledTask,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Thumbnail
            AsyncImage(
                model = task.thumbnailUrl.ifBlank { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )

            Spacer(Modifier.width(12.dp))

            // Content column
            Column(modifier = Modifier.weight(1f)) {

                // Title row + three-dot menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = task.title.ifBlank { task.url },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(32.dp)
                                .offset(x = 6.dp, y = (-4).dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_scheduled_download)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.cancel_scheduled_download_action),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Chips row — time + network
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Scheduled-time chip
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = ScheduleUtil.formatScheduledTime(task.scheduledTimeMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    // Network-preference chip
                    val networkPref = ScheduleNetworkPreference.entries
                        .firstOrNull { it.id == task.networkPreference }
                        ?: ScheduleNetworkPreference.BOTH

                    val (netIcon, netLabel) = when (networkPref) {
                        ScheduleNetworkPreference.WIFI_ONLY ->
                            Pair(Icons.Outlined.SignalWifi4Bar, stringResource(R.string.wifi_only))
                        ScheduleNetworkPreference.BOTH ->
                            Pair(Icons.Rounded.NetworkCheck, stringResource(R.string.schedule_network_both))
                        ScheduleNetworkPreference.MOBILE_DATA ->
                            Pair(Icons.Outlined.SignalCellular4Bar, stringResource(R.string.mobile_data))
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = netIcon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = netLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog that lets the user change the scheduled time and network preference of an
 * existing [ScheduledTask] without having to delete and re-create the task.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScheduleDialog(
    task: ScheduledTask,
    onDismiss: () -> Unit,
    onSave: (newTimeMillis: Long, newNetworkPref: ScheduleNetworkPreference) -> Unit,
) {
    val context = LocalContext.current
    var dateTimeMillis by remember { mutableLongStateOf(task.scheduledTimeMillis) }
    var networkPreference by remember {
        mutableStateOf(
            ScheduleNetworkPreference.entries.firstOrNull { it.id == task.networkPreference }
                ?: ScheduleNetworkPreference.BOTH
        )
    }

    val dateLabel = remember(dateTimeMillis) {
        SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault()).format(Date(dateTimeMillis))
    }
    val timeLabel = remember(dateTimeMillis) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dateTimeMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_scheduled_download)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Date + time pickers side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = dateTimeMillis }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = dateTimeMillis
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    dateTimeMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = dateTimeMillis }
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = dateTimeMillis
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    dateTimeMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }

                // Network preference
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.network_preference),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ScheduleNetworkPreference.entries.forEach { pref ->
                            val label = stringResource(
                                when (pref) {
                                    ScheduleNetworkPreference.WIFI_ONLY -> R.string.wifi_only
                                    ScheduleNetworkPreference.BOTH -> R.string.schedule_network_both
                                    ScheduleNetworkPreference.MOBILE_DATA -> R.string.mobile_data
                                }
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { networkPreference = pref },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = networkPreference == pref,
                                    onClick = { networkPreference = pref },
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(dateTimeMillis, networkPreference) },
                enabled = dateTimeMillis > System.currentTimeMillis(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Full-width warning card shown when the app is still battery-optimized.
 *
 * Tapping the action button opens the system dialog that lets the user exempt this app
 * from battery optimizations, which is the definitive fix for alarms being suppressed
 * after the app is swiped away from Recents on OEM-skinned devices (MIUI, One UI, etc.).
 */
@Composable
private fun BatteryOptimizationBanner(
    modifier: Modifier = Modifier,
    onDisableClick: () -> Unit = {},
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
                    text = stringResource(R.string.battery_optimization_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.battery_optimization_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDisableClick) {
                Text(
                    text = stringResource(R.string.disable_battery_optimization),
                    color = MaterialTheme.colorScheme.onErrorContainer,
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
