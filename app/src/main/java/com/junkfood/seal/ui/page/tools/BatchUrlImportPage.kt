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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HighQuality
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
import androidx.compose.material3.OutlinedButton
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
    val SurfaceVariant = Color(0xFF1E1E28)
    val Primary = Color(0xFF7C4DFF)
    val Border = Color(0xFF2A2A35)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9E9EAB)
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
    var preferences by remember {
        mutableStateOf(DownloadUtil.DownloadPreferences.createFromPreferences())
    }
    var selectedType by remember {
        mutableStateOf(
            if (preferences.extractAudio) DownloadType.Audio else DownloadType.Video
        )
    }
    var showLinksExpanded by remember { mutableStateOf(true) }
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

    val chipSelectedBg = if (isDarkMode) BatchColors.Primary.copy(alpha = 0.12f)
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onNavigateBack)
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.batch_url_import),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary,
                        )
                        Text(
                            text = "Download multiple links",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                        )
                    }
                }
                HorizontalDivider(color = border.copy(alpha = 0.4f))
            }
        },
        bottomBar = {
            Surface(color = bg) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = detectedUrls.isNotEmpty(),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = surfaceVariant,
                        ),
                    ) {
                        val bgMod = if (detectedUrls.isNotEmpty()) {
                            Modifier.background(
                                GradientBrush,
                                RoundedCornerShape(26.dp),
                            )
                        } else {
                            Modifier
                        } as Modifier
                        Box(
                            modifier = Modifier.fillMaxSize().then(bgMod),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (detectedUrls.isNotEmpty()) Color.White else textSecondary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (detectedUrls.isEmpty()) stringResource(R.string.download)
                                    else stringResource(R.string.download_task_count, detectedUrls.size),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = if (detectedUrls.isNotEmpty()) Color.White else textSecondary,
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
            Spacer(Modifier.height(12.dp))

            // ── URL Input + Paste inline ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = surface,
                border = BorderStroke(1.dp, border.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { if (it.length <= 200) urlText = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        placeholder = {
                            Text(
                                "Paste links here...",
                                color = textSecondary.copy(alpha = 0.5f),
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primary.copy(alpha = 0.4f),
                            unfocusedBorderColor = border.copy(alpha = 0.3f),
                            cursorColor = primary,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = bg,
                            unfocusedContainerColor = bg,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {}),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = primary,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${urlText.length}/200",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary.copy(alpha = 0.6f),
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboardManager.getText()?.let {
                                    urlText = findURLsFromString(it.toString()).joinToString("\n")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = primary,
                            ),
                            border = BorderStroke(1.dp, primary.copy(alpha = 0.4f)),
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) {
                            Icon(
                                Icons.Outlined.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Paste",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Download Type ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) chipSelectedBg else surface,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) chipSelectedBorder else chipUnselectedBorder,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isSelected) primary else textSecondary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
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
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondary.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Selection Info ──
            if (detectedUrls.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(14.dp),
                    color = surface,
                    border = BorderStroke(1.dp, border.copy(alpha = 0.5f)),
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
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipSelectedBg, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = primary,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${detectedUrls.size} link${if (detectedUrls.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = textPrimary,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (detectedUrls.size > 1) {
                                    Text(
                                        text = if (showLinksExpanded) "Collapse" else "View all",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                urlText = ""
                                                showLinksExpanded = true
                                            },
                                        )
                                        .padding(6.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.clear),
                                        modifier = Modifier.size(14.dp),
                                        tint = textSecondary,
                                    )
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = showLinksExpanded && detectedUrls.size > 1,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                                HorizontalDivider(color = border.copy(alpha = 0.3f))
                                Spacer(Modifier.height(6.dp))
                                detectedUrls.forEachIndexed { index, url ->
                                    Text(
                                        text = "${index + 1}. $url",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (index < detectedUrls.lastIndex) {
                                        Spacer(Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Format Selection Card ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = surface,
                border = BorderStroke(1.dp, border.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedType == DownloadType.Video) Icons.Outlined.HighQuality
                            else Icons.Outlined.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedType == DownloadType.Video) "Video Settings"
                            else "Audio Settings",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        when (selectedType) {
                                            DownloadType.Video -> showVideoPresetDialog = true
                                            DownloadType.Audio -> showAudioPresetDialog = true
                                            else -> {}
                                        }
                                    },
                                )
                                .padding(6.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.edit),
                                modifier = Modifier.size(16.dp),
                                tint = primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = border.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))

                    // ── Video / Audio Quality ──
                    if (selectedType == DownloadType.Video) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Resolution",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = textSecondary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Row(
                                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val chips = listOf(
                                    QualityChipItem(RES_HIGHEST, "Best", icon = Icons.Outlined.LocalFireDepartment),
                                    QualityChipItem(1, "4K"),
                                    QualityChipItem(2, "1440p"),
                                    QualityChipItem(3, "1080p"),
                                    QualityChipItem(4, "720p"),
                                    QualityChipItem(5, "480p"),
                                )
                                chips.forEach { chip ->
                                    val sel = preferences.videoResolution == chip.value
                                    CompactChip(
                                        selected = sel,
                                        title = chip.label,
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
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Format",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = textSecondary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    FORMAT_QUALITY to "Quality (Recommended)",
                                    FORMAT_COMPATIBILITY to "Legacy",
                                ).forEach { (value, label) ->
                                    val sel = preferences.videoFormat == value
                                    CompactRadioChip(
                                        selected = sel,
                                        label = label,
                                        chipSelectedBg = chipSelectedBg,
                                        chipSelectedBorder = chipSelectedBorder,
                                        chipUnselectedBorder = chipUnselectedBorder,
                                        primary = primary,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onClick = {
                                            VIDEO_FORMAT.updateInt(value)
                                            updatePreferences()
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Quality",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = textSecondary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Row(
                                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val chips = listOf(
                                    QualityChipItem(NOT_SPECIFIED, "Best", icon = Icons.Outlined.LocalFireDepartment),
                                    QualityChipItem(HIGH, "192K"),
                                    QualityChipItem(MEDIUM, "128K"),
                                    QualityChipItem(LOW, "64K"),
                                )
                                chips.forEach { chip ->
                                    val sel = preferences.audioQuality == chip.value
                                    CompactChip(
                                        selected = sel,
                                        title = chip.label,
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
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Format",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = textSecondary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    OPUS to "OPUS",
                                    M4A to "M4A",
                                ).forEach { (value, label) ->
                                    val sel = preferences.audioFormat == value
                                    CompactRadioChip(
                                        selected = sel,
                                        label = label,
                                        chipSelectedBg = chipSelectedBg,
                                        chipSelectedBorder = chipSelectedBorder,
                                        chipUnselectedBorder = chipUnselectedBorder,
                                        primary = primary,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        onClick = {
                                            AUDIO_FORMAT.updateInt(value)
                                            USE_CUSTOM_AUDIO_PRESET.updateBoolean(true)
                                            updatePreferences()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Download Summary ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = chipSelectedBg,
                border = BorderStroke(1.dp, chipSelectedBorder.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.HighQuality,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (selectedType == DownloadType.Video) {
                            "${PreferenceStrings.getVideoResolutionDesc(preferences.videoResolution)} · ${
                                if (preferences.videoFormat == FORMAT_QUALITY) "MP4" else "WebM"
                            }"
                        } else {
                            "${if (preferences.audioFormat == OPUS) "OPUS" else "M4A"} · ${
                                PreferenceStrings.getAudioQualityDesc(preferences.audioQuality)
                            }"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = textPrimary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Footer Badges ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    Triple(Icons.Outlined.Shield, "Secure", BatchColors.Success),
                    Triple(Icons.Outlined.Bolt, "Fast", BatchColors.Warning),
                    Triple(Icons.Outlined.CheckCircle, "Best Quality", BatchColors.Primary),
                ).forEach { (icn, lbl, clr) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = chipSelectedBg,
                        border = BorderStroke(1.dp, border.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = icn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = clr,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = lbl,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
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

// ── Compact Chip ──
@Composable
private fun CompactChip(
    selected: Boolean,
    title: String,
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
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) chipSelectedBg else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) chipSelectedBorder else chipUnselectedBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (selected) primary else textSecondary,
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = if (selected) textPrimary else textSecondary,
            )
        }
    }
}

// ── Compact Radio Chip ──
@Composable
private fun CompactRadioChip(
    selected: Boolean,
    label: String,
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
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) chipSelectedBg else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) chipSelectedBorder else chipUnselectedBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
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
                        modifier = Modifier.size(8.dp),
                        tint = Color.White,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = if (selected) textPrimary else textSecondary,
            )
        }
    }
}
