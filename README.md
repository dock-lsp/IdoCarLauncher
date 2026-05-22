# IdoCarLauncher - 智能车载桌面启动器

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="IdoCarLauncher Logo" width="120">
</p>

<p align="center">
  <a href="https://github.com/yourusername/IdoCarLauncher/releases">
    <img src="https://img.shields.io/github/v/release/yourusername/IdoCarLauncher?include_prereleases&style=flat-square" alt="Release">
  </a>
  <a href="https://github.com/yourusername/IdoCarLauncher/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/yourusername/IdoCarLauncher?style=flat-square" alt="License">
  </a>
  <a href="https://developer.android.com/studio/releases/platforms#8.0">
    <img src="https://img.shields.io/badge/Android-8.0+-brightgreen?style=flat-square&logo=android" alt="Android Version">
  </a>
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/Kotlin-1.9.20-blue?style=flat-square&logo=kotlin" alt="Kotlin">
  </a>
</p>

## 项目简介

IdoCarLauncher 是一款专为车载环境设计的 Android 桌面启动器应用，提供安全、便捷、智能的车载交互体验。该应用采用现代化的 Material Design 3 设计语言，针对横屏车载场景进行了深度优化，集成了导航、媒体控制、语音助手等核心功能。

## 核心特性

### 🚗 车载优化设计
- **横屏适配**：专为车载横屏显示优化，界面元素适合驾驶场景操作
- **全屏沉浸**：隐藏系统状态栏和导航栏，提供沉浸式体验
- **大按钮设计**：触控区域优化，适合驾驶中快速操作

### 🧭 智能导航系统
- **画中画导航**：支持 PiP 模式，导航信息悬浮显示
- **多导航应用支持**：兼容高德地图、百度地图、Google Maps
- **实时位置追踪**：GPS + 网络定位双重保障

### 🎵 媒体控制中心
- **统一媒体控制**：集成 Android MediaSession API，支持所有音乐应用
- **蓝牙音乐支持**：自动识别蓝牙音频源
- **播放状态同步**：实时显示当前播放信息

### 🎙️ 智能语音助手
- **语音唤醒**：支持语音命令唤醒
- **多命令识别**：支持导航、音乐、电话、设置等多种命令类型
- **语音合成**：TTS 语音播报反馈

### 🔌 插件扩展系统
- **动态插件加载**：支持 APK 插件热插拔
- **内置插件管理**：支持内置插件和外部插件
- **插件生命周期管理**：完整的插件加载、启用、禁用、卸载流程

### 🎨 主题定制
- **多主题支持**：支持默认主题、深色主题等
- **动态主题切换**：实时切换主题无需重启
- **自定义主题下载**：支持在线主题下载

### ⚡ 快捷操作
- **悬浮球功能**：可拖拽的悬浮快捷按钮
- **快捷方式管理**：支持应用快捷方式和功能快捷方式
- **智能推荐**：根据使用习惯推荐常用应用

## 技术架构

### 技术栈
- **开发语言**：Kotlin 1.9.20
- **最低支持**：Android 8.0 (API 26)
- **目标版本**：Android 14 (API 34)
- **构建工具**：Gradle 8.2.0
- **架构模式**：MVVM + Repository

### 核心依赖
```kotlin
// UI
AndroidX Core / AppCompat / Material Design 3
Jetpack Compose 1.6.1
ConstraintLayout / RecyclerView / ViewPager2

// Architecture
Lifecycle / ViewModel / LiveData
Room Database 2.6.1
Navigation Component

// Media & Location
Media3 ExoPlayer 1.2.1
Google Play Services Location & Maps

// Network & Data
Retrofit 2.9.0 / OkHttp 4.12.0
Gson / Kotlin Serialization

// Image Loading
Glide 4.16.0

// Dependency Injection
Hilt 2.50

// Speech Recognition
Google ML Kit Speech 1.0.0
```

