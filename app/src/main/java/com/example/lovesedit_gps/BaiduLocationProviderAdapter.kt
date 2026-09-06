package com.example.lovesedit_gps

import android.content.Context

/**
 * 百度定位适配器 —— 目前是预留位,尚未真正接入。
 *
 * 原因:
 * 1. 需要额外在 build.gradle.kts 里加百度定位SDK的Maven仓库和依赖
 * 2. 百度定位SDK的Key通常只能在 AndroidManifest.xml 里用 meta-data 静态声明,
 *    不像高德那样能在代码里随时 setApiKey() 动态切换,这是百度SDK自身的限制
 *
 * 如果想真正启用百度,需要:
 * 1. 去 https://lbsyun.baidu.com 申请Key
 * 2. 加百度SDK依赖
 * 3. 在Manifest里加 <meta-data android:name="com.baidu.lbsapi.API_KEY" android:value="你的Key"/>
 * 4. 把下面的实现换成真实的 LocationClient 调用逻辑
 *
 * 目前选中"百度"这个选项,会直接返回一个提示错误,不会让程序崩溃。
 */
class BaiduLocationProviderAdapter : LocationProviderAdapter {
    override fun start(
        context: Context,
        apiKey: String,
        onLocation: (Double, Double, Float, String?) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        onError(-999, "百度定位SDK尚未接入,请先选择高德,或参考代码注释自行完成接入")
    }

    override fun stop() {}
}