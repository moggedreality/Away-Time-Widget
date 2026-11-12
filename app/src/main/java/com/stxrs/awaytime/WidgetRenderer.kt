package com.stxrs.awaytime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Draws the whole widget card into one bitmap. That is what makes custom fonts, themes, per-line
 * text styling and the optional rows possible, since RemoteViews can't change typefaces at runtime.
 */
object WidgetRenderer {

    private const val MAX_PIXELS = 600_000f
    private val NAME_TOKEN = Regex("""[,\s]*\{name\}""")

    private enum class Kind(val key: String) {
        GREETING("greeting"), DAY("day"), LABEL("label"), VALUE("value"), BAR("bar"), DETAIL("detail")
    }

    private class Row(val kind: Kind, val text: String)

    private class Placed(val row: Row, val paint: Paint?, val height: Float, val gap: Float)

    fun render(
        context: Context,
        s: WidgetSettings,
        result: AwayTimeCalculator.Result?,
        widthDp: Int,
        heightDp: Int,
        palette: Palette,
        now: Long = System.currentTimeMillis()
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val rawW = max(1f, widthDp * density)
        val rawH = max(1f, heightDp * density)
        // Keep the bitmap small enough for the RemoteViews memory limit; the ImageView scales it back up.
        val shrink = min(1f, sqrt(MAX_PIXELS / (rawW * rawH)))
        val u = density * shrink   // bitmap pixels per dp

        val w = max(1, (widthDp * u).roundToInt())
        val h = max(1, (heightDp * u).roundToInt())
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawCard(canvas, w, h, u, s.opacity.coerceIn(0, 100) / 100f, palette, s.radius.coerceIn(0, 60))

        val base = baseTypeface(context, s)
        val rows = buildRows(s, result, now)
        val pad = s.padding.coerceIn(0, 60) * u
        val availW = w - 2f * pad
        val availH = h - 2f * pad
        val scale = s.textScale.coerceIn(30, 250) / 100f

        fun layout(f: Float): List<Placed> = rows.mapIndexed { i, row ->
            if (row.kind == Kind.BAR) {
                val gap = if (i == 0) 0f else s.barGap * u * f
                Placed(row, null, s.barHeight.coerceAtLeast(1) * u, gap)
            } else {
                val el = s.element(row.kind.key)
                val gap = if (i == 0) 0f else el.gap * u * f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = if (el.bold) Typeface.create(base, Typeface.BOLD) else base
                    textSize = sizeDp(row.kind) * u * f * scale * (el.size.coerceIn(30, 300) / 100f)
                    color = if (el.color != 0) el.color else colorFor(row.kind, palette)
                    textAlign = when (s.align) {
                        WidgetSettings.ALIGN_CENTER -> Paint.Align.CENTER
                        WidgetSettings.ALIGN_RIGHT -> Paint.Align.RIGHT
                        else -> Paint.Align.LEFT
                    }
                    letterSpacing = el.spacing / 100f
                }
                val measured = paint.measureText(row.text)
                if (availW > 0f && measured > availW) {
                    paint.textSize = paint.textSize * availW / measured
                }
                val fm = paint.fontMetrics
                Placed(row, paint, fm.descent - fm.ascent, gap)
            }
        }

        fun totalOf(list: List<Placed>): Float = list.fold(0f) { acc, p -> acc + p.height + p.gap }

        // Auto-fit scales the stack to the widget; with it off, sizes are used exactly as set.
        val f = if (s.autoFit) {
            (availH / max(1f, totalOf(layout(1f)))).coerceIn(0.5f, 1.4f)
        } else {
            1f
        }
        val placed = layout(f)
        val total = totalOf(placed)

        var y = pad + max(0f, (availH - total) / 2f)
        val x = when (s.align) {
            WidgetSettings.ALIGN_CENTER -> w / 2f
            WidgetSettings.ALIGN_RIGHT -> w - pad
            else -> pad
        }

