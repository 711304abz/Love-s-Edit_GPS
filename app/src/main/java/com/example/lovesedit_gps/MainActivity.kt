package com.example.lovesedit_gps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private lateinit var spinnerProvider: Spinner
    private lateinit var editApiKey: EditText
    private val handler = Handler(Looper.getMainLooper())

    private val providerOptions = listOf("AMAP" to "高德地图", "BAIDU" to "百度地图(未接入)")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startLocationService()
        } else {
            statusText.text = "缺少必要权限,无法定位"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        spinnerProvider = findViewById(R.id.spinnerProvider)
        editApiKey = findViewById(R.id.editApiKey)
        val btnStartService: Button = findViewById(R.id.btnStartService)
        val btnStopService: Button = findViewById(R.id.btnStopService)
        val btnBattery: Button = findViewById(R.id.btnBattery)
        val btnSaveKey: Button = findViewById(R.id.btnSaveKey)

        spinnerProvider.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, providerOptions.map { it.second }
        )

        // 回显已保存的配置
        val savedProvider = PrefsManager.getProvider(this)
        val savedIndex = providerOptions.indexOfFirst { it.first == savedProvider }
        if (savedIndex >= 0) spinnerProvider.setSelection(savedIndex)
        editApiKey.setText(PrefsManager.getApiKey(this))

        btnStartService.setOnClickListener { checkPermissionsAndStart() }
        btnStopService.setOnClickListener { stopService(Intent(this, LocationService::class.java)) }
        btnBattery.setOnClickListener { requestIgnoreBatteryOptimization() }

        btnSaveKey.setOnClickListener {
            val selected = providerOptions[spinnerProvider.selectedItemPosition].first
            val key = editApiKey.text.toString().trim()
            if (key.isBlank()) {
                Toast.makeText(this, "请先填写API Key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PrefsManager.saveConfig(this, selected, key)
            stopService(Intent(this, LocationService::class.java))
            handler.postDelayed({ checkPermissionsAndStart() }, 500)
            Toast.makeText(this, "已保存,服务重启中...", Toast.LENGTH_SHORT).show()
        }

        checkPermissionsAndStart()
        requestIgnoreBatteryOptimization()

        handler.post(object : Runnable {
            override fun run() {
                statusText.text = LocationService.lastStatusText +
                        "\n\n服务运行状态: ${if (LocationService.isRunning) "运行中" else "未运行"}"
                handler.postDelayed(this, 2000)
            }
        })
    }

    private fun checkPermissionsAndStart() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val allGranted = permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            startLocationService()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun startLocationService() {
        val apiKey = PrefsManager.getApiKey(this)
        if (apiKey.isBlank()) {
            statusText.text = "请先在下方填入定位SDK的API Key并保存"
            return
        }
        val serviceIntent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                })
            } catch (e: Exception) {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}