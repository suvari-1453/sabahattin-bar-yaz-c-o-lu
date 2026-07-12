package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie

data class MediaItem(
    val uri: Uri,
    val name: String,
    val type: String // "image", "audio", "video"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(tts: android.speech.tts.TextToSpeech?, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var activeTab by remember { mutableStateOf(0) } // 0: Kimlik, 1: Dosyalar, 2: Sosyal Medya
    
    // Permission State
    var hasPermission by remember { mutableStateOf(hasStoragePermission(context)) }
    
    // Device Media State
    var deviceMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    
    // Social Accounts Connection State
    var connectedWhatsapp by remember { mutableStateOf<String?>(null) }
    var connectedInstagram by remember { mutableStateOf<String?>(null) }
    var connectedFacebook by remember { mutableStateOf<String?>(null) }
    var connectedYoutube by remember { mutableStateOf<String?>(null) }

    // Bottom sheets / Dialog for connection
    var showConnectDialog by remember { mutableStateOf<String?>(null) } // "whatsapp", "instagram", "facebook", "youtube"
    var tempUsernameInput by remember { mutableStateOf("") }

    // Update media when permission is active
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            deviceMedia = queryDeviceMedia(context)
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val isGranted = permissions.values.any { it }
            hasPermission = isGranted
            if (isGranted) {
                deviceMedia = queryDeviceMedia(context)
                Toast.makeText(context, "İzin verildi! Dosyalarınız başarıyla yüklendi.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Erişim izni verilmedi.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Manual content selector as fallback / addition
    val mediaSelectorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                val newMedia = uris.map { uri ->
                    val name = getFileName(context, uri)
                    val type = when {
                        name.endsWith(".mp3", true) -> "audio"
                        name.endsWith(".mp4", true) || name.endsWith(".avi", true) -> "video"
                        else -> "image"
                    }
                    MediaItem(uri, name, type)
                }
                deviceMedia = (newMedia + deviceMedia).distinctBy { it.uri }
                Toast.makeText(context, "${uris.size} adet medya başarıyla listeye eklendi!", Toast.LENGTH_SHORT).show()
            }
        }
    )

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
                            "Entegrasyon Paneli", 
                            fontWeight = FontWeight.Bold, 
                            color = Color.White,
                            fontSize = 16.sp
                        ) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (ScreenReaderState.isActive) {
                            ScreenReaderState.stop(tts)
                        } else {
                            readScreenContext(context, tts, "profile")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = "Ekranı Oku",
                            tint = if (ScreenReaderState.isActive) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1F1B24),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121214) // Rich dark surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1F1B24), Color(0xFF121214))
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tab Row for Navigation
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.White.copy(alpha = 0.05f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 16.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Kimlik", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Kimlik") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Dosya & Medya", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Medya") }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Sosyal Ağlar", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Share, contentDescription = "Sosyal") }
                    )
                }

                // Dynamic content based on Tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeTab) {
                        0 -> ProfileTabContent(
                            connectedWhatsapp = connectedWhatsapp,
                            connectedInstagram = connectedInstagram,
                            connectedFacebook = connectedFacebook,
                            connectedYoutube = connectedYoutube
                        )
                        1 -> FileAndMediaTabContent(
                            context = context,
                            hasPermission = hasPermission,
                            deviceMedia = deviceMedia,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_MEDIA_IMAGES,
                                            Manifest.permission.READ_MEDIA_VIDEO,
                                            Manifest.permission.READ_MEDIA_AUDIO
                                        )
                                    )
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    )
                                }
                            },
                            onSelectFiles = {
                                mediaSelectorLauncher.launch("*/*")
                            }
                        )
                        2 -> SocialNetworksTabContent(
                            context = context,
                            connectedWhatsapp = connectedWhatsapp,
                            connectedInstagram = connectedInstagram,
                            connectedFacebook = connectedFacebook,
                            connectedYoutube = connectedYoutube,
                            onConnectClick = { platform ->
                                tempUsernameInput = ""
                                showConnectDialog = platform
                            },
                            onDisconnectClick = { platform ->
                                when (platform) {
                                    "whatsapp" -> connectedWhatsapp = null
                                    "instagram" -> connectedInstagram = null
                                    "facebook" -> connectedFacebook = null
                                    "youtube" -> connectedYoutube = null
                                }
                                Toast.makeText(context, "Bağlantı kesildi.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Connection Dialog
            if (showConnectDialog != null) {
                AlertDialog(
                    onDismissRequest = { showConnectDialog = null },
                    confirmButton = {
                        Button(
                            onClick = {
                                val input = tempUsernameInput.trim()
                                if (input.isNotEmpty()) {
                                    val formatted = if (input.startsWith("@")) input else "@$input"
                                    when (showConnectDialog) {
                                        "whatsapp" -> connectedWhatsapp = formatted
                                        "instagram" -> connectedInstagram = formatted
                                        "facebook" -> connectedFacebook = formatted
                                        "youtube" -> connectedYoutube = formatted
                                    }
                                    showConnectDialog = null
                                    Toast.makeText(context, "Hesap başarıyla bağlandı!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Lütfen geçerli bir kullanıcı adı girin.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Bağlantıyı Tamamla")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConnectDialog = null }) {
                            Text("Vazgeç", color = Color.Gray)
                        }
                    },
                    title = {
                        Text(
                            text = when (showConnectDialog) {
                                "whatsapp" -> "WhatsApp Hesabını Bağla"
                                "instagram" -> "Instagram Hesabını Bağla"
                                "facebook" -> "Facebook Hesabını Bağla"
                                "youtube" -> "YouTube Kanalını Bağla"
                                else -> ""
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "GUNDİ Bro ile hesap entegrasyonu sağlamak için profil isminizi veya numaranızı girin. Bu sayede paylaşımlarınızı doğrudan yapabilirsiniz.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = tempUsernameInput,
                                onValueChange = { tempUsernameInput = it },
                                label = { Text("Kullanıcı Adı / Telefon No") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileTabContent(
    connectedWhatsapp: String?,
    connectedInstagram: String?,
    connectedFacebook: String?,
    connectedYoutube: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Avatar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80",
                        contentDescription = "Profil Resmi",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                            .border(3.dp, Color(0xFF1F1B24), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Kullanıcı Profili", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("GUNDİ Bro Dijital Asistan Sürümü", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Interactive counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItem("Aktif", "Durum")
                    ProfileStatItem("Sınırsız", "Yapay Zeka")
                    ProfileStatItem("Güvenli", "Gizlilik")
                }
            }
        }

        // Active Connections Checklist
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Bağlı Entegrasyonlar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ConnectionStatusRow("WhatsApp", connectedWhatsapp)
                ConnectionStatusRow("Instagram", connectedInstagram)
                ConnectionStatusRow("Facebook", connectedFacebook)
                ConnectionStatusRow("YouTube", connectedYoutube)
            }
        }
    }
}

@Composable
fun ProfileStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ConnectionStatusRow(label: String, connectedHandle: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (connectedHandle != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (connectedHandle != null) Color(0xFF22C55E) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.LightGray, fontSize = 14.sp)
        }
        Text(
            text = connectedHandle ?: "Bağlı Değil",
            color = if (connectedHandle != null) MaterialTheme.colorScheme.primary else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (connectedHandle != null) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun FileAndMediaTabContent(
    context: Context,
    hasPermission: Boolean,
    deviceMedia: List<MediaItem>,
    onRequestPermission: () -> Unit,
    onSelectFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermission) Color(0xFF1E3A1E).copy(alpha = 0.5f) else Color(0xFF3E1E1E).copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.FolderSpecial else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (hasPermission) Color(0xFF4ADE80) else Color(0xFFF87171),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (hasPermission) "Dosya ve Medya Erişimi Aktif" else "Erişim İzni Bekleniyor",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (hasPermission) "Cihazınızdaki fotoğraflar, sesler ve videolar taranabiliyor." else "Medyaları analiz etmek ve oynatmak için izin verin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }
                if (!hasPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("İzin Ver", fontSize = 12.sp)
                    }
                }
            }
        }

        // Örnek Medyalar Bölümü
        Text("GUNDİ Bro Örnek Medya Dosyaları (MP3 / MP4)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaPlayerManager.samples.forEach { sample ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { MediaPlayerManager.playTrack(context, sample) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (sample.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(sample.title, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Oynat", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cihaz Medyalarım & Belgelerim", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            IconButton(onClick = onSelectFiles) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Medya Ekle", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (deviceMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { onSelectFiles() }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Gösterilecek dosya veya medya yok.",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Cihazınızdan medya seçmek için buraya tıklayın ya da yukarıdaki örnek hazır dosyaları oynatın.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deviceMedia) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                if (item.type == "video") {
                                    MediaPlayerManager.play(context, item.uri, item.name, true)
                                } else if (item.type == "audio") {
                                    MediaPlayerManager.play(context, item.uri, item.name, false)
                                } else {
                                    Toast.makeText(context, "${item.name} seçildi.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        if (item.type == "image") {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = "Görüntü",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (item.type == "video") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = "Video", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Oynat", tint = Color.White)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF2A2438)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
                                    Icon(Icons.Default.MusicNote, contentDescription = "Ses", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.name, color = Color.White, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialNetworksTabContent(
    context: Context,
    connectedWhatsapp: String?,
    connectedInstagram: String?,
    connectedFacebook: String?,
    connectedYoutube: String?,
    onConnectClick: (String) -> Unit,
    onDisconnectClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SocialNetworkCard(
                platformName = "WhatsApp",
                color = Color(0xFF25D366),
                appPackage = "com.whatsapp",
                connectedHandle = connectedWhatsapp,
                icon = Icons.Default.Call,
                context = context,
                onConnect = { onConnectClick("whatsapp") },
                onDisconnect = { onDisconnectClick("whatsapp") },
                onAction = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "GUNDİ Bro harika bir yapay zeka! Benimle sohbet ediyor ve dosyaları analiz ediyor. Buradan dene: https://ais-pre-es7wuwjior536rpzprdzvo-867764121261.europe-west2.run.app")
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(Intent.createChooser(shareIntent, "GUNDİ Bro Paylaşım"))
                    } catch (e: Exception) {
                        // Fallback to web link if app not installed
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=GUNDI%20Bro%20harika%20bir%20yapay%20zeka!%20Sen%20de%20dene:%20https://ais-pre-es7wuwjior536rpzprdzvo-867764121261.europe-west2.run.app"))
                        context.startActivity(webIntent)
                    }
                },
                actionLabel = "Sohbeti WhatsApp'ta Paylaş"
            )
        }

        item {
            SocialNetworkCard(
                platformName = "Instagram",
                color = Color(0xFFE1306C),
                appPackage = "com.instagram.android",
                connectedHandle = connectedInstagram,
                icon = Icons.Default.CameraAlt,
                context = context,
                onConnect = { onConnectClick("instagram") },
                onDisconnect = { onDisconnectClick("instagram") },
                onAction = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com"))
                    context.startActivity(intent)
                },
                actionLabel = "Instagram Akışına Git"
            )
        }

        item {
            SocialNetworkCard(
                platformName = "Facebook",
                color = Color(0xFF1877F2),
                appPackage = "com.facebook.katana",
                connectedHandle = connectedFacebook,
                icon = Icons.Default.Public,
                context = context,
                onConnect = { onConnectClick("facebook") },
                onDisconnect = { onDisconnectClick("facebook") },
                onAction = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "GUNDİ Bro yapay zeka asistanı çok eğlenceli ve pratik!")
                        setPackage("com.facebook.katana")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com"))
                        context.startActivity(webIntent)
                    }
                },
                actionLabel = "Facebook'ta Paylaş"
            )
        }

        item {
            SocialNetworkCard(
                platformName = "YouTube",
                color = Color(0xFFFF0000),
                appPackage = "com.google.android.youtube",
                connectedHandle = connectedYoutube,
                icon = Icons.Default.PlayArrow,
                context = context,
                onConnect = { onConnectClick("youtube") },
                onDisconnect = { onDisconnectClick("youtube") },
                onAction = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=GUNDI+Bro"))
                    context.startActivity(intent)
                },
                actionLabel = "YouTube'da GUNDİ Bro'yu Ara"
            )
        }
    }
}

