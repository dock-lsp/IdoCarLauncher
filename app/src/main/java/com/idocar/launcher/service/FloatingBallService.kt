package com.idocar.launcher.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.idocar.launcher.R
import com.idocar.launcher.databinding.DialogFloatingMenuBinding
import com.idocar.launcher.ui.HomeActivity
import com.idocar.launcher.navigation.NavigationActivity
import com.idocar.launcher.ui.SettingsActivity
import com.idocar.launcher.ui.VoiceAssistantActivity

/**
 * 悬浮球服务
 * 提供快速访问常用功能的悬浮按钮
 */
class FloatingBallService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var menuView: View? = null
    private lateinit var menuBinding: DialogFloatingMenuBinding

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isMenuShowing = false

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): FloatingBallService = this@FloatingBallService
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingBall()
    }

    private fun createFloatingBall() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.view_floating_ball, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = event.rawX - initialTouchX
                    val diffY = event.rawY - initialTouchY
                    
                    // 如果移动距离很小，认为是点击
                    if (kotlin.math.abs(diffX) < 10 && kotlin.math.abs(diffY) < 10) {
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatingView, params)
        isRunning = true
    }

    private fun toggleMenu() {
        if (isMenuShowing) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    private fun showMenu() {
        if (menuView != null) return

        menuBinding = DialogFloatingMenuBinding.inflate(LayoutInflater.from(this))
        menuView = menuBinding.root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // 设置菜单项点击事件
        menuBinding.btnHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            hideMenu()
        }

        menuBinding.btnNavigation.setOnClickListener {
            startActivity(Intent(this, NavigationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            hideMenu()
        }

        menuBinding.btnVoice.setOnClickListener {
            startActivity(Intent(this, VoiceAssistantActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            hideMenu()
        }

        menuBinding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            hideMenu()
        }

        menuBinding.btnClose.setOnClickListener {
            hideMenu()
        }

        windowManager.addView(menuView, params)
        isMenuShowing = true
    }

    private fun hideMenu() {
        menuView?.let {
            windowManager.removeView(it)
            menuView = null
        }
        isMenuShowing = false
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        hideMenu()
        floatingView?.let {
            windowManager.removeView(it)
        }
        isRunning = false
    }

    companion object {
        var isRunning = false
            private set
    }
}
