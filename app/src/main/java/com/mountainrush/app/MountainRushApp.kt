package com.mountainrush.app

import android.app.Application
import android.content.Context
import org.osmdroid.config.Configuration

class MountainRushApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Setup OSMdroid: serve user agent (è obbligatorio dalle policy OSM)
        val prefs = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(applicationContext, prefs)
        Configuration.getInstance().userAgentValue = packageName
    }
}
