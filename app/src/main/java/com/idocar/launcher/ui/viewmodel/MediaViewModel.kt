package com.idocar.launcher.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.data.MediaPlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val items = mutableListOf<MediaItem>()
            withContext(Dispatchers.IO) {
                val resolver: ContentResolver = getApplication<Application>().contentResolver
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

                resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idColumn)
                        val title = cursor.getString(titleColumn) ?: "未知标题"
                        val artist = cursor.getString(artistColumn) ?: "未知艺术家"
                        val album = cursor.getString(albumColumn) ?: "未知专辑"
                        val duration = cursor.getLong(durationColumn)
                        // dataColumn 路径可用于后续播放
                        items.add(MediaItem(id, title, artist, album, duration))
                    }
                }
            }
            _mediaItems.value = items
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
