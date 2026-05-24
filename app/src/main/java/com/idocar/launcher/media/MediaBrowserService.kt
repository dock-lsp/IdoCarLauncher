package com.idocar.launcher.media

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat

/**
 * 媒体浏览器服务
 * 提供媒体浏览和控制功能
 */
class MediaBrowserService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        
        mediaSession = MediaSessionCompat(this, "CarLauncherMediaService").apply {
            setCallback(MediaSessionCallback())
            setSessionToken(sessionToken)
            
            // 设置初始播放状态
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SEEK_TO
                    )
                    .build()
            )
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        // 允许所有客户端访问
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        // 加载媒体项
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        result.sendResult(mediaItems)
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            // 处理播放
        }

        override fun onPause() {
            // 处理暂停
        }

        override fun onSkipToNext() {
            // 处理下一首
        }

        override fun onSkipToPrevious() {
            // 处理上一首
        }

        override fun onSeekTo(pos: Long) {
            // 处理进度跳转
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }
}
