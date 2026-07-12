package com.example

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object ScreenReaderState {
    var isActive by mutableStateOf(false)
    var spokenText by mutableStateOf("")
    
    fun speak(context: Context, tts: TextToSpeech?, text: String) {
        if (tts == null) return
        spokenText = text
        isActive = true
        val cleanSpeech = cleanTextForTts(text)
        tts.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, "SCREEN_READER_ID")
        BubbleStateManager.updateSpeech(cleanSpeech)
        
        // Monitor speaking state to deactivate scanner automatically when finished
        Thread {
            while (tts.isSpeaking) {
                try { Thread.sleep(300) } catch (e: Exception) {}
            }
            isActive = false
        }.start()
    }

    fun stop(tts: TextToSpeech?) {
        tts?.stop()
        isActive = false
        spokenText = ""
    }
}

@Composable
fun ScreenScannerAnimation() {
    if (!ScreenReaderState.isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "Scanner")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Sweep"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxHeightPx = maxHeight
        val yOffset = maxHeightPx * sweepProgress

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = yOffset)
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF3B82F6), // Futuristic blue
                            Color(0xFF10B981), // Turquoise green
                            Color(0xFF3B82F6),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// Generates and reads screen narration content in natural Turkish
fun readScreenContext(context: Context, tts: TextToSpeech?, screenType: String, chatMessages: List<com.example.ChatMessage> = emptyList()) {
    val builder = java.lang.StringBuilder()
    
    when (screenType) {
        "chat" -> {
            builder.append("Dost A-I sohbet ekranı okuyucusu devrede. ")
            builder.append("Bu ekranda dijital yapay zeka asistanınız Dost A-I ile sesli ve yazılı görüşmeler yapabilirsiniz. ")
            if (chatMessages.isEmpty()) {
                builder.append("Şu anda aktif sohbet geçmişiniz boş görünüyor. Aşağıdaki metin kutusunu veya ses kayıt mikrofonunu kullanarak yeni bir sohbet başlatabilirsiniz. ")
            } else {
                builder.append("Mevcut sohbet oturumunda toplam ${chatMessages.size} adet mesaj bulunuyor. ")
                val userCount = chatMessages.count { it.isUser }
                val aiCount = chatMessages.count { !it.isUser }
                builder.append("Bu mesajlardan ${userCount} tanesini siz gönderdiniz, ${aiCount} tanesi ise asistanınızın cevaplarıdır. ")
                
                // Read the last message as highlights
                val lastMsg = chatMessages.last()
                val sender = if (lastMsg.isUser) "Sizin son iletiniz: " else "Asistanınızın son cevabı: "
                builder.append("En son gelen ileti okunuyor. $sender. ${lastMsg.text}. ")
            }
            builder.append("Ekranın üst kısmında menü, sesli okuma ve profil tuşları yer almaktadır.")
        }
        "profile" -> {
            builder.append("Dost A-I entegrasyon ve profil paneli okuyucusu devrede. ")
            builder.append("Bu ekrandan cihazınızdaki dosya, medya ve sosyal medya hesaplarınızın entegrasyonlarını kontrol edebilirsiniz. ")
            builder.append("Ekranın üst kısmında üç adet ana sekme bulunmaktadır: Kimlik sekmesi, Dosya ve Medya sekmesi ve Sosyal Ağlar sekmesi. ")
            builder.append("Sosyal ağlar sekmesinden WhatsApp, Instagram, Facebook ve YouTube entegrasyonlarını sağlayıp paylaşımlar yapabilirsiniz. ")
            builder.append("Dosya ve medya sekmesinden cihaz belleğindeki resim, MP-3 ses ve MP-4 video dosyalarını tarayabilir ve mini medya oynatıcıda oynatabilirsiniz.")
        }
        else -> {
            builder.append("Dost A-I uygulaması ekran okuyucu aktif. Ekran içeriği algılanıyor.")
        }
    }
    
    ScreenReaderState.speak(context, tts, builder.toString())
}
