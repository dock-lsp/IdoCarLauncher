package com.idocar.launcher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.data.MediaPlaybackState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 媒体播放 ViewModel
 */
class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaController = (application as CarLauncherApp).mediaControllerManager

    private val _playbackState = MutableStateFlow(MediaPlaybackState())
    val playbackState: StateFlow<MediaPlaybackState> = _playbackState.asStateFlow()

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    init {
        viewModelScope.launch {
            mediaController.playbackState.collect { state ->
                _playbackState.value = state
            }
        }

        // 更新播放进度
        viewModelScope.launch {
            while (isActive) {
                if (_playbackState.value.isPlaying) {
                    _currentPosition.value += 1000
                }
                delay(1000)
            }
        }

        loadLocalMusic()
    }

    fun loadLocalMusic() {
        viewModelScope.launch {
            // 模拟加载本地音乐
            val mockItems = listOf(
                MediaItem("1", "歌曲 1", "艺术家 A", "专辑 X", 240000),
                MediaItem("2", "歌曲 2", "艺术家 B", "专辑 Y", 180000),
                MediaItem("3", "歌曲 3", "艺术家 C", "专辑 Z", 210000),
                MediaItem("4", "歌曲 4", "艺术家 A", "专辑 X", 195000),
                MediaItem("5", "歌曲 5", "艺术家 D", "专辑 W", 225000)
            )
            _mediaItems.value = mockItems
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            // 加载播放列表
            _mediaItems.value = emptyList()
        }
    }

    fun loadArtists() {
        viewModelScope.launch {
            // 加载艺术家
            _mediaItems.value = emptyList()
        }
    }

    fun loadAlbums() {
        viewModelScope.launch {
            // 加载专辑
            _mediaItems.value = emptyList()
        }
    }

    fun playMedia(item: MediaItem) {
        _playbackState.value = _playbackState.value.copy(
            title = item.title,
            artist = item.artist,
            album = item.album,
            duration = item.duration,
            isPlaying = true
        )
        _currentPosition.value = 0
        mediaController.play()
    }

    fun togglePlayPause() {
        mediaController.togglePlayPause()
    }

    fun skipToNext() {
        mediaController.skipToNext()
    }

    fun skipToPrevious() {
        mediaController.skipToPrevious()
    }

    fun seekTo(position: Long) {
        _currentPosition.value = position
        mediaController.seekTo(position)
    }

    fun toggleShuffle() {
        val newMode = if (_playbackState.value.shuffleMode == 0) 1 else 0
        mediaController.setShuffleMode(newMode)
        _playbackState.value = _playbackState.value.copy(shuffleMode = newMode)
    }

    fun toggleRepeat() {
        val newMode = if (_playbackState.value.repeatMode == 0) 1 else 0
        mediaController.setRepeatMode(newMode)
        _playbackState.value = _playbackState.value.copy(repeatMode = newMode)
    }

    data class MediaItem(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val duration: Long
    )
}
