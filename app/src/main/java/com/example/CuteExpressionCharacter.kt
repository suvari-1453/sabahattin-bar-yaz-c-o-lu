package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background

enum class CharacterExpression {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    JOY,
    SADNESS,
    SURPRISE
}

@Composable
fun CuteExpressionCharacter(
    expression: CharacterExpression,
    soundLevel: Float, // Used to animate the mouth dynamically during speaking/listening
    modifier: Modifier = Modifier.size(120.dp),
    avatarStyle: String = "classic"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "minion_animations")

    // Gentle floating/breathing animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Periodic blinking animation for eyes (occurs every few seconds)
    val blinkProgress by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3500
                1.0f at 0 // Open
                1.0f at 3100 // Open
                0.0f at 3200 // Closed
                1.0f at 3300 // Open
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blinking"
    )

    // Thinking eye roll offset (moves left and right when thinking)
    val thinkingEyeXOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking_eye"
    )

    // Happy bouncing animation for JOY
    val joyBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "joy_bounce"
    )

    // Shaking animation for SURPRISE
    val surpriseShake by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(70, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "surprise_shake"
    )

    // Sliding teardrop animation for SADNESS
    val sadTeardropY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "sad_teardrop"
    )

    // Speech/sound level multiplier for talking mouth opening size
    val normalizedSound = ((soundLevel + 2f) / 14f).coerceIn(0f, 1f)
    val speechMouthScale by animateFloatAsState(
        targetValue = if (expression == CharacterExpression.SPEAKING) 0.3f + normalizedSound * 0.7f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "mouth_scale"
    )

    val finalOffsetY = when (expression) {
        CharacterExpression.JOY -> offsetY + joyBounce
        CharacterExpression.SADNESS -> offsetY + 1f
        else -> offsetY
    }

    val finalOffsetX = when (expression) {
        CharacterExpression.SURPRISE -> surpriseShake
        else -> 0f
    }

    if (avatarStyle == "custom" || avatarStyle == "uploaded" || avatarStyle == "bymix") {
        val speakingScale = if (expression == CharacterExpression.SPEAKING) {
            1.0f + speechMouthScale * 0.12f
        } else if (expression == CharacterExpression.LISTENING) {
            1.0f + ((soundLevel).coerceIn(0f, 15f) / 15f) * 0.1f
        } else {
            1.0f
        }
        val customRotation = if (expression == CharacterExpression.THINKING) {
            thinkingEyeXOffset * 1.5f
        } else {
            0f
        }
        val customBorderColor = when (expression) {
            CharacterExpression.JOY -> Color(0xFFFFD54F) // Yellow glow
            CharacterExpression.SADNESS -> Color(0xFF64B5F6) // Blue glow
            CharacterExpression.SURPRISE -> Color(0xFFFF8A65) // Orange glow
            CharacterExpression.THINKING -> Color(0xFFBA68C8) // Purple glow
            CharacterExpression.LISTENING -> Color(0xFF81C784) // Green glow
            CharacterExpression.SPEAKING -> Color(0xFF4DB6AC) // Teal glow
            else -> Color.Transparent
        }
        val customBorderWidth = if (customBorderColor != Color.Transparent) 3.dp else 0.dp
        
        Box(
            modifier = modifier
                .offset(x = finalOffsetX.dp, y = finalOffsetY.dp)
                .scale(speakingScale)
                .graphicsLayer {
                    rotationZ = customRotation
                }
                .clip(CircleShape)
                .background(Color(0xFF1E1F24)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            val baseImageModifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
            val finalImageModifier = if (customBorderWidth > 0.dp) {
                baseImageModifier.border(customBorderWidth, customBorderColor, CircleShape)
            } else {
                baseImageModifier
            }

            val imageResId = if (avatarStyle == "bymix") {
                com.example.R.drawable.gundi_app_icon_1784149123318
            } else {
                com.example.R.drawable.gundi_app_icon_1784149123318
            }

            Image(
                painter = androidx.compose.ui.res.painterResource(id = imageResId),
                contentDescription = "Gundi Özel",
                modifier = finalImageModifier,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            // Add custom visual reaction markers depending on expression
            if (expression == CharacterExpression.SADNESS) {
                // Let's add a crying emoji or water droplets overlay
                androidx.compose.material3.Text(
                    text = "💧",
                    fontSize = 24.sp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.CenterEnd)
                        .offset(x = (-12).dp, y = (sadTeardropY - 10).dp)
                )
            }
            if (expression == CharacterExpression.JOY) {
                androidx.compose.material3.Text(
                    text = "✨",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .offset(x = 10.dp, y = 10.dp)
                )
            }
            if (expression == CharacterExpression.THINKING) {
                androidx.compose.material3.Text(
                    text = "🤔",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 4.dp)
                )
            }
            if (expression == CharacterExpression.SURPRISE) {
                androidx.compose.material3.Text(
                    text = "💥",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .offset(x = 6.dp, y = 4.dp)
                )
            }
            if (expression == CharacterExpression.SPEAKING) {
                androidx.compose.material3.Text(
                    text = "💬",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                )
            }
            if (expression == CharacterExpression.LISTENING) {
                androidx.compose.material3.Text(
                    text = "🎙️",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomStart)
                        .offset(x = 4.dp, y = (-4).dp)
                )
            }
        }
    } else {
        Canvas(
            modifier = modifier
                .offset(x = finalOffsetX.dp, y = finalOffsetY.dp)
        ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Minion body base colors
        val yellowBodyColor = when (avatarStyle) {
            "cool" -> Color(0xFF4FC3F7) // Cool Light Blue Minion
            "sultan" -> Color(0xFFFFD54F) // Classic Golden Yellow Minion
            "cyber" -> Color(0xFF00E676) // Cyber Neon Green Minion
            "gamer" -> Color(0xFFEC407A) // Gamer Neon Pink Minion
            else -> Color(0xFFFFD54F) // Classic Golden Yellow Minion
        }
        
        val yellowShadowColor = when (avatarStyle) {
            "cool" -> Color(0xFF0288D1)
            "cyber" -> Color(0xFF00C853)
            "gamer" -> Color(0xFFAD1457)
            else -> Color(0xFFF57F17) // Classic Minion shadow (deep dark golden yellow)
        }

        // 1. HAIR (Sparse cute black strands on head)
        val hairPositions = listOf(-15f, -8f, 0f, 8f, 15f)
        hairPositions.forEach { offset ->
            val hairPath = Path().apply {
                moveTo(centerX + offset, centerY - 48f)
                quadraticTo(
                    centerX + offset * 1.3f, centerY - 62f,
                    centerX + offset * 1.5f + (sin(offsetY) * 2f), centerY - 68f
                )
            }
            drawPath(
                path = hairPath,
                color = Color(0xFF263238),
                style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }

        // 2. CAPSULE BODY (Traditional Minion capsule shape)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(yellowBodyColor, yellowShadowColor),
                startY = centerY - 50f,
                endY = centerY + 50f
            ),
            topLeft = Offset(centerX - 36f, centerY - 48f),
            size = Size(72f, 96f),
            cornerRadius = CornerRadius(36f, 36f)
        )

        // 3. DENIM OVERALLS (Bottom blue suit with straps)
        val denimColor = Color(0xFF1565C0) // Classic denim blue
        val denimDarkColor = Color(0xFF0D47A1) // Deep blue shadow
        
        // Overalls main bottom block
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(denimColor, denimDarkColor)
            ),
            topLeft = Offset(centerX - 36f, centerY + 16f),
            size = Size(72f, 32f),
            cornerRadius = CornerRadius(x = 0f, y = 0f)
        )
        // Overalls front bib block
        drawRoundRect(
            color = denimColor,
            topLeft = Offset(centerX - 24f, centerY),
            size = Size(48f, 20f),
            cornerRadius = CornerRadius(x = 4f, y = 4f)
        )

        // Overalls straps
        // Left Strap
        val leftStrapPath = Path().apply {
            moveTo(centerX - 36f, centerY - 8f)
            lineTo(centerX - 24f, centerY + 4f)
            lineTo(centerX - 18f, centerY + 2f)
            lineTo(centerX - 32f, centerY - 10f)
            close()
        }
        drawPath(path = leftStrapPath, color = denimDarkColor)
        
        // Right Strap
        val rightStrapPath = Path().apply {
            moveTo(centerX + 36f, centerY - 8f)
            lineTo(centerX + 24f, centerY + 4f)
            lineTo(centerX + 18f, centerY + 2f)
            lineTo(centerX + 32f, centerY - 10f)
            close()
        }
        drawPath(path = rightStrapPath, color = denimDarkColor)

        // Small black overall buttons
        drawCircle(color = Color(0xFF212121), radius = 2.5f, center = Offset(centerX - 20f, centerY + 4f))
        drawCircle(color = Color(0xFF212121), radius = 2.5f, center = Offset(centerX + 20f, centerY + 4f))

        // Cute pocket logo in the center of the overalls
        drawRoundRect(
            color = denimDarkColor,
            topLeft = Offset(centerX - 8f, centerY + 6f),
            size = Size(16f, 12f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Group logo dot
        drawCircle(color = Color(0xFF212121), radius = 2f, center = Offset(centerX, centerY + 12f))

        // 4. GOGGLES STRAP (Black band wrapped around the head)
        drawRect(
            color = Color(0xFF212121),
            topLeft = Offset(centerX - 36f, centerY - 24f),
            size = Size(72f, 10f)
        )

        // 5. GOGGLES (Silver/Grey metallic circular frames)
        val metalOuterColor = Color(0xFFCFD8DC)
        val metalInnerColor = Color(0xFF78909C)
        val leftEyeCenter = Offset(centerX - 15f, centerY - 20f)
        val rightEyeCenter = Offset(centerX + 15f, centerY - 20f)
        val goggleRadius = 16f
        val eyeRadius = 12f

        // Draw Left metallic frame
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(metalOuterColor, metalInnerColor)
            ),
            radius = goggleRadius,
            center = leftEyeCenter
        )
        // Draw Right metallic frame
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(metalOuterColor, metalInnerColor)
            ),
            radius = goggleRadius,
            center = rightEyeCenter
        )

        // Draw Eye background (White)
        drawCircle(color = Color.White, radius = eyeRadius, center = leftEyeCenter)
        drawCircle(color = Color.White, radius = eyeRadius, center = rightEyeCenter)

        // 6. EYES & EXPRESSION ANATOMY (Pupils, Blinking, Tears)
        val irisColor = Color(0xFF8D6E63) // Minion brown eyes
        val pupilColor = Color(0xFF212121)

        val eyeYOffset = when (expression) {
            CharacterExpression.THINKING -> -2f
            else -> 0f
        }
        val eyeXOffset = when (expression) {
            CharacterExpression.THINKING -> thinkingEyeXOffset
            else -> 0f
        }

        // Render pupils depending on expression state
        when (expression) {
            CharacterExpression.JOY -> {
                // Happy closed/laughing arches
                val happyPathLeft = Path().apply {
                    moveTo(leftEyeCenter.x - 8f, leftEyeCenter.y + 2f)
                    quadraticTo(leftEyeCenter.x, leftEyeCenter.y - 6f, leftEyeCenter.x + 8f, leftEyeCenter.y + 2f)
                }
                val happyPathRight = Path().apply {
                    moveTo(rightEyeCenter.x - 8f, rightEyeCenter.y + 2f)
                    quadraticTo(rightEyeCenter.x, rightEyeCenter.y - 6f, rightEyeCenter.x + 8f, rightEyeCenter.y + 2f)
                }
                drawPath(happyPathLeft, color = pupilColor, style = Stroke(width = 3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(happyPathRight, color = pupilColor, style = Stroke(width = 3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            CharacterExpression.SADNESS -> {
                // Sad crying droopy eyes
                val sadPathLeft = Path().apply {
                    moveTo(leftEyeCenter.x - 8f, leftEyeCenter.y - 4f)
                    quadraticTo(leftEyeCenter.x, leftEyeCenter.y + 2f, leftEyeCenter.x + 8f, leftEyeCenter.y - 4f)
                }
                val sadPathRight = Path().apply {
                    moveTo(rightEyeCenter.x - 8f, rightEyeCenter.y - 4f)
                    quadraticTo(rightEyeCenter.x, rightEyeCenter.y + 2f, rightEyeCenter.x + 8f, rightEyeCenter.y - 4f)
                }
                drawPath(sadPathLeft, color = pupilColor, style = Stroke(width = 3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(sadPathRight, color = pupilColor, style = Stroke(width = 3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

                // Teardrop falling down
                if (sadTeardropY < 24f) {
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 3.5f,
                        center = Offset(leftEyeCenter.x, leftEyeCenter.y + 4f + sadTeardropY)
                    )
                }
            }
            else -> {
                // Standard blinking or standard open eyes
                val finalEyeHeight = eyeRadius * 2f * blinkProgress
                if (blinkProgress > 0.15f) {
                    // Left Iris and Pupil
                    drawCircle(color = irisColor, radius = 5f, center = Offset(leftEyeCenter.x + eyeXOffset, leftEyeCenter.y + eyeYOffset))
                    drawCircle(color = pupilColor, radius = 2.5f, center = Offset(leftEyeCenter.x + eyeXOffset, leftEyeCenter.y + eyeYOffset))
                    drawCircle(color = Color.White, radius = 1f, center = Offset(leftEyeCenter.x + eyeXOffset - 1.5f, leftEyeCenter.y + eyeYOffset - 1.5f)) // shine

                    // Right Iris and Pupil
                    drawCircle(color = irisColor, radius = 5f, center = Offset(rightEyeCenter.x + eyeXOffset, rightEyeCenter.y + eyeYOffset))
                    drawCircle(color = pupilColor, radius = 2.5f, center = Offset(rightEyeCenter.x + eyeXOffset, rightEyeCenter.y + eyeYOffset))
                    drawCircle(color = Color.White, radius = 1f, center = Offset(rightEyeCenter.x + eyeXOffset - 1.5f, rightEyeCenter.y + eyeYOffset - 1.5f)) // shine

                    // Blinking eyelid overlay
                    if (blinkProgress < 0.95f) {
                        val eyelidHeight = eyeRadius * 2f * (1f - blinkProgress)
                        drawRect(
                            color = yellowBodyColor,
                            topLeft = Offset(leftEyeCenter.x - eyeRadius, leftEyeCenter.y - eyeRadius),
                            size = Size(eyeRadius * 2f, eyelidHeight)
                        )
                        drawRect(
                            color = yellowBodyColor,
                            topLeft = Offset(rightEyeCenter.x - eyeRadius, rightEyeCenter.y - eyeRadius),
                            size = Size(eyeRadius * 2f, eyelidHeight)
                        )
                    }
                } else {
                    // Closed sleepy/blink lines
                    drawLine(
                        color = pupilColor,
                        start = Offset(leftEyeCenter.x - 7f, leftEyeCenter.y),
                        end = Offset(leftEyeCenter.x + 7f, leftEyeCenter.y),
                        strokeWidth = 3f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = pupilColor,
                        start = Offset(rightEyeCenter.x - 7f, rightEyeCenter.y),
                        end = Offset(rightEyeCenter.x + 7f, rightEyeCenter.y),
                        strokeWidth = 3f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // Rosy cheeks
        drawCircle(color = Color(0xFFFF8A80).copy(alpha = 0.5f), radius = 5f, center = Offset(centerX - 26f, centerY - 8f))
        drawCircle(color = Color(0xFFFF8A80).copy(alpha = 0.5f), radius = 5f, center = Offset(centerX + 26f, centerY - 8f))

        // 7. DYNAMIC MOUTH (Smiling, Talking, Surprised)
        val mouthY = centerY + 4f
        when (expression) {
            CharacterExpression.JOY -> {
                // Wide open laughing happy mouth showing cute teeth
                val laughPath = Path().apply {
                    moveTo(centerX - 14f, mouthY)
                    quadraticTo(centerX, mouthY + 14f, centerX + 14f, mouthY)
                }
                drawPath(laughPath, color = Color(0xFFC2185B)) // Deep dark pink inside mouth
                drawPath(laughPath, color = pupilColor, style = Stroke(width = 2.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                
                // Cute top tooth row
                drawRect(
                    color = Color.White,
                    topLeft = Offset(centerX - 8f, mouthY),
                    size = Size(16f, 3.5f)
                )
            }
            CharacterExpression.SADNESS -> {
                // Sad droopy downturned mouth line
                val sadMouth = Path().apply {
                    moveTo(centerX - 10f, mouthY + 6f)
                    quadraticTo(centerX, mouthY, centerX + 10f, mouthY + 6f)
                }
                drawPath(sadMouth, color = pupilColor, style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            CharacterExpression.SURPRISE -> {
                // Gaped round surprised 'O' mouth
                drawOval(
                    color = pupilColor,
                    topLeft = Offset(centerX - 6f, mouthY),
                    size = Size(12f, 16f)
                )
            }
            CharacterExpression.SPEAKING -> {
                // Dynamic speaking open oval mouth
                val openH = 4f + 12f * speechMouthScale
                drawOval(
                    color = Color(0xFFC2185B),
                    topLeft = Offset(centerX - 8f, mouthY),
                    size = Size(16f, openH)
                )
                drawOval(
                    color = pupilColor,
                    topLeft = Offset(centerX - 8f, mouthY),
                    size = Size(16f, openH),
                    style = Stroke(width = 2f)
                )
                // Small top white tooth
                drawRect(
                    color = Color.White,
                    topLeft = Offset(centerX - 5f, mouthY),
                    size = Size(10f, 2.5f)
                )
            }
            CharacterExpression.THINKING -> {
                // Smart smirk/pucker to one side (as in the thinking notepad Minion)
                val smirkPath = Path().apply {
                    moveTo(centerX - 10f, mouthY + 2f)
                    quadraticTo(centerX - 4f, mouthY - 1f, centerX + 6f, mouthY + 3f)
                }
                drawPath(smirkPath, color = pupilColor, style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            else -> {
                // Sweet, confident smirk/smile
                val smilePath = Path().apply {
                    moveTo(centerX - 12f, mouthY)
                    quadraticTo(centerX, mouthY + 8f, centerX + 12f, mouthY)
                }
                drawPath(smilePath, color = pupilColor, style = Stroke(width = 2.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }

        // 8. CUSTOM ACCESSORIES (Skins support)
        if (avatarStyle == "cool") {
            // Draw Cool Black Sunglasses over Goggles
            drawRoundRect(
                color = Color(0xFF212121),
                topLeft = Offset(centerX - 28f, centerY - 26f),
                size = Size(24f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFF212121),
                topLeft = Offset(centerX + 4f, centerY - 26f),
                size = Size(24f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Sunglasses bridge
            drawLine(
                color = Color(0xFF212121),
                start = Offset(centerX - 4f, centerY - 20f),
                end = Offset(centerX + 4f, centerY - 20f),
                strokeWidth = 4f
            )
            // Sunglasses white reflection shine
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 2f, center = Offset(centerX - 20f, centerY - 21f))
            drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 2f, center = Offset(centerX + 12f, centerY - 21f))
        }

        if (avatarStyle == "sultan") {
            // Draw a traditional Turkish Sultan Fez on Minion's head!
            val fezPath = Path().apply {
                moveTo(centerX - 22f, centerY - 46f)
                lineTo(centerX - 15f, centerY - 68f)
                lineTo(centerX + 15f, centerY - 68f)
                lineTo(centerX + 22f, centerY - 46f)
                close()
            }
            drawPath(path = fezPath, color = Color(0xFFD50000)) // Deep crimson red fez
            
            // Black bottom rim band
            drawLine(
                color = Color(0xFF212121),
                start = Offset(centerX - 21.5f, centerY - 47f),
                end = Offset(centerX + 21.5f, centerY - 47f),
                strokeWidth = 3.5f
            )
            // Golden crescent ornament
            drawCircle(color = Color(0xFFFFD54F), radius = 4f, center = Offset(centerX, centerY - 56f))
            
            // Fez black tassel hanging on top
            drawLine(
                color = Color(0xFF212121),
                start = Offset(centerX, centerY - 68f),
                end = Offset(centerX + 14f, centerY - 60f),
                strokeWidth = 2f
            )
            drawCircle(color = Color(0xFF212121), radius = 2.5f, center = Offset(centerX + 14f, centerY - 60f))
        }

        if (avatarStyle == "cyber") {
            // Neon cyan cyberpunk futuristic visor glowing line
            drawRoundRect(
                color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                topLeft = Offset(centerX - 30f, centerY - 24f),
                size = Size(60f, 10f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawLine(
                color = Color.White,
                start = Offset(centerX - 24f, centerY - 19f),
                end = Offset(centerX + 24f, centerY - 19f),
                strokeWidth = 2f
            )
        }

        if (avatarStyle == "gamer") {
            // Draw RGB gamer headphones
            val bandPath = Path().apply {
                moveTo(centerX - 38f, centerY - 10f)
                cubicTo(
                    centerX - 38f, centerY - 54f,
                    centerX + 38f, centerY - 54f,
                    centerX + 38f, centerY - 10f
                )
            }
            drawPath(
                path = bandPath,
                color = Color(0xFFD500F9), // Purple headband
                style = Stroke(width = 5f)
            )
            // Left headphone cup
            drawRoundRect(
                color = Color(0xFF212121),
                topLeft = Offset(centerX - 42f, centerY - 22f),
                size = Size(8f, 24f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = Color(0xFFD500F9), // glowing purple ring
                topLeft = Offset(centerX - 41f, centerY - 18f),
                size = Size(3f, 16f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
            // Right headphone cup
            drawRoundRect(
                color = Color(0xFF212121),
                topLeft = Offset(centerX + 34f, centerY - 22f),
                size = Size(8f, 24f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = Color(0xFFD500F9), // glowing purple ring
                topLeft = Offset(centerX + 38f, centerY - 18f),
                size = Size(3f, 16f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
        }

        // --- HANDS & ARMS: CHOOSE BASED ON SKIN STYLE ---
        if (avatarStyle == "teddy") {
            // Draw a cute little brown teddy bear held in the minion's left arm!
            // Teddy bear center
            val teddyCenter = Offset(centerX - 18f, centerY + 24f)
            
            // Draw left yellow arm/sleeve holding the teddy bear
            val leftArmTeddy = Path().apply {
                moveTo(centerX - 30f, centerY + 18f)
                quadraticTo(centerX - 24f, centerY + 26f, centerX - 18f, centerY + 24f)
            }
            drawPath(
                path = leftArmTeddy,
                color = yellowBodyColor,
                style = Stroke(width = 7f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Head
            drawCircle(color = Color(0xFF8D6E63), radius = 9f, center = teddyCenter)
            // Ears
            drawCircle(color = Color(0xFF8D6E63), radius = 3.5f, center = Offset(teddyCenter.x - 7.5f, teddyCenter.y - 7.5f))
            drawCircle(color = Color(0xFF8D6E63), radius = 3.5f, center = Offset(teddyCenter.x + 7.5f, teddyCenter.y - 7.5f))
            drawCircle(color = Color(0xFFFFCC80), radius = 1.8f, center = Offset(teddyCenter.x - 7.5f, teddyCenter.y - 7.5f))
            drawCircle(color = Color(0xFFFFCC80), radius = 1.8f, center = Offset(teddyCenter.x + 7.5f, teddyCenter.y - 7.5f))
            // Muzzle
            drawCircle(color = Color(0xFFFFCC80), radius = 3.5f, center = Offset(teddyCenter.x, teddyCenter.y + 2.5f))
            // Nose
            drawCircle(color = Color(0xFF212121), radius = 1.2f, center = Offset(teddyCenter.x, teddyCenter.y + 1.5f))
            // Eyes
            drawCircle(color = Color(0xFF212121), radius = 1.2f, center = Offset(teddyCenter.x - 3.2f, teddyCenter.y - 1.5f))
            drawCircle(color = Color(0xFF212121), radius = 1.2f, center = Offset(teddyCenter.x + 3.2f, teddyCenter.y - 1.5f))
            
            // Teddy body/torso
            drawRoundRect(
                color = Color(0xFF8D6E63),
                topLeft = Offset(teddyCenter.x - 6.5f, teddyCenter.y + 9f),
                size = Size(13f, 15f),
                cornerRadius = CornerRadius(4.5f, 4.5f)
            )
            // Teddy arms/legs
            drawCircle(color = Color(0xFF8D6E63), radius = 2.8f, center = Offset(teddyCenter.x - 8f, teddyCenter.y + 13f))
            drawCircle(color = Color(0xFF8D6E63), radius = 2.8f, center = Offset(teddyCenter.x + 8f, teddyCenter.y + 13f))
            drawCircle(color = Color(0xFF8D6E63), radius = 3.2f, center = Offset(teddyCenter.x - 5.5f, teddyCenter.y + 24f))
            drawCircle(color = Color(0xFF8D6E63), radius = 3.2f, center = Offset(teddyCenter.x + 5.5f, teddyCenter.y + 24f))
            
            // Minion's black glove holding the teddy bear
            drawCircle(color = Color(0xFF212121), radius = 5f, center = Offset(teddyCenter.x + 4.5f, teddyCenter.y + 11f))

            // Right arm (yellow sleeve) - relaxed or waving
            val rightArmPath = Path().apply {
                moveTo(centerX + 30f, centerY + 18f)
                quadraticTo(centerX + 36f, centerY + 24f, centerX + 26f, centerY + 28f)
            }
            drawPath(
                path = rightArmPath,
                color = yellowBodyColor,
                style = Stroke(width = 7f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            // Right black glove
            drawCircle(
                color = Color(0xFF212121),
                radius = 5f,
                center = Offset(centerX + 26f, centerY + 28f)
            )
        } else if (avatarStyle == "placard") {
            // --- DRAW GUNDİ BRO PLACARD/SIGN ---
            // Stick
            drawRect(
                color = Color(0xFF8D6E63), // Brown stick
                topLeft = Offset(centerX + 38f, centerY - 15f),
                size = Size(5f, 50f)
            )
            // Hand glove holding the stick
            drawCircle(
                color = Color(0xFF212121), // Black glove
                radius = 5f,
                center = Offset(centerX + 40.5f, centerY + 10f)
            )
            // Sign Board Background (Wooden frame)
            drawRoundRect(
                color = Color(0xFFFFB300), // Gold/Orange wooden border
                topLeft = Offset(centerX + 22f, centerY - 45f),
                size = Size(55f, 28f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color.White, // White board
                topLeft = Offset(centerX + 24f, centerY - 43f),
                size = Size(51f, 24f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            
            // Write "GUNDI" and "BRO" using clean lines!
            val textColor = Color(0xFF1A237E) // Nice dark blue/black ink
            val textStroke = 2f
            
            // --- G ---
            val gPath = Path().apply {
                moveTo(centerX + 29f, centerY - 38f)
                lineTo(centerX + 26f, centerY - 38f)
                lineTo(centerX + 26f, centerY - 32f)
                lineTo(centerX + 29f, centerY - 32f)
                lineTo(centerX + 29f, centerY - 35f)
                lineTo(centerX + 27.5f, centerY - 35f)
            }
            drawPath(gPath, textColor, style = Stroke(width = textStroke))
            
            // --- U ---
            val uPath = Path().apply {
                moveTo(centerX + 31f, centerY - 38f)
                lineTo(centerX + 31f, centerY - 33f)
                quadraticTo(centerX + 32.5f, centerY - 31.5f, centerX + 34f, centerY - 33f)
                lineTo(centerX + 34f, centerY - 38f)
            }
            drawPath(uPath, textColor, style = Stroke(width = textStroke))
            
            // --- N ---
            val nPath = Path().apply {
                moveTo(centerX + 36f, centerY - 32f)
                lineTo(centerX + 36f, centerY - 38f)
                lineTo(centerX + 39f, centerY - 32f)
                lineTo(centerX + 39f, centerY - 38f)
            }
            drawPath(nPath, textColor, style = Stroke(width = textStroke))
            
            // --- D ---
            val dPath = Path().apply {
                moveTo(centerX + 41f, centerY - 38f)
                lineTo(centerX + 41f, centerY - 32f)
                lineTo(centerX + 43f, centerY - 32f)
                quadraticTo(centerX + 44.5f, centerY - 35f, centerX + 43f, centerY - 38f)
                close()
            }
            drawPath(dPath, textColor, style = Stroke(width = textStroke))
            
            // --- İ ---
            drawLine(textColor, Offset(centerX + 47f, centerY - 36f), Offset(centerX + 47f, centerY - 32f), strokeWidth = textStroke)
            drawCircle(textColor, radius = 1f, center = Offset(centerX + 47f, centerY - 38.5f))
            
            // --- B ---
            val bPath = Path().apply {
                moveTo(centerX + 50f, centerY - 32f)
                lineTo(centerX + 50f, centerY - 25f)
                lineTo(centerX + 52.5f, centerY - 25f)
                quadraticTo(centerX + 54f, centerY - 26.5f, centerX + 52.5f, centerY - 28.5f)
                lineTo(centerX + 50f, centerY - 28.5f)
                lineTo(centerX + 52.5f, centerY - 28.5f)
                quadraticTo(centerX + 54f, centerY - 30f, centerX + 52.5f, centerY - 32f)
                close()
            }
            drawPath(bPath, textColor, style = Stroke(width = textStroke))
            
            // --- R ---
            val rPath = Path().apply {
                moveTo(centerX + 56f, centerY - 25f)
                lineTo(centerX + 56f, centerY - 32f)
                lineTo(centerX + 58.5f, centerY - 32f)
                quadraticTo(centerX + 60f, centerY - 30f, centerX + 58.5f, centerY - 28.5f)
                lineTo(centerX + 56f, centerY - 28.5f)
                moveTo(centerX + 57.5f, centerY - 28.5f)
                lineTo(centerX + 59.5f, centerY - 25f)
            }
            drawPath(rPath, textColor, style = Stroke(width = textStroke))
            
            // --- O ---
            drawOval(
                color = textColor,
                topLeft = Offset(centerX + 62f, centerY - 32f),
                size = Size(5.5f, 7f),
                style = Stroke(width = textStroke)
            )
        } else {
            // --- DRAW CUTE HAND-CLASPING ARMS/HANDS (IMAGE 1 STYLE) ---
            // Left arm (yellow sleeve)
            val leftSleevePath = Path().apply {
                moveTo(centerX - 30f, centerY + 18f)
                quadraticTo(centerX - 20f, centerY + 30f, centerX - 5f, centerY + 28f)
            }
            drawPath(
                path = leftSleevePath,
                color = yellowBodyColor,
                style = Stroke(width = 7f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawPath(
                path = leftSleevePath,
                color = yellowShadowColor,
                style = Stroke(width = 7f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                alpha = 0.3f
            )

            // Right arm (yellow sleeve)
            val rightSleevePath = Path().apply {
                moveTo(centerX + 30f, centerY + 18f)
                quadraticTo(centerX + 20f, centerY + 30f, centerX + 5f, centerY + 28f)
            }
            drawPath(
                path = rightSleevePath,
                color = yellowBodyColor,
                style = Stroke(width = 7f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawPath(
                path = rightSleevePath,
                color = yellowShadowColor,
                style = Stroke(width = 7f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                alpha = 0.3f
            )

            // Black gloves clasping in front
            drawCircle(
                color = Color(0xFF212121), // Black glove left
                radius = 5.5f,
                center = Offset(centerX - 4f, centerY + 28f)
            )
            drawCircle(
                color = Color(0xFF212121), // Black glove right
                radius = 5.5f,
                center = Offset(centerX + 4f, centerY + 28f)
            )
            drawCircle(
                color = Color(0xFF1E1E1E), // Clasp overlap shadow
                radius = 4f,
                center = Offset(centerX, centerY + 28f)
            )
        }
    }
}
}
