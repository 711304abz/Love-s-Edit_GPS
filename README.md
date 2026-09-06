# LovesEDITGPS —— 国内定位桥接工具

在中国大陆,Google 地图、Google 天气、Find Hub(查找我的设备)等依赖 Google 定位服务的应用经常出现定位不准甚至完全无法定位的问题。本项目通过接入国产地图定位 SDK(目前支持高德地图),将其定位结果转换坐标系后注入 Android 系统的模拟定位接口,使所有依赖系统定位的应用都能获得可用的位置数据。

## 效果对比

在实际测试中(以 Google Find Hub 为例):

| 版本 | 定位偏差 |
|---|---|
| 未接管 Network Provider | 约 398 米 |
| 同时接管 GPS + Network Provider | 约 52 米 |

## 工作原理

```
高德定位SDK → 获取GCJ-02坐标(国内网络环境下精度更可靠)
     ↓
坐标系转换 → GCJ-02 转 WGS-84(消除国内地图强制加密偏移)
     ↓
写入系统模拟定位 → 同时接管 GPS_PROVIDER 与 NETWORK_PROVIDER
     ↓
系统内所有定位调用方 → Google地图 / 天气 / Find Hub 等
```

详细的技术背景和实现细节,见仓库内的技术文章。

## 功能特性

- 前台服务常驻运行,避免被系统后台回收
- 开机自动启动
- 支持申请电池优化白名单,提升存活率
- 定位数据源与 API Key 可在 App 内直接配置,无需修改代码或重新编译
- 同时接管 GPS 与 Network 两个 Provider,大幅降低融合定位场景下的偏移

## 使用前提

- 一台已开启开发者选项、USB调试的 Android 设备
- 一个高德开放平台的开发者账号(免费),用于申请定位服务 API Key

## 安装与使用

### 1. 申请高德定位 API Key

1. 前往 [高德开放平台](https://lbs.amap.com) 注册开发者账号
2. 控制台 → 应用管理 → 创建新应用 → 添加Key(服务平台选择 Android)
3. 需要填写应用包名(`com.example.lovesedit_gps`,如果你自行修改了包名,以实际为准)和调试签名 SHA1

### 2. 编译与安装

用 Android Studio 打开本项目,连接手机后直接运行,或者执行:

```bash
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`,可直接安装。

### 3. 配置 Key 并启动

打开 App,在"定位数据源设置"卡片中:

1. 选择定位 SDK(目前仅高德已完整接入)
2. 填入申请到的 API Key
3. 点击"保存并重启服务"

### 4. 手动授权模拟定位(重要)

部分机型的"开发者选项 → 选择模拟位置信息应用"列表可能无法正常显示本应用。如遇到这种情况,可以用 ADB 直接授权:

```bash
adb shell appops set com.example.lovesedit_gps android:mock_location allow
```

该授权绑定在应用安装实例上,只要不卸载重装,重启设备不会失效。

## 已知限制

- **并非所有应用都会采信模拟定位数据。** 部分安全敏感应用(如金融类App)会主动检测 `Location.isFromMockProvider()` 并拒绝使用,这是系统预留的合理防护机制,本项目对此不作规避,也不建议规避。
- **坐标转换存在正常误差。** GCJ-02 到 WGS-84 的转换基于公开的近似算法,通常有几米到几十米级别的残差,属正常现象。
- **百度地图 SDK 尚未真正接入**,目前选中该选项会返回明确的错误提示,不会导致程序崩溃。详见 `BaiduLocationProviderAdapter.kt` 内的注释说明。
- **定位数据源的免费额度存在上限**,长期高频使用建议关注对应平台的额度政策。

## 项目结构

```
├── MainActivity.kt                    # 主界面,负责权限申请与服务控制
├── LocationService.kt                 # 前台服务,核心定位与坐标注入逻辑
├── BootReceiver.kt                    # 开机自启动广播接收器
├── PrefsManager.kt                    # 本地配置存取(定位源类型、API Key)
├── LocationProviderAdapter.kt         # 定位数据源适配器接口
├── AMapLocationProviderAdapter.kt     # 高德定位SDK适配器实现
└── BaiduLocationProviderAdapter.kt    # 百度定位SDK适配器(预留,未接入)
```

## 扩展新的定位数据源

实现 `LocationProviderAdapter` 接口即可接入新的定位SDK,在 `LocationService.kt` 的 `startLocationUpdates()` 方法中注册对应的适配器分支。

## 免责声明

本项目仅用于个人设备的自用场景,通过 Android 系统提供的标准开发者接口(模拟定位)提供一份替代性的位置数据,不涉及对任何第三方应用的逆向工程、数据窃取或安全机制绕过。使用者需自行承担因使用本项目产生的一切后果,并遵守当地法律法规及相关服务条款。

## License

MIT
