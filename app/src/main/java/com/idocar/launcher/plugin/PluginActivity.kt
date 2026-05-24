package com.idocar.launcher.plugin

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.idocar.launcher.CarLauncherApp
import com.idocar.launcher.databinding.ActivityPluginBinding
import kotlinx.coroutines.launch

/**
 * 插件管理界面
 */
class PluginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPluginBinding
    private lateinit var pluginAdapter: PluginAdapter
    private lateinit var pluginManager: PluginManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPluginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "插件管理"
        }

        pluginManager = (application as CarLauncherApp).pluginManager

        setupRecyclerView()
        observePlugins()
    }

    private fun setupRecyclerView() {
        pluginAdapter = PluginAdapter(
            onToggleEnabled = { plugin, enabled ->
                pluginManager.setPluginEnabled(plugin.id, enabled)
            },
            onConfigure = { plugin ->
                openPluginSettings(plugin)
            },
            onUninstall = { plugin ->
                confirmUninstall(plugin)
            }
        )

        binding.recyclerPlugins.apply {
            layoutManager = LinearLayoutManager(this@PluginActivity)
            adapter = pluginAdapter
        }
    }

    private fun observePlugins() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pluginManager.plugins.collect { plugins ->
                    pluginAdapter.submitList(plugins)
                    updateEmptyState(plugins.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerPlugins.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun openPluginSettings(pluginInfo: com.idocar.launcher.data.PluginInfo) {
        val plugin = pluginManager.getPlugin(pluginInfo.id) ?: return
        val settings = plugin.getSettings()

        if (settings.isEmpty()) {
            android.widget.Toast.makeText(this, "该插件没有可配置项", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // 打开设置对话框
        PluginSettingsDialog.newInstance(pluginInfo.id, settings)
            .show(supportFragmentManager, "plugin_settings")
    }

    private fun confirmUninstall(pluginInfo: com.idocar.launcher.data.PluginInfo) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("卸载插件")
            .setMessage("确定要卸载 ${pluginInfo.name} 吗？")
            .setPositiveButton("卸载") { _, _ ->
                pluginManager.uninstallPlugin(pluginInfo.id)
                android.widget.Toast.makeText(this, "插件已卸载", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
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
}
