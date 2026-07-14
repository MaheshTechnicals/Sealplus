package com.junkfood.seal.ui.page.settings.sealplus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.SignalWifi4Bar
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSingleChoiceItem
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.page.security.LockScreen
import com.junkfood.seal.util.AuthenticationManager
import com.junkfood.seal.util.ARIA2C_CONNECTIONS
import com.junkfood.seal.util.FORMAT_LIST_VIEW
import com.junkfood.seal.util.MAX_CONCURRENT_DOWNLOADS
import com.junkfood.seal.util.NETWORK_ANY
import com.junkfood.seal.util.NETWORK_MOBILE_ONLY
import com.junkfood.seal.util.NETWORK_PAUSE_DELAY_SECONDS
import com.junkfood.seal.util.NETWORK_TYPE_RESTRICTION
import com.junkfood.seal.util.NETWORK_WIFI_ONLY
import com.junkfood.seal.util.NOTIFICATION_ERROR_SOUND
import com.junkfood.seal.util.NOTIFICATION_LED
import com.junkfood.seal.util.NOTIFICATION_SOUND
import com.junkfood.seal.util.NOTIFICATION_SUCCESS_SOUND
import com.junkfood.seal.util.NOTIFICATION_VIBRATE
import com.junkfood.seal.util.SPONSOR_DIALOG_FREQUENCY
import com.junkfood.seal.util.SPONSOR_FREQ_MONTHLY
import com.junkfood.seal.util.SPONSOR_FREQ_OFF
import com.junkfood.seal.util.SPONSOR_FREQ_WEEKLY
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.makeToast
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealPlusExtrasPage(
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToHiddenContent: () -> Unit = {},
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var networkTypeRestriction by remember { mutableStateOf(NETWORK_TYPE_RESTRICTION.getInt()) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    var networkPauseDelaySeconds by remember {
        mutableStateOf(NETWORK_PAUSE_DELAY_SECONDS.getInt())
    }
    
    var notificationSound by remember { mutableStateOf(NOTIFICATION_SOUND.getBoolean()) }
    var notificationVibrate by remember { mutableStateOf(NOTIFICATION_VIBRATE.getBoolean()) }
    var notificationLed by remember { mutableStateOf(NOTIFICATION_LED.getBoolean()) }
    var notificationSuccessSound by remember { mutableStateOf(NOTIFICATION_SUCCESS_SOUND.getBoolean()) }
    var sponsorDialogFrequency by remember { mutableStateOf(SPONSOR_DIALOG_FREQUENCY.getInt()) }
    var showSponsorFrequencyDialog by remember { mutableStateOf(false) }
    var notificationErrorSound by remember { mutableStateOf(NOTIFICATION_ERROR_SOUND.getBoolean()) }
    var formatListView by remember { mutableStateOf(FORMAT_LIST_VIEW.getBoolean()) }
    
    // Authentication state for AppLock settings
    var showAuthScreen by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(false) }

    // Authentication state for Hidden Content navigation
    var showHiddenContentAuthScreen by remember { mutableStateOf(false) }
    var hiddenContentAuthDone by remember { mutableStateOf(false) }

    // Show authentication screen if AppLock is enabled and user tries to access settings
    if (showAuthScreen && !isAuthenticated) {
        LockScreen(
            onUnlocked = {
                isAuthenticated = true
                showAuthScreen = false
                onNavigateToSecurity()
            },
            useBiometric = AuthenticationManager.useBiometric()
        )
        return
    }

    // Show authentication screen before entering Hidden Content page
    if (showHiddenContentAuthScreen && !hiddenContentAuthDone) {
        LockScreen(
            onUnlocked = {
                hiddenContentAuthDone = true
                showHiddenContentAuthScreen = false
                onNavigateToHiddenContent()
            },
            useBiometric = AuthenticationManager.useBiometric()
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(text = stringResource(id = R.string.sealplus_extras)) 
                },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            item {
                PreferenceSubtitle(text = stringResource(R.string.download_control))
            }
            
            item {
                var maxConcurrentDownloads by remember { 
                    mutableStateOf(MAX_CONCURRENT_DOWNLOADS.getInt()) 
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.max_concurrent_downloads),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (maxConcurrentDownloads == 0) {
                                    stringResource(R.string.unlimited_concurrent)
                                } else {
                                    stringResource(R.string.concurrent_downloads_desc, maxConcurrentDownloads)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (maxConcurrentDownloads == 0) "∞" else maxConcurrentDownloads.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    androidx.compose.material3.Slider(
                        value = maxConcurrentDownloads.toFloat(),
                        onValueChange = { newValue ->
                            maxConcurrentDownloads = newValue.toInt()
                        },
                        onValueChangeFinished = {
                            MAX_CONCURRENT_DOWNLOADS.updateInt(maxConcurrentDownloads)
                        },
                        valueRange = 0f..5f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0 (${stringResource(R.string.unlimited)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item {
                val aria2cConnectionOptions = listOf(2, 4, 8, 16, 32)
                var aria2cConnections by remember {
                    mutableStateOf(ARIA2C_CONNECTIONS.getInt().let { saved ->
                        if (saved in aria2cConnectionOptions) saved else 16
                    })
                }
                val aria2cSliderIndex = aria2cConnectionOptions.indexOf(aria2cConnections).coerceAtLeast(0)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.aria2c_connections),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.aria2c_connections_desc, aria2cConnections),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = aria2cConnections.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    androidx.compose.material3.Slider(
                        value = aria2cSliderIndex.toFloat(),
                        onValueChange = { newValue ->
                            val idx = newValue.roundToInt().coerceIn(0, aria2cConnectionOptions.size - 1)
                            aria2cConnections = aria2cConnectionOptions[idx]
                        },
                        onValueChangeFinished = {
                            ARIA2C_CONNECTIONS.updateInt(aria2cConnections)
                        },
                        valueRange = 0f..4f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "2",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "32",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                PreferenceSubtitle(text = stringResource(R.string.format_selection_layout))
            }

            item {
                PreferenceSwitch(
                    title = stringResource(R.string.format_list_view_title),
                    description = stringResource(R.string.format_list_view_desc),
                    icon = Icons.Outlined.ViewAgenda,
                    isChecked = formatListView,
                    onClick = {
                        FORMAT_LIST_VIEW.updateBoolean(!formatListView)
                        formatListView = !formatListView
                    }
                )
            }

            item {
                PreferenceSubtitle(text = stringResource(R.string.security_and_privacy))
            }
            
            item {
                PreferenceItem(
                    title = stringResource(R.string.app_lock),
                    description = stringResource(R.string.lock_app_with_pin_biometric),
                    icon = Icons.Outlined.Lock,
                    onClick = {
                        // Check if AppLock is enabled and PIN is set
                        if (AuthenticationManager.isSecurityEnabled() && AuthenticationManager.isPinSet()) {
                            // Show authentication screen before allowing access
                            showAuthScreen = true
                        } else {
                            // AppLock not enabled, go directly to settings
                            onNavigateToSecurity()
                        }
                    }
                )
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.hidden_content),
                    description = stringResource(R.string.hidden_content_desc),
                    icon = Icons.Outlined.VisibilityOff,
                    onClick = {
                        // Hidden Content requires App Lock to be enabled
                        if (AuthenticationManager.isSecurityEnabled() && AuthenticationManager.isPinSet()) {
                            hiddenContentAuthDone = false
                            showHiddenContentAuthScreen = true
                        } else {
                            // App Lock not set up — cannot access hidden content
                            context.makeToast(R.string.hidden_content_requires_app_lock)
                        }
                    }
                )
            }
            
            item {
                PreferenceItem(
                    title = stringResource(R.string.network_type_restriction),
                    description = when (networkTypeRestriction) {
                        NETWORK_WIFI_ONLY -> stringResource(R.string.wifi_only)
                        NETWORK_MOBILE_ONLY -> stringResource(R.string.mobile_only)
                        else -> stringResource(R.string.any_network)
                    },
                    icon = when (networkTypeRestriction) {
                        NETWORK_WIFI_ONLY -> Icons.Outlined.SignalWifi4Bar
                        NETWORK_MOBILE_ONLY -> Icons.Outlined.SignalCellular4Bar
                        else -> Icons.Rounded.NetworkCheck
                    },
                    onClick = { showNetworkDialog = true }
                )
            }

            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.network_pause_delay_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text =
                                    stringResource(
                                        R.string.network_pause_delay_desc,
                                        networkPauseDelaySeconds,
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = networkPauseDelaySeconds.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    androidx.compose.material3.Slider(
                        value = networkPauseDelaySeconds.toFloat(),
                        onValueChange = { newValue ->
                            networkPauseDelaySeconds = newValue.roundToInt().coerceIn(5, 120)
                        },
                        onValueChangeFinished = {
                            NETWORK_PAUSE_DELAY_SECONDS.updateInt(networkPauseDelaySeconds)
                        },
                        valueRange = 5f..120f,
                        steps = 114,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "120",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            
            item {
                PreferenceSubtitle(text = stringResource(R.string.notification_settings))
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_sound_settings),
                    description = stringResource(R.string.notification_sound_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationSound,
                    onClick = { 
                        NOTIFICATION_SOUND.updateBoolean(!notificationSound)
                        notificationSound = !notificationSound
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_vibrate_settings),
                    description = stringResource(R.string.notification_vibrate_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationVibrate,
                    enabled = notificationSound,
                    onClick = { 
                        NOTIFICATION_VIBRATE.updateBoolean(!notificationVibrate)
                        notificationVibrate = !notificationVibrate
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_led_settings),
                    description = stringResource(R.string.notification_led_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationLed,
                    enabled = notificationSound,
                    onClick = { 
                        NOTIFICATION_LED.updateBoolean(!notificationLed)
                        notificationLed = !notificationLed
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_success_sound_settings),
                    description = stringResource(R.string.notification_success_sound_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationSuccessSound,
                    enabled = notificationSound,
                    onClick = { 
                        NOTIFICATION_SUCCESS_SOUND.updateBoolean(!notificationSuccessSound)
                        notificationSuccessSound = !notificationSuccessSound
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_error_sound_settings),
                    description = stringResource(R.string.notification_error_sound_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationErrorSound,
                    enabled = notificationSound,
                    onClick = { 
                        NOTIFICATION_ERROR_SOUND.updateBoolean(!notificationErrorSound)
                        notificationErrorSound = !notificationErrorSound
                    }
                )
            }

            item {
                PreferenceSubtitle(text = stringResource(R.string.sponsor_support_section))
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.sponsor_dialog_frequency_title),
                    description = when (sponsorDialogFrequency) {
                        SPONSOR_FREQ_OFF -> stringResource(R.string.sponsor_dialog_off)
                        SPONSOR_FREQ_MONTHLY -> stringResource(R.string.sponsor_dialog_monthly)
                        else -> stringResource(R.string.sponsor_dialog_weekly)
                    },
                    icon = Icons.Outlined.VolunteerActivism,
                    onClick = { showSponsorFrequencyDialog = true },
                )
            }
        }

        if (showSponsorFrequencyDialog) {
            SponsorFrequencyDialog(
                currentSelection = sponsorDialogFrequency,
                onDismissRequest = { showSponsorFrequencyDialog = false },
                onConfirm = { selected ->
                    SPONSOR_DIALOG_FREQUENCY.updateInt(selected)
                    sponsorDialogFrequency = selected
                    showSponsorFrequencyDialog = false
                },
            )
        }

        if (showNetworkDialog) {
            NetworkTypeDialog(
                currentSelection = networkTypeRestriction,
                onDismissRequest = { showNetworkDialog = false },
                onConfirm = { selectedType ->
                    NETWORK_TYPE_RESTRICTION.updateInt(selectedType)
                    networkTypeRestriction = selectedType
                    showNetworkDialog = false
                }
            )
        }
    }
}

@Composable
private fun NetworkTypeDialog(
    currentSelection: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentSelection) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.network_type_restriction)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.network_type_restriction_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.any_network),
                    selected = selectedType == NETWORK_ANY,
                    onClick = { selectedType = NETWORK_ANY }
                )
                
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.wifi_only),
                    selected = selectedType == NETWORK_WIFI_ONLY,
                    onClick = { selectedType = NETWORK_WIFI_ONLY }
                )
                
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.mobile_only),
                    selected = selectedType == NETWORK_MOBILE_ONLY,
                    onClick = { selectedType = NETWORK_MOBILE_ONLY }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(selectedType) }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
@Composable
private fun SponsorFrequencyDialog(
    currentSelection: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selected by remember { mutableStateOf(currentSelection) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.sponsor_dialog_frequency_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.sponsor_dialog_frequency_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.sponsor_dialog_off),
                    selected = selected == SPONSOR_FREQ_OFF,
                    onClick = { selected = SPONSOR_FREQ_OFF },
                )
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.sponsor_dialog_weekly),
                    selected = selected == SPONSOR_FREQ_WEEKLY,
                    onClick = { selected = SPONSOR_FREQ_WEEKLY },
                )
                PreferenceSingleChoiceItem(
                    text = stringResource(R.string.sponsor_dialog_monthly),
                    selected = selected == SPONSOR_FREQ_MONTHLY,
                    onClick = { selected = SPONSOR_FREQ_MONTHLY },
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}