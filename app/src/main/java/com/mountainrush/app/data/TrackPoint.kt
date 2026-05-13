package com.mountainrush.app.data

/** Punto GPS registrato durante la corsa. */
data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val altitude: Double,
    val speedKmh: Double,
    val timestamp: Long,
    val bearing: Float
)
