package com.stxrs.awaytime

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

data class Palette(
    val key: String,
    val label: String,
    val bg: Int,
    val surface: Int,
    val text: Int,
    val subtext: Int,
    val accent: Int
) {
    val isDark: Boolean
        get() = (0.299f * Color.red(bg) + 0.587f * Color.green(bg) + 0.114f * Color.blue(bg)) / 255f < 0.5f
}

object Themes {
    const val ADAPTIVE = "adaptive"

    private fun p(key: String, label: String, bg: Long, surface: Long, text: Long, sub: Long, accent: Long) =
        Palette(key, label, bg.toInt(), surface.toInt(), text.toInt(), sub.toInt(), accent.toInt())

    val MOCHA = p("mocha", "Catppuccin Mocha", 0xFF1E1E2E, 0xFF313244, 0xFFCDD6F4, 0xFFA6ADC8, 0xFFCBA6F7)
    // True black for OLED panels; surface is lifted so the bar track stays visible
    val AMOLED = p("amoled", "AMOLED black", 0xFF000000, 0xFF2A2A33, 0xFFF2F2F7, 0xFF8E8E99, 0xFFCBA6F7)
    val LATTE = p("latte", "Catppuccin Latte", 0xFFEFF1F5, 0xFFCCD0DA, 0xFF4C4F69, 0xFF6C6F85, 0xFF8839EF)
    val NORD = p("nord", "Nord", 0xFF2E3440, 0xFF3B4252, 0xFFECEFF4, 0xFF9AA5B8, 0xFF88C0D0)
    val GRUVBOX = p("gruvbox", "Gruvbox", 0xFF282828, 0xFF3C3836, 0xFFEBDBB2, 0xFFA89984, 0xFFFABD2F)
    val ROSE = p("rosepine", "Rosé Pine", 0xFF191724, 0xFF26233A, 0xFFE0DEF4, 0xFF908CAA, 0xFFEBBCBA)
    val DRACULA = p("dracula", "Dracula", 0xFF282A36, 0xFF44475A, 0xFFF8F8F2, 0xFFB0B6D8, 0xFFBD93F9)
    val TOKYO = p("tokyonight", "Tokyo Night", 0xFF1A1B26, 0xFF292E42, 0xFFC0CAF5, 0xFF9AA5CE, 0xFF7AA2F7)
    val PAPER = p("paper", "Paper", 0xFFFAF8F4, 0xFFE4E0D8, 0xFF2B2B2B, 0xFF6B6B6B, 0xFFC2410C)

    val ALL = listOf(MOCHA, AMOLED, LATTE, NORD, GRUVBOX, ROSE, DRACULA, TOKYO, PAPER)

    fun byKey(key: String): Palette = ALL.firstOrNull { it.key == key } ?: MOCHA

    /** Key/label pairs for pickers. */
    fun options(withAdaptive: Boolean): List<Pair<String, String>> =
        ALL.map { it.key to it.label } +
            (if (withAdaptive) listOf(ADAPTIVE to "Adaptive (device theme)") else emptyList())

    fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /** The widget's palette: chosen theme, plus the user's accent/background overrides. */
    fun resolve(s: WidgetSettings, night: Boolean): Palette {
        val base = if (s.theme == ADAPTIVE) {
            byKey(if (night) s.adaptiveDark else s.adaptiveLight)
        } else {
            byKey(s.theme)
        }
        return base.copy(
            bg = if (s.bgOverride != 0) s.bgOverride else base.bg,
            accent = if (s.accentOverride != 0) s.accentOverride else base.accent
        )
    }

    /** The app's own palette: synced with the widget, or an independent choice. */
    fun resolveApp(s: WidgetSettings, night: Boolean): Palette = when (s.appTheme) {
        WidgetSettings.APP_SYNC -> resolve(s, night)
        ADAPTIVE -> byKey(if (night) s.adaptiveDark else s.adaptiveLight)
        else -> byKey(s.appTheme)
    }
}
