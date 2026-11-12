package com.stxrs.awaytime

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import kotlin.math.max

/** One tab of the app. build() makes its view; rebuild() refreshes it in place, keeping scroll position. */
interface Screen {
    fun build(): View
    fun rebuild()
}

/**
 * The whole app lives in this one activity: Home, Design, Presets and Settings tabs, with bottom
 * sheets for pickers. The UI is built in code from a palette, so changing the app theme just
 * rebuilds it (with a circular reveal) instead of restarting.
 */
class MainActivity : Activity() {

    lateinit var settings: WidgetSettings
    lateinit var ui: Ui
    lateinit var motion: Motion
    lateinit var sheets: Sheets
    lateinit var pickers: Pickers

    var tab = 0
    var designSection = 0
    var textTarget = "value"

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private lateinit var snapshotView: ImageView
    private lateinit var appLayer: FrameLayout
    private lateinit var sheetLayer: FrameLayout
    private var contentHost: FrameLayout? = null
    private var nav: NavBar? = null
    private var screen: Screen? = null

    private val previews = ArrayList<Preview>()
    private var cachedResult: AwayTimeCalculator.Result? = null
    private var cachedAt = 0L
    private var skipNextResume = true

    // Android 15 forces edge-to-edge and Android 16 removed the opt-out, so the app draws behind the
    // system bars and pads its content by these insets (in px).
    private var insetLeft = 0
    private var insetTop = 0
    private var insetRight = 0
    private var insetBottom = 0
    private var contentColumn: LinearLayout? = null

    // The predictive-back callback (an OnBackInvokedCallback on Android 13+), held only while needed.
    private var backCallback: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = WidgetSettings.get(this)
        pickers = Pickers(this)

        val root = FrameLayout(this)
        snapshotView = ImageView(this)
        snapshotView.visibility = View.GONE
        snapshotView.scaleType = ImageView.ScaleType.FIT_XY
        appLayer = FrameLayout(this)
        sheetLayer = FrameLayout(this)
        root.addView(snapshotView, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(appLayer, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(sheetLayer, FrameLayout.LayoutParams(MATCH, MATCH))

        // Same edge-to-edge look on Android 11-14 as the system now forces on 15+.
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
        root.setOnApplyWindowInsetsListener { _, insets ->
            readInsets(insets)
            applyInsets()
            insets   // not consumed, so the bottom sheets receive them too
        }
        setContentView(root)

        sheets = Sheets(sheetLayer, { ui }, { motion }) { updateBackCallback() }
        buildApp()
    }

    override fun onResume() {
        super.onResume()
        if (skipNextResume) {
            skipNextResume = false
            return
        }
        refreshData()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotation or a device dark-mode switch: rebuild so widths and adaptive colors are right.
        buildApp()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            lastTouchX = ev.rawX
            lastTouchY = ev.rawY
        }
        return super.dispatchTouchEvent(ev)
    }

