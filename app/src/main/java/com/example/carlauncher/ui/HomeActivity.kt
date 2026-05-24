package com.example.carlauncher.ui

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.carlauncher.CarLauncherApp
import com.example.carlauncher.R
import com.example.carlauncher.adapter.AppGridAdapter
import com.example.carlauncher.data.AppItem
import com.example.carlauncher.databinding.ActivityHomeBinding
import com.example.carlauncher.databinding.DialogVolumeControlBinding
import com.example.carlauncher.service.FloatingBallService
import com.example.carlauncher.util.AppUtils
import com.example.carlauncher.util.TimeUtils
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var appAdapter: AppGridAdapter
    private lateinit var audioManager: AudioManager
    
    private var volumeDialog: Dialog? = null
    private var isEditMode = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            initializeComponents()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        setupFullscreen()
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
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            initializeComponents()
        }
    }

    private fun initializeComponents() {
        setupUI()
        setupRecyclerView()
        setupClickListeners()
        startServices()
    }

    private fun setupUI() {
        updateTime()
    }

    private fun setupRecyclerView() {
        appAdapter = AppGridAdapter(
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> toggleEditMode() },
            onRemoveClick = { app -> removeAppFromGrid(app) }
        )

        binding.recyclerApps.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = appAdapter
        }

        // 加载已添加的应用
        loadAddedApps()
    }

    private fun setupClickListeners() {
        // 底部导航栏
        binding.btnHome.setOnClickListener { /* 已在主页 */ }
        binding.btnApps.setOnClickListener { openAppManager() }
        binding.btnNav.setOnClickListener { launchNavigation() }
        binding.btnMusic.setOnClickListener { launchMusic() }
        binding.btnSettings.setOnClickListener { openSettings() }
        binding.btnVoice.setOnClickListener { openVoiceAssistant() }
        binding.btnVolume.setOnClickListener { showVolumeDialog() }
        
        // 横屏额外的按钮
        binding.btnFiles?.setOnClickListener { /* 文件管理 */ }
        binding.btnLock?.setOnClickListener { lockScreen() }

        // 主卡片点击
        binding.cardNavigation.setOnClickListener { launchNavigation() }
        binding.cardMedia.setOnClickListener { launchMusic() }

        // 音乐控制
        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnNext.setOnClickListener { nextTrack() }
        binding.btnPrevious?.setOnClickListener { previousTrack() }
        binding.btnPlaylist?.setOnClickListener { showPlaylist() }

        // 添加应用
        binding.btnAddApp.setOnClickListener { showAddAppDialog() }
    }

    private fun startServices() {
        if (!FloatingBallService.isRunning) {
            startService(android.content.Intent(this, FloatingBallService::class.java))
        }
    }

    private fun updateTime() {
        binding.tvTime.text = TimeUtils.getCurrentTime()
        binding.tvDate.text = TimeUtils.getCurrentDate()
        binding.root.postDelayed({ updateTime() }, 60000)
    }

    private fun launchApp(app: AppItem) {
        val intent = AppUtils.getLaunchIntent(this, app.packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.cannot_open_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchNavigation() {
        val navApps = listOf(
            "com.autonavi.minimap",
            "com.baidu.BaiduMap",
            "com.google.android.apps.maps"
        )
        for (pkg in navApps) {
            AppUtils.getLaunchIntent(this, pkg)?.let { 
                startActivity(it)
                return
            }
        }
        Toast.makeText(this, R.string.no_navigation_app, Toast.LENGTH_SHORT).show()
    }

    private fun launchMusic() {
        val musicApps = listOf(
            "com.tencent.qqmusic",
            "com.kugou.android",
            "com.netease.cloudmusic"
        )
        for (pkg in musicApps) {
            AppUtils.getLaunchIntent(this, pkg)?.let { 
                startActivity(it)
                return
            }
        }
        // 打开本地音乐
        startActivity(android.content.Intent(this, MediaActivity::class.java))
    }

    private fun openSettings() {
        startActivity(android.content.Intent(this, SettingsActivity::class.java))
    }

    private fun openVoiceAssistant() {
        startActivity(android.content.Intent(this, VoiceAssistantActivity::class.java))
    }

    private fun openAppManager() {
        startActivity(android.content.Intent(this, AppManagerActivity::class.java))
    }

    private fun togglePlayPause() {
        (application as CarLauncherApp).mediaControllerManager.togglePlayPause()
    }

    private fun nextTrack() {
        (application as CarLauncherApp).mediaControllerManager.skipToNext()
    }

    private fun previousTrack() {
        (application as CarLauncherApp).mediaControllerManager.skipToPrevious()
    }

    private fun showPlaylist() {
        startActivity(android.content.Intent(this, MediaActivity::class.java))
    }

    private fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        finish()
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        appAdapter.setEditMode(isEditMode)
        if (isEditMode) {
            Toast.makeText(this, "编辑模式：点击图标删除", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeAppFromGrid(app: AppItem) {
        val prefs = getSharedPreferences("home_grid", MODE_PRIVATE)
        val apps = prefs.getStringSet("added_apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        apps.remove(app.packageName)
        prefs.edit().putStringSet("added_apps", apps).apply()
        loadAddedApps()
    }

    private fun loadAddedApps() {
        val prefs = getSharedPreferences("home_grid", MODE_PRIVATE)
        val packageNames = prefs.getStringSet("added_apps", emptySet()) ?: emptySet()
        
        val apps = packageNames.mapNotNull { pkg ->
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                AppItem(
                    packageName = pkg,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    activityName = "",
                    icon = packageManager.getApplicationIcon(pkg)
                )
            } catch (e: Exception) {
                null
            }
        }
        appAdapter.submitList(apps)
    }

    private fun showAddAppDialog() {
        val prefs = getSharedPreferences("home_grid", MODE_PRIVATE)
        val existingApps = prefs.getStringSet("added_apps", emptySet()) ?: emptySet()

        if (existingApps.size >= 10) {
            Toast.makeText(this, "主页应用网格最多添加10个应用", Toast.LENGTH_SHORT).show()
            return
        }

        val installedApps = AppUtils.getInstalledApps(this)
        
        val availableApps = installedApps.filter { it.packageName !in existingApps }
        
        if (availableApps.isEmpty()) {
            Toast.makeText(this, "没有更多可添加的应用", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("添加应用到主页")
            .setItems(availableApps.map { it.appName }.toTypedArray()) { _, which ->
                val app = availableApps[which]
                addAppToGrid(app)
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    private fun addAppToGrid(app: AppItem) {
        val prefs = getSharedPreferences("home_grid", MODE_PRIVATE)
        val apps = prefs.getStringSet("added_apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        if (apps.size >= 10) {
            Toast.makeText(this, "主页应用网格最多添加10个应用", Toast.LENGTH_SHORT).show()
            return
        }

        apps.add(app.packageName)
        prefs.edit().putStringSet("added_apps", apps).apply()
        loadAddedApps()
    }

    private fun showVolumeDialog() {
        if (volumeDialog?.isShowing == true) {
            volumeDialog?.dismiss()
            return
        }

        val dialogBinding = DialogVolumeControlBinding.inflate(layoutInflater)
        volumeDialog = Dialog(this, R.style.Theme_CarLauncher).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        // 初始化音量显示
        updateVolumeSeekbar(dialogBinding)

        // 媒体音量
        setupVolumeControls(
            dialogBinding.seekbarMedia,
            dialogBinding.tvMediaVolume,
            dialogBinding.btnMediaMinus,
            dialogBinding.btnMediaPlus,
            AudioManager.STREAM_MUSIC
        )

        // 铃声音量
        setupVolumeControls(
            dialogBinding.seekbarRingtone,
            dialogBinding.tvRingtoneVolume,
            dialogBinding.btnRingtoneMinus,
            dialogBinding.btnRingtonePlus,
            AudioManager.STREAM_RING
        )

        // 闹钟音量
        setupVolumeControls(
            dialogBinding.seekbarAlarm,
            dialogBinding.tvAlarmVolume,
            dialogBinding.btnAlarmMinus,
            dialogBinding.btnAlarmPlus,
            AudioManager.STREAM_ALARM
        )

        dialogBinding.btnClose.setOnClickListener {
            volumeDialog?.dismiss()
        }

        volumeDialog?.show()
    }

    private fun updateVolumeSeekbar(dialogBinding: DialogVolumeControlBinding) {
        val mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val mediaMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        dialogBinding.seekbarMedia.max = mediaMax
        dialogBinding.seekbarMedia.progress = mediaVolume
        dialogBinding.tvMediaVolume.text = ((mediaVolume * 100) / mediaMax).toString()

        val ringtoneVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val ringtoneMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        dialogBinding.seekbarRingtone.max = ringtoneMax
        dialogBinding.seekbarRingtone.progress = ringtoneVolume
        dialogBinding.tvRingtoneVolume.text = ((ringtoneVolume * 100) / ringtoneMax).toString()

        val alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val alarmMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        dialogBinding.seekbarAlarm.max = alarmMax
        dialogBinding.seekbarAlarm.progress = alarmVolume
        dialogBinding.tvAlarmVolume.text = ((alarmVolume * 100) / alarmMax).toString()
    }

    private fun setupVolumeControls(
        seekBar: SeekBar,
        volumeText: android.widget.TextView,
        minusBtn: View,
        plusBtn: View,
        streamType: Int
    ) {
        val max = audioManager.getStreamMaxVolume(streamType)

        minusBtn.setOnClickListener {
            val current = audioManager.getStreamVolume(streamType)
            if (current > 0) {
                audioManager.setStreamVolume(streamType, current - 1, 0)
                seekBar.progress = current - 1
                volumeText.text = (((current - 1) * 100) / max).toString()
            }
        }

        plusBtn.setOnClickListener {
            val current = audioManager.getStreamVolume(streamType)
            if (current < max) {
                audioManager.setStreamVolume(streamType, current + 1, 0)
                seekBar.progress = current + 1
                volumeText.text = (((current + 1) * 100) / max).toString()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(streamType, progress, 0)
                    volumeText.text = ((progress * 100) / max).toString()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        updateTime()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }
}
