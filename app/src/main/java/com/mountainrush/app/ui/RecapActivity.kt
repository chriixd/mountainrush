package com.mountainrush.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mountainrush.app.data.AppDatabase
import com.mountainrush.app.data.RunSession
import com.mountainrush.app.data.TrackPoint
import com.mountainrush.app.databinding.ActivityRecapBinding
import com.mountainrush.app.util.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RUN_ID = "run_id"
    }

    private lateinit var binding: ActivityRecapBinding
    private var runSession: RunSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getLongExtra(EXTRA_RUN_ID, -1L)
        if (id < 0) { finish(); return }

        // Setup mappa OSM
        binding.recapMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.recapMap.setMultiTouchControls(true)

        binding.doneBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        binding.exportBtn.setOnClickListener { exportGpx() }

        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) {
                AppDatabase.get(this@RecapActivity).runDao().getById(id)
            } ?: kotlin.run { finish(); return@launch }
            runSession = r
            populate(r)
        }
    }

    private fun populate(r: RunSession) {
        binding.recapDate.text = Formatter.formatDate(r.startTime)
        binding.recapMaxSpeed.text = Formatter.formatSpeed(r.maxSpeedKmh)
        binding.recapAvgSpeed.text = Formatter.formatSpeed(r.avgSpeedKmh)
        binding.recapDistance.text = Formatter.formatDistanceKm(r.distanceMeters)
        binding.recapTime.text = Formatter.formatDuration(r.durationMs)
        binding.recapGain.text = Formatter.formatAltitude(r.elevationGainM)
        binding.recapLoss.text = Formatter.formatAltitude(r.elevationLossM)
        binding.recapPeak.text = Formatter.formatAltitude(r.maxAltitudeM)
        binding.recapTurns.text = r.turnsCount.toString()

        val pts: List<TrackPoint> = parsePath(r.pathJson)
        if (pts.isNotEmpty()) {
            drawPathOnMap(pts)
            binding.elevationChart.setData(pts)
        }
    }

    private fun parsePath(json: String): List<TrackPoint> = try {
        val type = object : TypeToken<List<TrackPoint>>() {}.type
        Gson().fromJson<List<TrackPoint>>(json, type) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    private fun drawPathOnMap(pts: List<TrackPoint>) {
        val geoPoints = pts.map { GeoPoint(it.lat, it.lon) }
        val polyline = Polyline().apply {
            setPoints(geoPoints)
            outlinePaint.color = Color.parseColor("#FF1F3D")
            outlinePaint.strokeWidth = 8f
        }
        binding.recapMap.overlays.add(polyline)

        // Marker start (verde) e end (rosso)
        val startM = Marker(binding.recapMap).apply {
            position = geoPoints.first()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "START"
        }
        val endM = Marker(binding.recapMap).apply {
            position = geoPoints.last()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "FINISH"
        }
        binding.recapMap.overlays.add(startM)
        binding.recapMap.overlays.add(endM)

        // Bounding box per inquadrare tutto
        val lats = geoPoints.map { it.latitude }
        val lons = geoPoints.map { it.longitude }
        val bb = BoundingBox(lats.max(), lons.max(), lats.min(), lons.min())
        binding.recapMap.post {
            binding.recapMap.zoomToBoundingBox(bb, false, 80)
        }
    }

    /** Esporta GPX standard. */
    private fun exportGpx() {
        val r = runSession ?: return
        val pts = parsePath(r.pathJson)
        if (pts.isEmpty()) {
            Toast.makeText(this, "No track to export", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
        val name = "mountainrush_${sdf.format(Date(r.startTime))}.gpx"
        val gpx = buildGpx(r, pts)

        val outFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), name)
        outFile.writeText(gpx)

        val uri = FileProvider.getUriForFile(
            this, "$packageName.fileprovider", outFile
        )
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(share, "Share GPX"))
    }

    private fun buildGpx(r: RunSession, pts: List<TrackPoint>): String {
        val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("""<gpx version="1.1" creator="MountainRush" xmlns="http://www.topografix.com/GPX/1/1">""").append('\n')
        sb.append("  <trk><name>MountainRush ${sdfIso.format(Date(r.startTime))}</name><trkseg>\n")
        for (p in pts) {
            sb.append("""    <trkpt lat="${p.lat}" lon="${p.lon}"><ele>${p.altitude}</ele><time>${sdfIso.format(Date(p.timestamp))}</time></trkpt>""")
            sb.append('\n')
        }
        sb.append("  </trkseg></trk>\n</gpx>\n")
        return sb.toString()
    }

    override fun onResume() {
        super.onResume()
        binding.recapMap.onResume()
    }
    override fun onPause() {
        super.onPause()
        binding.recapMap.onPause()
    }
}
