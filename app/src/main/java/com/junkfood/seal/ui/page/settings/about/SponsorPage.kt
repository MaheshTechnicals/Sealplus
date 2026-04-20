package com.junkfood.seal.ui.page.settings.about

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.common.AsyncImageImpl
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.SealModalBottomSheet
import com.junkfood.seal.ui.component.SponsorItem
import com.junkfood.seal.ui.component.gitHubAvatar
import com.junkfood.seal.ui.component.gitHubProfile
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.util.makeToast
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.SHOW_SPONSOR_MSG
import com.junkfood.seal.util.Sponsor
import com.junkfood.seal.util.SocialAccount
import com.junkfood.seal.util.SocialAccounts
import com.junkfood.seal.util.SponsorEntity
import com.junkfood.seal.util.SponsorShip
import com.junkfood.seal.util.SponsorUtil
import com.junkfood.seal.util.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SponsorPage"

private const val SPONSORS = "Sponsors ☕️"
private const val BACKERS = "Backers ❤️"
private const val SUPPORTERS = "Supporters 💖"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubSponsorsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    val uriHandler = LocalUriHandler.current
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    val sponsorList = remember { mutableStateListOf<SponsorShip>() }
    val backerList = remember { mutableStateListOf<SponsorShip>() }
    val supporterList = remember { mutableStateListOf<SponsorShip>() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var viewingSponsorShip by remember {
        mutableStateOf(SponsorShip(sponsorEntity = SponsorEntity("login")))
    }
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val onSponsorClick: (SponsorShip) -> Unit = {
        viewingSponsorShip = it
        showSheet = true
        scope.launch {
            delay(80)
            sheetState.show()
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            SHOW_SPONSOR_MSG.updateInt(0)
            SponsorUtil.getSponsors()
                .onFailure { Log.e(TAG, "SponsorPage: ", it) }
                .onSuccess { response ->
                    val items = response.sponsors.map { sp ->
                        SponsorShip(
                            sponsorEntity = SponsorEntity(
                                login = sp.id.toString(),
                                name = sp.name,
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        sponsorList.addAll(items)
                    }
                }
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.sponsors))
                },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { values ->
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 12.dp),
                columns = GridCells.Fixed(12),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = values,
            ) {
                // Hero Header
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HeroHeader(isDarkTheme = isDarkTheme, isGradientDark = isGradientDark)
                }
                
                // About Seal Plus Section
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AboutSealPlusSection()
                }
                
                // Features Grid
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FeaturesSection()
                }
                
                // Why Sponsor Section
                item(span = { GridItemSpan(maxLineSpan) }) {
                    WhySponsorSection(isDarkTheme = isDarkTheme, isGradientDark = isGradientDark)
                }
                if (supporterList.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = SUPPORTERS) {
                        PreferenceSubtitle(
                            text = SUPPORTERS,
                            contentPadding =
                                PaddingValues(start = 12.dp, top = 24.dp, bottom = 12.dp),
                        )
                    }

                    items(
                        span = { GridItemSpan(maxLineSpan / 3) },
                        items = supporterList,
                        key = { it.sponsorEntity.login },
                    ) { sponsorShip ->
                        SponsorItem(sponsorShip = sponsorShip) { onSponsorClick(sponsorShip) }
                    }
                }

                if (backerList.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = BACKERS) {
                        PreferenceSubtitle(
                            text = BACKERS,
                            contentPadding =
                                PaddingValues(start = 12.dp, top = 12.dp, bottom = 12.dp),
                        )
                    }

                    items(
                        items = backerList,
                        span = { GridItemSpan(maxLineSpan / 3) },
                        key = { it.sponsorEntity.login },
                    ) { sponsorShip ->
                        SponsorItem(sponsorShip = sponsorShip) { onSponsorClick(sponsorShip) }
                    }
                }

                if (sponsorList.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = SPONSORS) {
                        PreferenceSubtitle(
                            text = SPONSORS,
                            contentPadding =
                                PaddingValues(start = 12.dp, top = 12.dp, bottom = 12.dp),
                        )
                    }

                    items(
                        items = sponsorList,
                        span = { GridItemSpan(maxLineSpan / 4) },
                        key = { it.sponsorEntity.login },
                    ) { sponsorShip ->
                        SponsorItem(sponsorShip = sponsorShip) { onSponsorClick(sponsorShip) }
                    }
                }

                // Developer Message and Sponsor Button
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DeveloperMessageSection(
                        uriHandler = uriHandler,
                        isDarkTheme = isDarkTheme,
                        isGradientDark = isGradientDark
                    )
                }
            }
            if (showSheet) {
                SponsorDialog(sponsorShip = viewingSponsorShip, sheetState = sheetState) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
                }
            }
        },
    )
}

