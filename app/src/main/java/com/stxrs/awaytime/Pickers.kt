package com.stxrs.awaytime

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** Sheet-based pickers: option lists, theme tiles, text input, confirmation and a color picker. */
class Pickers(private val a: MainActivity) {

    fun options(title: String, options: List<Pair<String, String>>, current: String, onPick: (String) -> Unit) {
        a.sheets.show(title) { col, close ->
            val u = a.ui
            for ((key, label) in options) {
                col.addView(u.optionRow(label, key == current) {
                    onPick(key)
                    close()
                })
            }
        }
    }

    fun input(
        title: String,
        hint: String,
        initial: String,
        onDelete: (() -> Unit)? = null,
        onDone: (String) -> Unit
    ) {
        a.sheets.show(title) { col, close ->
            val u = a.ui
            val edit = EditText(a)
            edit.setText(initial)
            edit.hint = hint
            edit.setSingleLine(true)
            edit.textSize = 16f
            edit.setTextColor(u.p.text)
            edit.setHintTextColor(alphaOf(u.p.subtext, 0.7f))
            edit.backgroundTintList = ColorStateList.valueOf(u.p.accent)
            edit.imeOptions = EditorInfo.IME_ACTION_DONE
            edit.setSelection(edit.text.length)
            col.addView(edit, u.lp(top = 6))

            fun save() {
                onDone(edit.text.toString().trim())
                close()
            }
            edit.setOnEditorActionListener { _, _, _ ->
                save()
                true
            }

            val buttons = LinearLayout(a)
            buttons.addView(u.button("Cancel", false) { close() }, LinearLayout.LayoutParams(0, u.dp(52), 1f).apply {
                marginEnd = u.dp(8)
            })
            if (onDelete != null) {
                buttons.addView(u.button("Delete", false) {
                    onDelete()
                    close()
                }, LinearLayout.LayoutParams(0, u.dp(52), 1f).apply { marginEnd = u.dp(8) })
            }
            buttons.addView(u.button("Save", true) { save() }, LinearLayout.LayoutParams(0, u.dp(52), 1f))
            col.addView(buttons, u.lp(top = 4))

            edit.requestFocus()
            edit.postDelayed({
                val imm = a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
            }, 320L)
        }
    }

    fun confirm(title: String, message: String, action: String, onYes: () -> Unit) {
        a.sheets.show(title) { col, close ->
            val u = a.ui
            col.addView(u.hint(message))
            val buttons = LinearLayout(a)
            buttons.addView(u.button("Cancel", false) { close() }, LinearLayout.LayoutParams(0, u.dp(52), 1f).apply {
                marginEnd = u.dp(8)
            })
            buttons.addView(u.button(action, true) {
                onYes()
                close()
            }, LinearLayout.LayoutParams(0, u.dp(52), 1f))
            col.addView(buttons, u.lp(top = 6))
        }
    }

    // ---- theme tiles ----------------------------------------------------------------------

