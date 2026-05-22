package com.idocar.launcher.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.idocar.launcher.databinding.ActivityVoiceAssistantBinding
import com.idocar.launcher.voice.VoiceAssistantManager
import com.idocar.launcher.voice.VoiceCommandProcessor
import kotlinx.coroutines.launch

/**
 * 语音助手界面
 */
class VoiceAssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceAssistantBinding
    private lateinit var voiceManager: VoiceAssistantManager
    private lateinit var commandProcessor: VoiceCommandProcessor

    private var waveAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeVoiceAssistant()
        setupUI()
        setupListeners()
        observeVoiceState()
    }

    private fun initializeVoiceAssistant() {
        voiceManager = VoiceAssistantManager(this)
        commandProcessor = VoiceCommandProcessor(this)
        voiceManager.setCommandHandler(commandProcessor)
    }

    private fun setupUI() {
        // 设置初始状态
        updateUIState(VoiceAssistantManager.VoiceState.Idle)
    }

    private fun setupListeners() {
        // 点击麦克风开始/停止录音
        binding.btnMicrophone.setOnClickListener {
            when (voiceManager.voiceState.value) {
                is VoiceAssistantManager.VoiceState.Listening -> {
                    voiceManager.stopListening()
                }
                else -> {
                    voiceManager.startListening()
                }
            }
        }

        // 关闭按钮
        binding.btnClose.setOnClickListener {
            finish()
        }

        // 帮助按钮
        binding.btnHelp.setOnClickListener {
            showHelpDialog()
        }

        // 文字输入
        binding.btnTextInput.setOnClickListener {
            showTextInputDialog()
        }
    }

    private fun observeVoiceState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                voiceManager.voiceState.collect { state ->
                    updateUIState(state)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                voiceManager.recognizedText.collect { text ->
                    binding.tvRecognizedText.text = text
                }
            }
        }
    }

    private fun updateUIState(state: VoiceAssistantManager.VoiceState) {
        when (state) {
            is VoiceAssistantManager.VoiceState.Idle -> {
                stopWaveAnimation()
                binding.tvStatus.text = "点击麦克风开始说话"
                binding.tvRecognizedText.text = ""
                binding.btnMicrophone.isActivated = false
            }
            is VoiceAssistantManager.VoiceState.Listening -> {
                startWaveAnimation()
                binding.tvStatus.text = "正在聆听..."
                binding.btnMicrophone.isActivated = true
            }
            is VoiceAssistantManager.VoiceState.Processing -> {
                stopWaveAnimation()
                binding.tvStatus.text = "正在处理..."
            }
            is VoiceAssistantManager.VoiceState.Recognized -> {
                binding.tvStatus.text = "识别成功"
            }
            is VoiceAssistantManager.VoiceState.Completed -> {
                binding.tvStatus.text = "完成"
                binding.tvResponse.text = state.response
                // 3秒后重置
                binding.root.postDelayed({
                    updateUIState(VoiceAssistantManager.VoiceState.Idle)
                }, 3000)
            }
            is VoiceAssistantManager.VoiceState.Error -> {
                stopWaveAnimation()
                binding.tvStatus.text = state.message
                binding.btnMicrophone.isActivated = false
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startWaveAnimation() {
        waveAnimator?.cancel()
        waveAnimator = ObjectAnimator.ofFloat(binding.waveView, "scaleX", 1f, 1.5f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        
        ObjectAnimator.ofFloat(binding.waveView, "scaleY", 1f, 1.5f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopWaveAnimation() {
        waveAnimator?.cancel()
        binding.waveView.scaleX = 1f
        binding.waveView.scaleY = 1f
    }

    private fun showHelpDialog() {
        // 显示帮助对话框，列出支持的语音命令
        val commands = """
            支持的语音命令：
            
            📱 打开应用
            "打开导航"、"打开音乐"
            
            🎵 播放音乐
            "播放音乐"、"播放周杰伦的歌"
            
            🗺️ 导航
            "导航到北京天安门"、"去机场怎么走"
            
            📞 打电话
            "打电话给张三"
            
            ⚙️ 设置
            "打开蓝牙"、"调节音量"
            
            🌤️ 天气
            "今天天气怎么样"
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("语音助手帮助")
            .setMessage(commands)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showTextInputDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "输入指令..."
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("文字输入")
            .setView(editText)
            .setPositiveButton("发送") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    // 处理文字指令
                    processTextCommand(text)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun processTextCommand(text: String) {
        binding.tvRecognizedText.text = text
        // 使用 VoiceCommandProcessor 解析命令类型并处理
        val commandType = parseTextCommand(text)
        val response = commandProcessor.handleCommand(commandType, text)
        binding.tvResponse.text = response
        voiceManager.speak(response)
    }

    /**
     * 解析文字命令类型
     */
    private fun parseTextCommand(text: String): com.idocar.launcher.data.VoiceCommandType {
        val lowerText = text.lowercase(java.util.Locale.getDefault())
        return when {
            // 打开应用
            lowerText.contains("打开") || lowerText.contains("启动") -> com.idocar.launcher.data.VoiceCommandType.OPEN_APP
            // 播放音乐
            lowerText.contains("播放") || lowerText.contains("听歌") ||
            lowerText.contains("音乐") -> com.idocar.launcher.data.VoiceCommandType.PLAY_MUSIC
            // 导航
            lowerText.contains("导航") || lowerText.contains("去") ||
            lowerText.contains("怎么走") -> com.idocar.launcher.data.VoiceCommandType.NAVIGATE
            // 打电话
            lowerText.contains("打电话") || lowerText.contains("呼叫") ||
            lowerText.contains("拨打") -> com.idocar.launcher.data.VoiceCommandType.CALL
            // 设置
            lowerText.contains("设置") || (lowerText.contains("打开") &&
            (lowerText.contains("蓝牙") || lowerText.contains("wifi") || lowerText.contains("音量"))) ->
                com.idocar.launcher.data.VoiceCommandType.SETTING
            // 天气
            lowerText.contains("天气") || lowerText.contains("温度") -> com.idocar.launcher.data.VoiceCommandType.WEATHER
            // 音量
            lowerText.contains("音量") || lowerText.contains("大声") ||
            lowerText.contains("小声") -> com.idocar.launcher.data.VoiceCommandType.VOLUME
            else -> com.idocar.launcher.data.VoiceCommandType.UNKNOWN
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        waveAnimator?.cancel()
        voiceManager.cleanup()
    }
}
