package com.junkfood.seal.ui.page.settings.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.booleanState
import com.junkfood.seal.ui.common.intState
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DialogSingleChoiceItemVariant
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.util.PreferenceStrings.getUpdateIntervalText
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.junkfood.seal.util.UpdateIntervalList
import com.junkfood.seal.util.YT_DLP_AUTO_UPDATE
import com.junkfood.seal.util.YT_DLP_NIGHTLY
import com.junkfood.seal.util.YT_DLP_STABLE
import com.junkfood.seal.util.YT_DLP_UPDATE_CHANNEL
import com.junkfood.seal.util.YT_DLP_UPDATE_INTERVAL

/** Small pill that shows the channel kind. It never wraps, so it stays readable on any width. */
@Composable
private fun ChannelBadge(text: String, containerColor: Color) {
    Surface(
        modifier = Modifier.wrapContentWidth(),
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            text = text,
            color = MaterialTheme.colorScheme.contentColorFor(containerColor),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtdlpUpdateChannelDialog(modifier: Modifier = Modifier, onDismissRequest: () -> Unit) {
    var ytdlpUpdateChannel by YT_DLP_UPDATE_CHANNEL.intState
    var ytdlpAutoUpdate by YT_DLP_AUTO_UPDATE.booleanState
    var updateInterval by remember { mutableLongStateOf(YT_DLP_UPDATE_INTERVAL.getLong()) }
    var autoUpdateExpanded by remember { mutableStateOf(false) }

    SealDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton {
                YT_DLP_AUTO_UPDATE.updateBoolean(ytdlpAutoUpdate)
                YT_DLP_UPDATE_CHANNEL.updateInt(ytdlpUpdateChannel)
                YT_DLP_UPDATE_INTERVAL.updateLong(updateInterval)
                onDismissRequest()
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        title = { Text(text = stringResource(id = R.string.update)) },
        icon = { Icon(Icons.Outlined.SyncAlt, null, tint = MaterialTheme.colorScheme.primary) },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
            ) {
                SectionLabel(text = stringResource(id = R.string.update_channel))

                DialogSingleChoiceItemVariant(
                    title = "yt-dlp",
                    desc = "Recommended. Tested stable release.",
                    selected = ytdlpUpdateChannel == YT_DLP_STABLE,
                    action = {
                        ChannelBadge(
                            text = "Stable",
                            containerColor = MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    ytdlpUpdateChannel = YT_DLP_STABLE
                }

                DialogSingleChoiceItemVariant(
                    title = "yt-dlp nightly builds",
                    desc = "Newest fixes. May be less stable.",
                    selected = ytdlpUpdateChannel == YT_DLP_NIGHTLY,
                    action = {
                        ChannelBadge(
                            text = "Nightly",
                            containerColor = MaterialTheme.colorScheme.tertiary,
                        )
                    },
                ) {
                    ytdlpUpdateChannel = YT_DLP_NIGHTLY
                }

                SectionLabel(
                    text = stringResource(id = R.string.additional_settings),
                    topPadding = 20.dp,
                )

                ExposedDropdownMenuBox(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    expanded = autoUpdateExpanded,
                    onExpandedChange = { autoUpdateExpanded = it },
                ) {
                    OutlinedTextField(
                        value =
                            if (!ytdlpAutoUpdate) stringResource(id = R.string.disabled)
                            else getUpdateIntervalText(updateInterval),
                        onValueChange = {},
                        label = { Text(text = stringResource(id = R.string.auto_update)) },
                        readOnly = true,
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoUpdateExpanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = autoUpdateExpanded,
                        onDismissRequest = { autoUpdateExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.disabled)) },
                            onClick = {
                                ytdlpAutoUpdate = false
                                autoUpdateExpanded = false
                            },
                        )
                        for ((interval, stringId) in UpdateIntervalList) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = stringId)) },
                                onClick = {
                                    ytdlpAutoUpdate = true
                                    updateInterval = interval
                                    autoUpdateExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun SectionLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = topPadding + 8.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