### 项目结构
```
app/src/main/java/com/idocar/launcher/
├── adapter/          # RecyclerView 适配器
├── data/             # 数据模型和数据库
│   └── database/     # Room 数据库 DAO 和 Entity
├── media/            # 媒体播放相关
├── navigation/       # 导航功能模块
├── pip/              # 画中画服务
├── plugin/           # 插件系统
├── service/          # 后台服务
├── ui/               # UI 界面和 ViewModel
│   └── viewmodel/    # MVVM ViewModel
├── util/             # 工具类
├── voice/            # 语音助手
└── CarLauncherApp.kt # 应用入口
```

## 功能模块

| 模块 | 功能描述 | 状态 |
|------|----------|------|
| 主界面 | 应用网格、快捷方式、车辆信息 | ✅ 已完成 |
| 导航 | GPS定位、画中画导航、多地图支持 | ✅ 已完成 |
| 媒体控制 | 音乐播放控制、蓝牙音频、视频播放 | ✅ 已完成 |
| 语音助手 | 语音识别、命令处理、TTS播报 | ✅ 已完成 |
| 插件系统 | 动态加载、插件管理、生命周期 | ✅ 已完成 |
| 主题系统 | 多主题支持、动态切换、在线下载 | ✅ 已完成 |
| 悬浮球 | 快捷操作、拖拽定位、菜单展开 | ✅ 已完成 |
| 设置 | 通用设置、显示设置、声音设置 | ✅ 已完成 |

## 安装与使用

### 系统要求
- Android 8.0 (API 26) 或更高版本
- 支持横屏显示的车载设备
- 建议内存：2GB+

### 安装步骤

1. **下载 APK**
   ```bash
   # 从 Releases 页面下载最新版本
   wget https://github.com/yourusername/IdoCarLauncher/releases/download/v1.0.0/IdoCarLauncher-v1.0.0.apk
   ```

2. **安装应用**
   ```bash
   adb install IdoCarLauncher-v1.0.0.apk
   ```

3. **设置为默认桌面**
   - 安装完成后，按 Home 键
   - 选择 "IdoCarLauncher" 作为默认桌面

### 权限说明

应用需要以下权限才能正常工作：

| 权限 | 用途 | 必需 |
|------|------|------|
| `INTERNET` | 网络访问 | 是 |
| `ACCESS_FINE_LOCATION` | GPS定位 | 是 |
| `RECORD_AUDIO` | 语音识别 | 是 |
| `SYSTEM_ALERT_WINDOW` | 悬浮球显示 | 否 |
| `FOREGROUND_SERVICE` | 后台服务 | 是 |
| `QUERY_ALL_PACKAGES` | 查询已安装应用 | 是 |

## 开发指南

### 环境搭建

1. **克隆仓库**
   ```bash
   git clone https://github.com/yourusername/IdoCarLauncher.git
   cd IdoCarLauncher
   ```

2. **使用 Android Studio 打开**
   - 安装 Android Studio Hedgehog (2023.1.1) 或更高版本
   - 打开项目根目录
   - 等待 Gradle 同步完成

3. **构建项目**
   ```bash
   ./gradlew assembleDebug
   ```

### 插件开发

IdoCarLauncher 支持插件扩展，开发者可以创建自定义插件：

1. **创建插件项目**
   ```kotlin
   class MyPlugin : CarLauncherPlugin {
       override fun onCreate(context: Context) {
           // 插件初始化
       }
       
       override fun onDestroy() {
           // 插件销毁
       }
   }
   ```

2. **配置插件信息**
   ```xml
   <meta-data
       android:name="plugin_id"
       android:value="com.example.myplugin" />
   <meta-data
       android:name="plugin_entry"
       android:value="com.example.myplugin.MyPlugin" />
   ```

## 版本历史

### v1.0.0 (2024-XX-XX)
- 🎉 初始版本发布
- ✅ 完整的桌面启动器功能
- ✅ 导航、媒体、语音核心功能
- ✅ 插件系统支持
- ✅ 主题定制功能

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 开源协议

本项目采用 [MIT 许可证](LICENSE) 开源。

## 致谢

感谢以下开源项目的支持：
- [Android Open Source Project](https://source.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)

---

<p align="center">
  Made with ❤️ for Car Enthusiasts
</p>
