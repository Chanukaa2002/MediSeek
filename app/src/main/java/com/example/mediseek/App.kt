package com.example.mediseek

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d("MyApplication", "Initializing Cloudinary MediaManager...")
        try {
            val config = mapOf(
                "cloud_name" to "dlohr6hrn",
                "api_key" to "636754332753459",
                "api_secret" to "HW2nA9qeNpljN5jQ-ej3FoaNiOk"
            )
            MediaManager.init(this, config)
            Log.d("MyApplication", "Cloudinary initialization successful.")
        } catch (e: Exception) {
            Log.e("MyApplication", "CRITICAL: Cloudinary initialization failed!", e)
        }
    }
}
