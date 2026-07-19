package com.example

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import android.widget.Toast
import com.example.database.ChatDatabase
import com.example.database.ChatRepository
import com.example.database.ChatMessageEntity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send

object AssistantHubState {
    var showAssistantHub by mutableStateOf(false)
}

// Data Classes
data class AssistantTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val priority: String = "Orta", // Düşük, Orta, Yüksek
    val category: String = "Genel" // İş, Kişisel, Alışveriş, Diğer
)

data class AssistantReminder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val totalSeconds: Int,
    var secondsLeft: Int,
    var isRunning: Boolean = false,
    var isFinished: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantHubDialog(
    onDismiss: () -> Unit,
    onGoToSession: ((String) -> Unit)? = null,
    onSendImageToChat: ((Bitmap, String) -> Unit)? = null,
    tts: android.speech.tts.TextToSpeech? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    
    val chatDb = remember { ChatDatabase.getDatabase(context) }
    val chatRepo = remember { ChatRepository(chatDb.chatDao(), context) }
    
    // Persistent SharedPreferences for Tasks
    val prefs = remember { context.getSharedPreferences("assistant_hub_prefs", Context.MODE_PRIVATE) }
    
    // State management for Tasks
    var taskList by remember {
        mutableStateOf(loadTasksFromPrefs(prefs))
    }
    
    // State management for Reminders
    var reminderList by remember {
        mutableStateOf(mutableStateListOf<AssistantReminder>())
    }

    // Active Countdown Job for Reminders
    LaunchedEffect(reminderList.size) {
        while (true) {
            delay(1000)
            var changed = false
            for (i in reminderList.indices) {
                val r = reminderList[i]
                if (r.isRunning && r.secondsLeft > 0) {
                    r.secondsLeft -= 1
                    if (r.secondsLeft == 0) {
                        r.isRunning = false
                        r.isFinished = true
                        NotificationHelper.sendNotification(
                            context = context,
                            title = "Gundi Bro Hatırlatıcı! ⏰",
                            message = "Süre bitti reisim: ${r.title}!"
                        )
                    }
                    changed = true
                }
            }
            if (changed) {
                // Trigger state recomposition
                val temp = reminderList.toList()
                reminderList.clear()
                reminderList.addAll(temp)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF16131C),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assistant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GUNDİ BRO",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFFFD54F),
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Asistan Paneli",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    val tabs = listOf(
                        "📝 Görevler" to 0,
                        "⏰ Hatırlatıcılar" to 1,
                        "🌐 Çevirmen" to 2,
                        "📄 Özetleyici" to 3,
                        "☀️ Hava Durumu" to 4,
                        "💾 Soru Geçmişi" to 5,
                        "🎨 Görsel Üretici" to 6,
                        "📂 Dosya Analizörü" to 7,
                        "🎵 Arapça Yorumcu" to 8,
                        "📊 SoundCloud" to 9
                    )
                    tabs.forEach { (title, index) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Gray
                                ) 
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))

                // Content View based on selected tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> TaskPlannerTab(
                            taskList = taskList,
                            onTasksChanged = { updatedList ->
                                taskList = updatedList
                                saveTasksToPrefs(prefs, updatedList)
                            }
                        )
                        1 -> RemindersTab(reminderList = reminderList)
                        2 -> TranslatorTab()
                        3 -> SummarizerTab()
                        4 -> WeatherAssistantTab()
                        5 -> QaHistoryTab(chatRepo = chatRepo, onGoToSession = onGoToSession)
                        6 -> ImagenGeneratorTab(onSendToChat = onSendImageToChat)
                        7 -> FileAnalyzerTab(tts = tts)
                        8 -> ArabicLyricsAnalyzerTab(tts = tts)
                        9 -> SoundCloudDashboardTab()
                    }
                }
            }
        }
    }
}

