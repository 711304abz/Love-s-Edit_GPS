package com.example.lovesedit_gps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class LocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var providerAdapter: LocationProviderAdapter? = null

    // 同时接管 GPS 和 网络定位 两个Provider,避免Fused Location融合时被
    // 未接管的真实基站定位数据拉偏
    private val mockProviders = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

    private val channelId = "location_bridge_channel"
    private val notificationId = 1001

    companion object {
        var lastStatusText: String = "服务启动中..."
        var isRunning: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        setupMockProviders()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, buildNotification("定位服务运行中"))
        isRunning = true
        startLocationUpdates()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "定位桥接服务", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("定位桥接运行中")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(notificationId, buildNotification(text))
    }

    private fun setupMockProviders() {
        mockProviders.forEach { provider ->
            try {
                locationManager.addTestProvider(
                    provider, false, false, false, false,
                    true, true, true,
                    android.location.provider.ProviderProperties.POWER_USAGE_LOW,
                    android.location.provider.ProviderProperties.ACCURACY_FINE
                )
            } catch (e: Exception) {
                try {
                    locationManager.addTestProvider(
                        provider, false, false, false, false,
                        true, true, true, 1, 1
                    )
                } catch (e2: Exception) {
                    lastStatusText = "无法注册模拟Provider[$provider]: ${e2.message}"
                    return@forEach
                }
            }
            try {
                locationManager.setTestProviderEnabled(provider, true)
            } catch (e: Exception) {
            }
        }
    }

    private fun startLocationUpdates() {
        val providerType = PrefsManager.getProvider(this)
        val apiKey = PrefsManager.getApiKey(this)

        providerAdapter = when (providerType) {
            "BAIDU" -> BaiduLocationProviderAdapter()
            else -> AMapLocationProviderAdapter()
        }

        providerAdapter?.start(
            context = this,
            apiKey = apiKey,
            onLocation = { gcjLat, gcjLon, accuracy, address ->
                val wgs = gcj02ToWgs84(gcjLat, gcjLon)
                writeMockLocation(wgs[0], wgs[1], accuracy)

                lastStatusText = """
                    数据源: $providerType
                    原始坐标(GCJ-02): $gcjLat, $gcjLon
                    转换后坐标(WGS-84): ${wgs[0]}, ${wgs[1]}
                    精度: ${accuracy}米
                    地址: ${address ?: "未知"}
                """.trimIndent()

                updateNotification("定位: ${address ?: "已更新坐标"}")
            },
            onError = { code, msg ->
                lastStatusText = "定位失败[$providerType],错误码: $code, $msg"
                updateNotification("定位失败: $msg")
            }
        )
    }

    private fun writeMockLocation(lat: Double, lon: Double, accuracy: Float) {
        mockProviders.forEach { provider ->
            try {
                val mockLocation = Location(provider).apply {
                    latitude = lat
                    longitude = lon
                    this.accuracy = if (accuracy > 0) accuracy else 10f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
                locationManager.setTestProviderLocation(provider, mockLocation)
            } catch (e: Exception) {
                lastStatusText = "写入模拟定位失败[$provider]: ${e.message}"
            }
        }
    }

    private fun gcj02ToWgs84(lat: Double, lon: Double): DoubleArray {
        val a = 6378245.0
        val ee = 0.00669342162296594323

        fun transformLat(x: Double, y: Double): Double {
            var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
            ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0
            ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0
            ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0
            return ret
        }

        fun transformLon(x: Double, y: Double): Double {
            var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
            ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0
            ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0
            ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0
            return ret
        }

        val dLat0 = transformLat(lon - 105.0, lat - 35.0)
        val dLon0 = transformLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * Math.PI
        var magic = Math.sin(radLat)
        magic = 1 - ee * magic * magic
        val sqrtMagic = Math.sqrt(magic)
        val dLat = (dLat0 * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI)
        val dLon = (dLon0 * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI)

        val mgLat = lat + dLat
        val mgLon = lon + dLon
        val realLat = lat * 2 - mgLat
        val realLon = lon * 2 - mgLon

        return doubleArrayOf(realLat, realLon)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        providerAdapter?.stop()
        mockProviders.forEach {
            try { locationManager.removeTestProvider(it) } catch (e: Exception) {}
        }
    }
}