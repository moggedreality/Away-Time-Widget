package com.stxrs.awaytime

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import org.json.JSONObject

/**
 * The Presets tab. A preset is a saved look (theme, layout, text styling, greetings).
 * Each card shows a live thumbnail rendered with the same code as the widget.
 */
class PresetsScreen(private val a: MainActivity) : Screen {

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
        val s = a.settings
        c.addView(u.title("Presets", "Save how your widget looks and switch back any time"))
        c.addView(u.button("Save current look as a preset", true) { askSave() })

        val mine = s.presetNames()
        if (mine.isEmpty()) {
            c.addView(u.hint("Nothing saved yet. Style the widget in Design, then save it here."))
        } else {
            c.addView(u.section("Your presets"))
            for (name in mine) c.addView(card(name, "Saved look", s.presetJson(name), null))
        }
        c.addView(u.section("Built-in"))
        for (b in Builtins.ALL) c.addView(card(b.name, b.blurb, null, b))

        if (animate) a.motion.stagger(c)
    }

    private fun card(name: String, blurb: String, json: JSONObject?, builtin: Builtin?): View {
        val u = a.ui
        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(u.dp(14), u.dp(14), u.dp(14), u.dp(12))
        box.background = u.rippleBg(u.card, 22)
        box.layoutParams = u.lp(top = 12)

        box.addView(thumbnail(json, builtin), LinearLayout.LayoutParams(MATCH, WRAP))

        val row = LinearLayout(a)
        row.gravity = Gravity.CENTER_VERTICAL
        val labels = LinearLayout(a)
        labels.orientation = LinearLayout.VERTICAL
        labels.addView(u.tv(name, 16f, u.p.text, true))
        labels.addView(u.tv(blurb, 12.5f, u.p.subtext).apply { setPadding(0, u.dp(2), 0, 0) })
        row.addView(labels, LinearLayout.LayoutParams(0, WRAP, 1f))
        if (builtin == null) {
            val more = ImageView(a)
            more.setImageResource(R.drawable.ic_more)
            more.imageTintList = android.content.res.ColorStateList.valueOf(u.p.subtext)
            more.setPadding(u.dp(8), u.dp(8), u.dp(8), u.dp(8))
            more.setOnClickListener { actions(name) }
            row.addView(more, LinearLayout.LayoutParams(u.dp(40), u.dp(40)))
        }
        box.addView(row, u.lp(top = 12))

        box.setOnClickListener {
            if (builtin != null) Builtins.apply(a.settings, builtin) else a.settings.applyPreset(name)
            Toast.makeText(a, "Applied $name", Toast.LENGTH_SHORT).show()
            a.styleChanged(true)
        }
        a.motion.press(box)
        return box
    }

    /** Renders the preset on a scratch copy of the settings so the real widget isn't touched. */
    private fun thumbnail(json: JSONObject?, builtin: Builtin?): View {
        val u = a.ui
        val scratch = WidgetSettings.scratch(a)
        if (builtin != null) Builtins.apply(scratch, builtin)
        else if (json != null) scratch.restore(json)
        else scratch.resetStyle()

        // Screen width minus page padding (20dp a side), card padding (14dp a side) and frame padding (10dp a side).
        val widthDp = a.resources.configuration.screenWidthDp - 40 - 28 - 20
        val bitmap = WidgetRenderer.render(
            a, scratch, Preview.SAMPLE, widthDp, 96, Themes.resolve(scratch, Themes.isNight(a))
        )
        val image = ImageView(a)
        image.setImageBitmap(bitmap)
        image.scaleType = ImageView.ScaleType.FIT_CENTER

        val frame = android.widget.FrameLayout(a)
        frame.setPadding(u.dp(10), u.dp(10), u.dp(10), u.dp(10))
        frame.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(mixColors(u.p.bg, u.p.accent, 0.28f), mixColors(u.p.bg, u.p.accent, 0.62f))
        ).apply { cornerRadius = u.dpf(16f) }
        frame.addView(image, android.widget.FrameLayout.LayoutParams(MATCH, u.dp(96)))
        return frame
    }

    private fun askSave() {
        a.pickers.input("Name this preset", "e.g. Night stand", "") { name ->
            if (name.isNotEmpty()) {
                val existed = a.settings.presetNames().contains(name)
                a.settings.savePreset(name)
                Toast.makeText(a, if (existed) "Updated $name" else "Saved $name", Toast.LENGTH_SHORT).show()
                rebuild()
            }
        }
    }

    private fun actions(name: String) {
        a.sheets.show(name) { col, close ->
            val u = a.ui
            col.addView(u.actionRow("Update with current look", "Overwrite this preset", R.drawable.ic_refresh) {
                a.settings.savePreset(name)
                Toast.makeText(a, "Updated $name", Toast.LENGTH_SHORT).show()
                close()
                rebuild()
            })
            col.addView(u.actionRow("Rename", null, R.drawable.ic_edit) {
                close()
                a.pickers.input("Rename preset", "Name", name) { newName ->
                    if (newName.isNotEmpty() && newName != name) {
                        a.settings.renamePreset(name, newName)
                        rebuild()
                    }
                }
            })
            col.addView(u.actionRow("Delete", "This can't be undone", R.drawable.ic_more) {
                close()
                a.pickers.confirm("Delete $name?", "The saved look will be removed. Your current widget stays as it is.", "Delete") {
                    a.settings.deletePreset(name)
                    rebuild()
                }
            })
        }
    }
}
