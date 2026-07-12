package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmotionDashboardWidget(
    expression: CharacterExpression,
    soundLevel: Float,
    isLoading: Boolean,
    isListening: Boolean,
    isTtsSpeaking: Boolean,
    userSentimentExpression: CharacterExpression?,
    modifier: Modifier = Modifier,
    avatarStyle: String = "classic"
) {
    var showDialog by remember { mutableStateOf(false) }

    // Dynamic color styling based on the active expression
    val (emotionLabel, statusText, primaryColor, secondaryColor) = when (expression) {
        CharacterExpression.JOY -> Quadruple(
            "MUTLU & NEŞELİ 😊",
            "GUNDİ Bro şu an aşırı keyifli ve enerjik! Senin enerjin ona çok iyi geldi bro.",
            Color(0xFFFFD54F), // Amber
            Color(0xFFFFA726)  // Orange Accent
        )
        CharacterExpression.SADNESS -> Quadruple(
            "DUYGUSAL & DERTLİ 😢",
            "GUNDİ Bro dert ortaklığı modunda... Seni can kulağıyla dinliyor, üzülme bro.",
            Color(0xFF64B5F6), // Blue
            Color(0xFF42A5F5)  // Blue Accent
        )
        CharacterExpression.SURPRISE -> Quadruple(
            "HAYRETLER İÇİNDE 😲",
            "GUNDİ Bro şu an şokta! Duydukları karşısında ağzı açık kaldı diyebiliriz.",
            Color(0xFFFF8A65), // Coral/Orange
            Color(0xFFFF7043)  // Deep Orange
        )
        CharacterExpression.THINKING -> Quadruple(
            "DERİN DÜŞÜNCELİ 🧠",
            "GUNDİ Bro derin analizler yapıyor... Sana en kral cevabı bulmak için çalışıyor.",
            Color(0xFFBA68C8), // Purple
            Color(0xFF9C27B0)  // Deep Purple
        )
        CharacterExpression.LISTENING -> Quadruple(
            "SENİ DİNLİYOR 🎙️",
            "GUNDİ Bro pür dikkat sende. Kelimelerini analiz ediyor, sesini bekliyor.",
            Color(0xFF81C784), // Green
            Color(0xFF4CAF50)  // Deep Green
        )
        CharacterExpression.SPEAKING -> Quadruple(
            "KONUŞUYOR 💬",
            "GUNDİ Bro içtenlikle seslendiriyor... Onun samimi sesine kulak ver bro.",
            Color(0xFF4DB6AC), // Teal
            Color(0xFF009688)  // Deep Teal
        )
        else -> Quadruple(
            "SAKİN & DENGELİ 🧘‍♂️",
            "GUNDİ Bro dengeli ve huzurlu. Seninle her konuda laflamaya hazır bekliyor.",
            Color(0xFFE0E0E0), // Neutral Light
            Color(0xFF9E9E9E)  // Neutral Gray
        )
    }

    val animatedBgColor by animateColorAsState(
        targetValue = primaryColor.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 600),
        label = "bgColorAnim"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = primaryColor.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 600),
        label = "borderColorAnim"
    )

    // Animated meters for visual dashboard richness
    val empathyTarget = when (expression) {
        CharacterExpression.JOY -> 0.95f
        CharacterExpression.SADNESS -> 0.99f
        CharacterExpression.SURPRISE -> 0.90f
        else -> 0.92f
    }
    val energyTarget = when {
        isListening || isTtsSpeaking -> 0.98f
        isLoading -> 0.85f
        expression == CharacterExpression.JOY -> 0.96f
        expression == CharacterExpression.SADNESS -> 0.50f
        else -> 0.75f
    }
    val harmonyTarget = when (expression) {
        CharacterExpression.SADNESS -> 0.95f
        CharacterExpression.JOY -> 0.92f
        else -> 0.88f
    }

    val animatedEmpathy by animateFloatAsState(targetValue = empathyTarget, animationSpec = tween(1000), label = "empathy")
    val animatedEnergy by animateFloatAsState(targetValue = energyTarget, animationSpec = tween(1000), label = "energy")
    val animatedHarmony by animateFloatAsState(targetValue = harmonyTarget, animationSpec = tween(1000), label = "harmony")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBgColor)
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, animatedBorderColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Character Emoji
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor.copy(alpha = 0.12f))
                        .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CuteExpressionCharacter(
                        expression = expression,
                        soundLevel = soundLevel,
                        modifier = Modifier.fillMaxSize(),
                        avatarStyle = avatarStyle
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Emotion Stats & Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GUNDİ BRO",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFFFD54F),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DUYGU PANOSU",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Bilgi",
                            modifier = Modifier.size(14.dp),
                            tint = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = emotionLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Miniature Animated Meters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MeterColumn("Empati", animatedEmpathy, primaryColor, Modifier.weight(1f))
                        MeterColumn("Enerji", animatedEnergy, secondaryColor, Modifier.weight(1f))
                        MeterColumn("Uyum", animatedHarmony, primaryColor, Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GUNDİ Bro Kalibrasyonu")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Ben GUNDİ Bro! Tamamen yerli ve milli bir dijital canım dostunum. Konuştuğumuz konuları kelime kelime analiz ederek ruh halimi anlık güncellerim.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Şu anki Duygusal Veriler:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InteractiveStatRow("Ruh Hali:", emotionLabel)
                    InteractiveStatRow("Empati Katsayısı:", "%${(animatedEmpathy * 100).toInt()}")
                    InteractiveStatRow("İşlem Enerjisi:", "%${(animatedEnergy * 100).toInt()}")
                    InteractiveStatRow("Uyum Seviyesi:", "%${(animatedHarmony * 100).toInt()}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Eyvallah Bro", color = primaryColor)
                }
            }
        )
    }
}

@Composable
fun MeterColumn(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "%${(value * 100).toInt()}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = value,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun InteractiveStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
