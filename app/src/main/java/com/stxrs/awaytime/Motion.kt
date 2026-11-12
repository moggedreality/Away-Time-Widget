package com.stxrs.awaytime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Damped-spring easing: overshoots once, then settles. Used for moves and entrances instead of fades. */
class SpringInterpolator(private val period: Float = 0.5f) : Interpolator {
    override fun getInterpolation(t: Float): Float {
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        val decay = Math.pow(2.0, -10.0 * t)
        return (decay * Math.sin((t - period / 4f) * (2.0 * Math.PI) / period) + 1.0).toFloat()
    }
}

/**
 * All in-app motion goes through here so one setting can tone it down.
 * FULL uses springs and staggered entrances, REDUCED uses short decelerating slides,
 * OFF skips animation entirely. The system "remove animations" switch also forces OFF.
 */
class Motion(private val context: Context, private val settings: WidgetSettings) {

    enum class Mode { FULL, REDUCED, OFF }

    private val spring = SpringInterpolator(0.5f)
    private val decel = DecelerateInterpolator(1.6f)
    private val accel = AccelerateInterpolator(1.4f)

    val mode: Mode
        get() {
            val systemScale = try {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            } catch (e: Exception) {
                1f
            }
            if (systemScale == 0f) return Mode.OFF
            return when (settings.animations) {
                WidgetSettings.ANIM_OFF -> Mode.OFF
                WidgetSettings.ANIM_REDUCED -> Mode.REDUCED
                else -> Mode.FULL
            }
        }

    private fun dp(v: Int): Float = v * context.resources.displayMetrics.density

    /** Runs [block] just before the view's first frame, once it has been measured. */
    fun onFirstDraw(view: View, block: () -> Unit) {
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val observer = view.viewTreeObserver
                if (observer.isAlive) observer.removeOnPreDrawListener(this)
                block()
                return true
            }
        })
    }

    /** Rise-and-settle entrance. Items start offset (never transparent), so nothing fades. */
    fun enter(view: View, index: Int) {
        when (mode) {
            Mode.OFF -> return
            Mode.REDUCED -> {
                view.translationY = dp(14)
                view.animate().translationY(0f).setDuration(170L).setInterpolator(decel).start()
            }
            Mode.FULL -> {
                view.translationY = dp(30)
                view.scaleX = 0.95f
                view.scaleY = 0.95f
                view.animate()
                    .translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(min(index, 8) * 40L)
                    .setDuration(560L)
                    .setInterpolator(spring)
                    .start()
            }
        }
    }

    fun stagger(group: ViewGroup, from: Int = 0) {
        for (i in from until group.childCount) enter(group.getChildAt(i), i - from)
    }

    /** Squish on touch, spring back on release. The listener returns false so clicks still fire. */
    @SuppressLint("ClickableViewAccessibility")
    fun press(view: View) {
        view.setOnTouchListener { v, event ->
            val m = mode
            if (m != Mode.OFF) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN ->
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90L).setInterpolator(decel).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(if (m == Mode.FULL) 450L else 120L)
                            .setInterpolator(if (m == Mode.FULL) spring else decel)
                            .start()
                }
            }
            false
        }
    }

    /** Slides [incoming] in from the side while [old] slides out the other way. dir is +1 or -1. */
    fun swap(container: FrameLayout, old: View?, incoming: View, dir: Int) {
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            if (child !== old) {
                child.animate().cancel()
                container.removeViewAt(i)
            }
        }
        container.addView(incoming, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (old == null) return
        old.animate().cancel()
        val m = mode
        if (m == Mode.OFF || dir == 0) {
            container.removeView(old)
            return
        }
        if (m == Mode.REDUCED) {
            container.removeView(old)
            incoming.translationX = dir * dp(36)
            incoming.animate().translationX(0f).setDuration(170L).setInterpolator(decel).start()
            return
        }
        val w = (if (container.width > 0) container.width else context.resources.displayMetrics.widthPixels).toFloat()
        incoming.translationX = dir * w
        incoming.animate().translationX(0f).setDuration(480L).setInterpolator(spring).start()
        old.animate().translationX(-dir * w).setDuration(320L).setInterpolator(decel)
            .withEndAction { container.removeView(old) }
            .start()
    }

    /** A small settle when something has just changed, e.g. the live preview. */
    fun bump(view: View) {
        if (mode != Mode.FULL) return
        view.animate().cancel()
        view.scaleX = 0.97f
        view.scaleY = 0.97f
        view.animate().scaleX(1f).scaleY(1f).setDuration(450L).setInterpolator(spring).start()
    }

    /** Grows a bar from the left up to [share] of its width. */
    fun fill(view: View, share: Float) {
        view.pivotX = 0f
        val m = mode
        if (m == Mode.OFF) {
            view.scaleX = share
            return
        }
        view.scaleX = 0f
        view.animate().scaleX(share)
            .setStartDelay(150L)
            .setDuration(if (m == Mode.FULL) 900L else 300L)
            .setInterpolator(if (m == Mode.FULL) spring else decel)
            .start()
    }

    fun countUp(view: TextView, target: Long, format: (Long) -> String) {
        if (mode != Mode.FULL || target <= 0L) {
            view.text = format(target)
            return
        }
        val animator = ValueAnimator.ofFloat(0f, target.toFloat())
        animator.duration = 900L
        animator.interpolator = decel
        animator.addUpdateListener { view.text = format((it.animatedValue as Float).toLong()) }
        animator.start()
    }

    fun sheetIn(panel: View, scrim: View) {
        if (mode == Mode.OFF) return
        onFirstDraw(panel) {
            val full = mode == Mode.FULL
            panel.translationY = panel.height.toFloat()
            scrim.alpha = 0f
            scrim.animate().alpha(1f).setDuration(200L).start()
            panel.animate().translationY(0f)
                .setDuration(if (full) 560L else 220L)
                .setInterpolator(if (full) spring else decel)
                .start()
        }
    }

    fun sheetOut(panel: View, scrim: View, end: () -> Unit) {
        if (mode == Mode.OFF) {
            end()
            return
        }
        scrim.animate().alpha(0f).setDuration(200L).start()
        panel.animate().translationY(panel.height.toFloat())
            .setDuration(230L)
            .setInterpolator(accel)
            .withEndAction { end() }
            .start()
    }

    /** Circular reveal used when the in-app theme changes: the new look grows out from the touch point. */
    fun reveal(view: View, cx: Int, cy: Int, end: () -> Unit) {
        if (mode == Mode.OFF || !view.isAttachedToWindow || view.width <= 0 || view.height <= 0) {
            end()
            return
        }
        val radius = hypot(max(cx, view.width - cx).toFloat(), max(cy, view.height - cy).toFloat())
        val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, radius)
        anim.duration = if (mode == Mode.FULL) 650L else 280L
        anim.interpolator = decel
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                end()
            }
        })
        anim.start()
    }
}
