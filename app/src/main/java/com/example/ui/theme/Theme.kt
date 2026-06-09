package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemePreset(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val onPrimary: Color,
    val isDarkByPref: Boolean
) {
    SLATE(
        displayName = "Slate Luxury",
        primary = Color.White,
        secondary = Indigo400,
        background = ElegantDarkBg,
        surfaceVariant = Zinc800,
        onSurfaceVariant = Slate400,
        onPrimary = Color.Black,
        isDarkByPref = true
    ),
    CYBERPUNK(
        displayName = "Cyberpunk Neon",
        primary = Color(0xFFFF007F), // Hot Pink
        secondary = Color(0xFF00F0FF), // Neon Cyan
        background = Color(0xFF0A0512), // Midnight Cyan/Purple
        surfaceVariant = Color(0xFF1D112E), // Violet Slate
        onSurfaceVariant = Color(0xFFB39DDB), // Light Violet
        onPrimary = Color.White,
        isDarkByPref = true
    ),
    FOREST(
        displayName = "Forest Zen",
        primary = Color(0xFF81C784), // Soft Mint Green
        secondary = Color(0xFF4CAF50), // Sage Green
        background = Color(0xFF0C1410), // Pine obsidian
        surfaceVariant = Color(0xFF1A2621), // Sage bark
        onSurfaceVariant = Color(0xFFA5D6A7), // Forest leaf
        onPrimary = Color(0xFF0C1410),
        isDarkByPref = true
    ),
    AMBER(
        displayName = "Amber Sunset",
        primary = Color(0xFFFFB300), // Glowing Amber
        secondary = Color(0xFFFF5722), // Deep Orange
        background = Color(0xFF140D07), // Coal Cocoa
        surfaceVariant = Color(0xFF2B1A10), // Burnt sienna
        onSurfaceVariant = Color(0xFFFFCC80), // Peach silk
        onPrimary = Color.Black,
        isDarkByPref = true
    ),
    OCEAN(
        displayName = "Ocean Spark",
        primary = Color(0xFF00D2FF), // Neon Azure
        secondary = Color(0xFF0066FF), // Royal Sapphire
        background = Color(0xFF030A16), // Abyss Midnight
        surfaceVariant = Color(0xFF0C1B35), // Nautical Slate
        onSurfaceVariant = Color(0xFF90CAF9), // Foam blue
        onPrimary = Color.Black,
        isDarkByPref = true
    ),
    ROYAL_ROSE(
        displayName = "Sunset Rose",
        primary = Color(0xFFF06292), // Radiant Pink
        secondary = Color(0xFFEC407A), // Sunset Magenta
        background = Color(0xFF1C0A10), // Royal Rosewood
        surfaceVariant = Color(0xFF33141F), // Blackberry plum
        onSurfaceVariant = Color(0xFFF8BBD0), // Soft cream rose
        onPrimary = Color.White,
        isDarkByPref = true
    )
}

private val DarkColorScheme =
    darkColorScheme(
        primary = Color.White,
        onPrimary = Color.Black,
        secondary = Indigo400,
        onSecondary = Color.White,
        tertiary = Slate100,
        background = ElegantDarkBg,
        surface = ElegantDarkBg,
        onBackground = Slate100,
        onSurface = Slate100,
        surfaceVariant = Zinc800,
        onSurfaceVariant = Slate400,
        outline = White10
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color.Black,
        onPrimary = Color.White,
        secondary = Indigo600,
        onSecondary = Color.White,
        tertiary = Slate800,
        background = Slate100,
        surface = Color.White,
        onBackground = Color.Black,
        onSurface = Color.Black,
        surfaceVariant = Slate200,
        onSurfaceVariant = Slate500,
        outline = Black10
    )

