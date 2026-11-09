package com.nexora.circle.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** App colour choices. "Black" is monochrome: white accent in Dark mode, black accent in Light mode. */
enum class AppAccent(val label: String, val swatch: Color) {
    Cyan("Cyan", Color(0xFF00F0FF)),
    Pink("Pink", Color(0xFFFF4FB0)),
    Purple("Purple", Color(0xFFA855F7)),
    Blue("Blue", Color(0xFF3B82F6)),
    Green("Green", Color(0xFF22C55E)),
    Black("Black", Color(0xFF000000)),
    Orange("Orange", Color(0xFFFF9800)),
}

enum class ThemeMode(val label: String) {
    Dark("Dark"),
    Light("Light"),
    System("System"),
}

enum class FontStyleChoice(val label: String) {
    Normal("Normal"),
    Bold("Bold"),
    Stylish("Stylish"),
}

enum class FontSizeChoice(val label: String, val scale: Float) {
    Small("Small", 0.88f),
    Medium("Medium", 1.0f),
    Large("Large", 1.15f),
}

/**
 * One place that holds the look of the whole app.
 * Every value is Compose state, so changing it recolours / resizes the whole app instantly.
 * Values are saved on the phone and restored the next time the app opens.
 */
object ThemeState {
    private const val PREFS = "nexora_theme"
    private const val K_ACCENT = "accent"
    private const val K_MODE = "mode"
    private const val K_FONT = "font_style"
    private const val K_SIZE = "font_size"

    private var prefs: SharedPreferences? = null

    var accent by mutableStateOf(AppAccent.Cyan)
        private set
    var mode by mutableStateOf(ThemeMode.Dark)
        private set
    var fontStyle by mutableStateOf(FontStyleChoice.Normal)
        private set
    var fontSize by mutableStateOf(FontSizeChoice.Medium)
        private set

    /** Updated by NexoraTheme; only used when mode = System. */
    var systemDark by mutableStateOf(true)

    val isDark: Boolean
        get() = when (mode) {
            ThemeMode.Dark -> true
            ThemeMode.Light -> false
            ThemeMode.System -> systemDark
        }

    /** Safe to call many times; only the first call reads the saved values. */
    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p

        accent = readEnum(p.getString(K_ACCENT, null), AppAccent.Cyan)
        fontStyle = readEnum(p.getString(K_FONT, null), FontStyleChoice.Normal)
        fontSize = readEnum(p.getString(K_SIZE, null), FontSizeChoice.Medium)

        // Older versions saved the mode as "dark" / "light" / "system" in nexora_prefs.
        val saved = p.getString(K_MODE, null)
        val legacy = app.getSharedPreferences("nexora_prefs", Context.MODE_PRIVATE)
            .getString("theme_mode", null)
        mode = when ((saved ?: legacy)?.lowercase()) {
            "light" -> ThemeMode.Light
            "system" -> ThemeMode.System
            else -> ThemeMode.Dark
        }

        val night = app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        systemDark = night == Configuration.UI_MODE_NIGHT_YES
    }

    private inline fun <reified T : Enum<T>> readEnum(name: String?, fallback: T): T {
        if (name == null) return fallback
        return try {
            enumValueOf<T>(name)
        } catch (e: IllegalArgumentException) {
            fallback
        }
    }

    fun setAccent(value: AppAccent) {
        accent = value
        prefs?.edit()?.putString(K_ACCENT, value.name)?.apply()
    }

    fun setMode(value: ThemeMode) {
        mode = value
        prefs?.edit()?.putString(K_MODE, value.name)?.apply()
    }

    fun setFontStyle(value: FontStyleChoice) {
        fontStyle = value
        prefs?.edit()?.putString(K_FONT, value.name)?.apply()
    }

    fun setFontSize(value: FontSizeChoice) {
        fontSize = value
        prefs?.edit()?.putString(K_SIZE, value.name)?.apply()
    }

    fun resetAll() {
        setAccent(AppAccent.Cyan)
        setMode(ThemeMode.Dark)
        setFontStyle(FontStyleChoice.Normal)
        setFontSize(FontSizeChoice.Medium)
    }
}
