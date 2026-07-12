package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReactiveGundiEmojis(
    text: String,
    humorLevel: GundiHumorLevel,
    modifier: Modifier = Modifier
) {
    // 1. Determine which emojis to show based on text content & humor level
    val emojis = remember(text, humorLevel) {
        val list = mutableListOf<String>()
        val lowercaseText = text.lowercase(java.util.Locale("tr", "TR"))
        
        // Context-aware checks
        if (lowercaseText.contains("?") || lowercaseText.contains("ne ") || lowercaseText.contains("neden") || lowercaseText.contains("nasıl")) {
            list.add("🤔")
        }
        if (lowercaseText.contains("çay") || lowercaseText.contains("cay") || lowercaseText.contains("kahve") || lowercaseText.contains("baddi")) {
            list.add("☕")
        }
        if (lowercaseText.contains("kral") || lowercaseText.contains("başkan") || lowercaseText.contains("karizma") || lowercaseText.contains("bro")) {
            list.add("👑")
        }
        if (lowercaseText.contains("görsel") || lowercaseText.contains("resim") || lowercaseText.contains("çiz") || lowercaseText.contains("boya") || lowercaseText.contains("sanat")) {
            list.add("🎨")
        }
        if (lowercaseText.contains("sır") || lowercaseText.contains("gizli") || lowercaseText.contains("şifre") || lowercaseText.contains("panel")) {
            list.add("🤫")
        }
        if (lowercaseText.contains("roket") || lowercaseText.contains("uç") || lowercaseText.contains("tavan") || lowercaseText.contains("kop")) {
            list.add("🚀")
        }
        if (lowercaseText.contains("para") || lowercaseText.contains("dolar") || lowercaseText.contains("euro") || lowercaseText.contains("zengin")) {
            list.add("💵")
        }
        if (lowercaseText.contains("müzik") || lowercaseText.contains("şarkı") || lowercaseText.contains("ses") || lowercaseText.contains("dinle")) {
            list.add("🎵")
        }

        // Fill remaining or default slots based on humor level
        if (list.size < 3) {
            val defaults = when (humorLevel) {
                GundiHumorLevel.COK_ESPRILI -> listOf("😂", "🤣", "🤪", "😜", "🔥")
                GundiHumorLevel.KOMIK -> listOf("😎", "😏", "😅", "😉", "🤙")
                GundiHumorLevel.CIDDI -> listOf("🧠", "💼", "🧐", "📝", "💪")
            }
            for (emoji in defaults) {
                if (!list.contains(emoji)) {
                    list.add(emoji)
                }
                if (list.size >= 3) break
            }
        }
        list.take(3)
    }

    // 2. Setup Infinite Transition for playful animations
    val infiniteTransition = rememberInfiniteTransition(label = "reactive_emojis")
    
    // Wave/Float offset animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_float"
    )

    // Pulse/Scale animation
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_scale"
    )

    // Rotation animation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_rotate"
    )

    Row(
        modifier = modifier
            .offset(y = offsetY.dp)
            .background(
                color = Color(0xFF1E1F28).copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = when (humorLevel) {
                    GundiHumorLevel.COK_ESPRILI -> Color(0xFF00E5FF).copy(alpha = 0.6f)
                    GundiHumorLevel.KOMIK -> Color(0xFFFFD54F).copy(alpha = 0.6f)
                    GundiHumorLevel.CIDDI -> Color(0xFF90A4AE).copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEachIndexed { index, emoji ->
            // Distribute animation types among the 3 emojis to make them move independently!
            val emojiModifier = when (index) {
                0 -> Modifier.graphicsLayer { 
                    translationY = offsetY * 1.5f 
                }
                1 -> Modifier.graphicsLayer { 
                    rotationZ = rotationAngle 
                }
                else -> Modifier.scale(scaleFactor)
            }
            
            Text(
                text = emoji,
                fontSize = 14.sp,
                modifier = emojiModifier
            )
        }
    }
}
