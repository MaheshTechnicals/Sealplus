package com.junkfood.seal.ui.page.settings.sealplus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NetworkCell
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
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
import androidx.fragment.app.FragmentActivity
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSingleChoiceItem
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.page.security.LockScreen
import com.junkfood.seal.util.AuthenticationManager
import com.junkfood.seal.util.MAX_CONCURRENT_DOWNLOADS
import com.junkfood.seal.util.NETWORK_ANY
import com.junkfood.seal.util.NETWORK_MOBILE_ONLY
import com.junkfood.seal.util.NETWORK_TYPE_RESTRICTION
import com.junkfood.seal.util.NETWORK_WIFI_ONLY
import com.junkfood.seal.util.NOTIFICATION_ERROR_SOUND
import com.junkfood.seal.util.NOTIFICATION_LED
import com.junkfood.seal.util.NOTIFICATION_SOUND
import com.junkfood.seal.util.NOTIFICATION_SUCCESS_SOUND
import com.junkfood.seal.util.NOTIFICATION_VIBRATE
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealPlusExtrasPage(
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToProxySettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var networkTypeRestriction by remember { mutableStateOf(NETWORK_TYPE_RESTRICTION.getInt()) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    
    var notificationSound by remember { mutableStateOf(NOTIFICATION_SOUND.getBoolean()) }
    var notificationVibrate by remember { mutableStateOf(NOTIFICATION_VIBRATE.getBoolean()) }
    var notificationLed by remember { mutableStateOf(NOTIFICATION_LED.getBoolean()) }
    var notificationSuccessSound by remember { mutableStateOf(NOTIFICATION_SUCCESS_SOUND.getBoolean()) }
    var notificationErrorSound by remember { mutableStateOf(NOTIFICATION_ERROR_SOUND.getBoolean()) }
    
    // Authentication state for AppLock settings
    var showAuthScreen by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(false) }

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
                            text = "10",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                PreferenceSubtitle(text = stringResource(R.string.network_settings))
            }

            item {
                PreferenceItem(
                    title = stringResource(R.string.proxy_settings),
                    description = stringResource(R.string.proxy_toggle_description),
                    icon = Icons.Outlined.Public,
                    onClick = { onNavigateToProxySettings() }
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
