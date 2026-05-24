package com.idocar.launcher.media

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.R
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 视频播放服务
 * 使用 ExoPlayer (Media3) 管理视频播放，支持播放列表、进度保存/恢复和后台音频播放
 */
class VideoPlaybackService : Service() {

    companion object {
        private const val TAG = "VideoPlaybackService"

        /** 通知 ID */
        const val NOTIFICATION_ID = 1002

        /** 进度保存间隔（毫秒） */
        private const val SAVE_INTERVAL_MS = 3000L

        /** 快进/快退步长（毫秒） */
        const val SEEK_FORWARD_MS = 10_000L
        const val SEEK_BACKWARD_MS = 10_000L

        /** 播放状态广播 Action */
        const val ACTION_VIDEO_STATE_CHANGED = "com.idocar.launcher.action.VIDEO_STATE_CHANGED"
        const val ACTION_VIDEO_ITEM_CHANGED = "com.idocar.launcher.action.VIDEO_ITEM_CHANGED"

        /** 控制广播 Action */
        const val ACTION_VIDEO_PLAY = "com.idocar.launcher.action.VIDEO_PLAY"
        const val ACTION_VIDEO_PAUSE = "com.idocar.launcher.action.VIDEO_PAUSE"
        const val ACTION_VIDEO_NEXT = "com.idocar.launcher.action.VIDEO_NEXT"
        const val ACTION_VIDEO_PREVIOUS = "com.idocar.launcher.action.VIDEO_PREVIOUS"
        const val ACTION_VIDEO_STOP = "com.idocar.launcher.action.VIDEO_STOP"
        const val ACTION_VIDEO_SEEK_FORWARD = "com.idocar.launcher.action.VIDEO_SEEK_FORWARD"
        const val ACTION_VIDEO_SEEK_BACKWARD = "com.idocar.launcher.action.VIDEO_SEEK_BACKWARD"

        /** 广播 Extra 键 */
        const val EXTRA_VIDEO_IS_PLAYING = "video_is_playing"
        const val EXTRA_VIDEO_ITEM = "video_item"
        const val EXTRA_VIDEO_POSITION = "video_position"
        const val EXTRA_VIDEO_DURATION = "video_duration"
        const val EXTRA_VIDEO_QUEUE_POSITION = "video_queue_position"

        /** SharedPreferences 文件名 */
        private const val PREFS_NAME = "video_playback_progress"

        /** 进度保存前缀 */
        private const val PROGRESS_PREFIX = "progress_"
    }

    /** 视频播放队列 */
    private val playQueue = CopyOnWriteArrayList<MediaItem>()

    /** 当前播放索引 */
    @Volatile
    private var currentIndex = -1

    /** ExoPlayer 实例 */
    private var player: ExoPlayer? = null

    /** MediaSession */
    private lateinit var mediaSession: MediaSessionCompat

    /** AudioManager */
    private lateinit var audioManager: AudioManager

    /** AudioFocusRequest (API 26+) */
    private var audioFocusRequest: AudioFocusRequest? = null

    /** 是否拥有音频焦点 */
    private var hasAudioFocus = false

    /** 是否因失去音频焦点而暂停 */
    private var pausedByFocusLoss = false

    /** 进度保存 Handler */
    private var progressSaveHandler: android.os.Handler? = null

