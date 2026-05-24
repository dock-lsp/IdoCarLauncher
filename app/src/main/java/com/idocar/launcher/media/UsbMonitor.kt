package com.idocar.launcher.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * USB 设备监听器
 * 监听 USB 大容量存储设备的挂载/卸载事件，并提供 USB 路径查询功能
 */
class UsbMonitor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    /** USB 路径变化事件流 */
    private val _usbPathChanges = MutableSharedFlow<UsbEvent>(extraBufferCapacity = 10)
    val usbPathChanges: SharedFlow<UsbEvent> = _usbPathChanges.asSharedFlow()

    /** 当前已挂载的 USB 路径列表 */
    @Volatile
    private var mountedUsbPaths: MutableList<String> = mutableListOf()

    private val storageManager: StorageManager? by lazy {
        context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
    }

    private val usbManager: UsbManager? by lazy {
        context.getSystemService(Context.USB_SERVICE) as? UsbManager
    }

    /**
     * USB 事件类型
     */
    sealed class UsbEvent {
        /** USB 设备挂载 */
        data class UsbMounted(val path: String) : UsbEvent()
        /** USB 设备卸载 */
        data class UsbUnmounted(val path: String) : UsbEvent()
        /** USB 设备列表已更新 */
        data class UsbPathsUpdated(val paths: List<String>) : UsbEvent()
    }

    /**
     * USB 广播接收器
     * 监听 USB 设备挂载/卸载广播
     */
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_MEDIA_MOUNTED -> {
                    val path = intent.data?.path
                    if (path != null && isUsbPath(path)) {
                        mountedUsbPaths.add(path)
                        scope.launch {
                            _usbPathChanges.emit(UsbEvent.UsbMounted(path))
                            _usbPathChanges.emit(UsbEvent.UsbPathsUpdated(getUsbPaths()))
                        }
                    }
                }
                Intent.ACTION_MEDIA_UNMOUNTED -> {
                    val path = intent.data?.path
                    if (path != null) {
                        mountedUsbPaths.remove(path)
                        scope.launch {
                            _usbPathChanges.emit(UsbEvent.UsbUnmounted(path))
                            _usbPathChanges.emit(UsbEvent.UsbPathsUpdated(getUsbPaths()))
                        }
                    }
                }
                Intent.ACTION_MEDIA_EJECT -> {
                    val path = intent.data?.path
                    if (path != null) {
                        mountedUsbPaths.remove(path)
                        scope.launch {
                            _usbPathChanges.emit(UsbEvent.UsbUnmounted(path))
                            _usbPathChanges.emit(UsbEvent.UsbPathsUpdated(getUsbPaths()))
                        }
                    }
                }
                Intent.ACTION_MEDIA_BAD_REMOVAL -> {
                    val path = intent.data?.path
                    if (path != null) {
                        mountedUsbPaths.remove(path)
                        scope.launch {
                            _usbPathChanges.emit(UsbEvent.UsbUnmounted(path))
                            _usbPathChanges.emit(UsbEvent.UsbPathsUpdated(getUsbPaths()))
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    // USB 设备物理连接，延迟检查挂载路径
                    scope.launch {
                        kotlinx.coroutines.delay(1000)
                        refreshUsbPaths()
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        refreshUsbPaths()
                    }
                }
            }
        }
    }

    init {
        // 初始化时扫描已挂载的 USB 路径
        refreshUsbPaths()
        registerReceiver()
    }

    /**
     * 注册广播接收器
     */
    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addDataScheme("file")
        }
        context.registerReceiver(usbReceiver, filter)
    }

    /**
     * 刷新 USB 挂载路径列表
     */
    private fun refreshUsbPaths() {
        val paths = scanUsbMountPoints()
        mountedUsbPaths.clear()
        mountedUsbPaths.addAll(paths)
        scope.launch {
            _usbPathChanges.emit(UsbEvent.UsbPathsUpdated(paths))
        }
    }

    /**
     * 扫描所有可能的 USB 挂载点
     */
    private fun scanUsbMountPoints(): List<String> {
        val usbPaths = mutableListOf<String>()

        // 方法1：通过 StorageManager 获取外部存储卷
        storageManager?.let { sm ->
            try {
                val storageVolumes = sm.storageVolumes
                for (volume in storageVolumes) {
                    if (volume.isRemovable) {
                        volume.getPath(context)?.let { path ->
                            if (path != Environment.getExternalStorageDirectory().absolutePath) {
                                usbPaths.add(path)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 方法2：扫描常见的 USB 挂载目录
        val commonMountPoints = listOf(
            "/mnt/media_rw",
            "/mnt/usb",
            "/storage",
            "/mnt",
            "/run/media"
        )

        for (mountDir in commonMountPoints) {
            val dir = File(mountDir)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.canRead()) {
                        val absolutePath = file.absolutePath
                        // 排除内部存储
                        if (absolutePath != Environment.getExternalStorageDirectory().absolutePath &&
                            absolutePath != "/storage/emulated" &&
                            !absolutePath.startsWith("/storage/emulated/") &&
                            !usbPaths.contains(absolutePath)
                        ) {
                            // 检查是否为 USB 挂载点
                            if (isLikelyUsbMount(absolutePath)) {
                                usbPaths.add(absolutePath)
                            }
                        }
                    }
                }
            }
        }

        // 方法3：通过读取 /proc/mounts 查找 USB 设备
        try {
            val mountsFile = File("/proc/mounts")
            if (mountsFile.exists()) {
                mountsFile.readLines().forEach { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val device = parts[0]
                        val mountPoint = parts[1]
                        // USB 设备通常以 /dev/block/vold, /dev/bus/usb 等开头
                        if ((device.contains("vold") || device.contains("fuse") ||
                             device.contains("sd") || device.contains("usb")) &&
                            mountPoint.startsWith("/mnt") || mountPoint.startsWith("/storage")
                        ) {
                            if (mountPoint != Environment.getExternalStorageDirectory().absolutePath &&
                                !usbPaths.contains(mountPoint)
                            ) {
                                usbPaths.add(mountPoint)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return usbPaths.distinct()
    }

    /**
     * 判断路径是否可能是 USB 挂载点
     */
    private fun isLikelyUsbMount(path: String): Boolean {
        val usbIndicators = listOf("usb", "UDISK", "usbdisk", "usb_storage", "usbdrive")
        val pathLower = path.lowercase()
        return usbIndicators.any { pathLower.contains(it.lowercase()) }
    }

    /**
     * 判断给定路径是否为 USB 路径
     */
    private fun isUsbPath(path: String): Boolean {
        // 排除内部存储
        if (path == Environment.getExternalStorageDirectory().absolutePath) return false
        if (path.startsWith("/storage/emulated")) return false

        // 检查是否为可移动存储
        storageManager?.let { sm ->
            try {
                val storageVolumes = sm.storageVolumes
                for (volume in storageVolumes) {
                    val volumePath = volume.getPath(context)
                    if (volumePath == path && volume.isRemovable) {
                        return true
                    }
                }
            } catch (e: Exception) {
                // 忽略
            }
        }

        // 通过路径名称判断
        return isLikelyUsbMount(path)
    }

    /**
     * 获取当前所有 USB 挂载路径
     * @return USB 挂载路径列表
     */
    fun getUsbPaths(): List<String> {
        return mountedUsbPaths.toList()
    }

    /**
     * 判断是否有 USB 设备连接
     * @return true 表示至少有一个 USB 设备已挂载
     */
    fun isUsbConnected(): Boolean {
        return mountedUsbPaths.isNotEmpty()
    }

    /**
     * 获取所有 USB 设备的根目录文件列表
     * @return Map<USB路径, 根目录文件列表>
     */
    fun getUsbRootFiles(): Map<String, List<File>> {
        val result = mutableMapOf<String, List<File>>()
        for (path in mountedUsbPaths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                result[path] = dir.listFiles()?.sortedBy { it.name.lowercase() } ?: emptyList()
            }
        }
        return result
    }

    /**
     * 在所有 USB 设备中搜索指定扩展名的文件
     * @param extensions 文件扩展名列表（不含点号），如 ["mp3", "wav"]
     * @param recursive 是否递归搜索子目录
     * @return 匹配的文件列表
     */
    fun searchFilesOnUsb(extensions: List<String>, recursive: Boolean = true): List<File> {
        val result = mutableListOf<File>()
        for (path in mountedUsbPaths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                result.addAll(searchFiles(dir, extensions, recursive))
            }
        }
        return result
    }

    /**
     * 递归搜索文件
     */
    private fun searchFiles(dir: File, extensions: List<String>, recursive: Boolean): List<File> {
        val result = mutableListOf<File>()
        val files = dir.listFiles() ?: return result

        for (file in files) {
            if (file.isDirectory && recursive) {
                result.addAll(searchFiles(file, extensions, recursive))
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (ext in extensions) {
                    result.add(file)
                }
            }
        }
        return result
    }

    /**
     * 释放资源，注销广播接收器
     */
    fun release() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: IllegalArgumentException) {
            // 接收器未注册，忽略
        }
    }
}

/**
 * StorageVolume 扩展函数：获取挂载路径
 */
private fun StorageVolume.getPath(context: Context): String? {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            this.directory?.absolutePath
        } else {
            @Suppress("DEPRECATION")
            this.path
        }
    } catch (e: Exception) {
        null
    }
}
