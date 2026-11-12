package com.stxrs.awaytime

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * The Design tab: a pinned live preview, and four sections underneath.
 * Look = theme, colors, layout and font. Text = per-line text tools. Content = which lines show.
 * Greetings = write your own greeting lines and cycles.
 */
class DesignScreen(private val a: MainActivity) : Screen {

    private var host: FrameLayout? = null
    private var scroll: ScrollView? = null
    private var col: LinearLayout? = null
    private var seg: Segmented? = null

    private val s: WidgetSettings
        get() = a.settings

    private val targets = listOf(
        "greeting" to "Greet", "day" to "Day", "label" to "Label",
        "value" to "Time", "detail" to "Detail", "bar" to "Bar"
    )

    override fun build(): View {
        val u = a.ui
        val root = LinearLayout(a)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(u.dp(20), u.dp(16), u.dp(20), 0)
        root.addView(Preview(a, 118), LinearLayout.LayoutParams(MATCH, WRAP))

        val segmented = Segmented(
            u, a.motion, listOf("Look", "Text", "Content", "Greetings"), a.designSection, u.card
        ) { select(it) }
        seg = segmented
        root.addView(segmented, u.lp(top = 12))

        val sectionHost = FrameLayout(a)
        host = sectionHost
        root.addView(sectionHost, LinearLayout.LayoutParams(MATCH, 0, 1f))
        sectionHost.addView(newSection(), FrameLayout.LayoutParams(MATCH, MATCH))
        return root
    }

    /** Switches section. Also used when another screen asks to open a specific section. */
    fun select(index: Int) {
        val sectionHost = host ?: return
        if (index == a.designSection && col != null) return
        val dir = if (index > a.designSection) 1 else -1
        a.designSection = index
        seg?.choose(index, true)
        val old = if (sectionHost.childCount > 0) sectionHost.getChildAt(sectionHost.childCount - 1) else null
        a.motion.swap(sectionHost, old, newSection(), dir)
    }

    override fun rebuild() {
        val c = col ?: return
        val sv = scroll ?: return
        val y = sv.scrollY
        c.removeAllViews()
        populate(c)
        sv.post { sv.scrollTo(0, y) }
    }

    private fun newSection(): View {
        val sv = ScrollView(a)
        sv.isVerticalScrollBarEnabled = false
        val c = a.ui.page(horizontal = 0, top = 4)
        sv.addView(c)
        scroll = sv
        col = c
        populate(c)
        return sv
    }

    private fun populate(c: LinearLayout) {
        when (a.designSection) {
            0 -> look(c)
            1 -> text(c)
            2 -> content(c)
            else -> greetings(c)
        }
    }

    /** A slider that redraws the preview while dragging and updates the widget on release. */
    private fun slider(
        c: LinearLayout, title: String, lo: Int, hi: Int, value: Int,
        fmt: (Int) -> String = { it.toString() }, set: (Int) -> Unit
    ) {
        c.addView(a.ui.sliderRow(title, lo, hi, value, fmt, { set(it); a.previewChanged() }, { a.styleChanged() }))
    }

    // ---- Look -----------------------------------------------------------------------------

    private fun themeLabel(key: String): String =
        if (key == Themes.ADAPTIVE) "Adaptive" else Themes.byKey(key).label

