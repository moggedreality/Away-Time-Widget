package com.stxrs.awaytime

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

internal const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
internal const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun mixColors(a: Int, b: Int, t: Float): Int {
    val f = t.coerceIn(0f, 1f)
    return Color.argb(
        255,
        (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt()
    )
}

fun alphaOf(color: Int, f: Float): Int =
    Color.argb((255 * f.coerceIn(0f, 1f)).toInt(), Color.red(color), Color.green(color), Color.blue(color))

fun luminanceOf(c: Int): Float =
    (0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)) / 255f

fun hexOf(c: Int): String = String.format("#%06X", c and 0xFFFFFF)

/**
 * The app's view toolkit. Every color comes from the palette [p], so the whole app re-themes when
 * it is rebuilt with a new one. Sizes passed to the helpers are dp unless stated.
 */
class Ui(val ctx: Context, val p: Palette, val motion: Motion) {

    private val density = ctx.resources.displayMetrics.density

    val card: Int = mixColors(p.bg, p.surface, 0.6f)
    val navBg: Int = mixColors(p.bg, p.surface, 0.45f)
    val sheetBg: Int = mixColors(p.bg, p.surface, 0.35f)
    val onAccent: Int = if (luminanceOf(p.accent) > 0.6f) 0xFF14141C.toInt() else 0xFFFFFFFF.toInt()

    fun dp(v: Int): Int = Math.round(v * density)
    fun dpf(v: Float): Float = v * density

    fun rounded(color: Int, radiusDp: Int, stroke: Int = 0, strokeDp: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dpf(radiusDp.toFloat())
            if (stroke != 0 && strokeDp > 0) setStroke(dp(strokeDp), stroke)
        }

    fun rippleBg(color: Int, radiusDp: Int): Drawable =
        RippleDrawable(ColorStateList.valueOf(alphaOf(p.accent, 0.18f)), rounded(color, radiusDp), null)

    /** Layout params with margins in dp; w and h are raw (MATCH, WRAP or pixels). */
    fun lp(w: Int = MATCH, h: Int = WRAP, top: Int = 0, bottom: Int = 0, start: Int = 0, end: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { setMargins(dp(start), dp(top), dp(end), dp(bottom)) }

    fun tv(text: CharSequence, sizeSp: Float = 15f, color: Int = p.text, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        }

    fun icon(res: Int, tint: Int, sizeDp: Int): ImageView = ImageView(ctx).apply {
        setImageResource(res)
        imageTintList = ColorStateList.valueOf(tint)
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    // ---- structure ----------------------------------------------------------------------

    fun page(horizontal: Int = 20, top: Int = 16): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(horizontal), dp(top), dp(horizontal), dp(28))
    }

