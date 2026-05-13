package com.mountainrush.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mountainrush.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            binding.permsHint.visibility = android.view.View.GONE
            startTracking()
        } else {
            binding.permsHint.visibility = android.view.View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startBtn.setOnClickListener { onStartClicked() }
        binding.historyBtn.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun onStartClicked() {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val toAsk = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toAsk.isEmpty()) startTracking()
        else permissionLauncher.launch(toAsk.toTypedArray())
    }

    private fun startTracking() {
        startActivity(Intent(this, TrackingActivity::class.java))
    }
}