    private fun look(c: LinearLayout) {
        val u = a.ui
        val pal = Themes.resolve(s, Themes.isNight(a))

        c.addView(u.choiceRow("Theme", themeLabel(s.theme)) {
            a.pickers.themes("Widget theme", s.theme, listOf(Themes.ADAPTIVE to "Adaptive")) { key ->
                s.theme = key
                a.styleChanged(true)
            }
        })
        if (s.theme == Themes.ADAPTIVE) {
            c.addView(u.choiceRow("Dark mode theme", Themes.byKey(s.adaptiveDark).label) {
                a.pickers.themes("Theme in dark mode", s.adaptiveDark, emptyList()) { key ->
                    s.adaptiveDark = key
                    a.styleChanged(true)
                }
            })
            c.addView(u.choiceRow("Light mode theme", Themes.byKey(s.adaptiveLight).label) {
                a.pickers.themes("Theme in light mode", s.adaptiveLight, emptyList()) { key ->
                    s.adaptiveLight = key
                    a.styleChanged(true)
                }
            })
        }
        c.addView(u.colorRow("Accent color", s.accentOverride, pal.accent) {
            a.pickers.color("Accent color", s.accentOverride, pal.accent) {
                s.accentOverride = it
                a.styleChanged(true)
            }
        })
        c.addView(u.colorRow("Background color", s.bgOverride, pal.bg) {
            a.pickers.color("Background color", s.bgOverride, pal.bg) {
                s.bgOverride = it
                a.styleChanged(true)
            }
        })

        c.addView(u.section("Layout"))
        val aligns = listOf(WidgetSettings.ALIGN_LEFT, WidgetSettings.ALIGN_CENTER, WidgetSettings.ALIGN_RIGHT)
        c.addView(u.segRow("Alignment", listOf("Left", "Center", "Right"), aligns.indexOf(s.align).coerceAtLeast(0)) { i ->
            s.align = aligns[i]
            a.styleChanged()
        })
        slider(c, "Card opacity", 0, 100, s.opacity, { "$it%" }) { s.opacity = it }
        slider(c, "Corner radius", 0, 60, s.radius, { "$it dp" }) { s.radius = it }
        slider(c, "Padding", 0, 48, s.padding, { "$it dp" }) { s.padding = it }
        slider(c, "Text size", 40, 200, s.textScale, { "$it%" }) { s.textScale = it }
        c.addView(u.switchRow("Auto-fit", "Scale everything to fill the widget", s.autoFit) {
            s.autoFit = it
            a.styleChanged()
        })

        c.addView(u.section("Font"))
        val fonts = ArrayList<Pair<String, String>>()
        fonts.add(WidgetSettings.FONT_LIGHT to "Sans light")
        fonts.add(WidgetSettings.FONT_SYSTEM to "Sans regular")
        fonts.add(WidgetSettings.FONT_SERIF to "Serif")
        fonts.add(WidgetSettings.FONT_MONO to "Monospace")
        if (s.customFontName.isNotBlank()) {
            fonts.add(WidgetSettings.FONT_CUSTOM to "Custom: ${s.customFontName}")
        }
        val current = fonts.firstOrNull { it.first == s.font }?.second ?: "Sans light"
        c.addView(u.choiceRow("Font", current) {
            val withLoader = fonts + ("load" to "Load a font file (.ttf / .otf)…")
            a.pickers.options("Font", withLoader, s.font) { key ->
                if (key == "load") {
                    a.launchFontPicker()
                } else {
                    s.font = key
                    a.styleChanged()
                }
            }
        })
        c.addView(u.hint("Text is drawn as an image, so any font file you load works in the widget."))
    }

    // ---- Text -----------------------------------------------------------------------------

    private fun defaultColor(key: String, pal: Palette): Int = when (key) {
        "greeting" -> pal.text
        "value" -> pal.accent
        else -> pal.subtext
    }

    private fun text(c: LinearLayout) {
        val u = a.ui
        val index = targets.indexOfFirst { it.first == a.textTarget }.coerceAtLeast(0)
        val rows = LinearLayout(a)
        rows.orientation = LinearLayout.VERTICAL
        c.addView(u.segRow("Edit which line", targets.map { it.second }, index) { i ->
            a.textTarget = targets[i].first
            rows.removeAllViews()
            fillText(rows)
        })
        c.addView(rows, LinearLayout.LayoutParams(MATCH, WRAP))
        fillText(rows)
    }

    private fun fillText(rows: LinearLayout) {
        val u = a.ui
        val key = a.textTarget
        if (key == "bar") {
            slider(rows, "Thickness", 1, 16, s.barHeight, { "$it dp" }) { s.barHeight = it }
            slider(rows, "Space above", 0, 40, s.barGap, { "$it dp" }) { s.barGap = it }
            rows.addView(u.hint("The bar uses your accent color. Change it under Look."))
            return
        }
        val e = s.element(key)
        val pal = Themes.resolve(s, Themes.isNight(a))
        val fallback = defaultColor(key, pal)
        rows.addView(u.colorRow("Color", e.color, if (e.color != 0) e.color else fallback) {
            a.pickers.color("Text color", e.color, fallback) {
                e.color = it
                a.styleChanged()
                rows.removeAllViews()
                fillText(rows)
            }
        })
        slider(rows, "Size", 30, 300, e.size, { "$it%" }) { e.size = it }
        slider(rows, "Letter spacing", -10, 40, e.spacing, { String.format("%.2f em", it / 100f) }) { e.spacing = it }
        slider(rows, "Space above", 0, 40, e.gap, { "$it dp" }) { e.gap = it }
        rows.addView(u.switchRow("Bold", null, e.bold) {
            e.bold = it
            a.styleChanged()
        })
        rows.addView(u.button("Reset this line", false) {
            val defaults = WidgetSettings.scratch(a)
            defaults.resetStyle()
            val d = defaults.element(key)
            e.color = d.color
            e.size = d.size
            e.spacing = d.spacing
            e.bold = d.bold
            e.gap = d.gap
            a.styleChanged()
            rows.removeAllViews()
            fillText(rows)
        })
    }

    // ---- Content --------------------------------------------------------------------------

