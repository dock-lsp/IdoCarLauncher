package com.idocar.launcher.data

import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 应用信息数据类
 */
@Parcelize
@Entity(tableName = "apps")
data class AppItem(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val activityName: String,
    val category: AppCategory = AppCategory.OTHER,
    val isSystemApp: Boolean = false,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0,
    val lastUsedTime: Long = 0,
    val iconUri: String? = null
) : Parcelable {
    
    @Transient
    var icon: Drawable? = null
    
    enum class AppCategory {
        NAVIGATION,     // 导航
        MUSIC,          // 音乐
        VIDEO,          // 视频
        COMMUNICATION,  // 通讯
        TOOLS,          // 工具
        SETTINGS,       // 设置
        ENTERTAINMENT,  // 娱乐
        OTHER           // 其他
    }
}

/**
 * 快捷方式数据类
 */
@Parcelize
@Entity(tableName = "shortcuts")
data class ShortcutItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val iconResId: Int,
    val actionType: ActionType,
    val targetPackage: String? = null,
    val targetActivity: String? = null,
    val extraData: String? = null,
    val sortOrder: Int = 0
) : Parcelable {
    
    enum class ActionType {
        APP,            // 启动应用
        FUNCTION,       // 系统功能
        WIDGET,         // 小部件
        CUSTOM          // 自定义操作
    }
}

/**
 * 主题数据类
 */
@Parcelize
@Entity(tableName = "themes")
data class ThemeItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val previewUrl: String,
    val wallpaperUrl: String,
    val primaryColor: Int,
    val accentColor: Int,
    val isDarkTheme: Boolean = false,
    val isDownloaded: Boolean = false
) : Parcelable

/**
 * 车辆信息数据类
 */
data class VehicleInfo(
    val speed: Float = 0f,              // 车速 km/h
    val rpm: Int = 0,                   // 转速
    val fuelLevel: Float = 0f,          // 油量百分比
    val fuelRange: Float = 0f,          // 续航里程 km
    val temperature: Float = 0f,        // 车外温度
    val engineTemp: Float = 0f,         // 发动机温度
    val tirePressures: List<Float> = emptyList(), // 胎压
    val doorStatus: DoorStatus = DoorStatus(),
    val lightStatus: LightStatus = LightStatus(),
    val timestamp: Long = System.currentTimeMillis()
) {
    data class DoorStatus(
        val frontLeft: Boolean = false,
        val frontRight: Boolean = false,
        val rearLeft: Boolean = false,
        val rearRight: Boolean = false,
        val trunk: Boolean = false,
        val hood: Boolean = false
    )
    
    data class LightStatus(
        val headlights: Boolean = false,
        val highBeam: Boolean = false,
        val fogLights: Boolean = false,
        val turnLeft: Boolean = false,
        val turnRight: Boolean = false
    )
}

/**
 * 媒体播放状态
 */
data class MediaPlaybackState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArt: String? = null,
    val duration: Long = 0,
    val position: Long = 0,
    val source: MediaSource = MediaSource.LOCAL,
    val repeatMode: Int = 0,
    val shuffleMode: Int = 0
) {
    enum class MediaSource {
        LOCAL,          // 本地音乐
        BLUETOOTH,      // 蓝牙音乐
        USB,            // USB音乐
        RADIO,          // 收音机
        ONLINE          // 在线音乐
    }
}

/**
 * 导航状态
 */
data class NavigationState(
    val isNavigating: Boolean = false,
    val destination: String = "",
    val distanceRemaining: Int = 0,     // 剩余距离(米)
    val timeRemaining: Int = 0,         // 剩余时间(秒)
    val nextTurnType: Int = 0,          // 下一个转向类型
    val nextTurnDistance: Int = 0,      // 下个转向距离
    val currentRoad: String = "",
    val nextRoad: String = "",
    val isPictureInPicture: Boolean = false
)

/**
 * 语音命令类型
 */
enum class VoiceCommandType {
    OPEN_APP,
    PLAY_MUSIC,
    NAVIGATE,
    CALL,
    SETTING,
    WEATHER,
    VOLUME,
    UNKNOWN
}

/**
 * 插件信息
 */
data class PluginInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val packageName: String,
    val entryClass: String,
    val iconPath: String? = null,
    val isEnabled: Boolean = true,
    val isLoaded: Boolean = false
)
