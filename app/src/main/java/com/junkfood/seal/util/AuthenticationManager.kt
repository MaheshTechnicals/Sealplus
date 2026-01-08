package com.junkfood.seal.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.R
import com.tencent.mmkv.MMKV
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "AuthenticationManager"
private const val KEYSTORE_ALIAS = "SealPlusAuthKey"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

object AuthenticationManager {
    
    private const val PREF_SECURITY_ENABLED = "security_enabled"
    private const val PREF_PIN_HASH = "pin_hash"
    private const val PREF_USE_BIOMETRIC = "use_biometric"
    private const val PREF_REQUIRE_AUTH_ON_LAUNCH = "require_auth_on_launch"
    private const val PREF_AUTH_TIMEOUT = "auth_timeout"
    private const val PREF_LAST_AUTH_TIME = "last_auth_time"
    
    private val prefs = MMKV.defaultMMKV()
    
    /**
     * Check if security is enabled
     */
    fun isSecurityEnabled(): Boolean {
        return prefs?.decodeBool(PREF_SECURITY_ENABLED, false) ?: false
    }
    
    /**
     * Enable or disable security
     */
    fun setSecurityEnabled(enabled: Boolean) {
        prefs?.encode(PREF_SECURITY_ENABLED, enabled)
    }
    
    /**
     * Check if biometric authentication should be used
     */
    fun useBiometric(): Boolean {
        return prefs?.decodeBool(PREF_USE_BIOMETRIC, true) ?: true
    }
    
    /**
     * Set whether to use biometric authentication
     */
    fun setUseBiometric(use: Boolean) {
        prefs?.encode(PREF_USE_BIOMETRIC, use)
    }
    
    /**
     * Check if authentication is required on app launch
     */
    fun requireAuthOnLaunch(): Boolean {
        return prefs?.decodeBool(PREF_REQUIRE_AUTH_ON_LAUNCH, true) ?: true
    }
    
    /**
     * Set whether authentication is required on app launch
     */
    fun setRequireAuthOnLaunch(require: Boolean) {
        prefs?.encode(PREF_REQUIRE_AUTH_ON_LAUNCH, require)
    }
    
    /**
     * Get authentication timeout in minutes
     */
    fun getAuthTimeout(): Int {
        return prefs?.decodeInt(PREF_AUTH_TIMEOUT, 5) ?: 5
    }
    
    /**
     * Set authentication timeout in minutes
     */
    fun setAuthTimeout(minutes: Int) {
        prefs?.encode(PREF_AUTH_TIMEOUT, minutes)
    }
    
    /**
     * Check if a PIN is set
     */
    fun isPinSet(): Boolean {
        return prefs?.decodeString(PREF_PIN_HASH, null) != null
    }
    
    /**
     * Set a new PIN
     */
    fun setPin(pin: String): Boolean {
        return try {
            val hash = hashPin(pin)
            prefs?.encode(PREF_PIN_HASH, hash)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting PIN", e)
            false
        }
    }
    
    /**
     * Verify a PIN
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs?.decodeString(PREF_PIN_HASH, null) ?: return false
        return try {
            val inputHash = hashPin(pin)
            storedHash == inputHash
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying PIN", e)
            false
        }
    }
    
    /**
     * Clear the stored PIN
     */
    fun clearPin() {
        prefs?.removeValueForKey(PREF_PIN_HASH)
    }
    
    /**
     * Reset all AppLock settings
     * Clears PIN, disables security, and resets all preferences
     */
    fun resetAppLock() {
        prefs?.removeValueForKey(PREF_PIN_HASH)
        prefs?.removeValueForKey(PREF_SECURITY_ENABLED)
        prefs?.removeValueForKey(PREF_USE_BIOMETRIC)
        prefs?.removeValueForKey(PREF_REQUIRE_AUTH_ON_LAUNCH)
        prefs?.removeValueForKey(PREF_AUTH_TIMEOUT)
        prefs?.removeValueForKey(PREF_LAST_AUTH_TIME)
    }
    
    /**
     * Hash a PIN using SHA-256
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
    
    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Check if device credentials (PIN/Pattern/Password) are available
     */
    fun isDeviceCredentialAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Get biometric authentication status message
     */
    fun getBiometricStatusMessage(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> 
                context.getString(R.string.biometric_available)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> 
                context.getString(R.string.biometric_no_hardware)
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> 
                context.getString(R.string.biometric_hw_unavailable)
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> 
                context.getString(R.string.biometric_none_enrolled)
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                context.getString(R.string.biometric_security_update_required)
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                context.getString(R.string.biometric_unsupported)
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN ->
                context.getString(R.string.biometric_status_unknown)
            else -> context.getString(R.string.biometric_unavailable)
        }
    }
    
    /**
     * Show biometric authentication prompt
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        allowDeviceCredential: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                subtitle?.let { setSubtitle(it) }
                description?.let { setDescription(it) }
            }
            .apply {
                if (allowDeviceCredential) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                    } else {
                        @Suppress("DEPRECATION")
                        setDeviceCredentialAllowed(true)
                    }
                } else {
                    setNegativeButtonText(activity.getString(android.R.string.cancel))
                }
            }
            .build()
        
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            // User cancelled, don't show error
                        }
                        else -> onError(errString.toString())
                    }
                }
                
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Validate PIN format (4 digits only)
     */
    fun isValidPinFormat(pin: String): Boolean {
        return pin.length == 4 && pin.all { it.isDigit() }
    }
    
    /**
     * Check if authentication is needed based on timeout
     */
    private const val PREF_LAST_AUTH_TIME = "last_auth_time"
    
    fun updateLastAuthTime() {
        prefs?.encode(PREF_LAST_AUTH_TIME, System.currentTimeMillis())
    }
    
    fun getLastAuthTime(): Long {
        return prefs?.decodeLong(PREF_LAST_AUTH_TIME, 0L) ?: 0L
    }
    
    fun isAuthenticationNeeded(): Boolean {
        if (!isSecurityEnabled()) return false
        if (!requireAuthOnLaunch()) return false
        
        val lastAuthTime = getLastAuthTime()
        if (lastAuthTime == 0L) return true // First time or after reset
        
        val timeout = getAuthTimeout() * 60 * 1000L // Convert to milliseconds
        val currentTime = System.currentTimeMillis()
        
        return (currentTime - lastAuthTime) > timeout
    }
    
    fun resetAuthTime() {
        prefs?.encode(PREF_LAST_AUTH_TIME, 0L)
    }
}
