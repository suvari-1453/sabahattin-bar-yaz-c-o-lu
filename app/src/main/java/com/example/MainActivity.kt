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
            composable("login") { LoginScreen(tts = tts) { navController.navigate("chat") { popUpTo("login") { inclusive = true } } } }
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
fun LoginScreen(tts: TextToSpeech?, onLoginSuccess: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val gundiAvatar by settingsManager.gundiAvatar.collectAsState()
    val startupGreeting by settingsManager.startupGreeting.collectAsState()
    val speechSpeed by settingsManager.speechSpeed.collectAsState()
    val speechPitch by settingsManager.speechPitch.collectAsState()
    val isTtsEnabled by settingsManager.isTtsEnabled.collectAsState()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Premium Glowing Cyberpunk Logo (BYMIX DJ / Turkish Flag theme)
        Box(
            modifier = Modifier
                .size(170.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulse Glow behind logo
            val infiniteTransition = rememberInfiniteTransition(label = "logo_glow_pulse")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow"
            )

            Box(
                modifier = Modifier
                    .size(150.dp * glowScale)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F).copy(alpha = 0.25f),
                                Color(0xFFFF1744).copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Main Logo Image with sweep border
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFF1744), // Red
                                Color(0xFFFFD54F), // Gold
                                Color(0xFF00E5FF), // Cyber Cyan
                                Color(0xFFFF1744)  // Red
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                CuteExpressionCharacter(
                    expression = CharacterExpression.JOY,
                    soundLevel = 0f,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Seni anlayan, seninle konuşan ve gören yapay zeka dostun.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { isLoading = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD54F), // Premium Gundi Gold color
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

        Spacer(modifier = Modifier.height(32.dp))

        // Animated, eye-catching credit text for Sebahattin Barış Yazıcıoğlu
        var creditVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(400)
            creditVisible = true
        }

        AnimatedVisibility(
            visible = creditVisible,
            enter = fadeIn(animationSpec = tween(1200)) + expandVertically(animationSpec = tween(1000)),
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulsating_glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )

            val colorShift by infiniteTransition.animateColor(
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
    val gundiAvatar by settingsManager.gundiAvatar.collectAsState()
    var isTtsSpeaking by remember { mutableStateOf(false) }
    var userSentimentExpression by remember { mutableStateOf<CharacterExpression?>(null) }
    var activeZoomedImage by remember { mutableStateOf<Bitmap?>(null) }

    var isConsoleMode by remember { mutableStateOf(false) }
    var bubbleOffsetX by remember { mutableStateOf(100f) }
    var bubbleOffsetY by remember { mutableStateOf(350f) }
    var textToExport by remember { mutableStateOf<String?>(null) }
    var showVoiceToneSelector by remember { mutableStateOf(false) }

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
            isTtsSpeaking = tts?.isSpeaking == true
            delay(150)
        }
    }

    val expression = when {
        userSentimentExpression != null -> userSentimentExpression!!
        isLoading -> CharacterExpression.THINKING
        isListening -> CharacterExpression.LISTENING
        isTtsSpeaking -> CharacterExpression.SPEAKING
        else -> CharacterExpression.IDLE
    }

    // Auto-scroll and speak when new AI message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
            val lastMsg = messages.last()
            if (!lastMsg.isUser && isTtsEnabled) {
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
                val cleanSpeechText = cleanTextForTts(lastMsg.text)
                tts?.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, null, null)
                BubbleStateManager.updateSpeech(cleanSpeechText)
            }
        }
    }

    // STT Setup
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
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

                // Drawer bottom options: Profile and Settings
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
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1B24))
                                    .border(1.2.dp, Color(0xFFFFD54F), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CuteExpressionCharacter(
                                    expression = if (isListening) {
                                        CharacterExpression.LISTENING
                                    } else if (isTtsSpeaking) {
                                        CharacterExpression.SPEAKING
                                    } else {
                                        CharacterExpression.JOY
                                    },
                                    soundLevel = soundLevel,
                                    avatarStyle = gundiAvatar,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "GUNDİ BRO",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                                    fontWeight = FontWeight.Black
                                ),
                                fontSize = 18.sp,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        IconButton(onClick = { 
                            if (ScreenReaderState.isActive) {
                                ScreenReaderState.stop(tts)
                            } else {
                                readScreenContext(context, tts, "chat", messages)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Accessibility,
                                contentDescription = "Ekranı Oku",
                                tint = if (ScreenReaderState.isActive) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                        IconButton(onClick = { 
                            exportLauncher.launch("Sohbet_${System.currentTimeMillis()}.txt")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Sohbeti İndir",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { 
                            val nextVal = !isTtsEnabled
                            settingsManager.setIsTtsEnabled(nextVal)
                            if (!nextVal) {
                                tts?.stop()
                                Toast.makeText(context, "Gundi susturuldu! 🤫", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gundi sesli okuma aktif! 🔊", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Sesli Okuma"
                            )
                        }
                        if (isTtsSpeaking) {
                            IconButton(
                                onClick = { 
                                    tts?.stop()
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { 
                            showVoiceToneSelector = !showVoiceToneSelector
                        }) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Ses Tonu",
                                tint = if (showVoiceToneSelector) Color(0xFFFFD54F) else Color.White
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
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
                    modifier = Modifier
                        .fillMaxSize()
                        .paint(
                            painter = painterResource(id = R.drawable.turkish_flag_bg_1783589624111),
                            contentScale = ContentScale.Crop,
                            alpha = 0.15f
                        )
                ) {
                    GundiVoiceWave(
                        isSpeaking = isTtsSpeaking,
                        onMuteClick = {
                            tts?.stop()
                            Toast.makeText(context, "Gundi susturuldu! 🤫", Toast.LENGTH_SHORT).show()
                        }
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
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp)
                ) {
                    AnimatedVisibility(visible = selectedBitmap != null || selectedFile != null) {
                        Box(modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)) {
                            selectedBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            selectedFile?.let { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = "Dosya", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(file.name, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = Color.White)
                            }
                        }
                    }

                    if (isConsoleMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { isConsoleMode = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Sesli Moda Geç",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { documentPickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Dosya Ekle", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Resim Ekle", tint = MaterialTheme.colorScheme.primary)
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("gundi@bro:~$ Mesaj yazın...", color = Color(0xFF00FF00).copy(alpha = 0.5f), fontSize = 13.sp) },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color(0xFF00FF00),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0D0E12),
                                    unfocusedContainerColor = Color(0xFF0D0E12),
                                    focusedBorderColor = Color(0xFF00FF00),
                                    unfocusedBorderColor = Color(0xFF00FF00).copy(alpha = 0.4f),
                                    cursorColor = Color(0xFF00FF00)
                                ),
                                singleLine = true,
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
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
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
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Gönder",
                                            tint = if (inputText.isNotBlank()) Color(0xFF00FF00) else Color.Gray
                                        )
                                    }
                                }
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { isConsoleMode = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Yazma Konsoluna Geç",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { documentPickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Dosya Ekle", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Resim Ekle", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Infinite Transition for Chromatic Neon Rainbow Color Shift
                            val infiniteTransition = rememberInfiniteTransition(label = "color_shift")
                            val phase by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 6000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "phase"
                            )

                            // Generate smoothly shifting hue colors
                            val color1 = Color.hsv(phase, 0.75f, 0.95f)
                            val color2 = Color.hsv((phase + 120f) % 360f, 0.8f, 0.9f)
                            val color3 = Color.hsv((phase + 240f) % 360f, 0.85f, 0.85f)

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable {
                                        if (isListening) {
                                            speechRecognizer.stopListening()
                                            isListening = false
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                shape = RoundedCornerShape(28.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = if (isListening) {
                                                    listOf(Color(0xFFFF1744), Color(0xFFD500F9), Color(0xFFFF1744))
                                                } else {
                                                    listOf(color1, color2, color3)
                                                }
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = "Bas Konuş",
                                            tint = Color.Black,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .graphicsLayer {
                                                    if (isListening) {
                                                        val scale = 1.15f + 0.15f * kotlin.math.sin(phase * Math.PI / 180f).toFloat()
                                                        scaleX = scale
                                                        scaleY = scale
                                                    }
                                                }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isListening) "Dinliyorum Bro... 🎙️" else "Bas Konuş Bro 👑",
                                            color = Color.Black,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // FLOATING DRAGGABLE GUNDI BUBBLE (HAREKETLİ BALONCUK)
                val bubbleInfiniteTransition = rememberInfiniteTransition(label = "floating_bubble")
                val bubbleDrift by bubbleInfiniteTransition.animateFloat(
                    initialValue = -5f,
                    targetValue = 5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bubble_drift"
                )

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
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.35f),
                                    Color(0xFFFF1744).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(Color(0xFFFF1744), Color(0xFFFFD54F), Color(0xFF00E5FF), Color(0xFFFF1744))
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
                    if (isTtsSpeaking) {
                        val ringScale by bubbleInfiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "bubble_ring"
                        )
                        val ringAlpha by bubbleInfiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 0.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearOutSlowInEasing),
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
                                    width = 2.dp,
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFF00E5FF), Color(0xFFFF1744))
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
                onDismiss = { AssistantHubState.showAssistantHub = false }
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
    onExportClick: (String) -> Unit
) {
    val context = LocalContext.current
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
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!message.isUser) {
                val currentBorderColor = when (humorLevel) {
                    GundiHumorLevel.COK_ESPRILI -> Color(0xFF00E5FF)
                    GundiHumorLevel.KOMIK -> Color(0xFFFFD54F)
                    GundiHumorLevel.CIDDI -> Color(0xFF90A4AE)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, currentBorderColor, CircleShape)
                        .background(Color(0xFF1E1B24))
                ) {
                    CuteExpressionCharacter(
                        expression = when (humorLevel) {
                            GundiHumorLevel.COK_ESPRILI -> CharacterExpression.JOY
                            GundiHumorLevel.KOMIK -> CharacterExpression.LISTENING
                            GundiHumorLevel.CIDDI -> CharacterExpression.THINKING
                        },
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
                        painter = painterResource(id = R.drawable.turkish_flag_bg_1783589624111),
                        contentScale = ContentScale.Crop,
                        alpha = 0.15f
                    )
                    .padding(10.dp)
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
                    if (message.bitmap != null) {
                        Image(
                            bitmap = message.bitmap.asImageBitmap(),
                            contentDescription = "Görüntü",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(message.bitmap) },
                            contentScale = ContentScale.Crop
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
                    if (message.text.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = message.text,
                                color = textColor,
                                fontSize = 14.5.sp
                            )
                        }
                    }

                    // Action icons row (Copy, Share, Export) for ALL messages
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.text))
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
                                        putExtra(Intent.EXTRA_TEXT, message.text)
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
                // You could add a user avatar here if needed, but for now just spacing
            }
        }
    }
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
