package com.stxrs.awaytime

import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Renders the widget exactly as the launcher will show it. The tinted gradient behind it lets you
 * see what card opacity and transparency do.
 */
class Preview(private val a: MainActivity, private val heightDp: Int) : FrameLayout(a) {

    private val image = ImageView(a)

    init {
        val u = a.ui
        image.scaleType = ImageView.ScaleType.FIT_CENTER
        val pad = u.dp(12)
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(mixColors(u.p.bg, u.p.accent, 0.28f), mixColors(u.p.bg, u.p.accent, 0.62f))
        ).apply { cornerRadius = u.dpf(22f) }
        addView(image, FrameLayout.LayoutParams(MATCH, u.dp(heightDp)))
        a.registerPreview(this)
        refresh(false)
    }

    fun refresh(animate: Boolean) {
        val s = a.settings
        // Screen width minus the page padding (20dp a side) and this view's own padding (12dp a side).
        val widthDp = resources.configuration.screenWidthDp - 40 - 24
        val palette = Themes.resolve(s, Themes.isNight(a))
        image.setImageBitmap(WidgetRenderer.render(a, s, a.result() ?: SAMPLE, widthDp, heightDp, palette))
        if (animate) a.motion.bump(image)
    }

    companion object {
        /** Shown until usage access is granted, so the styling is still visible. */
        val SAMPLE = AwayTimeCalculator.Result(
            awayMs = 5 * 3_600_000L + 42 * 60_000L,
            screenOnMs = 2 * 3_600_000L + 10 * 60_000L,
            elapsedMs = 7 * 3_600_000L + 52 * 60_000L
        )
    }
}
