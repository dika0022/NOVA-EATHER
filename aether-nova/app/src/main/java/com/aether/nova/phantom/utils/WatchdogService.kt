package com.aether.nova.phantom.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.aether.nova.phantom.overlay.OverlayService

class WatchdogService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serviceIntent = Intent(this, OverlayService::class.java)
        try {
            startForegroundService(serviceIntent)
        } catch (_: Exception) {
            // OverlayService might already be running
        }
        stopSelf()
        return START_NOT_STICKY
    }
}
