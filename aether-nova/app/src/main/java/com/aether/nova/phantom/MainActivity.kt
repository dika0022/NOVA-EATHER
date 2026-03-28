package com.aether.nova.phantom

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aether.nova.phantom.overlay.OverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Settings.canDrawOverlays(this)) {
                statusText.text = "Izin overlay diberikan. Klik Start!"
                requestNotificationPermission()
            } else {
                statusText.text = "Izin overlay diperlukan untuk menampilkan sprite"
                Toast.makeText(this, "Izin overlay ditolak", Toast.LENGTH_SHORT).show()
            }
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                statusText.text = "Semua izin diberikan. Siap start!"
            }
        }

        startButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            } else {
                requestOverlayPermission()
            }
        }

        stopButton.setOnClickListener {
            stopOverlayService()
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val hasOverlay = Settings.canDrawOverlays(this)
        statusText.text = if (hasOverlay) {
            "Izin overlay: OK. Klik Start untuk memulai"
        } else {
            "Perlu izin overlay. Klik Start"
        }
        startButton.isEnabled = true
        stopButton.isEnabled = true
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        try {
            ContextCompat.startForegroundService(this, intent)
            statusText.text = "Aether & Nova aktif!"
            Toast.makeText(this, "Aether & Nova muncul di layar!", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            statusText.text = "Error: ${e.message}"
            Toast.makeText(this, "Gagal memulai service", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        statusText.text = "Aether & Nova dihentikan"
        Toast.makeText(this, "Service dihentikan", Toast.LENGTH_SHORT).show()
    }
}