@Composable
fun SocialNetworkCard(
    platformName: String,
    color: Color,
    appPackage: String,
    connectedHandle: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    context: Context,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onAction: () -> Unit,
    actionLabel: String
) {
    val isAppInstalled = remember(appPackage) { checkAppInstalled(context, appPackage) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(platformName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isAppInstalled) Color(0xFF22C55E) else Color(0xFFF59E0B), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAppInstalled) "Resmi Uygulama Yüklü" else "Tarayıcı Entegrasyonu",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // Account Connect / Connected Status badge or action
                if (connectedHandle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.1f))
                            .clickable { onDisconnect() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(connectedHandle, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                } else {
                    OutlinedButton(
                        onClick = onConnect,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Hesabı Bağla", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            // Sharing / Action Button
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.85f), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Helper to query actual system media (Images, Audio, Video) from device
private fun queryDeviceMedia(context: Context): List<MediaItem> {
    val list = mutableListOf<MediaItem>()
    
    // Query Video (MP4, AVI, vb.)
    try {
        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME)
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            var count = 0
            while (cursor.moveToNext() && count < 6) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                list.add(MediaItem(uri, name, "video"))
                count++
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Query Audio (MP3)
    try {
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            var count = 0
            while (cursor.moveToNext() && count < 6) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                list.add(MediaItem(uri, name, "audio"))
                count++
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Query Images
    try {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            var count = 0
            while (cursor.moveToNext() && count < 6) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol)
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                list.add(MediaItem(uri, name, "image"))
                count++
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return list
}

// Check if an application package is installed on the user's Android device
private fun checkAppInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
}

private fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
