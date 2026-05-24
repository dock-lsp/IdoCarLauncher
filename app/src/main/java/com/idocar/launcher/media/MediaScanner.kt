package com.idocar.launcher.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 媒体文件扫描器
 * 扫描本地存储和 USB 设备中的音频/视频文件
 * 使用 MediaStore 查询系统媒体库，同时支持直接扫描文件系统
 */
class MediaScanner(private val context: Context) {

    companion object {
        private const val TAG = "MediaScanner"

        /** 支持的音频文件扩展名 */
        val AUDIO_EXTENSIONS = listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus")

        /** 支持的视频文件扩展名 */
        val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "avi", "3gp", "wmv", "flv", "mov", "webm", "ts", "rmvb")

        /** 媒体库扫描类型 */
        const val SCAN_TYPE_ALL = 0
        const val SCAN_TYPE_AUDIO = 1
        const val SCAN_TYPE_VIDEO = 2
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    /** 所有媒体文件列表 */
    private val _allMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val allMediaItems: StateFlow<List<MediaItem>> = _allMediaItems.asStateFlow()

    /** 音频文件列表 */
    private val _audioItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val audioItems: StateFlow<List<MediaItem>> = _audioItems.asStateFlow()

    /** 视频文件列表 */
    private val _videoItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val videoItems: StateFlow<List<MediaItem>> = _videoItems.asStateFlow()

    /** USB 媒体文件列表 */
    private val _usbMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val usbMediaItems: StateFlow<List<MediaItem>> = _usbMediaItems.asStateFlow()

    /** 扫描状态 */
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /** USB 监听器 */
    private val usbMonitor = UsbMonitor(context)

    init {
        // 监听 USB 设备变化，自动扫描 USB 媒体
        scope.launch {
            usbMonitor.usbPathChanges.collect { event ->
                when (event) {
                    is UsbMonitor.UsbEvent.UsbMounted -> {
                        Log.d(TAG, "USB 已挂载: ${event.path}")
                        scanUsbMedia(event.path)
                    }
                    is UsbMonitor.UsbEvent.UsbUnmounted -> {
                        Log.d(TAG, "USB 已卸载: ${event.path}")
                        removeUsbMedia(event.path)
                    }
                    is UsbMonitor.UsbEvent.UsbPathsUpdated -> {
                        Log.d(TAG, "USB 路径已更新: ${event.paths}")
                        // 重新扫描所有 USB 媒体
                        refreshUsbMedia()
                    }
                }
            }
        }

        // 初始扫描
        scope.launch {
            scanAllMedia()
        }
    }

    /**
     * 扫描所有媒体文件（本地 + USB）
     * @return 所有媒体文件列表
     */
    suspend fun scanAllMedia(): List<MediaItem> {
        _isScanning.value = true
        try {
            val localAudio = scanLocalAudio()
            val localVideo = scanLocalVideo()

            val allItems = mutableListOf<MediaItem>()
            allItems.addAll(localAudio)
            allItems.addAll(localVideo)

            // 扫描 USB 媒体
            val usbPaths = usbMonitor.getUsbPaths()
            for (path in usbPaths) {
                val usbItems = scanUsbMediaInternal(path)
                allItems.addAll(usbItems)
            }

            _allMediaItems.value = allItems
            _audioItems.value = allItems.filter { it.type == MediaItem.MediaType.AUDIO }
            _videoItems.value = allItems.filter { it.type == MediaItem.MediaType.VIDEO }

            Log.d(TAG, "扫描完成: 共 ${allItems.size} 个媒体文件 (音频: ${localAudio.size}, 视频: ${localVideo.size})")
            return allItems
        } catch (e: Exception) {
            Log.e(TAG, "扫描媒体文件失败", e)
            return emptyList()
        } finally {
            _isScanning.value = false
        }
    }

