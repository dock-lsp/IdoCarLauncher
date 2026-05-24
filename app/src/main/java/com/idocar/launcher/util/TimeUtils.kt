package com.idocar.launcher.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 时间工具类
 */
object TimeUtils {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 获取当前时间 (HH:mm)
     */
    fun getCurrentTime(): String {
        return timeFormat.format(Date())
    }

    /**
     * 获取当前日期 (MM月dd日 星期X)
     */
    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }

    /**
     * 获取完整日期时间
     */
    fun getFullDateTime(): String {
        return fullDateFormat.format(Date())
    }

    /**
     * 格式化时间戳为时间字符串
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    /**
     * 格式化时间戳为日期字符串
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 格式化持续时间 (秒 -> mm:ss 或 HH:mm:ss)
     */
    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    /**
     * 格式化持续时间 (毫秒 -> mm:ss)
     */
    fun formatDurationMs(milliseconds: Long): String {
        val seconds = (milliseconds / 1000).toInt()
        return formatDuration(seconds)
    }

    /**
     * 获取问候语
     */
    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "早上好"
            in 12..13 -> "中午好"
            in 14..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深了"
        }
    }

    /**
     * 判断是否为同一天
     */
    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * 获取相对时间描述
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "刚刚"
            diff < 3600000 -> "${diff / 60000}分钟前"
            diff < 86400000 -> "${diff / 3600000}小时前"
            diff < 604800000 -> "${diff / 86400000}天前"
            else -> formatDate(timestamp)
        }
    }
}
