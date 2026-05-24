package com.idocar.launcher.plugin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.util.DisplayUtils

/**
 * 插件设置对话框
 * 用于配置插件的各项设置
 */
class PluginSettingsDialog : DialogFragment() {

    companion object {
        private const val ARG_PLUGIN_ID = "plugin_id"

        fun newInstance(pluginId: String, settings: List<PluginSetting>): PluginSettingsDialog {
            return PluginSettingsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLUGIN_ID, pluginId)
                }
                this.settings = settings
            }
        }
    }

    private var settings: List<PluginSetting> = emptyList()
    private lateinit var pluginId: String
    private lateinit var plugin: CarLauncherPlugin
    private val settingViews = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pluginId = arguments?.getString(ARG_PLUGIN_ID)
            ?: throw IllegalArgumentException("Plugin ID is required")
        
        plugin = (requireActivity().application as CarLauncherApp)
            .pluginManager.getPlugin(pluginId)
            ?: throw IllegalStateException("Plugin not found: $pluginId")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val container = createSettingsContainer()
        
        return AlertDialog.Builder(requireContext())
            .setTitle("${plugin.getPluginName()} 设置")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                saveSettings()
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("重置") { _, _ ->
                resetSettings()
            }
            .create()
    }

    private fun createSettingsContainer(): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DisplayUtils.dpToPx(requireContext(), 16f))
        }

        for (setting in settings) {
            val settingView = createSettingView(setting)
            container.addView(settingView)
        }

        return container
    }

    private fun createSettingView(setting: PluginSetting): View {
        return when (setting) {
            is PluginSetting.SwitchSetting -> createSwitchSetting(setting)
            is PluginSetting.TextSetting -> createTextSetting(setting)
            is PluginSetting.NumberSetting -> createNumberSetting(setting)
            is PluginSetting.SelectSetting -> createSelectSetting(setting)
        }
    }

    private fun createSwitchSetting(setting: PluginSetting.SwitchSetting): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, DisplayUtils.dpToPx(requireContext(), 8f), 0, DisplayUtils.dpToPx(requireContext(), 8f))
        }

        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleText = TextView(requireContext()).apply {
            text = setting.title
            textSize = 16f
        }
        textContainer.addView(titleText)

        setting.description?.let { desc ->
            val descText = TextView(requireContext()).apply {
                text = desc
                textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
            }
            textContainer.addView(descText)
        }

        layout.addView(textContainer)

        val switch = MaterialSwitch(requireContext()).apply {
            isChecked = setting.defaultValue
        }
        settingViews[setting.key] = switch
        layout.addView(switch)

        return layout
    }

    private fun createTextSetting(setting: PluginSetting.TextSetting): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, DisplayUtils.dpToPx(requireContext(), 8f), 0, DisplayUtils.dpToPx(requireContext(), 8f))
        }

        val textInputLayout = TextInputLayout(requireContext()).apply {
            hint = setting.title
        }

        val editText = EditText(requireContext()).apply {
            setText(setting.defaultValue)
            setting.hint?.let { hint = it }
        }
        settingViews[setting.key] = editText

        textInputLayout.addView(editText)
        container.addView(textInputLayout)

        setting.description?.let { desc ->
            val descText = TextView(requireContext()).apply {
                text = desc
                textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
            }
            container.addView(descText)
        }

        return container
    }

    private fun createNumberSetting(setting: PluginSetting.NumberSetting): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, DisplayUtils.dpToPx(requireContext(), 8f), 0, DisplayUtils.dpToPx(requireContext(), 8f))
        }

        val titleText = TextView(requireContext()).apply {
            text = setting.title
            textSize = 16f
        }
        container.addView(titleText)

        val valueText = TextView(requireContext()).apply {
            text = setting.defaultValue.toString()
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
        }
        container.addView(valueText)

        val seekBar = SeekBar(requireContext()).apply {
            max = setting.max - setting.min
            progress = setting.defaultValue - setting.min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    valueText.text = (progress + setting.min).toString()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        settingViews[setting.key] = seekBar
        container.addView(seekBar)

        setting.description?.let { desc ->
            val descText = TextView(requireContext()).apply {
                text = desc
                textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
            }
            container.addView(descText)
        }

        return container
    }

    private fun createSelectSetting(setting: PluginSetting.SelectSetting): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, DisplayUtils.dpToPx(requireContext(), 8f), 0, DisplayUtils.dpToPx(requireContext(), 8f))
        }

        val titleText = TextView(requireContext()).apply {
            text = setting.title
            textSize = 16f
        }
        container.addView(titleText)

        val selectedText = TextView(requireContext()).apply {
            text = setting.options.getOrElse(setting.defaultIndex) { "" }
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showSelectOptions(setting, this)
            }
        }
        settingViews[setting.key] = selectedText
        container.addView(selectedText)

        setting.description?.let { desc ->
            val descText = TextView(requireContext()).apply {
                text = desc
                textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
            }
            container.addView(descText)
        }

        return container
    }

    private fun showSelectOptions(setting: PluginSetting.SelectSetting, textView: TextView) {
        AlertDialog.Builder(requireContext())
            .setTitle(setting.title)
            .setItems(setting.options.toTypedArray()) { _, which ->
                textView.text = setting.options[which]
                textView.tag = which
            }
            .show()
    }

    private fun saveSettings() {
        for (setting in settings) {
            val view = settingViews[setting.key] ?: continue

            when (setting) {
                is PluginSetting.SwitchSetting -> {
                    val switch = view as MaterialSwitch
                    plugin.onSettingChanged(setting.key, switch.isChecked)
                }
                is PluginSetting.TextSetting -> {
                    val editText = view as EditText
                    plugin.onSettingChanged(setting.key, editText.text.toString())
                }
                is PluginSetting.NumberSetting -> {
                    val seekBar = view as SeekBar
                    val value = seekBar.progress + setting.min
                    plugin.onSettingChanged(setting.key, value)
                }
                is PluginSetting.SelectSetting -> {
                    val textView = view as TextView
                    val selectedIndex = view.tag as? Int ?: setting.defaultIndex
                    plugin.onSettingChanged(setting.key, selectedIndex)
                }
            }
        }

        Toast.makeText(requireContext(), "设置已保存", Toast.LENGTH_SHORT).show()
    }

    private fun resetSettings() {
        AlertDialog.Builder(requireContext())
            .setTitle("重置设置")
            .setMessage("确定要重置所有设置吗？")
            .setPositiveButton("重置") { _, _ ->
                for (setting in settings) {
                    when (setting) {
                        is PluginSetting.SwitchSetting -> {
                            plugin.onSettingChanged(setting.key, setting.defaultValue)
                        }
                        is PluginSetting.TextSetting -> {
                            plugin.onSettingChanged(setting.key, setting.defaultValue)
                        }
                        is PluginSetting.NumberSetting -> {
                            plugin.onSettingChanged(setting.key, setting.defaultValue)
                        }
                        is PluginSetting.SelectSetting -> {
                            plugin.onSettingChanged(setting.key, setting.defaultIndex)
                        }
                    }
                }
                // 重新创建对话框以刷新UI
                dismiss()
                newInstance(pluginId, settings).show(parentFragmentManager, "plugin_settings")
                Toast.makeText(requireContext(), "设置已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
