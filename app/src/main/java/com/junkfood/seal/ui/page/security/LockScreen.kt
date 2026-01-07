package com.junkfood.seal.ui.page.security

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.junkfood.seal.R
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.util.AuthenticationManager
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    useBiometric: Boolean = AuthenticationManager.useBiometric()
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(0) }
    val maxAttempts = 5
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Show biometric prompt on first composition if available
    LaunchedEffect(Unit) {
        if (useBiometric && AuthenticationManager.isBiometricAvailable(context)) {
            if (context is FragmentActivity) {
                AuthenticationManager.showBiometricPrompt(
                    activity = context,
                    title = context.getString(R.string.unlock_seal_plus),
                    subtitle = context.getString(R.string.biometric_prompt_subtitle),
                    description = context.getString(R.string.biometric_prompt_description),
                    allowDeviceCredential = false,
                    onSuccess = {
                        AuthenticationManager.updateLastAuthTime()
                        onUnlocked()
                    },
                    onError = { error ->
                        // Silently fail to PIN entry
                    },
                    onFailed = {
                        // Authentication failed, stay on PIN screen
                    }
                )
            }
        }
    }
    
    // Handle PIN verification
    LaunchedEffect(pin) {
        if (pin.length == 4 || pin.length == 6) {
            delay(100) // Small delay for better UX
            if (AuthenticationManager.verifyPin(pin)) {
                AuthenticationManager.updateLastAuthTime()
                onUnlocked()
            } else {
                isError = true
                attempts++
                
                if (attempts >= maxAttempts) {
                    errorMessage = context.getString(R.string.too_many_attempts)
                    // Could add timeout logic here
                } else {
                    errorMessage = context.getString(
                        R.string.incorrect_pin_attempts,
                        maxAttempts - attempts
                    )
                }
                
                // Clear PIN after error
                delay(500)
                pin = ""
                delay(1500)
                isError = false
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // App Icon and Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.seal_plus),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.enter_pin_to_unlock),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // PIN Dots
                PinDots(
                    pinLength = pin.length,
                    isError = isError,
                    maxLength = 6
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error Message
                AnimatedVisibility(
                    visible = isError && errorMessage.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            // Number Pad
            Column(
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                NumberPad(
                    onNumberClick = { number ->
                        if (pin.length < 6) {
                            pin += number
                        }
                    },
                    onBackspaceClick = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                        }
                    },
                    onBiometricClick = if (useBiometric && 
                        AuthenticationManager.isBiometricAvailable(context) && 
                        context is FragmentActivity) {
                        {
                            AuthenticationManager.showBiometricPrompt(
                                activity = context,
                                title = context.getString(R.string.unlock_seal_plus),
                                subtitle = context.getString(R.string.biometric_prompt_subtitle),
                                allowDeviceCredential = false,
                                onSuccess = {
                                    AuthenticationManager.updateLastAuthTime()
                                    onUnlocked()
                                },
                                onError = { error ->
                                    context.makeToast(error)
                                },
                                onFailed = {
                                    context.makeToast(context.getString(R.string.authentication_failed))
                                }
                            )
                        }
                    } else null,
                    isEnabled = !isError
                )
            }
        }
    }
}

@Composable
private fun PinDots(
    pinLength: Int,
    isError: Boolean,
    maxLength: Int,
    modifier: Modifier = Modifier
) {
    val shakeOffset by animateDpAsState(
        targetValue = if (isError) 10.dp else 0.dp,
        animationSpec = repeatable(
            iterations = 3,
            animation = tween(50),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )
    
    Row(
        modifier = modifier
            .offset(x = shakeOffset)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        repeat(maxLength) { index ->
            PinDot(
                isFilled = index < pinLength,
                isError = isError
            )
        }
    }
}

@Composable
private fun PinDot(
    isFilled: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isFilled) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val color = when {
        isError -> MaterialTheme.colorScheme.error
        isFilled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = modifier
            .size(16.dp)
            .scale(scale)
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

@Composable
private fun NumberPad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rows 1-3
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { number ->
                    NumberButton(
                        text = number,
                        onClick = { onNumberClick(number) },
                        enabled = isEnabled
                    )
                }
            }
        }
        
        // Row 4: Biometric / 0 / Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            // Biometric or empty space
            if (onBiometricClick != null) {
                IconButton(
                    onClick = onBiometricClick,
                    modifier = Modifier.size(72.dp),
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = stringResource(R.string.use_biometric),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(72.dp))
            }
            
            // 0
            NumberButton(
                text = "0",
                onClick = { onNumberClick("0") },
                enabled = isEnabled
            )
            
            // Backspace
            IconButton(
                onClick = onBackspaceClick,
                modifier = Modifier.size(72.dp),
                enabled = isEnabled
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = stringResource(R.string.backspace),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun NumberButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(72.dp)
            .scale(scale),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        enabled = enabled
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(name = "Lock Screen Light", showBackground = true)
@Preview(name = "Lock Screen Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun LockScreenPreview() {
    SealTheme {
        LockScreen(
            onUnlocked = {},
            useBiometric = true
        )
    }
}
