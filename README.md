#LovesEDITGPS —— 国内定位桥接工具
因为一些原因，pixel手机在大陆内依赖Google服务的定位非常不稳定，有时候也连不上
主要是因为Network Location Provider依赖的基站/WiFi数据库通常来自Google自家的地理位置服务，这个数据库在内地访问性存在一些限制，导致定位不稳和无法定位
因此这个解决方案是利用了Android系统开发者模式里面的模拟位置.只需要将这个应用在开发者模式-选择模拟位置的应用里面选择为模拟位置的应用,
Android会向LocationManager写入自定义坐标，系统会将其视为一个正常的定位Provider输出，分发给所有请求定位的应用,因此实现定位

#工作原理

1-高德定位SDK → 获取GCJ-02坐标

2-坐标系转换 → GCJ-02 转 WGS-84

3-写入系统模拟定位 → 同时接管 GPS_PROVIDER 与 NETWORK_PROVIDER

##使用前提
一台已开启开发者选项、USB调试的 Android 设备
一个高德开放平台的开发者账号,用于申请定位服务 API Key

#构建
申请高德定位 API Key
1. 前往 [高德开放平台](https://lbs.amap.com) 注册开发者账号
2. 控制台 → 应用管理 → 创建新应用 → 添加Key(服务平台选择 Android)
3. 需要填写应用包名(`com.example.lovesedit_gps`,如果你自行修改了包名,以实际为准)和调试签名 SHA1
用 Android Studio 打开本项目,连接手机后直接运行,或者执行:

```bash
./gradlew assembleDebug
```
%提示%:
部分机型的"开发者选项 → 选择模拟位置信息应用"列表可能无法正常显示本应用。如遇到这种情况,可以用 ADB 直接授权:

```bash
adb shell appops set com.example.lovesedit_gps android:mock_location allow
```

已知限制

并非所有应用都会采信模拟定位数据
*部分安全敏感应用(如金融类App)会主动检测 `Location.isFromMockProvider()` 并拒绝使用
*坐标转换存在正常误差,GCJ-02 到 WGS-84 的转换基于公开的近似算法,通常有几米到几十米级别的残差
*定位数据源的免费额度存在上限,长期高频使用建议关注对应平台的额度政策
