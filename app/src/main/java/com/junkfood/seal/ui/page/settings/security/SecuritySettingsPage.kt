package com.junkfood.seal.ui.page.settings.security

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSwitchWithDivider
import com.junkfood.seal.ui.page.security.SetPinDialog
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.util.AuthenticationManager
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsPage(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var securityEnabled by remember { mutableStateOf(AuthenticationManager.isSecurityEnabled()) }
    var useBiometric by remember { mutableStateOf(AuthenticationManager.useBiometric()) }
    var requireAuthOnLaunch by remember { mutableStateOf(AuthenticationManager.requireAuthOnLaunch()) }
    var authTimeout by remember { mutableIntStateOf(AuthenticationManager.getAuthTimeout()) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showVerifyOldPinDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showDisableSecurityDialog by remember { mutableStateOf(false) }
    
    val isPinSet = AuthenticationManager.isPinSet()
    val isBiometricAvailable = AuthenticationManager.isBiometricAvailable(context)
    val biometricStatusMessage = AuthenticationManager.getBiometricStatusMessage(context)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                PreferenceSubtitle(
                    text = stringResource(R.string.app_lock)
                )
            }
            
            // Enable Security
            item {
                PreferenceSwitchWithDivider(
                    title = stringResource(R.string.enable_app_lock),
                    description = stringResource(R.string.protect_app_with_pin_biometric),
                    icon = Icons.Default.Lock,
                    isChecked = securityEnabled,
                    onClick = {
                        if (!securityEnabled) {
                            // Enabling security - need to set PIN first
                            if (!isPinSet) {
                                showSetPinDialog = true
                            } else {
                                securityEnabled = true
                                AuthenticationManager.setSecurityEnabled(true)
                                context.makeToast(R.string.security_enabled)
                            }
                        } else {
                            // Disabling security
                            showDisableSecurityDialog = true
                        }
                    }
                )
            }
            
            if (securityEnabled && isPinSet) {
                item {
                    PreferenceSubtitle(
                        text = stringResource(R.string.authentication_methods)
                    )
                }
                
                // Use Biometric
                item {
                    PreferenceSwitchWithDivider(
                        title = stringResource(R.string.use_biometric_authentication),
                        description = if (isBiometricAvailable) {
                            stringResource(R.string.unlock_with_fingerprint_face)
                        } else {
                            biometricStatusMessage
                        },
                        icon = Icons.Default.Fingerprint,
                        isChecked = useBiometric,
                        enabled = isBiometricAvailable,
                        onClick = {
                            useBiometric = !useBiometric
                            AuthenticationManager.setUseBiometric(useBiometric)
                        }
                    )
                }
                
                item {
                    PreferenceSubtitle(
                        text = stringResource(R.string.pin_management)
                    )
                }
                
                // Change PIN
                item {
                    PreferenceItem(
                        title = stringResource(R.string.change_pin),
                        description = stringResource(R.string.update_your_pin_code),
                        icon = Icons.Default.Key,
                        onClick = { showVerifyOldPinDialog = true }
                    )
                }
                
                item {
                    PreferenceSubtitle(
                        text = stringResource(R.string.security_options)
                    )
                }
                
                // Require Auth on Launch
                item {
                    PreferenceSwitchWithDivider(
                        title = stringResource(R.string.require_auth_on_launch),
                        description = stringResource(R.string.always_require_authentication_when_opening),
                        icon = Icons.Default.LockOpen,
                        isChecked = requireAuthOnLaunch,
                        onClick = {
                            requireAuthOnLaunch = !requireAuthOnLaunch
                            AuthenticationManager.setRequireAuthOnLaunch(requireAuthOnLaunch)
                        }
                    )
                }
                
                // Auth Timeout
                item {
                    PreferenceItem(
                        title = stringResource(R.string.authentication_timeout),
                        description = stringResource(R.string.auth_timeout_description, authTimeout),
                        icon = Icons.Default.Timer,
                        onClick = { showTimeoutDialog = true }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Security Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.security_info_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Set PIN Dialog
    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onPinSet = {
                showSetPinDialog = false
                securityEnabled = true
                AuthenticationManager.setSecurityEnabled(true)
                context.makeToast(R.string.security_enabled)
            }
        )
    }
    
    // Verify Old PIN Dialog (before allowing change)
    if (showVerifyOldPinDialog) {
        VerifyOldPinDialog(
            onDismiss = { showVerifyOldPinDialog = false },
            onVerified = {
                showVerifyOldPinDialog = false
                showChangePinDialog = true
            }
        )
    }
    
    // Change PIN Dialog
    if (showChangePinDialog) {
        SetPinDialog(
            onDismiss = { showChangePinDialog = false },
            onPinSet = {
                showChangePinDialog = false
                context.makeToast(R.string.pin_changed_successfully)
            }
        )
    }
    
    // Timeout Dialog
    if (showTimeoutDialog) {
        AuthTimeoutDialog(
            currentTimeout = authTimeout,
            onDismiss = { showTimeoutDialog = false },
            onConfirm = { newTimeout ->
                authTimeout = newTimeout
                AuthenticationManager.setAuthTimeout(newTimeout)
                showTimeoutDialog = false
                context.makeToast(R.string.timeout_updated)
            }
        )
    }
    
    // Disable Security Confirmation Dialog
    if (showDisableSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showDisableSecurityDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.disable_security)) },
            text = { Text(stringResource(R.string.disable_security_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        securityEnabled = false
                        AuthenticationManager.setSecurityEnabled(false)
                        AuthenticationManager.resetAuthTime()
                        showDisableSecurityDialog = false
                        context.makeToast(R.string.security_disabled)
                    }
                ) {
                    Text(stringResource(R.string.disable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableSecurityDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun VerifyOldPinDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(0) }
    val maxAttempts = 5
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Handle PIN verification
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            delay(100)
            if (AuthenticationManager.verifyPin(pin)) {
                onVerified()
            } else {
                isError = true
                attempts++
                
                if (attempts >= maxAttempts) {
                    errorMessage = context.getString(R.string.too_many_attempts)
                    delay(1500)
                    onDismiss()
                } else {
                    errorMessage = context.getString(
                        R.string.incorrect_pin_attempts,
                        maxAttempts - attempts
                    )
                }
                
                delay(500)
                pin = ""
                delay(1500)
                isError = false
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.verify_old_pin)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.enter_current_pin_to_continue),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < pin.length
                        val color = when {
                            isError -> MaterialTheme.colorScheme.error
                            isFilled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isFilled) {
                                        Modifier.background(color)
                                    } else {
                                        Modifier.border(2.dp, color, CircleShape)
                                    }
                                )
                        )
                    }
                }
                
                if (isError && errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Number Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "⌫")
                    ).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { char ->
                                if (char.isEmpty()) {
                                    Spacer(modifier = Modifier.size(56.dp))
                                } else if (char == "⌫") {
                                    OutlinedButton(
                                        onClick = {
                                            if (pin.isNotEmpty() && !isError) {
                                                pin = pin.dropLast(1)
                                            }
                                        },
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            if (pin.length < 4 && !isError) {
                                                pin += char
                                            }
                                        },
                                        modifier = Modifier.size(56.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(char, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun AuthTimeoutDialog(
    currentTimeout: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedTimeout by remember { mutableIntStateOf(currentTimeout) }
    val timeoutOptions = listOf(1, 2, 5, 10, 15, 30, 60)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timer, contentDescription = null) },
        title = { Text(stringResource(R.string.authentication_timeout)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.select_timeout_duration),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                timeoutOptions.forEach { timeout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedTimeout == timeout,
                            onClick = { selectedTimeout = timeout }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.timeout_minutes, timeout),
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.CenterVertically)
                                .weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedTimeout) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview(name = "Security Settings Light")
@Preview(name = "Security Settings Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecuritySettingsPagePreview() {
    SealTheme {
        SecuritySettingsPage(
            onBackPressed = {}
        )
    }
}
