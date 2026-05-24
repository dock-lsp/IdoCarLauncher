package com.idocar.launcher.media

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.audioeffect.AudioEffect
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.R
import java.io.IOException
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 音乐播放后台服务
 * 使用 MediaPlayer 播放本地音频文件，支持播放队列管理、多种播放模式和前台通知
 */
class MediaPlaybackService : Service() {

    companion object {
        private const val TAG = "MediaPlaybackService"

        /** 通知 ID */
        const val NOTIFICATION_ID = 1001

        /** 播放状态广播 Action */
        const val ACTION_PLAY_STATE_CHANGED = "com.idocar.launcher.action.PLAY_STATE_CHANGED"
        const val ACTION_SONG_CHANGED = "com.idocar.launcher.action.SONG_CHANGED"

        /** 控制广播 Action */
        const val ACTION_PLAY = "com.idocar.launcher.action.PLAY"
        const val ACTION_PAUSE = "com.idocar.launcher.action.PAUSE"
        const val ACTION_NEXT = "com.idocar.launcher.action.NEXT"
        const val ACTION_PREVIOUS = "com.idocar.launcher.action.PREVIOUS"
        const val ACTION_STOP = "com.idocar.launcher.action.STOP"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.idocar.launcher.action.TOGGLE"

        /** 广播 Extra 键 */
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_MEDIA_ITEM = "media_item"
        const val EXTRA_POSITION = "position"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_QUEUE_POSITION = "queue_position"
        const val EXTRA_QUEUE_SIZE = "queue_size"

        /** 播放模式 */
        const val REPEAT_MODE_OFF = 0
        const val REPEAT_MODE_ALL = 1
        const val REPEAT_MODE_ONE = 2
    }

    /** 播放队列 */
    private val playQueue = CopyOnWriteArrayList<MediaItem>()

    /** 当前播放索引 */
    private var currentIndex = -1

    /** 播放模式 */
    @Volatile
    private var repeatMode = REPEAT_MODE_OFF

    /** 随机播放 */
    @Volatile
    private var isShuffle = false

    /** MediaPlayer 实例 */
    private var mediaPlayer: MediaPlayer? = null

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

