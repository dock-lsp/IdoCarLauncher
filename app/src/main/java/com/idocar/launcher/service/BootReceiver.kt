package com.idocar.launcher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.idocar.launcher.util.PreferenceManager

/**
 * 开机启动接收器
 * 设备启动时自动启动车机桌面
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed received")
            
            val prefs = PreferenceManager(context)
            
            // 启动悬浮球服务
            if (prefs.isFloatingBallEnabled) {
                context.startService(Intent(context, FloatingBallService::class.java))
            }
            
            // 启动主界面
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
    }
}
