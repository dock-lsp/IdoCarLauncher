package com.idocar.launcher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.R
import com.idocar.launcher.adapter.AppGridAdapter
import com.idocar.launcher.adapter.ShortcutAdapter
import com.idocar.launcher.data.AppItem
import com.idocar.launcher.data.ShortcutItem
import com.idocar.launcher.databinding.ActivityHomeBinding
import com.idocar.launcher.service.FloatingBallService
import com.idocar.launcher.navigation.NavigationActivity
import com.idocar.launcher.ui.viewmodel.HomeViewModel
import com.idocar.launcher.util.AppUtils
import com.idocar.launcher.util.TimeUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory((application as CarLauncherApp).database)
    }
    
    private lateinit var appAdapter: AppGridAdapter
    private lateinit var shortcutAdapter: ShortcutAdapter
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeComponents()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置全屏
        setupFullscreen()
        
        // 检查权限
        checkPermissions()
    }

    private fun setupFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.QUERY_ALL_PACKAGES)
                != PackageManager.PERMISSION_GRANTED) {
                // QUERY_ALL_PACKAGES 需要特殊声明
            }
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            initializeComponents()
        }
    }

    private fun initializeComponents() {
        setupUI()
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
        startServices()
    }

    private fun setupUI() {
        // 更新时间显示
        updateTime()
        
        // 车辆信息（模拟）
        updateVehicleInfo()
    }

    private fun setupRecyclerViews() {
        // 应用网格
        appAdapter = AppGridAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptions(app) }
        )
        
        binding.recyclerApps.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 4)
            adapter = appAdapter
            setHasFixedSize(true)
        }
        
        // 快捷方式
        shortcutAdapter = ShortcutAdapter(
            onShortcutClick = { shortcut -> handleShortcutClick(shortcut) },
            onShortcutLongClick = { shortcut -> showShortcutOptions(shortcut) }
        )
        
        binding.recyclerShortcuts.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = shortcutAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察应用列表
                viewModel.apps.collectLatest { apps ->
                    appAdapter.submitList(apps)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察快捷方式
                viewModel.shortcuts.collectLatest { shortcuts ->
                    shortcutAdapter.submitList(shortcuts)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察媒体播放状态
                (application as CarLauncherApp).mediaControllerManager.playbackState.collect { state ->
                    updateMediaWidget(state)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察导航状态
                (application as CarLauncherApp).navigationManager.navigationState.collect { state ->
                    updateNavigationWidget(state)
                }
            }
        }
    }

    private fun setupClickListeners() {
        // 设置按钮
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // 应用管理
        binding.btnAppManager.setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }
        
        // 主题切换
        binding.btnTheme.setOnClickListener {
            startActivity(Intent(this, ThemeActivity::class.java))
        }
        
        // 语音助手
        binding.btnVoice.setOnClickListener {
            startActivity(Intent(this, VoiceAssistantActivity::class.java))
        }
        
        // 导航
        binding.cardNavigation.setOnClickListener {
            launchNavigation()
        }
        
        // 媒体控制
        binding.cardMedia.setOnClickListener {
            launchMediaPlayer()
        }
        
        // 播放/暂停
        binding.btnPlayPause.setOnClickListener {
            (application as CarLauncherApp).mediaControllerManager.togglePlayPause()
        }
        
        // 下一首
        binding.btnNext.setOnClickListener {
            (application as CarLauncherApp).mediaControllerManager.skipToNext()
        }
    }

    private fun startServices() {
        // 启动悬浮球服务
        if (FloatingBallService.isRunning.not()) {
            startService(Intent(this, FloatingBallService::class.java))
        }
    }

    private fun updateTime() {
        binding.tvTime.text = TimeUtils.getCurrentTime()
        binding.tvDate.text = TimeUtils.getCurrentDate()
        
        // 每分钟更新
        binding.root.postDelayed({ updateTime() }, 60000)
    }

    private fun updateVehicleInfo() {
        // 模拟车辆信息更新
        binding.tvSpeed.text = "0"
        binding.tvTemperature.text = "24°C"
    }

    private fun updateMediaWidget(state: com.idocar.launcher.data.MediaPlaybackState) {
        binding.tvMediaTitle.text = state.title.takeIf { it.isNotEmpty() } ?: getString(R.string.no_media)
        binding.tvMediaArtist.text = state.artist
        binding.btnPlayPause.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateNavigationWidget(state: com.idocar.launcher.data.NavigationState) {
        if (state.isNavigating) {
            binding.tvNavDestination.text = state.destination
            binding.tvNavInfo.text = "${state.distanceRemaining / 1000}km · ${state.timeRemaining / 60}min"
            binding.cardNavigation.visibility = View.VISIBLE
        } else {
            binding.tvNavDestination.text = getString(R.string.tap_to_navigate)
            binding.tvNavInfo.text = ""
        }
    }

    private fun launchApp(app: AppItem) {
        val intent = AppUtils.getLaunchIntent(this, app.packageName)
        if (intent != null) {
            startActivity(intent)
            viewModel.updateAppUsage(app.packageName)
        } else {
            Toast.makeText(this, R.string.cannot_open_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchNavigation() {
        val navApps = listOf(
            "com.autonavi.minimap",      // 高德地图
            "com.baidu.BaiduMap",        // 百度地图
            "com.google.android.apps.maps" // Google Maps
        )
        
        for (pkg in navApps) {
            val intent = AppUtils.getLaunchIntent(this, pkg)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
        
        // 打开导航选择界面
        startActivity(Intent(this, NavigationActivity::class.java))
    }

    private fun launchMediaPlayer() {
        val intent = Intent(this, MediaActivity::class.java)
        startActivity(intent)
    }

    private fun handleShortcutClick(shortcut: ShortcutItem) {
        when (shortcut.actionType) {
            ShortcutItem.ActionType.APP -> {
                shortcut.targetPackage?.let { pkg ->
                    AppUtils.getLaunchIntent(this, pkg)?.let { startActivity(it) }
                }
            }
            ShortcutItem.ActionType.FUNCTION -> {
                handleFunctionShortcut(shortcut)
            }
            ShortcutItem.ActionType.WIDGET -> {
                // 打开小部件
            }
            ShortcutItem.ActionType.CUSTOM -> {
                // 自定义操作
            }
        }
    }

    private fun handleFunctionShortcut(shortcut: ShortcutItem) {
        when (shortcut.extraData) {
            "wifi" -> openSettings("android.settings.WIFI_SETTINGS")
            "bluetooth" -> openSettings("android.settings.BLUETOOTH_SETTINGS")
            "display" -> openSettings("android.settings.DISPLAY_SETTINGS")
            "volume" -> openSettings("android.settings.SOUND_SETTINGS")
        }
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.action_not_available, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppOptions(app: AppItem) {
        // 显示应用选项对话框
        AppOptionsDialog.newInstance(app).show(supportFragmentManager, "app_options")
    }

    private fun showShortcutOptions(shortcut: ShortcutItem) {
        // 显示快捷方式选项
    }

    override fun onResume() {
        super.onResume()
        updateTime()
    }

    override fun onBackPressed() {
        // 主界面不响应返回键，保持桌面状态
        // 可以添加确认退出或进入最近任务
    }
}
