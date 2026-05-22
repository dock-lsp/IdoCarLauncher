package com.idocar.launcher.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.idocar.launcher.data.VoiceCommandType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * 语音助手管理器
 * 处理语音识别、命令解析和语音合成
 */
class VoiceAssistantManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var commandHandler: VoiceCommandHandler? = null

    init {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.Listening
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                            SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                            SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                            SpeechRecognizer.ERROR_NO_MATCH -> "无法识别"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                            SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "超时，请重试"
                            else -> "未知错误"
                        }
                        _voiceState.value = VoiceState.Error(errorMessage)
                        Log.e(TAG, "Speech recognition error: $errorMessage")
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            _recognizedText.value = text
                            _voiceState.value = VoiceState.Recognized(text)
                            processCommand(text)
                        } else {
                            _voiceState.value = VoiceState.Error("未识别到语音")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _recognizedText.value = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINESE
            }
        }
    }

    /**
     * 开始语音识别
     */
    fun startListening() {
        if (isListening) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        
        speechRecognizer?.startListening(intent)
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        _voiceState.value = VoiceState.Idle
    }

    /**
     * 设置命令处理器
     */
    fun setCommandHandler(handler: VoiceCommandHandler) {
        this.commandHandler = handler
    }

    /**
     * 处理语音命令
     */
    private fun processCommand(text: String) {
        _voiceState.value = VoiceState.Processing
        
        val command = parseCommand(text)
        val response = commandHandler?.handleCommand(command, text) ?: "我不明白您的意思"
        
        speak(response)
        _voiceState.value = VoiceState.Completed(command, response)
    }

    /**
     * 解析命令类型
     */
    private fun parseCommand(text: String): VoiceCommandType {
        val lowerText = text.lowercase(Locale.getDefault())
        
        return when {
            // 打开应用
            lowerText.contains("打开") || lowerText.contains("启动") -> VoiceCommandType.OPEN_APP
            
            // 播放音乐
            lowerText.contains("播放") || lowerText.contains("听歌") || 
            lowerText.contains("音乐") -> VoiceCommandType.PLAY_MUSIC
            
            // 导航
            lowerText.contains("导航") || lowerText.contains("去") || 
            lowerText.contains("怎么走") -> VoiceCommandType.NAVIGATE
            
            // 打电话
            lowerText.contains("打电话") || lowerText.contains("呼叫") || 
            lowerText.contains("拨打") -> VoiceCommandType.CALL
            
            // 设置
            lowerText.contains("设置") || lowerText.contains("打开") && 
            (lowerText.contains("蓝牙") || lowerText.contains("wifi") || lowerText.contains("音量")) -> 
                VoiceCommandType.SETTING
            
            // 天气
            lowerText.contains("天气") || lowerText.contains("温度") -> VoiceCommandType.WEATHER
            
            // 音量
            lowerText.contains("音量") || lowerText.contains("大声") || 
            lowerText.contains("小声") -> VoiceCommandType.VOLUME
            
            else -> VoiceCommandType.UNKNOWN
        }
    }

    /**
     * 语音合成播报
     */
    fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * 停止播报
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    /**
     * 释放资源
     */
    fun cleanup() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    sealed class VoiceState {
        object Idle : VoiceState()
        object Listening : VoiceState()
        object Processing : VoiceState()
        data class Recognized(val text: String) : VoiceState()
        data class Completed(val command: VoiceCommandType, val response: String) : VoiceState()
        data class Error(val message: String) : VoiceState()
    }

    interface VoiceCommandHandler {
        fun handleCommand(command: VoiceCommandType, rawText: String): String
    }

    companion object {
        private const val TAG = "VoiceAssistant"
    }
}
