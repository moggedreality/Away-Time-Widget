package com.stxrs.awaytime

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.max

/** A ScrollView that stops growing at [maxHeightPx], so tall sheets scroll instead of covering the screen. */
class MaxHeightScroll(context: Context, private val maxHeightPx: Int) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            View.MeasureSpec.makeMeasureSpec(maxHeightPx, View.MeasureSpec.AT_MOST)
        )
    }
}

/**
 * Bottom sheets drawn inside the app (not dialog windows), so they share the app theme and the
 * spring motion. Sheets stack; back closes the top one.
 */
class Sheets(
    private val layer: FrameLayout,
    private val ui: () -> Ui,
    private val motion: () -> Motion,
    private val onChange: () -> Unit = {}   // called whenever a sheet opens or closes
) {
    private class Entry(val overlay: View, val close: () -> Unit)

    private val stack = ArrayList<Entry>()

    val isOpen: Boolean
        get() = stack.isNotEmpty()

    fun dismissTop() {
        stack.lastOrNull()?.let { it.close() }
    }

    /** [build] fills the sheet body; the second argument closes the sheet. */
    fun show(title: String?, build: (LinearLayout, () -> Unit) -> Unit) {
        val u = ui()
        val m = motion()
        val ctx = layer.context

        val overlay = FrameLayout(ctx)
        val scrim = View(ctx)
        scrim.setBackgroundColor(0x99000000.toInt())
        overlay.addView(scrim, FrameLayout.LayoutParams(MATCH, MATCH))

        val panel = LinearLayout(ctx)
        panel.orientation = LinearLayout.VERTICAL
        val r = u.dpf(26f)
        panel.background = GradientDrawable().apply {
            setColor(u.sheetBg)
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
        panel.setPadding(u.dp(20), u.dp(10), u.dp(20), u.dp(24))
        panel.isClickable = true   // keeps taps on the panel from reaching the scrim

        val handle = View(ctx)
        handle.background = u.rounded(alphaOf(u.p.subtext, 0.5f), 3)
        panel.addView(handle, LinearLayout.LayoutParams(u.dp(40), u.dp(5)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = u.dp(14)
        })
        if (title != null) {
            panel.addView(u.tv(title, 19f, u.p.text, true), u.lp(bottom = 6))
        }

        val scroll = MaxHeightScroll(ctx, (ctx.resources.displayMetrics.heightPixels * 0.72f).toInt())
        scroll.isVerticalScrollBarEnabled = false
        val content = LinearLayout(ctx)
        content.orientation = LinearLayout.VERTICAL
        scroll.addView(content)
        panel.addView(scroll, LinearLayout.LayoutParams(MATCH, WRAP))

        overlay.addView(panel, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        // Edge-to-edge: keep the sheet clear of the navigation bar and lift it above the keyboard.
        val basePad = u.dp(24)
        overlay.setOnApplyWindowInsetsListener { _, insets ->
            var bottom = 0
            if (Build.VERSION.SDK_INT >= 30) {
                bottom = max(
                    insets.getInsets(WindowInsets.Type.systemBars()).bottom,
                    insets.getInsets(WindowInsets.Type.ime()).bottom
                )
            }
            panel.setPadding(u.dp(20), u.dp(10), u.dp(20), basePad + bottom)
            insets
        }
        layer.addView(overlay, FrameLayout.LayoutParams(MATCH, MATCH))

        var closed = false
        var entryRef: Entry? = null
        fun close() {
            if (closed) return
            closed = true
            entryRef?.let { stack.remove(it) }
            onChange()
            m.sheetOut(panel, scrim) { layer.removeView(overlay) }
        }
        val entry = Entry(overlay) { close() }
        entryRef = entry
        stack.add(entry)
        onChange()

        scrim.setOnClickListener { close() }
        build(content) { close() }
        m.sheetIn(panel, scrim)
    }
}
