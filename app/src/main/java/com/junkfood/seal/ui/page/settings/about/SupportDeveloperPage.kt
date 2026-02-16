package com.junkfood.seal.ui.page.settings.about

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientBrushes
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val TAG = "SupportDeveloperPage"

/**
 * Data model for a single sponsor
 */
@Serializable
data class Sponsor(
    val id: Int,
    val name: String
)

/**
 * API response model for sponsors list
 */
@Serializable
data class SponsorsResponse(
    val sponsors: List<Sponsor> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDeveloperPage(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
    val isGradientDark = LocalGradientDarkMode.current
    val coroutineScope = rememberCoroutineScope()
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    // State for sponsors dialog and data
    var showSponsorsDialog by remember { mutableStateOf(false) }
    var sponsors by remember { mutableStateOf<List<Sponsor>>(emptyList()) }
    var isLoadingSponsors by remember { mutableStateOf(false) }
    var sponsorsError by remember { mutableStateOf<String?>(null) }
    
    // Function to fetch sponsors from API
    fun fetchSponsors() {
        coroutineScope.launch {
            isLoadingSponsors = true
            sponsorsError = null
            
            try {
                val result = withContext(Dispatchers.IO) {
                    Log.d(TAG, "Fetching sponsors from API...")
                    
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                    
                    val request = Request.Builder()
                        .url("https://raw.githubusercontent.com/MaheshTechnicals/Sealplus/refs/heads/test/sponsors.json")
                        .get()
                        .addHeader("Accept", "application/json")
                        .build()
                    
                    val response = client.newCall(request).execute()
                    
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }
                    
                    val jsonString = response.body?.string()
                    if (jsonString.isNullOrBlank()) {
                        throw Exception("Empty response from server")
                    }
                    
                    Log.d(TAG, "API Response: $jsonString")
                    
                    // Configure JSON parser with lenient mode
                    val jsonParser = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                        encodeDefaults = true
                    }
                    
                    try {
                        // First try direct deserialization
                        val sponsorsResponse = jsonParser.decodeFromString<SponsorsResponse>(jsonString)
                        Log.d(TAG, "Parsed ${sponsorsResponse.sponsors.size} sponsors")
                        sponsorsResponse.sponsors
                    } catch (e: SerializationException) {
                        Log.w(TAG, "Direct deserialization failed, trying manual parsing: ${e.message}")
                        
                        // Fallback: Manual parsing
                        try {
                            val jsonElement = Json.parseToJsonElement(jsonString)
                            val sponsorsArray = jsonElement.jsonObject["sponsors"]?.jsonArray
                            
                            if (sponsorsArray != null) {
                                sponsorsArray.mapNotNull { element ->
                                    try {
                                        val obj = element.jsonObject
                                        Sponsor(
                                            id = obj["id"]?.jsonPrimitive?.int ?: 0,
                                            name = obj["name"]?.jsonPrimitive?.content ?: ""
                                        )
                                    } catch (ex: Exception) {
                                        Log.w(TAG, "Failed to parse sponsor: ${ex.message}")
                                        null
                                    }
                                }.also {
                                    Log.d(TAG, "Manually parsed ${it.size} sponsors")
                                }
                            } else {
                                throw Exception("'sponsors' field not found in JSON")
                            }
                        } catch (ex: Exception) {
                            Log.e(TAG, "Manual parsing also failed: ${ex.message}", ex)
                            throw Exception("Failed to parse sponsors data: ${ex.message}")
                        }
                    }
                }
                
                sponsors = result
                if (result.isNotEmpty()) {
                    showSponsorsDialog = true
                } else {
                    context.makeToast("No sponsors found")
                }
                
            } catch (e: SerializationException) {
                val error = "JSON parsing error: ${e.message}"
                Log.e(TAG, error, e)
                sponsorsError = error
                context.makeToast(error)
            } catch (e: Exception) {
                val error = e.message ?: "Failed to load sponsors"
                Log.e(TAG, "Error fetching sponsors: $error", e)
                sponsorsError = error
                context.makeToast(error)
            } finally {
                isLoadingSponsors = false
            }
        }
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Support Developer",
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
                                GradientBrushes.Primary
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
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
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = if (isDarkTheme && isGradientDark) 
                                Color.White 
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Support the Development",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isDarkTheme && isGradientDark) 
                                Color.White 
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Your support helps keep Seal Plus free and actively maintained",
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
            
            // Developer Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "About the Developer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        
                        // Developer Name
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Developer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Mahesh Varma",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // GitHub Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    uriHandler.openUri("https://github.com/MaheshTechnicals")
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GitHub",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "@MaheshTechnicals",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Donation Options Section
            item {
                Text(
                    text = "Support Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            // UPI Payment
            item {
                DonationOptionCard(
                    icon = Icons.Outlined.Payment,
                    title = "UPI Payment",
                    description = "maheshtechnicals@apl",
                    gradient = if (isDarkTheme && isGradientDark) {
                        GradientBrushes.Primary
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        )
                    },
                    onClick = {
                        openUpiPayment(
                            context = context,
                            upiId = "maheshtechnicals@apl",
                            name = "Mahesh Technicals",
                            note = "Support Seal Plus Development"
                        )
                    }
                )
            }
            
            // PayPal
            item {
                DonationOptionCard(
                    icon = Icons.Outlined.AccountBalance,
                    title = "PayPal",
                    description = "Support via PayPal",
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0070BA),
                            Color(0xFF1546A0)
                        )
                    ),
                    onClick = {
                        uriHandler.openUri("https://www.paypal.com/paypalme/Varma161")
                    }
                )
            }
            
            // Buy Me a Coffee
            item {
                DonationOptionCard(
                    icon = Icons.Outlined.LocalCafe,
                    title = "Buy Me a Coffee",
                    description = "Support with a coffee",
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF813F),
                            Color(0xFFFF5E00)
                        )
                    ),
                    onClick = {
                        uriHandler.openUri("https://buymeacoffee.com/maheshtechnical")
                    }
                )
            }
            
            // Our Sponsors
            item {
                DonationOptionCard(
                    icon = Icons.Outlined.Group,
                    title = "Our Sponsors",
                    description = if (isLoadingSponsors) "Loading..." else "View our amazing supporters",
                    gradient = if (isDarkTheme && isGradientDark) {
                        GradientBrushes.Accent
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFEC4899),
                                Color(0xFFF97316)
                            )
                        )
                    },
                    onClick = {
                        if (!isLoadingSponsors) {
                            fetchSponsors()
                        }
                    }
                )
            }
            
            // Footer Message
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "All donations help maintain and improve Seal Plus. Thank you for your support! 💙",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Sponsors Dialog
    if (showSponsorsDialog) {
        SponsorsDialog(
            sponsors = sponsors,
            onDismiss = { showSponsorsDialog = false },
            isDarkTheme = isDarkTheme,
            isGradientDark = isGradientDark
        )
    }
}

