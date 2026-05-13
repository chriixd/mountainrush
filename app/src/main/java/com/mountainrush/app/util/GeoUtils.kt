package com.mountainrush.app.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_R = 6371000.0 // metri

    /** Distanza haversine in metri tra due punti. */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).let { it * it }
        return 2 * EARTH_R * atan2(sqrt(a), sqrt(1 - a))
    }
}
