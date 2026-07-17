package com.junkfood.seal.ui.page.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.page.settings.format.AudioQuickSettingsDialog
import com.junkfood.seal.ui.page.settings.format.VideoQuickSettingsDialog
import com.junkfood.seal.util.AUDIO_CONVERSION_FORMAT
import com.junkfood.seal.util.AUDIO_CONVERT
import com.junkfood.seal.util.AUDIO_FORMAT
import com.junkfood.seal.util.AUDIO_QUALITY
import com.junkfood.seal.util.DownloadType
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.EXTRACT_AUDIO
import com.junkfood.seal.util.FORMAT_COMPATIBILITY
import com.junkfood.seal.util.FORMAT_QUALITY
import com.junkfood.seal.util.HIGH
import com.junkfood.seal.util.LOW
import com.junkfood.seal.util.M4A
import com.junkfood.seal.util.MEDIUM
import com.junkfood.seal.util.NOT_SPECIFIED
import com.junkfood.seal.util.OPUS
import com.junkfood.seal.util.PreferenceStrings
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.RES_HIGHEST
import com.junkfood.seal.util.USE_CUSTOM_AUDIO_PRESET
import com.junkfood.seal.util.VIDEO_FORMAT
import com.junkfood.seal.util.VIDEO_QUALITY
import com.junkfood.seal.util.findURLsFromString
import com.junkfood.seal.util.makeToast
import org.koin.compose.koinInject

private object BatchColors {
    val Background = Color(0xFF09090B)
    val Surface = Color(0xFF14141A)
    val SurfaceVariant = Color(0xFF1A1A22)
    val Primary = Color(0xFF7C4DFF)
    val Border = Color(0xFF2D2D35)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB2B2B8)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
}

private val GradientBrush = Brush.horizontalGradient(
    colors = listOf(Color(0xFF7C4DFF), Color(0xFF9C6DFF)),
)

