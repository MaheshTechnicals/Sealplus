package com.junkfood.seal.ui.page.settings.about

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.Sponsor
import com.junkfood.seal.util.SponsorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SPONSORS_TAG = "SponsorsPage"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsPage(onNavigateBack: () -> Unit) {
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var sponsors by remember { mutableStateOf<List<Sponsor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Incrementing retryKey re-triggers the LaunchedEffect to retry the fetch
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryKey) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) { SponsorUtil.getSponsors() }
            .onSuccess { response ->
                // Reverse so the latest sponsors (added last in JSON) appear at the top
                sponsors = response.sponsors.reversed()
            }
            .onFailure { e ->
                Log.e(SPONSORS_TAG, "Error fetching sponsors", e)
                errorMessage = e.message ?: "Failed to load sponsors"
            }
        isLoading = false
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Our Sponsors",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Loading sponsors...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Failed to load sponsors",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { retryKey++ }) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header banner
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isDarkTheme && isGradientDark) GradientBrushes.Accent
                                        else Brush.linearGradient(
                                            colors = listOf(Color(0xFFEC4899), Color(0xFFF97316))
                                        )
                                    )
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "Our Amazing Sponsors",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (sponsors.isEmpty()) "Be the first to support!"
                                        else "${sponsors.size} supporter${if (sponsors.size == 1) "" else "s"} making this possible",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (sponsors.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Favorite,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "No sponsors yet",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Be the first to support!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(items = sponsors, key = { it.id }) { sponsor ->
                                SponsorListItem(
                                    sponsor = sponsor,
                                    isDarkTheme = isDarkTheme,
                                    isGradientDark = isGradientDark
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SponsorListItem(
    sponsor: Sponsor,
    isDarkTheme: Boolean,
    isGradientDark: Boolean
) {
    val avatarColor = remember(sponsor.name) { sponsorAvatarColor(sponsor.name) }
    val initials = remember(sponsor.name) {
        sponsor.name.split(" ").take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("").take(2)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme && isGradientDark)
                GradientDarkColors.SurfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = sponsor.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = if (isDarkTheme && isGradientDark) Color.White
                else MaterialTheme.colorScheme.onSurface
            )
            SponsorMethodIcon(method = sponsor.method, modifier = Modifier.size(24.dp))
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = Color(0xFFEC4899),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SponsorMethodIcon(method: String, modifier: Modifier = Modifier) {
    val iconRes = sponsorMethodIconRes(method)
    if (iconRes != null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    } else {
        Spacer(modifier = modifier)
    }
}

@DrawableRes
private fun sponsorMethodIconRes(method: String): Int? {
    return when (method.trim().uppercase()) {
        "UPI" -> R.drawable.google_pay
        "PAYPAL" -> R.drawable.paypal
        "COFEE" -> R.drawable.coffee_cup
        "GITHUB" -> R.drawable.github
        "CRYPTO" -> R.drawable.bitcoin
        else -> null
    }
}

private fun sponsorAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF97316),
        Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF06B6D4)
    )
    val hash = name.hashCode()
    val index = (hash % colors.size).let { if (it < 0) it + colors.size else it }
    return colors[index]
}
