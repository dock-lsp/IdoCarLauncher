package com.idocar.launcher.ui.viewmodel

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idocar.launcher.data.ThemeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主题设置 ViewModel
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val _themes = MutableStateFlow<List<ThemeItem>>(emptyList())
    val themes: StateFlow<List<ThemeItem>> = _themes.asStateFlow()

    private val _selectedTheme = MutableStateFlow<ThemeItem?>(null)
    val selectedTheme: StateFlow<ThemeItem?> = _selectedTheme.asStateFlow()

    init {
        loadThemes()
    }

    private fun loadThemes() {
        viewModelScope.launch {
            // 加载预设主题
            val presetThemes = listOf(
                ThemeItem(
                    id = "default",
                    name = "默认主题",
                    previewUrl = "",
                    wallpaperUrl = "",
                    primaryColor = Color.parseColor("#1976D2"),
                    accentColor = Color.parseColor("#FF4081"),
                    isDarkTheme = false,
                    isDownloaded = true
                ),
                ThemeItem(
                    id = "dark",
                    name = "深色主题",
                    previewUrl = "",
                    wallpaperUrl = "",
                    primaryColor = Color.parseColor("#212121"),
                    accentColor = Color.parseColor("#64FFDA"),
                    isDarkTheme = true,
                    isDownloaded = true
                ),
                ThemeItem(
                    id = "red",
                    name = "热情红",
                    previewUrl = "",
                    wallpaperUrl = "",
                    primaryColor = Color.parseColor("#D32F2F"),
                    accentColor = Color.parseColor("#FFCDD2"),
                    isDarkTheme = false,
                    isDownloaded = true
                ),
                ThemeItem(
                    id = "green",
                    name = "自然绿",
                    previewUrl = "",
                    wallpaperUrl = "",
                    primaryColor = Color.parseColor("#388E3C"),
                    accentColor = Color.parseColor("#C8E6C9"),
                    isDarkTheme = false,
                    isDownloaded = true
                ),
                ThemeItem(
                    id = "purple",
                    name = "优雅紫",
                    previewUrl = "",
                    wallpaperUrl = "",
                    primaryColor = Color.parseColor("#7B1FA2"),
                    accentColor = Color.parseColor("#E1BEE7"),
                    isDarkTheme = false,
                    isDownloaded = true
                )
            )
            _themes.value = presetThemes

            // 加载当前选中的主题
            val prefs = getApplication<Application>().getSharedPreferences("theme_prefs", 0)
            val currentThemeId = prefs.getString("current_theme_id", "default")
            _selectedTheme.value = presetThemes.find { it.id == currentThemeId }
        }
    }

    fun selectTheme(theme: ThemeItem) {
        _selectedTheme.value = theme
    }

    fun downloadTheme(theme: ThemeItem) {
        viewModelScope.launch {
            // 模拟下载
            kotlinx.coroutines.delay(1000)
            val updatedTheme = theme.copy(isDownloaded = true)
            _themes.value = _themes.value.map {
                if (it.id == theme.id) updatedTheme else it
            }
        }
    }
}
