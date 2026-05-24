package com.idocar.launcher.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.idocar.launcher.data.AppItem

/**
 * 应用工具类
 */
object AppUtils {

    /**
     * 获取已安装的应用列表
     */
    fun getInstalledApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val apps = mutableListOf<AppItem>()

        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            // 过滤掉没有启动界面的应用
            if (pm.getLaunchIntentForPackage(appInfo.packageName) == null) {
                continue
            }

            val appName = pm.getApplicationLabel(appInfo).toString()
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val category = categorizeApp(appInfo.packageName, appName)

            val appItem = AppItem(
                packageName = appInfo.packageName,
                appName = appName,
                activityName = "",
                category = category,
                isSystemApp = isSystemApp
            )
            appItem.icon = appInfo.loadIcon(pm)
            apps.add(appItem)
        }

        return apps.sortedBy { it.appName }
    }

    /**
     * 根据包名和应用名分类应用
     */
    private fun categorizeApp(packageName: String, appName: String): AppItem.AppCategory {
        val pkg = packageName.lowercase()
        val name = appName.lowercase()

        return when {
            // 导航
            pkg.contains("map") || pkg.contains("nav") ||
            name.contains("地图") || name.contains("导航") -> AppItem.AppCategory.NAVIGATION

            // 音乐
            pkg.contains("music") || pkg.contains("audio") || pkg.contains("player") ||
            name.contains("音乐") || name.contains("播放器") -> AppItem.AppCategory.MUSIC

            // 视频
            pkg.contains("video") || pkg.contains("movie") ||
            name.contains("视频") || name.contains("电影") -> AppItem.AppCategory.VIDEO

            // 通讯
            pkg.contains("phone") || pkg.contains("dialer") || pkg.contains("contact") ||
            pkg.contains("message") || pkg.contains("wechat") || pkg.contains("qq") ||
            name.contains("电话") || name.contains("通讯录") || name.contains("信息") ->
                AppItem.AppCategory.COMMUNICATION

            // 设置
            pkg.contains("setting") || name.contains("设置") -> AppItem.AppCategory.SETTINGS

            // 工具
            pkg.contains("calculator") || pkg.contains("calendar") || pkg.contains("clock") ||
            pkg.contains("file") || pkg.contains("browser") ||
            name.contains("计算器") || name.contains("日历") || name.contains("时钟") ||
            name.contains("文件") || name.contains("浏览器") -> AppItem.AppCategory.TOOLS

            else -> AppItem.AppCategory.OTHER
        }
    }

    /**
     * 获取应用的启动 Intent
     */
    fun getLaunchIntent(context: Context, packageName: String): Intent? {
        val pm = context.packageManager
        return pm.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * 检查应用是否已安装
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 获取应用图标
     */
    fun getAppIcon(context: Context, packageName: String): android.graphics.drawable.Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 获取应用名称
     */
    fun getAppName(context: Context, packageName: String): String? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