/**
 * Opens UPI payment intent with pre-filled UPI ID
 * Shows all available UPI apps (Google Pay, PhonePe, Paytm, BHIM, etc.)
 * 
 * @param context Android context
 * @param upiId UPI ID of the payee
 * @param name Name of the payee
 * @param note Transaction note/description
 */
private fun openUpiPayment(
    context: android.content.Context,
    upiId: String,
    name: String,
    note: String = ""
) {
    try {
        // Build UPI payment URI
        // Format: upi://pay?pa=UPI_ID&pn=NAME&tn=NOTE&cu=CURRENCY
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId)  // Payee address (UPI ID)
            .appendQueryParameter("pn", name)    // Payee name
            .appendQueryParameter("tn", note)    // Transaction note
            .appendQueryParameter("cu", "INR")   // Currency
            .build()
        
        // Create intent to handle UPI payment
        val intent = Intent(Intent.ACTION_VIEW, uri)
        
        // Show chooser with all available UPI apps
        val chooser = Intent.createChooser(intent, "Pay with")
        context.startActivity(chooser)
        
    } catch (e: android.content.ActivityNotFoundException) {
        // No UPI app found - copy to clipboard as fallback
        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("UPI ID", upiId)
        clipboardManager.setPrimaryClip(clip)
        context.makeToast("No UPI apps found. UPI ID copied to clipboard")
    } catch (e: Exception) {
        // Other error - copy to clipboard as fallback
        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("UPI ID", upiId)
        clipboardManager.setPrimaryClip(clip)
        context.makeToast("Error opening UPI app. UPI ID copied to clipboard")
    }
}

/**
 * Dialog to display list of sponsors with avatar images
 */
@Composable
private fun SponsorsDialog(
    sponsors: List<Sponsor>,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    isGradientDark: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme && isGradientDark) {
                    GradientDarkColors.Surface
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDarkTheme && isGradientDark) {
                                GradientBrushes.Primary
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFEC4899),
                                        Color(0xFFF97316)
                                    )
                                )
                            }
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Our Amazing Sponsors",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${sponsors.size} supporters making this possible",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Sponsors List
                if (sponsors.isEmpty()) {
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
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Be the first to support!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sponsors) { sponsor ->
                            SponsorItem(
                                sponsor = sponsor,
                                isDarkTheme = isDarkTheme,
                                isGradientDark = isGradientDark
                            )
                        }
                    }
                }
                
                // Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual sponsor item with avatar and name
 */
@Composable
private fun SponsorItem(
    sponsor: Sponsor,
    isDarkTheme: Boolean,
    isGradientDark: Boolean
) {
    // Generate consistent color for avatar based on name
    val avatarColor = remember(sponsor.name) {
        generateAvatarColor(sponsor.name)
    }
    
    // Get initials from name
    val initials = remember(sponsor.name) {
        sponsor.name
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .take(2)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme && isGradientDark) {
                GradientDarkColors.SurfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
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
            // Avatar with initials
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
            
            // Sponsor Name
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = sponsor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme && isGradientDark) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            // Heart icon
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = Color(0xFFEC4899),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Generate consistent avatar color based on name
 */
private fun generateAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFFF97316), // Orange
        Color(0xFF10B981), // Green
        Color(0xFF3B82F6), // Blue
        Color(0xFF14B8A6), // Teal
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF06B6D4)  // Cyan
    )
    
    // Generate consistent index based on name hash
    val hash = name.hashCode()
    val index = (hash % colors.size).let { if (it < 0) it + colors.size else it }
    
    return colors[index]
}

@Composable
private fun DonationOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
