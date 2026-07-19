package com.example

import java.lang.StringBuilder
import android.media.AudioTrack
import android.media.AudioFormat
import android.media.AudioManager

/**
 * Cleans the input text by removing markdown formatting, punctuation signs/symbols, 
 * and emojis/icons before passing to TextToSpeech to prevent TTS from reading them aloud.
 */
fun cleanTextForTts(text: String?): String {
    if (text == null || text.isEmpty()) return ""
    
    // First remove [DUYGU: ...] tags completely so TTS doesn't read them
    val textWithoutEmotion = text.replace("""\[DUYGU:\s*([^\]]+)\]""".toRegex(RegexOption.IGNORE_CASE), "")
    
    // 1. Remove markdown symbols
    var cleaned = textWithoutEmotion
        .replace("\\*+".toRegex(), "")
        .replace("_+".toRegex(), "")
        .replace("#+".toRegex(), "")
        .replace("`+".toRegex(), "")
        .replace("~+".toRegex(), "")
        .replace("<+".toRegex(), "")
        .replace(">+".toRegex(), "")
        .replace("\\[".toRegex(), "")
        .replace("\\]".toRegex(), "")
        .replace("\\{".toRegex(), "")
        .replace("\\}".toRegex(), "")

    val sb = StringBuilder()
    var i = 0
    while (i < cleaned.length) {
        val codePoint = cleaned.codePointAt(i)
        val charCount = Character.charCount(codePoint)
        val type = Character.getType(codePoint)
        
        // Skip emojis, symbols, and formatting characters
        // Character types:
        // SURROGATE = 19
        // OTHER_SYMBOL = 28
        // MATH_SYMBOL = 14
        // MODIFIER_SYMBOL = 27
        // PRIVATE_USE = 18
        val shouldSkip = when (type.toByte()) {
            Character.SURROGATE,
            Character.PRIVATE_USE,
            Character.OTHER_SYMBOL,
            Character.MODIFIER_SYMBOL -> true
            else -> false
        }
        
        if (!shouldSkip) {
            val ch = cleaned[i]
            // Skip other specific non-speech punctuation symbols
            if (ch != '@' && ch != '#' && ch != '*' && ch != '^' && ch != '~' && ch != '|' && ch != '\\' && ch != '/' && ch != '<' && ch != '>' && ch != '=' && ch != '+') {
                sb.append(cleaned.substring(i, i + charCount))
            }
        }
        i += charCount
    }
    
    // Normalize spacing and return
    return sb.toString().replace("\\s+".toRegex(), " ").trim()
}

object GeminiVoicePlayer {
    private var activeAudioTrack: AudioTrack? = null

    fun playPcm(base64Data: String) {
        try {
            stop()
            val pcmBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            
            // Gemini audio output is typically 24kHz, mono, 16-bit PCM
            val sampleRate = 24000
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val trackSize = maxOf(minBufSize, pcmBytes.size)
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                trackSize,
                AudioTrack.MODE_STATIC
            )
            
            track.write(pcmBytes, 0, pcmBytes.size)
            activeAudioTrack = track
            track.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            activeAudioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
            activeAudioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isPlaying(): Boolean {
        return try {
            activeAudioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
        } catch (e: Exception) {
            false
        }
    }
}
