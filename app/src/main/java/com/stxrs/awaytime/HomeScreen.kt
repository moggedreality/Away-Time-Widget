package com.stxrs.awaytime

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Outline
import android.graphics.Typeface
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The Home tab: today's number, the widget as it currently looks, and shortcuts. */
class HomeScreen(private val a: MainActivity) : Screen {

    private var scroll: ScrollView? = null
    private var col: LinearLayout? = null

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
        val result = a.result()

        val header = LinearLayout(a)
        header.gravity = Gravity.CENTER_VERTICAL
        val logo = ImageView(a)
        logo.setImageResource(R.drawable.logo)
        logo.scaleType = ImageView.ScaleType.CENTER_CROP
        logo.clipToOutline = true
        logo.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.28f)
            }
        }
        header.addView(logo, LinearLayout.LayoutParams(u.dp(48), u.dp(48)))
        val titles = LinearLayout(a)
        titles.orientation = LinearLayout.VERTICAL
        titles.setPadding(u.dp(14), 0, 0, 0)
        titles.addView(u.tv("Away Time", 24f, u.p.text, true))
        titles.addView(u.tv(today(), 13f, u.p.subtext).apply { setPadding(0, u.dp(3), 0, 0) })
        header.addView(titles)
        c.addView(header)

        if (result == null) c.addView(banner())
        if (!Battery.isExempt(a) && !a.settings.batteryDismissed) c.addView(batteryCard())
        c.addView(hero(result))

        c.addView(u.section("Your widget"))
        c.addView(Preview(a, 120), u.lp(top = 10))

        c.addView(u.section("Shortcuts"))
        c.addView(u.actionRow("Customize the look", "Theme, text, layout and greetings", R.drawable.ic_tune) {
            a.openDesign(0)
        })
        c.addView(u.actionRow("Presets", "Save a look and switch between them", R.drawable.ic_bookmark) {
            a.goTo(2)
        })
        c.addView(u.actionRow("Add to home screen", "Place the widget on your launcher", R.drawable.ic_add) {
            pinWidget()
        })
        c.addView(u.actionRow("Refresh now", "Re-read today's screen time", R.drawable.ic_refresh) {
            a.refreshData()
        })

        if (animate) a.motion.stagger(c)
    }

    private fun banner(): View {
        val u = a.ui
        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(u.dp(18), u.dp(16), u.dp(18), u.dp(16))
        box.background = u.rounded(alphaOf(u.p.accent, 0.14f), 20, alphaOf(u.p.accent, 0.5f), 1)
        box.layoutParams = u.lp(top = 18)
        box.addView(u.tv("Usage access needed", 16f, u.p.text, true))
        box.addView(u.tv("Away Time reads Android's screen on/off log. Switch it on for this app to start counting.", 13.5f, u.p.subtext).apply {
            setPadding(0, u.dp(4), 0, 0)
        })
        box.addView(u.button("Open settings", true) {
            a.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        return box
    }

    /** Asks the user to exempt Away Time from Battery Saver so the widget's refresh timer keeps running. */
    private fun batteryCard(): View {
        val u = a.ui
        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(u.dp(18), u.dp(16), u.dp(18), u.dp(16))
        box.background = u.rounded(alphaOf(u.p.accent, 0.14f), 20, alphaOf(u.p.accent, 0.5f), 1)
        box.layoutParams = u.lp(top = 18)
        box.addView(u.tv("Keep the widget up to date", 16f, u.p.text, true))
        box.addView(u.tv("Battery Saver can pause Away Time in the background, so the widget may stop refreshing. " +
            "Allow unrestricted battery use and the refresh timer keeps working.", 13.5f, u.p.subtext).apply {
            setPadding(0, u.dp(4), 0, 0)
        })
        val buttons = LinearLayout(a)
        buttons.addView(u.button("Not now", false) {
            a.settings.batteryDismissed = true
            rebuild()
        }, LinearLayout.LayoutParams(0, u.dp(52), 1f).apply {
            topMargin = u.dp(14)
            marginEnd = u.dp(8)
        })
        buttons.addView(u.button("Allow", true) {
            Battery.request(a)
        }, LinearLayout.LayoutParams(0, u.dp(52), 1f).apply {
            topMargin = u.dp(14)
        })
        box.addView(buttons)
        return box
    }

    private fun hero(result: AwayTimeCalculator.Result?): View {
        val u = a.ui
        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(u.dp(22), u.dp(20), u.dp(22), u.dp(22))
        box.background = u.rounded(u.card, 26)
        box.layoutParams = u.lp(top = 18)

        box.addView(u.tv("AWAY TODAY", 12f, u.p.subtext, true).apply { letterSpacing = 0.12f })
        val big = u.tv("--", 56f, u.p.accent)
        big.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        big.setPadding(0, u.dp(6), 0, u.dp(4))
        box.addView(big)

        if (result == null) {
            box.addView(u.tv("Grant usage access to start counting", 14f, u.p.subtext))
        } else {
            a.motion.countUp(big, result.awayMs) { AwayTimeCalculator.format(it) }
            box.addView(
                u.tv(
                    "${if (result.usageMode) "apps used" else "screen on"} " +
                        "${AwayTimeCalculator.format(result.screenOnMs)} of " +
                        "${AwayTimeCalculator.format(result.elapsedMs)} so far",
                    14f, u.p.subtext
                )
            )
            if (result.standbyMs >= 60_000L) {
                box.addView(
                    u.tv("includes ${AwayTimeCalculator.format(result.standbyMs)} in standby apps", 13f, u.p.subtext)
                        .apply { setPadding(0, u.dp(4), 0, 0) }
                )
            }
            val bar = FrameLayout(a)
            val track = View(a)
            track.background = u.rounded(u.p.surface, 4)
            val fill = View(a)
            fill.background = u.rounded(u.p.accent, 4)
            bar.addView(track, FrameLayout.LayoutParams(MATCH, MATCH))
            bar.addView(fill, FrameLayout.LayoutParams(MATCH, MATCH))
            box.addView(bar, u.lp(h = u.dp(8), top = 18))
            val share = (result.awayMs.toFloat() / maxOf(1L, result.elapsedMs)).coerceIn(0f, 1f)
            a.motion.fill(fill, share)
        }
        return box
    }

    private fun today(): String {
        val locale = Locale.getDefault()
        return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEE d MMMM"), locale).format(Date())
    }

    private fun pinWidget() {
        val manager = AppWidgetManager.getInstance(a)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(a, AwayTimeWidget::class.java), null, null)
        } else {
            Toast.makeText(a, "Long-press your home screen, open Widgets and add Away Time", Toast.LENGTH_LONG).show()
        }
    }
}