private data class QualityChipItem(
    val value: Int,
    val label: String,
    val subtitle: String = "",
    val icon: ImageVector? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchUrlImportPage(
    onNavigateBack: () -> Unit,
    downloader: DownloaderV2 = koinInject(),
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDarkMode = LocalDarkTheme.current.isDarkTheme()

    var urlText by remember { mutableStateOf("") }
    var selectedType by remember {
        mutableStateOf(
            if (preferences.extractAudio) DownloadType.Audio else DownloadType.Video
        )
    }
    var preferences by remember {
        mutableStateOf(DownloadUtil.DownloadPreferences.createFromPreferences())
    }
    var showLinksExpanded by remember { mutableStateOf(false) }
    var showVideoPresetDialog by remember { mutableStateOf(false) }
    var showAudioPresetDialog by remember { mutableStateOf(false) }

    val detectedUrls = remember(urlText) { findURLsFromString(urlText).distinct() }

    val bg = if (isDarkMode) BatchColors.Background else MaterialTheme.colorScheme.background
    val surface = if (isDarkMode) BatchColors.Surface else MaterialTheme.colorScheme.surface
    val surfaceVariant = if (isDarkMode) BatchColors.SurfaceVariant else MaterialTheme.colorScheme.surfaceVariant
    val border = if (isDarkMode) BatchColors.Border else MaterialTheme.colorScheme.outlineVariant
    val primary = if (isDarkMode) BatchColors.Primary else MaterialTheme.colorScheme.primary
    val textPrimary = if (isDarkMode) BatchColors.TextPrimary else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDarkMode) BatchColors.TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    val chipSelectedBg = if (isDarkMode) BatchColors.Primary.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.primaryContainer
    val chipSelectedBorder = if (isDarkMode) BatchColors.Primary
    else MaterialTheme.colorScheme.primary
    val chipUnselectedBorder = if (isDarkMode) BatchColors.Border
    else MaterialTheme.colorScheme.outlineVariant

    fun updatePreferences() {
        preferences = DownloadUtil.DownloadPreferences.createFromPreferences()
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            Column {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onNavigateBack)
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            text = stringResource(R.string.batch_url_import),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                            ),
                            color = textPrimary,
                        )
                        Text(
                            text = "Download multiple links together",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                        )
                    }
                    IconButton(onClick = { /* Help: TBD */ }) {
                        Icon(
                            Icons.Outlined.HelpOutline,
                            contentDescription = "Help",
                            tint = textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = border.copy(alpha = 0.5f))
            }
        },
        bottomBar = {
            Surface(color = bg, shadowElevation = 8.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = {
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
                            context.makeToast(context.getString(R.string.task_added))
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        enabled = detectedUrls.isNotEmpty(),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = surfaceVariant,
                        ),
                    ) {
                        val bgMod = if (detectedUrls.isNotEmpty()) {
                            Modifier.background(GradientBrush, RoundedCornerShape(32.dp))
                        } else {
                            Modifier
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().then(bgMod),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (detectedUrls.isNotEmpty()) Color.White
                                    else textSecondary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (detectedUrls.isEmpty()) stringResource(R.string.download)
                                    else stringResource(R.string.download_task_count, detectedUrls.size),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = if (detectedUrls.isNotEmpty()) Color.White
                                    else textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── URL Input Card ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = surface,
                border = BorderStroke(1.dp, border.copy(alpha = 0.6f)),
                tonalElevation = 0.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.video_url),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = {
                            if (it.length <= 200) urlText = it
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        placeholder = { Text("Paste one or more URLs here...", color = textSecondary.copy(alpha = 0.5f)) },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = border.copy(alpha = 0.4f),
                            cursorColor = primary,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = bg,
                            unfocusedContainerColor = bg,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { /* dismiss keyboard */ }),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "${urlText.length}/200",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Paste Button ──
            Button(
                onClick = {
                    clipboardManager.getText()?.let {
                        urlText = findURLsFromString(it.toString()).joinToString("\n")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(GradientBrush, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Paste from Clipboard",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Selection Info (Links count) ──
            Surface(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(20.dp),
                color = surface,
                border = BorderStroke(1.dp, border.copy(alpha = 0.6f)),
                tonalElevation = 0.dp,
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showLinksExpanded = !showLinksExpanded },
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(chipSelectedBg, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = primary,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${detectedUrls.size} Links Selected",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = textPrimary,
                                )
                            }
                        }
                        if (detectedUrls.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (detectedUrls.size > 1) {
                                    Text(
                                        text = if (showLinksExpanded) "Collapse" else "View All",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            urlText = ""
                                            showLinksExpanded = false
                                        }
                                        .padding(8.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.clear),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDarkMode) BatchColors.Warning else textSecondary,
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showLinksExpanded && detectedUrls.size > 1,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                            HorizontalDivider(color = border.copy(alpha = 0.4f))
                            Spacer(Modifier.height(8.dp))
                            detectedUrls.forEachIndexed { index, url ->
                                Text(
                                    text = "${index + 1}. $url",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (index < detectedUrls.lastIndex) {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Download Type ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    DownloadType.Video to (Icons.Outlined.PlayCircle to "Download video"),
                    DownloadType.Audio to (Icons.Outlined.MusicNote to "Extract audio"),
                ).forEach { (type, pair) ->
                    val (icon, desc) = pair
                    val isSelected = selectedType == type
                    Surface(
                        modifier = Modifier.weight(1f).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                selectedType = type
                                EXTRACT_AUDIO.updateBoolean(type == DownloadType.Audio)
                                updatePreferences()
                            },
                        ),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) chipSelectedBg else surface,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) chipSelectedBorder else chipUnselectedBorder,
                        ),
                        tonalElevation = 0.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (isSelected) primary else textSecondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = when (type) {
                                    DownloadType.Video -> stringResource(R.string.video)
                                    DownloadType.Audio -> stringResource(R.string.audio)
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) textPrimary else textSecondary,
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Format Selection ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surface,
                border = BorderStroke(1.dp, border.copy(alpha = 0.6f)),
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (selectedType == DownloadType.Video) Icons.Outlined.HighQuality
                        else Icons.Outlined.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedType == DownloadType.Video) {
                                PreferenceStrings.getVideoPresetText(preferences)
                            } else {
                                PreferenceStrings.getAudioPresetText(preferences)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = textPrimary,
                        )
                        Text(
                            text = if (selectedType == DownloadType.Video) {
                                stringResource(R.string.video_format_preference)
                            } else {
                                stringResource(R.string.audio_format)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                when (selectedType) {
                                    DownloadType.Video -> showVideoPresetDialog = true
                                    DownloadType.Audio -> showAudioPresetDialog = true
                                    else -> {}
                                }
                            }
                            .padding(10.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(20.dp),
                            tint = primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Video / Audio Quality Chips ──
            if (selectedType == DownloadType.Video) {
                Text(
                    text = stringResource(R.string.video_resolution),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val chips = listOf(
                        QualityChipItem(RES_HIGHEST, "Best", "Auto", Icons.Outlined.LocalFireDepartment),
                        QualityChipItem(1, "2160P", "4K"),
                        QualityChipItem(2, "1440P", "2K"),
                        QualityChipItem(3, "1080P", "FHD"),
                        QualityChipItem(4, "720P", "HD"),
                        QualityChipItem(5, "480P", "SD"),
                    )
                    chips.forEach { chip ->
                        val sel = preferences.videoResolution == chip.value
                        QualityChip(
                            selected = sel,
                            title = chip.label,
                            subtitle = chip.subtitle,
                            icon = chip.icon,
                            chipSelectedBg = chipSelectedBg,
                            chipSelectedBorder = chipSelectedBorder,
                            chipUnselectedBorder = chipUnselectedBorder,
                            primary = primary,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onClick = {
                                VIDEO_QUALITY.updateInt(chip.value)
                                updatePreferences()
                            },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Preferred Video Format ──
                Text(
                    text = stringResource(R.string.video_format_preference),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        selected = preferences.videoFormat == FORMAT_QUALITY,
                        title = "Quality",
                        subtitle = "Recommended",
                        chipSelectedBorder = chipSelectedBorder,
                        chipUnselectedBorder = chipUnselectedBorder,
                        chipSelectedBg = chipSelectedBg,
                        surface = surface,
                        primary = primary,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = {
                            VIDEO_FORMAT.updateInt(FORMAT_QUALITY)
                            updatePreferences()
                        },
                    )
                    OptionCard(
                        selected = preferences.videoFormat == FORMAT_COMPATIBILITY,
                        title = "Legacy",
                        subtitle = "Compatibility",
                        chipSelectedBorder = chipSelectedBorder,
                        chipUnselectedBorder = chipUnselectedBorder,
                        chipSelectedBg = chipSelectedBg,
                        surface = surface,
                        primary = primary,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = {
                            VIDEO_FORMAT.updateInt(FORMAT_COMPATIBILITY)
                            updatePreferences()
                        },
                    )
                }
            } else {
                // ── Audio Quality ──
                Text(
                    text = stringResource(R.string.audio_quality),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val chips = listOf(
                        QualityChipItem(NOT_SPECIFIED, "Best", icon = Icons.Outlined.LocalFireDepartment),
                        QualityChipItem(HIGH, "192K"),
                        QualityChipItem(MEDIUM, "128K"),
                        QualityChipItem(LOW, "64K"),
                    )
                    chips.forEach { chip ->
                        val sel = preferences.audioQuality == chip.value
                        QualityChip(
                            selected = sel,
                            title = chip.label,
                            subtitle = chip.subtitle,
                            icon = chip.icon,
                            chipSelectedBg = chipSelectedBg,
                            chipSelectedBorder = chipSelectedBorder,
                            chipUnselectedBorder = chipUnselectedBorder,
                            primary = primary,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onClick = {
                                AUDIO_QUALITY.updateInt(chip.value)
                                USE_CUSTOM_AUDIO_PRESET.updateBoolean(chip.value != NOT_SPECIFIED)
                                updatePreferences()
                            },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Audio Format ──
                Text(
                    text = stringResource(R.string.audio_format),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        selected = preferences.audioFormat == OPUS,
                        title = "OPUS",
                        subtitle = "",
                        chipSelectedBorder = chipSelectedBorder,
                        chipUnselectedBorder = chipUnselectedBorder,
                        chipSelectedBg = chipSelectedBg,
                        surface = surface,
                        primary = primary,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = {
                            AUDIO_FORMAT.updateInt(OPUS)
                            USE_CUSTOM_AUDIO_PRESET.updateBoolean(true)
                            updatePreferences()
                        },
                    )
                    OptionCard(
                        selected = preferences.audioFormat == M4A,
                        title = "M4A",
                        subtitle = "",
                        chipSelectedBorder = chipSelectedBorder,
                        chipUnselectedBorder = chipUnselectedBorder,
                        chipSelectedBg = chipSelectedBg,
                        surface = surface,
                        primary = primary,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = {
                            AUDIO_FORMAT.updateInt(M4A)
                            USE_CUSTOM_AUDIO_PRESET.updateBoolean(true)
                            updatePreferences()
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Download Summary ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surface,
                border = BorderStroke(1.dp, border.copy(alpha = 0.6f)),
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipSelectedBg, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.format_selection),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = textPrimary,
                        )
                        Text(
                            text = if (selectedType == DownloadType.Video)
                                "${PreferenceStrings.getVideoResolutionDesc(preferences.videoResolution)} · ${
                                    if (preferences.videoFormat == FORMAT_QUALITY) "MP4"
                                    else "WebM"
                                } · ${PreferenceStrings.getAudioQualityDesc(preferences.audioQuality)}"
                            else
                                "${if (preferences.audioFormat == OPUS) "OPUS" else "M4A"} · ${
                                    PreferenceStrings.getAudioQualityDesc(preferences.audioQuality)
                                }",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Footer ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Triple(Icons.Outlined.Shield, "Secure", BatchColors.Success),
                    Triple(Icons.Outlined.Bolt, "Fast", BatchColors.Warning),
                    Triple(Icons.Outlined.CheckCircle, "Best Quality", BatchColors.Primary),
                ).forEach { (icn, lbl, clr) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = surface,
                        border = BorderStroke(1.dp, border.copy(alpha = 0.4f)),
                        tonalElevation = 0.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = icn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = clr,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Dialogs ──
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
                updatePreferences()
            },
        )
    }

    if (showAudioPresetDialog) {
        var quality by remember(preferences) { mutableIntStateOf(preferences.audioQuality) }
        var customPreset by remember(preferences) { mutableStateOf(preferences.useCustomAudioPreset) }
        var conversionFmt by remember(preferences) { mutableIntStateOf(preferences.audioConvertFormat) }
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
                updatePreferences()
            },
        )
    }
}

// ── Reusable Components ──

@Composable
private fun QualityChip(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector?,
    chipSelectedBg: Color,
    chipSelectedBorder: Color,
    chipUnselectedBorder: Color,
    primary: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) chipSelectedBg else Color.Transparent,
        border = BorderStroke(
            1.5.dp,
            if (selected) chipSelectedBorder else chipUnselectedBorder,
        ),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) primary else textSecondary,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) textPrimary else textSecondary,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun OptionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    chipSelectedBorder: Color,
    chipUnselectedBorder: Color,
    chipSelectedBg: Color,
    surface: Color,
    primary: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) chipSelectedBg else surface,
        border = BorderStroke(
            1.5.dp,
            if (selected) chipSelectedBorder else chipUnselectedBorder,
        ),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) primary else chipUnselectedBorder,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (selected) textPrimary else textSecondary,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
