package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SocialMediaAnalyzerState {
    var showAnalyzer by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMediaAnalyzerDialog(
    onDismiss: () -> Unit
) {
    var postText by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("Instagram") }
    var selectedTone by remember { mutableStateOf("Samimi & Eğlenceli") }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val platforms = listOf("Instagram", "X (Twitter)", "YouTube", "LinkedIn", "TikTok")
    val tones = listOf("Samimi & Eğlenceli", "Profesyonel & Kurumsal", "Heyecanlı & Dikkat Çekici", "Bilgilendirici & Net", "Mizahi & İronik")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF16131C),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sosyal Medya Analizcisi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.LightGray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    if (analysisResult == null) {
                        // 1. INPUT FORM STATE
                        Text(
                            text = "Analiz Edilecek Paylaşım Metni veya Açıklama",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = { Text("Sosyal medyada paylaşmak istediğin açıklamayı buraya yaz veya yapıştır...", color = Color.Gray.copy(alpha = 0.7f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Templates Selection
                        Text(
                            text = "Hızlı Şablonlar",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val templates = listOf(
                                "🚀 Teknoloji Ürünü Lansmanı" to "Yeni akıllı saatimiz bugün piyasaya çıktı! Gelişmiş yapay zeka koçu, kalp ritmi takibi ve 1 haftalık pil ömrüyle hayatınızı değiştirecek. Sınırlı sayıdaki ön sipariş fırsatını kaçırmayın!",
                                "🍕 Gurme Yemek Postu" to "Çıtır çıtır taş fırın pizzamızın sırrı 48 saat mayalanan özel hamurunda gizli! Bugün kendinizi şımartmak için şubemize gelin veya hemen sipariş verin.",
                                "💡 Kariyer Tavsiyesi" to "Yazılım sektöründe başarılı olmanın ilk adımı sürekli öğrenmek! Bugün junior yazılımcılara 5 altın tavsiye paylaşıyorum. Kaydetmeyi unutmayın!"
                            )
                            templates.forEach { (label, content) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                        .clickable { postText = content }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Platform Selector
                        Text(
                            text = "Platform Seç",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            platforms.forEach { platform ->
                                val isSelected = selectedPlatform == platform
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        .clickable { selectedPlatform = platform }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = platform,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Tone Selector
                        Text(
                            text = "Hedeflenen Ton / Stil",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val subsetTones = tones.take(3)
                            subsetTones.forEach { tone ->
                                val isSelected = selectedTone == tone
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        .clickable { selectedTone = tone }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tone.split(" ")[0],
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (postText.isBlank()) {
                                    errorMessage = "Lütfen önce analiz edilecek bir paylaşım metni girin."
                                    return@Button
                                }
                                errorMessage = null
                                analyzing = true
                                coroutineScope.launch {
                                    val result = performSocialMediaAnalysis(postText, selectedPlatform, selectedTone, context)
                                    analyzing = false
                                    if (result != null) {
                                        analysisResult = result
                                    } else {
                                        errorMessage = "Analiz yapılırken bir hata oluştu. Lütfen API anahtarını kontrol et."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !analyzing
                        ) {
                            if (analyzing) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Analiz Yapılıyor...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Yapay Zeka ile Analiz Et", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // 2. RESULTS STATE
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📊 YAPAY ZEKA ANALİZ RAPORU",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = selectedPlatform,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = analysisResult ?: "",
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    analysisResult = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Text("Yeni Analiz", color = Color.White)
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Kapat", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun performSocialMediaAnalysis(text: String, platform: String, tone: String, context: android.content.Context): String? = withContext(Dispatchers.IO) {
    val apiKey = SettingsManager.getInstance(context).getActiveApiKey()
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
        return@withContext null
    }

    val prompt = """
        Sen uzman bir Sosyal Medya Analisti, Stratejisti ve Metin Yazarısın. 
        Lütfen aşağıdaki paylaşım metnini analiz et ve iyileştirme önerileri sun.
        
        Platform: $platform
        Hedeflenen Üslup/Ton: $tone
        
        Analiz edilecek içerik:
        \"\"\"
        $text
        \"\"\"
        
        Lütfen aşağıdaki 6 bölümden oluşan profesyonel bir analiz raporu sun. Her bölümün başlığını kalın yaz ve aralara okumayı kolaylaştıran emojiler yerleştir. Her şeyi akıcı ve Türkçe olarak sun:
        
        1. 📌 **Genel Değerlendirme & Güçlü Yanlar**: İçeriğin ilk bakışta hissettirdikleri ve başarılı yönleri.
        2. 🎭 **Ton ve Duygu Durumu Uygunluğu**: İçeriğin hedeflenen '$tone' tonuna ne kadar yaklaştığı.
        3. 📈 **Etkileşim & Erişim Potansiyeli**: Bu paylaşımın beğeni, kaydetme ve yorum oranlarını artırma ihtimali.
        4. 🎯 **Hedef Kitle Eşleşmesi**: Bu içeriğin hangi demografik/ilgi grubuna ulaştığı.
        5. 💡 **İyileştirme ve Optimizasyon Tavsiyeleri**: İçeriği daha da patlatmak için net, uygulanabilir cümle tavsiyeleri (varsa alternatif başlık önerisi).
        6. 🏷️ **Trend Etiket (Hashtag) Önerileri**: Erişim hacmini katlayacak 5-7 adet nokta atışı trend etiket önerisi.
    """.trimIndent()

    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        systemInstruction = Content(parts = listOf(Part(text = "Sen her zaman Türkçe yanıt veren, profesyonel, yapıcı ve vizyoner bir sosyal medya danışmanısın.")))
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