        for (p in placed) {
            y += p.gap
            val paint = p.paint
            if (paint != null) {
                canvas.drawText(p.row.text, x, y - paint.fontMetrics.ascent, paint)
            } else if (result != null) {
                drawBar(canvas, pad, y, w - pad, p.height, result, palette)
            }
            y += p.height
        }
        return bitmap
    }

    private fun drawCard(canvas: Canvas, w: Int, h: Int, u: Float, opacity: Float, p: Palette, radiusDp: Int) {
        if (opacity <= 0f) return
        val radius = radiusDp * u
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(p.bg, opacity) }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, fill)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = u
            color = withAlpha(p.surface, opacity)
        }
        val inset = u / 2f
        canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), radius, radius, stroke)
    }

    private fun drawBar(
        canvas: Canvas, left: Float, top: Float, right: Float, height: Float,
        result: AwayTimeCalculator.Result, p: Palette
    ) {
        val r = height / 2f
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.surface }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent }
        canvas.drawRoundRect(RectF(left, top, right, top + height), r, r, track)
        val share = (result.awayMs.toFloat() / max(1L, result.elapsedMs)).coerceIn(0f, 1f)
        if (share > 0f) {
            val fillRight = left + max(height, (right - left) * share)
            canvas.drawRoundRect(RectF(left, top, fillRight, top + height), r, r, fill)
        }
    }

    private fun buildRows(s: WidgetSettings, result: AwayTimeCalculator.Result?, now: Long): List<Row> {
        val rows = mutableListOf<Row>()

        if (s.greeting) rows += Row(Kind.GREETING, greetingText(s, now))
        when (s.dayMode) {
            WidgetSettings.DAY_DAY -> rows += Row(Kind.DAY, formatDate("EEEE", now))
            WidgetSettings.DAY_DATE -> rows += Row(Kind.DAY, formatDate("EEEE d MMM", now))
        }
        if (s.showLabel) rows += Row(Kind.LABEL, "AWAY TODAY")
        rows += Row(Kind.VALUE, if (result == null) "--" else AwayTimeCalculator.format(result.awayMs))

        if (result == null) {
            // Always shown: without it there is no hint why the widget is empty.
            rows += Row(Kind.DETAIL, "Tap to grant usage access")
        } else {
            if (s.showBar) rows += Row(Kind.BAR, "")
            if (s.showDetail) {
                rows += Row(Kind.DETAIL, "${if (result.usageMode) "apps used" else "screen on"} ${AwayTimeCalculator.format(result.screenOnMs)}")
            }
        }
        return rows
    }

    /** Public so the app can preview the exact text the widget would show. */
    fun greetingText(s: WidgetSettings, now: Long): String {
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        val template = when (s.greetingMode) {
            WidgetSettings.G_CYCLE -> phrases(s.greetingList).let {
                // Moves to the next phrase every hour, deterministic so refreshes don't flicker.
                if (it.isEmpty()) "Hello" else it[((now / 3_600_000L) % it.size).toInt()]
            }
            WidgetSettings.G_RANDOM -> phrases(s.greetingList).let {
                if (it.isEmpty()) "Hello" else it[Random.nextInt(it.size)]
            }
            else -> {
                val idx = when (hour) {
                    in 5..11 -> 0
                    in 12..16 -> 1
                    in 17..20 -> 2
                    else -> 3
                }
                s.greetingSlots.split('\n').getOrNull(idx)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: WidgetSettings.DEFAULT_SLOTS.split('\n')[idx]
            }
        }
        return fillName(template, s.name)
    }

    private fun phrases(raw: String): List<String> =
        raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    private fun fillName(template: String, name: String): String =
        if (name.isBlank()) {
            template.replace(NAME_TOKEN, "").trim().trimStart(',').trim()
        } else {
            template.replace("{name}", name.trim())
        }

    private fun formatDate(skeleton: String, now: Long): String {
        val locale = Locale.getDefault()
        return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
            .format(Date(now))
    }

    private fun baseTypeface(context: Context, s: WidgetSettings): Typeface = when (s.font) {
        WidgetSettings.FONT_SYSTEM -> Typeface.DEFAULT
        WidgetSettings.FONT_SERIF -> Typeface.SERIF
        WidgetSettings.FONT_MONO -> Typeface.MONOSPACE
        WidgetSettings.FONT_CUSTOM -> FontStore.load(context) ?: Typeface.DEFAULT
        else -> Typeface.create("sans-serif-light", Typeface.NORMAL)
    }

    private fun sizeDp(kind: Kind): Float = when (kind) {
        Kind.GREETING -> 16f
        Kind.DAY -> 13f
        Kind.LABEL -> 11f
        Kind.VALUE -> 40f
        Kind.DETAIL -> 12f
        Kind.BAR -> 0f
    }

    private fun colorFor(kind: Kind, p: Palette): Int = when (kind) {
        Kind.GREETING -> p.text
        Kind.VALUE -> p.accent
        else -> p.subtext
    }

    private fun withAlpha(color: Int, factor: Float): Int =
        Color.argb((255 * factor).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))
}
