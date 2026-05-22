package com.idocar.launcher.plugin

import android.content.Context
import android.view.View

/**
 * 车机启动器插件接口
 * 所有插件必须实现此接口
 */
interface CarLauncherPlugin {

    /**
     * 插件创建时调用
     */
    fun onCreate(context: Context)

    /**
     * 插件销毁时调用
     */
    fun onDestroy()

    /**
     * 获取插件 ID
     */
    fun getPluginId(): String

    /**
     * 获取插件名称
     */
    fun getPluginName(): String

    /**
     * 获取插件版本
     */
    fun getVersion(): String

    /**
     * 获取插件描述
     */
    fun getDescription(): String

    /**
     * 获取插件图标
     */
    fun getIcon(): Int?

    /**
     * 获取主界面视图（可选）
     */
    fun getMainView(): View? = null

    /**
     * 获取小部件视图（可选）
     */
    fun getWidgetView(): View? = null

    /**
     * 处理命令
     * @param command 命令字符串
     * @param params 参数
     * @return 处理结果
     */
    fun handleCommand(command: String, params: Map<String, Any>? = null): Any? = null

    /**
     * 获取插件提供的快捷方式
     */
    fun getShortcuts(): List<PluginShortcut> = emptyList()

    /**
     * 获取插件设置项
     */
    fun getSettings(): List<PluginSetting> = emptyList()

    /**
     * 当设置改变时调用
     */
    fun onSettingChanged(key: String, value: Any) {}

    /**
     * 当主界面显示时调用
     */
    fun onHomeShown() {}

    /**
     * 当主界面隐藏时调用
     */
    fun onHomeHidden() {}
}

/**
 * 插件快捷方式
 */
data class PluginShortcut(
    val id: String,
    val title: String,
    val iconResId: Int,
    val action: () -> Unit
)

/**
 * 插件设置项
 */
sealed class PluginSetting(
    val key: String,
    val title: String,
    val description: String? = null
) {
    class SwitchSetting(
        key: String,
        title: String,
        description: String? = null,
        val defaultValue: Boolean = false
    ) : PluginSetting(key, title, description)

    class TextSetting(
        key: String,
        title: String,
        description: String? = null,
        val defaultValue: String = "",
        val hint: String? = null
    ) : PluginSetting(key, title, description)

    class NumberSetting(
        key: String,
        title: String,
        description: String? = null,
        val defaultValue: Int = 0,
        val min: Int = 0,
        val max: Int = 100
    ) : PluginSetting(key, title, description)

    class SelectSetting(
        key: String,
        title: String,
        description: String? = null,
        val options: List<String>,
        val defaultIndex: Int = 0
    ) : PluginSetting(key, title, description)
}
