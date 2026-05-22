package com.idocar.launcher.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaMetadataCompat
import com.idocar.launcher.data.MediaPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 媒体控制器管理器
 * 统一管理音乐播放控制
 */
class MediaControllerManager(private val context: Context) {

    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private var activeController: MediaControllerCompat? = null
    
    private val _playbackState = MutableStateFlow(MediaPlaybackState())
    val playbackState: StateFlow<MediaPlaybackState> = _playbackState.asStateFlow()
    
    private val _availableControllers = MutableStateFlow<List<MediaController>>(emptyList())
    val availableControllers: StateFlow<List<MediaController>> = _availableControllers.asStateFlow()

    private val sessionCallback = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
            _availableControllers.value = controllers ?: emptyList()
            controllers?.firstOrNull()?.let { connectToController(it) }
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            updatePlaybackState()
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            updatePlaybackState()
        }
    }

    init {
        // 监听活跃的媒体会话
        val componentName = ComponentName(context, MediaBrowserService::class.java)
        mediaSessionManager.addOnActiveSessionsChangedListener(sessionCallback, componentName)
        
        // 获取当前活跃的会话
        val controllers = mediaSessionManager.getActiveSessions(componentName)
        _availableControllers.value = controllers
        controllers.firstOrNull()?.let { connectToController(it) }
    }

    private fun connectToController(controller: MediaController) {
        activeController?.unregisterCallback(controllerCallback)
        
        val token = MediaSessionCompat.Token.fromToken(controller.sessionToken)
        activeController = MediaControllerCompat(context, token).apply {
            registerCallback(controllerCallback)
        }
        
        updatePlaybackState()
    }

    private fun updatePlaybackState() {
        val controller = activeController ?: return
        
        val playbackState = controller.playbackState
        val metadata = controller.metadata
        
        _playbackState.value = MediaPlaybackState(
            isPlaying = playbackState?.state == PlaybackStateCompat.STATE_PLAYING,
            title = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "",
            artist = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "",
            album = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM) ?: "",
            albumArt = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI),
            duration = metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0,
            position = playbackState?.position ?: 0,
            source = detectMediaSource(controller.packageName),
            repeatMode = controller.repeatMode,
            shuffleMode = controller.shuffleMode
        )
    }

    private fun detectMediaSource(packageName: String): MediaPlaybackState.MediaSource {
        return when (packageName) {
            "com.android.bluetooth" -> MediaPlaybackState.MediaSource.BLUETOOTH
            "com.google.android.apps.youtube.music",
            "com.spotify.music",
            "com.netease.cloudmusic" -> MediaPlaybackState.MediaSource.ONLINE
            else -> MediaPlaybackState.MediaSource.LOCAL
        }
    }

    // 播放控制方法
    fun play() {
        activeController?.transportControls?.play()
    }

    fun pause() {
        activeController?.transportControls?.pause()
    }

    fun togglePlayPause() {
        val state = _playbackState.value
        if (state.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun skipToNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(position: Long) {
        activeController?.transportControls?.seekTo(position)
    }

    fun setRepeatMode(repeatMode: Int) {
        activeController?.transportControls?.setRepeatMode(repeatMode)
    }

    fun setShuffleMode(shuffleMode: Int) {
        activeController?.transportControls?.setShuffleMode(shuffleMode)
    }

    // 音量控制
    fun setVolume(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    fun getCurrentVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    fun cleanup() {
        activeController?.unregisterCallback(controllerCallback)
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionCallback)
    }
}
