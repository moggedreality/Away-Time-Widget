package com.stxrs.awaytime

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Every option, stored in SharedPreferences and shared by all widget instances.
 * Each option registers itself in [specs]; options flagged inPreset make up a "look",
 * which is what user presets save and restore.
 */
class WidgetSettings private constructor(private val sp: SharedPreferences) {

    private class Spec(val key: String, val default: Any, val inPreset: Boolean)

    private val specs = LinkedHashMap<String, Spec>()

    private inner class Pref<T : Any>(
        private val key: String,
        private val def: T,
        preset: Boolean
    ) : ReadWriteProperty<Any?, T> {
        init {
            specs[key] = Spec(key, def, preset)
        }

        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = when (def) {
            is Boolean -> sp.getBoolean(key, def)
            is Int -> sp.getInt(key, def)
            else -> sp.getString(key, def as String) ?: def
        } as T

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val e = sp.edit()
            when (value) {
                is Boolean -> e.putBoolean(key, value)
                is Int -> e.putInt(key, value)
                else -> e.putString(key, value as String)
            }
            e.apply()
        }
    }

    private fun <T : Any> pref(key: String, def: T, preset: Boolean = true) = Pref(key, def, preset)

    // ---- theme & card -------------------------------------------------------------------
    var theme by pref("theme", "mocha")
    var adaptiveDark by pref("adaptive_dark", "mocha")
    var adaptiveLight by pref("adaptive_light", "latte")
    var accentOverride by pref("accent_override", 0)   // 0 = use the theme's accent
    var bgOverride by pref("bg_override", 0)           // 0 = use the theme's background
    var opacity by pref("opacity", 100)                // card opacity, 0..100

    // ---- layout -------------------------------------------------------------------------
    var align by pref("align", ALIGN_LEFT)
    var padding by pref("padding", 16)                 // dp
    var radius by pref("radius", 24)                   // dp
    var barHeight by pref("bar_height", 4)             // dp
    var barGap by pref("bar_gap", 8)                   // dp above the bar
    var textScale by pref("text_scale", 100)           // percent, applies to every text
    var autoFit by pref("auto_fit", true)              // scale the stack to the widget size

    // ---- font ---------------------------------------------------------------------------
    var font by pref("font", FONT_LIGHT)
    var customFontName by pref("font_name", "", preset = false)

    // ---- content ------------------------------------------------------------------------
    var greeting by pref("greeting", false)
    var greetingMode by pref("greeting_mode", G_TIME)
    var greetingSlots by pref("greeting_slots", DEFAULT_SLOTS)   // 4 lines: morning/afternoon/evening/night
    var greetingList by pref("greeting_list", DEFAULT_LIST)      // one phrase per line
    var name by pref("name", "")
    var dayMode by pref("day_mode", DAY_NONE)
    var showLabel by pref("show_label", true)
    var showBar by pref("show_bar", true)
    var showDetail by pref("show_detail", true)

    // ---- behaviour & app (not part of a look) -------------------------------------------
    var refreshMinutes by pref("refresh_minutes", 15, preset = false)
    var appTheme by pref("app_theme", APP_SYNC, preset = false)
    var animations by pref("animations", ANIM_FULL, preset = false)
    var batteryDismissed by pref("battery_dismissed", false, preset = false)   // "Not now" on the Home card
    var dataSource by pref("data_source", SOURCE_USAGE, preset = false)      // how "screen on" is measured
    private var standbyRaw by pref("standby_apps", "", preset = false)        // package names, one per line

    /** Apps whose time on screen still counts as away (a clock, music or navigation app on a stand). */
    var standbyApps: Set<String>
        get() = standbyRaw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        set(value) {
            standbyRaw = value.sorted().joinToString("\n")
        }

    /** Text styling for one line of the widget. spacing is letter spacing in 1/100 em. */
    inner class Element(kind: String, defSpacing: Int, defBold: Boolean, defGap: Int) {
        var color by pref("t_${kind}_color", 0)        // 0 = theme color
        var size by pref("t_${kind}_size", 100)        // percent
        var spacing by pref("t_${kind}_ls", defSpacing)
        var bold by pref("t_${kind}_bold", defBold)
        var gap by pref("t_${kind}_gap", defGap)       // dp above this line
    }

    private val greetingEl = Element("greeting", 0, false, 0)
    private val dayEl = Element("day", 0, false, 2)
    private val labelEl = Element("label", 12, true, 12)
    private val valueEl = Element("value", 0, false, 4)
    private val detailEl = Element("detail", 0, false, 8)

    fun element(kind: String): Element = when (kind) {
        "greeting" -> greetingEl
        "day" -> dayEl
        "label" -> labelEl
        "detail" -> detailEl
        else -> valueEl
    }

    // ---- looks and presets --------------------------------------------------------------

    fun snapshot(): JSONObject {
        val o = JSONObject()
        for (s in specs.values) {
            if (!s.inPreset) continue
            when (s.default) {
                is Boolean -> o.put(s.key, sp.getBoolean(s.key, s.default))
                is Int -> o.put(s.key, sp.getInt(s.key, s.default))
                else -> o.put(s.key, sp.getString(s.key, s.default as String))
            }
        }
        return o
    }

    /** Applies a saved look. Keys missing from [o] go back to their defaults. */
    fun restore(o: JSONObject) {
        val e = sp.edit()
        for (s in specs.values) {
            if (!s.inPreset) continue
            if (!o.has(s.key)) {
                e.remove(s.key)
                continue
            }
            when (s.default) {
                is Boolean -> e.putBoolean(s.key, o.getBoolean(s.key))
                is Int -> e.putInt(s.key, o.getInt(s.key))
                else -> e.putString(s.key, o.getString(s.key))
            }
        }
        e.apply()
    }

    fun resetStyle() = restore(JSONObject())

    private fun presetStore(): JSONObject = try {
        JSONObject(sp.getString(K_PRESETS, "{}") ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    fun presetNames(): List<String> = presetStore().keys().asSequence().toList()

    fun savePreset(name: String) {
        val store = presetStore()
        store.put(name, snapshot())
        sp.edit().putString(K_PRESETS, store.toString()).apply()
    }

    fun applyPreset(name: String): Boolean {
        val o = presetStore().optJSONObject(name) ?: return false
        restore(o)
        return true
    }

    fun deletePreset(name: String) {
        val store = presetStore()
        store.remove(name)
        sp.edit().putString(K_PRESETS, store.toString()).apply()
    }

    fun presetJson(name: String): JSONObject? = presetStore().optJSONObject(name)

    fun renamePreset(old: String, new: String): Boolean {
        val store = presetStore()
        val saved = store.optJSONObject(old) ?: return false
        store.remove(old)
        store.put(new, saved)
        sp.edit().putString(K_PRESETS, store.toString()).apply()
        return true
    }

    companion object {
        const val ALIGN_LEFT = "left"
        const val ALIGN_CENTER = "center"
        const val ALIGN_RIGHT = "right"

        const val FONT_LIGHT = "light"
        const val FONT_SYSTEM = "system"
        const val FONT_SERIF = "serif"
        const val FONT_MONO = "mono"
        const val FONT_CUSTOM = "custom"

        const val DAY_NONE = "none"
        const val DAY_DAY = "day"
        const val DAY_DATE = "day_date"

        const val G_TIME = "time"
        const val G_CYCLE = "cycle"
        const val G_RANDOM = "random"

        const val SOURCE_SCREEN = "screen"   // screen on/off events
        const val SOURCE_USAGE = "usage"     // app foreground time, the way Digital Wellbeing counts (default)

        const val APP_SYNC = "sync"
        const val ANIM_FULL = "full"
        const val ANIM_REDUCED = "reduced"
        const val ANIM_OFF = "off"

        const val DEFAULT_SLOTS =
            "Good morning, {name}\nGood afternoon, {name}\nGood evening, {name}\nGood night, {name}"
        const val DEFAULT_LIST = "Hello, {name}\nWelcome back\nHope your day is going well"

        private const val K_PRESETS = "user_presets"

        /** A throwaway copy backed by its own file, for rendering preset thumbnails and reading defaults. */
        fun scratch(context: Context): WidgetSettings = WidgetSettings(
            context.applicationContext.getSharedPreferences("away_time_scratch", Context.MODE_PRIVATE)
        )

        fun get(context: Context): WidgetSettings = WidgetSettings(
            context.applicationContext.getSharedPreferences("away_time_prefs", Context.MODE_PRIVATE)
        )
    }
}

/** Ready-made looks shown in the Presets tab. Each starts from the default look. */
class Builtin(val name: String, val blurb: String, val configure: WidgetSettings.() -> Unit)

object Builtins {
    val ALL = listOf(
        Builtin("Classic", "The original Away Time look") { },
        Builtin("Minimal", "Greeting, day and time on a clear background") {
            greeting = true
            dayMode = WidgetSettings.DAY_DAY
            showLabel = false
            showBar = false
            showDetail = false
            opacity = 0
        },
        Builtin("AMOLED clean", "True black, thin bar, no label") {
            theme = Themes.AMOLED.key
            showLabel = false
            barHeight = 3
            radius = 28
        },
        Builtin("Paper", "Warm light theme with a quiet serif") {
            theme = Themes.PAPER.key
            font = WidgetSettings.FONT_SERIF
            align = WidgetSettings.ALIGN_CENTER
            dayMode = WidgetSettings.DAY_DATE
            showBar = false
        }
    )

    fun apply(s: WidgetSettings, b: Builtin) {
        s.resetStyle()
        b.configure(s)
    }
}
