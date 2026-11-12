package com.stxrs.awaytime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build

/** Looks up installed apps for the Standby apps picker. Needs the <queries> block in the manifest. */
object Apps {

    class Info(val pkg: String, val label: String, val icon: Drawable)

    private fun query(pm: PackageManager, intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

    /** Apps with a launcher icon, A to Z, without Away Time itself. Loads icons, so call it off the main thread. */
    fun launchable(context: Context): List<Info> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val out = ArrayList<Info>()
        for (ri in query(pm, intent)) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || !seen.add(pkg)) continue
            out.add(Info(pkg, ri.loadLabel(pm).toString(), ri.loadIcon(pm)))
        }
        return out.sortedBy { it.label.lowercase() }
    }

    /** Home screen launchers and system UI. Their foreground time isn't "using an app". */
    fun systemSurfaces(context: Context): Set<String> {
        val pm = context.packageManager
        val set = HashSet<String>()
        set.add("com.android.systemui")
        set.add("android")
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        for (ri in query(pm, home)) ri.activityInfo?.packageName?.let { set.add(it) }
        return set
    }

    /** The app's name, or the package name when it can't be resolved (uninstalled or hidden). */
    fun label(context: Context, pkg: String): String = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(pkg, 0)
        }
        pm.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        pkg
    }
}
