package com.aether.nova.phantom.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aether.nova.phantom.overlay.OverlayService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
                intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
            ) {
                val serviceIntent = Intent(ctx, OverlayService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ctx.startForegroundService(serviceIntent)
                    } else {
                        ctx.startService(serviceIntent)
                    }
                } catch (_: Exception) {
                    // Service start failed, will retry via watchdog
                }
            }
        }
    }
}
