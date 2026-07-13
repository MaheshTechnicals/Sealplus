package com.junkfood.seal.ui.page.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.makeToast

private const val BEP20_ADDRESS = "0x8c857c0FaDc6B3d58678Af5F5A28905a75Cc0c16"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoDonationPage(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crypto Donation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with gradient
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isDarkTheme && isGradientDark) {
                                GradientBrushes.Accent
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF7931A),
                                        Color(0xFFF9B24D)
                                    )
                                )
                            }
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MonetizationOn,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = if (isDarkTheme && isGradientDark)
                                Color.White
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Crypto Currency Donation",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isDarkTheme && isGradientDark)
                                Color.White
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Support Seal Plus development with USDT (BEP20) tokens",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (isDarkTheme && isGradientDark)
                                Color.White.copy(alpha = 0.9f)
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Warning Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme && isGradientDark) {
                            GradientDarkColors.SurfaceContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (isDarkTheme && isGradientDark) {
                                Color(0xFFF7931A)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Please ensure you send tokens only on the supported network specified below. Sending via the wrong network will result in permanent loss of funds.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDarkTheme && isGradientDark) {
                                GradientDarkColors.OnSurface
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Network & Address Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme && isGradientDark) {
                            GradientDarkColors.SurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Network
                        Text(
                            text = "Supported Network",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme && isGradientDark) {
                                GradientDarkColors.OnSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                tint = if (isDarkTheme && isGradientDark) {
                                    GradientDarkColors.GradientCyan
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "BNB Smart Chain (BEP20)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkTheme && isGradientDark) {
                                        GradientDarkColors.OnSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = "Token: USDT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDarkTheme && isGradientDark) {
                                        GradientDarkColors.OnSurface.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Wallet Address
                        Text(
                            text = "Wallet Address",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme && isGradientDark) {
                                GradientDarkColors.OnSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme && isGradientDark) {
                                GradientDarkColors.SurfaceContainerHigh
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ) {
                            Text(
                                text = BEP20_ADDRESS,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkTheme && isGradientDark) {
                                    GradientDarkColors.GradientCyan
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(16.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2
                            )
                        }

                        // Copy Address Button
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(BEP20_ADDRESS))
                                context.makeToast("BEP20 Address copied!")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = if (isDarkTheme && isGradientDark) {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = GradientDarkColors.OnPrimary
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isDarkTheme && isGradientDark) {
                                            GradientBrushes.Primary
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    )
                                    .clip(RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Copy Address",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Additional Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme && isGradientDark) {
                            GradientDarkColors.SurfaceContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = if (isDarkTheme && isGradientDark) {
                                GradientDarkColors.GradientPurpleBright
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Important Notes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme && isGradientDark) {
                                    GradientDarkColors.OnSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = "• Only send USDT tokens on BNB Smart Chain (BEP20)\n• Do not send tokens via ERC20, TRC20, or other networks\n• Minimum recommended amount: \$5 USD\n• All donations are non-refundable",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme && isGradientDark) {
                                    GradientDarkColors.OnSurface.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
