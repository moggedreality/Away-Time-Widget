package com.stxrs.awaytime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

/**
 * Battery Saver and Doze pause background work for apps that aren't exempt, which stops the widget's
 * refresh timer. An app can't switch Battery Saver off, but it can ask the user to put it on the
 * "unrestricted" list, which is what these helpers do.
 */
object Battery {

    fun isExempt(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Shows the system's "let this app run in the background?" prompt for Away Time. */
    fun request(activity: Activity) {
        try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
        } catch (e: Exception) {
            openSettings(activity)
        }
    }

    /** Opens the system list of battery-optimized apps, where the exemption can be changed either way. */
    fun openSettings(activity: Activity) {
        try {
            activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Open Settings > Apps > Away Time > Battery and choose Unrestricted",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
