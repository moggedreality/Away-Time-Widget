package com.stxrs.awaytime

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Bottom navigation. The highlight pill grows out of the center with a spring instead of fading in. */
class NavBar(
    ctx: Context,
    private val u: Ui,
    private val motion: Motion,
    selected: Int,
    private val onSelect: (Int) -> Unit
) : LinearLayout(ctx) {

    private class Item(val pill: View, val icon: ImageView, val label: TextView)

    private val items = ArrayList<Item>()
    private var current = selected

    init {
        orientation = LinearLayout.HORIZONTAL
        background = ColorDrawable(u.navBg)
        setPadding(0, u.dp(8), 0, u.dp(8))

        val defs = listOf(
            R.drawable.ic_home to "Home",
            R.drawable.ic_tune to "Design",
            R.drawable.ic_bookmark to "Presets",
            R.drawable.ic_settings to "Settings"
        )
        defs.forEachIndexed { i, def ->
            val column = LinearLayout(ctx)
            column.orientation = LinearLayout.VERTICAL
            column.gravity = Gravity.CENTER_HORIZONTAL

            val frame = FrameLayout(ctx)
            val pill = View(ctx)
            pill.background = u.rounded(alphaOf(u.p.accent, 0.22f), 16)
            frame.addView(pill, FrameLayout.LayoutParams(MATCH, MATCH))
            val icon = u.icon(def.first, u.p.subtext, 24)
            frame.addView(icon, FrameLayout.LayoutParams(u.dp(24), u.dp(24), Gravity.CENTER))
            column.addView(frame, LinearLayout.LayoutParams(u.dp(60), u.dp(32)))

            val label = u.tv(def.second, 11f, u.p.subtext, true)
            column.addView(label, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = u.dp(3) })

            column.setOnClickListener { if (i != current) onSelect(i) }
            addView(column, LinearLayout.LayoutParams(0, WRAP, 1f))
            items.add(Item(pill, icon, label))
        }
        items.forEachIndexed { i, _ -> paint(i, i == current, false) }
    }

    /** Extra bottom padding for the system navigation bar, since the app draws edge-to-edge. */
    fun setBottomInset(px: Int) {
        setPadding(0, u.dp(8), 0, u.dp(8) + px)
    }

    fun select(index: Int) {
        if (index == current) return
        val old = current
        current = index
        paint(old, false, true)
        paint(index, true, true)
    }

    private fun paint(i: Int, on: Boolean, animate: Boolean) {
        val item = items[i]
        item.icon.imageTintList = ColorStateList.valueOf(if (on) u.p.accent else u.p.subtext)
        item.label.setTextColor(if (on) u.p.text else u.p.subtext)
        val target = if (on) 1f else 0f
        val mode = motion.mode
        if (!animate || mode == Motion.Mode.OFF) {
            item.pill.scaleX = target
            return
        }
        if (on && mode == Motion.Mode.FULL) {
            item.pill.animate().scaleX(1f).setDuration(480L).setInterpolator(SpringInterpolator(0.55f)).start()
            item.icon.translationY = -u.dpf(4f)
            item.icon.animate().translationY(0f).setDuration(520L).setInterpolator(SpringInterpolator(0.5f)).start()
        } else {
            item.pill.animate().scaleX(target).setDuration(160L).start()
        }
    }
}
