package com.idocar.launcher.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.idocar.launcher.databinding.ItemMediaBinding
import com.idocar.launcher.ui.viewmodel.MediaViewModel

/**
 * 媒体列表适配器
 */
class MediaItemAdapter(
    private val onItemClick: (MediaViewModel.MediaItem) -> Unit
) : ListAdapter<MediaViewModel.MediaItem, MediaItemAdapter.MediaViewHolder>(MediaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(
        private val binding: ItemMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaViewModel.MediaItem) {
            binding.apply {
                tvTitle.text = item.title
                tvArtist.text = item.artist
                tvDuration.text = formatDuration(item.duration)

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }

        private fun formatDuration(ms: Long): String {
            val seconds = (ms / 1000) % 60
            val minutes = (ms / (1000 * 60)) % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    class MediaDiffCallback : DiffUtil.ItemCallback<MediaViewModel.MediaItem>() {
        override fun areItemsTheSame(
            oldItem: MediaViewModel.MediaItem,
            newItem: MediaViewModel.MediaItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: MediaViewModel.MediaItem,
            newItem: MediaViewModel.MediaItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
