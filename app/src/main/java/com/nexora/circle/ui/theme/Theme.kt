package com.nexora.circle.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

/**
 * Every colour is read from ThemeState, so when the user changes the theme
 * all screens that use NexoraColors recolour instantly (no restart).
 * Names are unchanged, so existing screens keep working.
 */
object NexoraColors {
    // Background (pure black in Dark, white in Light). Also used as "text on accent".
    val SpaceBlack: Color
        get() = if (ThemeState.isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val DeepNavy: Color
        get() = if (ThemeState.isDark) Color(0xFF0A0A0A) else Color(0xFFF7F8FA)
    val Panel: Color
        get() = if (ThemeState.isDark) Color(0xFF121212) else Color(0xFFF0F2F5)
    val NebulaViolet: Color
        get() = if (ThemeState.isDark) Color(0xFFB0B0B8) else Color(0xFF6B7280)
    val NebulaPink: Color
        get() = Color(0xFFFF4FB0)

    /** The app accent colour chosen in Customize Theme (name kept so old screens keep working). */
    val PlasmaCyan: Color
        get() {
            val a = ThemeState.accent
            return if (a == AppAccent.Black) {
                if (ThemeState.isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
            } else {
                a.swatch
            }
        }
    val StarWhite: Color
        get() = if (ThemeState.isDark) Color(0xFFFFFFFF) else Color(0xFF0F1419)
    val Muted: Color
        get() = if (ThemeState.isDark) Color(0xFFFFFFFF) else Color(0xFF4B5563)
    val Divider: Color
        get() = if (ThemeState.isDark) Color(0xFF2A2A2A) else Color(0xFFD8DDE3)
}

private fun buildTypography(style: FontStyleChoice): Typography {
    val base = Typography()
    if (style == FontStyleChoice.Normal) return base

    val family: FontFamily = if (style == FontStyleChoice.Stylish) FontFamily.Serif else FontFamily.Default
    val weight: FontWeight? = if (style == FontStyleChoice.Bold) FontWeight.Bold else null
    val italicStyle: FontStyle? = if (style == FontStyleChoice.Stylish) FontStyle.Italic else null

    fun TextStyle.restyled(): TextStyle = copy(
        fontFamily = family,
        fontWeight = weight ?: this.fontWeight,
        fontStyle = italicStyle ?: this.fontStyle,
    )

    return Typography(
        displayLarge = base.displayLarge.restyled(),
        displayMedium = base.displayMedium.restyled(),
        displaySmall = base.displaySmall.restyled(),
        headlineLarge = base.headlineLarge.restyled(),
        headlineMedium = base.headlineMedium.restyled(),
        headlineSmall = base.headlineSmall.restyled(),
        titleLarge = base.titleLarge.restyled(),
        titleMedium = base.titleMedium.restyled(),
        titleSmall = base.titleSmall.restyled(),
        bodyLarge = base.bodyLarge.restyled(),
        bodyMedium = base.bodyMedium.restyled(),
        bodySmall = base.bodySmall.restyled(),
        labelLarge = base.labelLarge.restyled(),
        labelMedium = base.labelMedium.restyled(),
        labelSmall = base.labelSmall.restyled(),
    )
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun NexoraTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    ThemeState.init(context)

    // Keep "System" mode in sync with the phone setting.
    val systemDark = isSystemInDarkTheme()
    SideEffect { ThemeState.systemDark = systemDark }

    val dark = ThemeState.isDark
    val accent = NexoraColors.PlasmaCyan

    val colorScheme = if (dark) {
        darkColorScheme(
            primary = accent,
            secondary = NexoraColors.NebulaViolet,
            tertiary = NexoraColors.NebulaPink,
            background = NexoraColors.SpaceBlack,
            surface = NexoraColors.DeepNavy,
            onPrimary = NexoraColors.SpaceBlack,
            onSecondary = NexoraColors.SpaceBlack,
            onTertiary = NexoraColors.SpaceBlack,
            onBackground = NexoraColors.StarWhite,
            onSurface = NexoraColors.StarWhite,
            onSurfaceVariant = NexoraColors.StarWhite,
            outline = NexoraColors.Divider,
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = NexoraColors.NebulaViolet,
            tertiary = NexoraColors.NebulaPink,
            background = NexoraColors.SpaceBlack,
            surface = NexoraColors.DeepNavy,
            onPrimary = NexoraColors.SpaceBlack,
            onSecondary = NexoraColors.SpaceBlack,
            onTertiary = NexoraColors.SpaceBlack,
            onBackground = NexoraColors.StarWhite,
            onSurface = NexoraColors.StarWhite,
            onSurfaceVariant = NexoraColors.StarWhite,
            outline = NexoraColors.Divider,
        )
    }

    // Status bar / navigation bar icons must stay readable in Light and Dark.
    val view = LocalView.current
    SideEffect {
        val window = context.findActivity()?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    // Font size: scales every sp value in the whole app.
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * ThemeState.fontSize.scale,
    )

    val fontStyleChoice = ThemeState.fontStyle
    val typography = buildTypography(fontStyleChoice)

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
        ) {
            if (fontStyleChoice == FontStyleChoice.Normal) {
                content()
            } else {
                // Plain Text() calls (no explicit style) follow the chosen font style too.
                val family: FontFamily =
                    if (fontStyleChoice == FontStyleChoice.Stylish) FontFamily.Serif else FontFamily.Default
                val weight: FontWeight? =
                    if (fontStyleChoice == FontStyleChoice.Bold) FontWeight.Bold else null
                val italic: FontStyle? =
                    if (fontStyleChoice == FontStyleChoice.Stylish) FontStyle.Italic else null
                ProvideTextStyle(
                    TextStyle(fontFamily = family, fontWeight = weight, fontStyle = italic),
                ) {
                    content()
                }
            }
        }
    }
}
