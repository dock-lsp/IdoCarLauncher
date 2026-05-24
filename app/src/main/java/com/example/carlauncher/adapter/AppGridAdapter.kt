package com.example.carlauncher.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.carlauncher.data.AppItem
import com.example.carlauncher.databinding.ItemAppGridBinding

/**
 * 应用网格适配器 - 支持横向滚动、添加、删除
 */
class AppGridAdapter(
    private val onAppClick: (AppItem) -> Unit,
    private val onAppLongClick: (AppItem) -> Unit,
    private val onRemoveClick: (AppItem) -> Unit
) : ListAdapter<AppItem, AppGridAdapter.AppViewHolder>(AppDiffCallback()) {

    private var isEditMode = false

    fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(
        private val binding: ItemAppGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppItem) {
            binding.apply {
                tvAppName.text = app.appName
                
                // 加载图标
                app.icon?.let { drawable ->
                    ivAppIcon.setImageDrawable(drawable)
                }

                // 显示/隐藏删除按钮
                btnRemove.visibility = if (isEditMode) View.VISIBLE else View.GONE

                root.setOnClickListener {
                    if (isEditMode) {
                        onRemoveClick(app)
                    } else {
                        onAppClick(app)
                    }
                }

                root.setOnLongClickListener {
                    onAppLongClick(app)
                    true
                }

                btnRemove.setOnClickListener {
                    onRemoveClick(app)
                }
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
            return oldItem == newItem
        }
    }
}