    /**
     * 扫描本地音频文件
     * 通过 MediaStore.Audio 查询系统媒体库
     * @return 音频文件列表
     */
    suspend fun scanLocalAudio(): List<MediaItem> {
        return with(Dispatchers.IO) {
            val items = mutableListOf<MediaItem>()

            try {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATE_ADDED
                )

                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE LOCALIZED ASC"

                context.contentResolver.query(
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
                    val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: ""
                        val artist = cursor.getString(artistColumn) ?: ""
                        val album = cursor.getString(albumColumn) ?: ""
                        val duration = cursor.getLong(durationColumn)
                        val path = cursor.getString(dataColumn) ?: ""
                        val albumId = cursor.getLong(albumIdColumn)
                        val size = cursor.getLong(sizeColumn)
                        val dateAdded = cursor.getLong(dateAddedColumn)

                        if (path.isNotEmpty() && File(path).exists()) {
                            val albumArtUri = ContentUris.withAppendedId(
                                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                albumId
                            ).toString()

                            items.add(
                                MediaItem(
                                    id = id,
                                    title = title.ifEmpty { File(path).nameWithoutExtension },
                                    artist = artist,
                                    album = album,
                                    duration = duration,
                                    path = path,
                                    albumArt = albumArtUri,
                                    type = MediaItem.MediaType.AUDIO,
                                    size = size,
                                    dateAdded = dateAdded
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "扫描本地音频失败", e)
            }

            items
        }
    }

    /**
     * 扫描本地视频文件
     * 通过 MediaStore.Video 查询系统媒体库
     * @return 视频文件列表
     */
    suspend fun scanLocalVideo(): List<MediaItem> {
        return with(Dispatchers.IO) {
            val items = mutableListOf<MediaItem>()

            try {
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.ARTIST,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DATE_ADDED
                )

                val sortOrder = "${MediaStore.Video.Media.TITLE} COLLATE LOCALIZED ASC"

                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: ""
                        val artist = cursor.getString(artistColumn) ?: ""
                        val duration = cursor.getLong(durationColumn)
                        val path = cursor.getString(dataColumn) ?: ""
                        val size = cursor.getLong(sizeColumn)
                        val dateAdded = cursor.getLong(dateAddedColumn)

                        if (path.isNotEmpty() && File(path).exists()) {
                            // 视频缩略图
                            val albumArtUri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            ).toString()

                            items.add(
                                MediaItem(
                                    id = id,
                                    title = title.ifEmpty { File(path).nameWithoutExtension },
                                    artist = artist,
                                    duration = duration,
                                    path = path,
                                    albumArt = albumArtUri,
                                    type = MediaItem.MediaType.VIDEO,
                                    size = size,
                                    dateAdded = dateAdded
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "扫描本地视频失败", e)
            }

            items
        }
    }

    /**
     * 扫描指定 USB 路径下的媒体文件
     * @param usbPath USB 设备挂载路径
     * @return USB 媒体文件列表
     */
    suspend fun scanUsbMedia(usbPath: String): List<MediaItem> {
        _isScanning.value = true
        try {
            val items = scanUsbMediaInternal(usbPath)

            // 更新 USB 媒体列表
            val currentUsbItems = _usbMediaItems.value.toMutableList()
            // 移除该路径下的旧数据
            currentUsbItems.removeAll { it.path.startsWith(usbPath) }
            currentUsbItems.addAll(items)
            _usbMediaItems.value = currentUsbItems

            // 更新全部媒体列表
            refreshAllMediaItems()

            Log.d(TAG, "USB 媒体扫描完成 ($usbPath): ${items.size} 个文件")
            return items
        } catch (e: Exception) {
            Log.e(TAG, "扫描 USB 媒体失败: $usbPath", e)
            return emptyList()
        } finally {
            _isScanning.value = false
        }
    }

    /**
     * 内部方法：扫描 USB 路径下的媒体文件
     */
    private suspend fun scanUsbMediaInternal(usbPath: String): List<MediaItem> {
        return with(Dispatchers.IO) {
            val items = mutableListOf<MediaItem>()
            val usbDir = File(usbPath)

            if (!usbDir.exists() || !usbDir.isDirectory || !usbDir.canRead()) {
                return@with items
            }

            // 先尝试通过 MediaStore 查询
            val mediaStoreItems = scanUsbFromMediaStore(usbPath)
            if (mediaStoreItems.isNotEmpty()) {
                items.addAll(mediaStoreItems)
            }

            // 再通过文件系统扫描（补充 MediaStore 未索引的文件）
            val fileSystemItems = scanDirectory(usbDir, AUDIO_EXTENSIONS + VIDEO_EXTENSIONS)
            val mediaStorePaths = mediaStoreItems.map { it.path }.toSet()

            for (file in fileSystemItems) {
                if (file.absolutePath !in mediaStorePaths) {
                    val ext = file.extension.lowercase()
                    val type = if (ext in AUDIO_EXTENSIONS) {
                        MediaItem.MediaType.AUDIO
                    } else {
                        MediaItem.MediaType.VIDEO
                    }

                    items.add(
                        MediaItem(
                            id = file.hashCode().toLong(),
                            title = file.nameWithoutExtension,
                            path = file.absolutePath,
                            type = type,
                            size = file.length(),
                            dateAdded = file.lastModified() / 1000
                        )
                    )
                }
            }

            items
        }
    }

    /**
     * 通过 MediaStore 查询指定路径下的媒体文件
     */
    private fun scanUsbFromMediaStore(usbPath: String): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        try {
            // 查询音频
            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED
            )

            val audioSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
            val audioSelectionArgs = arrayOf("$usbPath/%")

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection,
                audioSelection,
                audioSelectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: ""
                    val artist = cursor.getString(artistColumn) ?: ""
                    val album = cursor.getString(albumColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val albumId = cursor.getLong(albumIdColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)

                    if (path.isNotEmpty() && File(path).exists()) {
                        val albumArtUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                            albumId
                        ).toString()

                        items.add(
                            MediaItem(
                                id = id,
                                title = title.ifEmpty { File(path).nameWithoutExtension },
                                artist = artist,
                                album = album,
                                duration = duration,
                                path = path,
                                albumArt = albumArtUri,
                                type = MediaItem.MediaType.AUDIO,
                                size = size,
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
            }

            // 查询视频
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.ARTIST,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            )

            val videoSelection = "${MediaStore.Video.Media.DATA} LIKE ?"
            val videoSelectionArgs = arrayOf("$usbPath/%")

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                videoSelection,
                videoSelectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: ""
                    val artist = cursor.getString(artistColumn) ?: ""
                    val duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)

                    if (path.isNotEmpty() && File(path).exists()) {
                        val albumArtUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        items.add(
                            MediaItem(
                                id = id,
                                title = title.ifEmpty { File(path).nameWithoutExtension },
                                artist = artist,
                                duration = duration,
                                path = path,
                                albumArt = albumArtUri,
                                type = MediaItem.MediaType.VIDEO,
                                size = size,
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "通过 MediaStore 扫描 USB 媒体失败", e)
        }

        return items
    }

    /**
     * 递归扫描目录下的指定类型文件
     * @param dir 要扫描的目录
     * @param extensions 文件扩展名列表
     * @param maxDepth 最大递归深度，默认 5 层
     * @return 匹配的文件列表
     */
    private fun scanDirectory(dir: File, extensions: List<String>, maxDepth: Int = 5): List<File> {
        val result = mutableListOf<File>()
        scanDirectoryInternal(dir, extensions, maxDepth, 0, result)
        return result
    }

    private fun scanDirectoryInternal(
        dir: File,
        extensions: List<String>,
        maxDepth: Int,
        currentDepth: Int,
        result: MutableList<File>
    ) {
        if (currentDepth >= maxDepth) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                // 跳过隐藏目录和常见的非媒体目录
                if (!file.name.startsWith(".")) {
                    scanDirectoryInternal(file, extensions, maxDepth, currentDepth + 1, result)
                }
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in extensions) {
                    result.add(file)
                }
            }
        }
    }

    /**
     * 刷新 USB 媒体列表
     */
    private suspend fun refreshUsbMedia() {
        val usbPaths = usbMonitor.getUsbPaths()
        val allUsbItems = mutableListOf<MediaItem>()
        for (path in usbPaths) {
            allUsbItems.addAll(scanUsbMediaInternal(path))
        }
        _usbMediaItems.value = allUsbItems
        refreshAllMediaItems()
    }

    /**
     * 移除指定 USB 路径下的媒体
     */
    private fun removeUsbMedia(usbPath: String) {
        val currentUsbItems = _usbMediaItems.value.toMutableList()
        currentUsbItems.removeAll { it.path.startsWith(usbPath) }
        _usbMediaItems.value = currentUsbItems
        refreshAllMediaItems()
    }

    /**
     * 刷新所有媒体列表（合并本地和 USB）
     */
    private fun refreshAllMediaItems() {
        val localAudio = _audioItems.value
        val localVideo = _videoItems.value
        val usbItems = _usbMediaItems.value

        val allItems = mutableListOf<MediaItem>()
        allItems.addAll(localAudio)
        allItems.addAll(localVideo)
        allItems.addAll(usbItems)

        _allMediaItems.value = allItems
    }

    /**
     * 通过 ContentResolver 扫描指定路径的媒体文件
     * 请求 MediaStore 重新扫描指定文件或目录
     * @param path 要扫描的文件或目录路径
     */
    fun requestMediaScan(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) return

            val uri = android.net.Uri.fromFile(file)
            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
            context.sendBroadcast(scanIntent)
        } catch (e: Exception) {
            Log.e(TAG, "请求媒体扫描失败: $path", e)
        }
    }

    /**
     * 按关键词搜索媒体文件
     * @param query 搜索关键词
     * @param type 媒体类型过滤，null 表示搜索全部
     * @return 匹配的媒体文件列表
     */
    fun searchMedia(query: String, type: MediaItem.MediaType? = null): List<MediaItem> {
        val allItems = _allMediaItems.value
        val queryLower = query.lowercase()

        return allItems.filter { item ->
            val matchesType = type == null || item.type == type
            val matchesQuery = item.title.lowercase().contains(queryLower) ||
                    item.artist.lowercase().contains(queryLower) ||
                    item.album.lowercase().contains(queryLower)
            matchesType && matchesQuery
        }
    }

    /**
     * 获取所有音频文件
     * @return 音频文件列表
     */
    fun getAudioItems(): List<MediaItem> {
        return _allMediaItems.value.filter { it.type == MediaItem.MediaType.AUDIO }
    }

    /**
     * 获取所有视频文件
     * @return 视频文件列表
     */
    fun getVideoItems(): List<MediaItem> {
        return _allMediaItems.value.filter { it.type == MediaItem.MediaType.VIDEO }
    }

    /**
     * 获取 USB 媒体文件
     * @return USB 媒体文件列表
     */
    fun getUsbMediaItems(): List<MediaItem> {
        return _usbMediaItems.value
    }

    /**
     * 获取所有 USB 路径
     * @return USB 路径列表
     */
    fun getUsbPaths(): List<String> {
        return usbMonitor.getUsbPaths()
    }

    /**
     * 判断 USB 是否连接
     * @return true 表示有 USB 设备已挂载
     */
    fun isUsbConnected(): Boolean {
        return usbMonitor.isUsbConnected()
    }

    /**
     * 释放资源
     */
    fun release() {
        usbMonitor.release()
    }
}
