package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("gundi_bro_settings", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // --- StateFlows for Compose UI ---

    private val _nickname = MutableStateFlow(getSavedNickname())
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _theme = MutableStateFlow(getSavedTheme())
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _language = MutableStateFlow(getSavedLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    private val _creativity = MutableStateFlow(getSavedCreativity())
    val creativity: StateFlow<String> = _creativity.asStateFlow()

    private val _replyMode = MutableStateFlow(getSavedReplyMode())
    val replyMode: StateFlow<String> = _replyMode.asStateFlow()

    private val _emotionSensitivity = MutableStateFlow(getSavedEmotionSensitivity())
    val emotionSensitivity: StateFlow<String> = _emotionSensitivity.asStateFlow()

    private val _speechSpeed = MutableStateFlow(getSavedSpeechSpeed())
    val speechSpeed: StateFlow<Float> = _speechSpeed.asStateFlow()

    private val _continuousListening = MutableStateFlow(getSavedContinuousListening())
    val continuousListening: StateFlow<Boolean> = _continuousListening.asStateFlow()

    private val _voiceTrigger = MutableStateFlow(getSavedVoiceTrigger())
    val voiceTrigger: StateFlow<Boolean> = _voiceTrigger.asStateFlow()

    private val _screenScanRate = MutableStateFlow(getSavedScreenScanRate())
    val screenScanRate: StateFlow<String> = _screenScanRate.asStateFlow()

    private val _hapticFeedback = MutableStateFlow(getSavedHapticFeedback())
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    private val _debugLogging = MutableStateFlow(getSavedDebugLogging())
    val debugLogging: StateFlow<Boolean> = _debugLogging.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(getSavedIsTtsEnabled())
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _witLevel = MutableStateFlow(getSavedWitLevel())
    val witLevel: StateFlow<Float> = _witLevel.asStateFlow()

    private val _gundiAvatar = MutableStateFlow(getSavedGundiAvatar())
    val gundiAvatar: StateFlow<String> = _gundiAvatar.asStateFlow()

    private val _speechPitch = MutableStateFlow(getSavedSpeechPitch())
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _voiceStyle = MutableStateFlow(getSavedVoiceStyle())
    val voiceStyle: StateFlow<String> = _voiceStyle.asStateFlow()

    private val _startupGreeting = MutableStateFlow(getSavedStartupGreeting())
    val startupGreeting: StateFlow<String> = _startupGreeting.asStateFlow()

    private val _isBubbleEnabled = MutableStateFlow(getSavedIsBubbleEnabled())
    val isBubbleEnabled: StateFlow<Boolean> = _isBubbleEnabled.asStateFlow()


    // --- Setters that persist and update UI instantly ---

    fun setNickname(value: String) {
        prefs.edit().putString("nickname", value).apply()
        _nickname.value = value
    }

    fun setTheme(value: String) {
        prefs.edit().putString("theme", value).apply()
        _theme.value = value
    }

    fun setLanguage(value: String) {
        prefs.edit().putString("language", value).apply()
        _language.value = value
    }

    fun setCreativity(value: String) {
        prefs.edit().putString("creativity", value).apply()
        _creativity.value = value
    }

    fun setReplyMode(value: String) {
        prefs.edit().putString("reply_mode", value).apply()
        _replyMode.value = value
    }

    fun setEmotionSensitivity(value: String) {
        prefs.edit().putString("emotion_sensitivity", value).apply()
        _emotionSensitivity.value = value
    }

    fun setSpeechSpeed(value: Float) {
        prefs.edit().putFloat("speech_speed", value).apply()
        _speechSpeed.value = value
    }

    fun setContinuousListening(value: Boolean) {
        prefs.edit().putBoolean("continuous_listening", value).apply()
        _continuousListening.value = value
    }

    fun setVoiceTrigger(value: Boolean) {
        prefs.edit().putBoolean("voice_trigger", value).apply()
        _voiceTrigger.value = value
    }

    fun setScreenScanRate(value: String) {
        prefs.edit().putString("screen_scan_rate", value).apply()
        _screenScanRate.value = value
    }

    fun setHapticFeedback(value: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", value).apply()
        _hapticFeedback.value = value
    }

    fun setDebugLogging(value: Boolean) {
        prefs.edit().putBoolean("debug_logging", value).apply()
        _debugLogging.value = value
    }

    fun setIsTtsEnabled(value: Boolean) {
        prefs.edit().putBoolean("is_tts_enabled", value).apply()
        _isTtsEnabled.value = value
    }

    fun setWitLevel(value: Float) {
        prefs.edit().putFloat("wit_level", value).apply()
        _witLevel.value = value
    }

    fun setGundiAvatar(value: String) {
        prefs.edit().putString("gundi_avatar", value).apply()
        _gundiAvatar.value = value
    }

    fun setSpeechPitch(value: Float) {
        prefs.edit().putFloat("speech_pitch", value).apply()
        _speechPitch.value = value
    }

    fun setVoiceStyle(value: String) {
        prefs.edit().putString("voice_style", value).apply()
        _voiceStyle.value = value
        
        // Auto-configure speed and pitch when preset style changes
        when (value) {
            "classic" -> {
                setSpeechPitch(1.0f)
                setSpeechSpeed(1.0f)
            }
            "squeaky" -> {
                setSpeechPitch(1.45f)
                setSpeechSpeed(1.15f)
            }
            "deep" -> {
                setSpeechPitch(0.75f)
                setSpeechSpeed(0.95f)
            }
            "excited" -> {
                setSpeechPitch(1.15f)
                setSpeechSpeed(1.35f)
            }
            "robotic" -> {
                setSpeechPitch(0.6f)
                setSpeechSpeed(1.0f)
            }
        }
    }

    fun setStartupGreeting(value: String) {
        prefs.edit().putString("startup_greeting", value).apply()
        _startupGreeting.value = value
    }

    fun setBubbleEnabled(value: Boolean) {
        prefs.edit().putBoolean("is_bubble_enabled", value).apply()
        _isBubbleEnabled.value = value
    }


    // --- SharedPreferences Readers ---

    private fun getSavedNickname(): String = prefs.getString("nickname", "Bro") ?: "Bro"
    private fun getSavedTheme(): String = prefs.getString("theme", "Dark") ?: "Dark"
    private fun getSavedLanguage(): String = prefs.getString("language", "Turkish") ?: "Turkish"
    private fun getSavedCreativity(): String = prefs.getString("creativity", "Balanced") ?: "Balanced"
    private fun getSavedReplyMode(): String = prefs.getString("reply_mode", "Detailed") ?: "Detailed"
    private fun getSavedEmotionSensitivity(): String = prefs.getString("emotion_sensitivity", "Medium") ?: "Medium"
    private fun getSavedSpeechSpeed(): Float = prefs.getFloat("speech_speed", 1.0f)
    private fun getSavedContinuousListening(): Boolean = prefs.getBoolean("continuous_listening", false)
    private fun getSavedVoiceTrigger(): Boolean = prefs.getBoolean("voice_trigger", false)
    private fun getSavedScreenScanRate(): String = prefs.getString("screen_scan_rate", "Normal") ?: "Normal"
    private fun getSavedHapticFeedback(): Boolean = prefs.getBoolean("haptic_feedback", true)
    private fun getSavedDebugLogging(): Boolean = prefs.getBoolean("debug_logging", false)
    private fun getSavedIsTtsEnabled(): Boolean = prefs.getBoolean("is_tts_enabled", true)
    private fun getSavedWitLevel(): Float = prefs.getFloat("wit_level", 3.0f)
    private fun getSavedGundiAvatar(): String = prefs.getString("gundi_avatar", "classic") ?: "classic"
    private fun getSavedSpeechPitch(): Float = prefs.getFloat("speech_pitch", 1.0f)
    private fun getSavedVoiceStyle(): String = prefs.getString("voice_style", "classic") ?: "classic"
    private fun getSavedStartupGreeting(): String = prefs.getString("startup_greeting", "Merhaba kralım, hoş geldin!") ?: "Merhaba kralım, hoş geldin!"
    private fun getSavedIsBubbleEnabled(): Boolean = prefs.getBoolean("is_bubble_enabled", false)
}