    /** 进度更新 Runnable */
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    broadcastPlayState()
                    progressUpdateHandler?.postDelayed(this, 500)
                }
            }
        }
    }

    private var progressUpdateHandler: android.os.Handler? = null

    /**
     * 控制广播接收器
     */
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PLAY -> play()
                ACTION_PAUSE -> pause()
                ACTION_TOGGLE_PLAY_PAUSE -> togglePlayPause()
                ACTION_NEXT -> playNext()
                ACTION_PREVIOUS -> playPrevious()
                ACTION_STOP -> stopPlayback()
            }
        }
    }

    /**
     * 音频焦点变化监听器
     */
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 重新获得音频焦点
                hasAudioFocus = true
                if (pausedByFocusLoss) {
                    pausedByFocusLoss = false
                    play()
                }
                // 恢复音量
                mediaPlayer?.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久失去音频焦点，停止播放
                hasAudioFocus = false
                pausedByFocusLoss = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时失去音频焦点，暂停播放
                hasAudioFocus = false
                pausedByFocusLoss = true
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 暂时失去音频焦点，降低音量
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
        }
    }

    /**
     * MediaPlayer 完成监听器
     */
    private val completionListener = MediaPlayer.OnCompletionListener {
        // 当前歌曲播放完毕，播放下一首
        playNext()
    }

    /**
     * MediaPlayer 错误监听器
     */
    private val errorListener = MediaPlayer.OnErrorListener { _, what, extra ->
        Log.e(TAG, "MediaPlayer 错误: what=$what, extra=$extra")
        broadcastPlayState()
        true // 返回 true 表示已处理错误
    }

    /**
     * MediaPlayer 缓冲监听器
     */
    private val bufferListener = MediaPlayer.OnBufferingUpdateListener { _, percent ->
        // 缓冲进度更新
    }

    /**
     * MediaPlayer 准备完成监听器
     */
    private val preparedListener = MediaPlayer.OnPreparedListener { player ->
        player.start()
        updateNotification()
        broadcastPlayState()
        broadcastSongChanged()
        startProgressUpdate()
    }

    // ==================== Service 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服务创建")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        progressUpdateHandler = android.os.Handler(mainLooper)

        // 初始化 MediaSession
        initMediaSession()

        // 注册控制广播接收器
        registerControlReceiver()

        // 初始化 MediaPlayer
        initMediaPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服务启动")

        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_TOGGLE_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> {
                stopPlayback()
                stopForeground(true)
                stopSelf()
            }
        }

        // 处理播放队列设置
        intent?.getParcelableArrayListExtra<MediaItem>("queue")?.let { queue ->
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
        Log.d(TAG, "服务销毁")

        stopProgressUpdate()
        releaseMediaPlayer()
        releaseAudioFocus()
        mediaSession.release()
        unregisterControlReceiver()
        stopForeground(true)
    }

    // ==================== 初始化方法 ====================

    private fun initMediaPlayer() {
        releaseMediaPlayer()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnCompletionListener(completionListener)
            setOnErrorListener(errorListener)
            setOnBufferingUpdateListener(bufferListener)
            setOnPreparedListener(preparedListener)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MediaPlaybackService").apply {
            setCallback(mediaSessionCallback)
            setSessionToken(sessionToken)

            // 设置初始播放状态
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
                    )
                    .build()
            )
        }
    }

    private fun registerControlReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_TOGGLE_PLAY_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_STOP)
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
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                requestAudioFocus()
                player.start()
                updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                updateNotification()
                broadcastPlayState()
                startProgressUpdate()
            }
        }
    }

    /**
     * 暂停
     */
    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                updateNotification()
                broadcastPlayState()
                stopProgressUpdate()
            }
        }
    }

    /**
     * 切换播放/暂停
     */
    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    /**
     * 停止播放
     */
    fun stopPlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
        }
        updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        stopProgressUpdate()
        stopForeground(true)
        broadcastPlayState()
    }

    /**
     * 播放下一首
     */
    fun playNext() {
        if (playQueue.isEmpty()) return

        val nextIndex = if (isShuffle) {
            Random().nextInt(playQueue.size)
        } else {
            if (currentIndex < playQueue.size - 1) currentIndex + 1 else 0
        }

        playAt(nextIndex)
    }

    /**
     * 播放上一首
     */
    fun playPrevious() {
        if (playQueue.isEmpty()) return

        // 如果当前播放超过 3 秒，则重新播放当前歌曲
        mediaPlayer?.let {
            if (it.isPlaying && it.currentPosition > 3000) {
                it.seekTo(0)
                return
            }
        }

        val prevIndex = if (isShuffle) {
            Random().nextInt(playQueue.size)
        } else {
            if (currentIndex > 0) currentIndex - 1 else playQueue.size - 1
        }

        playAt(prevIndex)
    }

    /**
     * 播放指定位置的歌曲
     * @param index 播放队列中的索引
     */
    fun playAt(index: Int) {
        if (index < 0 || index >= playQueue.size) return

        currentIndex = index
        val item = playQueue[index]

        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(this, Uri.parse(item.path))
            mediaPlayer?.prepareAsync()

            // 更新 MediaSession 元数据
            updateMediaSessionMetadata(item)
        } catch (e: IOException) {
            Log.e(TAG, "播放文件失败: ${item.path}", e)
            // 播放失败，尝试播放下一首
            playNext()
        }

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification(item))
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

    /**
     * 跳转到指定位置
     * @param position 毫秒
     */
    fun seekTo(position: Long) {
        mediaPlayer?.let {
            if (it.isPlaying || it.currentPosition > 0) {
                it.seekTo(position.toInt())
                broadcastPlayState()
            }
        }
    }

    // ==================== 播放队列管理 ====================

    /**
     * 设置播放队列
     * @param queue 媒体项列表
     */
    fun setPlayQueue(queue: List<MediaItem>) {
        playQueue.clear()
        playQueue.addAll(queue)
        currentIndex = -1
    }

    /**
     * 添加到播放队列
     * @param item 媒体项
     */
    fun addToQueue(item: MediaItem) {
        playQueue.add(item)
    }

    /**
     * 添加到播放队列（指定位置）
     * @param item 媒体项
     * @param position 插入位置
     */
    fun addToQueue(item: MediaItem, position: Int) {
        if (position in 0..playQueue.size) {
            playQueue.add(position, item)
            if (position <= currentIndex) {
                currentIndex++
            }
        }
    }

    /**
     * 从播放队列中移除
     * @param position 要移除的位置
     */
    fun removeFromQueue(position: Int) {
        if (position in playQueue.indices) {
            playQueue.removeAt(position)
            when {
                position < currentIndex -> currentIndex--
                position == currentIndex -> {
                    // 当前播放的歌曲被移除
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

    // ==================== 播放模式 ====================

    /**
     * 设置重复模式
     * @param mode REPEAT_MODE_OFF / REPEAT_MODE_ALL / REPEAT_MODE_ONE
     */
    fun setRepeatMode(mode: Int) {
        repeatMode = mode
        updateMediaSessionRepeatMode()
    }

    /**
     * 获取重复模式
     */
    fun getRepeatMode(): Int = repeatMode

    /**
     * 切换重复模式
     */
    fun toggleRepeatMode(): Int {
        repeatMode = when (repeatMode) {
            REPEAT_MODE_OFF -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
            REPEAT_MODE_ONE -> REPEAT_MODE_OFF
            else -> REPEAT_MODE_OFF
        }
        updateMediaSessionRepeatMode()
        return repeatMode
    }

    /**
     * 设置随机播放
     */
    fun setShuffle(shuffle: Boolean) {
        isShuffle = shuffle
        updateMediaSessionShuffleMode()
    }

    /**
     * 获取随机播放状态
     */
    fun isShuffleEnabled(): Boolean = isShuffle

    /**
     * 切换随机播放
     */
    fun toggleShuffle(): Boolean {
        isShuffle = !isShuffle
        updateMediaSessionShuffleMode()
        return isShuffle
    }

    // ==================== 播放状态查询 ====================

    /**
     * 是否正在播放
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    /**
     * 获取当前播放位置
     */
    fun getCurrentPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0

    /**
     * 获取当前歌曲总时长
     */
    fun getDuration(): Long = mediaPlayer?.duration?.toLong() ?: 0

    // ==================== 音频焦点 ====================

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
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

        override fun onSetRepeatMode(repeatMode: Int) {
            setRepeatMode(repeatMode)
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            isShuffle = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
            updateMediaSessionShuffleMode()
        }
    }

    private fun updateMediaSessionMetadata(item: MediaItem) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, item.duration)
            .apply {
                item.albumArt?.let {
                    putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, it)
                }
            }
            .build()

        mediaSession.setMetadata(metadata)
    }

    private fun updateMediaSessionPlaybackState(state: Int) {
        val position = mediaPlayer?.currentPosition?.toLong() ?: 0
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SEEK_TO
                        or PlaybackStateCompat.ACTION_STOP
            )
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun updateMediaSessionRepeatMode() {
        val sessionRepeatMode = when (repeatMode) {
            REPEAT_MODE_OFF -> PlaybackStateCompat.REPEAT_MODE_NONE
            REPEAT_MODE_ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
            REPEAT_MODE_ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
            else -> PlaybackStateCompat.REPEAT_MODE_NONE
        }
        mediaSession.setRepeatMode(sessionRepeatMode)
    }

    private fun updateMediaSessionShuffleMode() {
        val sessionShuffleMode = if (isShuffle) {
            PlaybackStateCompat.SHUFFLE_MODE_ALL
        } else {
            PlaybackStateCompat.SHUFFLE_MODE_NONE
        }
        mediaSession.setShuffleMode(sessionShuffleMode)
    }

    // ==================== 通知栏 ====================

    private fun createNotification(item: MediaItem): Notification {
        val channelId = CarLauncherApp.CHANNEL_ID_MEDIA

        // 创建 PendingIntent
        val packageName = packageName
        val activityIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 上一首 PendingIntent
        val prevIntent = Intent(ACTION_PREVIOUS).apply { setPackage(packageName) }
        val prevPendingIntent = PendingIntent.getBroadcast(
            this, 0, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 播放/暂停 PendingIntent
        val playPauseIntent = Intent(
            if (mediaPlayer?.isPlaying == true) ACTION_PAUSE else ACTION_PLAY
        ).apply { setPackage(packageName) }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 下一首 PendingIntent
        val nextIntent = Intent(ACTION_NEXT).apply { setPackage(packageName) }
        val nextPendingIntent = PendingIntent.getBroadcast(
            this, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 停止 PendingIntent
        val stopIntent = Intent(ACTION_STOP).apply { setPackage(packageName) }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(item.title)
            .setContentText(item.artist)
            .setSubText(item.album)
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_music_note))
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_skip_previous, "上一首", prevPendingIntent)
            .addAction(
                if (mediaPlayer?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play,
                if (mediaPlayer?.isPlaying == true) "暂停" else "播放",
                playPausePendingIntent
            )
            .addAction(R.drawable.ic_skip_next, "下一首", nextPendingIntent)
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

    private fun updateNotification() {
        val item = getCurrentMediaItem() ?: return
        val notification = createNotification(item)

        if (mediaPlayer?.isPlaying == true) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    // ==================== 广播通知 ====================

    private fun broadcastPlayState() {
        val item = getCurrentMediaItem()
        val intent = Intent(ACTION_PLAY_STATE_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_IS_PLAYING, mediaPlayer?.isPlaying == true)
            putExtra(EXTRA_POSITION, getCurrentPosition())
            putExtra(EXTRA_DURATION, getDuration())
            putExtra(EXTRA_QUEUE_POSITION, currentIndex)
            putExtra(EXTRA_QUEUE_SIZE, playQueue.size)
            item?.let { putExtra(EXTRA_MEDIA_ITEM, it) }
        }
        sendBroadcast(intent)
    }

    private fun broadcastSongChanged() {
        val item = getCurrentMediaItem() ?: return
        val intent = Intent(ACTION_SONG_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_MEDIA_ITEM, item)
            putExtra(EXTRA_QUEUE_POSITION, currentIndex)
            putExtra(EXTRA_QUEUE_SIZE, playQueue.size)
        }
        sendBroadcast(intent)
    }

    // ==================== 进度更新 ====================

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressUpdateHandler?.postDelayed(progressUpdateRunnable, 500)
    }

    private fun stopProgressUpdate() {
        progressUpdateHandler?.removeCallbacks(progressUpdateRunnable)
    }

    // ==================== 资源释放 ====================

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            setOnCompletionListener(null)
            setOnErrorListener(null)
            setOnBufferingUpdateListener(null)
            setOnPreparedListener(null)
            try {
                if (isPlaying) stop()
            } catch (e: Exception) {
                // 忽略
            }
            release()
        }
        mediaPlayer = null
    }
}
