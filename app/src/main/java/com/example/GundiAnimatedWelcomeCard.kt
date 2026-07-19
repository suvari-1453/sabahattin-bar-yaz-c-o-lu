package com.example

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SparklingGeminiStar(
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2
        val cy = height / 2
        
        val path = Path().apply {
            moveTo(cx, cy - height * 0.48f * scale)
            quadraticTo(cx, cy, cx + width * 0.48f * scale, cy)
            quadraticTo(cx, cy, cx, cy + height * 0.48f * scale)
            quadraticTo(cx, cy, cx - width * 0.48f * scale, cy)
            quadraticTo(cx, cy, cx, cy - height * 0.48f * scale)
            close()
        }
        
        val colors = listOf(
            Color(0xFF9B51E0), // Violet/Indigo
            Color(0xFF4285F4), // Blue
            Color(0xFF00E5FF), // Cyan
            Color(0xFFFFD54F), // Yellow/Minion gold
            Color(0xFFEA4335), // Red
            Color(0xFF9B51E0)  // Violet/Indigo
        )
        
        drawPath(
            path = path,
            brush = Brush.sweepGradient(colors, Offset(cx, cy))
        )
    }
}

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
    val savedStartupGreeting by settingsManager.startupGreeting.collectAsState(initial = "")

    // Turkish Gundi Greetings
    val greetings = remember {
        listOf(
            "Hoş geldin reisim Barış abim! Gözlerim yollarda kaldı bro. Sırlar paneli tamamsa dök içini Gundi baddine, hemen halledelim! 😎",
            "Aha, buraların kralı Barış abim damladı! Bugün hangi çılgın problemi dize getireceğiz başkan? Söyle de yapay zekam şenlensin! 🚀",
            "Selam reisim Barış abim! Çaylar benden, dert ortağı olmak senden. Anlat bakalım, bugün Gundi'den ne istiyorsun? ☕",
            "Gundi badin sana kurban olsun Barış abim! Bugün karizma seviyemiz zirvede, hadi beynimin işlemcilerini coşturacak bir soru sor! 🧠",
            "Barış abim! Hoş geldin reisim. Yine efsanevi bir sohbete hazır mıyız? İstediğin konudan dalabilirsin, sınırımız yok! 🎯"
        )
    }

    var currentGreeting by remember(sessionId, savedStartupGreeting) {
        mutableStateOf(
            if (savedStartupGreeting.isNotBlank()) savedStartupGreeting else greetings.random()
        )
    }

    var startAnim by remember(sessionId) { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        startAnim = false
        delay(150)
        startAnim = true

        if (isTtsEnabled && tts != null) {
            delay(1000)
            val cleanGreeting = cleanTextForTts(currentGreeting)
            tts.speak(cleanGreeting, TextToSpeech.QUEUE_FLUSH, null, "WELCOME_TTS_ID")
            BubbleStateManager.updateSpeech(cleanGreeting)
        }
    }

    val scaleProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale_spring"
    )

    val offsetYProgress by animateFloatAsState(
        targetValue = if (startAnim) 0f else 80f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset_spring"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_gundi_bob")
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_bob"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleProgress)
            .offset(y = offsetYProgress.dp)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Center: Mascot Avatar + Gemini Star integration
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .offset(y = bobbingOffset.dp)
        ) {
            // Glowing Background Aura (Dynamic Multi-color Glow)
            Box(
                modifier = Modifier
                    .size(160.dp * glowScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F).copy(alpha = 0.22f), // Yellow
                                Color(0xFF4285F4).copy(alpha = 0.12f), // Blue
                                Color(0xFF9B51E0).copy(alpha = 0.08f), // Violet
                                Color.Transparent
                            )
                        )
                    )
            )

            // Outer multi-color border for the avatar frame
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF4285F4), // Blue
                                Color(0xFFFFD54F), // Gold
                                Color(0xFF00E5FF), // Cyan
                                Color(0xFFFFD54F), // Gold
                                Color(0xFF4285F4)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                CuteExpressionCharacter(
                    expression = CharacterExpression.JOY,
                    soundLevel = 0f,
                    avatarStyle = gundiAvatar,
                    modifier = Modifier.fillMaxSize(0.92f)
                )
            }

            // Beautiful Sparkling Gemini Star floating near the mascot
            SparklingGeminiStar(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Premium Headline (Matching the user's screenshot exactly!)
        Text(
            text = "Nereden başlayalım?",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            ),
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        var showEditDialog by remember { mutableStateOf(false) }
        var editGreetingText by remember { mutableStateOf("") }

        // Gundi speech bubble/greeting box
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable {
                    editGreetingText = currentGreeting
                    showEditDialog = true
                }
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GUNDİ BRO",
                        color = Color(0xFFFFD54F),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = Color(0xFFFFD54F).copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentGreeting,
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = Color(0xFF1E1F24),
                title = {
                    Text(
                        text = "Karşılama Mesajını Düzenle ✏️",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Gündi Bro seni bu mesajla karşılasın:",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = editGreetingText,
                            onValueChange = { editGreetingText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD54F),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                cursorColor = Color(0xFFFFD54F)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editGreetingText.isNotBlank()) {
                                settingsManager.setStartupGreeting(editGreetingText)
                                currentGreeting = editGreetingText
                                showEditDialog = false
                                // Speak immediately
                                if (isTtsEnabled && tts != null) {
                                    val cleanGreeting = cleanTextForTts(editGreetingText)
                                    tts.speak(cleanGreeting, TextToSpeech.QUEUE_FLUSH, null, "WELCOME_TTS_ID")
                                    BubbleStateManager.updateSpeech(cleanGreeting)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD54F),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Kaydet", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("İptal", color = Color.Gray)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Suggestion Chips (Interactive Quick Actions)
        Text(
            text = "Hızlı Sohbet Önerileri",
            color = Color.Gray,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val suggestions = listOf(
            "Gemini Restoranında Nano Muz Sipariş Et! 🍌✨",
            "Bana Gundi usulü fıkra anlat 🤣",
            "Bugün beynimi yakacak bir soru sor! 🧠",
            "GUNDİ Bro kimdir, gücünü nereden alır? ⚡"
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
                    color = Color(0xFF1E1F24).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
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
