# Gradient Dark - Quick Reference Card

## 🎨 Component Cheat Sheet

### Replace These...
```kotlin
// ❌ Old
Card { 
    Text("Title")
}

// ❌ Old  
Button(onClick = {}) {
    Text("Action")
}

// ❌ Old
Text("Section", style = Typography.headlineMedium)
```

### ...With These!
```kotlin
// ✅ New
PremiumGlassCard(
    title = "Title",
    icon = Icons.Outlined.Icon
)

// ✅ New
PremiumGradientButton(
    text = "Action",
    icon = Icons.Outlined.Icon,
    onClick = {}
)

// ✅ New
PremiumSectionHeader(
    title = "Section",
    icon = Icons.Outlined.Icon
)
```

## 🌈 Color Quick Copy

```kotlin
// Backgrounds
GradientDarkColors.Background        // #0A0A0F
GradientDarkColors.Surface           // #14141F
GradientDarkColors.SurfaceVariant    // #1A1A2E

// Gradients
GradientBrushes.Primary              // Blue → Purple
GradientBrushes.Secondary            // Blue → Indigo → Purple
GradientBrushes.Accent               // Purple → Pink
GradientBrushes.Vibrant              // Blue → Purple → Pink

// Text
GradientDarkColors.OnSurface         // #F5F5F5
GradientDarkColors.OnBackground      // #FAFAFA
```

## 📐 Size Quick Reference

```kotlin
cornerRadius = 20.dp    // Cards
cornerRadius = 16.dp    // Buttons
cornerRadius = 8.dp     // Icon boxes

elevation = 4.dp        // Standard card
elevation = 8.dp        // Elevated card

padding = 16.dp         // Section spacing
padding = 20.dp         // Card internal
padding = 12.dp         // Item spacing

buttonHeight = 56.dp    // Standard button
iconSize = 24.dp        // Standard icon
touchTarget = 48.dp     // Minimum touch
```

## ⏱️ Animation Quick Reference

```kotlin
// Card entrance
delayMillis = 0          // First item
delayMillis = 50         // Second item
delayMillis = 100        // Third item
// etc. (increment by 50-100)

// Durations
300ms  // Card entrance (fade + scale)
150ms  // Button press
400ms  // Page transition

// Easing
FastOutSlowInEasing     // Standard
Spring.DampingRatioMediumBouncy  // Buttons
```

## 🔧 Modifier Extensions

```kotlin
// Apply gradient
Modifier.gradientBackground(
    brush = GradientBrushes.Primary,
    cornerRadius = 16.dp
)

// Apply glassmorphism
Modifier.glassmorphism(
    cornerRadius = 20.dp,
    borderWidth = 1.dp,
    alpha = 0.05f
)

// Detect gradient dark
val isGradientDark = LocalGradientDarkMode.current
```

## 📱 Common Patterns

### LazyColumn with animated cards
```kotlin
LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    items(list.size) { index ->
        AnimatedCardContainer(delayMillis = index * 50) {
            PremiumGlassCard(
                title = list[index].title,
                icon = list[index].icon
            )
        }
    }
}
```

### Section with header and cards
```kotlin
item {
    PremiumSectionHeader(
        title = "Section",
        icon = Icons.Outlined.Icon
    )
}
items(items) { item ->
    AnimatedCardContainer {
        PremiumGlassCard(
            title = item.title,
            description = item.desc
        )
    }
}
```

### Action button row
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    PremiumGradientButton(
        text = "Primary",
        onClick = {},
        modifier = Modifier.weight(1f),
        brush = GradientBrushes.Primary
    )
    PremiumGradientButton(
        text = "Secondary",
        onClick = {},
        modifier = Modifier.weight(1f),
        brush = GradientBrushes.Secondary
    )
}
```

### Info banner
```kotlin
PremiumInfoCard(
    text = "Information message here",
    icon = Icons.Outlined.Info
)
```

## 🎯 Best Practices

### DO ✅
- Use 16dp spacing between sections
- Increment animation delays by 50-100ms
- Use Primary gradient for main actions
- Keep glass opacity at 5-10%
- Add icons to all cards
- Use descriptive titles

### DON'T ❌
- Mix old and new components
- Exceed 300ms animation delay
- Use multiple glass layers
- Ignore touch target sizes
- Skip section headers
- Overuse vibrant gradient

## 🚀 5-Minute Integration

1. **Import**
```kotlin
import com.junkfood.seal.ui.component.*
import com.junkfood.seal.ui.theme.GradientBrushes
```

2. **Replace Cards**
```kotlin
Card { } → PremiumGlassCard { }
```

3. **Replace Buttons**
```kotlin
Button { } → PremiumGradientButton(text = "", onClick = {})
```

4. **Add Headers**
```kotlin
PremiumSectionHeader(title = "", icon = Icons.Outlined.Icon)
```

5. **Animate**
```kotlin
AnimatedCardContainer(delayMillis = 100) { }
```

6. **Test**
- Enable Dark Theme
- Toggle Gradient Dark
- Verify animations

## 📖 Full Documentation

- [Implementation Guide](GRADIENT_DARK_IMPLEMENTATION_GUIDE.md)
- [Visual Reference](GRADIENT_DARK_VISUAL_REFERENCE.md)
- [Integration Examples](app/src/main/java/com/junkfood/seal/ui/integration/IntegrationExamples.kt)
- [Example Page](app/src/main/java/com/junkfood/seal/ui/page/settings/appearance/GradientDarkExample.kt)

---

**💡 Tip**: All components auto-detect Gradient Dark mode and gracefully fall back to Material 3 when disabled!
