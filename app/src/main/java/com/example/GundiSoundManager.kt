package com.example

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

object GundiSoundManager {

    private var activeAudioTrack: AudioTrack? = null

    /**
     * Generates a smooth, click-free frequency sweep and plays it via AudioTrack.
     * All synthesis runs on Dispatchers.Default to keep UI fully responsive.
     */
    suspend fun playSynthesizedSweep(
        startFreq: Float,
        endFreq: Float,
        durationMs: Int,
        volume: Float = 0.5f,
        sampleRate: Int = 16000
    ) = withContext(Dispatchers.Default) {
        try {
            stopActiveTrack()

            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ByteArray(numSamples * 2)
            var bufferIndex = 0

            for (sample in 0 until numSamples) {
                val timeSec = sample.toDouble() / sampleRate
                val totalDurationSec = durationMs.toDouble() / 1000
                val sweepRate = (endFreq - startFreq) / totalDurationSec
                
                // Continuous phase formula for a linear sweep: 2 * PI * (f0 * t + 0.5 * k * t^2)
                val angle = 2.0 * PI * (startFreq * timeSec + 0.5 * sweepRate * timeSec * timeSec)

                // Enveloping (Attack and Release) to eliminate popping/clicking sounds
                val envelope = when {
                    sample < numSamples * 0.15 -> sample / (numSamples * 0.15) // Attack
                    sample > numSamples * 0.85 -> (numSamples - sample) / (numSamples * 0.15) // Release
                    else -> 1.0
                }

                val amplitude = 32767 * volume * envelope
                val value = (sin(angle) * amplitude).toInt().toShort()

                buffer[bufferIndex++] = (value.toInt() and 0x00FF).toByte()
                buffer[bufferIndex++] = ((value.toInt() and 0xFF00) ushr 8).toByte()
            }

            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val trackSize = maxOf(minBufSize, buffer.size)

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                trackSize,
                AudioTrack.MODE_STATIC
            )

            track.write(buffer, 0, buffer.size)
            activeAudioTrack = track
            track.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Plays a sequence of precise tones (arpeggio / chord sequence) for melodic indicators.
     */
    suspend fun playToneSequence(
        freqs: List<Float>,
        noteDurationMs: Int,
        gapMs: Int = 10,
        volume: Float = 0.5f,
        sampleRate: Int = 16000
    ) = withContext(Dispatchers.Default) {
        try {
            stopActiveTrack()

            val singleNoteSamples = (sampleRate * noteDurationMs / 1000)
            val gapSamples = (sampleRate * gapMs / 1000)
            val totalSamples = (singleNoteSamples + gapSamples) * freqs.size
            val buffer = ByteArray(totalSamples * 2)
            var bufferIndex = 0

            for (freq in freqs) {
                // Note synthesis
                for (sample in 0 until singleNoteSamples) {
                    val angle = 2.0 * PI * sample * freq / sampleRate
                    
                    // Note envelope (fade in & fade out)
                    val envelope = when {
                        sample < singleNoteSamples * 0.1 -> sample / (singleNoteSamples * 0.1)
                        sample > singleNoteSamples * 0.8 -> (singleNoteSamples - sample) / (singleNoteSamples * 0.2)
                        else -> 1.0
                    }

                    val amplitude = 32767 * volume * envelope
                    val value = (sin(angle) * amplitude).toInt().toShort()

                    buffer[bufferIndex++] = (value.toInt() and 0x00FF).toByte()
                    buffer[bufferIndex++] = ((value.toInt() and 0xFF00) ushr 8).toByte()
                }
                // Silence Gap synthesis
                for (sample in 0 until gapSamples) {
                    buffer[bufferIndex++] = 0
                    buffer[bufferIndex++] = 0
                }
            }

            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val trackSize = maxOf(minBufSize, buffer.size)

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                trackSize,
                AudioTrack.MODE_STATIC
            )

            track.write(buffer, 0, buffer.size)
            activeAudioTrack = track
            track.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopActiveTrack() {
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

    /**
     * Returns a collection of Gundi-style witty phrases based on the active emotion.
     */
    fun getWittyPhrasesForEmotion(expression: CharacterExpression): List<String> {
        return when (expression) {
            CharacterExpression.JOY -> listOf(
                "Ooooh keyifler gıcır reisim, fişek gibiyiz fişek!",
                "Yallah bro, her şey tıkırında maşallah!",
                "Şefim neşe doldu her yanım, haydi hayırlısı!"
            )
            CharacterExpression.SADNESS -> listOf(
                "Yak yak yak... Sigaraları dertli dertli yaktık yine be bro.",
                "Ah be can dostum, içim kıyıldı sanki dert çöktü tepeme.",
                "Gözümden bir damla yaş süzüldü süzülecek canısı..."
            )
            CharacterExpression.SURPRISE -> listOf(
                "Hoppaaa! O neydi gız?! Şok üstüne şok yaşadım reisim!",
                "Uyy anam! Kalbim duracaktı az daha şef! Bu neydi şimdi?",
                "Gözlerime inanamıyorum bro, şaşkınlıktan fıttırdım vallahi!"
            )
            CharacterExpression.THINKING -> listOf(
                "Dur bakayım... Saksıyı azıcık çalıştıralım hele, derin analiz başlıyor.",
                "Beyin bedava dedik ama şu an fırın gibi ısınıyor işlemci kanka!",
                "Kafada deli sorular... Derin felsefi teoriler dönüyor şef."
            )
            CharacterExpression.LISTENING -> listOf(
                "Dinliyorum canısı, dökül bakalım, söz sende!",
                "Söyle kanka, kulaklarım dört açıldı seni can kulağıyla dinliyorum.",
                "Eee dök içini bakalım bro, Gundi kankana anlat her şeyi."
            )
            CharacterExpression.SPEAKING -> listOf(
                "Bak şimdi, kelamın sırası bende şefim!",
                "Sıkı dur reisim, şimdi asıl bombayı, en tatlı konuyu anlatıyorum!",
                "Gundi söze girdi mi akan sular durur bro, dinle bak."
            )
            CharacterExpression.IDLE -> emptyList()
        }
    }

    /**
     * Synthesizes and plays a unique thematic retro sound effect based on the emotion,
     * and triggers a witty TTS phrase to complement Gundi's expressive character.
     */
    suspend fun playGundiEmotionReaction(
        context: Context,
        expression: CharacterExpression,
        tts: TextToSpeech?,
        isTtsEnabled: Boolean = true
    ) {
        // 1. Play the synthesized electronic sound effect corresponding to the expression
        when (expression) {
            CharacterExpression.JOY -> {
                // Cheery ascending major-scale chord sequence (C5, E5, G5, C6)
                playToneSequence(
                    freqs = listOf(523.25f, 659.25f, 783.99f, 1046.50f),
                    noteDurationMs = 70,
                    gapMs = 5,
                    volume = 0.45f
                )
            }
            CharacterExpression.SADNESS -> {
                // A slowly descending sad sigh pitch sweep
                playSynthesizedSweep(
                    startFreq = 320f,
                    endFreq = 120f,
                    durationMs = 450,
                    volume = 0.5f
                )
            }
            CharacterExpression.SURPRISE -> {
                // Energetic quick dual pitch sweep (laser style)
                playSynthesizedSweep(
                    startFreq = 400f,
                    endFreq = 1400f,
                    durationMs = 180,
                    volume = 0.4f
                )
            }
            CharacterExpression.THINKING -> {
                // Short double processor ticking sounds ("tic-tic")
                playToneSequence(
                    freqs = listOf(1100f, 1100f),
                    noteDurationMs = 20,
                    gapMs = 60,
                    volume = 0.3f
                )
            }
            CharacterExpression.LISTENING -> {
                // High-quality activator bell sequence
                playToneSequence(
                    freqs = listOf(740f, 987.77f),
                    noteDurationMs = 90,
                    gapMs = 15,
                    volume = 0.4f
                )
            }
            CharacterExpression.SPEAKING -> {
                // Quick double chirp
                playToneSequence(
                    freqs = listOf(600f, 850f),
                    noteDurationMs = 35,
                    gapMs = 10,
                    volume = 0.35f
                )
            }
            CharacterExpression.IDLE -> {
                // Neutral state (optional small blip or silence)
            }
        }

        // 2. Play the witty Turkish spoken phrase via TTS if enabled and not already speaking a main message
        if (isTtsEnabled && tts != null && expression != CharacterExpression.IDLE) {
            val isMainSpeechActive = tts.isSpeaking
            if (!isMainSpeechActive) {
                val phrases = getWittyPhrasesForEmotion(expression)
                if (phrases.isNotEmpty()) {
                    val randomPhrase = phrases.random()
                    // Apply voice parameters
                    val settings = SettingsManager.getInstance(context)
                    tts.setSpeechRate(settings.speechSpeed.value)
                    tts.setPitch(settings.speechPitch.value)
                    
                    val locale = when (settings.language.value) {
                        "Turkish" -> java.util.Locale("tr", "TR")
                        else -> java.util.Locale("tr", "TR")
                    }
                    tts.language = locale

                    // Speak the brief humorous comment
                    tts.speak(randomPhrase, TextToSpeech.QUEUE_FLUSH, null, "EMOTION_REACTION_ID")
                }
            }
        }
    }
}
