package com.mountainrush.app.service

import com.mountainrush.app.data.TrackPoint
import com.mountainrush.app.util.GeoUtils
import kotlin.math.abs

/**
 * Tiene traccia delle statistiche live di una corsa.
 * Riceve TrackPoint via [addPoint] e mantiene aggiornate distanza, velocità, dislivello, tornanti.
 */
class RunTracker {

    private val points = mutableListOf<TrackPoint>()
    val trackPoints: List<TrackPoint> get() = points.toList()

    var distanceMeters: Double = 0.0
        private set
    var maxSpeedKmh: Double = 0.0
        private set
    var elevationGain: Double = 0.0
        private set
    var elevationLoss: Double = 0.0
        private set
    var maxAltitude: Double = Double.NEGATIVE_INFINITY
        private set
    var minAltitude: Double = Double.POSITIVE_INFINITY
        private set
    var turnsCount: Int = 0
        private set

    // Soglie tornanti: cambio di bearing > 60° tra due tratti consecutivi sufficientemente distanziati
    private var lastBearing: Float? = null
    private var bearingMovedDistance: Double = 0.0

    // Filtro altitudine: ignoro variazioni < 2m (rumore GPS)
    private var lastAltitudeRef: Double? = null
    private val altitudeThreshold = 2.0

    var startTimeMs: Long = 0L
        private set
    var lastTimestamp: Long = 0L
        private set

    // Tempo accumulato in movimento (ignora pause con velocità < 2 km/h)
    var movingDurationMs: Long = 0L
        private set

    fun start() {
        points.clear()
        distanceMeters = 0.0
        maxSpeedKmh = 0.0
        elevationGain = 0.0
        elevationLoss = 0.0
        maxAltitude = Double.NEGATIVE_INFINITY
        minAltitude = Double.POSITIVE_INFINITY
        turnsCount = 0
        lastBearing = null
        bearingMovedDistance = 0.0
        lastAltitudeRef = null
        startTimeMs = System.currentTimeMillis()
        lastTimestamp = startTimeMs
        movingDurationMs = 0L
    }

    fun addPoint(p: TrackPoint) {
        val previous = points.lastOrNull()
        points.add(p)

        // Velocità max
        if (p.speedKmh > maxSpeedKmh) maxSpeedKmh = p.speedKmh

        // Quote min/max
        if (p.altitude > maxAltitude) maxAltitude = p.altitude
        if (p.altitude < minAltitude) minAltitude = p.altitude

        if (previous != null) {
            val d = GeoUtils.distance(previous.lat, previous.lon, p.lat, p.lon)
            // filtro micro-spostamenti < 2 metri (rumore GPS da fermi)
            if (d >= 2.0) {
                distanceMeters += d
                bearingMovedDistance += d

                // Tornanti: confronto il bearing rispetto all'ultimo riferimento
                // ma solo dopo almeno 15m percorsi, per ignorare oscillazioni
                val last = lastBearing
                if (last != null && bearingMovedDistance >= 15.0) {
                    val delta = angleDelta(last, p.bearing)
                    if (delta > 60f) {
                        turnsCount++
                    }
                    lastBearing = p.bearing
                    bearingMovedDistance = 0.0
                } else if (last == null) {
                    lastBearing = p.bearing
                }
            }

            // Tempo di movimento: somma intervalli in cui velocità > 2 km/h
            val dt = p.timestamp - previous.timestamp
            if (dt in 1..10_000 && p.speedKmh > 2.0) {
                movingDurationMs += dt
            }
        }

        // Dislivello con filtro anti-rumore
        val ref = lastAltitudeRef
        if (ref == null) {
            lastAltitudeRef = p.altitude
        } else {
            val diff = p.altitude - ref
            if (abs(diff) >= altitudeThreshold) {
                if (diff > 0) elevationGain += diff else elevationLoss += -diff
                lastAltitudeRef = p.altitude
            }
        }

        lastTimestamp = p.timestamp
    }

    /** Velocità media in km/h calcolata sul tempo di movimento. */
    fun avgSpeedKmh(): Double {
        if (movingDurationMs <= 0) return 0.0
        val hours = movingDurationMs / 3_600_000.0
        return (distanceMeters / 1000.0) / hours
    }

    /** Differenza minima tra due angoli in gradi, 0..180 */
    private fun angleDelta(a: Float, b: Float): Float {
        var d = abs(a - b) % 360f
        if (d > 180f) d = 360f - d
        return d
    }
}
