package com.idocar.launcher.media

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.idocar.launcher.R
import com.idocar.launcher.databinding.ActivityVideoPlayerBinding

/**
 * 视频播放器 Activity
 * 支持本地视频和网络视频播放
 */
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null
    
    private lateinit var playerView: PlayerView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnFullscreen: ImageButton

    private var videoList: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var isFullscreen: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlayPauseButton()
            if (playbackState == Player.STATE_READY) {
                seekBar.max = (player?.duration ?: 0).toInt()
                tvTotalTime.text = formatTime(player?.duration ?: 0)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseButton()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupFullscreen()
        initViews()
        setupListeners()
        
        // 获取视频列表
        videoList = intent.getStringArrayListExtra("video_list") ?: emptyList()
        currentIndex = intent.getIntExtra("current_index", 0)
        
        initializePlayer()
    }

    private fun setupFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun initViews() {
        playerView = binding.playerView
        btnPlayPause = binding.btnPlayPause
        btnPrevious = binding.btnPrevious
        btnNext = binding.btnNext
        seekBar = binding.seekBar
        tvCurrentTime = binding.tvCurrentTime
        tvTotalTime = binding.tvTotalTime
        btnBack = binding.btnBack
        btnFullscreen = binding.btnFullscreen
    }

    private fun setupListeners() {
        btnPlayPause.setOnClickListener {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
        }

        btnPrevious.setOnClickListener {
            playPrevious()
        }

        btnNext.setOnClickListener {
            playNext()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 定期更新进度
        seekBar.postDelayed(object : Runnable {
            override fun run() {
                player?.let {
                    seekBar.progress = it.currentPosition.toInt()
                    tvCurrentTime.text = formatTime(it.currentPosition)
                }
                seekBar.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            playerView.player = this
            addListener(playerListener)
            
            if (videoList.isNotEmpty() && currentIndex < videoList.size) {
                val mediaItem = MediaItem.fromUri(videoList[currentIndex])
                setMediaItem(mediaItem)
                prepare()
                play()
            }
        }
    }

    private fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            loadVideo(currentIndex)
        }
    }

    private fun playNext() {
        if (currentIndex < videoList.size - 1) {
            currentIndex++
            loadVideo(currentIndex)
        }
    }

    private fun loadVideo(index: Int) {
        if (index in videoList.indices) {
            val mediaItem = MediaItem.fromUri(videoList[index])
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            supportActionBar?.hide()
            binding.controlsOverlay.visibility = View.GONE
        } else {
            supportActionBar?.show()
            binding.controlsOverlay.visibility = View.VISIBLE
        }
    }

    private fun updatePlayPauseButton() {
        val isPlaying = player?.isPlaying ?: false
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.removeListener(playerListener)
        player?.release()
        player = null
    }
}
