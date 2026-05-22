package com.idocar.launcher.pip

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.R
import com.idocar.launcher.navigation.NavigationActivity

/**
 * 画中画导航服务
 * 在后台保持导航状态，支持画中画显示
 */
class PipNavigationService : Service() {

    private val binder = LocalBinder()
    private var isRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): PipNavigationService = this@PipNavigationService
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, NavigationActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PipNavigationService::class.java).apply {
                action = ACTION_STOP_NAVIGATION
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CarLauncherApp.CHANNEL_ID_NAVIGATION)
            .setContentTitle(getString(R.string.navigating))
            .setContentText(getString(R.string.navigation_in_progress))
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.stop), stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        // 停止导航
        (application as CarLauncherApp).navigationManager.stopNavigation()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP_NAVIGATION = "stop_navigation"
        
        var isServiceRunning = false
            private set
    }
}