    /** SharedPreferences 用于保存进度 */
    private val progressPrefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 控制广播接收器
     */
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_VIDEO_PLAY -> play()
                ACTION_VIDEO_PAUSE -> pause()
                ACTION_VIDEO_NEXT -> playNext()
                ACTION_VIDEO_PREVIOUS -> playPrevious()
                ACTION_VIDEO_STOP -> stopPlayback()
                ACTION_VIDEO_SEEK_FORWARD -> seekForward()
                ACTION_VIDEO_SEEK_BACKWARD -> seekBackward()
            }
        }
    }

    /**
     * 音频焦点变化监听器
     */
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (pausedByFocusLoss) {
                    pausedByFocusLoss = false
                    play()
                }
                player?.volume = 1.0f
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                pausedByFocusLoss = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                pausedByFocusLoss = true
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player?.volume = 0.3f
            }
        }
    }

    /**
     * ExoPlayer 监听器
     */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    updateMediaSessionPlaybackState(
                        if (player?.isPlaying == true) PlaybackStateCompat.STATE_PLAYING
                        else PlaybackStateCompat.STATE_PAUSED
                    )
                    updateNotification()
                    broadcastVideoState()
                }
                Player.STATE_ENDED -> {
                    // 自动播放下一个
                    playNext()
                }
                Player.STATE_IDLE -> {
                    updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED)
                }
                Player.STATE_BUFFERING -> {
                    updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startProgressSave()
            } else {
                stopProgressSave()
                saveCurrentProgress()
            }
            updateNotification()
            broadcastVideoState()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "ExoPlayer 播放错误: ${error.message}", error)
            // 尝试播放下一个
            playNext()
        }
    }

    // ==================== Service 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "视频播放服务创建")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        progressSaveHandler = android.os.Handler(mainLooper)

        initMediaSession()
        registerControlReceiver()
        initPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "视频播放服务启动")

        when (intent?.action) {
            ACTION_VIDEO_PLAY -> play()
            ACTION_VIDEO_PAUSE -> pause()
            ACTION_VIDEO_NEXT -> playNext()
            ACTION_VIDEO_PREVIOUS -> playPrevious()
            ACTION_VIDEO_STOP -> {
                stopPlayback()
                stopForeground(true)
                stopSelf()
            }
            ACTION_VIDEO_SEEK_FORWARD -> seekForward()
            ACTION_VIDEO_SEEK_BACKWARD -> seekBackward()
        }

        // 处理播放队列设置
        @Suppress("DEPRECATION")
        intent?.getParcelableArrayListExtra<MediaItem>("video_queue")?.let { queue ->
            setPlayQueue(queue)
            val startPosition = intent.getIntExtra("start_position", 0)
            playAt(startPosition)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "视频播放服务销毁")

        saveCurrentProgress()
        stopProgressSave()
        releasePlayer()
        releaseAudioFocus()
        mediaSession.release()
        unregisterControlReceiver()
        stopForeground(true)
    }

    // ==================== 初始化方法 ====================

    private fun initPlayer() {
        releasePlayer()

        player = ExoPlayer.Builder(this).build().apply {
            // 设置音频属性
            val audioAttrs = Media3AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttrs, true)

            // 设置后台音频播放（视频只播放音频）
            setHandleAudioBecomingNoisy(true)

            addListener(playerListener)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "VideoPlaybackService").apply {
            setCallback(mediaSessionCallback)
            @Suppress("DEPRECATION")
            setSessionToken(sessionToken)

            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY
                                or PlaybackStateCompat.ACTION_PAUSE
                                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                or PlaybackStateCompat.ACTION_SEEK_TO
                                or PlaybackStateCompat.ACTION_STOP
                                or PlaybackStateCompat.ACTION_FAST_FORWARD
                                or PlaybackStateCompat.ACTION_REWIND
                    )
                    .build()
            )
        }
    }

    private fun registerControlReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_VIDEO_PLAY)
            addAction(ACTION_VIDEO_PAUSE)
            addAction(ACTION_VIDEO_NEXT)
            addAction(ACTION_VIDEO_PREVIOUS)
            addAction(ACTION_VIDEO_STOP)
            addAction(ACTION_VIDEO_SEEK_FORWARD)
            addAction(ACTION_VIDEO_SEEK_BACKWARD)
        }
        registerReceiver(controlReceiver, filter)
    }

    private fun unregisterControlReceiver() {
        try {
            unregisterReceiver(controlReceiver)
        } catch (e: IllegalArgumentException) {
            // 忽略
        }
    }

    // ==================== 播放控制 ====================

    /**
     * 播放
     */
    fun play() {
        player?.let {
            if (!it.isPlaying) {
                requestAudioFocus()
                it.play()
                updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                updateNotification()
                broadcastVideoState()
            }
        }
    }

    /**
     * 暂停
     */
    fun pause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                updateNotification()
                broadcastVideoState()
            }
        }
    }

    /**
     * 停止播放
     */
    fun stopPlayback() {
        player?.let {
            if (it.isPlaying || it.playbackState == Player.STATE_READY) {
                it.stop()
            }
        }
        saveCurrentProgress()
        updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        stopProgressSave()
        stopForeground(true)
        broadcastVideoState()
    }

    /**
     * 播放下一个视频
     */
    fun playNext() {
        if (playQueue.isEmpty()) return

        val nextIndex = if (currentIndex < playQueue.size - 1) {
            currentIndex + 1
        } else {
            0 // 循环到第一个
        }

        playAt(nextIndex)
    }

    /**
     * 播放上一个视频
     */
    fun playPrevious() {
        if (playQueue.isEmpty()) return

        // 如果当前播放超过 3 秒，则重新播放当前视频
        player?.let {
            if (it.isPlaying && it.currentPosition > 3000) {
                it.seekTo(0)
                return
            }
        }

        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            playQueue.size - 1
        }

        playAt(prevIndex)
    }

    /**
     * 快进
     */
    fun seekForward() {
        player?.let {
            val newPosition = it.currentPosition + SEEK_FORWARD_MS
            it.seekTo(minOf(newPosition, it.duration.coerceAtLeast(0)))
            broadcastVideoState()
        }
    }

    /**
     * 快退
     */
    fun seekBackward() {
        player?.let {
            val newPosition = it.currentPosition - SEEK_BACKWARD_MS
            it.seekTo(maxOf(newPosition, 0))
            broadcastVideoState()
        }
    }

    /**
     * 跳转到指定位置
     * @param position 毫秒
     */
    fun seekTo(position: Long) {
        player?.seekTo(position)
        broadcastVideoState()
    }

    /**
     * 播放指定位置的视频
     * @param index 播放队列中的索引
     */
    fun playAt(index: Int) {
        if (index < 0 || index >= playQueue.size) return

        // 保存当前进度
        saveCurrentProgress()

        currentIndex = index
        val mediaItem = playQueue[index]

        player?.let { exoPlayer ->
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            // 恢复播放进度
            val savedPosition = getSavedProgress(mediaItem.mediaId)
            if (savedPosition > 0) {
                exoPlayer.seekTo(savedPosition)
            }

            exoPlayer.playWhenReady = true
        }

        // 更新 MediaSession 元数据
        updateMediaSessionMetadata(index)

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification(index))
    }

    /**
     * 播放指定 MediaItem
     * @param item 要播放的媒体项
     */
    fun playItem(item: MediaItem) {
        val index = playQueue.indexOf(item)
        if (index >= 0) {
            playAt(index)
        } else {
            playQueue.add(item)
            playAt(playQueue.size - 1)
        }
    }

    // ==================== 播放队列管理 ====================

    /**
     * 设置播放队列
     */
    fun setPlayQueue(queue: List<MediaItem>) {
        playQueue.clear()
        playQueue.addAll(queue)
        currentIndex = -1
    }

    /**
     * 添加到播放队列
     */
    fun addToQueue(item: MediaItem) {
        playQueue.add(item)
    }

    /**
     * 从播放队列中移除
     */
    fun removeFromQueue(position: Int) {
        if (position in playQueue.indices) {
            playQueue.removeAt(position)
            when {
                position < currentIndex -> currentIndex--
                position == currentIndex -> {
                    if (playQueue.isEmpty()) {
                        stopPlayback()
                    } else {
                        if (currentIndex >= playQueue.size) {
                            currentIndex = 0
                        }
                        playAt(currentIndex)
                    }
                }
            }
        }
    }

    /**
     * 清空播放队列
     */
    fun clearQueue() {
        playQueue.clear()
        currentIndex = -1
        stopPlayback()
    }

    /**
     * 获取播放队列
     */
    fun getPlayQueue(): List<MediaItem> = playQueue.toList()

    /**
     * 获取当前播放索引
     */
    fun getCurrentIndex(): Int = currentIndex

    /**
     * 获取当前播放的媒体项
     */
    fun getCurrentMediaItem(): MediaItem? {
        return if (currentIndex in playQueue.indices) playQueue[currentIndex] else null
    }

    // ==================== 播放状态查询 ====================

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean = player?.isPlaying == true

    /**
     * 获取当前播放位置
     */
    fun getCurrentPosition(): Long = player?.currentPosition?.toLong() ?: 0

    /**
     * 获取当前视频总时长
     */
    fun getDuration(): Long = player?.duration?.toLong() ?: 0

    /**
     * 获取缓冲百分比
     */
    fun getBufferedPercentage(): Int = player?.bufferedPercentage ?: 0

    // ==================== 进度保存/恢复 ====================

    /**
     * 保存当前视频的播放进度
     */
    private fun saveCurrentProgress() {
        val item = getCurrentMediaItem() ?: return
        val position = getCurrentPosition()
        val duration = getDuration()

        // 只在有效进度时保存（进度大于 5 秒且小于总时长的 95%）
        if (position > 5000 && duration > 0 && position < duration * 0.95) {
            progressPrefs.edit()
                .putLong(PROGRESS_PREFIX + item.mediaId, position)
                .apply()
            Log.d(TAG, "保存进度: ${item.mediaId} -> ${position}ms")
        }
    }

    /**
     * 获取已保存的播放进度
     * @param mediaId 媒体 ID
     * @return 保存的进度（毫秒），未保存返回 0
     */
    fun getSavedProgress(mediaId: String): Long {
        return progressPrefs.getLong(PROGRESS_PREFIX + mediaId, 0)
    }

    /**
     * 恢复指定视频的播放进度
     * @param mediaId 媒体 ID
     * @return 保存的进度（毫秒）
     */
    fun restoreProgress(mediaId: String): Long {
        val position = getSavedProgress(mediaId)
        if (position > 0) {
            player?.seekTo(position)
        }
        return position
    }

    /**
     * 清除指定视频的播放进度
     */
    fun clearSavedProgress(mediaId: String) {
        progressPrefs.edit().remove(PROGRESS_PREFIX + mediaId).apply()
    }

    /**
     * 清除所有保存的播放进度
     */
    fun clearAllProgress() {
        progressPrefs.edit().clear().apply()
    }

    /**
     * 启动定时保存进度
     */
    private fun startProgressSave() {
        stopProgressSave()
        progressSaveHandler?.postDelayed(object : Runnable {
            override fun run() {
                saveCurrentProgress()
                progressSaveHandler?.postDelayed(this, SAVE_INTERVAL_MS)
            }
        }, SAVE_INTERVAL_MS)
    }

    /**
     * 停止定时保存进度
     */
    private fun stopProgressSave() {
        progressSaveHandler?.removeCallbacksAndMessages(null)
    }

    // ==================== 音频焦点 ====================

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioFocusRequest = focusRequest
            val result = audioManager.requestAudioFocus(focusRequest)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasAudioFocus
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasAudioFocus
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    // ==================== MediaSession ====================

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            play()
        }

        override fun onPause() {
            pause()
        }

        override fun onSkipToNext() {
            playNext()
        }

        override fun onSkipToPrevious() {
            playPrevious()
        }

        override fun onStop() {
            stopPlayback()
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            seekTo(pos)
        }

        override fun onFastForward() {
            seekForward()
        }

        override fun onRewind() {
            seekBackward()
        }
    }

    private fun updateMediaSessionMetadata(index: Int) {
        if (index !in playQueue.indices) return

        val item = playQueue[index]
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.mediaMetadata.title?.toString() ?: "")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.mediaMetadata.artist?.toString() ?: "")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
            .build()

        mediaSession.setMetadata(metadata)
    }

    private fun updateMediaSessionPlaybackState(state: Int) {
        val position = getCurrentPosition()
        val speed = player?.playbackParameters?.speed ?: 1.0f
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, speed)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SEEK_TO
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_FAST_FORWARD
                        or PlaybackStateCompat.ACTION_REWIND
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    // ==================== 通知栏 ====================

    private fun createNotification(index: Int): Notification {
        val channelId = CarLauncherApp.CHANNEL_ID_MEDIA

        if (index !in playQueue.indices) {
            return createEmptyNotification(channelId)
        }

        val item = playQueue[index]
        val title = item.mediaMetadata.title?.toString() ?: "未知视频"

        // PendingIntent
        val packageName = packageName
        val activityIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(ACTION_VIDEO_PREVIOUS).apply { setPackage(packageName) }
        val prevPendingIntent = PendingIntent.getBroadcast(
            this, 0, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(
            if (player?.isPlaying == true) ACTION_VIDEO_PAUSE else ACTION_VIDEO_PLAY
        ).apply { setPackage(packageName) }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(ACTION_VIDEO_NEXT).apply { setPackage(packageName) }
        val nextPendingIntent = PendingIntent.getBroadcast(
            this, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(ACTION_VIDEO_STOP).apply { setPackage(packageName) }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText("正在播放视频")
            .setSmallIcon(R.drawable.ic_video)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_skip_previous, "上一个", prevPendingIntent)
            .addAction(
                if (player?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play,
                if (player?.isPlaying == true) "暂停" else "播放",
                playPausePendingIntent
            )
            .addAction(R.drawable.ic_skip_next, "下一个", nextPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createEmptyNotification(channelId: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("视频播放器")
            .setContentText("没有正在播放的视频")
            .setSmallIcon(R.drawable.ic_video)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        if (currentIndex in playQueue.indices) {
            val notification = createNotification(currentIndex)
            if (player?.isPlaying == true) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    // ==================== 广播通知 ====================

    private fun broadcastVideoState() {
        val intent = Intent(ACTION_VIDEO_STATE_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_VIDEO_IS_PLAYING, player?.isPlaying == true)
            putExtra(EXTRA_VIDEO_POSITION, getCurrentPosition())
            putExtra(EXTRA_VIDEO_DURATION, getDuration())
            putExtra(EXTRA_VIDEO_QUEUE_POSITION, currentIndex)
        }
        sendBroadcast(intent)
    }

    private fun broadcastVideoItemChanged() {
        val item = getCurrentMediaItem() ?: return
        val intent = Intent(ACTION_VIDEO_ITEM_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_VIDEO_QUEUE_POSITION, currentIndex)
        }
        sendBroadcast(intent)
    }

    // ==================== 资源释放 ====================

    private fun releasePlayer() {
        player?.let {
            it.removeListener(playerListener)
            it.release()
        }
        player = null
    }
}