    private fun content(c: LinearLayout) {
        val u = a.ui
        c.addView(u.switchRow("Greeting", "A line above the time", s.greeting) {
            s.greeting = it
            a.styleChanged(true)
        })
        if (s.greeting) {
            c.addView(u.actionRow("Edit greetings", "Write your own lines and cycles", R.drawable.ic_edit) {
                a.openDesign(3)
            })
        }
        val days = listOf(WidgetSettings.DAY_NONE, WidgetSettings.DAY_DAY, WidgetSettings.DAY_DATE)
        c.addView(u.segRow("Day", listOf("Hidden", "Day", "Day & date"), days.indexOf(s.dayMode).coerceAtLeast(0)) { i ->
            s.dayMode = days[i]
            a.styleChanged()
        })
        c.addView(u.switchRow("Title label", "Small \"AWAY TODAY\" above the time", s.showLabel) {
            s.showLabel = it
            a.styleChanged()
        })
        c.addView(u.switchRow("Progress bar", "How much of today you've been away", s.showBar) {
            s.showBar = it
            a.styleChanged()
        })
        c.addView(u.switchRow("Screen-on detail", "A line showing your screen-on time", s.showDetail) {
            s.showDetail = it
            a.styleChanged()
        })
    }

    // ---- Greetings ------------------------------------------------------------------------

    private fun slotList(): MutableList<String> {
        val parts = s.greetingSlots.split('\n').toMutableList()
        val defaults = WidgetSettings.DEFAULT_SLOTS.split('\n')
        while (parts.size < 4) parts.add(defaults[parts.size])
        return parts
    }

    private fun phraseList(): MutableList<String> =
        s.greetingList.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()

    private fun greetings(c: LinearLayout) {
        val u = a.ui
        c.addView(u.switchRow("Show greeting", "A line above the time", s.greeting) {
            s.greeting = it
            a.styleChanged()
        })
        c.addView(u.choiceRow("Your name", if (s.name.isBlank()) "Not set" else s.name) {
            a.pickers.input("Your name", "Used wherever you write {name}", s.name) { v ->
                s.name = v
                a.styleChanged(true)
            }
        })

        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL
        val modes = listOf(WidgetSettings.G_TIME, WidgetSettings.G_CYCLE, WidgetSettings.G_RANDOM)
        c.addView(u.segRow("Style", listOf("Time of day", "Hourly cycle", "Random"), modes.indexOf(s.greetingMode).coerceAtLeast(0)) { i ->
            s.greetingMode = modes[i]
            a.styleChanged()
            refreshList(box)
        })
        c.addView(box, LinearLayout.LayoutParams(MATCH, WRAP))
        fillList(box)
    }

    private fun refreshList(box: LinearLayout) {
        box.removeAllViews()
        fillList(box)
    }

    private fun fillList(box: LinearLayout) {
        val u = a.ui
        box.addView(u.hint("Shows now: " + WidgetRenderer.greetingText(s, System.currentTimeMillis())))

        if (s.greetingMode == WidgetSettings.G_TIME) {
            val names = listOf("Morning", "Afternoon", "Evening", "Night")
            val ranges = listOf("5am to noon", "noon to 5pm", "5pm to 9pm", "9pm to 5am")
            val slots = slotList()
            for (i in 0 until 4) {
                box.addView(u.actionRow("${names[i]} · ${ranges[i]}", slots[i], R.drawable.ic_edit) {
                    a.pickers.input("${names[i]} greeting", "e.g. Good ${names[i].lowercase()}, {name}", slots[i]) { v ->
                        val list = slotList()
                        list[i] = v.replace('\n', ' ')
                        s.greetingSlots = list.take(4).joinToString("\n")
                        a.styleChanged()
                        refreshList(box)
                    }
                })
            }
        } else {
            val phrases = phraseList()
            phrases.forEachIndexed { i, phrase ->
                box.addView(u.actionRow(phrase, "Tap to edit or delete", R.drawable.ic_edit) {
                    a.pickers.input("Edit greeting", "Greeting", phrase, onDelete = {
                        val list = phraseList()
                        if (i < list.size) list.removeAt(i)
                        s.greetingList = list.joinToString("\n")
                        a.styleChanged()
                        refreshList(box)
                    }) { v ->
                        if (v.isNotEmpty()) {
                            val list = phraseList()
                            if (i < list.size) list[i] = v.replace('\n', ' ')
                            s.greetingList = list.joinToString("\n")
                            a.styleChanged()
                            refreshList(box)
                        }
                    }
                })
            }
            box.addView(u.button("Add a greeting", false) {
                a.pickers.input("New greeting", "e.g. Hello, {name}", "") { v ->
                    if (v.isNotEmpty()) {
                        val list = phraseList()
                        list.add(v.replace('\n', ' '))
                        s.greetingList = list.joinToString("\n")
                        a.styleChanged()
                        refreshList(box)
                    }
                }
            })
        }
        val tail = if (s.greetingMode == WidgetSettings.G_RANDOM) {
            "Random picks a new line each time the widget refreshes."
        } else if (s.greetingMode == WidgetSettings.G_CYCLE) {
            "The widget moves to the next line every hour."
        } else {
            "The widget picks the line that matches the time of day."
        }
        box.addView(u.hint("Write {name} where your name should appear. $tail"))
    }
}
