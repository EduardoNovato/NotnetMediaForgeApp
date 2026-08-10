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

/** Formatea bytes como unidades legibles. */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "${bytes} B"
    else String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}
