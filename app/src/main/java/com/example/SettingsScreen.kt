package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }

    // Read state flows from settingsManager
    val nickname by settingsManager.nickname.collectAsState()
    val theme by settingsManager.theme.collectAsState()
    val language by settingsManager.language.collectAsState()
    val creativity by settingsManager.creativity.collectAsState()
    val replyMode by settingsManager.replyMode.collectAsState()
    val emotionSensitivity by settingsManager.emotionSensitivity.collectAsState()
    val speechSpeed by settingsManager.speechSpeed.collectAsState()
    val continuousListening by settingsManager.continuousListening.collectAsState()
    val voiceTrigger by settingsManager.voiceTrigger.collectAsState()
    val screenScanRate by settingsManager.screenScanRate.collectAsState()
    val hapticFeedback by settingsManager.hapticFeedback.collectAsState()
    val debugLogging by settingsManager.debugLogging.collectAsState()
    val isTtsEnabled by settingsManager.isTtsEnabled.collectAsState()
    val isSoundEffectsEnabled by settingsManager.isSoundEffectsEnabled.collectAsState()
    val witLevel by settingsManager.witLevel.collectAsState()
    val gundiAvatar by settingsManager.gundiAvatar.collectAsState()
    val speechPitch by settingsManager.speechPitch.collectAsState()
    val voiceStyle by settingsManager.voiceStyle.collectAsState()
    val startupGreeting by settingsManager.startupGreeting.collectAsState()
    val isBubbleEnabled by settingsManager.isBubbleEnabled.collectAsState()
    val searchGrounding by settingsManager.searchGrounding.collectAsState()
    val customApiKey by settingsManager.customApiKey.collectAsState()
    val isProxyEnabled by settingsManager.isProxyEnabled.collectAsState()
    val proxyUrl by settingsManager.proxyUrl.collectAsState()
    val secondaryProxyUrl by settingsManager.secondaryProxyUrl.collectAsState()
    val isSecureKeyStripEnabled by settingsManager.isSecureKeyStripEnabled.collectAsState()
    val proxyAuthToken by settingsManager.proxyAuthToken.collectAsState()
    val proxyUsername by settingsManager.proxyUsername.collectAsState()
    val proxyPassword by settingsManager.proxyPassword.collectAsState()

    val isPasscodeEnabled by settingsManager.isPasscodeEnabled.collectAsState()
    val appPasscode by settingsManager.appPasscode.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf(nickname) }
    var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
    var startupGreetingInput by remember(startupGreeting) { mutableStateOf(startupGreeting) }
    var proxyUrlInput by remember(proxyUrl) { mutableStateOf(proxyUrl) }
    var secondaryProxyUrlInput by remember(secondaryProxyUrl) { mutableStateOf(secondaryProxyUrl) }
    var proxyAuthTokenInput by remember(proxyAuthToken) { mutableStateOf(proxyAuthToken) }
    var proxyUsernameInput by remember(proxyUsername) { mutableStateOf(proxyUsername) }
    var proxyPasswordInput by remember(proxyPassword) { mutableStateOf(proxyPassword) }

    var appPasscodeInput by remember(appPasscode) { mutableStateOf(appPasscode) }
    var isPasscodeVisible by remember { mutableStateOf(false) }

    var isApiKeyVisible by remember { mutableStateOf(false) }

    var isProxyTokenVisible by remember { mutableStateOf(false) }
    var isProxyPasswordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "GUNDİ BRO",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFD54F),
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Ayarları", 
                            fontWeight = FontWeight.Bold, 
                            color = Color.White
                        ) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Geri", 
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1B24),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121016)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Dashboard Header
            SettingsHeroHeader(nickname = nickname, language = language)

            // Gundi Bro Avatar Selector Panel
            GundiAvatarSelectorCard(
                settingsManager = settingsManager,
                currentAvatar = gundiAvatar
            )

            // App Launcher Icon & Theme Selector
            AppIconSettingsCard(
                settingsManager = settingsManager
            )

            // --- SECTION 1: GENEL AYARLAR ---
            SettingsCategoryCard(
                title = "Genel Ayarlar",
                icon = Icons.Default.Settings,
                tint = Color(0xFFFFD54F)
            ) {
                // Nickname Config
                Text(
                    text = "Kullanıcı Takma Adı (Rumuz)",
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFFD54F)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini API Key Config
                Text(
                    text = "Gemini API Anahtarı (İsteğe Bağlı)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { 
                        apiKeyInput = it
                        settingsManager.setCustomApiKey(it)
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
                    placeholder = { Text("Boş bırakılırsa varsayılan veya 2. anahtar kullanılır", color = Color.Gray, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFFFD54F)) },
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Şifre Görünürlüğü",
                                tint = Color(0xFFFFD54F)
                            )
                        }
                    },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Language Selection Selector
                SettingsClickableItem(
                    title = "Uygulama & Konuşma Dili",
                    subtitle = getLanguageDisplayName(language),
                    icon = Icons.Default.Language,
                    tint = Color(0xFF64B5F6)
                ) {
                    showLanguageDialog = true
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Selector
                SettingsClickableItem(
                    title = "Görsel Arayüz Teması",
                    subtitle = getThemeDisplayName(theme),
                    icon = Icons.Default.Palette,
                    tint = Color(0xFFBA68C8)
                ) {
                    showThemeDialog = true
                }
            }

            // --- SECTION 1B: GÜVENLİK AYARLARI ---
            SettingsCategoryCard(
                title = "Giriş Şifresi & Güvenlik",
                icon = Icons.Default.Lock,
                tint = Color(0xFFE57373)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { settingsManager.setIsPasscodeEnabled(!isPasscodeEnabled) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Şifreli Giriş Kilidi", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Uygulama açılışında şifre sor", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                    Switch(
                        checked = isPasscodeEnabled,
                        onCheckedChange = { settingsManager.setIsPasscodeEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE57373))
                    )
                }

                if (isPasscodeEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Giriş Şifresi / PIN Kodu",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = appPasscodeInput,
                        onValueChange = { 
                            appPasscodeInput = it
                            settingsManager.setAppPasscode(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE57373),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        placeholder = { Text("Uygulama şifresini yazın (Örn: 1234)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFE57373)) },
                        trailingIcon = {
                            IconButton(onClick = { isPasscodeVisible = !isPasscodeVisible }) {
                                Icon(
                                    imageVector = if (isPasscodeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Şifre Görünürlüğü",
                                    tint = Color(0xFFE57373)
                                )
                            }
                        },
                        visualTransformation = if (isPasscodeVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )
                }
            }

            // --- SECTION 2: YAPAY ZEKA VE SOHBET AYARLARI ---

            SettingsCategoryCard(
                title = "Yapay Zeka & Sohbet",
                icon = Icons.Default.Psychology,
                tint = Color(0xFF81C784)
            ) {
                // Creativity Level
                Text(
                    text = "Gelecek Yanıt Yoğunluğu (Yaratıcılık)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Balanced" to "Dengeli", "Funny" to "Eğlenceli", "Scientific" to "Ciddi").forEach { (key, label) ->
                        val isSelected = creativity == key
                        Button(
                            onClick = { settingsManager.setCreativity(key) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF81C784) else Color.White.copy(alpha = 0.05f),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wit Level Selector (Ciddi, Komik, Çok Esprili)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SentimentVerySatisfied,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GUNDİ Mizah Seviyesi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val levels = listOf(
                        Triple(1.0f, "Ciddi 😐", "Ciddi"),
                        Triple(3.0f, "Komik 😄", "Komik"),
                        Triple(5.0f, "Çok Esprili 🚀", "Çok Esprili")
                    )
                    levels.forEach { (value, label, name) ->
                        val isSelected = when (value) {
                            1.0f -> witLevel <= 1.5f
                            3.0f -> witLevel > 1.5f && witLevel <= 3.5f
                            else -> witLevel > 3.5f
                        }
                        Button(
                            onClick = { settingsManager.setWitLevel(value) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.05f),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val descriptionText = when {
                    witLevel <= 1.5f -> "😐 GUNDİ şakaları bırakır, tamamen ciddi, profesyonel, mesafeli ve tamamen teknik konuşur."
                    witLevel <= 3.5f -> "😄 GUNDİ Bro samimi, dostane, doğal espriler yapan, tatlı dilli klasik bir dost olur."
                    else -> "🚀 Aşırı espiritüel, hicivli, her cümlede yaratıcı espriler ve komik Gundi nidaları kullanan çılgın komedyen!"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reply Mode Option
                SettingsSwitchItem(
                    title = "Hızlı Yanıt Modu (Kısa ve Öz)",
                    subtitle = "Saniyeler kazanmak için cevapları son derece doğrudan ve kısa tutar.",
                    checked = replyMode == "Short",
                    onCheckedChange = { settingsManager.setReplyMode(if (it) "Short" else "Detailed") },
                    icon = Icons.Default.FlashOn,
                    tint = Color(0xFFFFB74D)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Grounding Option
                SettingsSwitchItem(
                    title = "Google Arama Entegrasyonu (Grounding)",
                    subtitle = "Haberler, canlı veriler ve güncel bilgileri Google Arama ile gerçek zamanlı sorgular.",
                    checked = searchGrounding,
                    onCheckedChange = { settingsManager.setSearchGrounding(it) },
                    icon = Icons.Default.Search,
                    tint = Color(0xFF64B5F6)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Emotion Sensitivity
                Text(
                    text = "Duygu Analizör Hassasiyeti",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low" to "Düşük", "Medium" to "Orta", "High" to "Yüksek").forEach { (key, label) ->
                        val isSelected = emotionSensitivity == key
                        Button(
                            onClick = { settingsManager.setEmotionSensitivity(key) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF4DB6AC) else Color.White.copy(alpha = 0.05f),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- SECTION 3: SES VE KONUŞMA AYARLARI ---
            SettingsCategoryCard(
                title = "Ses & Konuşma (TTS)",
                icon = Icons.Default.VolumeUp,
                tint = Color(0xFF4FC3F7)
            ) {
                // Voice / Silent toggle switch
                SettingsSwitchItem(
                    title = "Sesli Yanıt (TTS) Aktif",
                    subtitle = "GUNDİ Bro yanıtlarını sesli olarak seslendirsin. Kapatırsanız sessizce okuyabilirsiniz.",
                    checked = isTtsEnabled,
                    onCheckedChange = { settingsManager.setIsTtsEnabled(it) },
                    icon = Icons.Default.RecordVoiceOver,
                    tint = Color(0xFF4FC3F7)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSwitchItem(
                    title = "Duygu Ses Efektleri Aktif 🔔",
                    subtitle = "GUNDİ Bro'nun o anki ruh haline ve duygusuna göre esprili ses efektleri ve kısa ses klipleri çalınır.",
                    checked = isSoundEffectsEnabled,
                    onCheckedChange = { settingsManager.setIsSoundEffectsEnabled(it) },
                    icon = Icons.Default.NotificationsActive,
                    tint = Color(0xFFFFD54F)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- VOICE PRESET STYLE SELECTOR ---
                Text(
                    text = "Farklı Ses Sentezi Karakterleri",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Gündi'nin konuşma tarzını tek tıkla değiştir bro!",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                val voiceSkins = listOf(
                    Triple("classic", "Klasik Gündi 🌟", Color(0xFFFFD54F)),
                    Triple("squeaky", "Tiz/Bebek 👶", Color(0xFF81C784)),
                    Triple("deep", "Tok/Babacan 🎙️", Color(0xFF4FC3F7)),
                    Triple("excited", "Heyecanlı DJ 🎧", Color(0xFFEC407A)),
                    Triple("robotic", "Siber Robot 🤖", Color(0xFF00E676))
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(voiceSkins) { (id, label, color) ->
                        val isSelected = voiceStyle == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) color else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { settingsManager.setVoiceStyle(id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- CUSTOM STARTUP GREETING ---
                Text(
                    text = "Açılış Karşılama Mesajı",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Uygulama açıldığında kralına söyleyeceği özel sesli karşılama:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startupGreetingInput,
                        onValueChange = { startupGreetingInput = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        placeholder = { Text("Örn: Merhaba kralım, hoş geldin!", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4FC3F7),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = { settingsManager.setStartupGreeting(startupGreetingInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speech Rate Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Konuşma Hızı (Speed)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${String.format("%.2f", speechSpeed)}x",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4FC3F7)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = speechSpeed,
                    onValueChange = { settingsManager.setSpeechSpeed(it) },
                    valueRange = 0.5f..1.5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4FC3F7),
                        activeTrackColor = Color(0xFF4FC3F7),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Ses Tizliği (Speech Pitch) Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ses Tizliği (Pitch)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${String.format("%.2f", speechPitch)}x",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = speechPitch,
                    onValueChange = { settingsManager.setSpeechPitch(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF81C784),
                        activeTrackColor = Color(0xFF81C784),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Continuous Mic Listening
                SettingsSwitchItem(
                    title = "Kesintisiz Dinleme Modu",
                    subtitle = "Yanıt bittikten sonra mikrofonu otomatik olarak tekrar aktif hale getirir.",
                    checked = continuousListening,
                    onCheckedChange = { settingsManager.setContinuousListening(it) },
                    icon = Icons.Default.Mic,
                    tint = Color(0xFF81C784)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Activation (Hey Gundi)
                SettingsSwitchItem(
                    title = "\"Hey Gundi\" Aktivasyonu",
                    subtitle = "Arka planda sesinizi dinleyerek uyanmasını sağlar.",
                    checked = voiceTrigger,
                    onCheckedChange = { settingsManager.setVoiceTrigger(it) },
                    icon = Icons.Default.Hearing,
                    tint = Color(0xFFE57373)
                )

            }

            // --- SECTION 4: ERİŞİLEBİLİRLİK VE DİĞERLERİ ---
            SettingsCategoryCard(
                title = "Erişilebilirlik & Bildirimler",
                icon = Icons.Default.Accessibility,
                tint = Color(0xFFE57373)
            ) {
                // Screen Scan speed Selection
                Text(
                    text = "Ekran Tarayıcı Çözümleme Hızı",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Slow" to "Yavaş", "Normal" to "Normal", "Fast" to "Hızlı").forEach { (key, label) ->
                        val isSelected = screenScanRate == key
                        Button(
                            onClick = { settingsManager.setScreenScanRate(key) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFE57373) else Color.White.copy(alpha = 0.05f),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Haptic Feedback Switch
                SettingsSwitchItem(
                    title = "Dokunsal Geri Bildirim (Haptic)",
                    subtitle = "Her mesaj alındığında ve işlemlerde kısa cihaz titreşimi sağlar.",
                    checked = hapticFeedback,
                    onCheckedChange = { settingsManager.setHapticFeedback(it) },
                    icon = Icons.Default.Vibration,
                    tint = Color(0xFFFFB74D)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Debug Logging
                SettingsSwitchItem(
                    title = "Sistem Geliştirici Logları",
                    subtitle = "Olası bağlantı hatalarını detaylı incelemek için debug kaydı tutar.",
                    checked = debugLogging,
                    onCheckedChange = { settingsManager.setDebugLogging(it) },
                    icon = Icons.Default.BugReport,
                    tint = Color(0xFFBA68C8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Floating Speech Bubble Switch
                SettingsSwitchItem(
                    title = "Yüzen Konuşma Balonu (Floating Bubble)",
                    subtitle = "Gündi'nin konuşmalarını ekran üzerinde şirin bir yüzen balon olarak gösterir.",
                    checked = isBubbleEnabled,
                    onCheckedChange = { settingsManager.setBubbleEnabled(it) },
                    icon = Icons.Default.ChatBubble,
                    tint = Color(0xFFFFD54F)
                )
            }

            // --- SECTION 5: GÜVENLİ PROXY SUNUCU ENTEGRASYONU ---
            SettingsCategoryCard(
                title = "Güvenli Proxy Entegrasyonu",
                icon = Icons.Default.Security,
                tint = Color(0xFF4CAF50)
            ) {
                SettingsSwitchItem(
                    title = "Güvenli Proxy Modu (Canlı Sürüm)",
                    subtitle = "Yapay zeka isteklerini doğrudan Google yerine kendi güvenli proxy sunucunuz üzerinden iletir.",
                    checked = isProxyEnabled,
                    onCheckedChange = { settingsManager.setIsProxyEnabled(it) },
                    icon = Icons.Default.Router,
                    tint = Color(0xFF81C784)
                )

                AnimatedVisibility(
                    visible = isProxyEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Proxy Sunucu Base URL",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = proxyUrlInput,
                            onValueChange = { 
                                proxyUrlInput = it
                                settingsManager.setProxyUrl(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            placeholder = { Text("https://your-proxy-domain.com/v1beta/", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF4CAF50)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "İkincil Proxy Sunucu Base URL (Yedek)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = secondaryProxyUrlInput,
                            onValueChange = { 
                                secondaryProxyUrlInput = it
                                settingsManager.setSecondaryProxyUrl(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            placeholder = { Text("https://your-secondary-proxy.com/", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF4CAF50)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Proxy Kullanıcı Adı (Basic Auth)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = proxyUsernameInput,
                            onValueChange = { 
                                proxyUsernameInput = it
                                settingsManager.setProxyUsername(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            placeholder = { Text("Örn: Bymix", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4CAF50)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Proxy Şifre (Basic Auth)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = proxyPasswordInput,
                            onValueChange = { 
                                proxyPasswordInput = it
                                settingsManager.setProxyPassword(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            placeholder = { Text("Örn: bymix1453", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF4CAF50)) },
                            trailingIcon = {
                                IconButton(onClick = { isProxyPasswordVisible = !isProxyPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isProxyPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Şifre Görünürlüğü",
                                        tint = Color(0xFF4CAF50)
                                    )
                                }
                            },
                            visualTransformation = if (isProxyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SettingsSwitchItem(
                            title = "API Anahtarını Gizle (Secure Strip)",
                            subtitle = "Cihazdaki API anahtarı proxy sunucusuna ASLA gönderilmez. Proxy, istekleri kendi sunucu tarafındaki anahtarla tamamlar.",
                            checked = isSecureKeyStripEnabled,
                            onCheckedChange = { settingsManager.setIsSecureKeyStripEnabled(it) },
                            icon = Icons.Default.Lock,
                            tint = Color(0xFFFFB74D)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Proxy Yetkilendirme Belirteci (Token - İsteğe Bağlı)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = proxyAuthTokenInput,
                            onValueChange = { 
                                proxyAuthTokenInput = it
                                settingsManager.setProxyAuthToken(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                            ),
                            placeholder = { Text("Örn: bearer_token_xyz", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF4CAF50)) },
                            trailingIcon = {
                                IconButton(onClick = { isProxyTokenVisible = !isProxyTokenVisible }) {
                                    Icon(
                                        imageVector = if (isProxyTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Şifre Görünürlüğü",
                                        tint = Color(0xFF4CAF50)
                                    )
                                }
                            },
                            visualTransformation = if (isProxyTokenVisible) VisualTransformation.None else PasswordVisualTransformation()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Version Display
            Text(
                text = "GUNDİ Bro v2.5.0 - Lisanslı Dijital Dost\n© 2026 Tüm Hakları Saklıdır.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }

    // --- DIALOGS FOR PICKERS ---

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
            title = { Text("Dil Seçimi") },
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
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Kapat")
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
            title = { Text("Tema Seçimi") },
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
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}

@Composable
fun SettingsHeroHeader(nickname: String, language: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B24))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE57373).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsSuggest,
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Selam, $nickname!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ayarların anlık olarak buluta ve yerel hafızaya senkronize ediliyor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B24))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            content()
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tint,
                checkedTrackColor = tint.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

private fun getLanguageDisplayName(langKey: String): String {
    return when (langKey) {
        "Turkish" -> "Türkçe 🇹🇷"
        "English" -> "English 🇬🇧"
        "German" -> "Deutsch 🇩🇪"
        "Azerbaijani" -> "Azərbaycanca 🇦🇿"
        "Kurdish" -> "Kurmancî ☀️"
        else -> "Türkçe 🇹🇷"
    }
}

private fun getThemeDisplayName(themeKey: String): String {
    return when (themeKey) {
        "Dark" -> "Asil Siyah 🌌"
        "Light" -> "Aydınlık Gündüz ☀️"
        "Matrix" -> "Hacker Yeşili 👾"
        "Crimson" -> "Al Kırmızı 🇹🇷"
        else -> "Asil Siyah 🌌"
    }
}

@Composable
fun GundiAvatarSelectorCard(
    settingsManager: SettingsManager,
    currentAvatar: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B24))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Gündi Bro Görünüm Seçici",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Gündi'nin tarzını dilediğin gibi özelleştir bro!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Live Preview Box with a glowing neon outline
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                when (currentAvatar) {
                                    "cool" -> Color(0xFF4FC3F7)
                                    "sultan" -> Color(0xFFFF5252)
                                    "cyber" -> Color(0xFF00E676)
                                    "gamer" -> Color(0xFFEC407A)
                                    else -> Color(0xFFFFD54F)
                                },
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Interactive dynamic Gundi Bro!
                CuteExpressionCharacter(
                    expression = CharacterExpression.JOY,
                    soundLevel = 1.5f, // Cute smiling & subtle talking mouth
                    modifier = Modifier.size(110.dp),
                    avatarStyle = currentAvatar
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Label of Active Appearance
            val activeName = when (currentAvatar) {
                "bymix" -> "BYMIX DJ 😎"
                "custom" -> "Özel Gündi 💝"
                "cool" -> "Karizmatik Gündi 😎"
                "sultan" -> "Sultan Gündi 🕌"
                "cyber" -> "Siberpunk Gündi 🤖"
                "gamer" -> "Oyuncu Gündi 🎮"
                "teddy" -> "Ayıcıklı Gündi 🧸"
                "placard" -> "Tabelalı Gündi 📢"
                else -> "Klasik Gündi 🌟"
            }
            Text(
                text = activeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = when (currentAvatar) {
                    "bymix" -> Color(0xFFFF1744)
                    "custom" -> Color(0xFFE57373)
                    "cool" -> Color(0xFF4FC3F7)
                    "sultan" -> Color(0xFFFF5252)
                    "cyber" -> Color(0xFF00E676)
                    "gamer" -> Color(0xFFEC407A)
                    "teddy" -> Color(0xFF8D6E63)
                    "placard" -> Color(0xFFFFB300)
                    else -> Color(0xFFFFD54F)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Selector Row
            val skins = listOf(
                Triple("classic", "Klasik", Color(0xFFFFD54F)),
                Triple("bymix", "BYMIX 😎", Color(0xFFFF1744)),
                Triple("custom", "Özel 💝", Color(0xFFE57373)),
                Triple("teddy", "Ayıcık", Color(0xFF8D6E63)),
                Triple("placard", "Tabela", Color(0xFFFFB300)),
                Triple("cool", "Cool", Color(0xFF4FC3F7)),
                Triple("sultan", "Sultan", Color(0xFFFF5252)),
                Triple("cyber", "Cyber", Color(0xFF00E676)),
                Triple("gamer", "Gamer", Color(0xFFEC407A))
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(skins) { (id, label, accentColor) ->
                    val isSelected = currentAvatar == id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { settingsManager.setGundiAvatar(id) }
                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(8.dp)
                            .width(64.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accentColor else accentColor.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Draw a tiny representation of Gundi for this skin
                            CuteExpressionCharacter(
                                expression = CharacterExpression.IDLE,
                                soundLevel = 0f,
                                modifier = Modifier.size(38.dp),
                                avatarStyle = id
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconSettingsCard(
    settingsManager: SettingsManager
) {
    var selectedStyle by remember { mutableStateOf("bymix") }
    var isApplied by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B24))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CropOriginal,
                    contentDescription = null,
                    tint = Color(0xFFFF1744),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Uygulama İkonu & Logo Seçici",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Ana ekrandaki ve uygulama içindeki başlatıcı ikonunu özelleştir bro!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Icon Live Device Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Simulated Android launcher icon container
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = when (selectedStyle) {
                                        "bymix" -> listOf(Color(0xFF121016), Color(0xFFFF1744))
                                        "classic" -> listOf(Color(0xFF1E1B24), Color(0xFFFFD54F))
                                        else -> listOf(Color(0xFF1A1A1A), Color(0xFF00E5FF))
                                    }
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CuteExpressionCharacter(
                            expression = CharacterExpression.JOY,
                            soundLevel = 0f,
                            avatarStyle = selectedStyle,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gündi Bro",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(Ana Ekran Önizlemesi)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val iconStyles = listOf(
                    "bymix" to "BYMIX DJ 😎",
                    "classic" to "Klasik Bro 🌟",
                    "cyber" to "Siberpunk 🤖"
                )
                iconStyles.forEach { (styleId, name) ->
                    val isSel = selectedStyle == styleId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) Color(0xFFFF1744).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) Color(0xFFFF1744) else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { 
                                selectedStyle = styleId
                                isApplied = false
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.White else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { isApplied = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isApplied) Color(0xFF00E676) else Color(0xFFFF1744)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isApplied) Icons.Default.CheckCircle else Icons.Default.InstallMobile,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isApplied) "İkon Başarıyla Uygulandı! 🎉" else "İkon Tasarımını Uygula",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            if (isApplied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Harika bro! Uygulama Başlatıcı İkonu seçilen tasarım olarak güncellendi. Değişiklik birkaç saniye içinde ana ekranına yansıyacaktır!",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00E676),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
