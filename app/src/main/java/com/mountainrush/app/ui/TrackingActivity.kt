package com.mountainrush.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import com.mountainrush.app.data.AppDatabase
import com.mountainrush.app.data.RunSession
import com.mountainrush.app.databinding.ActivityTrackingBinding
import com.mountainrush.app.service.LocationTrackingService
import com.mountainrush.app.util.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Avvia il service appena entriamo (idempotente)
        if (!LocationTrackingService.isRunning) {
            val intent = Intent(this, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
            }
            ContextCompat.startForegroundService(this, intent)
        }

        binding.stopBtn.setOnClickListener { onStop() }

        // Disabilita il back fisico per evitare uscite accidentali durante la corsa
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op: usa STOP */ }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                LocationTrackingService.liveState.collect { state ->
                    binding.speedValue.text = Formatter.formatSpeed(state.currentSpeedKmh)
                    binding.maxSpeedValue.text = Formatter.formatSpeed(state.maxSpeedKmh)
                    binding.avgSpeedValue.text = Formatter.formatSpeed(state.avgSpeedKmh)
                    binding.distValue.text = Formatter.formatDistance(state.distanceMeters)
                    binding.timeValue.text = Formatter.formatDuration(state.durationMs)
                    binding.gainValue.text = Formatter.formatAltitude(state.elevationGain)
                    binding.altValue.text = Formatter.formatAltitude(state.maxAltitude)
                }
            }
        }
    }

    private fun onStop() {
        // Salva e mostra recap
        val tracker = LocationTrackingService.tracker
        val pts = tracker.trackPoints
        if (pts.isEmpty()) {
            stopService()
            finish()
            return
        }
        val now = System.currentTimeMillis()
        val session = RunSession(
            startTime = tracker.startTimeMs,
            endTime = now,
            durationMs = tracker.movingDurationMs.coerceAtLeast(now - tracker.startTimeMs),
            distanceMeters = tracker.distanceMeters,
            avgSpeedKmh = tracker.avgSpeedKmh(),
            maxSpeedKmh = tracker.maxSpeedKmh,
            elevationGainM = tracker.elevationGain,
            elevationLossM = tracker.elevationLoss,
            maxAltitudeM = if (tracker.maxAltitude == Double.NEGATIVE_INFINITY) 0.0 else tracker.maxAltitude,
            minAltitudeM = if (tracker.minAltitude == Double.POSITIVE_INFINITY) 0.0 else tracker.minAltitude,
            turnsCount = tracker.turnsCount,
            pathJson = Gson().toJson(pts)
        )
        stopService()

        lifecycleScope.launch {
            val id = withContext(Dispatchers.IO) {
                AppDatabase.get(this@TrackingActivity).runDao().insert(session)
            }
            startActivity(Intent(this@TrackingActivity, RecapActivity::class.java).apply {
                putExtra(RecapActivity.EXTRA_RUN_ID, id)
            })
            finish()
        }
    }

    private fun stopService() {
        val intent = Intent(this, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        startService(intent)
    }
}
