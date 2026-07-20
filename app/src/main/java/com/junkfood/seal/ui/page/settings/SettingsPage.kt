package com.junkfood.seal.ui.page.settings

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.SignalCellular4Bar
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.Route
import com.junkfood.seal.ui.common.intState
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferencesHintCard
import com.junkfood.seal.util.EXTRACT_AUDIO
import com.junkfood.seal.util.BatteryUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.SHOW_SPONSOR_MSG

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(onNavigateBack: () -> Unit, onNavigateTo: (String) -> Unit) {
    val context = LocalContext.current
    // NOTE: intentionally independent of the home screen's battery dialog cooldown — this hint
    // card is a low-friction, always-visible-when-relevant reminder that should show in
    // Settings for as long as battery optimization is actually still restricting the app,
    // recomputed fresh on every composition/resume (see the launcher callback below), not
    // gated by any dismissal history.
    var showBatteryHint by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !BatteryUtil.isIgnoringBatteryOptimizations(context)
            } else {
                false
            }
        )
    }
    val batteryIntent = remember { BatteryUtil.buildBatterySettingsIntent(context) }
    val isActivityAvailable: Boolean = remember {
        if (Build.VERSION.SDK_INT < 23) false
        else context.packageManager
            .resolveActivity(batteryIntent, 0) != null
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showBatteryHint = !BatteryUtil.isIgnoringBatteryOptimizations(context)
            }
        }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val showSponsorMessage by SHOW_SPONSOR_MSG.intState

    LaunchedEffect(Unit) { SHOW_SPONSOR_MSG.updateInt(showSponsorMessage + 1) }

    val typography = MaterialTheme.typography

    @Composable
    fun trailingChevron() {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val overrideTypography =
                remember(typography) { typography.copy(headlineMedium = typography.displaySmall) }

            MaterialTheme(typography = overrideTypography) {
                LargeTopAppBar(
                    title = { Text(text = stringResource(id = R.string.settings)) },
                    navigationIcon = { BackButton(onNavigateBack) },
                    scrollBehavior = scrollBehavior,
                    expandedHeight = TopAppBarDefaults.LargeAppBarExpandedHeight + 24.dp,
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = it,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                item {
                    AnimatedVisibility(
                        visible = showBatteryHint && isActivityAvailable,
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        PreferencesHintCard(
                            title = stringResource(R.string.battery_configuration),
                            icon = Icons.Rounded.EnergySavingsLeaf,
                            description = stringResource(R.string.battery_configuration_desc),
                        ) {
                            launcher.launch(batteryIntent)
                        }
                    }
                }
            }
            if (!showBatteryHint && showSponsorMessage > 30)
                item {
                    PreferencesHintCard(
                        title = stringResource(id = R.string.sponsor),
                        icon = Icons.Rounded.VolunteerActivism,
                        description = stringResource(id = R.string.sponsor_desc),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        onNavigateTo(Route.DONATE)
                    }
                }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.general_settings),
                    description = stringResource(id = R.string.general_settings_desc),
                    icon = Icons.Rounded.SettingsApplications,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.GENERAL_DOWNLOAD_PREFERENCES) },
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.download_directory),
                    description = stringResource(id = R.string.download_directory_desc),
                    icon = Icons.Rounded.Folder,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.DOWNLOAD_DIRECTORY) },
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.format),
                    description = stringResource(id = R.string.format_settings_desc),
                    icon =
                        if (EXTRACT_AUDIO.getBoolean()) Icons.Rounded.AudioFile
                        else Icons.Rounded.VideoFile,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.DOWNLOAD_FORMAT) },
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.network),
                    description = stringResource(id = R.string.network_settings_desc),
                    icon =
                        if (App.connectivityManager.isActiveNetworkMetered)
                            Icons.Rounded.SignalCellular4Bar
                        else Icons.Rounded.SignalWifi4Bar,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.NETWORK_PREFERENCES) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.custom_command),
                    description = stringResource(id = R.string.custom_command_desc),
                    icon = Icons.Rounded.Terminal,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.TEMPLATE) },
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.look_and_feel),
                    description = stringResource(id = R.string.display_settings),
                    icon = Icons.Rounded.Palette,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.APPEARANCE) },
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.interface_and_interaction),
                    description = stringResource(id = R.string.settings_before_download),
                    icon = Icons.Rounded.ViewComfy,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.INTERACTION) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                PreferenceItem(
                    title = stringResource(id = R.string.sealplus_extras),
                    description = stringResource(id = R.string.sealplus_extras_desc),
                    icon = Icons.Rounded.Stars,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.SEALPLUS_EXTRAS) },
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(R.string.trouble_shooting),
                    description = stringResource(R.string.trouble_shooting_desc),
                    icon = Icons.Rounded.BugReport,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.TROUBLESHOOTING) },
                )
            }
            item {
                PreferenceItem(
                    title = stringResource(id = R.string.about),
                    description = stringResource(id = R.string.about_page),
                    icon = Icons.Rounded.Info,
                    trailingIcon = { trailingChevron() },
                    onClick = { onNavigateTo(Route.ABOUT) },
                )
            }
        }
    }
}
