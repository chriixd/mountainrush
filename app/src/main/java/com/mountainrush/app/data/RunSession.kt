package com.mountainrush.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,           // millis epoch
    val endTime: Long,             // millis epoch
    val durationMs: Long,          // tempo totale di movimento
    val distanceMeters: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevationGainM: Double,
    val elevationLossM: Double,
    val maxAltitudeM: Double,
    val minAltitudeM: Double,
    val turnsCount: Int,
    val pathJson: String           // lista di TrackPoint serializzata
)
