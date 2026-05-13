package com.mountainrush.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mountainrush.app.R
import com.mountainrush.app.data.TrackPoint
import com.mountainrush.app.ui.TrackingActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service che riceve aggiornamenti GPS in continuo (anche con schermo spento)
 * e mantiene un RunTracker live esposto via StateFlow.
 */
class LocationTrackingService : Service(), LocationListener {

    companion object {
        const val ACTION_START = "com.mountainrush.START"
        const val ACTION_STOP  = "com.mountainrush.STOP"
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIF_ID = 42

        // Stato globale osservabile - shared con la UI
        val tracker = RunTracker()
        private val _liveState = MutableStateFlow(LiveState())
        val liveState: StateFlow<LiveState> = _liveState
        var isRunning: Boolean = false
            private set
    }

    data class LiveState(
        val currentSpeedKmh: Double = 0.0,
        val maxSpeedKmh: Double = 0.0,
        val avgSpeedKmh: Double = 0.0,
        val distanceMeters: Double = 0.0,
        val elevationGain: Double = 0.0,
        val elevationLoss: Double = 0.0,
        val maxAltitude: Double = 0.0,
        val durationMs: Long = 0L,
        val turns: Int = 0,
        val currentAltitude: Double = 0.0,
        val lastLat: Double = 0.0,
        val lastLon: Double = 0.0,
        val pointCount: Int = 0
    )

    private lateinit var locationManager: LocationManager
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (isRunning) return
        if (!hasPermission()) { stopSelf(); return }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
        tracker.start()
        _liveState.value = LiveState()
        isRunning = true

        try {
            // GPS aggiornato ogni 1 secondo o 2 metri
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 2f, this
            )
        } catch (se: SecurityException) {
            stopSelf()
        }
    }

    private fun stopTracking() {
        if (!isRunning) return
        try { locationManager.removeUpdates(this) } catch (_: SecurityException) {}
        isRunning = false
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onLocationChanged(location: Location) {
        // Filtro accuratezza: scarto fix imprecisi (> 30m)
        if (location.hasAccuracy() && location.accuracy > 30f) return

        val speedKmh = if (location.hasSpeed()) location.speed * 3.6 else 0.0
        val altitude = if (location.hasAltitude()) location.altitude else 0.0
        val bearing = if (location.hasBearing()) location.bearing else 0f

        val tp = TrackPoint(
            lat = location.latitude,
            lon = location.longitude,
            altitude = altitude,
            speedKmh = speedKmh,
            timestamp = location.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            bearing = bearing
        )
        tracker.addPoint(tp)

        _liveState.value = LiveState(
            currentSpeedKmh = speedKmh,
            maxSpeedKmh = tracker.maxSpeedKmh,
            avgSpeedKmh = tracker.avgSpeedKmh(),
            distanceMeters = tracker.distanceMeters,
            elevationGain = tracker.elevationGain,
            elevationLoss = tracker.elevationLoss,
            maxAltitude = if (tracker.maxAltitude == Double.NEGATIVE_INFINITY) 0.0 else tracker.maxAltitude,
            durationMs = System.currentTimeMillis() - tracker.startTimeMs,
            turns = tracker.turnsCount,
            currentAltitude = altitude,
            lastLat = location.latitude,
            lastLon = location.longitude,
            pointCount = tracker.trackPoints.size
        )
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in API 29 ma necessario per compatibilità")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, TrackingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { locationManager.removeUpdates(this) } catch (_: SecurityException) {}
        isRunning = false
    }
}
