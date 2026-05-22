package com.idocar.launcher.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.idocar.launcher.data.VoiceCommandType
import com.idocar.launcher.util.AppUtils

/**
 * 语音命令处理器
 * 实现具体的命令处理逻辑
 */
class VoiceCommandProcessor(private val context: Context) : VoiceAssistantManager.VoiceCommandHandler {

    override fun handleCommand(command: VoiceCommandType, rawText: String): String {
        return when (command) {
            VoiceCommandType.OPEN_APP -> handleOpenApp(rawText)
            VoiceCommandType.PLAY_MUSIC -> handlePlayMusic(rawText)
            VoiceCommandType.NAVIGATE -> handleNavigate(rawText)
            VoiceCommandType.CALL -> handleCall(rawText)
            VoiceCommandType.SETTING -> handleSetting(rawText)
            VoiceCommandType.WEATHER -> handleWeather(rawText)
            VoiceCommandType.VOLUME -> handleVolume(rawText)
            VoiceCommandType.UNKNOWN -> "抱歉，我没有理解您的指令，请再说一遍"
        }
    }

    private fun handleOpenApp(text: String): String {
        val appName = extractAppName(text)
        val packageName = findAppByName(appName)
        
        return if (packageName != null) {
            val intent = AppUtils.getLaunchIntent(context, packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "正在打开$appName"
            } else {
                "无法打开$appName"
            }
        } else {
            "没有找到应用$appName"
        }
    }

    private fun handlePlayMusic(text: String): String {
        // 提取歌曲名或歌手名
        val songName = extractSongName(text)
        
        // 启动音乐应用
        val musicApps = listOf(
            "com.netease.cloudmusic",
            "com.tencent.qqmusic",
            "com.kugou.android",
            "com.spotify.music"
        )
        
        for (pkg in musicApps) {
            val intent = AppUtils.getLaunchIntent(context, pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return if (songName.isNotEmpty()) "正在为您播放$songName" else "正在打开音乐"
            }
        }
        
        return "没有找到音乐应用"
    }

    private fun handleNavigate(text: String): String {
        val destination = extractDestination(text)
        
        return if (destination.isNotEmpty()) {
            // 尝试启动导航应用
            val navApps = listOf(
                "com.autonavi.minimap",
                "com.baidu.BaiduMap",
                "com.google.android.apps.maps"
            )
            
            for (pkg in navApps) {
                val intent = AppUtils.getLaunchIntent(context, pkg)
                if (intent != null) {
                    // 构建导航意图
                    val navIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("geo:0,0?q=$destination")
                        `package` = pkg
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(navIntent)
                    return "正在导航到$destination"
                }
            }
            
            "没有找到导航应用"
        } else {
            "请告诉我您要去哪里"
        }
    }

    private fun handleCall(text: String): String {
        val contactName = extractContactName(text)
        
        return if (contactName.isNotEmpty()) {
            // 这里应该查询联系人数据库
            "正在呼叫$contactName"
        } else {
            "请告诉我要呼叫谁"
        }
    }

    private fun handleSetting(text: String): String {
        return when {
            text.contains("蓝牙") -> {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "正在打开蓝牙设置"
            }
            text.contains("wifi") || text.contains("无线") -> {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "正在打开WiFi设置"
            }
            text.contains("音量") -> {
                "请使用音量键调节"
            }
            text.contains("显示") || text.contains("屏幕") -> {
                context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "正在打开显示设置"
            }
            else -> "请告诉我具体要设置什么"
        }
    }

    private fun handleWeather(text: String): String {
        // 这里应该调用天气API
        // 模拟返回
        return "今天天气晴朗，温度24度，适合出行"
    }

    private fun handleVolume(text: String): String {
        return when {
            text.contains("大") || text.contains("高") -> {
                // 增加音量
                adjustVolume(true)
                "已调高音量"
            }
            text.contains("小") || text.contains("低") -> {
                // 降低音量
                adjustVolume(false)
                "已调低音量"
            }
            text.contains("静音") -> {
                "已静音"
            }
            else -> "请告诉我要怎么调节音量"
        }
    }

    private fun adjustVolume(increase: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val direction = if (increase) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, direction, 0)
    }

    // 辅助方法：提取应用名
    private fun extractAppName(text: String): String {
        val patterns = listOf("打开", "启动", "运行")
        var result = text
        for (pattern in patterns) {
            result = result.replace(pattern, "")
        }
        return result.trim()
    }

    // 辅助方法：提取歌曲名
    private fun extractSongName(text: String): String {
        val patterns = listOf("播放", "听", "来一首", "放")
        var result = text
        for (pattern in patterns) {
            result = result.replace(pattern, "")
        }
        return result.trim()
    }

    // 辅助方法：提取目的地
    private fun extractDestination(text: String): String {
        val patterns = listOf("导航到", "去", "到", "怎么走")
        var result = text
        for (pattern in patterns) {
            result = result.replace(pattern, "")
        }
        return result.trim()
    }

    // 辅助方法：提取联系人名
    private fun extractContactName(text: String): String {
        val patterns = listOf("打电话给", "呼叫", "拨打")
        var result = text
        for (pattern in patterns) {
            result = result.replace(pattern, "")
        }
        return result.trim()
    }

    // 辅助方法：根据名称查找应用
    private fun findAppByName(appName: String): String? {
        val appMap = mapOf(
            "导航" to listOf("com.autonavi.minimap", "com.baidu.BaiduMap"),
            "地图" to listOf("com.autonavi.minimap", "com.baidu.BaiduMap"),
            "音乐" to listOf("com.netease.cloudmusic", "com.tencent.qqmusic"),
            "电话" to listOf("com.android.dialer"),
            "设置" to listOf("com.android.settings"),
            "浏览器" to listOf("com.android.chrome"),
            "相机" to listOf("com.android.camera")
        )
        
        val candidates = appMap[appName] ?: return null
        
        for (pkg in candidates) {
            if (AppUtils.isAppInstalled(context, pkg)) {
                return pkg
            }
        }
        
        return null
    }
}
