package com.idocar.launcher.plugin

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.idocar.launcher.data.PluginInfo
import dalvik.system.DexClassLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/**
 * 插件管理器
 * 管理插件的加载、启用/禁用和生命周期
 */
class PluginManager(private val context: Context) {

    private val _plugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val plugins: StateFlow<List<PluginInfo>> = _plugins.asStateFlow()

    private val loadedPlugins = mutableMapOf<String, CarLauncherPlugin>()
    private val pluginDir = File(context.getExternalFilesDir(null), "plugins")

    init {
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
        }
    }

    /**
     * 加载所有插件
     */
    fun loadPlugins() {
        val pluginList = mutableListOf<PluginInfo>()

        // 从插件目录加载
        pluginDir.listFiles()?.filter { it.extension == "apk" }?.forEach { apkFile ->
            try {
                val info = parsePluginApk(apkFile)
                if (info != null) {
                    pluginList.add(info)
                    if (info.isEnabled) {
                        loadPluginInstance(info)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load plugin: ${apkFile.name}", e)
            }
        }

        // 从 assets/plugins 加载内置插件
        loadBuiltInPlugins(pluginList)

        _plugins.value = pluginList
    }

    /**
     * 解析插件 APK
     */
    private fun parsePluginApk(apkFile: File): PluginInfo? {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_META_DATA)
            ?: return null

        val appInfo = info.applicationInfo
        appInfo?.sourceDir = apkFile.absolutePath
        appInfo?.publicSourceDir = apkFile.absolutePath

        val metaData = appInfo?.metaData
        val pluginId = metaData?.getString("plugin_id") ?: info.packageName
        val pluginName = pm.getApplicationLabel(appInfo).toString()
        val pluginVersion = info.versionName ?: "1.0"
        val entryClass = metaData?.getString("plugin_entry") ?: ""

        return PluginInfo(
            id = pluginId,
            name = pluginName,
            version = pluginVersion,
            description = "",
            author = metaData?.getString("plugin_author") ?: "",
            packageName = info.packageName,
            entryClass = entryClass,
            iconPath = null,
            isEnabled = true,
            isLoaded = false
        )
    }

    /**
     * 加载内置插件
     */
    private fun loadBuiltInPlugins(pluginList: MutableList<PluginInfo>) {
        try {
            val assets = context.assets
            // 检查 plugins 目录是否存在
            val builtInPlugins = try {
                assets.list("plugins")
            } catch (e: Exception) {
                null
            } ?: return

            for (pluginFile in builtInPlugins) {
                if (pluginFile.endsWith(".json")) {
                    try {
                        val json = assets.open("plugins/$pluginFile").bufferedReader().use { it.readText() }
                        val pluginInfo = parsePluginJson(json)
                        pluginList.add(pluginInfo)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load plugin config: $pluginFile", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load built-in plugins", e)
        }
    }

    /**
     * 解析插件 JSON 配置
     */
    private fun parsePluginJson(json: String): PluginInfo {
        val obj = JSONObject(json)
        return PluginInfo(
            id = obj.getString("id"),
            name = obj.getString("name"),
            version = obj.getString("version"),
            description = obj.optString("description", ""),
            author = obj.optString("author", ""),
            packageName = obj.optString("packageName", ""),
            entryClass = obj.optString("entryClass", ""),
            iconPath = obj.optString("iconPath", null),
            isEnabled = obj.optBoolean("enabled", true),
            isLoaded = false
        )
    }

    /**
     * 加载插件实例
     */
    private fun loadPluginInstance(pluginInfo: PluginInfo): CarLauncherPlugin? {
        if (loadedPlugins.containsKey(pluginInfo.id)) {
            return loadedPlugins[pluginInfo.id]
        }

        return try {
            val dexPath = pluginDir.listFiles()
                ?.find { it.name.startsWith(pluginInfo.packageName) && it.extension == "apk" }
                ?.absolutePath
                ?: return null

            val optimizedDir = context.cacheDir
            val parentLoader = context.classLoader

            val classLoader = DexClassLoader(dexPath, optimizedDir.absolutePath, null, parentLoader)
            val clazz = classLoader.loadClass(pluginInfo.entryClass)
            val plugin = clazz.getDeclaredConstructor().newInstance() as CarLauncherPlugin

            plugin.onCreate(context)
            loadedPlugins[pluginInfo.id] = plugin

            // 更新插件状态
            updatePluginStatus(pluginInfo.id, isLoaded = true)

            Log.d(TAG, "Plugin loaded: ${pluginInfo.name}")
            plugin
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load plugin instance: ${pluginInfo.name}", e)
            null
        }
    }

    /**
     * 启用/禁用插件
     */
    fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        val plugin = _plugins.value.find { it.id == pluginId } ?: return

        if (enabled) {
            if (!plugin.isLoaded) {
                loadPluginInstance(plugin)
            }
        } else {
            unloadPlugin(pluginId)
        }

        updatePluginStatus(pluginId, isEnabled = enabled)
    }

    /**
     * 卸载插件
     */
    private fun unloadPlugin(pluginId: String) {
        loadedPlugins[pluginId]?.onDestroy()
        loadedPlugins.remove(pluginId)
        updatePluginStatus(pluginId, isLoaded = false)
    }

    /**
     * 更新插件状态
     */
    private fun updatePluginStatus(pluginId: String, isEnabled: Boolean? = null, isLoaded: Boolean? = null) {
        _plugins.value = _plugins.value.map { plugin ->
            if (plugin.id == pluginId) {
                plugin.copy(
                    isEnabled = isEnabled ?: plugin.isEnabled,
                    isLoaded = isLoaded ?: plugin.isLoaded
                )
            } else {
                plugin
            }
        }
    }

    /**
     * 获取已加载的插件实例
     */
    fun getPlugin(pluginId: String): CarLauncherPlugin? {
        return loadedPlugins[pluginId]
    }

    /**
     * 获取所有已加载的插件
     */
    fun getLoadedPlugins(): List<CarLauncherPlugin> {
        return loadedPlugins.values.toList()
    }

    /**
     * 安装插件
     */
    fun installPlugin(apkFile: File): Boolean {
        return try {
            val targetFile = File(pluginDir, apkFile.name)
            apkFile.copyTo(targetFile, overwrite = true)
            loadPlugins()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install plugin", e)
            false
        }
    }

    /**
     * 卸载插件
     */
    fun uninstallPlugin(pluginId: String): Boolean {
        val plugin = _plugins.value.find { it.id == pluginId } ?: return false

        unloadPlugin(pluginId)

        // 删除 APK 文件
        pluginDir.listFiles()?.find { it.name.startsWith(plugin.packageName) }?.delete()

        // 更新列表
        _plugins.value = _plugins.value.filter { it.id != pluginId }

        return true
    }

    /**
     * 清理所有插件
     */
    fun cleanup() {
        loadedPlugins.values.forEach { it.onDestroy() }
        loadedPlugins.clear()
    }

    companion object {
        private const val TAG = "PluginManager"
    }
}
