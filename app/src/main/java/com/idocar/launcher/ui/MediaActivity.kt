package com.idocar.launcher.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.idocar.launcher.R
import com.idocar.launcher.adapter.MediaItemAdapter
import com.idocar.launcher.databinding.ActivityMediaBinding
import com.idocar.launcher.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.launch

/**
 * 媒体播放器界面
 */
class MediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaBinding
    private val viewModel: MediaViewModel by viewModels()
    private lateinit var mediaAdapter: MediaItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "音乐"
        }

        setupRecyclerView()
        setupListeners()
        observeMediaState()
    }

    private fun setupRecyclerView() {
        mediaAdapter = MediaItemAdapter(
            onItemClick = { item ->
                viewModel.playMedia(item)
            }
        )

        binding.recyclerPlaylist.apply {
            layoutManager = LinearLayoutManager(this@MediaActivity)
            adapter = mediaAdapter
        }
    }

    private fun setupListeners() {
        // 播放控制
        binding.btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }

        binding.btnPrevious.setOnClickListener {
            viewModel.skipToPrevious()
        }

        binding.btnNext.setOnClickListener {
            viewModel.skipToNext()
        }

        binding.btnShuffle.setOnClickListener {
            viewModel.toggleShuffle()
        }

        binding.btnRepeat.setOnClickListener {
            viewModel.toggleRepeat()
        }

        // 进度条
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    viewModel.seekTo(it.progress.toLong())
                }
            }
        })

        // 标签切换
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadLocalMusic()
                    1 -> viewModel.loadPlaylists()
                    2 -> viewModel.loadArtists()
                    3 -> viewModel.loadAlbums()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun observeMediaState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playbackState.collect { state ->
                    updatePlaybackUI(state)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mediaItems.collect { items ->
                    mediaAdapter.submitList(items)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentPosition.collect { position ->
                    binding.seekBar.progress = position.toInt()
                    binding.tvCurrentTime.text = formatTime(position)
                }
            }
        }
    }

    private fun updatePlaybackUI(state: com.idocar.launcher.data.MediaPlaybackState) {
        // 更新歌曲信息
        binding.tvTitle.text = state.title.takeIf { it.isNotEmpty() } ?: "未知歌曲"
        binding.tvArtist.text = state.artist.takeIf { it.isNotEmpty() } ?: "未知艺术家"

        // 更新播放按钮
        binding.btnPlayPause.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        // 更新进度条
        binding.seekBar.max = state.duration.toInt()
        binding.tvTotalTime.text = formatTime(state.duration)

        // 更新随机播放和循环按钮状态
        binding.btnShuffle.isActivated = state.shuffleMode != 0
        binding.btnRepeat.isActivated = state.repeatMode != 0

        // 加载专辑封面
        state.albumArt?.let { uri ->
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_album)
                .error(R.drawable.ic_album)
                .into(binding.ivAlbumArt)
        } ?: run {
            binding.ivAlbumArt.setImageResource(R.drawable.ic_album)
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
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

    override fun onDestroy() {
        super.onDestroy()
        // 不释放资源，继续后台播放
    }
}
