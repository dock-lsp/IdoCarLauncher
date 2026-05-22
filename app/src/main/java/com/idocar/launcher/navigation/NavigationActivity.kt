package com.idocar.launcher.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.R
import com.idocar.launcher.databinding.ActivityNavigationBinding
import kotlinx.coroutines.launch

/**
 * 导航界面 Activity
 */
class NavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationBinding
    private lateinit var navigationManager: NavigationManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            initializeNavigation()
        } else {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navigationManager = (application as CarLauncherApp).navigationManager

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED -> {
                initializeNavigation()
            }
            else -> {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    private fun initializeNavigation() {
        setupUI()
        setupListeners()
        observeNavigationState()
        
        // 检查是否需要启动导航
        val destination = intent.getStringExtra("destination")
        if (!destination.isNullOrEmpty()) {
            startNavigation(destination)
        }
    }

    private fun setupUI() {
        // 设置全屏
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnStartNav.setOnClickListener {
            val destination = binding.etDestination.text.toString()
            if (destination.isNotEmpty()) {
                startNavigation(destination)
            } else {
                Toast.makeText(this, R.string.enter_destination, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopNav.setOnClickListener {
            stopNavigation()
        }

        binding.btnVoiceInput.setOnClickListener {
            // 启动语音输入
        }

        binding.btnPipMode.setOnClickListener {
            enterPictureInPictureMode()
        }
    }

    private fun observeNavigationState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationManager.navigationState.collect { state ->
                    updateNavigationUI(state)
                }
            }
        }
    }

    private fun updateNavigationUI(state: com.idocar.launcher.data.NavigationState) {
        if (state.isNavigating) {
            binding.layoutNavInput.visibility = View.GONE
            binding.layoutNavigating.visibility = View.VISIBLE
            
            binding.tvNavDestination.text = state.destination
            binding.tvDistanceRemaining.text = formatDistance(state.distanceRemaining)
            binding.tvTimeRemaining.text = formatTime(state.timeRemaining)
            binding.tvCurrentRoad.text = state.currentRoad
            binding.tvNextRoad.text = state.nextRoad
            binding.tvNextTurnDistance.text = "${state.nextTurnDistance}m"
            
            // 更新转向图标
            updateTurnIcon(state.nextTurnType)
        } else {
            binding.layoutNavInput.visibility = View.VISIBLE
            binding.layoutNavigating.visibility = View.GONE
        }
    }

    private fun startNavigation(destination: String) {
        navigationManager.simulateNavigation(destination)
        Toast.makeText(this, getString(R.string.navigating_to, destination), Toast.LENGTH_SHORT).show()
    }

    private fun stopNavigation() {
        navigationManager.stopNavigation()
        Toast.makeText(this, R.string.navigation_stopped, Toast.LENGTH_SHORT).show()
    }

    private fun formatDistance(meters: Int): String {
        return if (meters >= 1000) {
            String.format("%.1f km", meters / 1000.0)
        } else {
            "$meters m"
        }
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) {
            "${hours}h ${minutes}min"
        } else {
            "${minutes}min"
        }
    }

    private fun updateTurnIcon(turnType: Int) {
        val iconRes = when (turnType) {
            0 -> R.drawable.ic_turn_straight
            1 -> R.drawable.ic_turn_left
            2 -> R.drawable.ic_turn_right
            3 -> R.drawable.ic_turn_uturn
            else -> R.drawable.ic_turn_straight
        }
        binding.ivTurnIcon.setImageResource(iconRes)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // 用户离开应用时进入画中画模式
        if (navigationManager.navigationState.value.isNavigating) {
            enterPictureInPictureMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        navigationManager.setPictureInPictureMode(isInPictureInPictureMode)
        
        if (isInPictureInPictureMode) {
            // 隐藏非必要控件
            binding.btnBack.visibility = View.GONE
            binding.btnStopNav.visibility = View.GONE
        } else {
            binding.btnBack.visibility = View.VISIBLE
            binding.btnStopNav.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 不在这里停止导航，让用户可以继续导航
    }
}
