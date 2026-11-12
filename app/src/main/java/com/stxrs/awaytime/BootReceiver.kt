package com.stxrs.awaytime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are wiped on reboot and app updates, so re-arm the auto-refresh timer here. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AwayTimeWidget.applySettings(context.applicationContext)
    }
}