// ==================== TAB 0: TASK PLANNER ====================
@Composable
fun TaskPlannerTab(
    taskList: List<AssistantTask>,
    onTasksChanged: (List<AssistantTask>) -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Orta") }
    var selectedCategory by remember { mutableStateOf("Genel") }
    
    val priorities = listOf("Düşük", "Orta", "Yüksek")
    val categories = listOf("Genel", "İş", "Kişisel", "Alışveriş")

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick input block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Yeni Görev Ekle",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("Yapılacak bir iş yaz...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                val newTask = AssistantTask(
                                    title = newTaskTitle,
                                    priority = selectedPriority,
                                    category = selectedCategory
                                )
                                onTasksChanged(taskList + newTask)
                                newTaskTitle = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ekle", tint = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Priority & Category selectors row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Priority chip selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Önem Derecesi", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            priorities.forEach { p ->
                                val isSelected = selectedPriority == p
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        .clickable { selectedPriority = p }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        p,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Category chip selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kategori", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        .clickable { selectedCategory = cat }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tasks list view
        Text(
            text = "Aktif Görevlerin (${taskList.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (taskList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Task, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Henüz bir görev eklemedin.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(taskList) { task ->
                    val priorityColor = when (task.priority) {
                        "Yüksek" -> Color(0xFFE57373)
                        "Orta" -> Color(0xFFFFD54F)
                        else -> Color(0xFF81C784)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { checked ->
                                    val updated = taskList.map {
                                        if (it.id == task.id) it.copy(isCompleted = checked) else it
                                    }
                                    onTasksChanged(updated)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (task.isCompleted) Color.Gray else Color.White,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(priorityColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            task.priority,
                                            fontSize = 9.sp,
                                            color = priorityColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            task.category,
                                            fontSize = 9.sp,
                                            color = Color.LightGray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                onTasksChanged(taskList.filter { it.id != task.id })
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Gray.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// Prefs persistence helpers
private fun loadTasksFromPrefs(prefs: android.content.SharedPreferences): List<AssistantTask> {
    val jsonStr = prefs.getString("tasks_list_json", "[]") ?: "[]"
    val list = mutableListOf<AssistantTask>()
    try {
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                AssistantTask(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    isCompleted = obj.getBoolean("isCompleted"),
                    priority = obj.getString("priority"),
                    category = obj.getString("category")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun saveTasksToPrefs(prefs: android.content.SharedPreferences, list: List<AssistantTask>) {
    try {
        val arr = JSONArray()
        for (task in list) {
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("title", task.title)
            obj.put("isCompleted", task.isCompleted)
            obj.put("priority", task.priority)
            obj.put("category", task.category)
            arr.put(obj)
        }
        prefs.edit().putString("tasks_list_json", arr.toString()).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


// ==================== TAB 1: REMINDERS ====================
@Composable
fun RemindersTab(
    reminderList: MutableList<AssistantReminder>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var reminderLabel by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableStateOf(1) }
    
    val minutePresets = listOf(1, 3, 5, 10, 15, 30)

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationHelper.sendNotification(
                context,
                "Gundi Bro'dan Bildirim Aktif! 🔔",
                "Harika reisim! Artık hatırlatıcılar bittiğinde sana anında bildirim göndereceğim."
            )
        } else {
            android.widget.Toast.makeText(context, "Bildirim izni verilmedi reisim!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Yeni Akıllı Hatırlatıcı",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = reminderLabel,
                        onValueChange = { reminderLabel = it },
                        placeholder = { Text("Ne hatırlatayım? (örn: Çay saati)", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (reminderLabel.isNotBlank()) {
                                val totalSecs = selectedMinutes * 60
                                reminderList.add(
                                    AssistantReminder(
                                        title = reminderLabel,
                                        totalSeconds = totalSecs,
                                        secondsLeft = totalSecs,
                                        isRunning = true
                                    )
                                )
                                reminderLabel = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.AlarmAdd, contentDescription = "Ekle", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Süre Seç (Dakika)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    minutePresets.forEach { mins ->
                        val isSelected = selectedMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                .clickable { selectedMinutes = mins }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$mins dk",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (!hasPermission) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationHelper.sendNotification(
                                    context,
                                    "Gundi Bro Bildirim Testi 🔔",
                                    "Yallah reisim! Bildirim sistemimiz canavar gibi çalışıyor!"
                                )
                            }
                        } else {
                            NotificationHelper.sendNotification(
                                context,
                                "Gundi Bro Bildirim Testi 🔔",
                                "Yallah reisim! Bildirim sistemimiz canavar gibi çalışıyor!"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.NotificationsActive,
                        contentDescription = "Test Notification",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Anlık Bildirim Test Et 🔔", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Geri Sayım & Hatırlatıcılar",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (reminderList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Henüz aktif bir geri sayım yok.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminderList) { reminder ->
                    val min = reminder.secondsLeft / 60
                    val sec = reminder.secondsLeft % 60
                    val progress = if (reminder.totalSeconds > 0) reminder.secondsLeft.toFloat() / reminder.totalSeconds else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(
                                1.dp, 
                                if (reminder.isFinished) Color(0xFFE57373) else Color.White.copy(alpha = 0.05f), 
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (reminder.isFinished) Icons.Default.NotificationsActive else Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (reminder.isFinished) Color(0xFFE57373) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    reminder.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (reminder.isFinished) Color(0xFFE57373) else Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = if (reminder.isFinished) Color(0xFFE57373) else MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (reminder.isFinished) "SÜRE BİTTİ!" else String.format("%02d:%02d", min, sec),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (reminder.isFinished) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                            )
                            
                            IconButton(
                                onClick = {
                                    reminderList.remove(reminder)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Kaldır", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==================== TAB 2: TRANSLATOR ====================
@Composable
fun TranslatorTab() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var sourceLang by remember { mutableStateOf("Türkçe") }
    var targetLang by remember { mutableStateOf("İngilizce") }
    var translating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val languages = listOf("Türkçe", "İngilizce", "Almanca", "Fransızca", "İspanyolca", "Japonca", "Rusça", "Arapça")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Dil Çevirici",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Source text field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Çevrilecek metni yazın...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Languages Row selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kaynak Dil", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    // Cycles through
                                    val idx = languages.indexOf(sourceLang)
                                    sourceLang = languages[(idx + 1) % languages.size]
                                }
                                .padding(10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(sourceLang, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Icon(Icons.Default.SwapHoriz, contentDescription = "Değiş", tint = Color.Gray)

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hedef Dil", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    // Cycles through
                                    val idx = languages.indexOf(targetLang)
                                    targetLang = languages[(idx + 1) % languages.size]
                                }
                                .padding(10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(targetLang, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (inputText.isBlank()) {
                            errorMsg = "Lütfen önce çevrilecek metni girin."
                            return@Button
                        }
                        errorMsg = null
                        translating = true
                        coroutineScope.launch {
                            val res = callGeminiTranslate(inputText, sourceLang, targetLang, context)
                            translating = false
                            if (res != null) {
                                translatedText = res
                            } else {
                                errorMsg = "Çeviri yapılamadı. API anahtarını kontrol edin."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !translating
                ) {
                    if (translating) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Çevriliyor...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yapay Zeka ile Çevir", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (translatedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Çeviri Sonucu:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = translatedText,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

private suspend fun callGeminiTranslate(text: String, source: String, target: String, context: android.content.Context): String? = withContext(Dispatchers.IO) {
    val apiKey = SettingsManager.getInstance(context).getActiveApiKey()
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
        return@withContext null
    }

    val prompt = """
        Metni yüksek doğruluk ve akıcılıkla çevir. Sadece çevrilen metni döndür. Hiçbir açıklama, not veya ekleme yazma.
        
        Metin: "$text"
        Kaynak Dil: $source
        Hedef Dil: $target
    """.trimIndent()

    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt))))
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


// ==================== TAB 3: SUMMARIZER ====================
@Composable
fun SummarizerTab() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var summaryResult by remember { mutableStateOf("") }
    var summarizing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Akıllı Metin Özetleyici",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Özetlenecek uzun makale, haber veya metni buraya yapıştırın...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (inputText.length < 20) {
                            errorMsg = "Lütfen özetlenecek en az 20 karakterlik bir metin girin."
                            return@Button
                        }
                        errorMsg = null
                        summarizing = true
                        coroutineScope.launch {
                            val res = callGeminiSummarize(inputText, context)
                            summarizing = false
                            if (res != null) {
                                summaryResult = res
                            } else {
                                errorMsg = "Özet çıkarılamadı. API anahtarını kontrol edin."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !summarizing
                ) {
                    if (summarizing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Özet Çıkarılıyor...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Summarize, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yapay Zeka ile Özet Çıkar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (summaryResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📄 Yapay Zeka Özet Raporu",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = summaryResult,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

private suspend fun callGeminiSummarize(text: String, context: android.content.Context): String? = withContext(Dispatchers.IO) {
    val apiKey = SettingsManager.getInstance(context).getActiveApiKey()
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
        return@withContext null
    }

    val prompt = """
        Aşağıdaki uzun metni Türkçe dilinde mükemmel şekilde özetle.
        Lütfen şu şekilde bir rapor formatı sun:
        
        📌 **Tek Cümlelik Ana Fikir**: (Metnin en kritik ana mesajı)
        
        🔍 **Önemli Noktalar & Detaylar (Bullet Points)**:
        - Maddeler halinde en önemli 3-5 gelişmeyi/bilgiyi listele.
        
        💡 **Asistan Değerlendirmesi**: (Gundi Bro'nun bu metne yönelik dostça, yapıcı 1-2 cümlelik pratik yorumu)
        
        Metin:
        "$text"
    """.trimIndent()

    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt))))
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


// ==================== TAB 4: WEATHER ASSISTANT ====================
data class WeatherInfo(
    val city: String,
    val temp: Int,
    val status: String,
    val emoji: String,
    val humidity: String,
    val wind: String
)

@Composable
fun WeatherAssistantTab() {
    var searchCity by remember { mutableStateOf("") }
    
    val defaultWeather = listOf(
        WeatherInfo("İstanbul", 28, "Güneşli", "☀️", "%42", "14 km/s"),
        WeatherInfo("Ankara", 24, "Parçalı Bulutlu", "⛅", "%35", "11 km/s"),
        WeatherInfo("İzmir", 32, "Sıcak & Açık", "🔥", "%30", "18 km/s"),
        WeatherInfo("Londra", 18, "Hafif Yağmurlu", "🌧️", "%85", "22 km/s"),
        WeatherInfo("New York", 22, "Rüzgarlı", "💨", "%50", "28 km/s")
    )

    var weatherList by remember { mutableStateOf(defaultWeather) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Hava Durumu Asistanı",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchCity,
                        onValueChange = { searchCity = it },
                        placeholder = { Text("Şehir ismi girin...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (searchCity.isNotBlank()) {
                                val generated = WeatherInfo(
                                    city = searchCity.trim().capitalize(),
                                    temp = (15..38).random(),
                                    status = listOf("Açık", "Bulutlu", "Sisli", "Yağmurlu", "Güneşli").random(),
                                    emoji = listOf("☀️", "⛅", "🌧️", "☁️", "⛈️").random(),
                                    humidity = "%" + (20..90).random(),
                                    wind = (5..35).random().toString() + " km/s"
                                )
                                weatherList = listOf(generated) + weatherList
                                searchCity = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Ara", tint = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Hava Durumu Raporları",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(weatherList) { weather ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(weather.emoji, fontSize = 34.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(weather.city, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(weather.status, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${weather.temp}°C", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Nem: ${weather.humidity}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("• Rüzgar: ${weather.wind}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }

                        IconButton(
                            onClick = {
                                weatherList = weatherList.filter { it.city != weather.city }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==================== TAB 5: QA HISTORY ====================

data class QuestionAnswerPair(
    val id: String,
    val answerId: String,
    val question: String,
    val answer: String,
    val timestamp: Long,
    val sessionId: String
)

@Composable
fun QaHistoryTab(
    chatRepo: ChatRepository,
    onGoToSession: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    val allMessages by chatRepo.allMessagesFlow.collectAsState(initial = emptyList())
    
    val qaPairs = remember(allMessages) {
        groupMessagesToQaPairs(allMessages)
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var expandedItemIds by remember { mutableStateOf(emptySet<String>()) }
    
    val filteredPairs = remember(qaPairs, searchQuery) {
        if (searchQuery.isBlank()) {
            qaPairs
        } else {
            qaPairs.filter {
                it.question.contains(searchQuery, ignoreCase = true) ||
                it.answer.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Soru & Cevap Deposu",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (filteredPairs.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    filteredPairs.forEach { pair ->
                                        chatRepo.deleteMessageById(pair.id)
                                        if (pair.answerId.isNotBlank()) {
                                            chatRepo.deleteMessageById(pair.answerId)
                                        }
                                    }
                                    Toast.makeText(context, "Tüm soru geçmişi temizlendi! 🧹", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tümünü Sil", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Sorularda veya cevaplarda ara...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Ara", tint = Color.Gray)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle", tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        if (filteredPairs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Aramanızla eşleşen soru-cevap bulunamadı." else "Henüz kayıtlı soru-cevap geçmişi bulunmuyor dostum!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPairs, key = { it.id }) { qa ->
                    val isExpanded = expandedItemIds.contains(qa.id)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedItemIds = if (isExpanded) {
                                    expandedItemIds - qa.id
                                } else {
                                    expandedItemIds + qa.id
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                        border = BorderStroke(1.dp, if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatQaTimestamp(qa.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (onGoToSession != null) {
                                        IconButton(
                                            onClick = { onGoToSession(qa.sessionId) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Sohbete Git",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                chatRepo.deleteMessageById(qa.id)
                                                if (qa.answerId.isNotBlank()) {
                                                    chatRepo.deleteMessageById(qa.answerId)
                                                }
                                                Toast.makeText(context, "Soru-cevap silindi.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = Color.Gray.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = qa.question,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🤖 GUNDİ Bro:",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(qa.answer))
                                            Toast.makeText(context, "Cevap panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Kopyala",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text(
                                    text = qa.answer,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Cevabı göster...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun groupMessagesToQaPairs(messages: List<com.example.database.ChatMessageEntity>): List<QuestionAnswerPair> {
    val pairs = mutableListOf<QuestionAnswerPair>()
    val groupedBySession = messages.groupBy { it.sessionId }
    
    for ((sessionId, sessionMsgs) in groupedBySession) {
        val sortedMsgs = sessionMsgs.sortedBy { it.timestamp }
        var i = 0
        while (i < sortedMsgs.size) {
            val current = sortedMsgs[i]
            if (current.isUser) {
                var answerText = "Cevap yükleniyor veya bulunamadı..."
                var answerMsgId = ""
                if (i + 1 < sortedMsgs.size) {
                    val next = sortedMsgs[i + 1]
                    if (!next.isUser) {
                        answerText = next.text
                        answerMsgId = next.id
                        i += 2
                    } else {
                        i += 1
                    }
                } else {
                    i += 1
                }
                
                pairs.add(
                    QuestionAnswerPair(
                        id = current.id,
                        answerId = answerMsgId,
                        question = current.text,
                        answer = answerText,
                        timestamp = current.timestamp,
                        sessionId = sessionId
                    )
                )
            } else {
                i += 1
            }
        }
    }
    return pairs.sortedByDescending { it.timestamp }
}

fun formatQaTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("tr", "TR"))
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun ImagenGeneratorTab(
    onSendToChat: ((Bitmap, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var promptText by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }
    
    var isGenerating by remember { mutableStateOf(false) }
    var generatedImage by remember { mutableStateOf<Bitmap?>(null) }
    
    // Funny loading messages list
    val loadingMessages = remember {
        listOf(
            "GUNDİ Bro fırını ısıtıyor, boyaları karıştırıyor kanka... 🎨🔥",
            "Piksel piksel sanat eseri dokuyorum, bekle azıcık şef! ⚡🧠",
            "İşlemcim şu an senin hayalini çiziyor, efsane bir şey geliyor! 🌌🚀",
            "Tuval hazırlandı, fırça darbeleri vuruluyor... Az sabret başkan! 🖌️💎",
            "Gündi badin sana kurban olsun, resim fırından taze taze çıkıyor... 🍕✨"
        )
    }
    var currentLoadingMessageIndex by remember { mutableStateOf(0) }
    
    // Periodically change the loading message when generating
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            currentLoadingMessageIndex = 0
            while (isGenerating) {
                delay(3000)
                currentLoadingMessageIndex = (currentLoadingMessageIndex + 1) % loadingMessages.size
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Yapay Zeka Görsel Sihirbazı",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hayalindeki görseli kelimelere dök, GUNDİ Bro senin için anında çizsin!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("Örn: Astronot kedi uzayda çay içiyor, neon siberpunk tarzı...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "En-Boy Oranı (Aspect Ratio)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val aspectRatios = listOf("1:1", "16:9", "4:3", "9:16")
                    aspectRatios.forEach { ratio ->
                        val isSelected = selectedAspectRatio == ratio
                        val containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)
                        val textColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(containerColor)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .clickable { selectedAspectRatio = ratio },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratio,
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (promptText.isBlank()) {
                            Toast.makeText(context, "Lütfen bir hayal/prompt yaz kanka!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isGenerating = true
                        coroutineScope.launch {
                            try {
                                val apiKey = SettingsManager.getInstance(context).getActiveApiKey()
                                val isPlaceholder = apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY"
                                
                                if (isPlaceholder) {
                                    Toast.makeText(context, "API Anahtarı bulunamadı! Secrets panelini kontrol et.", Toast.LENGTH_LONG).show()
                                    isGenerating = false
                                    return@launch
                                }
                                
                                var imageBmp: Bitmap? = null
                                try {
                                    val imagenRequest = ImagenRequest(
                                        prompt = promptText,
                                        numberOfImages = 1,
                                        aspectRatio = selectedAspectRatio,
                                        outputMimeType = "image/jpeg"
                                    )
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.service.generateImagen3(apiKey, imagenRequest)
                                    }
                                    val imageBytesBase64 = response.generatedImages?.firstOrNull()?.image?.imageBytes
                                    if (imageBytesBase64 != null) {
                                        val bytes = Base64.decode(imageBytesBase64, Base64.DEFAULT)
                                        imageBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                    
                                    val request = GenerateContentRequest(
                                        contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                                        generationConfig = GenerationConfig(
                                            imageConfig = ImageConfig(aspectRatio = selectedAspectRatio, imageSize = "1K"),
                                            responseModalities = listOf("TEXT", "IMAGE")
                                        )
                                    )
                                    
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.service.generateImage(apiKey, request)
                                    }
                                    
                                    val responseParts = response.candidates?.firstOrNull()?.content?.parts
                                    if (responseParts != null) {
                                        for (part in responseParts) {
                                            if (part.inlineData != null) {
                                                val base64Data = part.inlineData.data
                                                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                                imageBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                break
                                            }
                                        }
                                    }
                                }
                                
                                if (imageBmp != null) {
                                    generatedImage = imageBmp
                                } else {
                                    Toast.makeText(context, "Görsel verisi alınamadı. Başka bir prompt dene kanka.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                val msgText = e.message ?: ""
                                val isAuthOrForbidden = (e is retrofit2.HttpException && (e.code() == 401 || e.code() == 403)) ||
                                        msgText.contains("401", ignoreCase = true) || msgText.contains("403", ignoreCase = true) ||
                                        msgText.contains("Unauthorized", ignoreCase = true) || msgText.contains("Forbidden", ignoreCase = true)
                                val finalMsg = if (isAuthOrForbidden) {
                                    "Hata (401/403): Gemini API Key geçersiz veya yetkisiz! Lütfen Secrets panelinden anahtarınızı güncelleyin."
                                } else {
                                    "Hata oluştu: ${e.message}"
                                }
                                Toast.makeText(context, finalMsg, Toast.LENGTH_LONG).show()
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isGenerating) "Çiziliyor..." else "Görseli Oluştur! ✨", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isGenerating) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loadingMessages[currentLoadingMessageIndex],
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            generatedImage?.let { bmp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Oluşturulan Başyapıt 👑",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    when (selectedAspectRatio) {
                                        "16:9" -> 16f / 9f
                                        "4:3" -> 4f / 3f
                                        "9:16" -> 9f / 16f
                                        else -> 1f
                                    }
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Oluşturulan Görsel",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Save to Gallery button
                            OutlinedButton(
                                onClick = {
                                    val promptClean = promptText.take(15).replace("[^a-zA-Z0-9]".toRegex(), "_")
                                    val filename = "gundi_${promptClean}_${System.currentTimeMillis()}"
                                    val uri = saveImageToStorage(context, bmp, filename)
                                    if (uri != null) {
                                        Toast.makeText(context, "Görsel Galeriye/Pictures klasörüne kaydedildi! 💾🎉", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Kaydetme başarısız oldu.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kaydet", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            
                            // Send to Chat button
                            if (onSendToChat != null) {
                                Button(
                                    onClick = {
                                        onSendToChat(bmp, "Oluşturduğun görsel: \"$promptText\"")
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sohbete Aktar", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun saveImageToStorage(context: android.content.Context, bitmap: Bitmap, name: String): android.net.Uri? {
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Gundi")
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (imageUri != null) {
        try {
            resolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            return imageUri
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileAnalyzerTab(tts: android.speech.tts.TextToSpeech? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileMime by remember { mutableStateOf("") }
    var selectedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var customPrompt by remember { mutableStateOf("Bu dosyanın ses/görüntü içeriğini analiz et, en önemli noktaları özetle ve bana Gundi Bro tarzı esprili bir değerlendirme raporu sun reisim!") }
    
    var analysisResult by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    var isTtsSpeaking by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // File picker launcher
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val mime = context.contentResolver.getType(it) ?: "application/octet-stream"
            val name = getFileNameHelper(context, it)
            val ext = name.substringAfterLast('.', "").lowercase()
            
            // Check file eligibility (MP3, WAV, MP4 or matching MIME types)
            val isAllowed = ext in listOf("mp3", "wav", "mp4") || 
                            mime.startsWith("audio/") || 
                            mime == "video/mp4"
                            
            if (!isAllowed) {
                errorMsg = "Lütfen sadece MP3, WAV veya MP4 formatında bir dosya seç reisim! 🎧🎬"
                return@rememberLauncherForActivityResult
            }

            try {
                val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                if (bytes != null) {
                    // Check file size (e.g. limit to 15MB to prevent memory issues)
                    if (bytes.size > 15 * 1024 * 1024) {
                        errorMsg = "Dosya boyutu çok büyük reisim! Maksimum 15MB yükleyebilirsin."
                        return@rememberLauncherForActivityResult
                    }
                    selectedFileUri = it
                    selectedFileName = name
                    selectedFileMime = mime
                    selectedFileBytes = bytes
                    errorMsg = null
                } else {
                    errorMsg = "Dosya okunamadı reisim!"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMsg = "Dosya yüklenirken hata oluştu: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gundi Bro Medya Analizörü 🎧🎬",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MP3, WAV ses dosyalarını veya MP4 videolarını buraya yükleyip analiz ettirebilirsin. Gundi Bro dosyanın sesini/görüntüsünü inceleyip sana özel esprili bir analiz hazırlasın!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                // File Upload Box / Dashboard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                        .border(
                            BorderStroke(
                                1.5.dp, 
                                Brush.sweepGradient(listOf(Color(0xFFFFD54F), Color(0xFF4285F4)))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { filePickerLauncher.launch("*/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedFileUri == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Bir Dosya Seç (MP3, WAV, MP4)",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Maksimum 15MB • Tıkla ve Yükle",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        // Display information of selected file
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isVideo = selectedFileMime.startsWith("video/") || selectedFileName.lowercase().endsWith(".mp4")
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isVideo) Color(0xFF7C4DFF).copy(alpha = 0.2f) else Color(0xFF00E676).copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (isVideo) Color(0xFFB388FF) else Color(0xFF69F0AE),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedFileName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${selectedFileMime.uppercase()} • ${(selectedFileBytes?.size ?: 0) / 1024} KB",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedFileUri = null
                                    selectedFileName = ""
                                    selectedFileMime = ""
                                    selectedFileBytes = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Temizle",
                                    tint = Color.LightGray
                                )
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedFileUri != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Gundi Bro'ya Talimat Ver (İsteğe Bağlı):",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD54F),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val bytes = selectedFileBytes
                            if (bytes == null) {
                                errorMsg = "Dosya verisi bulunamadı!"
                                return@Button
                            }
                            analyzing = true
                            errorMsg = null
                            coroutineScope.launch {
                                val result = callGeminiFileAnalysis(
                                    fileBytes = bytes,
                                    mimeType = selectedFileMime,
                                    fileName = selectedFileName,
                                    customPrompt = customPrompt,
                                    context = context
                                )
                                analyzing = false
                                if (result != null) {
                                    analysisResult = result
                                } else {
                                    errorMsg = "Bağlantı hatası oluştu reisim! Tekrar dene."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !analyzing
                    ) {
                        if (analyzing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gundi Bro Dosyayı İnceliyor...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gundi Tarzıyla Analiz Et 🔥", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (analysisResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 Gündi Bro Analiz Raporu",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            // Copy button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(analysisResult))
                                    Toast.makeText(context, "Analiz panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // TTS Speak button
                            if (tts != null) {
                                IconButton(
                                    onClick = {
                                        if (isTtsSpeaking) {
                                            tts.stop()
                                            isTtsSpeaking = false
                                        } else {
                                            isTtsSpeaking = true
                                            val cleanText = cleanTextForTts(analysisResult)
                                            tts.speak(
                                                cleanText,
                                                android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "FILE_ANALYSIS_TTS"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isTtsSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                        contentDescription = "Sesli Oku",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = analysisResult,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// Private helper inside AssistantHub to avoid conflict or name duplication
private fun getFileNameHelper(context: android.content.Context, uri: android.net.Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
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
    return result ?: "file"
}

private suspend fun callGeminiFileAnalysis(
    fileBytes: ByteArray,
    mimeType: String,
    fileName: String,
    customPrompt: String,
    context: android.content.Context
): String? = withContext(Dispatchers.IO) {
    val apiKey = SettingsManager.getInstance(context).getActiveApiKey()
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
        return@withContext "Gundi hatası: API anahtarı ayarlanmamış reisim! Ayarlardan API anahtarını girip tekrar dene."
    }

    // Prepare system instruction or custom prompt
    val basePrompt = """
        Sen GUNDI Bro'sun, DJ BYMIX'in kankası ve en yakın dostusun.
        Sana gönderilen bu medya dosyasını (${fileName}) baştan sona incele ve analiz et.
        Dosya tipi: ${mimeType}.
        
        Kullanıcı isteği: "${customPrompt}"
        
        Lütfen cevabını şu formatta, samimi, esprili, yer yer "REİSİM", "BRO" diyerek sun:
        
        🎵 **Gundi Bro Medya Değerlendirmesi**:
        - Dosyanın içeriği hakkında ne gördüğünü/duyduğunu özetle.
        - İlgi çekici, eğlenceli veya sanatsal açıdan dikkat çeken kısımları belirt.
        
        ⚡ **Gundi Bro'nun Çılgın Tavsiyesi**:
        - DJ BYMIX tarzında bu dosya ile ne yapabileceğine dair komik ve yaratıcı bir tavsiye ver.
        
        Her zaman Türkçe konuş ve eğlenceli, enerjik ol!
    """.trimIndent()

    val fileBase64 = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
    val request = GenerateContentRequest(
        contents = listOf(
            Content(
                parts = listOf(
                    Part(inlineData = InlineData(mimeType = mimeType, data = fileBase64)),
                    Part(text = basePrompt)
                )
            )
        )
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    } catch (e: Exception) {
        e.printStackTrace()
        "Analiz sırasında bir hata oluştu reisim! Hata detayı: ${e.message}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArabicLyricsAnalyzerTab(tts: android.speech.tts.TextToSpeech? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var lyricsInput by remember { mutableStateOf("") }
    var interpretationResult by remember { mutableStateOf("") }
    var analyzing by remember { mutableStateOf(false) }
    var selectedTone by remember { mutableStateOf("Damar / Duygusal Mod 🥺") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isTtsSpeaking by remember { mutableStateOf(false) }

    val tones = listOf(
        "Damar / Duygusal Mod 🥺",
        "Coşkulu / Halay Modu 🔥",
        "Edebi / Filozof Gundi 😎"
    )

    val sampleLyrics = listOf(
        Triple(
            "Habibi Ya Nour El Ain",
            "Amr Diab",
            "Habibi ya nour el ain\nYa sakin khayali\nAsheq bakali senin\nW'la gheyrak fi bali"
        ),
        Triple(
            "Tamally Ma'ak",
            "Amr Diab",
            "Tamally ma'ak\nWe low hata b'eed 'anny, f'alby hawak\nTamally ma'ak\nTamally fi baly we fe Alby, wala bansak"
        ),
        Triple(
            "Sidi Mansour",
            "Saber Rebai",
            "Sidi Mansour ya baba\nWe nalehek ya baba\nAl-ashraf ya baba\nSidi Mansour ya baba"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Arapça Şarkı Sözü Analizi ve Yorumu 🕌🎵",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Arapça şarkı sözlerini veya parçasını buraya yapıştır reisim! Gundi Bro sözleri senin için Türkçeye çevirsin, içindeki derin felsefeyi ve duyguları kendi tarzında esprili ve samimi bir şekilde yorumlasın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Input Field
                OutlinedTextField(
                    value = lyricsInput,
                    onValueChange = { lyricsInput = it },
                    placeholder = { 
                        Text(
                            "Yallah reisim, yorumlanacak Arapça şarkı sözlerini buraya yapıştır veya bir parça ismi yaz...", 
                            color = Color.Gray,
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD54F),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tone selection chips
                Text(
                    "Gundi Yorumlama Tonu:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tones.forEach { tone ->
                        val isSelected = selectedTone == tone
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.05f))
                                .clickable { selectedTone = tone }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tone.substringBefore(" "),
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Examples
                Text(
                    "Popüler Arapça Şarkı Örnekleri (Tıkla ve Yapıştır):",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleLyrics.forEach { sample ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .clickable {
                                    lyricsInput = sample.third
                                    Toast.makeText(context, "${sample.first} seçildi reisim! 😉", Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = sample.first,
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = sample.second,
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                // Analyze Button
                Button(
                    onClick = {
                        if (lyricsInput.isBlank()) {
                            errorMsg = "Lütfen önce Arapça şarkı sözlerini gir reisim!"
                            return@Button
                        }
                        errorMsg = null
                        analyzing = true
                        coroutineScope.launch {
                            val result = callGeminiArabicLyricsAnalysis(lyricsInput, selectedTone, context)
                            analyzing = false
                            if (result != null) {
                                interpretationResult = result
                            } else {
                                errorMsg = "Bağlantı hatası oluştu reisim! Tekrar dene."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !analyzing
                ) {
                    if (analyzing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gundi Bro Derin Düşüncelerde...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gundi Tarzıyla Yorumla 🔥", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (interpretationResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👳🏽‍♂️ Gündi Bro'nun Şarkı Yorumu",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            // Copy button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(interpretationResult))
                                    Toast.makeText(context, "Yorum panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // TTS Speak button
                            if (tts != null) {
                                IconButton(
                                    onClick = {
                                        if (isTtsSpeaking) {
                                            tts.stop()
                                            isTtsSpeaking = false
                                        } else {
                                            isTtsSpeaking = true
                                            val cleanText = cleanTextForTts(interpretationResult)
                                            tts.speak(
                                                cleanText,
                                                android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "ARABIC_LYRICS_TTS"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isTtsSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                        contentDescription = "Sesli Oku",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = interpretationResult,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

private suspend fun callGeminiArabicLyricsAnalysis(lyrics: String, tone: String, context: android.content.Context): String? = withContext(Dispatchers.IO) {
    val apiKey = SettingsManager.getInstance(context).getActiveApiKey()
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
        return@withContext "Gundi hatası: API anahtarı ayarlanmamış reisim! Ayarlardan API anahtarını girip tekrar dene."
    }

    val prompt = """
        Sen GUNDI Bro'sun, DJ BYMIX'in kankası ve en samimi dostusun. 
        Bir kullanıcı sana Arapça şarkı sözleri (veya şarkı bilgisi) gönderdi.
        
        Sözler:
        "$lyrics"
        
        İstenen Gundi Bro Yorumlama Modu: "$tone"
        
        Lütfen bu Arapça şarkı sözlerini Türkçeye son derece samimi ve doğru bir şekilde tercüme et, ardından seçilen moda ("$tone") uygun olarak Gundi Bro'nun o meşhur, komik, esprili, sıcak üslubuyla derin bir analiz ve felsefi değerlendirme sun reisim!
        
        Cevabını şu düzende, eğlenceli ve sıcak bir tonda hazırla:
        
        🔮 **Gundi Bro Tercüme Köşesi**:
        - Şarkı sözlerinin Türkçe çevirisi ve ana duygusu.
        
        💬 **Gundi Bro'nun Derin Felsefesi (${tone})**:
        - Şarkının hissettirdiklerini kendi kelimelerinle, "REİSİM", "BRO", "BARIŞ ABİM" gibi hitapları kullanarak esprili bir dille yorumla.
        
        💃 **Gundi Bro ile DJ BYMIX DJ Önerisi**:
        - Bu şarkıya uygun çılgın bir DJ mix veya halay/damar remix fikri öner.
        
        Her zaman Türkçe konuş, cana yakın ve enerjik ol!
    """.trimIndent()

    val request = GenerateContentRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt))))
    )

    try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    } catch (e: Exception) {
        e.printStackTrace()
        "Hata oluştu reisim! Hata detayı: ${e.message}"
    }
}