    fun title(main: String, sub: String): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        addView(tv(main, 28f, p.text, true))
        addView(tv(sub, 14f, p.subtext).apply { setPadding(0, dp(4), 0, 0) })
    }

    fun section(title: String): TextView = tv(title.uppercase(), 12f, p.subtext, true).apply {
        letterSpacing = 0.1f
        layoutParams = lp(top = 26)
    }

    fun hint(text: String): TextView = tv(text, 13f, p.subtext).apply {
        setLineSpacing(0f, 1.15f)
        layoutParams = lp(top = 10)
    }

    // ---- rows ---------------------------------------------------------------------------

    private fun rowBox(onClick: (() -> Unit)?): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = lp(top = 10)
        if (onClick != null) {
            background = rippleBg(card, 18)
            setOnClickListener { onClick() }
            motion.press(this)
        } else {
            background = rounded(card, 18)
        }
    }

    private fun textColumn(title: String, sub: String?): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        addView(tv(title, 15f))
        if (sub != null) addView(tv(sub, 12.5f, p.subtext).apply { setPadding(0, dp(3), 0, 0) })
    }

    fun choiceRow(title: String, value: String, onClick: () -> Unit): View {
        val box = rowBox(onClick)
        box.addView(textColumn(title, null), LinearLayout.LayoutParams(0, WRAP, 1f))
        box.addView(tv(value, 14f, p.accent, true).apply {
            gravity = Gravity.END
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(170)
            setPadding(dp(12), 0, 0, 0)
        })
        return box
    }

    fun optionRow(label: String, selected: Boolean, onClick: () -> Unit): View {
        val box = rowBox(onClick)
        box.addView(tv(label, 15f, if (selected) p.accent else p.text, selected), LinearLayout.LayoutParams(0, WRAP, 1f))
        if (selected) box.addView(icon(R.drawable.ic_check, p.accent, 22))
        return box
    }

    fun actionRow(title: String, sub: String?, iconRes: Int, onClick: () -> Unit): View {
        val box = rowBox(onClick)
        box.addView(icon(iconRes, p.accent, 22).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(16)
        })
        box.addView(textColumn(title, sub), LinearLayout.LayoutParams(0, WRAP, 1f))
        return box
    }

    fun switchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit): View {
        var toggle: Switch? = null
        val box = rowBox { toggle?.toggle() }
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val sw = Switch(ctx).apply {
            isChecked = checked
            thumbTintList = ColorStateList(states, intArrayOf(p.accent, p.subtext))
            trackTintList = ColorStateList(states, intArrayOf(alphaOf(p.accent, 0.45f), p.surface))
            setOnCheckedChangeListener { _, value -> onChange(value) }
        }
        toggle = sw
        box.addView(textColumn(title, sub), LinearLayout.LayoutParams(0, WRAP, 1f))
        box.addView(sw)
        return box
    }

    /**
     * A labelled slider. [onChange] fires while dragging (cheap work only, e.g. redraw the preview);
     * [onDone] fires on release (push the change to the widget).
     */
    fun sliderRow(
        title: String, lo: Int, hi: Int, value: Int,
        fmt: (Int) -> String, onChange: (Int) -> Unit, onDone: () -> Unit
    ): View {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(8))
            background = rounded(card, 18)
            layoutParams = lp(top = 10)
        }
        val shown = tv(fmt(value), 14f, p.accent, true)
        val head = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(tv(title, 15f), LinearLayout.LayoutParams(0, WRAP, 1f))
        head.addView(shown)
        val bar = SeekBar(ctx).apply {
            this.min = lo
            this.max = hi
            progress = value.coerceIn(lo, hi)
            progressTintList = ColorStateList.valueOf(p.accent)
            thumbTintList = ColorStateList.valueOf(p.accent)
            progressBackgroundTintList = ColorStateList.valueOf(p.surface)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        shown.text = fmt(progress)
                        onChange(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    onDone()
                }
            })
        }
        box.addView(head)
        box.addView(bar, lp(top = 4))
        return box
    }

    /** [override] is the user's color (0 = follow the theme); [shown] is the color currently in effect. */
    fun colorRow(title: String, override: Int, shown: Int, onClick: () -> Unit): View {
        val box = rowBox(onClick)
        box.addView(
            textColumn(title, if (override == 0) "Theme color" else hexOf(override)),
            LinearLayout.LayoutParams(0, WRAP, 1f)
        )
        box.addView(View(ctx).apply { background = rounded(shown, 13, p.subtext, 1) }, LinearLayout.LayoutParams(dp(26), dp(26)))
        return box
    }

    fun segRow(title: String, labels: List<String>, selected: Int, onSelect: (Int) -> Unit): View {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(card, 18)
            layoutParams = lp(top = 10)
        }
        box.addView(tv(title, 15f))
        box.addView(Segmented(this, motion, labels, selected, p.bg, onSelect), lp(top = 10))
        return box
    }

    fun button(label: String, filled: Boolean, onClick: () -> Unit): View = TextView(ctx).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(if (filled) onAccent else p.text)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        background = if (filled) rippleBg(p.accent, 16) else RippleDrawable(
            ColorStateList.valueOf(alphaOf(p.accent, 0.18f)), rounded(card, 16, p.surface, 1), null
        )
        layoutParams = lp(h = dp(52), top = 14)
        setOnClickListener { onClick() }
        motion.press(this)
    }
}

/** A pill-shaped selector whose highlight glides between options. */
class Segmented(
    private val u: Ui,
    private val motion: Motion,
    private val labels: List<String>,
    selected: Int,
    trough: Int,
    private val onSelect: (Int) -> Unit
) : FrameLayout(u.ctx) {

    private val indicator = View(u.ctx)
    private val items = ArrayList<TextView>()
    private var index = selected.coerceIn(0, labels.size - 1)
    private var segWidth = 0

    init {
        background = u.rounded(trough, 14)
        val pad = u.dp(4)
        setPadding(pad, pad, pad, pad)
        indicator.background = u.rounded(u.p.accent, 11)
        addView(indicator, FrameLayout.LayoutParams(0, u.dp(36)))

        val row = LinearLayout(u.ctx)
        row.orientation = LinearLayout.HORIZONTAL
        labels.forEachIndexed { i, label ->
            val t = u.tv(label, 12.5f, u.p.subtext, true)
            t.gravity = Gravity.CENTER
            t.maxLines = 1
            t.setOnClickListener {
                if (i != index) {
                    choose(i, true)
                    onSelect(i)
                }
            }
            items.add(t)
            row.addView(t, LinearLayout.LayoutParams(0, u.dp(36), 1f))
        }
        addView(row, FrameLayout.LayoutParams(MATCH, WRAP))
        paint()

        addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val inner = right - left - paddingLeft - paddingRight
            val w = if (labels.isEmpty()) 0 else inner / labels.size
            if (w != segWidth) {
                segWidth = w
                val params = indicator.layoutParams
                params.width = w
                indicator.layoutParams = params
                indicator.translationX = (index * w).toFloat()
            }
        }
    }

    /** Moves the highlight without calling the select callback. */
    fun choose(i: Int, animate: Boolean) {
        index = i.coerceIn(0, labels.size - 1)
        paint()
        val target = (index * segWidth).toFloat()
        val mode = motion.mode
        if (!animate || mode == Motion.Mode.OFF) {
            indicator.translationX = target
        } else if (mode == Motion.Mode.FULL) {
            indicator.animate().translationX(target).setDuration(460L).setInterpolator(SpringInterpolator(0.6f)).start()
        } else {
            indicator.animate().translationX(target).setDuration(160L).start()
        }
    }

    private fun paint() {
        items.forEachIndexed { i, t -> t.setTextColor(if (i == index) u.onAccent else u.p.subtext) }
    }
}
