package com.junkfood.seal.ui.page.security

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.junkfood.seal.R
import com.junkfood.seal.ui.theme.SealTheme
import com.junkfood.seal.util.AuthenticationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class SetPinState {
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN,
    SUCCESS,
    ERROR
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onPinSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(SetPinState.ENTER_NEW_PIN) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = when (state) {
                        SetPinState.ENTER_NEW_PIN -> stringResource(R.string.enter_new_pin)
                        SetPinState.CONFIRM_NEW_PIN -> stringResource(R.string.confirm_pin)
                        SetPinState.SUCCESS -> stringResource(R.string.pin_set_successfully)
                        SetPinState.ERROR -> errorMessage.ifEmpty { stringResource(R.string.pins_do_not_match) }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = when (state) {
                        SetPinState.ENTER_NEW_PIN -> stringResource(R.string.enter_pin_4_6_digits)
                        SetPinState.CONFIRM_NEW_PIN -> stringResource(R.string.enter_pin_again)
                        SetPinState.SUCCESS -> stringResource(R.string.pin_has_been_set)
                        SetPinState.ERROR -> errorMessage
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state == SetPinState.ERROR) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // PIN Dots
                when (state) {
                    SetPinState.ENTER_NEW_PIN, SetPinState.CONFIRM_NEW_PIN -> {
                        val currentPin = if (state == SetPinState.ENTER_NEW_PIN) firstPin else confirmPin
                        
                        SetPinDots(
                            pinLength = currentPin.length,
                            isError = state == SetPinState.ERROR,
                            maxLength = 6
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Number Pad
                        SetPinNumberPad(
                            onNumberClick = { number ->
                                when (state) {
                                    SetPinState.ENTER_NEW_PIN -> {
                                        if (firstPin.length < 6) {
                                            firstPin += number
                                            
                                            // Auto-advance when valid length
                                            if (firstPin.length in 4..6) {
                                                scope.launch {
                                                    delay(100)
                                                    if (AuthenticationManager.isValidPinFormat(firstPin)) {
                                                        state = SetPinState.CONFIRM_NEW_PIN
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    SetPinState.CONFIRM_NEW_PIN -> {
                                        if (confirmPin.length < 6) {
                                            confirmPin += number
                                            
                                            // Check when lengths match
                                            if (confirmPin.length == firstPin.length) {
                                                scope.launch {
                                                    delay(100)
                                                    if (firstPin == confirmPin) {
                                                        if (AuthenticationManager.setPin(firstPin)) {
                                                            state = SetPinState.SUCCESS
                                                            delay(1500)
                                                            onPinSet()
                                                        } else {
                                                            errorMessage = context.getString(R.string.error_setting_pin)
                                                            state = SetPinState.ERROR
                                                            delay(2000)
                                                            state = SetPinState.ENTER_NEW_PIN
                                                            firstPin = ""
                                                            confirmPin = ""
                                                        }
                                                    } else {
                                                        errorMessage = context.getString(R.string.pins_do_not_match)
                                                        state = SetPinState.ERROR
                                                        delay(1500)
                                                        state = SetPinState.CONFIRM_NEW_PIN
                                                        confirmPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            onBackspaceClick = {
                                when (state) {
                                    SetPinState.ENTER_NEW_PIN -> {
                                        if (firstPin.isNotEmpty()) {
                                            firstPin = firstPin.dropLast(1)
                                        }
                                    }
                                    SetPinState.CONFIRM_NEW_PIN -> {
                                        if (confirmPin.isNotEmpty()) {
                                            confirmPin = confirmPin.dropLast(1)
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            isEnabled = state in listOf(SetPinState.ENTER_NEW_PIN, SetPinState.CONFIRM_NEW_PIN)
                        )
                    }
                    SetPinState.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    SetPinState.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                if (state in listOf(SetPinState.ENTER_NEW_PIN, SetPinState.CONFIRM_NEW_PIN)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cancel Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetPinDots(
    pinLength: Int,
    isError: Boolean,
    maxLength: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        repeat(maxLength) { index ->
            SetPinDot(
                isFilled = index < pinLength,
                isError = isError
            )
        }
    }
}

@Composable
private fun SetPinDot(
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
            .size(14.dp)
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
private fun SetPinNumberPad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rows 1-3
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { number ->
                    SetPinNumberButton(
                        text = number,
                        onClick = { onNumberClick(number) },
                        enabled = isEnabled
                    )
                }
            }
        }
        
        // Row 4: Empty / 0 / Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Spacer(modifier = Modifier.size(60.dp))
            
            SetPinNumberButton(
                text = "0",
                onClick = { onNumberClick("0") },
                enabled = isEnabled
            )
            
            IconButton(
                onClick = onBackspaceClick,
                modifier = Modifier.size(60.dp),
                enabled = isEnabled
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = stringResource(R.string.backspace),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SetPinNumberButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(60.dp)
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
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(name = "Set PIN Dialog Light")
@Preview(name = "Set PIN Dialog Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetPinDialogPreview() {
    SealTheme {
        SetPinDialog(
            onDismiss = {},
            onPinSet = {}
        )
    }
}
