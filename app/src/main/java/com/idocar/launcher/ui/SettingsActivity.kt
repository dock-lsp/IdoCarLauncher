package com.idocar.launcher.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.idocar.launcher.R
import com.idocar.launcher.service.FloatingBallService

/**
 * 设置界面
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.settings)
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // 悬浮球设置
            findPreference<SwitchPreferenceCompat>("floating_ball_enabled")?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    requireContext().startService(Intent(requireContext(), FloatingBallService::class.java))
                } else {
                    requireContext().stopService(Intent(requireContext(), FloatingBallService::class.java))
                }
                true
            }

            // 应用管理
            findPreference<Preference>("app_manager")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), AppManagerActivity::class.java))
                true
            }

            // 主题设置
            findPreference<Preference>("theme_settings")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), ThemeActivity::class.java))
                true
            }

            // 插件管理
            findPreference<Preference>("plugin_manager")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), com.idocar.launcher.plugin.PluginActivity::class.java))
                true
            }

            // 关于
            findPreference<Preference>("about")?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }
        }

        private fun showAboutDialog() {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage("""
                    版本: 1.0.0
                    
                    IdoCarLauncher 是一款专为车机设计的桌面启动器。
                    
                    功能特点:
                    • 多媒体控制
                    • 导航集成
                    • 语音助手
                    • 插件系统
                    • 画中画导航
                """.trimIndent())
                .setPositiveButton("确定", null)
                .show()
        }
    }
}
