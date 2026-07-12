package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoiceReactiveOrb(
    isListening: Boolean,
    soundLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    
    // Constant breathing/pulsing effect for idle/listening states
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_scale"
    )
    
    // Smooth continuous rotation of the sweep gradient highlights
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Normalize sound level (usually -2 to 10+ dB in speech recognizer)
    val normalizedSound = ((soundLevel + 2f) / 14f).coerceIn(0f, 1f)
    val animatedSoundLevel by animateFloatAsState(
        targetValue = if (isListening) normalizedSound else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sound_level"
    )

    // Dynamic scale based on voice volume
    val scale = if (isListening) {
        (1.0f + animatedSoundLevel * 0.9f) * idleScale
    } else {
        idleScale
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer glow 1: Broad soft red glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF1744).copy(alpha = if (isListening) 0.5f else 0.25f),
                            Color(0xFFD50000).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Outer glow 2: Sweeping neon/laser aura (Red, White, and Gold accents)
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .graphicsLayer { rotationZ = rotation }
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFF1744), // Turkish flag Red
                            Color(0xFFFFFFFF), // Turkish flag White
                            Color(0xFFFFD700), // Gold Highlight
                            Color(0xFFFF1744)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Mid ring: Dark red translucent shield separating the core
        Box(
            modifier = Modifier
                .fillMaxSize(0.78f)
                .background(Color(0xFF1E1B1B), CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        )

        // Core: High-density radial red gradient sphere containing the crescent and star
        Box(
            modifier = Modifier
                .fillMaxSize(0.65f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFD50000)
                        )
                    ),
                    shape = CircleShape
                )
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Crescent & Star Turkish motif
                Text(
                    text = "🌙⭐",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