    /**
     * Grid of theme swatches. [special] adds extra tiles first (e.g. Adaptive, or "same as widget").
     * Picking calls [onPick] with the tile's key.
     */
    fun themes(title: String, current: String, special: List<Pair<String, String>>, onPick: (String) -> Unit) {
        a.sheets.show(title) { col, close ->
            val u = a.ui
            val tiles = ArrayList<View>()
            for ((key, label) in special) {
                tiles.add(tile(key, label, key == current) {
                    onPick(key)
                    close()
                })
            }
            for (pal in Themes.ALL) {
                tiles.add(tile(pal.key, pal.label, pal.key == current) {
                    onPick(pal.key)
                    close()
                })
            }
            var i = 0
            while (i < tiles.size) {
                val row = LinearLayout(a)
                row.addView(tiles[i], LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                    setMargins(0, u.dp(6), u.dp(6), 0)
                })
                if (i + 1 < tiles.size) {
                    row.addView(tiles[i + 1], LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                        setMargins(u.dp(6), u.dp(6), 0, 0)
                    })
                } else {
                    row.addView(View(a), LinearLayout.LayoutParams(0, 1, 1f))
                }
                col.addView(row)
                i += 2
            }
        }
    }

    private fun tile(key: String, label: String, selected: Boolean, onClick: () -> Unit): View {
        val u = a.ui
        val night = Themes.isNight(a)
        val stroke = if (selected) u.p.accent else u.p.surface
        val strokeDp = if (selected) 2 else 1

        val swatch = FrameLayout(a)
        if (key == Themes.ADAPTIVE) {
            // Dark theme on the left, light theme on the right, to show it follows the device.
            val dark = Themes.byKey(a.settings.adaptiveDark)
            val light = Themes.byKey(a.settings.adaptiveLight)
            val halves = LinearLayout(a)
            val r = u.dpf(14f)
            halves.addView(sample(dark), LinearLayout.LayoutParams(0, MATCH, 1f).also {
                it.setMargins(u.dp(2), u.dp(2), 0, u.dp(2))
            })
            halves.addView(sample(light), LinearLayout.LayoutParams(0, MATCH, 1f).also {
                it.setMargins(0, u.dp(2), u.dp(2), u.dp(2))
            })
            (halves.getChildAt(0)).background = GradientDrawable().apply {
                setColor(dark.bg)
                cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
            }
            (halves.getChildAt(1)).background = GradientDrawable().apply {
                setColor(light.bg)
                cornerRadii = floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
            }
            swatch.background = u.rounded(0, 16, stroke, strokeDp)
            swatch.addView(halves, FrameLayout.LayoutParams(MATCH, MATCH))
        } else {
            // "Same as widget" mirrors whatever the widget currently uses.
            val pal = if (key == WidgetSettings.APP_SYNC) Themes.resolve(a.settings, night) else Themes.byKey(key)
            swatch.background = u.rounded(pal.bg, 16, stroke, strokeDp)
            swatch.addView(sample(pal), FrameLayout.LayoutParams(MATCH, MATCH))
        }

        val box = LinearLayout(a)
        box.orientation = LinearLayout.VERTICAL
        box.addView(swatch, LinearLayout.LayoutParams(MATCH, u.dp(58)))
        box.addView(u.tv(label, 13f, if (selected) u.p.accent else u.p.text, selected).apply {
            maxLines = 1
            setPadding(u.dp(4), u.dp(6), 0, u.dp(2))
        })
        box.setOnClickListener { onClick() }
        a.motion.press(box)
        return box
    }

    /** "Aa" in the palette's text color with an accent dot, drawn over the tile background. */
    private fun sample(pal: Palette): View {
        val u = a.ui
        val frame = FrameLayout(a)
        frame.addView(u.tv("Aa", 18f, pal.text, true), FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER_VERTICAL or Gravity.START).apply {
            marginStart = u.dp(14)
        })
        frame.addView(View(a).apply { background = u.rounded(pal.accent, 6) }, FrameLayout.LayoutParams(u.dp(12), u.dp(12), Gravity.CENTER_VERTICAL or Gravity.END).apply {
            marginEnd = u.dp(14)
        })
        return frame
    }

    // ---- color ----------------------------------------------------------------------------

    private val swatches = intArrayOf(
        0xFFF38BA8.toInt(), 0xFFFAB387.toInt(), 0xFFF9E2AF.toInt(), 0xFFA6E3A1.toInt(),
        0xFF94E2D5.toInt(), 0xFF89B4FA.toInt(), 0xFFB4BEFE.toInt(), 0xFFCBA6F7.toInt(),
        0xFFF5C2E7.toInt(), 0xFFFFFFFF.toInt(), 0xFF9399B2.toInt(), 0xFF000000.toInt()
    )

    /**
     * Color picker with hue/saturation/brightness sliders and a hex field.
     * [current] is the user's color (0 = theme default), [start] is where the sliders begin.
     * onResult gets the chosen color, or 0 for "use the theme color".
     */
    fun color(title: String, current: Int, start: Int, onResult: (Int) -> Unit) {
        a.sheets.show(title) { col, close ->
            val u = a.ui
            val hsv = FloatArray(3)
            Color.colorToHSV(if (current != 0) current else start, hsv)
            var updating = false

            val swatchView = View(a)
            val hex = EditText(a)
            hex.setSingleLine(true)
            hex.textSize = 16f
            hex.setTextColor(u.p.text)
            hex.backgroundTintList = ColorStateList.valueOf(u.p.accent)
            hex.filters = arrayOf(android.text.InputFilter.LengthFilter(7))

            val header = LinearLayout(a)
            header.gravity = Gravity.CENTER_VERTICAL
            header.addView(swatchView, LinearLayout.LayoutParams(u.dp(56), u.dp(56)).apply { marginEnd = u.dp(16) })
            header.addView(hex, LinearLayout.LayoutParams(0, WRAP, 1f))
            col.addView(header, u.lp(top = 8))

            fun track(colors: IntArray): LayerDrawable {
                val gd = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
                gd.cornerRadius = u.dpf(6f)
                val layer = LayerDrawable(arrayOf(gd))
                layer.setLayerHeight(0, u.dp(12))
                layer.setLayerGravity(0, Gravity.CENTER_VERTICAL)
                return layer
            }

            val hueColors = intArrayOf(
                0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(),
                0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt()
            )
            val hueBar = SeekBar(a)
            val satBar = SeekBar(a)
            val valBar = SeekBar(a)
            hueBar.max = 360
            satBar.max = 100
            valBar.max = 100
            hueBar.progressDrawable = track(hueColors)
            val satTrack = track(intArrayOf(Color.WHITE, Color.RED))
            val valTrack = track(intArrayOf(Color.BLACK, Color.RED))
            satBar.progressDrawable = satTrack
            valBar.progressDrawable = valTrack
            for (bar in listOf(hueBar, satBar, valBar)) {
                bar.thumbTintList = ColorStateList.valueOf(u.p.text)
                bar.splitTrack = false
            }

            fun currentColor(): Int = Color.HSVToColor(hsv)

            fun syncSliders() {
                hueBar.progress = hsv[0].toInt()
                satBar.progress = (hsv[1] * 100).toInt()
                valBar.progress = (hsv[2] * 100).toInt()
            }

            fun refresh(fromHex: Boolean) {
                val c = currentColor()
                swatchView.background = u.rounded(c, 14, u.p.subtext, 1)
                if (!fromHex) {
                    updating = true
                    hex.setText(hexOf(c))
                    updating = false
                }
                val full = Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))
                val topGd = (satTrack.getDrawable(0) as GradientDrawable)
                topGd.setColors(intArrayOf(Color.WHITE, full))
                val valGd = (valTrack.getDrawable(0) as GradientDrawable)
                valGd.setColors(intArrayOf(Color.BLACK, Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f))))
            }

            val listener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    hsv[0] = hueBar.progress.toFloat().coerceIn(0f, 359.9f)
                    hsv[1] = satBar.progress / 100f
                    hsv[2] = valBar.progress / 100f
                    refresh(false)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
            hueBar.setOnSeekBarChangeListener(listener)
            satBar.setOnSeekBarChangeListener(listener)
            valBar.setOnSeekBarChangeListener(listener)

            hex.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (updating) return
                    val raw = s?.toString()?.trim() ?: return
                    val text = if (raw.startsWith("#")) raw else "#$raw"
                    if (text.length != 7) return
                    val parsed = try {
                        Color.parseColor(text)
                    } catch (e: IllegalArgumentException) {
                        return
                    }
                    Color.colorToHSV(parsed, hsv)
                    syncSliders()
                    refresh(true)
                }
            })

            for ((label, bar) in listOf("Hue" to hueBar, "Saturation" to satBar, "Brightness" to valBar)) {
                col.addView(u.tv(label, 13f, u.p.subtext), u.lp(top = 14))
                col.addView(bar, u.lp(top = 2))
            }

            // Quick swatches, two rows of six.
            var index = 0
            for (rowIndex in 0 until 2) {
                val row = LinearLayout(a)
                for (k in 0 until 6) {
                    val c = swatches[index++]
                    val chip = View(a)
                    chip.background = u.rounded(c, 12, u.p.surface, 1)
                    chip.setOnClickListener {
                        Color.colorToHSV(c, hsv)
                        syncSliders()
                        refresh(false)
                    }
                    row.addView(chip, LinearLayout.LayoutParams(0, u.dp(34), 1f).apply {
                        setMargins(if (k == 0) 0 else u.dp(6), 0, 0, 0)
                    })
                }
                col.addView(row, u.lp(top = if (rowIndex == 0) 18 else 8))
            }

            val buttons = LinearLayout(a)
            buttons.addView(u.button("Use theme color", false) {
                onResult(0)
                close()
            }, LinearLayout.LayoutParams(0, u.dp(52), 1f).apply { marginEnd = u.dp(8) })
            buttons.addView(u.button("Apply", true) {
                onResult(currentColor())
                close()
            }, LinearLayout.LayoutParams(0, u.dp(52), 1f))
            col.addView(buttons, u.lp(top = 8))

            syncSliders()
            refresh(false)
        }
    }
}
