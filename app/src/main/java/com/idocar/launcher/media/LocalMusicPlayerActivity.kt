package com.idocar.launcher.media

import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.idocar.launcher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 本地音乐播放器界面
 * 提供完整的音乐播放体验，包括专辑封面旋转动画、播放列表管理和歌词显示
 */
class LocalMusicPlayerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LocalMusicPlayerActivity"

        /** Intent Extra 键 */
        const val EXTRA_MEDIA_LIST = "media_list"
        const val EXTRA_START_INDEX = "start_index"

        /** 进度更新间隔（毫秒） */
        private const val PROGRESS_UPDATE_INTERVAL = 200L
    }

    // ==================== 视图组件 ====================

    // 顶部栏
    private lateinit var btnBack: ImageButton
    private lateinit var tvSongTitle: TextView
    private lateinit var tvSongArtist: TextView

    // 中间区域 - 专辑封面
    private lateinit var ivAlbumArt: ImageView
    private lateinit var ivDiscBackground: ImageView

    // 进度条
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView

    // 控制按钮
    private lateinit var btnShuffle: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnRepeat: ImageButton

    // 播放列表
    private lateinit var tabLayout: com.google.android.material.tabs.TabLayout
    private lateinit var recyclerView: RecyclerView

    // 歌词区域
    private lateinit var lyricsContainer: View
    private lateinit var tvLyrics: TextView

    // ==================== 数据 ====================

    /** 完整媒体列表 */
    private var allMediaItems: List<MediaItem> = emptyList()

    /** 当前显示的媒体列表（根据 Tab 筛选） */
    private var currentMediaItems: List<MediaItem> = emptyList()

    /** 当前播放索引 */
    private var currentIndex: Int = -1

    /** 是否正在播放 */
    private var isPlaying: Boolean = false

    /** 随机播放模式 */
    private var isShuffle: Boolean = false

    /** 循环模式 */
    private var repeatMode: Int = MediaPlaybackService.REPEAT_MODE_OFF

    // ==================== 专辑封面旋转动画 ====================

    private var discRotateAnimation: RotateAnimation? = null

    // ==================== 进度更新 ====================

    private var progressUpdateJob: Job? = null

    // ==================== 播放列表适配器 ====================

    private lateinit var playlistAdapter: MusicPlaylistAdapter

    // ==================== 媒体扫描器 ====================

    private lateinit var mediaScanner: MediaScanner

    // ==================== 广播接收器 ====================

    private val playStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MediaPlaybackService.ACTION_PLAY_STATE_CHANGED -> {
                    isPlaying = intent.getBooleanExtra(MediaPlaybackService.EXTRA_IS_PLAYING, false)
                    updatePlayPauseButton()
                    updateDiscAnimation()

                    val position = intent.getLongExtra(MediaPlaybackService.EXTRA_POSITION, 0)
                    val duration = intent.getLongExtra(MediaPlaybackService.EXTRA_DURATION, 0)
                    if (duration > 0) {
                        seekBar.max = duration.toInt()
                        tvTotalTime.text = formatTime(duration)
                    }
                    tvCurrentTime.text = formatTime(position)
                }
                MediaPlaybackService.ACTION_SONG_CHANGED -> {
                    val queuePosition = intent.getIntExtra(MediaPlaybackService.EXTRA_QUEUE_POSITION, -1)
                    if (queuePosition >= 0) {
                        currentIndex = queuePosition
                        updateSongInfo()
                        updatePlaylistSelection()
                    }
                }
            }
        }
    }

    // ==================== Activity 生命周期 ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_music_player)

        // 初始化媒体扫描器
        mediaScanner = MediaScanner(this)

        // 初始化视图
        initViews()

        // 初始化播放列表适配器
        initPlaylistAdapter()

        // 设置监听器
        setupListeners()

        // 设置 TabLayout
        setupTabLayout()

        // 初始化旋转动画
        initDiscAnimation()

        // 注册广播接收器
        registerPlayStateReceiver()

        // 解析 Intent 数据
        parseIntent()

        // 加载媒体数据
        loadMediaData()
    }

    override fun onResume() {
        super.onResume()
        startProgressUpdate()
    }

    override fun onPause() {
        super.onPause()
        stopProgressUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressUpdate()
        unregisterPlayStateReceiver()
        discRotateAnimation?.cancel()
        mediaScanner.release()
    }

    // ==================== 初始化方法 ====================

    private fun initViews() {
        // 顶部栏
        btnBack = findViewById(R.id.btn_back)
        tvSongTitle = findViewById(R.id.tv_song_title)
        tvSongArtist = findViewById(R.id.tv_song_artist)

        // 专辑封面
        ivAlbumArt = findViewById(R.id.iv_album_art)
        ivDiscBackground = findViewById(R.id.iv_disc_background)

        // 进度条
        seekBar = findViewById(R.id.seek_bar)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvTotalTime = findViewById(R.id.tv_total_time)

        // 控制按钮
        btnShuffle = findViewById(R.id.btn_shuffle)
        btnPrevious = findViewById(R.id.btn_previous)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnNext = findViewById(R.id.btn_next)
        btnRepeat = findViewById(R.id.btn_repeat)

        // 播放列表
        tabLayout = findViewById(R.id.tab_layout)
        recyclerView = findViewById(R.id.recycler_view)

        // 歌词
        lyricsContainer = findViewById(R.id.lyrics_container)
        tvLyrics = findViewById(R.id.tv_lyrics)
    }

    private fun initPlaylistAdapter() {
        playlistAdapter = MusicPlaylistAdapter(
            onItemClick = { item, position ->
                playSong(item, position)
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LocalMusicPlayerActivity)
            adapter = playlistAdapter
        }
    }

    private fun initDiscAnimation() {
        discRotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 20000 // 20秒转一圈
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("全部"))
        tabLayout.addTab(tabLayout.newTab().setText("本地"))
        tabLayout.addTab(tabLayout.newTab().setText("USB"))

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                when (tab.position) {
                    0 -> showAllSongs()
                    1 -> showLocalSongs()
                    2 -> showUsbSongs()
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    private fun setupListeners() {
        // 返回按钮
        btnBack.setOnClickListener {
            finish()
        }

        // 播放/暂停
        btnPlayPause.setOnClickListener {
            sendControlAction(MediaPlaybackService.ACTION_TOGGLE_PLAY_PAUSE)
        }

        // 上一首
        btnPrevious.setOnClickListener {
            sendControlAction(MediaPlaybackService.ACTION_PREVIOUS)
        }

        // 下一首
        btnNext.setOnClickListener {
            sendControlAction(MediaPlaybackService.ACTION_NEXT)
        }

        // 随机播放
        btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            updateShuffleButton()
            sendControlAction(MediaPlaybackService.ACTION_TOGGLE_PLAY_PAUSE)
        }

        // 循环模式
        btnRepeat.setOnClickListener {
            repeatMode = when (repeatMode) {
                MediaPlaybackService.REPEAT_MODE_OFF -> MediaPlaybackService.REPEAT_MODE_ALL
                MediaPlaybackService.REPEAT_MODE_ALL -> MediaPlaybackService.REPEAT_MODE_ONE
                MediaPlaybackService.REPEAT_MODE_ONE -> MediaPlaybackService.REPEAT_MODE_OFF
                else -> MediaPlaybackService.REPEAT_MODE_OFF
            }
            updateRepeatButton()
        }

        // 进度条拖动
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopProgressUpdate()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val intent = Intent(MediaPlaybackService.ACTION_PLAY).apply {
                    setPackage(packageName)
                    putExtra("seek_to", seekBar?.progress?.toLong() ?: 0)
                }
                // 通过广播发送 seek 请求
                val seekIntent = Intent("com.idocar.launcher.action.SEEK").apply {
                    setPackage(packageName)
                    putExtra("position", seekBar?.progress?.toLong() ?: 0)
                }
                sendBroadcast(seekIntent)
                startProgressUpdate()
            }
        })
    }

    private fun parseIntent() {
        intent.getParcelableArrayListExtra<MediaItem>(EXTRA_MEDIA_LIST)?.let { list ->
            allMediaItems = list
            currentIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
        }
    }

    private fun loadMediaData() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (allMediaItems.isEmpty()) {
                // 如果没有传入列表，从 MediaScanner 加载
                allMediaItems = mediaScanner.scanAllMedia().filter {
                    it.type == MediaItem.MediaType.AUDIO
                }
            }

            launch(Dispatchers.Main) {
                showAllSongs()
                if (currentIndex >= 0 && currentIndex < allMediaItems.size) {
                    updateSongInfo()
                }
            }
        }
    }

    // ==================== 播放控制 ====================

    private fun playSong(item: MediaItem, position: Int) {
        currentIndex = position

        // 启动播放服务
        val intent = Intent(this, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_PLAY
            putParcelableArrayListExtra("queue", ArrayList(allMediaItems))
            putExtra("start_position", position)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        updateSongInfo()
        updatePlaylistSelection()
        isPlaying = true
        updatePlayPauseButton()
        updateDiscAnimation()
    }

    private fun playNext() {
        if (allMediaItems.isEmpty()) return

        val nextIndex = if (isShuffle) {
            (0 until allMediaItems.size).random()
        } else {
            if (currentIndex < allMediaItems.size - 1) currentIndex + 1 else 0
        }

        playSong(allMediaItems[nextIndex], nextIndex)
    }

    private fun playPrevious() {
        if (allMediaItems.isEmpty()) return

        val prevIndex = if (isShuffle) {
            (0 until allMediaItems.size).random()
        } else {
            if (currentIndex > 0) currentIndex - 1 else allMediaItems.size - 1
        }

        playSong(allMediaItems[prevIndex], prevIndex)
    }

    private fun sendControlAction(action: String) {
        val intent = Intent(action).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    // ==================== 播放列表 Tab 切换 ====================

    private fun showAllSongs() {
        currentMediaItems = allMediaItems.filter { it.type == MediaItem.MediaType.AUDIO }
        playlistAdapter.submitList(currentMediaItems)
        updatePlaylistSelection()
    }

    private fun showLocalSongs() {
        currentMediaItems = allMediaItems.filter {
            it.type == MediaItem.MediaType.AUDIO && !isUsbMedia(it)
        }
        playlistAdapter.submitList(currentMediaItems)
        updatePlaylistSelection()
    }

    private fun showUsbSongs() {
        currentMediaItems = allMediaItems.filter {
            it.type == MediaItem.MediaType.AUDIO && isUsbMedia(it)
        }
        playlistAdapter.submitList(currentMediaItems)
        updatePlaylistSelection()
    }

    /**
     * 判断是否为 USB 媒体
     */
    private fun isUsbMedia(item: MediaItem): Boolean {
        val usbPaths = mediaScanner.getUsbPaths()
        return usbPaths.any { item.path.startsWith(it) }
    }

    // ==================== UI 更新 ====================

    private fun updateSongInfo() {
        if (currentIndex in allMediaItems.indices) {
            val item = allMediaItems[currentIndex]
            tvSongTitle.text = item.title
            tvSongArtist.text = item.artist

            // 加载专辑封面
            loadAlbumArt(item.albumArt)

            // 更新进度条
            if (item.duration > 0) {
                seekBar.max = item.duration.toInt()
                tvTotalTime.text = formatTime(item.duration)
            }
        }
    }

    private fun loadAlbumArt(albumArtUri: String?) {
        if (albumArtUri.isNullOrEmpty()) {
            // 使用默认封面
            ivAlbumArt.setImageResource(R.drawable.ic_music_note)
            return
        }

        try {
            Glide.with(this)
                .load(albumArtUri)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .circleCrop()
                .into(ivAlbumArt)
        } catch (e: Exception) {
            ivAlbumArt.setImageResource(R.drawable.ic_music_note)
        }
    }

    private fun updatePlayPauseButton() {
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateDiscAnimation() {
        if (isPlaying) {
            ivDiscBackground.startAnimation(discRotateAnimation)
        } else {
            ivDiscBackground.clearAnimation()
        }
    }

    private fun updateShuffleButton() {
        if (isShuffle) {
            btnShuffle.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent))
        } else {
            btnShuffle.clearColorFilter()
        }
    }

    private fun updateRepeatButton() {
        when (repeatMode) {
            MediaPlaybackService.REPEAT_MODE_OFF -> {
                btnRepeat.setImageResource(R.drawable.ic_repeat)
                btnRepeat.clearColorFilter()
            }
            MediaPlaybackService.REPEAT_MODE_ALL -> {
                btnRepeat.setImageResource(R.drawable.ic_repeat)
                btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent))
            }
            MediaPlaybackService.REPEAT_MODE_ONE -> {
                btnRepeat.setImageResource(R.drawable.ic_repeat_one)
                btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent))
            }
        }
    }

    private fun updatePlaylistSelection() {
        // 找到当前歌曲在 currentMediaItems 中的位置
        val currentItem = if (currentIndex in allMediaItems.indices) {
            allMediaItems[currentIndex]
        } else {
            null
        }

        val positionInList = currentMediaItems.indexOf(currentItem)
        playlistAdapter.setSelectedPosition(positionInList)

        // 滚动到当前播放项
        if (positionInList >= 0) {
            recyclerView.scrollToPosition(positionInList)
        }
    }

    // ==================== 进度更新 ====================

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressUpdateJob = lifecycleScope.launch {
            while (isActive) {
                if (isPlaying) {
                    val current = seekBar.progress
                    seekBar.progress = current + PROGRESS_UPDATE_INTERVAL.toInt()
                    tvCurrentTime.text = formatTime(seekBar.progress.toLong())
                }
                delay(PROGRESS_UPDATE_INTERVAL)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    // ==================== 广播接收器 ====================

    private fun registerPlayStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(MediaPlaybackService.ACTION_PLAY_STATE_CHANGED)
            addAction(MediaPlaybackService.ACTION_SONG_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(playStateReceiver, filter)
        }
    }

    private fun unregisterPlayStateReceiver() {
        try {
            unregisterReceiver(playStateReceiver)
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

    // ==================== 播放列表适配器 ====================

    /**
     * 音乐播放列表适配器
     */
    class MusicPlaylistAdapter(
        private val onItemClick: (MediaItem, Int) -> Unit
    ) : ListAdapter<MediaItem, MusicPlaylistAdapter.ViewHolder>(MusicDiffCallback()) {

        private var selectedPosition = -1

        fun setSelectedPosition(position: Int) {
            val oldPosition = selectedPosition
            selectedPosition = position
            if (oldPosition >= 0) notifyItemChanged(oldPosition)
            if (position >= 0) notifyItemChanged(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_music_playlist, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position), position == selectedPosition)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tv_music_title)
            private val tvArtist: TextView = itemView.findViewById(R.id.tv_music_artist)
            private val tvDuration: TextView = itemView.findViewById(R.id.tv_music_duration)
            private val ivPlaying: ImageView = itemView.findViewById(R.id.iv_playing_indicator)

            fun bind(item: MediaItem, isSelected: Boolean) {
                tvTitle.text = item.title
                tvArtist.text = item.artist.ifEmpty { "未知艺术家" }
                tvDuration.text = item.getFormattedDuration()

                if (isSelected) {
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                    )
                    tvTitle.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                    tvArtist.setTextColor(
                        ContextCompat.getColor(itemView.context, android.R.color.white)
                    )
                    ivPlaying.visibility = View.VISIBLE
                } else {
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, android.R.color.transparent)
                    )
                    tvTitle.setTextColor(
                        ContextCompat.getColor(itemView.context, android.R.color.white)
                    )
                    tvArtist.setTextColor(
                        ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                    )
                    ivPlaying.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(getItem(position), position)
                    }
                }
            }
        }

        class MusicDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem.id == newItem.id && oldItem.path == newItem.path
            }

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
