package com.idocar.launcher.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.idocar.launcher.data.ThemeItem
import com.idocar.launcher.databinding.ItemThemeBinding

/**
 * 主题列表适配器
 */
class ThemeAdapter(
    private val onThemeSelected: (ThemeItem) -> Unit,
    private val onThemeDownload: (ThemeItem) -> Unit
) : ListAdapter<ThemeItem, ThemeAdapter.ThemeViewHolder>(ThemeDiffCallback()) {

    private var selectedThemeId: String? = null

    fun setSelectedTheme(themeId: String) {
        selectedThemeId = themeId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemThemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).id == selectedThemeId)
    }

    inner class ThemeViewHolder(
        private val binding: ItemThemeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(theme: ThemeItem, isSelected: Boolean) {
            binding.apply {
                tvThemeName.text = theme.name

                // 显示选中状态
                root.isSelected = isSelected
                ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

                // 主题预览色块
                viewPreview.setBackgroundColor(theme.primaryColor)

                // 加载预览图
                if (theme.previewUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(theme.previewUrl)
                        .into(ivThemePreview)
                }

                // 下载状态
                if (theme.isDownloaded) {
                    btnDownload.visibility = View.GONE
                } else {
                    btnDownload.visibility = View.VISIBLE
                    btnDownload.setOnClickListener {
                        onThemeDownload(theme)
                    }
                }

                root.setOnClickListener {
                    onThemeSelected(theme)
                }
            }
        }
    }

    class ThemeDiffCallback : DiffUtil.ItemCallback<ThemeItem>() {
        override fun areItemsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ThemeItem, newItem: ThemeItem): Boolean {
            return oldItem == newItem
        }
    }
}
