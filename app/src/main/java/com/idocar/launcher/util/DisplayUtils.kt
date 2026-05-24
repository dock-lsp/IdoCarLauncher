package com.idocar.launcher.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * 显示工具类
 */
object DisplayUtils {

    /**
     * dp 转 px
     */
    fun dpToPx(context: Context, dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    /**
     * px 转 dp
     */
    fun pxToDp(context: Context, px: Int): Float {
        val density = context.resources.displayMetrics.density
        return px / density
    }

    /**
     * sp 转 px
     */
    fun spToPx(context: Context, sp: Float): Int {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return (sp * scaledDensity + 0.5f).toInt()
    }

    /**
     * 获取屏幕宽度
     */
    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metrics)
        return metrics.widthPixels
    }

    /**
     * 获取屏幕高度
     */
    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metrics)
        return metrics.heightPixels
    }

    /**
     * 获取屏幕密度
     */
    fun getScreenDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    /**
     * 获取屏幕 DPI
     */
    fun getScreenDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * 判断是否为平板设备
     */
    fun isTablet(context: Context): Boolean {
        val configuration = context.resources.configuration
        return configuration.smallestScreenWidthDp >= 600
    }

    /**
     * 判断是否为横屏
     */
    fun isLandscape(context: Context): Boolean {
        val configuration = context.resources.configuration
        return configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    }
}
