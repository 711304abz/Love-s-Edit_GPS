package com.example.lovesedit_gps

import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener

class AMapLocationProviderAdapter : LocationProviderAdapter {

    private var client: AMapLocationClient? = null

    override fun start(
        context: Context,
        apiKey: String,
        onLocation: (Double, Double, Float, String?) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        AMapLocationClient.updatePrivacyShow(context, true, true)
        AMapLocationClient.updatePrivacyAgree(context, true)

        // 高德SDK支持代码里动态设置Key,不需要写死在manifest里
        if (apiKey.isNotBlank()) {
            AMapLocationClient.setApiKey(apiKey)
        }

        client = AMapLocationClient(context.applicationContext)
        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isNeedAddress = true
            interval = 5000
        }
        client?.setLocationOption(option)
        client?.setLocationListener(AMapLocationListener { loc ->
            if (loc != null && loc.errorCode == 0) {
                onLocation(loc.latitude, loc.longitude, loc.accuracy, loc.address)
            } else {
                onError(loc?.errorCode ?: -1, loc?.errorInfo ?: "未知错误")
            }
        })
        client?.startLocation()
    }

    override fun stop() {
        client?.stopLocation()
        client?.onDestroy()
        client = null
    }
}