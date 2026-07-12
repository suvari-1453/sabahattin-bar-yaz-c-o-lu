package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun GundiVoiceWave(
    isSpeaking: Boolean,
    onMuteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isSpeaking,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF14151B).copy(alpha = 0.95f),
                            Color(0xFF0D0E12).copy(alpha = 0.9f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pulse Indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF1744))
                    )
                    
                    Text(
                        text = "Gundi Bro Konuşuyor...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                // Dedicated "SUSTUR" Button in the card
                androidx.compose.material3.Button(
                    onClick = onMuteClick,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF1744).copy(alpha = 0.25f),
                        contentColor = Color(0xFFFF1744)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "SUSTUR 🤫",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Equalizer Wave Canvas
            EqualizerWaveAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )
        }
    }
}

@Composable
fun EqualizerWaveAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    
    // Animate a phase that drives multiple sine waves or random heights
    val animationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Dynamic wave bar heights using the phase
    val barCount = 32
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = (width / barCount) * 0.6f
        val gap = (width / barCount) * 0.4f
        
        val colors = listOf(
            Color(0xFFFF1744), // Turkish Red
            Color(0xFFFF9100), // Orange/Gold
            Color(0xFF00E5FF), // Cyan
            Color(0xFF2979FF)  // Blue
        )

        for (i in 0 until barCount) {
            // Create organic speech-like waveform using combined sine waves
            val multiplier = sin(animationPhase + (i * 0.3f)) * 0.4f + 
                             sin(animationPhase * 2f - (i * 0.15f)) * 0.3f + 
                             0.3f
            
            val scale = (multiplier.coerceIn(0.1f, 1.0f))
            val barHeight = height * scale
            
            val x = i * (barWidth + gap) + (gap / 2)
            val startY = (height - barHeight) / 2
            val endY = startY + barHeight
            
            // Interpolate color based on position
            val colorIndex = (i.toFloat() / barCount * (colors.size - 1)).toInt()
            val nextColorIndex = (colorIndex + 1).coerceAtMost(colors.size - 1)
            val fraction = (i.toFloat() / barCount * (colors.size - 1)) - colorIndex
            val barColor = lerpColor(colors[colorIndex], colors[nextColorIndex], fraction)
            
            drawLine(
                color = barColor,
                start = Offset(x + barWidth / 2, startY),
                end = Offset(x + barWidth / 2, endY),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}
