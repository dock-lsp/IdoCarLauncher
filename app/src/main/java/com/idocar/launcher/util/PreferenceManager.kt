package com.idocar.launcher.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 偏好设置管理器
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "car_launcher_prefs"

        // 设置键
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_FLOATING_BALL_ENABLED = "floating_ball_enabled"
        private const val KEY_VOICE_WAKEUP_ENABLED = "voice_wakeup_enabled"
        private const val KEY_AUTO_NAVIGATION = "auto_navigation"
        private const val KEY_SHOW_SPEED = "show_speed"
        private const val KEY_DEFAULT_NAV_APP = "default_nav_app"
        private const val KEY_DEFAULT_MUSIC_APP = "default_music_app"
        private const val KEY_VOLUME_LEVEL = "volume_level"
        private const val KEY_BRIGHTNESS_LEVEL = "brightness_level"
    }

    /**
     * 是否首次启动
     */
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH, value) }

    /**
     * 悬浮球是否启用
     */
    var isFloatingBallEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BALL_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_FLOATING_BALL_ENABLED, value) }

    /**
     * 语音唤醒是否启用
     */
    var isVoiceWakeupEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_WAKEUP_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_VOICE_WAKEUP_ENABLED, value) }

    /**
     * 是否自动启动导航
     */
    var isAutoNavigation: Boolean
        get() = prefs.getBoolean(KEY_AUTO_NAVIGATION, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_NAVIGATION, value) }

    /**
     * 是否显示车速
     */
    var isShowSpeed: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SPEED, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_SPEED, value) }

    /**
     * 默认导航应用
     */
    var defaultNavApp: String?
        get() = prefs.getString(KEY_DEFAULT_NAV_APP, null)
        set(value) = prefs.edit { putString(KEY_DEFAULT_NAV_APP, value) }

    /**
     * 默认音乐应用
     */
    var defaultMusicApp: String?
        get() = prefs.getString(KEY_DEFAULT_MUSIC_APP, null)
        set(value) = prefs.edit { putString(KEY_DEFAULT_MUSIC_APP, value) }

    /**
     * 音量级别
     */
    var volumeLevel: Int
        get() = prefs.getInt(KEY_VOLUME_LEVEL, 50)
        set(value) = prefs.edit { putInt(KEY_VOLUME_LEVEL, value.coerceIn(0, 100)) }

    /**
     * 亮度级别
     */
    var brightnessLevel: Int
        get() = prefs.getInt(KEY_BRIGHTNESS_LEVEL, 80)
        set(value) = prefs.edit { putInt(KEY_BRIGHTNESS_LEVEL, value.coerceIn(0, 100)) }

    /**
     * 清除所有设置
     */
    fun clearAll() {
        prefs.edit { clear() }
    }

    /**
     * 获取字符串设置
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }

    /**
     * 设置字符串
     */
    fun setString(key: String, value: String?) {
        prefs.edit { putString(key, value) }
    }

    /**
     * 获取布尔设置
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * 设置布尔值
     */
    fun setBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    /**
     * 获取整数设置
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    /**
     * 设置整数值
     */
    fun setInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }
}
