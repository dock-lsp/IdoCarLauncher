package com.idocar.launcher.plugin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.idocar.launcher.data.PluginInfo
import com.idocar.launcher.databinding.ItemPluginBinding

/**
 * 插件列表适配器
 */
class PluginAdapter(
    private val onToggleEnabled: (PluginInfo, Boolean) -> Unit,
    private val onConfigure: (PluginInfo) -> Unit,
    private val onUninstall: (PluginInfo) -> Unit
) : ListAdapter<PluginInfo, PluginAdapter.PluginViewHolder>(PluginDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PluginViewHolder {
        val binding = ItemPluginBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PluginViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PluginViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PluginViewHolder(
        private val binding: ItemPluginBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plugin: PluginInfo) {
            binding.apply {
                tvPluginName.text = plugin.name
                tvPluginVersion.text = "v${plugin.version}"
                tvPluginDescription.text = plugin.description
                tvPluginAuthor.text = "作者: ${plugin.author}"

                switchEnabled.isChecked = plugin.isEnabled
                switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    onToggleEnabled(plugin, isChecked)
                }

                btnConfigure.setOnClickListener {
                    onConfigure(plugin)
                }

                btnUninstall.setOnClickListener {
                    onUninstall(plugin)
                }

                // 加载状态指示
                tvStatus.text = when {
                    plugin.isLoaded -> "已加载"
                    plugin.isEnabled -> "待加载"
                    else -> "已禁用"
                }
            }
        }
    }

    class PluginDiffCallback : DiffUtil.ItemCallback<PluginInfo>() {
        override fun areItemsTheSame(oldItem: PluginInfo, newItem: PluginInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PluginInfo, newItem: PluginInfo): Boolean {
            return oldItem == newItem
        }
    }
}
