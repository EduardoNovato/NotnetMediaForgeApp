package com.col.notnetmediaforge.ui.components

import java.util.Locale

/** Formatea segundos como hh:mm:ss o mm:ss. */
fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "--:--"
    val s = seconds % 60
    val m = (seconds / 60) % 60
    val h = seconds / 3600
    return if (h > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
