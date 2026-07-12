package com.example

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VoiceToneSelector(
    tts: TextToSpeech?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    
    val voiceStyle by settingsManager.voiceStyle.collectAsState()
    val speechPitch by settingsManager.speechPitch.collectAsState()
    val speechSpeed by settingsManager.speechSpeed.collectAsState()
    val isTtsEnabled by settingsManager.isTtsEnabled.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isTestingVoice by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    // Preset configurations
    val voicePresets = listOf(
        VoiceTonePreset("classic", "Klasik 💬", "Doğal ve samimi Gündi tonu", 1.0f, 1.0f, Color(0xFFFFD54F)),
        VoiceTonePreset("excited", "Enerjik 🚀", "Deli dolu, heyecanlı ve hızlı", 1.15f, 1.35f, Color(0xFFEC407A)),
        VoiceTonePreset("deep", "Sakin 🧘", "Tok, babacan, huzurlu ve ağır", 0.75f, 0.85f, Color(0xFF4FC3F7)),
        VoiceTonePreset("squeaky", "Tiz/Bebek 👶", "Tiz, eğlenceli ve hareketli", 1.45f, 1.15f, Color(0xFF81C784)),
        VoiceTonePreset("robotic", "Siber Gündi 🤖", "Gelecekten gelen metalik ses", 0.60f, 1.00f, Color(0xFF00E676))
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16131D)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFD54F).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Tone Icon",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Gündi Ses Tonu Seçici",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Web Speech & TTS Enerji Ayarı",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Info Card about Speech API state
            if (!isTtsEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF1744).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Volume Off",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sesli okuma şu an kapalı bro! Ayarlardan aktif edebilirsin.",
                            color = Color(0xFFFF8A80),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Presets Horizontal Row
            Text(
                text = "Ses Enerjisi Seçin:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_presets_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(voicePresets) { preset ->
                    val isSelected = voiceStyle == preset.id
                    val selectedColor = preset.color

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(105.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) selectedColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) selectedColor else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                settingsManager.setVoiceStyle(preset.id)
                            }
                            .padding(10.dp)
                    ) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color.LightGray
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = if (preset.id == "deep") "Daha Sakin" else if (preset.id == "excited") "Daha Enerjik" else "Standart",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light,
                            color = if (isSelected) selectedColor else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Preset Description Text
            voicePresets.find { it.id == voiceStyle }?.let { activePreset ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = activePreset.color.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = activePreset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: TEST VOICE & ADVANCED TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Test Sound Button
                Button(
                    onClick = {
                        if (tts != null) {
                            coroutineScope.launch {
                                isTestingVoice = true
                                // Set current style parameters onto TTS
                                tts.setPitch(speechPitch)
                                tts.setSpeechRate(speechSpeed)
                                
                                val testPhrases = when (voiceStyle) {
                                    "excited" -> listOf(
                                        "Ooo bro! Roket gibiyim roket! Enerji tavan yaptı kral!",
                                        "Lan baddi! Nasıl sesim ama? Fişek gibiyim fişek!"
                                    )
                                    "deep" -> listOf(
                                        "Sakin ol bro, hayat güzel. Derin bir nefes al...",
                                        "Eyvallah babacan, sesim gayet sakin ve huzurlu."
                                    )
                                    "squeaky" -> listOf(
                                        "Uyy canını yediğim! Çok sevimli konuşuyom sanki!",
                                        "Bak şimdi nasıl konuşuyorum tiz tiz cik cik!"
                                    )
                                    "robotic" -> listOf(
                                        "Ben Gundi 2.0. Gelecekten geliyorum kanka.",
                                        "Siber dünya çok garip bro. Robotlaştım iyice."
                                    )
                                    else -> listOf(
                                        "Selam baddi, sesimi beğendin mi? Klasik Gündi tarzı!",
                                        "Eyvallah bro, ses tonumu değiştirdin, yakıştı bence!"
                                    )
                                }
                                val phraseToSpeak = testPhrases.random()
                                tts.speak(phraseToSpeak, TextToSpeech.QUEUE_FLUSH, null, "TEST_TTS_ID")
                                delay(2000)
                                isTestingVoice = false
                            }
                        } else {
                            Toast.makeText(context, "TTS motoru hazır değil kanka!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isTestingVoice
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTestingVoice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Test Voice",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (isTestingVoice) "Konuşuyor..." else "Ses Testi 🔊",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Advanced Controls Toggle
                IconButton(
                    onClick = { showAdvancedSettings = !showAdvancedSettings },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (showAdvancedSettings) Icons.Default.SettingsSuggest else Icons.Default.Tune,
                        contentDescription = "Hassas Ayar",
                        tint = if (showAdvancedSettings) Color(0xFFFFD54F) else Color.White
                    )
                }
            }

            // Collapsible Advanced Tuning Panel (Sliders)
            AnimatedVisibility(
                visible = showAdvancedSettings,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "İnce Ses Tonu Ayarları (Manuel)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ses Tizliği (Pitch)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = "${String.format("%.2f", speechPitch)}x",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = { settingsManager.setSpeechPitch(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFFFD54F),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Konuşma Hızı (Speed)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = "${String.format("%.2f", speechSpeed)}x",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                    Slider(
                        value = speechSpeed,
                        onValueChange = { settingsManager.setSpeechSpeed(it) },
                        valueRange = 0.5f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E676),
                            activeTrackColor = Color(0xFF00E676),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}

data class VoiceTonePreset(
    val id: String,
    val label: String,
    val description: String,
    val pitch: Float,
    val speed: Float,
    val color: Color
)
