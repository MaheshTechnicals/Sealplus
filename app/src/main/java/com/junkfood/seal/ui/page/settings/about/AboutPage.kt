package com.junkfood.seal.ui.page.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.UpdateDisabled
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.packageInfo
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.util.AUTO_UPDATE
import com.junkfood.seal.util.PreferenceUtil

private const val releaseURL = "https://github.com/MaheshTechnicals/Sealplus/releases"
private const val repoUrl = "https://github.com/MaheshTechnicals/Sealplus/blob/main/README.md"
const val weblate = "https://hosted.weblate.org/engage/seal/"
const val YtdlpRepository = "https://github.com/yt-dlp/yt-dlp"
private const val githubIssueUrl = "https://github.com/MaheshTechnicals/Sealplus/issues"
private const val telegramChannelUrl = "https://t.me/maheshtechnicals"
private const val youtubeChannelUrl = "https://youtube.com/@maheshtechnicals"
private const val githubSponsor = "https://github.com/sponsors/JunkFood02"
private const val TAG = "AboutPage"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateBack: () -> Unit,
    onNavigateToCreditsPage: () -> Unit,
    onNavigateToUpdatePage: () -> Unit,
    onNavigateToDonatePage: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    val context = LocalContext.current
    var isAutoUpdateEnabled by remember { mutableStateOf(PreferenceUtil.isAutoUpdateEnabled()) }

    val uriHandler = LocalUriHandler.current
    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(modifier = Modifier, text = stringResource(id = R.string.about)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(it),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.auto_update),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.check_for_updates_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.8f
                                        ),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isAutoUpdateEnabled,
                                onCheckedChange = {
                                    isAutoUpdateEnabled = !isAutoUpdateEnabled
                                    PreferenceUtil.updateValue(AUTO_UPDATE, isAutoUpdateEnabled)
                                },
                                enabled = !App.isFDroidBuild(),
                            )
                        }
                    }
                }

                item {
                    Card(
                        onClick = onNavigateToOnboarding,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "View Onboarding",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "See the introduction screens again",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.8f
                                        ),
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CommunityCard(
                                title = stringResource(R.string.readme),
                                description = stringResource(R.string.readme_desc),
                                icon = Icons.Outlined.Description,
                                onClick = { openUrl(repoUrl) },
                                modifier = Modifier.weight(1f),
                            )
                            CommunityCard(
                                title = stringResource(R.string.release),
                                description = stringResource(R.string.release_desc),
                                icon = Icons.Outlined.NewReleases,
                                onClick = { openUrl(releaseURL) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CommunityCard(
                                title = stringResource(R.string.sponsor),
                                description = stringResource(R.string.sponsor_desc),
                                icon = Icons.Outlined.VolunteerActivism,
                                onClick = onNavigateToDonatePage,
                                modifier = Modifier.weight(1f),
                            )
                            CommunityCard(
                                title = stringResource(R.string.telegram_channel),
                                description = telegramChannelUrl,
                                icon = painterResource(id = R.drawable.icons8_telegram_app),
                                onClick = { openUrl(telegramChannelUrl) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CommunityCard(
                                title = stringResource(R.string.youtube_channel),
                                description = youtubeChannelUrl,
                                icon = painterResource(id = R.drawable.icons8_youtube),
                                onClick = { openUrl(youtubeChannelUrl) },
                                modifier = Modifier.weight(1f),
                            )
                            CommunityCard(
                                title = stringResource(R.string.website),
                                description = "sealplus.in",
                                icon = Icons.Outlined.Public,
                                onClick = { openUrl("https://sealplus.in/") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CommunityCard(
                                title = stringResource(R.string.credits),
                                description = stringResource(R.string.credits_desc),
                                icon = Icons.Outlined.AutoAwesome,
                                onClick = onNavigateToCreditsPage,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        text = "Version ${packageInfo.versionName ?: ""}  \u2022  ${context.packageName}",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    )
}

@Composable
private fun CommunityCard(
    title: String,
    description: String,
    icon: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            when (icon) {
                is ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                is Painter -> {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
@Preview
fun AutoUpdateUnavailableDialog(onDismissRequest: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val hapticFeedback = LocalHapticFeedback.current
    val hyperLinkText = stringResource(id = R.string.switch_to_github_builds)
    val text = stringResource(id = R.string.auto_update_disabled_msg, "F-Droid", hyperLinkText)

    val annotatedString = buildAnnotatedString {
        append(text)
        val startIndex = text.indexOf(hyperLinkText)
        val endIndex = startIndex + hyperLinkText.length
        addUrlAnnotation(
            UrlAnnotation("https://github.com/MaheshTechnicals/Sealplus/releases/latest"),
            start = startIndex,
            end = endIndex,
        )
        addStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.tertiary,
                textDecoration = TextDecoration.Underline,
            ),
            start = startIndex,
            end = endIndex,
        )
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton(stringResource(id = R.string.got_it)) { onDismissRequest() }
        },
        icon = { Icon(Icons.Outlined.UpdateDisabled, null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(
                text = stringResource(id = R.string.feature_unavailable),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            ClickableText(
                text = annotatedString,
                onClick = { index ->
                    annotatedString.getUrlAnnotations(index, index).firstOrNull()?.let {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        uriHandler.openUri(it.item.url)
                    }
                },
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        MaterialTheme.colorScheme.onSurfaceVariant
                    ),
            )
        },
    )
}
