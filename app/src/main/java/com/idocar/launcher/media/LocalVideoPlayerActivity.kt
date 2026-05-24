package com.idocar.launcher.media

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idocar.launcher.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 本地视频播放器界面
 * 使用 ExoPlayer (Media3) 播放视频，支持手势控制、全屏切换和播放列表侧边栏
 */
class LocalVideoPlayerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LocalVideoPlayerActivity"

        /** Intent Extra 键 */
        const val EXTRA_VIDEO_LIST = "video_list"
        const val EXTRA_VIDEO_INDEX = "video_index"
        const val EXTRA_VIDEO_TITLE = "video_title"

        /** 控制栏自动隐藏延迟（毫秒） */
        private const val CONTROLS_HIDE_DELAY = 3000L

        /** 手势灵敏度 */
        private const val BRIGHTNESS_SENSITIVITY = 0.005f
        private const val VOLUME_SENSITIVITY = 0.005f
        private const val SEEK_SENSITIVITY = 0.5f

        /** 快进/快退步长（毫秒） */
        private const val SEEK_STEP_MS = 10_000L
    }

    // ==================== 视图组件 ====================

    private lateinit var playerView: PlayerView
    private lateinit var controlsOverlay: View
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnFullscreen: ImageButton
    private lateinit var btnPlaylist: ImageButton
    private lateinit var playlistPanel: View
    private lateinit var playlistRecyclerView: RecyclerView

    // 手势提示
    private lateinit var gestureOverlay: View
    private lateinit var gestureIcon: ImageView
    private lateinit var gestureText: TextView

    // ==================== 播放器 ====================

    private var player: ExoPlayer? = null

    /** 视频列表（MediaItem 路径列表） */
    private var videoPaths: ArrayList<String> = ArrayList()

    /** 当前播放索引 */
    private var currentIndex: Int = 0

    /** 是否全屏 */
    private var isFullscreen: Boolean = false

    /** 是否锁定屏幕 */
    private var isLocked: Boolean = false

    /** 控制栏是否可见 */
    private var isControlsVisible: Boolean = true

    /** 控制栏自动隐藏 Handler */
    private val hideControlsHandler = android.os.Handler(mainLooper)

    // ==================== 手势控制 ====================

    private lateinit var gestureDetector: GestureDetector
    private var gestureStartX: Float = 0f
    private var gestureStartY: Float = 0f
    private var initialBrightness: Float = -1f
    private var initialVolume: Int = -1
    private var seekStartPosition: Long = -1L
    private var isSeeking: Boolean = false
    private var isBrightnessGesture: Boolean = false
    private var isVolumeGesture: Boolean = false
    private var isSeekGesture: Boolean = false

    // ==================== 音量管理 ====================

    private lateinit var audioManager: AudioManager
    private var maxVolume: Int = 0

    // ==================== 播放列表适配器 ====================

    private lateinit var playlistAdapter: VideoPlaylistAdapter

    // ==================== 广播接收器 ====================

    private val videoStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                VideoPlaybackService.ACTION_VIDEO_STATE_CHANGED -> {
                    updatePlayPauseButton()
                }
                VideoPlaybackService.ACTION_VIDEO_ITEM_CHANGED -> {
                    val newPosition = intent.getIntExtra(
                        VideoPlaybackService.EXTRA_VIDEO_QUEUE_POSITION, -1
                    )
                    if (newPosition >= 0 && newPosition != currentIndex) {
                        currentIndex = newPosition
                        updatePlaylistSelection()
                    }
                }
            }
        }
    }

    // ==================== ExoPlayer 监听器 ====================

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    seekBar.max = (player?.duration ?: 0).toInt()
                    tvTotalTime.text = formatTime(player?.duration ?: 0)
                    updatePlayPauseButton()
                }
                Player.STATE_ENDED -> {
                    playNext()
                }
                Player.STATE_BUFFERING -> {
                    // 可显示加载指示器
                }
                Player.STATE_IDLE -> {
                    // 播放器空闲
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseButton()
            if (isPlaying) {
                startProgressUpdate()
                scheduleHideControls()
            } else {
                stopProgressUpdate()
                cancelHideControls()
            }
        }
    }

    // ==================== Activity 生命周期 ====================

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏设置
        setupFullscreen()

        setContentView(R.layout.activity_local_video_player)

        // 初始化音频管理器
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        // 初始化视图
        initViews()

        // 初始化手势检测
        initGestureDetector()

        // 设置播放列表
        initPlaylist()

        // 获取 Intent 数据
        parseIntent()

        // 初始化播放器
        initializePlayer()

        // 设置监听器
        setupListeners()

        // 注册广播接收器
        registerVideoStateReceiver()

        // 初始显示控制栏
        showControls()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isFullscreen = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        updateFullscreenState()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressUpdate()
        cancelHideControls()
        player?.removeListener(playerListener)
        player?.release()
        player = null
        unregisterVideoStateReceiver()
    }

    // ==================== 初始化方法 ====================

    private fun setupFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    private fun initViews() {
        playerView = findViewById(R.id.player_view)
        controlsOverlay = findViewById(R.id.controls_overlay)
        topBar = findViewById(R.id.top_bar)
        bottomBar = findViewById(R.id.bottom_bar)
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_title)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnPrevious = findViewById(R.id.btn_previous)
        btnNext = findViewById(R.id.btn_next)
        seekBar = findViewById(R.id.seek_bar)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvTotalTime = findViewById(R.id.tv_total_time)
        btnFullscreen = findViewById(R.id.btn_fullscreen)
        btnPlaylist = findViewById(R.id.btn_playlist)
        playlistPanel = findViewById(R.id.playlist_panel)
        playlistRecyclerView = findViewById(R.id.playlist_recycler_view)
        gestureOverlay = findViewById(R.id.gesture_overlay)
        gestureIcon = findViewById(R.id.gesture_icon)
        gestureText = findViewById(R.id.gesture_text)
    }

    private fun initGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                gestureStartX = e.x
                gestureStartY = e.y
                initialBrightness = -1f
                initialVolume = -1
                seekStartPosition = -1L
                isSeeking = false
                isBrightnessGesture = false
                isVolumeGesture = false
                isSeekGesture = false
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 == null) return false

                val deltaX = e2.x - gestureStartX
                val deltaY = e2.y - gestureStartY
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels

                // 判断手势类型
                if (!isBrightnessGesture && !isVolumeGesture && !isSeekGesture) {
                    when {
                        abs(deltaX) > abs(deltaY) && abs(deltaX) > 50 -> {
                            // 水平滑动 - 快进快退
                            isSeekGesture = true
                            seekStartPosition = player?.currentPosition ?: 0
                        }
                        abs(deltaY) > abs(deltaX) && abs(deltaY) > 50 -> {
                            if (gestureStartX < screenWidth / 2f) {
                                // 左侧上下滑动 - 亮度
                                isBrightnessGesture = true
                                try {
                                    initialBrightness = Settings.System.getInt(
                                        contentResolver,
                                        Settings.System.SCREEN_BRIGHTNESS
                                    ) / 255f
                                } catch (e: Settings.SettingNotFoundException) {
                                    initialBrightness = 0.5f
                                }
                            } else {
                                // 右侧上下滑动 - 音量
                                isVolumeGesture = true
                                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            }
                        }
                    }
                }

                // 处理手势
                when {
                    isBrightnessGesture -> handleBrightnessGesture(deltaY, screenHeight)
                    isVolumeGesture -> handleVolumeGesture(deltaY, screenHeight)
                    isSeekGesture -> handleSeekGesture(deltaX, screenWidth)
                }

                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isControlsVisible) {
                    hideControls()
                } else {
                    showControls()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenWidth = resources.displayMetrics.widthPixels
                if (e.x < screenWidth / 2f) {
                    // 左侧双击 - 快退
                    seekBackward()
                } else {
                    // 右侧双击 - 快进
                    seekForward()
                }
                return true
            }
        })

        // 在 PlayerView 上设置触摸监听
        playerView.setOnTouchListener { _, event ->
            if (isLocked && event.action != MotionEvent.ACTION_DOWN) {
                return@setOnTouchListener true
            }
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun initPlaylist() {
        playlistAdapter = VideoPlaylistAdapter(
            onItemClick = { position ->
                playAt(position)
            }
        )
        playlistRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@LocalVideoPlayerActivity)
            adapter = playlistAdapter
        }
    }

    private fun parseIntent() {
        videoPaths = intent.getStringArrayListExtra(EXTRA_VIDEO_LIST) ?: ArrayList()
        currentIndex = intent.getIntExtra(EXTRA_VIDEO_INDEX, 0)
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: ""
        tvTitle.text = title
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            playerView.player = this
            addListener(playerListener)

            if (videoPaths.isNotEmpty() && currentIndex < videoPaths.size) {
                val mediaItem = MediaItem.fromUri(videoPaths[currentIndex])
                setMediaItem(mediaItem)
                prepare()
                play()
            }
        }

        // 更新播放列表
        updatePlaylist()
    }

    // ==================== 监听器设置 ====================

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnPlayPause.setOnClickListener {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
        }

        btnPrevious.setOnClickListener {
            playPrevious()
        }

        btnNext.setOnClickListener {
            playNext()
        }

        btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }

        btnPlaylist.setOnClickListener {
            togglePlaylist()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                    tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopProgressUpdate()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                startProgressUpdate()
            }
        })

        // 点击控制栏区域时重置隐藏计时器
        controlsOverlay.setOnClickListener {
            if (!isControlsVisible) {
                showControls()
            } else {
                hideControls()
            }
        }
    }

    // ==================== 播放控制 ====================

    private fun playAt(index: Int) {
        if (index in videoPaths.indices) {
            currentIndex = index
            val mediaItem = MediaItem.fromUri(videoPaths[index])
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()

            // 更新标题
            val fileName = videoPaths[index].substringAfterLast('/')
            tvTitle.text = fileName

            // 更新播放列表选中状态
            updatePlaylistSelection()
        }
    }

    private fun playNext() {
        if (currentIndex < videoPaths.size - 1) {
            playAt(currentIndex + 1)
        } else if (videoPaths.isNotEmpty()) {
            // 循环到第一个
            playAt(0)
        }
    }

    private fun playPrevious() {
        if (currentIndex > 0) {
            playAt(currentIndex - 1)
        } else if (videoPaths.isNotEmpty()) {
            playAt(videoPaths.size - 1)
        }
    }

    private fun seekForward() {
        player?.let {
            val newPosition = it.currentPosition + SEEK_STEP_MS
            it.seekTo(min(newPosition, it.duration.coerceAtLeast(0)))
            showSeekToast("快进 ${SEEK_STEP_MS / 1000} 秒")
        }
    }

    private fun seekBackward() {
        player?.let {
            val newPosition = it.currentPosition - SEEK_STEP_MS
            it.seekTo(max(newPosition, 0))
            showSeekToast("快退 ${SEEK_STEP_MS / 1000} 秒")
        }
    }

    // ==================== 手势处理 ====================

    private fun handleBrightnessGesture(deltaY: Float, screenHeight: Int) {
        if (initialBrightness < 0) return

        val delta = -deltaY / screenHeight
        val newBrightness = (initialBrightness + delta).coerceIn(0f, 1f)

        // 设置屏幕亮度
        val layoutParams = window.attributes
        layoutParams.screenBrightness = newBrightness
        window.attributes = layoutParams

        // 显示手势提示
        showGestureHint(
            iconRes = R.drawable.ic_brightness,
            text = "${(newBrightness * 100).toInt()}%"
        )
    }

    private fun handleVolumeGesture(deltaY: Float, screenHeight: Int) {
        if (initialVolume < 0) return

        val delta = (-deltaY / screenHeight * maxVolume).toInt()
        val newVolume = (initialVolume + delta).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)

        // 显示手势提示
        showGestureHint(
            iconRes = R.drawable.ic_volume,
            text = "${newVolume}/${maxVolume}"
        )
    }

    private fun handleSeekGesture(deltaX: Float, screenWidth: Int) {
        if (seekStartPosition < 0) return

        val duration = player?.duration ?: return
        val seekDelta = (deltaX / screenWidth * duration * SEEK_SENSITIVITY).toLong()
        val newPosition = (seekStartPosition + seekDelta).coerceIn(0, duration)

        player?.seekTo(newPosition)

        // 显示手势提示
        val seekSeconds = ((newPosition - seekStartPosition) / 1000).toInt()
        val sign = if (seekSeconds >= 0) "+" else ""
        showGestureHint(
            iconRes = if (seekSeconds >= 0) R.drawable.ic_fast_forward else R.drawable.ic_fast_rewind,
            text = "${sign}${seekSeconds}秒\n${formatTime(newPosition)}"
        )
    }

    private fun showGestureHint(iconRes: Int, text: String) {
        gestureOverlay.visibility = View.VISIBLE
        gestureIcon.setImageResource(iconRes)
        gestureText.text = text

        // 3秒后自动隐藏
        hideControlsHandler.postDelayed({
            gestureOverlay.visibility = View.GONE
        }, 1000)
    }

    private fun showSeekToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ==================== 控制栏显示/隐藏 ====================

    private fun showControls() {
        isControlsVisible = true
        controlsOverlay.visibility = View.VISIBLE
        topBar.visibility = View.VISIBLE
        bottomBar.visibility = View.VISIBLE

        if (!isFullscreen) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            }
        }

        scheduleHideControls()
    }

    private fun hideControls() {
        isControlsVisible = false
        controlsOverlay.visibility = View.GONE
        topBar.visibility = View.GONE
        bottomBar.visibility = View.GONE
        gestureOverlay.visibility = View.GONE

        // 隐藏系统栏
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    private fun scheduleHideControls() {
        cancelHideControls()
        hideControlsHandler.postDelayed({
            if (player?.isPlaying == true) {
                hideControls()
            }
        }, CONTROLS_HIDE_DELAY)
    }

    private fun cancelHideControls() {
        hideControlsHandler.removeCallbacksAndMessages(null)
    }

    // ==================== 全屏切换 ====================

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        updateFullscreenState()
    }

    private fun updateFullscreenState() {
        if (isFullscreen) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    // ==================== 播放列表 ====================

    private fun togglePlaylist() {
        if (playlistPanel.visibility == View.VISIBLE) {
            playlistPanel.visibility = View.GONE
        } else {
            playlistPanel.visibility = View.VISIBLE
        }
    }

    private fun updatePlaylist() {
        val items = videoPaths.mapIndexed { index, path ->
            val fileName = path.substringAfterLast('/')
            PlaylistItem(index, fileName, path)
        }
        playlistAdapter.submitList(items)
        updatePlaylistSelection()
    }

    private fun updatePlaylistSelection() {
        playlistAdapter.setSelectedPosition(currentIndex)
        playlistRecyclerView.scrollToPosition(currentIndex)
    }

    // ==================== 进度更新 ====================

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            player?.let {
                if (it.isPlaying) {
                    seekBar.progress = it.currentPosition.toInt()
                    tvCurrentTime.text = formatTime(it.currentPosition)
                }
            }
            player?.let {
                if (it.isPlaying) {
                    hideControlsHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        hideControlsHandler.postDelayed(progressUpdateRunnable, 1000)
    }

    private fun stopProgressUpdate() {
        hideControlsHandler.removeCallbacks(progressUpdateRunnable)
    }

    // ==================== UI 更新 ====================

    private fun updatePlayPauseButton() {
        val isPlaying = player?.isPlaying ?: false
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    // ==================== 广播接收器 ====================

    private fun registerVideoStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(VideoPlaybackService.ACTION_VIDEO_STATE_CHANGED)
            addAction(VideoPlaybackService.ACTION_VIDEO_ITEM_CHANGED)
        }
        ContextCompat.registerReceiver(this, videoStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterVideoStateReceiver() {
        try {
            unregisterReceiver(videoStateReceiver)
        } catch (e: IllegalArgumentException) {
            // 忽略
        }
    }

    // ==================== 工具方法 ====================

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // ==================== 播放列表数据类和适配器 ====================

    /**
     * 播放列表项数据类
     */
    data class PlaylistItem(
        val index: Int,
        val title: String,
        val path: String
    )

    /**
     * 视频播放列表适配器
     */
    class VideoPlaylistAdapter(
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<VideoPlaylistAdapter.ViewHolder>() {

        private val items = mutableListOf<PlaylistItem>()
        private var selectedPosition = -1

        fun submitList(newItems: List<PlaylistItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun setSelectedPosition(position: Int) {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_playlist, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], position == selectedPosition)
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tv_playlist_title)
            private val tvIndex: TextView = itemView.findViewById(R.id.tv_playlist_index)
            private val playingIndicator: View = itemView.findViewById(R.id.playing_indicator)

            fun bind(item: PlaylistItem, isSelected: Boolean) {
                tvTitle.text = item.title
                tvIndex.text = "${item.index + 1}"

                if (isSelected) {
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                    )
                    tvTitle.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                    playingIndicator.visibility = View.VISIBLE
                } else {
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, android.R.color.transparent)
                    )
                    tvTitle.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                    playingIndicator.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    onItemClick(item.index)
                }
            }
        }
    }
}
