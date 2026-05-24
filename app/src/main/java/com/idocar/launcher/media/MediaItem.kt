package com.idocar.launcher.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 媒体文件数据类
 * 用于统一表示音频和视频媒体项
 */
@Parcelize
data class MediaItem(
    val id: Long = 0,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0,
    val path: String,
    val albumArt: String? = null,
    val type: MediaType = MediaType.AUDIO,
    val size: Long = 0,
    val dateAdded: Long = 0
) : Parcelable {

    /**
     * 媒体类型枚举
     */
    enum class MediaType {
        AUDIO,
        VIDEO
    }

    /**
     * 获取格式化的时长字符串
     * @return 格式化后的时长，如 "03:45" 或 "1:02:30"
     */
    fun getFormattedDuration(): String {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 获取格式化的文件大小字符串
     * @return 格式化后的文件大小，如 "3.5 MB"
     */
    fun getFormattedSize(): String {
        if (size <= 0) return ""
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            else -> String.format("%.1f KB", kb)
        }
    }

    /**
     * 获取文件扩展名
     */
    fun getFileExtension(): String {
        val lastDot = path.lastIndexOf('.')
        return if (lastDot >= 0) path.substring(lastDot + 1).lowercase() else ""
    }

    /**
     * 获取文件名（不含扩展名）
     */
    fun getFileNameWithoutExtension(): String {
        val lastSlash = path.lastIndexOf('/')
        val fileName = if (lastSlash >= 0) path.substring(lastSlash + 1) else path
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0) fileName.substring(0, lastDot) else fileName
    }
}
