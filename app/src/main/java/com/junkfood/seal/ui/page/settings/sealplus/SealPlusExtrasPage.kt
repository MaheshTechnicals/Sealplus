package com.junkfood.seal.ui.page.settings.sealplus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkCell
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.booleanState
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSingleChoiceItem
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.util.NETWORK_ANY
import com.junkfood.seal.util.NETWORK_MOBILE_ONLY
import com.junkfood.seal.util.NETWORK_TYPE_RESTRICTION
import com.junkfood.seal.util.NETWORK_WIFI_ONLY
import com.junkfood.seal.util.NOTIFICATION_ERROR_SOUND
import com.junkfood.seal.util.NOTIFICATION_LED
import com.junkfood.seal.util.NOTIFICATION_SOUND
import com.junkfood.seal.util.NOTIFICATION_SUCCESS_SOUND
import com.junkfood.seal.util.NOTIFICATION_VIBRATE
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealPlusExtrasPage(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var networkTypeRestriction by remember { mutableStateOf(NETWORK_TYPE_RESTRICTION.getInt()) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    
    val notificationSound by NOTIFICATION_SOUND.booleanState
    val notificationVibrate by NOTIFICATION_VIBRATE.booleanState
    val notificationLed by NOTIFICATION_LED.booleanState
    val notificationSuccessSound by NOTIFICATION_SUCCESS_SOUND.booleanState
    val notificationErrorSound by NOTIFICATION_ERROR_SOUND.booleanState

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
                PreferenceSubtitle(text = stringResource(R.string.network_settings))
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
                    onClick = { NOTIFICATION_SOUND.updateBoolean(!notificationSound) }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_vibrate_settings),
                    description = stringResource(R.string.notification_vibrate_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationVibrate,
                    enabled = notificationSound,
                    onClick = { NOTIFICATION_VIBRATE.updateBoolean(!notificationVibrate) }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_led_settings),
                    description = stringResource(R.string.notification_led_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationLed,
                    enabled = notificationSound,
                    onClick = { NOTIFICATION_LED.updateBoolean(!notificationLed) }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_success_sound_settings),
                    description = stringResource(R.string.notification_success_sound_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationSuccessSound,
                    enabled = notificationSound,
                    onClick = { NOTIFICATION_SUCCESS_SOUND.updateBoolean(!notificationSuccessSound) }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.notification_error_sound_settings),
                    description = stringResource(R.string.notification_error_sound_desc),
                    icon = Icons.Outlined.Notifications,
                    isChecked = notificationErrorSound,
                    enabled = notificationSound,
                    onClick = { NOTIFICATION_ERROR_SOUND.updateBoolean(!notificationErrorSound) }
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