// New Enhanced UI Components
@Composable
fun HeroHeader(isDarkTheme: Boolean, isGradientDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isDarkTheme && isGradientDark) {
                    GradientBrushes.Primary
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                }
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isDarkTheme && isGradientDark) 
                    Color.White 
                else 
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Support Seal Plus",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isDarkTheme && isGradientDark) 
                    Color.White 
                else 
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Help keep this project free, open-source, and actively maintained",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (isDarkTheme && isGradientDark) 
                    Color.White.copy(alpha = 0.9f) 
                else 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun AboutSealPlusSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "About Seal Plus",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Text(
                text = "Seal Plus is the enhanced premium edition of the popular Seal video downloader. Built with modern Android architecture and featuring an exclusive gradient dark theme, it's your ultimate companion for downloading videos and audio from 1000+ platforms.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Downloads", value = "100K+")
                StatItem(label = "Platforms", value = "1000+")
                StatItem(label = "Rating", value = "★ 4.8")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FeaturesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "What Your Support Enables",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                icon = Icons.Outlined.Speed,
                title = "Performance",
                description = "Optimized downloads with aria2c",
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                icon = Icons.Outlined.Palette,
                title = "Premium UI",
                description = "Exclusive gradient themes",
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                icon = Icons.Outlined.CloudDownload,
                title = "Auto Updates",
                description = "Latest features instantly",
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                icon = Icons.Outlined.Security,
                title = "Security",
                description = "Biometric app lock",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun WhySponsorSection(isDarkTheme: Boolean, isGradientDark: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Why Sponsor?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            BenefitItem(
                emoji = "🚀",
                title = "Faster Development",
                description = "Your support allows me to dedicate more time to developing new features and improvements"
            )
            
            BenefitItem(
                emoji = "🐛",
                title = "Quick Bug Fixes",
                description = "Priority support and faster resolution of issues reported by the community"
            )
            
            BenefitItem(
                emoji = "🎨",
                title = "Better UX",
                description = "Resources to design and implement more polished user interfaces and experiences"
            )
            
            BenefitItem(
                emoji = "🔒",
                title = "Always Free",
                description = "Your sponsorship ensures Seal Plus remains free and open-source for everyone"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDarkTheme && isGradientDark) {
                            GradientBrushes.Secondary
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                )
                            )
                        }
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "💙 Every contribution, big or small, makes a real difference and is deeply appreciated!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme && isGradientDark)
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BenefitItem(emoji: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun DeveloperMessageSection(
    uriHandler: androidx.compose.ui.platform.UriHandler,
    isDarkTheme: Boolean,
    isGradientDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Message from Developer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImageImpl(
                    model = gitHubAvatar("MaheshTechnicals"),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = if (isDarkTheme && isGradientDark) {
                                GradientBrushes.Primary
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            },
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Conversation(
                        modifier = Modifier,
                        text = stringResource(id = R.string.sponsor_msg)
                    )
                    Conversation(
                        modifier = Modifier,
                        text = stringResource(R.string.sponsor_msg2)
                    )
                }
            }
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        uriHandler.openUri("https://github.com/sponsors/MaheshTechnicals")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VolunteerActivism,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Become a Sponsor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                FilledTonalButton(
                    onClick = {
                        uriHandler.openUri("https://github.com/MaheshTechnicals/Sealplus")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "⭐ Star on GitHub",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                Text(
                    text = "Even starring the repo helps spread the word! 🌟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
@Preview
fun SponsorPagePreview() {
    GitHubSponsorsPage {}
}

@Composable
fun Conversation(modifier: Modifier = Modifier, text: String) {
    Row(
        modifier =
            modifier
                .padding(horizontal = 12.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun LazyGridItemScope.SponsorItem(sponsorShip: SponsorShip, onClick: () -> Unit) {
    SponsorItem(
        modifier = Modifier,
        userName = sponsorShip.sponsorEntity.name,
        userLogin = sponsorShip.sponsorEntity.login,
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorDialog(sponsorShip: SponsorShip, sheetState: SheetState, onDismissRequest: () -> Unit) {
    val amount = sponsorShip.tier?.monthlyPriceInDollars ?: 0
    val tierText =
        if (amount in 5 until 10) {
            SPONSORS
        } else if (amount in 10 until 25) {
            BACKERS
        } else if (amount > 25) {
            SUPPORTERS
        } else {
            null
        }

    SealModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentPadding = PaddingValues(0.dp),
    ) {
        SponsorDialogContent(
            userLogin = sponsorShip.sponsorEntity.login,
            userName = sponsorShip.sponsorEntity.name,
            avatarUrl = gitHubAvatar(sponsorShip.sponsorEntity.login),
            tierText = tierText,
            website = sponsorShip.sponsorEntity.websiteUrl,
            socialLinks = sponsorShip.sponsorEntity.socialAccounts?.nodes?.map { it.url.toString() },
        )
    }
}

@Composable
fun SponsorDialogContent(
    userLogin: String,
    userName: String?,
    avatarUrl: String,
    tierText: String? = null,
    website: String? = null,
    socialLinks: List<String>? = null,
) {
    Column {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp)
                    .height(IntrinsicSize.Min)
        ) {
            AsyncImageImpl(
                modifier = Modifier.heightIn(max = 72.dp).aspectRatio(1f, true).clip(CircleShape),
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(vertical = 20.dp).padding(start = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = userName ?: "@$userLogin",
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tierText.toString(),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.HorizontalDivider()
            LinkItem(icon = Icons.Outlined.House, link = website ?: gitHubProfile(userLogin))
            socialLinks?.forEach { LinkItem(icon = Icons.Outlined.Link, link = it) }
        }
    }
}

@Composable
private fun LinkItem(modifier: Modifier = Modifier, icon: ImageVector, link: String) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler
                        .runCatching { openUri(link) }
                        .onFailure {
                            clipboardManager.setText(AnnotatedString(link))
                            context.makeToast(R.string.link_copied)
                        }
                }
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = link, style = MaterialTheme.typography.titleSmall)
    }
}

@Preview
@Composable
private fun SponsorDialogContentPreview() {

    val sponsorShip =
        SponsorShip(
            sponsorEntity =
                SponsorEntity(
                    "example",
                    "example",
                    "https://www.example.com",
                    socialAccounts =
                        SocialAccounts(
                            buildList {
                                repeat(4) {
                                    add(
                                        SocialAccount(
                                            displayName = "Example",
                                            url = "https://www.example.com",
                                        )
                                    )
                                }
                            }
                        ),
                ),
            tier = Tier(10),
        )

    SealTheme {
        Surface {
            SponsorDialogContent(
                userLogin = sponsorShip.sponsorEntity.login,
                userName = sponsorShip.sponsorEntity.name,
                avatarUrl = gitHubAvatar(sponsorShip.sponsorEntity.login),
                website = sponsorShip.sponsorEntity.websiteUrl,
                socialLinks =
                    sponsorShip.sponsorEntity.socialAccounts?.nodes?.map { it.url.toString() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SponsorDialogPreview() {
    val sheetState =
        with(LocalDensity.current) {
            SheetState(
                skipPartiallyExpanded = true,
                density = this,
                initialValue = SheetValue.Expanded,
            )
        }

    val sponsorShip =
        SponsorShip(
            sponsorEntity =
                SponsorEntity(
                    "example",
                    "example",
                    "https://www.example.com",
                    socialAccounts =
                        SocialAccounts(
                            buildList {
                                repeat(4) {
                                    add(
                                        SocialAccount(
                                            displayName = "Example",
                                            url = "https://www.example.com",
                                        )
                                    )
                                }
                            }
                        ),
                ),
            tier = Tier(10),
        )

    SponsorDialog(sponsorShip = sponsorShip, onDismissRequest = {}, sheetState = sheetState)
}
