package com.example

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GundiAnimatedWelcomeCard(
    sessionId: String?,
    tts: TextToSpeech?,
    isTtsEnabled: Boolean,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val gundiAvatar by settingsManager.gundiAvatar.collectAsState(initial = "classic")
    val coroutineScope = rememberCoroutineScope()

    // Turkish Gundi Greetings
    val greetings = remember {
        listOf(
            "Ooo şefim hoş geldin! Gözlerim yollarda kaldı bro. Sırlar paneli tamamsa dök içini Gundi baddine, hemen halledelim! 😎",
            "Aha, buraların kralı damladı! Bugün hangi çılgın problemi dize getireceğiz başkan? Söyle de yapay zekam şenlensin! 🚀",
            "Selam bro! Çaylar benden, dert ortağı olmak senden. Anlat bakalım, bugün Gundi'den ne istiyorsun? ☕",
            "Gundi badin sana kurban olsun bro! Bugün karizma seviyemiz zirvede, hadi beynimin işlemcilerini coşturacak bir soru sor! 🧠",
            "Brooo! Hoş geldin. Yine efsanevi bir sohbete hazır mıyız? İstediğin konudan dalabilirsin, sınırımız yok! 🎯"
        )
    }

    // Keep track of greeting per unique session to avoid cycling on every recomposition
    var currentGreeting by remember(sessionId) {
        mutableStateOf(greetings.random())
    }

    // Animation state
    var startAnim by remember(sessionId) { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        startAnim = false
        delay(150) // Tiny delay to allow state reset
        startAnim = true

        // Trigger TTS if enabled
        if (isTtsEnabled && tts != null) {
            delay(1000)
            val cleanGreeting = cleanTextForTts(currentGreeting)
            tts.speak(cleanGreeting, TextToSpeech.QUEUE_FLUSH, null, "WELCOME_TTS_ID")
            BubbleStateManager.updateSpeech(cleanGreeting)
        }
    }

    // Spring scaling & slide-up transitions mimicking Framer-Motion physics
    val scaleProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale_spring"
    )

    val offsetYProgress by animateFloatAsState(
        targetValue = if (startAnim) 0f else 120f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset_spring"
    )

    val alphaProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "alpha_fade"
    )

    // Infinite bobbing/floating animation for Gundi avatar
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_gundi_bob")
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_bob"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleProgress)
            .offset(y = offsetYProgress.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Background for the Avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(170.dp)
                .offset(y = bobbingOffset.dp)
        ) {
            // Glowing border pulses behind avatar
            Box(
                modifier = Modifier
                    .size(150.dp * glowScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F).copy(alpha = 0.25f),
                                Color(0xFFFF1744).copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Stylized Circular Frame for Gundi avatar
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFF1744), // Crimson Red
                                Color(0xFFFFD54F), // Gold
                                Color(0xFF00E5FF), // Cyber Cyan/Blue
                                Color(0xFFFF1744)  // back to Crimson Red
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                CuteExpressionCharacter(
                    expression = CharacterExpression.JOY,
                    soundLevel = 0f,
                    avatarStyle = gundiAvatar,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Small badge overlaying avatar indicating he is listening/speaking
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD54F))
                    .border(2.dp, Color(0xFF1E1B24), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Framer-motion speech bubble
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GUNDİ BRO",
                    color = Color(0xFFFFD54F),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                        fontWeight = FontWeight.Bold
                    ),
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentGreeting,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Suggestion Chips (Interactive Quick Actions)
        Text(
            text = "Hızlı Sohbet Önerileri",
            color = Color.Gray,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val suggestions = listOf(
            "Bana Gundi usulü fıkra anlat 🤣",
            "Bugün beynimi yakacak bir soru sor! 🧠",
            "GUNDİ Bro kimdir, gücünü nereden alır? ⚡",
            "Motive et beni bro, canım sıkkın! 💪"
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(0.95f),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            suggestions.forEach { text ->
                var isPressed by remember { mutableStateOf(false) }
                val pressedScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "chip_press"
                )

                Surface(
                    onClick = {
                        coroutineScope.launch {
                            isPressed = true
                            delay(100)
                            isPressed = false
                            onSuggestionClick(text)
                        }
                    },
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .scale(pressedScale)
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
