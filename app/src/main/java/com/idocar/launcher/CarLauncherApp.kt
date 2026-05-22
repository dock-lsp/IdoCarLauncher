package com.idocar.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.idocar.launcher.data.database.AppDatabase
import com.idocar.launcher.media.MediaControllerManager
import com.idocar.launcher.navigation.NavigationManager
import com.idocar.launcher.plugin.PluginManager
import com.idocar.launcher.service.FloatingBallService
import com.idocar.launcher.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CarLauncherApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    lateinit var database: AppDatabase
        private set
    
    lateinit var preferenceManager: PreferenceManager
        private set
    
    lateinit var mediaControllerManager: MediaControllerManager
        private set
    
    lateinit var navigationManager: NavigationManager
        private set
    
    lateinit var pluginManager: PluginManager
        private set

    companion object {
        lateinit var instance: CarLauncherApp
            private set
        
        const val CHANNEL_ID_MEDIA = "media_channel"
        const val CHANNEL_ID_NAVIGATION = "navigation_channel"
        const val CHANNEL_ID_SYSTEM = "system_channel"
        
        private const val TAG = "CarLauncherApp"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化数据库
        database = AppDatabase.getDatabase(this)
        
        // 初始化偏好设置
        preferenceManager = PreferenceManager(this)
        
        // 初始化媒体控制器
        mediaControllerManager = MediaControllerManager(this)
        
        // 初始化导航管理器
        navigationManager = NavigationManager(this)
        
        // 初始化插件管理器
        pluginManager = PluginManager(this)
        
        // 创建通知渠道
        createNotificationChannels()
        
        // 初始化应用数据
        initializeAppData()
        
        // 监听应用生命周期
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_MEDIA,
                    getString(R.string.channel_media),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.channel_media_desc)
                    setSound(null, null)
                },
                NotificationChannel(
                    CHANNEL_ID_NAVIGATION,
                    getString(R.string.channel_navigation),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.channel_navigation_desc)
                },
                NotificationChannel(
                    CHANNEL_ID_SYSTEM,
                    getString(R.string.channel_system),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = getString(R.string.channel_system_desc)
                }
            )
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(channels)
        }
    }

    private fun initializeAppData() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                // 加载已安装应用
                val installedApps = com.idocar.launcher.util.AppUtils.getInstalledApps(this@CarLauncherApp)
                
                // 保存到数据库
                database.appDao().insertApps(installedApps)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize app data", e)
            }
            
            // 加载插件
            try {
                pluginManager.loadPlugins()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load plugins", e)
            }
        }
    }
}