@Composable
fun RockWallpapersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    presetName: String = "SLATE",
    dynamicColor: Boolean = false, // Set to false to prioritize custom rich color palettes
    content: @Composable () -> Unit,
) {
    val selectedPreset = try {
        AppThemePreset.valueOf(presetName)
    } catch (e: Exception) {
        AppThemePreset.SLATE
    }

    val colorScheme = when {
        // If dynamic option is true and on supporting OS, prioritize Google Monet dynamic thematic
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        // Otherwise use our premium custom selected theme preset
        else -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = selectedPreset.primary,
                    onPrimary = selectedPreset.onPrimary,
                    secondary = selectedPreset.secondary,
                    onSecondary = Color.White,
                    tertiary = selectedPreset.primary,
                    background = selectedPreset.background,
                    surface = selectedPreset.background,
                    onBackground = Slate100,
                    onSurface = Slate100,
                    surfaceVariant = selectedPreset.surfaceVariant,
                    onSurfaceVariant = selectedPreset.onSurfaceVariant,
                    outline = if (selectedPreset == AppThemePreset.SLATE) White10 else selectedPreset.primary.copy(alpha = 0.15f)
                )
            } else {
                // Adapt preset colors to have beautiful, high-contrast light mode counterparts
                val lightPrimary = when (selectedPreset) {
                    AppThemePreset.SLATE -> Color(0xFF0F172A) // Dark Slate
                    AppThemePreset.CYBERPUNK -> Color(0xFFD81B60) // Deep Magenta
                    AppThemePreset.FOREST -> Color(0xFF1B5E20) // Deep Forest Green
                    AppThemePreset.AMBER -> Color(0xFF78350F) // Deep Honey Amber
                    AppThemePreset.OCEAN -> Color(0xFF0E7490) // Deep Azure Ocean
                    AppThemePreset.ROYAL_ROSE -> Color(0xFF881337) // Deep Rosewood
                }
                
                val lightSecondary = when (selectedPreset) {
                    AppThemePreset.SLATE -> Color(0xFF4F46E5) // Indigo Primary Accent
                    AppThemePreset.CYBERPUNK -> Color(0xFF00B0FF) // Cyber Cyan
                    AppThemePreset.FOREST -> Color(0xFF2E7D32) // Sage Dark Accent
                    AppThemePreset.AMBER -> Color(0xFFD97706) // Burnt Amber Accent
                    AppThemePreset.OCEAN -> Color(0xFF0284C7) // Royal Sky Accent
                    AppThemePreset.ROYAL_ROSE -> Color(0xFFD81B60) // Radiant Rose Accent
                }

                val lightBg = when (selectedPreset) {
                    AppThemePreset.SLATE -> Color(0xFFF8FAFC)
                    AppThemePreset.CYBERPUNK -> Color(0xFFFAF5FF)
                    AppThemePreset.FOREST -> Color(0xFFF4F9F5)
                    AppThemePreset.AMBER -> Color(0xFFFFFBEB)
                    AppThemePreset.OCEAN -> Color(0xFFF0F9FF)
                    AppThemePreset.ROYAL_ROSE -> Color(0xFFFFF1F2)
                }

                val lightSurfaceVariant = when (selectedPreset) {
                    AppThemePreset.SLATE -> Color(0xFFE2E8F0)
                    AppThemePreset.CYBERPUNK -> Color(0xFFF3E8FF)
                    AppThemePreset.FOREST -> Color(0xFFE8F5E9)
                    AppThemePreset.AMBER -> Color(0xFFFEF3C7)
                    AppThemePreset.OCEAN -> Color(0xFFE0F2FE)
                    AppThemePreset.ROYAL_ROSE -> Color(0xFFFFE4E6)
                }

                val lightOnSurfaceVariant = when (selectedPreset) {
                    AppThemePreset.SLATE -> Color(0xFF475569)
                    AppThemePreset.CYBERPUNK -> Color(0xFF7E22CE)
                    AppThemePreset.FOREST -> Color(0xFF1B5E20)
                    AppThemePreset.AMBER -> Color(0xFF92400E)
                    AppThemePreset.OCEAN -> Color(0xFF0369A1)
                    AppThemePreset.ROYAL_ROSE -> Color(0xFFBE123C)
                }

                lightColorScheme(
                    primary = lightPrimary,
                    onPrimary = Color.White,
                    secondary = lightSecondary,
                    onSecondary = Color.White,
                    tertiary = lightSecondary,
                    background = lightBg,
                    surface = Color.White,
                    onBackground = lightPrimary,
                    onSurface = lightPrimary,
                    surfaceVariant = lightSurfaceVariant,
                    onSurfaceVariant = lightOnSurfaceVariant,
                    outline = lightSecondary.copy(alpha = 0.2f)
                )
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
