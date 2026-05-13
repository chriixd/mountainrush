package com.mountainrush.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatter {

    fun formatDuration(ms: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(ms)
        val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun formatSpeed(kmh: Double) = String.format(Locale.US, "%.1f", kmh)

    fun formatDistance(meters: Double): String =
        if (meters >= 1000) String.format(Locale.US, "%.2f km", meters / 1000.0)
        else String.format(Locale.US, "%.0f m", meters)

    fun formatDistanceKm(meters: Double) = String.format(Locale.US, "%.2f", meters / 1000.0)

    fun formatAltitude(m: Double) = String.format(Locale.US, "%.0f m", m)

    fun formatDate(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
        return sdf.format(Date(epochMs))
    }
}
