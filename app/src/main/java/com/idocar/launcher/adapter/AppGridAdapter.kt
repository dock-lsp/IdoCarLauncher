package com.idocar.launcher.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.idocar.launcher.data.AppItem
import com.idocar.launcher.databinding.ItemAppBinding

/**
 * 应用网格适配器
 */
class AppGridAdapter(
    private val onAppClick: (AppItem) -> Unit,
    private val onAppLongClick: (AppItem) -> Unit
) : ListAdapter<AppItem, AppGridAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
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
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppItem) {
            binding.apply {
                tvAppName.text = app.appName

                // 加载图标
                app.icon?.let { drawable ->
                    ivAppIcon.setImageDrawable(drawable)
                } ?: run {
                    Glide.with(root.context)
                        .load(app.iconUri)
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .into(ivAppIcon)
                }

                // 收藏标记
                ivFavorite.visibility = if (app.isFavorite) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                root.setOnClickListener {
                    onAppClick(app)
                }

                root.setOnLongClickListener {
                    onAppLongClick(app)
                    true
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
