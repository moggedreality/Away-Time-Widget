package com.stxrs.awaytime

import android.content.Intent
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

/** The Settings tab: app theme, animations, widget refresh, usage access and reset. */
class SettingsScreen(private val a: MainActivity) : Screen {

    private var scroll: ScrollView? = null
    private var col: LinearLayout? = null

    private val appPicker = AppPicker(a)

    private val sourceOptions = listOf(
        WidgetSettings.SOURCE_USAGE to "App usage, like Digital Wellbeing (default)",
        WidgetSettings.SOURCE_SCREEN to "Screen on/off"
    )

    private val refreshOptions = listOf(
        "0" to "Off (tap the widget to refresh)",
        "5" to "Every 5 minutes",
        "15" to "Every 15 minutes",
        "30" to "Every 30 minutes",
        "60" to "Every hour"
    )

    override fun build(): View {
        val sv = ScrollView(a)
        sv.isVerticalScrollBarEnabled = false
        val c = a.ui.page()
        sv.addView(c)
        scroll = sv
        col = c
        populate(c, true)
        return sv
    }

    override fun rebuild() {
        val c = col ?: return
        val sv = scroll ?: return
        val y = sv.scrollY
        c.removeAllViews()
        populate(c, false)
        sv.post { sv.scrollTo(0, y) }
    }

    private fun populate(c: LinearLayout, animate: Boolean) {
        val u = a.ui
        val s = a.settings

        c.addView(u.title("Settings", "How the app and the widget behave"))

        c.addView(u.section("App appearance"))
        c.addView(u.switchRow("Match widget look", "App colors follow your widget theme", s.appTheme == WidgetSettings.APP_SYNC) { on ->
            // Turning sync off freezes the app at whatever the widget uses right now.
            s.appTheme = if (on) WidgetSettings.APP_SYNC else s.theme
            a.appThemeChanged()
        })
        if (s.appTheme != WidgetSettings.APP_SYNC) {
            val label = if (s.appTheme == Themes.ADAPTIVE) "Adaptive" else Themes.byKey(s.appTheme).label
            c.addView(u.choiceRow("App theme", label) {
                a.pickers.themes("App theme", s.appTheme, listOf(Themes.ADAPTIVE to "Adaptive")) { key ->
                    s.appTheme = key
                    a.appThemeChanged()
                }
            })
        }

        c.addView(u.section("Motion"))
        val modes = listOf(WidgetSettings.ANIM_FULL, WidgetSettings.ANIM_REDUCED, WidgetSettings.ANIM_OFF)
        c.addView(u.segRow("Animations", listOf("Full", "Reduced", "Off"), modes.indexOf(s.animations).coerceAtLeast(0)) { i ->
            s.animations = modes[i]
        })
        c.addView(u.hint("Full uses springs and staggered entrances. Reduced keeps short slides only, which is lighter on low-end phones. Off removes all motion. Android's own \"remove animations\" setting is always respected."))

        c.addView(u.section("Widget"))
        val current = refreshOptions.firstOrNull { it.first == s.refreshMinutes.toString() }?.second ?: "Every 15 minutes"
        c.addView(u.choiceRow("Auto refresh", current) {
            a.pickers.options("Auto refresh", refreshOptions, s.refreshMinutes.toString()) { key ->
                s.refreshMinutes = key.toInt()
                a.pushWidget()
                rebuild()
            }
        })
        c.addView(u.hint("Android may shift refreshes a little to save battery."))

        c.addView(u.section("Data"))
        c.addView(u.choiceRow("Counts as time on phone", if (s.dataSource == WidgetSettings.SOURCE_USAGE) "App usage" else "Screen on") {
            a.pickers.options("Counts as time on phone", sourceOptions, s.dataSource) { key ->
                s.dataSource = key
                a.refreshData()
                if (!AwayTimeCalculator.hasUsageAccess(a)) askUsageAccess()
            }
        })
        c.addView(u.hint(
            if (s.dataSource == WidgetSettings.SOURCE_USAGE)
                "Time with an app open, the way Digital Wellbeing counts it. Home screen and lock screen time isn't counted, so away time comes out higher."
            else
                "Any time the screen is on, including the lock screen and home screen."
        ))
        c.addView(u.hint("Digital Wellbeing keeps its own data private, so no other app can read it directly. App usage reads the same Android usage stats it is built from, so the numbers come out close to its screen time (small differences are normal)."))

        val standby = s.standbyApps
        val standbySub = when {
            standby.isEmpty() -> "None chosen"
            else -> standby.map { Apps.label(a, it) }.sorted().take(3).joinToString(", ") +
                (if (standby.size > 3) " +${standby.size - 3} more" else "")
        }
        c.addView(u.actionRow("Standby apps", standbySub, R.drawable.ic_apps) {
            appPicker.show { a.refreshData() }
        })
        c.addView(u.hint("Time in standby apps counts as away even with the screen on. Use it for a clock, music or maps app left running on a stand."))

        c.addView(u.section("Battery"))
        val exempt = Battery.isExempt(a)
        c.addView(u.actionRow(
            "Unrestricted battery use",
            if (exempt) "Allowed: Battery Saver won't pause the refresh timer"
            else "Restricted: Battery Saver may pause widget updates",
            R.drawable.ic_refresh
        ) {
            if (exempt) Battery.openSettings(a) else Battery.request(a)
        })
        c.addView(u.hint("Some phones (Xiaomi, OPPO, Vivo, Samsung) also keep their own auto-start or sleeping-apps list. If the widget still stalls, allow Away Time there too."))

        c.addView(u.section("Access"))
        val granted = AwayTimeCalculator.hasUsageAccess(a)
        c.addView(u.actionRow(
            "Usage access",
            if (granted) "Granted" else "Needed to read screen and app usage",
            R.drawable.ic_settings
        ) {
            a.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })

        c.addView(u.section("Reset"))
        c.addView(u.button("Reset widget look to default", false) {
            a.pickers.confirm("Reset the look?", "Theme, text, layout and greetings go back to the defaults. Saved presets are kept.", "Reset") {
                s.resetStyle()
                a.styleChanged(true)
            }
        })
        c.addView(u.hint("Away Time 2.2"))

        if (animate) a.motion.stagger(c)
    }

    /** Both data sources read Android's usage stats, which the user must switch on by hand. */
    private fun askUsageAccess() {
        a.pickers.confirm(
            "Usage access needed",
            "Android only shares usage stats with apps you switch on under Usage access. Turn on Away Time there, then come back.",
            "Open settings"
        ) {
            a.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }
}
