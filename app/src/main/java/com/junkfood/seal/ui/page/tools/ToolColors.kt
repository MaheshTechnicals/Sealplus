package com.junkfood.seal.ui.page.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.junkfood.seal.ui.common.LocalDarkTheme

/**
 * Shared dark-mode palette for the "More Tools" family of screens (Batch URL Import,
 * Video Info Download, etc). Keeping this in one place means every tool page looks and
 * feels consistent, and any future palette tweak only needs to happen here.
 */
object ToolColors {
    val Background = Color(0xFF09090B)
    val Surface = Color(0xFF14141A)
    val SurfaceVariant = Color(0xFF1E1E28)
    val Primary = Color(0xFF7C4DFF)
    val Border = Color(0xFF2A2A35)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9E9EAB)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Success = Color(0xFF22C55E)
}

val ToolGradientBrush: Brush = Brush.horizontalGradient(
    colors = listOf(Color(0xFF7C4DFF), Color(0xFF9C6DFF)),
)

/**
 * Resolved set of colors for a tool page: [ToolColors] in dark mode, or the app's
 * Material theme roles in light mode. Every tool screen should derive its colors from
 * this so behaviour stays consistent with the rest of the app.
 */
@Immutable
data class ToolPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val primary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val chipSelectedBg: Color,
    val chipSelectedBorder: Color,
    val chipUnselectedBorder: Color,
    val warning: Color,
    val error: Color,
    val success: Color,
    val isDarkMode: Boolean,
)

@Composable
fun rememberToolPalette(): ToolPalette {
    val isDarkMode = LocalDarkTheme.current.isDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    return if (isDarkMode) {
        ToolPalette(
            background = ToolColors.Background,
            surface = ToolColors.Surface,
            surfaceVariant = ToolColors.SurfaceVariant,
            border = ToolColors.Border,
            primary = ToolColors.Primary,
            textPrimary = ToolColors.TextPrimary,
            textSecondary = ToolColors.TextSecondary,
            chipSelectedBg = ToolColors.Primary.copy(alpha = 0.12f),
            chipSelectedBorder = ToolColors.Primary,
            chipUnselectedBorder = ToolColors.Border,
            warning = ToolColors.Warning,
            error = ToolColors.Error,
            success = ToolColors.Success,
            isDarkMode = true,
        )
    } else {
        ToolPalette(
            background = colorScheme.background,
            surface = colorScheme.surface,
            surfaceVariant = colorScheme.surfaceVariant,
            border = colorScheme.outlineVariant,
            primary = colorScheme.primary,
            textPrimary = colorScheme.onSurface,
            textSecondary = colorScheme.onSurfaceVariant,
            chipSelectedBg = colorScheme.primaryContainer,
            chipSelectedBorder = colorScheme.primary,
            chipUnselectedBorder = colorScheme.outlineVariant,
            warning = ToolColors.Warning,
            error = colorScheme.error,
            success = ToolColors.Success,
            isDarkMode = false,
        )
    }
}
