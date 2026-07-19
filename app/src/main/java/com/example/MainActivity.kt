package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import kotlin.math.absoluteValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "unknown_file"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GundiApiRotator.init(this)
        NotificationHelper.initNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Global Text-to-Speech engine
    val tts = remember {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale("tr", "TR")
            }
        }
        ttsInstance
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") { 
                LoginScreen(
                    tts = tts,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onLoginSuccess = { 
                        navController.navigate("chat") { 
                            popUpTo("login") { inclusive = true } 
                        } 
                    }
                ) 
            }
            composable("chat") { 
                ChatScreen(
                    tts = tts, 
                    onNavigateToProfile = { navController.navigate("profile") },
                    onNavigateToSettings = { navController.navigate("settings") }
                ) 
            }
            composable("profile") { ProfileScreen(tts = tts, onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }

        // Draggable, collapsible Mini Media Player that stays visible on top of both screens
        FloatingMediaPlayer()

        // Futuristic screen-scanning glow animation during accessibility readings
        ScreenScannerAnimation()
    }
}

@Composable
fun LoginScreen(
    tts: TextToSpeech?,
    onNavigateToSettings: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val gundiAvatar by settingsManager.gundiAvatar.collectAsState()
    val startupGreeting by settingsManager.startupGreeting.collectAsState()
    val speechSpeed by settingsManager.speechSpeed.collectAsState()
    val speechPitch by settingsManager.speechPitch.collectAsState()
    val isTtsEnabled by settingsManager.isTtsEnabled.collectAsState()
    val isPasscodeEnabled by settingsManager.isPasscodeEnabled.collectAsState()
    val correctPasscode by settingsManager.appPasscode.collectAsState()
    var passwordText by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    // Feeling Motor Mode: "sakin", "neseli", "crazy", "dertli", "agresif"
    var motorMode by remember { mutableStateOf("sakin") }
    // Manual speed multiplier (0.1x to 5.0x)
    var manualSpeedMultiplier by remember { mutableStateOf(1.0f) }

    LaunchedEffect(tts, isTtsEnabled) {
        if (tts != null && isTtsEnabled) {
            // Wait slightly for TTS engine to set up language fully
            delay(1000)
            tts.setSpeechRate(speechSpeed)
            tts.setPitch(speechPitch)
            val cleanGreeting = cleanTextForTts(startupGreeting)
            tts.speak(cleanGreeting, TextToSpeech.QUEUE_FLUSH, null, "WELCOME_ID")
            BubbleStateManager.updateSpeech(cleanGreeting)
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(1500)
            onLoginSuccess()
        }
    }

    // Floating Icon definitions for all requested feelings
    val feelingIcons = remember {
        listOf(
            Triple("smiling", "😊", 0.0f),
            Triple("crying", "😢", 0.3f),
            Triple("angry", "😡", 0.6f),
            Triple("thinking2", "🤔", 0.9f),
            Triple("swearing", "🤬", 1.2f),
            Triple("laugh", "😂", 1.5f),
            Triple("love2", "😍", 1.8f),
            Triple("crazy", "🤪", 2.1f),
            Triple("sad", "😔", 2.4f),
            Triple("happy", "😁", 2.7f),
            Triple("sick", "🤢", 3.0f),
            Triple("dynamic", "⚡", 3.3f),
            Triple("cold", "🥶", 3.6f),
            Triple("hot", "🥵", 3.9f),
            Triple("scared", "😱", 4.2f),
            Triple("badmood", "👿", 4.5f),
            Triple("singing", "🎤", 4.8f),
            Triple("dumb", "🤡", 5.1f),
            Triple("confused", "🧐", 5.4f),
            Triple("xray", "💀", 5.7f),
            Triple("guilty", "🥺", 6.0f),
            Triple("proud", "😎", 1.0f),
            Triple("enthusiastic", "🥳", 2.0f)
        )
    }

    // Time ticker for smooth 60fps animations
    val infiniteTransition = rememberInfiniteTransition(label = "feeling_motor")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_time"
    )

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0D13))
    ) {
        // Main Scrollable Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with App Logo/Subtitle and Settings shortcut!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GUNDİ BRO",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD54F)
                        )
                    )
                    Text(
                        text = "Duygu Motoru Denetleyici v2.5",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), shape = CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Hızlı Ayarlar",
                        tint = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sphere of Moving Feeling Icons around the Gundi Mascot
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background compass ticks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = this.size.width / 2
                    val cy = this.size.height / 2
                    this.drawCircle(
                        color = Color.White.copy(alpha = 0.03f),
                        radius = 110.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                    this.drawCircle(
                        color = Color.White.copy(alpha = 0.015f),
                        radius = 70.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }

                // Orbiting/Floating/Vibrating Feelings Icons
                feelingIcons.forEachIndexed { index, (name, emoji, phase) ->
                    // Calculate individual physics attributes based on active feeling motor mode
                    val angleOffset = phase + (index * 0.27f)
                    val baseDistance = 85.dp + (index % 3 * 10).dp

                    var currentX = 0.dp
                    var currentY = 0.dp
                    var currentScale = 1.0f
                    var currentAlpha = 0.85f

                    val actualMultiplier = manualSpeedMultiplier * when (motorMode) {
                        "sakin" -> 0.4f
                        "neseli" -> 1.2f
                        "crazy" -> 3.5f
                        "dertli" -> 0.8f
                        "agresif" -> 5.0f
                        else -> 1.0f
                    }

                    when (motorMode) {
                        "sakin" -> {
                            val currentAngle = angleOffset + animTime * actualMultiplier * 0.02f
                            val radius = baseDistance.value + kotlin.math.sin(animTime * 0.05f + phase * 4f) * 8f
                            currentX = (kotlin.math.cos(currentAngle) * radius).dp
                            currentY = (kotlin.math.sin(currentAngle) * radius).dp
                            currentScale = 0.95f + kotlin.math.sin(animTime * 0.06f + phase) * 0.08f
                        }
                        "neseli" -> {
                            val currentAngle = angleOffset + animTime * actualMultiplier * 0.04f
                            val radius = baseDistance.value + kotlin.math.sin(animTime * 0.12f + phase * 5f) * 15f
                            currentX = (kotlin.math.cos(currentAngle) * radius).dp
                            currentY = (kotlin.math.sin(currentAngle) * radius).dp
                            currentScale = 1.05f + kotlin.math.sin(animTime * 0.15f + phase) * 0.15f
                        }
                        "crazy" -> {
                            val currentAngle = angleOffset + animTime * actualMultiplier * 0.08f
                            val radius = (baseDistance.value - 20) + kotlin.math.sin(animTime * 0.35f + phase * 7f) * 32f
                            currentX = (kotlin.math.cos(currentAngle) * radius).dp
                            currentY = (kotlin.math.sin(currentAngle) * radius).dp
                            currentScale = 1.1f + kotlin.math.sin(animTime * 0.45f + phase) * 0.28f
                        }
                        "dertli" -> {
                            // Falling teardrop waterfall animation
                            val dropSpeed = actualMultiplier * 3.2f
                            val startY = -120f
                            val endY = 120f
                            val totalSpan = endY - startY
                            val phaseOffset = (phase * 150f)
                            val rawY = startY + ((animTime * dropSpeed + phaseOffset) % totalSpan)
                            currentY = rawY.dp
                            // Spread horizontally in a gentle wave
                            currentX = (kotlin.math.sin(animTime * 0.05f + phase * 10f) * 90f).dp
                            currentScale = 0.9f
                            currentAlpha = if (rawY > 80f) (120f - rawY) / 40f else 0.9f
                        }
                        "agresif" -> {
                            // High-frequency shake on orbit
                            val currentAngle = angleOffset + animTime * 0.01f
                            val radius = baseDistance.value
                            val baseX = kotlin.math.cos(currentAngle) * radius
                            val baseY = kotlin.math.sin(currentAngle) * radius
                            // Vibration offset
                            val vibX = kotlin.math.sin(animTime * 9.0f + index * 1.5f) * 7f
                            val vibY = kotlin.math.cos(animTime * 9.0f + index * 1.5f) * 7f
                            currentX = (baseX + vibX).dp
                            currentY = (baseY + vibY).dp
                            currentScale = 1.0f + kotlin.math.sin(animTime * 0.5f) * 0.1f
                        }
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = currentX, y = currentY)
                            .scale(currentScale)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (motorMode) {
                                    "agresif" -> Color(0xFFFF1744).copy(alpha = 0.15f)
                                    "crazy" -> Color(0xFFE040FB).copy(alpha = 0.15f)
                                    "neseli" -> Color(0xFFFFD54F).copy(alpha = 0.15f)
                                    "dertli" -> Color(0xFF29B6F6).copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.06f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when (motorMode) {
                                    "agresif" -> Color(0xFFFF1744).copy(alpha = 0.4f)
                                    "crazy" -> Color(0xFFE040FB).copy(alpha = 0.4f)
                                    "neseli" -> Color(0xFFFFD54F).copy(alpha = 0.4f)
                                    "dertli" -> Color(0xFF29B6F6).copy(alpha = 0.4f)
                                    else -> Color.White.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 18.sp,
                            modifier = Modifier.alpha(currentAlpha)
                        )
                    }
                }

                // Central Gundi Mascot
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    when (motorMode) {
                                        "agresif" -> Color(0xFFFF1744).copy(alpha = 0.35f)
                                        "crazy" -> Color(0xFFE040FB).copy(alpha = 0.35f)
                                        "neseli" -> Color(0xFFFFD54F).copy(alpha = 0.35f)
                                        "dertli" -> Color(0xFF29B6F6).copy(alpha = 0.35f)
                                        else -> Color(0xFFFFD54F).copy(alpha = 0.25f)
                                    },
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(85.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFFFF1744),
                                        Color(0xFFFFD54F),
                                        Color(0xFF00E5FF),
                                        Color(0xFFFF1744)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CuteExpressionCharacter(
                            expression = when (motorMode) {
                                "agresif" -> CharacterExpression.SADNESS // angry state
                                "crazy" -> CharacterExpression.SPEAKING
                                "neseli" -> CharacterExpression.JOY
                                "dertli" -> CharacterExpression.LISTENING
                                else -> CharacterExpression.IDLE
                            },
                            soundLevel = if (motorMode == "crazy") 0.6f else 0.0f,
                            avatarStyle = gundiAvatar,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Welcome Text
            Text(
                text = "GUNDİ BRO",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD54F)
                ),
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "'ya Hoş Geldiniz",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Seni anlayan, seninle konuşan ve gören yapay zeka dostun.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // THE FEELING MOTOR (DUYGU MOTORU) Denetim Masası Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16131C)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gündi Duygu Motoru Kontrolü",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gündi'nin duygusal devrini ve ikon hareketlerini motor modlarıyla ayarlayın:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Grid/Row selector (5 options)
                    val modes = listOf(
                        Triple("sakin", "Sakin 🧘", Color(0xFF81C784)),
                        Triple("neseli", "Neşeli ✨", Color(0xFFFFD54F)),
                        Triple("crazy", "Deli 🤪", Color(0xFFBA68C8)),
                        Triple("dertli", "Dertli 😢", Color(0xFF4FC3F7)),
                        Triple("agresif", "Agresif 🤬", Color(0xFFFF5252))
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Row 1: Sakin, Neseli, Deli
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            modes.take(3).forEach { (modeId, label, accentColor) ->
                                val isSelected = motorMode == modeId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.22f)
                                            else Color.White.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { motorMode = modeId }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }

                        // Row 2: Dertli, Agresif
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            modes.drop(3).forEach { (modeId, label, accentColor) ->
                                val isSelected = motorMode == modeId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.22f)
                                            else Color.White.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { motorMode = modeId }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Motor Telemetry Data
                    val activeRPM = when (motorMode) {
                        "sakin" -> (1200 * manualSpeedMultiplier).toInt()
                        "neseli" -> (3200 * manualSpeedMultiplier).toInt()
                        "crazy" -> (7800 * manualSpeedMultiplier).toInt()
                        "dertli" -> (800 * manualSpeedMultiplier).toInt()
                        "agresif" -> (9500 * manualSpeedMultiplier).toInt()
                        else -> 1000
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Aktif Motor Devri (RPM)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "$activeRPM rpm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    activeRPM > 8000 -> Color(0xFFFF5252)
                                    activeRPM > 5000 -> Color(0xFFBA68C8)
                                    activeRPM > 2000 -> Color(0xFFFFD54F)
                                    else -> Color(0xFF81C784)
                                }
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Isı Seviyesi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = when (motorMode) {
                                    "agresif" -> "Hararet! 🌡️⚡"
                                    "crazy" -> "Yüksek 🔥"
                                    "neseli" -> "Normal ☕"
                                    "dertli" -> "Serin ❄️"
                                    else -> "Stabil 🧘"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (motorMode) {
                                    "agresif" -> Color(0xFFFF5252)
                                    "crazy" -> Color(0xFFFFB74D)
                                    "dertli" -> Color(0xFF4FC3F7)
                                    else -> Color(0xFF81C784)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Manual Speed Multiplier Slider (Continuous Control)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Manuel Devir Hassasiyeti: ${String.format("%.1f", manualSpeedMultiplier)}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "İkonları Hızlandır!",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = manualSpeedMultiplier,
                            onValueChange = { manualSpeedMultiplier = it },
                            valueRange = 0.1f..5.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFD54F),
                                activeTrackColor = Color(0xFFFFD54F),
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isPasscodeEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16131C)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (passwordError) Color(0xFFFF5252).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Güvenli Giriş Şifresi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var isLoginPasswordVisible by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { 
                                passwordText = it
                                passwordError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD54F),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            placeholder = { Text("Giriş şifresini yazın", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFFFD54F)) },
                            trailingIcon = {
                                IconButton(onClick = { isLoginPasswordVisible = !isLoginPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isLoginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Şifre Görünürlüğü",
                                        tint = Color(0xFFFFD54F)
                                    )
                                }
                            },
                            visualTransformation = if (isLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                        )

                        if (passwordError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hatalı Giriş Şifresi! Lütfen tekrar deneyin.",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Varsayılan şifre '1234'dür.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Start Login Action Button
            Button(
                onClick = { 
                    if (isPasscodeEnabled) {
                        if (passwordText == correctPasscode) {
                            isLoading = true
                        } else {
                            passwordError = true
                        }
                    } else {
                        isLoading = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54F),
                    contentColor = Color.Black
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Icon",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Kral Girişi Yap 👑", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated credit text for Sebahattin Barış Yazıcıoğlu
            var creditVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(400)
                creditVisible = true
            }

            AnimatedVisibility(
                visible = creditVisible,
                enter = fadeIn(animationSpec = tween(1200)) + expandVertically(animationSpec = tween(1000)),
            ) {
                val loopTransition = rememberInfiniteTransition(label = "pulsating_glow")
                val glowAlpha by loopTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow_alpha"
                )

                val colorShift by loopTransition.animateColor(
                    initialValue = Color(0xFFE57373),
                    targetValue = Color(0xFFFFD54F),
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "color_shift"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, colorShift.copy(alpha = 0.2f * glowAlpha), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Bu uygulama",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.Gray.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sebahattin Barış Yazıcıoğlu",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = colorShift.copy(alpha = 0.9f + 0.1f * glowAlpha),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "tarafından yapılmıştır",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.Gray.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel = viewModel(), 
    tts: TextToSpeech?, 
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFile by remember { mutableStateOf<AttachedFile?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var soundLevel by remember { mutableStateOf(0f) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val isTtsEnabled by settingsManager.isTtsEnabled.collectAsState()
    val isSoundEffectsEnabled by settingsManager.isSoundEffectsEnabled.collectAsState()
    val gundiAvatar by settingsManager.gundiAvatar.collectAsState()
    var isTtsSpeaking by remember { mutableStateOf(false) }

    var selectedDrawerTab by remember { mutableStateOf("sessions") }
    val nickname by settingsManager.nickname.collectAsState()
    val theme by settingsManager.theme.collectAsState()
    val language by settingsManager.language.collectAsState()
    val creativity by settingsManager.creativity.collectAsState()
    val replyMode by settingsManager.replyMode.collectAsState()
    val speechSpeed by settingsManager.speechSpeed.collectAsState()
    val speechPitch by settingsManager.speechPitch.collectAsState()
    val voiceStyle by settingsManager.voiceStyle.collectAsState()
    val witLevel by settingsManager.witLevel.collectAsState()
    val searchGrounding by settingsManager.searchGrounding.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember(nickname) { mutableStateOf(nickname) }

    var showArabicLyricsDialog by remember { mutableStateOf(false) }
    var arabicLyricsInput by remember { mutableStateOf("") }
    var userSentimentExpression by remember { mutableStateOf<CharacterExpression?>(null) }
    var activeZoomedImage by remember { mutableStateOf<Bitmap?>(null) }

    var isConsoleMode by remember { mutableStateOf(false) }
    var bubbleOffsetX by remember { mutableStateOf(100f) }
    var bubbleOffsetY by remember { mutableStateOf(350f) }
    var textToExport by remember { mutableStateOf<String?>(null) }
    var showVoiceToneSelector by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showProfileDropdown by remember { mutableStateOf(false) }

    val singleExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val text = textToExport ?: ""
                        outputStream.write(text.toByteArray())
                        Toast.makeText(context, "Cevap başarıyla dışarı aktarıldı! 💾", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Dışarı aktarma başarısız oldu!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        val text = messages.joinToString("\n\n") { msg ->
                            val sender = if (msg.isUser) "Kullanıcı" else "GUNDİ Bro"
                            "[$sender]: ${msg.text}"
                        }
                        outputStream.write(text.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )

    // Reset sentiment expression back to neutral after 5 seconds
    LaunchedEffect(userSentimentExpression) {
        if (userSentimentExpression != null) {
            delay(5000)
            userSentimentExpression = null
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Periodic ticker to check if TTS is actively speaking for facial animations
    LaunchedEffect(tts) {
        while (true) {
            isTtsSpeaking = (tts?.isSpeaking == true) || GeminiVoicePlayer.isPlaying()
            delay(150)
        }
    }

    val lastAiMsg = messages.lastOrNull { !it.isUser }
    val parsedEmotion = lastAiMsg?.let { parseGundiEmotion(it.text) }

    val expression = when {
        isLoading -> CharacterExpression.THINKING
        isListening -> CharacterExpression.LISTENING
        isTtsSpeaking -> CharacterExpression.SPEAKING
        userSentimentExpression != null -> userSentimentExpression!!
        parsedEmotion != null -> parsedEmotion
        else -> CharacterExpression.IDLE
    }

    var lastSoundExpression by remember { mutableStateOf<CharacterExpression?>(null) }
    LaunchedEffect(expression, isSoundEffectsEnabled) {
        if (isSoundEffectsEnabled && expression != lastSoundExpression) {
            if (expression != CharacterExpression.IDLE) {
                GundiSoundManager.playGundiEmotionReaction(context, expression, tts, isTtsEnabled)
            }
            lastSoundExpression = expression
        }
    }

    // Auto-scroll and speak when new AI message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
            val lastMsg = messages.last()
            if (!lastMsg.isUser && isTtsEnabled) {
                val cleanSpeechText = cleanTextForTts(lastMsg.text)
                if (!lastMsg.audioBase64.isNullOrBlank()) {
                    tts?.stop()
                    GeminiVoicePlayer.playPcm(lastMsg.audioBase64)
                } else {
                    val settings = SettingsManager.getInstance(context)
                    val currentSpeed = settings.speechSpeed.value
                    val currentPitch = settings.speechPitch.value
                    val currentLang = settings.language.value
                    tts?.setSpeechRate(currentSpeed)
                    tts?.setPitch(currentPitch)
                    val locale = when (currentLang) {
                        "Turkish" -> java.util.Locale("tr", "TR")
                        "English" -> java.util.Locale.ENGLISH
                        "German" -> java.util.Locale.GERMAN
                        "Azerbaijani" -> java.util.Locale("az", "AZ")
                        "Kurdish" -> java.util.Locale("ku", "TR")
                        else -> java.util.Locale("tr", "TR")
                    }
                    tts?.language = locale
                    tts?.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, null, null)
                }
                BubbleStateManager.updateSpeech(cleanSpeechText)
            }
        }
    }

    // STT Setup
    val speechContext = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            context.createAttributionContext("speech")
        } else {
            context
        }
    }
    val speechRecognizer = remember(speechContext) { SpeechRecognizer.createSpeechRecognizer(speechContext) }
    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                soundLevel = rmsdB
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { 
                isListening = false 
                soundLevel = 0f
            }
            override fun onError(error: Int) { 
                isListening = false 
                soundLevel = 0f
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val speechText = matches[0]
                    if (speechText.isNotBlank()) {
                        val detected = analyzeSentiment(speechText)
                        if (detected != CharacterExpression.IDLE) {
                            userSentimentExpression = detected
                        }
                        viewModel.sendMessage(speechText, null, null, context)
                    }
                }
                isListening = false
                soundLevel = 0f
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isListening = true
                speechRecognizer.startListening(recognitionIntent)
            }
        }
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                selectedFile = null
            }
        }
    )

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
                val name = getFileName(context, it)
                val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                if (bytes != null) {
                    selectedFile = AttachedFile(bytes, mimeType, name)
                    selectedBitmap = null
                }
            }
        }
    )

    val globalFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        globalFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070709), // Sleek deep black top
                        Color(0xFF0E0F1D), // Dark midnight indigo
                        Color(0xFF131932)  // Gemini deep blue bottom
                    )
                )
            )
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
        modifier = Modifier
            .focusRequester(globalFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                    if (drawerState.isOpen) {
                        coroutineScope.launch { drawerState.close() }
                        true
                    } else if (selectedBitmap != null || selectedFile != null) {
                        selectedBitmap = null
                        selectedFile = null
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E1B24),
                modifier = Modifier.width(320.dp)
            ) {
                if (selectedDrawerTab == "settings") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedDrawerTab = "sessions" }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Sohbetlere Dön",
                                        tint = Color.White
                                    )
                                }
                                Text(
                                    "Ayarlar",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.close() } }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.LightGray)
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Takma Adın (Rumuz)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = nicknameInput,
                                    onValueChange = { 
                                        nicknameInput = it
                                        settingsManager.setNickname(it)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFFD54F),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                    ),
                                    placeholder = { Text("Örn: Bro, Şef, Canım Dostum", color = Color.Gray) },
                                    singleLine = true
                                )
                            }

                            Column {
                                Text(
                                    text = "Uygulama & Konuşma Dili",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable { showLanguageDialog = true }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = when (language) {
                                                "Turkish" -> "Türkçe 🇹🇷"
                                                "English" -> "English 🇬🇧"
                                                "German" -> "Deutsch 🇩🇪"
                                                "Azerbaijani" -> "Azərbaycan 🇦🇿"
                                                "Kurdish" -> "Kurdî ☀️"
                                                else -> "Türkçe 🇹🇷"
                                            },
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                                }
                            }

                            Column {
                                Text(
                                    text = "Arayüz Teması",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable { showThemeDialog = true }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFBA68C8), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = when (theme) {
                                                "Dark" -> "Asil Siyah & Gece 🌌"
                                                "Light" -> "Sade & Gündüz ☀️"
                                                "Matrix" -> "Hacker & Terminal 👾"
                                                "Crimson" -> "Al Kırmızı & Bayrak 🇹🇷"
                                                else -> "Asil Siyah & Gece 🌌"
                                            },
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                                }
                            }

                            Column {
                                Text(
                                    text = "Gundi Mizah Seviyesi: ${witLevel.toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray
                                )
                                Slider(
                                    value = witLevel,
                                    onValueChange = { settingsManager.setWitLevel(it) },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color(0xFFFFD54F),
                                        thumbColor = Color(0xFFFFD54F)
                                    )
                                )
                                val witLabel = when (witLevel.toInt()) {
                                    1 -> "Düz & Robotik 🤖"
                                    2 -> "Hafif Esprili 😏"
                                    3 -> "Normal Gundi 😎"
                                    4 -> "Bol Kahkahalı 🤣"
                                    5 -> "Maksimum Makara 🔥"
                                    else -> "Normal Gundi 😎"
                                }
                                Text(
                                    text = witLabel,
                                    color = Color(0xFFFFD54F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { settingsManager.setSearchGrounding(!searchGrounding) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Google Arama (Grounding)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Gerçek zamanlı arama desteği", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                                Switch(
                                    checked = searchGrounding,
                                    onCheckedChange = { settingsManager.setSearchGrounding(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4285F4))
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable { settingsManager.setIsTtsEnabled(!isTtsEnabled) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Sesli Okuma (TTS)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Gelen yanıtları seslendir", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                                Switch(
                                    checked = isTtsEnabled,
                                    onCheckedChange = { settingsManager.setIsTtsEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD54F))
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sohbetler",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                viewModel.createNewSession()
                                coroutineScope.launch { drawerState.close() }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Yeni Sohbet Başlat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(sessions) { session ->
                            val isActive = session.id == currentSessionId
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChatBubble,
                                                contentDescription = "Sohbet",
                                                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = session.title,
                                                color = if (isActive) Color.White else Color.LightGray,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.togglePinSession(session.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (session.isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "Sabitle",
                                                    tint = if (session.isPinned) Color(0xFFFFD700) else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteSession(session.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Sil",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                selected = isActive,
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color.White.copy(alpha = 0.12f),
                                    unselectedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NavigationDrawerItem(
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Assistant,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Asistan Paneli", color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                AssistantHubState.showAssistantHub = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        NavigationDrawerItem(
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Ayarlar (Dil ve Seçenekler)", color = Color.White)
                                }
                            },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigateToSettings()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Gundi Bro v3.5 - Gundi Badi Her Zaman Yanında! 🤙", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "Gundi Bro",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                fontSize = 19.sp,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(12.dp)
                                    .graphicsLayer(rotationZ = 90f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    actions = {
                        if (isTtsSpeaking) {
                            IconButton(
                                onClick = { 
                                    tts?.stop()
                                    GeminiVoicePlayer.stop()
                                    Toast.makeText(context, "Gundi susturuldu! 🤫", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .background(Color(0xFFFF1744).copy(alpha = 0.25f), CircleShape)
                                    .border(1.5.dp, Color(0xFFFF1744), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Sustur",
                                    tint = Color(0xFFFF1744)
                                )
                            }
                        }

                        IconButton(onClick = { 
                            AssistantHubState.showAssistantHub = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Assistant,
                                contentDescription = "Asistan Paneli",
                                tint = Color(0xFFFFD54F) // Gold/Minion yellow sparkle
                            )
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1B24))
                                    .border(1.2.dp, Color(0xFFFFD54F), CircleShape)
                                    .clickable { showProfileDropdown = true },
                                contentAlignment = Alignment.Center
                            ) {
                                CuteExpressionCharacter(
                                    expression = expression,
                                    soundLevel = soundLevel,
                                    avatarStyle = gundiAvatar,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            DropdownMenu(
                                expanded = showProfileDropdown,
                                onDismissRequest = { showProfileDropdown = false },
                                modifier = Modifier.background(Color(0xFF1E1F24))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sesli Okuma: ${if (isTtsEnabled) "Açık" else "Kapalı"}", color = Color.White) },
                                    leadingIcon = { Icon(if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, contentDescription = null, tint = Color.LightGray) },
                                    onClick = {
                                        showProfileDropdown = false
                                        val nextVal = !isTtsEnabled
                                        settingsManager.setIsTtsEnabled(nextVal)
                                        if (!nextVal) {
                                            tts?.stop()
                                            GeminiVoicePlayer.stop()
                                            Toast.makeText(context, "Gundi susturuldu! 🤫", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Gundi sesli okuma aktif! 🔊", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ses Tonu Değiştir", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.LightGray) },
                                    onClick = {
                                        showProfileDropdown = false
                                        showVoiceToneSelector = !showVoiceToneSelector
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ekranı Sesli Oku", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Accessibility, contentDescription = null, tint = Color.LightGray) },
                                    onClick = {
                                        showProfileDropdown = false
                                        if (ScreenReaderState.isActive) {
                                            ScreenReaderState.stop(tts)
                                        } else {
                                            readScreenContext(context, tts, "chat", messages)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sohbeti Dışa Aktar", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color.LightGray) },
                                    onClick = {
                                        showProfileDropdown = false
                                        exportLauncher.launch("Sohbet_${System.currentTimeMillis()}.txt")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gelişmiş Ayarlar", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.LightGray) },
                                    onClick = {
                                        showProfileDropdown = false
                                        onNavigateToSettings()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    GundiVoiceWave(
                        isSpeaking = isTtsSpeaking,
                        onMuteClick = {
                            tts?.stop()
                            GeminiVoicePlayer.stop()
                            Toast.makeText(context, "Gundi susturuldu! 🤫", Toast.LENGTH_SHORT).show()
                        }
                    )

                    EmotionDashboardWidget(
                        expression = expression,
                        soundLevel = soundLevel,
                        isLoading = isLoading,
                        isListening = isListening,
                        isTtsSpeaking = isTtsSpeaking,
                        userSentimentExpression = userSentimentExpression,
                        avatarStyle = gundiAvatar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                GundiAnimatedWelcomeCard(
                                    sessionId = currentSessionId,
                                    tts = tts,
                                    isTtsEnabled = isTtsEnabled,
                                    onSuggestionClick = { suggestion ->
                                        inputText = suggestion
                                    }
                                )
                            }
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    VoiceReactiveOrb(
                                        isListening = isListening,
                                        soundLevel = soundLevel,
                                        modifier = Modifier
                                            .clickable {
                                                if (isListening) {
                                                    speechRecognizer.stopListening()
                                                    isListening = false
                                                    soundLevel = 0f
                                                } else {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isListening) "Seni dinliyorum..." else "Konuşarak başlamak için küreye tıkla",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isListening) Color(0xFFFF1744) else Color.LightGray
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "📺 Ufak Pencereli Medya Oynatıcı",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "GUNDİ Bro mini oynatıcıyı anında test edin!",
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        MediaPlayerManager.playTrack(context, MediaPlayerManager.samples[0])
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Örnek MP3", fontSize = 10.sp)
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        MediaPlayerManager.playTrack(context, MediaPlayerManager.samples[1])
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Örnek MP4", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        items(messages) { msg ->
                            MessageBubble(
                                message = msg,
                                onImageClick = { activeZoomedImage = it },
                                onExportClick = { text ->
                                    textToExport = text
                                    singleExportLauncher.launch("Gundi_Cevabi_${System.currentTimeMillis()}.txt")
                                },
                                onSpeakClick = { text, audioBase64 ->
                                    if (!audioBase64.isNullOrBlank()) {
                                        tts?.stop()
                                        GeminiVoicePlayer.playPcm(audioBase64)
                                    } else {
                                        val settings = SettingsManager.getInstance(context)
                                        val currentSpeed = settings.speechSpeed.value
                                        val currentPitch = settings.speechPitch.value
                                        val currentLang = settings.language.value
                                        tts?.setSpeechRate(currentSpeed)
                                        tts?.setPitch(currentPitch)
                                        val locale = when (currentLang) {
                                            "Turkish" -> java.util.Locale("tr", "TR")
                                            "English" -> java.util.Locale.ENGLISH
                                            "German" -> java.util.Locale.GERMAN
                                            "Azerbaijani" -> java.util.Locale("az", "AZ")
                                            "Kurdish" -> java.util.Locale("ku", "TR")
                                            else -> java.util.Locale("tr", "TR")
                                        }
                                        tts?.language = locale
                                        val cleanSpeechText = cleanTextForTts(text)
                                        tts?.stop()
                                        tts?.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                }
                            )
                        }
                        if (isLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(bottom = 8.dp)
                ) {
                    AnimatedVisibility(visible = selectedBitmap != null || selectedFile != null) {
                        Box(modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp)) {
                            selectedBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            selectedFile?.let { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1E1F24), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = "Dosya", tint = Color(0xFF4285F4))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(file.name, color = Color.White, maxLines = 1, fontSize = 13.sp)
                                }
                            }
                            IconButton(
                                onClick = { 
                                    selectedBitmap = null
                                    selectedFile = null 
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Expandable Attachment Menu
                    AnimatedVisibility(
                        visible = showAttachMenu,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1E1F24),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.width(180.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showAttachMenu = false
                                            imagePickerLauncher.launch("image/*")
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Görsel Ekle", color = Color.White, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showAttachMenu = false
                                            documentPickerLauncher.launch("*/*")
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Dosya Ekle", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Bottom Sleek Capsule
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(Color(0xFF1E1F24), RoundedCornerShape(28.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showAttachMenu = !showAttachMenu },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Dosya Ekle",
                                tint = Color.LightGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty() && selectedBitmap == null && selectedFile == null) {
                                    Text(
                                        text = "Gundi Bro'ya sorun...",
                                        color = Color.Gray,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() || selectedBitmap != null || selectedFile != null) {
                                        val prompt = inputText
                                        inputText = ""
                                        val bmp = selectedBitmap
                                        selectedBitmap = null
                                        val file = selectedFile
                                        selectedFile = null
                                        
                                        val detected = analyzeSentiment(prompt)
                                        if (detected != CharacterExpression.IDLE) {
                                            userSentimentExpression = detected
                                        }
                                        viewModel.sendMessage(prompt, bmp, file, context)
                                    }
                                }
                            )
                        )

                        if (inputText.isNotBlank() || selectedBitmap != null || selectedFile != null) {
                            IconButton(
                                onClick = {
                                    val prompt = inputText
                                    inputText = ""
                                    val bmp = selectedBitmap
                                    selectedBitmap = null
                                    val file = selectedFile
                                    selectedFile = null
                                    
                                    val detected = analyzeSentiment(prompt)
                                    if (detected != CharacterExpression.IDLE) {
                                        userSentimentExpression = detected
                                    }
                                    viewModel.sendMessage(prompt, bmp, file, context)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gönder",
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (isListening) {
                                        speechRecognizer.stopListening()
                                        isListening = false
                                        soundLevel = 0f
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Sesle Konuş",
                                    tint = if (isListening) Color(0xFFFF1744) else Color.LightGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // FLOATING DRAGGABLE GUNDI BUBBLE (HAREKETLİ BALONCUK)
                val currentSpeechText by BubbleStateManager.currentSpeech.collectAsState(initial = null)
                val bubbleInfiniteTransition = rememberInfiniteTransition(label = "floating_bubble")

                val (emotionLabel, emotionEmoji, emotionColor) = when (expression) {
                    CharacterExpression.JOY -> Triple("Mutlu & Neşeli", "😊", Color(0xFFFFD54F))
                    CharacterExpression.SADNESS -> Triple("Duygusal & Dertli", "😢", Color(0xFF64B5F6))
                    CharacterExpression.SURPRISE -> Triple("Şaşırmış", "😲", Color(0xFFFF8A65))
                    CharacterExpression.THINKING -> Triple("Düşünüyor", "🧠", Color(0xFFBA68C8))
                    CharacterExpression.LISTENING -> Triple("Seni Dinliyor", "🎙️", Color(0xFF81C784))
                    CharacterExpression.SPEAKING -> Triple("Konuşuyor", "💬", Color(0xFF4DB6AC))
                    else -> Triple("Sakin & Dengeli", "🧘‍♂️", Color(0xFF9E9E9E))
                }
                
                // Rotation angle for active/speaking state
                val rotationAngle by bubbleInfiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "bubble_rotation_anim"
                )

                val bubbleDrift by bubbleInfiniteTransition.animateFloat(
                    initialValue = -5f,
                    targetValue = 5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bubble_drift"
                )

                // Render the dynamic emotion display system instead of speech balloon
                AnimatedVisibility(
                    visible = true, // Always show Gundi's emotion state on main screen
                    enter = fadeIn() + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)),
                    exit = fadeOut() + scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)),
                    modifier = Modifier.offset {
                        // Display to the left of Gundi if Gundi is on the right half of screen
                        val screenWidth = context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density
                        val xOffset = if (bubbleOffsetX > screenWidth / 2) -140f else 90f
                        IntOffset(
                            (bubbleOffsetX + xOffset).roundToInt(),
                            (bubbleOffsetY + bubbleDrift + 10f).roundToInt()
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E1F24), Color(0xFF15161A))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.8.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(emotionColor, emotionColor.copy(alpha = 0.4f), emotionColor)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = emotionEmoji,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = emotionLabel,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Main Draggable Mascot Bubble
                val dynamicScale = 1.0f + (soundLevel.coerceIn(0f, 15f) / 35f)
                Box(
                    modifier = Modifier
                        .offset { 
                            IntOffset(
                                bubbleOffsetX.roundToInt(), 
                                (bubbleOffsetY + bubbleDrift).roundToInt()
                            ) 
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                bubbleOffsetX += dragAmount.x
                                bubbleOffsetY += dragAmount.y
                            }
                        }
                        .graphicsLayer {
                            scaleX = dynamicScale
                            scaleY = dynamicScale
                            // Rotate only if soundLevel is active (listening or speaking)
                            if (soundLevel > 1.5f || isListening || isTtsSpeaking) {
                                rotationZ = rotationAngle
                            }
                        }
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (soundLevel > 1.5f || isListening || isTtsSpeaking) {
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFFFFD54F),
                                        Color(0xFFFF1744),
                                        Color(0xFF00E5FF),
                                        Color(0xFF00E676),
                                        Color(0xFFFFD54F)
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD54F).copy(alpha = 0.35f),
                                        Color(0xFFFF1744).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            }
                        )
                        .border(
                            width = if (soundLevel > 1.5f) 4.dp else 3.dp,
                            brush = Brush.sweepGradient(
                                colors = if (soundLevel > 1.5f || isListening || isTtsSpeaking) {
                                    listOf(Color(0xFF00E5FF), Color(0xFFFFD54F), Color(0xFFFF1744), Color(0xFF00E5FF))
                                } else {
                                    listOf(Color(0xFFFF1744), Color(0xFFFFD54F), Color(0xFF00E5FF), Color(0xFFFF1744))
                                }
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            // Speak a fun Gundi quote!
                            val quotes = listOf(
                                "Ooo bro! Karizma seviyemiz tavan yapmış, dök içini Gundi'ne! 😎",
                                "Ne bakıyon şefim, çizelim mi sana şık bir görsel? 🎨",
                                "Gundi Bro her zaman yanında başkan! sırlar paneli bizde güvende. 👌",
                                "Ben buraların kralıyım bro, bi alo demen yeter! 🚀",
                                "Ooo, beni ekranın neresine istersen oraya sürükle gundi, dert etme! 😂",
                                "Yazma konsolundan bana dilediğini sor, dertleşelim gundi baddi! 👑",
                                "Çaylar hazır mı bro? Muhabbetin belini kıralım. ☕"
                            )
                            val randomQuote = quotes.random()
                            Toast.makeText(context, "GUNDİ Bro: \"$randomQuote\"", Toast.LENGTH_LONG).show()
                            if (isTtsEnabled) {
                                val cleanQuote = cleanTextForTts(randomQuote)
                                tts?.speak(cleanQuote, TextToSpeech.QUEUE_FLUSH, null, "BUBBLE_QUOTE_ID")
                                BubbleStateManager.updateSpeech(cleanQuote)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isTtsSpeaking || soundLevel > 1.5f) {
                        val ringScale by bubbleInfiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "bubble_ring"
                        )
                        val ringAlpha by bubbleInfiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 0.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "bubble_ring_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    scaleX = ringScale
                                    scaleY = ringScale
                                    alpha = ringAlpha
                                }
                                .border(
                                    width = 2.5.dp,
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFF00E5FF), Color(0xFFFF1744), Color(0xFFFFD54F))
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    CuteExpressionCharacter(
                        expression = expression,
                        soundLevel = soundLevel,
                        modifier = Modifier.size(64.dp),
                        avatarStyle = gundiAvatar
                    )

                    // Dynamic, pulsing floating Emotion Badge on Gundi
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(24.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(emotionColor, emotionColor.copy(alpha = 0.8f))
                                ),
                                shape = CircleShape
                            )
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emotionEmoji,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showVoiceToneSelector,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                VoiceToneSelector(
                    tts = tts,
                    onClose = { showVoiceToneSelector = false }
                )
            }
        }

                // Overlay when listening with active chat
                if (isListening && messages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .clickable {
                                speechRecognizer.stopListening()
                                isListening = false
                                soundLevel = 0f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            VoiceReactiveOrb(
                                isListening = true,
                                soundLevel = soundLevel
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CuteExpressionCharacter(
                                expression = CharacterExpression.LISTENING,
                                soundLevel = soundLevel,
                                modifier = Modifier.size(120.dp),
                                avatarStyle = gundiAvatar
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Konuşun, dinliyorum...",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Durdurmak için ekrana dokunun",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        if (SocialMediaAnalyzerState.showAnalyzer) {
            SocialMediaAnalyzerDialog(
                onDismiss = { SocialMediaAnalyzerState.showAnalyzer = false }
            )
        }

        if (AssistantHubState.showAssistantHub) {
            AssistantHubDialog(
                onDismiss = { AssistantHubState.showAssistantHub = false },
                onGoToSession = { sessionId ->
                    viewModel.selectSession(sessionId)
                    AssistantHubState.showAssistantHub = false
                },
                onSendImageToChat = { bitmap, prompt ->
                    viewModel.insertMessageDirectly(prompt, bitmap)
                },
                tts = tts
            )
        }

        activeZoomedImage?.let { zoomedBmp ->
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { activeZoomedImage = null }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { activeZoomedImage = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Image(
                            bitmap = zoomedBmp.asImageBitmap(),
                            contentDescription = "Büyütülmüş Görsel",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { activeZoomedImage = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Kapat", color = Color.White)
                        }
                    }
                }
            }
        }

        if (showLanguageDialog) {
            val languages = listOf(
                "Turkish" to "Türkçe (Varsayılan) 🇹🇷",
                "English" to "English (İngilizce) 🇬🇧",
                "German" to "Deutsch (Almanca) 🇩🇪",
                "Azerbaijani" to "Azərbaycanca (Azerbaycan) 🇦🇿",
                "Kurdish" to "Kurmancî (Kürtçe) ☀️"
            )
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("Dil Seçimi", color = Color.White) },
                containerColor = Color(0xFF1E1B24),
                text = {
                    Column {
                        languages.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        settingsManager.setLanguage(key)
                                        showLanguageDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = language == key,
                                    onClick = {
                                        settingsManager.setLanguage(key)
                                        showLanguageDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD54F))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text("Kapat", color = Color(0xFFFFD54F))
                    }
                }
            )
        }

        if (showThemeDialog) {
            val themes = listOf(
                "Dark" to "Asil Siyah & Koyu Gece 🌌",
                "Light" to "Sade & Aydınlık Gündüz ☀️",
                "Matrix" to "Hacker Yeşili & Terminal 👾",
                "Crimson" to "Al Kırmızı & Şanlı Bayrak 🇹🇷"
            )
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Tema Seçimi", color = Color.White) },
                containerColor = Color(0xFF1E1B24),
                text = {
                    Column {
                        themes.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        settingsManager.setTheme(key)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = theme == key,
                                    onClick = {
                                        settingsManager.setTheme(key)
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD54F))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Kapat", color = Color(0xFFFFD54F))
                    }
                }
            )
        }
    }
}

enum class GundiHumorLevel {
    CIDDI, KOMIK, COK_ESPRILI
}

fun detectGundiHumorLevel(text: String): GundiHumorLevel {
    val t = text.lowercase(java.util.Locale("tr", "TR"))
    
    // Check for "Çok Esprili" markers (high energy laughter, casual slang, extreme emojis)
    val wittyKeywords = listOf(
        "hahaha", "ahahaha", "puhaha", "😂", "🤣", "🤪", "😜", "kop", "kopmak", 
        "patla", "patladım", "fıkra", "mizah", "esprili", "espri", "baddi", "kralsın",
        "alem", "bombasın", "koptum"
    )
    // Check for "Komik" markers (standard casual tone, soft smiles, friendly teasing)
    val funnyKeywords = listOf(
        "şaka", "komik", "deli", "valla", "yahu", "bro", "kral", "başkan", "şefim",
        "muhabbet", "çorba", "kanka", "😄", "😉", "😎", "😅", "😏", "baba"
    )
    
    var wittyScore = 0
    var funnyScore = 0
    
    for (kw in wittyKeywords) {
        if (t.contains(kw)) {
            wittyScore += 2
        }
    }
    // Safe count of emoji strings
    wittyScore += (t.split("😂").size - 1) * 2
    wittyScore += (t.split("🤣").size - 1) * 2
    
    for (kw in funnyKeywords) {
        if (t.contains(kw)) {
            funnyScore += 1
        }
    }
    funnyScore += t.split("😄").size - 1
    funnyScore += t.split("😉").size - 1
    funnyScore += t.split("😎").size - 1
    
    return when {
        wittyScore >= 3 -> GundiHumorLevel.COK_ESPRILI
        (wittyScore + funnyScore) >= 1 -> GundiHumorLevel.KOMIK
        else -> GundiHumorLevel.CIDDI
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onImageClick: (Bitmap) -> Unit,
    onExportClick: (String) -> Unit,
    onSpeakClick: ((String, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val displayCleanText = remember(message.text) { cleanMessageText(message.text) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val gundiAvatar by settingsManager.gundiAvatar.collectAsState()
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
 
    val borderColors = listOf(Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC))
    val bubbleBorderColor = borderColors[message.id.hashCode().absoluteValue % borderColors.size]
    
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isUser) 16.dp else 4.dp,
        bottomEnd = if (message.isUser) 4.dp else 16.dp
    )

    // Detect Gundi's Humor Level and customize gradient background
    val humorLevel = if (!message.isUser) detectGundiHumorLevel(message.text) else GundiHumorLevel.CIDDI

    val bubbleModifier = if (!message.isUser) {
        val (gradientColors, currentBorderColor) = when (humorLevel) {
            GundiHumorLevel.COK_ESPRILI -> listOf(Color(0xFFD500F9), Color(0xFFFF1744), Color(0xFF00E5FF)) to Color(0xFF00E5FF)
            GundiHumorLevel.KOMIK -> listOf(Color(0xFFFF9100), Color(0xFFFFD54F), Color(0xFF00E676)) to Color(0xFFFFD54F)
            GundiHumorLevel.CIDDI -> listOf(Color(0xFF263238), Color(0xFF37474F)) to Color(0xFF90A4AE)
        }
        Modifier
            .clip(bubbleShape)
            .border(1.5.dp, currentBorderColor, bubbleShape)
            .background(brush = Brush.linearGradient(colors = gradientColors))
    } else {
        Modifier
            .clip(bubbleShape)
            .border(1.5.dp, bubbleBorderColor, bubbleShape)
            .background(bgColor)
    }
 
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = alignment
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!message.isUser) {
                val currentBorderColor = when (humorLevel) {
                    GundiHumorLevel.COK_ESPRILI -> Color(0xFF00E5FF)
                    GundiHumorLevel.KOMIK -> Color(0xFFFFD54F)
                    GundiHumorLevel.CIDDI -> Color(0xFF90A4AE)
                }
                val parsedExpression = remember(message.text) {
                    parseGundiEmotion(message.text) ?: when (humorLevel) {
                        GundiHumorLevel.COK_ESPRILI -> CharacterExpression.JOY
                        GundiHumorLevel.KOMIK -> CharacterExpression.LISTENING
                        GundiHumorLevel.CIDDI -> CharacterExpression.THINKING
                    }
                }
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, currentBorderColor, CircleShape)
                        .background(Color(0xFF1E1B24))
                ) {
                    CuteExpressionCharacter(
                        expression = parsedExpression,
                        soundLevel = 0f,
                        avatarStyle = gundiAvatar,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
 
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .then(bubbleModifier)
                    .paint(
                        painter = painterResource(id = R.drawable.gundi_app_icon_1784149123318),
                        contentScale = ContentScale.Crop,
                        alpha = if (message.isUser) 0.05f else 0.08f
                    )
                    .padding(8.dp)
            ) {
                Column {
                    // Humor Level Badge & Reactive Emojis Row
                    if (!message.isUser) {
                        val (badgeText, badgeBg, badgeTextColor) = when (humorLevel) {
                            GundiHumorLevel.COK_ESPRILI -> Triple("Gundi Bro: ÇOK ESPRİLİ 🔥🤣", Color(0xFFD500F9).copy(alpha = 0.35f), Color(0xFF00E5FF))
                            GundiHumorLevel.KOMIK -> Triple("Gundi Bro: KOMİK 😎🤙", Color(0xFFFF9100).copy(alpha = 0.35f), Color(0xFFFFD54F))
                            GundiHumorLevel.CIDDI -> Triple("Gundi Bro: CİDDİ 🧠💼", Color(0xFF546E7A).copy(alpha = 0.35f), Color(0xFF90A4AE))
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeTextColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            
                            ReactiveGundiEmojis(
                                text = message.text,
                                humorLevel = humorLevel
                            )
                        }
                    }
                    if (message.drawableResId != null) {
                        Image(
                            painter = painterResource(id = message.drawableResId),
                            contentDescription = "Görsel",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (message.bitmap != null) {
                        val imgWidth = message.bitmap.width.toFloat()
                        val imgHeight = message.bitmap.height.toFloat()
                        val aspectRatio = if (imgHeight > 0) imgWidth / imgHeight else 1f
                        Image(
                            bitmap = message.bitmap.asImageBitmap(),
                            contentDescription = "Görüntü",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio)
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(message.bitmap) },
                            contentScale = ContentScale.Fit
                        )
                    }
                    if (message.attachedFile != null) {
                        val file = message.attachedFile
                        val isVideo = file.mimeType.contains("video", true) || 
                                      file.name.endsWith(".mp4", true) || 
                                      file.name.endsWith(".avi", true)
                        val isAudio = file.mimeType.contains("audio", true) || 
                                      file.name.endsWith(".mp3", true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Default.Movie else if (isAudio) Icons.Default.MusicNote else Icons.Default.InsertDriveFile,
                                    contentDescription = "Dosya",
                                    tint = textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(file.name, color = textColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (isVideo || isAudio) {
                                IconButton(
                                    onClick = { MediaPlayerManager.playAttachedFile(context, file) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Oynat",
                                        tint = textColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (displayCleanText.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = displayCleanText,
                                color = textColor,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    // Action icons row (Copy, Share, Export) for ALL messages
                    if (displayCleanText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(displayCleanText))
                                    Toast.makeText(context, "Metin Kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, displayCleanText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Paylaş"))
                                },
                                modifier = Modifier.size(26.dp)
                             ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Paylaş",
                                    tint = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            if (!message.isUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        onSpeakClick?.invoke(displayCleanText, message.audioBase64)
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = if (!message.audioBase64.isNullOrBlank()) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        contentDescription = "Dinle",
                                        tint = if (!message.audioBase64.isNullOrBlank()) Color(0xFFFFD54F) else textColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        onExportClick(message.text)
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Dışarı Aktar",
                                        tint = textColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (message.isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, Color(0xFF8D6E63), CircleShape) // Brown teddy bear accent border
                        .background(Color(0xFF1E1B24)),
                    contentAlignment = Alignment.Center
                ) {
                    CuteExpressionCharacter(
                        expression = CharacterExpression.IDLE,
                        soundLevel = 0f,
                        avatarStyle = "teddy", // Minion holding teddy bear!
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

fun parseGundiEmotion(text: String): CharacterExpression? {
    val regex = """\[DUYGU:\s*([^\]]+)\]""".toRegex(RegexOption.IGNORE_CASE)
    val matchResult = regex.find(text) ?: return null
    val emotionStr = matchResult.groupValues[1].uppercase().trim()
    return when {
        emotionStr.contains("MUTLU") || emotionStr.contains("NEŞELİ") || emotionStr.contains("JOY") -> CharacterExpression.JOY
        emotionStr.contains("DERTLİ") || emotionStr.contains("DUYGUSAL") || emotionStr.contains("SADNESS") || emotionStr.contains("ÜZGÜN") -> CharacterExpression.SADNESS
        emotionStr.contains("ŞAŞ") || emotionStr.contains("SURPRISE") || emotionStr.contains("ŞOK") -> CharacterExpression.SURPRISE
        emotionStr.contains("DÜŞÜN") || emotionStr.contains("THINKING") -> CharacterExpression.THINKING
        emotionStr.contains("DİNL") || emotionStr.contains("LISTENING") -> CharacterExpression.LISTENING
        emotionStr.contains("KONUŞ") || emotionStr.contains("SPEAKING") -> CharacterExpression.SPEAKING
        else -> null
    }
}

fun cleanMessageText(text: String): String {
    return text.replace("""\[DUYGU:\s*([^\]]+)\]""".toRegex(RegexOption.IGNORE_CASE), "").trim()
}

private fun analyzeSentiment(text: String): CharacterExpression {
    val lowerText = text.lowercase()
    
    // Turkish keywords for Joy/Sevinç
    val joyKeywords = listOf(
        "harika", "süper", "muhteşem", "harikasın", "çok iyi", "mutlu", "sevindim", "yaşasın", "teşekkür", "tebrik",
        "gül", "komik", "güzel", "iyi", "sevdim", "haha", "muazzam", "şaheser", "sevinç", "başardım", "kazandım",
        "mutluyum", "güzeldir", "en iyi", "mükemmel", "keyif", "keyifli", "neşeli", "canım", "dostum"
    )
    
    // Turkish keywords for Sadness/Üzüntü
    val sadnessKeywords = listOf(
        "kötü", "üzgün", "üzüldüm", "malesef", "maalesef", "ağla", "canım sıkıldı", "moralim bozuk", "mutsuz", "acı",
        "keder", "dert", "bıktım", "yoruldum", "korku", "korkuyorum", "yalnız", "kaybettim", "başaramadım", "hüzün",
        "hüzünlü", "kırgın", "kırıldım", "üzücü", "yazık", "of", "off", "ah", "tüh", "perişan"
    )
    
    // Turkish keywords for Surprise/Şaşkınlık
    val surpriseKeywords = listOf(
        "nasıl", "nası", "ciddi mi", "şaşırdım", "şaka", "inanılmaz", "vov", "wow", "oh", "yok artık", "ne?", 
        "gerçekten mi", "şaşkın", "aman tanrım", "oha", "hadi canım", "ilginç", "tuhaf", "beklemiyordum", 
        "şaşırtıcı", "hayret", "farklı", "garip"
    )
    
    return when {
        joyKeywords.any { lowerText.contains(it) } -> CharacterExpression.JOY
        sadnessKeywords.any { lowerText.contains(it) } -> CharacterExpression.SADNESS
        surpriseKeywords.any { lowerText.contains(it) } -> CharacterExpression.SURPRISE
        else -> CharacterExpression.IDLE
    }
}
