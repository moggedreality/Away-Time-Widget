package com.stxrs.awaytime

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout

/**
 * Sheet for choosing standby apps: apps whose time on screen counts as away (a clock, music or
 * navigation app left running on a stand). Changes apply on Save; closing the sheet cancels.
 */
class AppPicker(private val a: MainActivity) {

    fun show(onSaved: () -> Unit) {
        val chosen = HashSet(a.settings.standbyApps)

        a.sheets.show("Standby apps") { col, close ->
            val u = a.ui
            col.addView(u.hint("Time in these apps counts as away, even with the screen on. Good for a clock, music or maps app left running on a stand."))

            // Buttons sit above the list so they stay in reach however many apps there are.
            var refresh: () -> Unit = {}
            val buttons = LinearLayout(a)
            buttons.addView(u.button("Clear all", false) {
                chosen.clear()
                refresh()
            }, LinearLayout.LayoutParams(0, u.dp(52), 1f).apply { marginEnd = u.dp(8) })
            buttons.addView(u.button("Save", true) {
                a.settings.standbyApps = chosen
                close()
                onSaved()
            }, LinearLayout.LayoutParams(0, u.dp(52), 1f))
            col.addView(buttons, u.lp(top = 12))

            val search = EditText(a)
            search.hint = "Search apps"
            search.setSingleLine(true)
            search.textSize = 16f
            search.setTextColor(u.p.text)
            search.setHintTextColor(alphaOf(u.p.subtext, 0.7f))
            search.backgroundTintList = ColorStateList.valueOf(u.p.accent)
            col.addView(search, u.lp(top = 8, bottom = 4))

            val list = LinearLayout(a)
            list.orientation = LinearLayout.VERTICAL
            col.addView(list)
            list.addView(u.hint("Loading apps..."))

            var all: List<Apps.Info> = emptyList()

            fun paint(row: View, check: ImageView, on: Boolean) {
                row.background = if (on) u.rounded(alphaOf(u.p.accent, 0.16f), 16) else u.rippleBg(u.card, 16)
                check.visibility = if (on) View.VISIBLE else View.INVISIBLE
            }

            fun render(query: String) {
                list.removeAllViews()
                val q = query.trim().lowercase()
                val shown = all.filter { q.isEmpty() || it.label.lowercase().contains(q) }
                if (shown.isEmpty()) {
                    list.addView(u.hint(if (all.isEmpty()) "No apps found." else "Nothing matches \"$query\"."))
                    return
                }
                for (app in shown) {
                    val row = LinearLayout(a)
                    row.orientation = LinearLayout.HORIZONTAL
                    row.gravity = Gravity.CENTER_VERTICAL
                    row.setPadding(u.dp(12), u.dp(10), u.dp(14), u.dp(10))

                    val iconView = ImageView(a)
                    iconView.setImageDrawable(app.icon)
                    row.addView(iconView, LinearLayout.LayoutParams(u.dp(34), u.dp(34)).apply { marginEnd = u.dp(14) })
                    row.addView(u.tv(app.label, 15f), LinearLayout.LayoutParams(0, WRAP, 1f))
                    val check = u.icon(R.drawable.ic_check, u.p.accent, 22)
                    row.addView(check)

                    paint(row, check, app.pkg in chosen)
                    row.setOnClickListener {
                        if (!chosen.remove(app.pkg)) chosen.add(app.pkg)
                        paint(row, check, app.pkg in chosen)
                    }
                    list.addView(row, u.lp(top = 6))
                }
            }

            refresh = { render(search.text.toString()) }

            // Loading every app's icon takes a moment, so do it off the main thread.
            Thread {
                val apps = Apps.launchable(a)
                a.runOnUiThread {
                    if (a.isDestroyed || a.isFinishing) return@runOnUiThread
                    // Apps chosen earlier that can't be listed any more (uninstalled) stay chosen until saved over.
                    all = apps
                    render(search.text.toString())
                }
            }.start()

            search.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    render(s?.toString() ?: "")
                }
            })
        }
    }
}
