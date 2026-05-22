package com.idocar.launcher.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.idocar.launcher.data.ShortcutItem
import com.idocar.launcher.databinding.ItemShortcutBinding

/**
 * 快捷方式适配器
 */
class ShortcutAdapter(
    private val onShortcutClick: (ShortcutItem) -> Unit,
    private val onShortcutLongClick: (ShortcutItem) -> Unit
) : ListAdapter<ShortcutItem, ShortcutAdapter.ShortcutViewHolder>(ShortcutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val binding = ItemShortcutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShortcutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShortcutViewHolder(
        private val binding: ItemShortcutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(shortcut: ShortcutItem) {
            binding.apply {
                tvShortcutTitle.text = shortcut.title
                ivShortcutIcon.setImageResource(shortcut.iconResId)

                root.setOnClickListener {
                    onShortcutClick(shortcut)
                }

                root.setOnLongClickListener {
                    onShortcutLongClick(shortcut)
                    true
                }
            }
        }
    }

    class ShortcutDiffCallback : DiffUtil.ItemCallback<ShortcutItem>() {
        override fun areItemsTheSame(oldItem: ShortcutItem, newItem: ShortcutItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShortcutItem, newItem: ShortcutItem): Boolean {
            return oldItem == newItem
        }
    }
}