    /** Android 12 and below. From Android 13 on (and always when targeting 16) the callback below is used instead. */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (sheets.isOpen || tab != 0) handleBack() else super.onBackPressed()
    }

    private fun handleBack() {
        when {
            sheets.isOpen -> sheets.dismissTop()
            tab != 0 -> goTo(0)
        }
    }

    /**
     * Predictive back: onBackPressed is no longer called on apps targeting Android 16. The callback is
     * registered only while there is something to close (an open sheet, or any tab but Home), so at the
     * top level the system still plays its back-to-home animation.
     */
    private fun updateBackCallback() {
        if (Build.VERSION.SDK_INT < 33) return
        val needed = sheets.isOpen || tab != 0
        val dispatcher = onBackInvokedDispatcher
        val existing = backCallback
        if (needed && existing == null) {
            val callback = OnBackInvokedCallback { handleBack() }
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
            backCallback = callback
        } else if (!needed && existing is OnBackInvokedCallback) {
            dispatcher.unregisterOnBackInvokedCallback(existing)
            backCallback = null
        }
    }

    // ---- window insets ----------------------------------------------------------------------

    private fun readInsets(w: WindowInsets) {
        // Before Android 11 the window isn't edge-to-edge here, so the system already keeps clear of the bars.
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = w.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            insetLeft = bars.left
            insetTop = bars.top
            insetRight = bars.right
            insetBottom = bars.bottom
        }
    }

    private fun applyInsets() {
        contentColumn?.setPadding(insetLeft, insetTop, insetRight, 0)
        nav?.setBottomInset(insetBottom)
    }

    // ---- building ---------------------------------------------------------------------------

    private fun buildApp() {
        val palette = Themes.resolveApp(settings, Themes.isNight(this))
        motion = Motion(this, settings)
        ui = Ui(this, palette, motion)
        applyWindowColors(palette)
        previews.clear()

        appLayer.removeAllViews()
        appLayer.setBackgroundColor(palette.bg)

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        val host = FrameLayout(this)
        column.addView(host, LinearLayout.LayoutParams(MATCH, 0, 1f))
        val bar = NavBar(this, ui, motion, tab) { goTo(it) }
        column.addView(bar, LinearLayout.LayoutParams(MATCH, WRAP))
        appLayer.addView(column, FrameLayout.LayoutParams(MATCH, MATCH))
        contentHost = host
        contentColumn = column
        nav = bar

        val s = createScreen(tab)
        screen = s
        host.addView(s.build(), FrameLayout.LayoutParams(MATCH, MATCH))
        applyInsets()
    }

    private fun createScreen(index: Int): Screen = when (index) {
        0 -> HomeScreen(this)
        1 -> DesignScreen(this)
        2 -> PresetsScreen(this)
        else -> SettingsScreen(this)
    }

    @Suppress("DEPRECATION")
    private fun applyWindowColors(p: Palette) {
        window.setBackgroundDrawable(ColorDrawable(p.bg))
        if (Build.VERSION.SDK_INT >= 30) {
            // Edge-to-edge: transparent bars, with the app's own backgrounds showing through them.
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            window.isNavigationBarContrastEnforced = false
        } else {
            window.statusBarColor = p.bg
            window.navigationBarColor = ui.navBg
        }
        val light = !p.isDark
        if (Build.VERSION.SDK_INT >= 30) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (light) mask else 0, mask)
        } else {
            val mask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            val flags = window.decorView.systemUiVisibility
            window.decorView.systemUiVisibility = if (light) flags or mask else flags and mask.inv()
        }
    }

    // ---- navigation -------------------------------------------------------------------------

    fun goTo(index: Int) {
        val host = contentHost ?: return
        if (index == tab && host.childCount > 0) return
        val dir = if (index > tab) 1 else -1
        val old = if (host.childCount > 0) host.getChildAt(host.childCount - 1) else null
        tab = index
        val s = createScreen(index)
        screen = s
        motion.swap(host, old, s.build(), dir)
        nav?.select(index)
        updateBackCallback()
    }

    fun openDesign(section: Int) {
        designSection = section
        val current = screen
        if (tab == 1 && current is DesignScreen) current.select(section) else goTo(1)
    }

    // ---- shared state -----------------------------------------------------------------------

    fun registerPreview(p: Preview) {
        previews.add(p)
    }

    /** Today's numbers, cached for a few seconds so dragging a slider doesn't re-query usage stats. */
    fun result(force: Boolean = false): AwayTimeCalculator.Result? {
        val now = System.currentTimeMillis()
        if (force || now - cachedAt > 4000L) {
            cachedResult = AwayTimeCalculator.compute(this)
            cachedAt = now
        }
        return cachedResult
    }

    fun refreshData() {
        result(true)
        AwayTimeWidget.refreshAll(this)
        screen?.rebuild()
    }

    fun pushWidget() {
        AwayTimeWidget.applySettings(applicationContext)
    }

    /** Cheap redraw while a slider is being dragged. The widget itself updates on release. */
    fun previewChanged() {
        previews.removeAll { !it.isAttachedToWindow }
        previews.forEach { it.refresh(false) }
    }

    /**
     * A look setting changed. Pushes it to the widget, and if the app follows the widget theme and
     * the palette changed, rebuilds the app. [structural] means rows may need to appear or vanish.
     */
    fun styleChanged(structural: Boolean = false) {
        pushWidget()
        val wanted = Themes.resolveApp(settings, Themes.isNight(this))
        if (wanted != ui.p) {
            retheme()
            return
        }
        previews.removeAll { !it.isAttachedToWindow }
        previews.forEach { it.refresh(structural) }
        if (structural) screen?.rebuild()
    }

    fun appThemeChanged() {
        val wanted = Themes.resolveApp(settings, Themes.isNight(this))
        if (wanted != ui.p) retheme() else screen?.rebuild()
    }

    /** Rebuilds with the new palette; the new look grows out from where the user last touched. */
    private fun retheme() {
        val loc = IntArray(2)
        appLayer.getLocationOnScreen(loc)
        val cx = (lastTouchX - loc[0]).toInt().coerceIn(0, max(1, appLayer.width))
        val cy = (lastTouchY - loc[1]).toInt().coerceIn(0, max(1, appLayer.height))
        val shot = snapshot(appLayer)
        buildApp()
        if (shot != null && motion.mode != Motion.Mode.OFF) {
            snapshotView.setImageBitmap(shot)
            snapshotView.visibility = View.VISIBLE
            motion.reveal(appLayer, cx, cy) {
                snapshotView.visibility = View.GONE
                snapshotView.setImageDrawable(null)
            }
        }
    }

    /** Half-resolution capture of the old UI, which sits underneath while the reveal runs. */
    private fun snapshot(v: View): Bitmap? {
        if (v.width <= 0 || v.height <= 0) return null
        return try {
            val bitmap = Bitmap.createBitmap(max(1, v.width / 2), max(1, v.height / 2), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(0.5f, 0.5f)
            v.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    // ---- font loading -----------------------------------------------------------------------

    fun launchFontPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, REQ_FONT)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_FONT || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val name = FontStore.import(this, uri)
        if (name == null) {
            Toast.makeText(this, "That file couldn't be used as a font", Toast.LENGTH_LONG).show()
        } else {
            settings.customFontName = name
            settings.font = WidgetSettings.FONT_CUSTOM
            styleChanged(true)
        }
    }

    companion object {
        private const val REQ_FONT = 41
    }
}
