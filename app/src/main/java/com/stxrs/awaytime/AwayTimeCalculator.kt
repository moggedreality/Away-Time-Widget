package com.stxrs.awaytime

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

/**
 * Works out how long the phone has been "away" since midnight, from the usage events Android
 * already records (the same source Digital Wellbeing is built on). Nothing runs in the
 * background to collect this.
 *
 * Two ways to measure time on the phone (WidgetSettings.dataSource):
 *  - screen: the screen was on (default).
 *  - usage: an app was in the foreground, which is how Digital Wellbeing counts. Home screen and
 *    lock screen time is not counted.
 * Time in the user's standby apps never counts as time on the phone in either mode.
 */
object AwayTimeCalculator {

    data class Result(
        val awayMs: Long,                 // time since midnight not spent on the phone
        val screenOnMs: Long,             // time on the phone since midnight (screen on, or apps in use)
        val elapsedMs: Long,              // time since midnight
        val standbyMs: Long = 0L,         // screen-on time spent in standby apps, counted as away
        val usageMode: Boolean = false    // true when screenOnMs is app usage rather than screen time
    )

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** Foreground spans per app screen. Events before midnight are read too, only to know what was open at midnight. */
    private class Tracker(private val assumedStart: Long) {
        private val open = HashMap<String, Long>()
        private val seen = HashSet<String>()
        val spans = ArrayList<LongArray>()

        fun resume(key: String, t: Long) {
            seen.add(key)
            if (!open.containsKey(key)) open[key] = t
        }

        fun pause(key: String, t: Long) {
            // A pause with no resume we saw means it was already open when our window began.
            val start = open.remove(key) ?: if (seen.add(key)) assumedStart else return
            if (t > start) spans.add(longArrayOf(start, t))
        }

        fun closeAll(t: Long) {
            for (start in open.values) if (t > start) spans.add(longArrayOf(start, t))
            open.clear()
        }
    }

    /** Returns null when usage access has not been granted yet. */
    fun compute(
        context: Context,
        now: Long = System.currentTimeMillis(),
        settings: WidgetSettings = WidgetSettings.get(context)
    ): Result? {
        if (!hasUsageAccess(context)) return null

        val dayStart = startOfDay(now)
        val lookback = dayStart - LOOKBACK_MS
        val wantUsage = settings.dataSource == WidgetSettings.SOURCE_USAGE
        val standby = settings.standbyApps
        val skip = if (wantUsage) Apps.systemSurfaces(context) else emptySet()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(lookback, now)
        val event = UsageEvents.Event()

        val screen = ArrayList<LongArray>()
        val standbyFg = Tracker(lookback)
        val counted = Tracker(lookback)
        var onSince: Long? = null   // start of the screen-on interval currently open
        var sawScreenEvent = false
        var sawAppEvent = false

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    if (onSince == null) onSince = event.timeStamp
                    sawScreenEvent = true
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    val start = when {
                        onSince != null -> onSince
                        !sawScreenEvent -> lookback   // screen was already on before our window
                        else -> null                   // duplicate "off" event, ignore
                    }
                    if (start != null && event.timeStamp > start) {
                        screen.add(longArrayOf(start, event.timeStamp))
                    }
                    onSince = null
                    sawScreenEvent = true
                    // Apps can't be in the foreground with the screen off.
                    standbyFg.closeAll(event.timeStamp)
                    counted.closeAll(event.timeStamp)
                }
                // ACTIVITY_RESUMED / ACTIVITY_PAUSED have the same values as the older
                // MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND, so this also works on Android 9.
                UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val pkg = event.packageName ?: continue
                    val key = pkg + "/" + (event.className ?: "")
                    val resumed = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                    sawAppEvent = true
                    val target = when {
                        pkg in standby -> standbyFg
                        wantUsage && pkg !in skip -> counted
                        else -> null
                    }
                    if (target != null) {
                        if (resumed) target.resume(key, event.timeStamp) else target.pause(key, event.timeStamp)
                    }
                }
            }
        }

        if (!sawScreenEvent) {
            // No screen events at all: the screen has stayed in one state the whole time.
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isInteractive) screen.add(longArrayOf(lookback, now))
        } else if (onSince != null) {
            screen.add(longArrayOf(onSince, now))   // screen is on right now
        }
        standbyFg.closeAll(now)
        counted.closeAll(now)

        val screenSpans = clipMerge(screen, dayStart, now)
        val standbySpans = clipMerge(standbyFg.spans, dayStart, now)
        val standbyMs = overlap(screenSpans, standbySpans)

        // Fall back to screen time if Android recorded no app events, so the widget never goes blank.
        val usage = wantUsage && sawAppEvent
        var onMs = if (usage) {
            overlap(screenSpans, clipMerge(counted.spans, dayStart, now))
        } else {
            total(screenSpans) - standbyMs
        }

        val elapsed = (now - dayStart).coerceAtLeast(1L)
        onMs = onMs.coerceIn(0L, elapsed)
        return Result(
            awayMs = elapsed - onMs,
            screenOnMs = onMs,
            elapsedMs = elapsed,
            standbyMs = standbyMs,
            usageMode = usage
        )
    }

    /** Clips to [lo, hi], sorts and joins overlapping spans. */
    private fun clipMerge(spans: List<LongArray>, lo: Long, hi: Long): List<LongArray> {
        val sorted = spans
            .map { longArrayOf(max(it[0], lo), min(it[1], hi)) }
            .filter { it[1] > it[0] }
            .sortedBy { it[0] }
        val out = ArrayList<LongArray>()
        for (s in sorted) {
            val last = out.lastOrNull()
            if (last != null && s[0] <= last[1]) last[1] = max(last[1], s[1]) else out.add(s)
        }
        return out
    }

    private fun total(spans: List<LongArray>): Long = spans.sumOf { it[1] - it[0] }

    /** Length of time covered by both lists. Both must already be merged and sorted. */
    private fun overlap(a: List<LongArray>, b: List<LongArray>): Long {
        var i = 0
        var j = 0
        var sum = 0L
        while (i < a.size && j < b.size) {
            val start = max(a[i][0], b[j][0])
            val end = min(a[i][1], b[j][1])
            if (end > start) sum += end - start
            if (a[i][1] < b[j][1]) i++ else j++
        }
        return sum
    }

    fun format(ms: Long): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private const val LOOKBACK_MS = 12L * 3_600_000L

    private fun startOfDay(now: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
