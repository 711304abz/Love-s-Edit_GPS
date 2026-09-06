package com.example.lovesedit_gps

import android.content.Context

interface LocationProviderAdapter {
    fun start(
        context: Context,
        apiKey: String,
        onLocation: (lat: Double, lon: Double, accuracy: Float, address: String?) -> Unit,
        onError: (code: Int, message: String) -> Unit
    )
    fun stop()
}