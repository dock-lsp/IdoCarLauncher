package com.idocar.launcher.navigation

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.idocar.launcher.data.NavigationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 导航管理器
 * 管理导航状态和位置信息
 */
class NavigationManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private var locationListener: LocationListener? = null
    private var isNavigating: Boolean = false

    private val navLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _currentLocation.value = location
            updateNavigationProgress(location)
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    companion object {
        private const val TAG = "NavigationManager"
    }

    /**
     * 开始导航
     */
    fun startNavigation(destination: String, route: NavigationRoute) {
        isNavigating = true
        _navigationState.value = NavigationState(
            isNavigating = true,
            destination = destination,
            distanceRemaining = route.totalDistance,
            timeRemaining = route.estimatedTime,
            currentRoad = route.currentRoad,
            nextRoad = route.nextRoad
        )
        
        // 开始位置监听
        startLocationUpdates()
    }

    /**
     * 停止导航
     */
    fun stopNavigation() {
        isNavigating = false
        _navigationState.value = NavigationState()
        stopLocationUpdates()
    }

    /**
     * 更新导航状态（画中画模式）
     */
    fun setPictureInPictureMode(enabled: Boolean) {
        _navigationState.value = _navigationState.value.copy(
            isPictureInPicture = enabled
        )
    }

    /**
     * 更新导航进度
     */
    private fun updateNavigationProgress(location: Location) {
        if (!isNavigating) return
        
        val currentState = _navigationState.value
        
        // 模拟导航进度更新
        // 实际应用中应该根据真实路线计算
        val remainingDistance = (currentState.distanceRemaining - 10).coerceAtLeast(0)
        val remainingTime = (currentState.timeRemaining - 1).coerceAtLeast(0)
        
        _navigationState.value = currentState.copy(
            distanceRemaining = remainingDistance,
            timeRemaining = remainingTime
        )
        
        // 检查是否到达
        if (remainingDistance <= 0) {
            stopNavigation()
        }
    }

    /**
     * 开始位置更新
     */
    private fun startLocationUpdates() {
        try {
            locationListener?.let { locationManager.removeUpdates(it) }
            
            locationListener = navLocationListener
            
            // 使用 GPS 和网络定位
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1秒更新一次
                5f,    // 5米移动更新
                navLocationListener,
                Looper.getMainLooper()
            )
            
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                5f,
                navLocationListener,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * 停止位置更新
     */
    private fun stopLocationUpdates() {
        locationListener?.let {
            locationManager.removeUpdates(it)
        }
        locationListener = null
    }

    /**
     * 获取最后已知位置
     */
    fun getLastKnownLocation(): Location? {
        return try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * 模拟导航数据（用于测试）
     */
    fun simulateNavigation(destination: String) {
        startNavigation(
            destination = destination,
            route = NavigationRoute(
                totalDistance = 15000,  // 15km
                estimatedTime = 1200,   // 20分钟
                currentRoad = "当前道路",
                nextRoad = "下一个道路"
            )
        )
    }

    data class NavigationRoute(
        val totalDistance: Int,
        val estimatedTime: Int,
        val currentRoad: String,
        val nextRoad: String
    )

    fun cleanup() {
        stopLocationUpdates()
    }
}
