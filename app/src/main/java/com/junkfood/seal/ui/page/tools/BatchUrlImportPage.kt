package com.junkfood.seal.ui.page.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ButtonChip
import com.junkfood.seal.ui.component.DrawerSheetSubtitle
import com.junkfood.seal.ui.component.FilledButtonWithIcon
import com.junkfood.seal.ui.page.settings.format.AudioQuickSettingsDialog
import com.junkfood.seal.ui.page.settings.format.VideoQuickSettingsDialog
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.AUDIO_CONVERSION_FORMAT
import com.junkfood.seal.util.AUDIO_CONVERT
import com.junkfood.seal.util.AUDIO_FORMAT
import com.junkfood.seal.util.AUDIO_QUALITY
import com.junkfood.seal.util.DownloadType
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.EXTRACT_AUDIO
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.USE_CUSTOM_AUDIO_PRESET
import com.junkfood.seal.util.VIDEO_FORMAT
import com.junkfood.seal.util.VIDEO_QUALITY
import com.junkfood.seal.util.findURLsFromString
import com.junkfood.seal.util.makeToast
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchUrlImportPage(
    onNavigateBack: () -> Unit,
    downloader: DownloaderV2 = koinInject(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isGradientDark = LocalGradientDarkMode.current

    var urlText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DownloadType.Video) }
    var preferences by remember {
        mutableStateOf(DownloadUtil.DownloadPreferences.createFromPreferences())
    }
    var showVideoPresetDialog by remember { mutableStateOf(false) }
    var showAudioPresetDialog by remember { mutableStateOf(false) }

    val detectedUrls = remember(urlText) { findURLsFromString(urlText) }

    val containerColor = if (isGradientDark) GradientDarkColors.Background
    else MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = containerColor,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.batch_url_import)) },
                navigationIcon = { BackButton(onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                label = { Text(stringResource(R.string.video_url)) },
                placeholder = { Text("https://youtube.com/watch?v=...\nhttps://youtube.com/watch?v=...") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 15,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SuggestionChip(
                    onClick = {
                        clipboardManager.getText()?.let {
                            urlText = findURLsFromString(it.toString()).joinToString("\n")
                        }
                    },
                    label = { Text(stringResource(R.string.paste)) },
                    icon = {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                    },
                )
                if (urlText.isNotBlank()) {
                    SuggestionChip(
                        onClick = { urlText = "" },
                        label = { Text(stringResource(R.string.clear)) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.selected_item_count, detectedUrls.size),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isGradientDark) GradientDarkColors.OnSurface.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DrawerSheetSubtitle(text = stringResource(R.string.download_type))
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedType == DownloadType.Video,
                    onClick = {
                        selectedType = DownloadType.Video
                        EXTRACT_AUDIO.updateBoolean(false)
                        preferences = DownloadUtil.DownloadPreferences.createFromPreferences()
                    },
                    label = { Text(stringResource(R.string.video)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isGradientDark)
                            GradientDarkColors.GradientPrimaryStart.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
                FilterChip(
                    selected = selectedType == DownloadType.Audio,
                    onClick = {
                        selectedType = DownloadType.Audio
                        EXTRACT_AUDIO.updateBoolean(true)
                        preferences = DownloadUtil.DownloadPreferences.createFromPreferences()
                    },
                    label = { Text(stringResource(R.string.audio)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isGradientDark)
                            GradientDarkColors.GradientPrimaryStart.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            DrawerSheetSubtitle(text = stringResource(R.string.format_selection))
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ButtonChip(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(
                        if (selectedType == DownloadType.Video) R.string.video
                        else R.string.audio
                    ),
                    onClick = {
                        when (selectedType) {
                            DownloadType.Video -> showVideoPresetDialog = true
                            DownloadType.Audio -> showAudioPresetDialog = true
                            else -> {}
                        }
                    },
                )
            }

            Spacer(Modifier.height(24.dp))

            FilledButtonWithIcon(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                text = stringResource(
                    if (detectedUrls.isEmpty()) R.string.download
                    else R.string.download_task_count,
                    detectedUrls.size,
                ),
                enabled = detectedUrls.isNotEmpty(),
            ) {
                val urlsToDownload = detectedUrls.toList()
                urlsToDownload.forEach { url ->
                    downloader.enqueue(
                        Task(
                            url = url,
                            preferences = preferences.copy(
                                extractAudio = selectedType == DownloadType.Audio,
                            ),
                        )
                    )
                }
                context.makeToast(
                    context.getString(R.string.task_added)
                )
                onNavigateBack()
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showVideoPresetDialog) {
        var res by remember(preferences) { mutableIntStateOf(preferences.videoResolution) }
        var format by remember(preferences) { mutableIntStateOf(preferences.videoFormat) }

        VideoQuickSettingsDialog(
            videoResolution = res,
            videoFormatPreference = format,
            onResolutionSelect = { res = it },
            onFormatSelect = { format = it },
            onDismissRequest = { showVideoPresetDialog = false },
            onSave = {
                VIDEO_FORMAT.updateInt(format)
                VIDEO_QUALITY.updateInt(res)
                preferences = DownloadUtil.DownloadPreferences.createFromPreferences()
            },
        )
    }

    if (showAudioPresetDialog) {
        var quality by remember(preferences) { mutableIntStateOf(preferences.audioQuality) }
        var customPreset by
            remember(preferences) { mutableStateOf(preferences.useCustomAudioPreset) }
        var conversionFmt by
            remember(preferences) { mutableIntStateOf(preferences.audioConvertFormat) }
        var convertAudio by remember(preferences) { mutableStateOf(preferences.convertAudio) }
        var preferredFormat by remember(preferences) { mutableIntStateOf(preferences.audioFormat) }

        AudioQuickSettingsDialog(
            modifier = Modifier,
            preferences = preferences,
            audioQuality = quality,
            onQualitySelect = { quality = it },
            useCustomAudioPreset = customPreset,
            onCustomPresetToggle = { customPreset = it },
            convertAudio = convertAudio,
            onConvertToggled = { convertAudio = it },
            conversionFormat = conversionFmt,
            onConversionSelect = { conversionFmt = it },
            preferredFormat = preferredFormat,
            onPreferredSelect = { preferredFormat = it },
            onDismissRequest = { showAudioPresetDialog = false },
            onSave = {
                AUDIO_QUALITY.updateInt(quality)
                USE_CUSTOM_AUDIO_PRESET.updateBoolean(customPreset)
                AUDIO_CONVERSION_FORMAT.updateInt(conversionFmt)
                AUDIO_CONVERT.updateBoolean(convertAudio)
                AUDIO_FORMAT.updateInt(preferredFormat)
                preferences = DownloadUtil.DownloadPreferences.createFromPreferences()
            },
        )
    }
}
