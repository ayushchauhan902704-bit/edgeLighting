package com.edgelighting.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var tvServiceStatus: TextView
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var btnNotificationAccess: Button
    private lateinit var btnOverlayPermission: Button
    private lateinit var ivNotificationStatus: ImageView
    private lateinit var ivOverlayStatus: ImageView
    private lateinit var switchEnabled: SwitchMaterial
    private lateinit var btnTestBlue: Button
    private lateinit var btnTestGreen: Button
    private lateinit var btnTestRed: Button

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updatePermissionStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        setupClickListeners()
        updatePermissionStatus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateServiceStatus()
    }

    private fun initializeViews() {
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        btnStartService = findViewById(R.id.btnStartService)
        btnStopService = findViewById(R.id.btnStopService)
        btnNotificationAccess = findViewById(R.id.btnNotificationAccess)
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission)
        ivNotificationStatus = findViewById(R.id.ivNotificationStatus)
        ivOverlayStatus = findViewById(R.id.ivOverlayStatus)
        switchEnabled = findViewById(R.id.switchEnabled)
        btnTestBlue = findViewById(R.id.btnTestBlue)
        btnTestGreen = findViewById(R.id.btnTestGreen)
        btnTestRed = findViewById(R.id.btnTestRed)
    }

    private fun setupClickListeners() {
        btnStartService.setOnClickListener { startEdgeLightingService() }
        btnStopService.setOnClickListener { stopEdgeLightingService() }
        btnNotificationAccess.setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        btnOverlayPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
        btnTestBlue.setOnClickListener { testEdgeLighting(Color.BLUE) }
        btnTestGreen.setOnClickListener { testEdgeLighting(Color.GREEN) }
        btnTestRed.setOnClickListener { testEdgeLighting(Color.RED) }

        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hasAllPermissions()) startEdgeLightingService() else {
                    switchEnabled.isChecked = false
                    Toast.makeText(this, "Pehle dono permissions Grant karo", Toast.LENGTH_SHORT).show()
                }
            } else stopEdgeLightingService()
        }
    }

    private fun startEdgeLightingService() {
        if (!hasAllPermissions()) { Toast.makeText(this, "Permissions baki hai", Toast.LENGTH_SHORT).show(); return }
        val intent = Intent(this, EdgeLightingService::class.java).apply { action = EdgeLightingService.ACTION_START_SERVICE }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        tvServiceStatus.text = "Service Running - Notification ka wait kar raha hu"
        switchEnabled.isChecked = true
    }

    private fun stopEdgeLightingService() {
        val intent = Intent(this, EdgeLightingService::class.java).apply { action = EdgeLightingService.ACTION_STOP_SERVICE }
        stopService(intent)
        tvServiceStatus.text = "Service Stopped"
        switchEnabled.isChecked = false
    }

    private fun testEdgeLighting(color: Int) {
        if (!hasOverlayPermission()) { Toast.makeText(this, "Overlay permission do", Toast.LENGTH_SHORT).show(); return }
        val intent = Intent(this, EdgeLightingService::class.java).apply {
            action = EdgeLightingService.ACTION_SHOW_EDGE_LIGHTING
            putExtra(EdgeLightingService.EXTRA_COLOR, color)
            putExtra(EdgeLightingService.EXTRA_DURATION, 3000L)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun hasNotificationListenerPermission(): Boolean {
        val cn = ComponentName(this, EdgeNotificationListener::class.java)
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabled?.contains(cn.flattenToString()) == true
    }
    private fun hasOverlayPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
    private fun hasPostNotificationPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true
    private fun hasAllPermissions(): Boolean = hasNotificationListenerPermission() && hasOverlayPermission()

    private fun updatePermissionStatus() {
        val hasNoti = hasNotificationListenerPermission()
        ivNotificationStatus.setColorFilter(ContextCompat.getColor(this, if (hasNoti) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        btnNotificationAccess.text = if (hasNoti) "Granted ✓" else "Grant"
        btnNotificationAccess.isEnabled = !hasNoti

        val hasOverlay = hasOverlayPermission()
        ivOverlayStatus.setColorFilter(ContextCompat.getColor(this, if (hasOverlay) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        btnOverlayPermission.text = if (hasOverlay) "Granted ✓" else "Grant"
        btnOverlayPermission.isEnabled = !hasOverlay

        val all = hasAllPermissions()
        btnStartService.isEnabled = all
        switchEnabled.isEnabled = all
        btnTestBlue.isEnabled = all
        btnTestGreen.isEnabled = all
        btnTestRed.isEnabled = all
    }

    private fun updateServiceStatus() {
        tvServiceStatus.text = if (switchEnabled.isChecked) "Service is running" else "Service is not running"
    }
}
