# Gradient Dark Theme - Quick Start

## What's New? 🎨

Seal Plus now features a premium **Gradient Dark** theme with:

- 🌌 Deep charcoal/obsidian backgrounds with vibrant blue-purple gradients
- ✨ Glassmorphism effects with subtle borders and shadows
- 🎬 Smooth micro-animations for all interactions
- 🎯 Premium card-based layouts throughout the app

## How to Enable

1. Open **Settings**
2. Go to **Look & Feel**
3. Enable **Dark Theme** (if not already on)
4. Toggle **Gradient Dark** switch
5. Enjoy the premium experience! ✨

## For Developers

### Quick Component Replacement

```kotlin
// Standard Card → Premium Glass Card
PremiumGlassCard(
    title = "Title",
    description = "Description",
    icon = Icons.Outlined.Icon
) { /* content */ }

// Standard Button → Premium Gradient Button
PremiumGradientButton(
    text = "Action",
    icon = Icons.Outlined.Icon,
    onClick = { /* action */ }
)

// Add Section Headers
PremiumSectionHeader(
    title = "Section",
    icon = Icons.Outlined.Icon
)

// Animate Card Entrance
AnimatedCardContainer(delayMillis = 100) {
    PremiumGlassCard { /* content */ }
}
```

### Available Gradient Brushes

```kotlin
GradientBrushes.Primary   // Blue → Purple
GradientBrushes.Secondary // Blue → Indigo → Purple
GradientBrushes.Accent    // Purple → Pink
GradientBrushes.Vibrant   // Blue → Purple → Pink
```

### Modifier Extensions

```kotlin
// Apply gradient background
Modifier.gradientBackground(
    brush = GradientBrushes.Primary,
    cornerRadius = 16.dp
)

// Apply glassmorphism effect
Modifier.glassmorphism(
    cornerRadius = 20.dp,
    borderWidth = 1.dp,
    alpha = 0.05f
)
```

## File Structure

```
app/src/main/
├── res/
│   ├── values/
│   │   └── colors_gradient_dark.xml      # Color palette
│   ├── drawable/
│   │   ├── gradient_primary.xml          # Gradient resources
│   │   ├── gradient_secondary.xml
│   │   ├── gradient_accent.xml
│   │   ├── glass_card_background.xml     # Glassmorphism backgrounds
│   │   └── glass_card_elevated.xml
│   └── anim/
│       ├── fade_in_scale.xml             # Entrance animations
│       ├── fade_out_scale.xml            # Exit animations
│       └── button_press.xml              # Press animations
└── java/.../seal/
    ├── ui/
    │   ├── theme/
    │   │   └── GradientDarkTheme.kt      # Theme implementation
    │   ├── component/
    │   │   └── PremiumComponents.kt      # Premium UI components
    │   └── page/settings/appearance/
    │       ├── GradientDarkExample.kt    # Example usage
    │       └── AppearancePreferences.kt  # Settings toggle (modified)
    └── util/
        └── PreferenceUtil.kt              # Preference management (modified)
```

## Design Specifications

### Colors
- Background: `#0A0A0F` (Deep obsidian)
- Surface: `#14141F` (Dark charcoal)
- Primary: `#5B47E5` → `#8B5CF6` (Blue-purple gradient)
- Glass Border: `#1AFFFFFF` (10% white)

### Dimensions
- Card Corner Radius: 20-24dp
- Button Corner Radius: 16dp
- Card Elevation: 4-8dp
- Standard Padding: 16-20dp
- Section Spacing: 16dp

### Animations
- Card Entrance: 300ms fade + scale
- Button Press: 150ms scale (0.96)
- Page Transition: 400ms fade

## Documentation

See [GRADIENT_DARK_IMPLEMENTATION_GUIDE.md](GRADIENT_DARK_IMPLEMENTATION_GUIDE.md) for:
- Detailed component API documentation
- Integration guide with code examples
- Best practices and design guidelines
- Performance considerations
- Accessibility standards
- Troubleshooting tips

## Example Page

Check out `GradientDarkExample.kt` for a complete demo showing:
- Premium glass cards with icons and descriptions
- Gradient buttons with different brush styles
- Section headers with gradient accents
- Info cards with gradient borders
- Animated card containers with staggered delays
- Statistics display with premium styling

Run the example to see all components in action!

---

**Tip**: Components automatically detect if Gradient Dark is enabled and adapt their appearance. When disabled, they gracefully fall back to standard Material 3 styling.
