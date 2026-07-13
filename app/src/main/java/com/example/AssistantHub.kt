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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    
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
                        "☁️ Cloudflare" to 5
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
                        5 -> CloudflareDnsTab()
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
    var reminderLabel by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableStateOf(1) }
    
    val minutePresets = listOf(1, 3, 5, 10, 15, 30)

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
                            val res = callGeminiTranslate(inputText, sourceLang, targetLang)
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

private suspend fun callGeminiTranslate(text: String, source: String, target: String): String? = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
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
                            val res = callGeminiSummarize(inputText)
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

private suspend fun callGeminiSummarize(text: String): String? = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
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

// ==================== TAB 5: CLOUDFLARE DNS MANAGEMENT ====================
@Composable
fun CloudflareDnsTab() {
    val coroutineScope = rememberCoroutineScope()
    val token = BuildConfig.CLOUDFLARE_TOKEN
    val zoneId = BuildConfig.CLOUDFLARE_ZONE_ID
    val accountId = BuildConfig.CLOUDFLARE_ACCOUNT_ID

    var records by remember { mutableStateOf<List<CloudflareRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var actionLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    // Form inputs
    var newType by remember { mutableStateOf("A") }
    var newName by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var newProxied by remember { mutableStateOf(true) }
    var newTtl by remember { mutableStateOf(1) } // 1 is Automatic TTL in Cloudflare

    val recordTypes = listOf("A", "AAAA", "CNAME", "TXT", "MX")

    fun loadRecords() {
        if (token.isBlank() || zoneId.isBlank() || token.startsWith("MY_") || token.startsWith("CLOUDFLARE_")) {
            errorMsg = "Lütfen önce .env dosyasına geçerli Cloudflare kimlik bilgilerinizi girin!"
            return
        }
        isLoading = true
        errorMsg = null
        coroutineScope.launch {
            val fetched = CloudflareManager.fetchDnsRecords(token, zoneId)
            records = fetched
            isLoading = false
            if (fetched.isEmpty()) {
                errorMsg = "DNS kayıtları alınamadı veya liste boş. Lütfen API token ve Zone ID'nizi doğrulayın."
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRecords()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Domain Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Aktif Alan Adı: gundi.com 👑",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Cloudflare Entegrasyonu",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { loadRecords() },
                        modifier = Modifier.testTag("refresh_dns_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.08f))

                // Account / Zone Details
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bölge Kimliği (Zone ID)", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (zoneId.length > 8) zoneId.take(8) + "..." + zoneId.takeLast(6) else zoneId,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hesap Kimliği (Account ID)", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (accountId.length > 8) accountId.take(8) + "..." + accountId.takeLast(6) else accountId,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Apply Reisin Şablonu (Predefined Records Quick Action)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B24)),
            border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Barış Abimin DNS Şablonu 👑",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gözlerin yollarda kalmasın! gundi.com için A, CNAME, MX ve TXT kayıtlarını tek bir tıkla otomatik olarak Cloudflare üzerinde yapılandırabilirsin.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        actionLoading = true
                        successMsg = null
                        errorMsg = null
                        coroutineScope.launch {
                            // Define Baris Abi's predefined records
                            val templateRecords = listOf(
                                Triple("A", "gundi.com", "216.168.96.50"),
                                Triple("A", "mail.gundi.com", "216.168.96.50"),
                                Triple("A", "webmail.gundi.com", "216.168.96.50"),
                                Triple("CNAME", "ftp.gundi.com", "gundi.com"),
                                Triple("CNAME", "www.gundi.com", "gundi.com"),
                                Triple("MX", "gundi.com", "10 mail.gundi.com"),
                                Triple("TXT", "gundi.com", "v=spf1 +a +mx -all")
                            )

                            var createdCount = 0
                            for (rec in templateRecords) {
                                // Check if already exists to prevent duplicate
                                val exists = records.any { it.type == rec.first && it.name.contains(rec.second) }
                                if (!exists) {
                                    val success = CloudflareManager.createDnsRecord(
                                        token = token,
                                        zoneId = zoneId,
                                        type = rec.first,
                                        name = rec.second,
                                        content = rec.third,
                                        ttl = 1,
                                        proxied = rec.first == "A" || rec.first == "CNAME"
                                    )
                                    if (success) createdCount++
                                }
                            }
                            actionLoading = false
                            if (createdCount > 0) {
                                successMsg = "$createdCount adet şablon DNS kaydı başarıyla oluşturuldu reisim!"
                                loadRecords()
                            } else {
                                successMsg = "Şablon kayıtlar zaten mevcut veya eklenemedi!"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_template_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !actionLoading && !isLoading
                ) {
                    if (actionLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Şablonu Otomatik Kur (Gözüm Kapalı) 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Message Banners
        if (errorMsg != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.3f))
            ) {
                Text(
                    text = errorMsg!!,
                    color = Color(0xFFEF5350),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (successMsg != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C).copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color(0xFF388E3C).copy(alpha = 0.3f))
            ) {
                Text(
                    text = successMsg!!,
                    color = Color(0xFF81C784),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Add DNS Record Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Yeni DNS Kaydı Oluştur",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Type selector row
                Text("Kayıt Türü", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recordTypes.forEach { type ->
                        val isSelected = newType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                .clickable { newType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name & Content fields
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("İsim (örn: www veya @)", color = Color.Gray) },
                    placeholder = { Text("gundi.com", color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newContent,
                    onValueChange = { newContent = it },
                    label = { Text("İçerik (örn: IP adresi veya Hedef)", color = Color.Gray) },
                    placeholder = { Text("216.168.96.50", color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Proxied and TTL row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Proxied switch (A / CNAME only)
                    if (newType == "A" || newType == "CNAME") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = newProxied,
                                onCheckedChange = { newProxied = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Column {
                                Text("Vekil Sunucu (Proxied)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                Text("Cloudflare koruması aktif", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // TTL Indicator
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TTL", fontSize = 11.sp, color = Color.Gray)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Otomatik (Auto)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (newName.isBlank() || newContent.isBlank()) {
                            errorMsg = "Lütfen isim ve içerik alanlarını doldurun reisim!"
                            return@Button
                        }
                        actionLoading = true
                        errorMsg = null
                        successMsg = null
                        coroutineScope.launch {
                            val success = CloudflareManager.createDnsRecord(
                                token = token,
                                zoneId = zoneId,
                                type = newType,
                                name = newName,
                                content = newContent,
                                ttl = newTtl,
                                proxied = newProxied
                            )
                            actionLoading = false
                            if (success) {
                                successMsg = "DNS kaydı başarıyla eklendi!"
                                newName = ""
                                newContent = ""
                                loadRecords()
                            } else {
                                errorMsg = "DNS kaydı oluşturulamadı. Cloudflare API hata döndürdü."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_dns_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !actionLoading && !isLoading
                ) {
                    if (actionLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DNS Kaydını Kaydet 💾", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Records List Title
        Text(
            text = "Kayıtlı DNS Kayıtları (${records.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(44.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hiç aktif kayıt bulunamadı.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            // LazyColumn replacement inside scrollable tab with Column + forEach to prevent nested scroll crashes
            records.forEach { record ->
                val recordColor = when (record.type) {
                    "A" -> Color(0xFF81C784)
                    "CNAME" -> Color(0xFF64B5F6)
                    "TXT" -> Color(0xFFBA68C8)
                    "MX" -> Color(0xFFFFD54F)
                    else -> Color.White
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
                        // Badge
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(recordColor.copy(alpha = 0.12f))
                                .border(1.2.dp, recordColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = record.type,
                                fontSize = 11.sp,
                                color = recordColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = record.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                // Proxied badge ☁️
                                if (record.proxied) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFF57C00).copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cloud,
                                                contentDescription = "Proxied",
                                                tint = Color(0xFFFFB74D),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                "Proxied",
                                                fontSize = 8.sp,
                                                color = Color(0xFFFFB74D),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "DNS Only",
                                            fontSize = 8.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = record.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    // Delete Record Action
                    IconButton(
                        onClick = {
                            actionLoading = true
                            errorMsg = null
                            successMsg = null
                            coroutineScope.launch {
                                val success = CloudflareManager.deleteDnsRecord(token, zoneId, record.id)
                                actionLoading = false
                                if (success) {
                                    successMsg = "DNS kaydı başarıyla silindi reisim!"
                                    loadRecords()
                                } else {
                                    errorMsg = "Kayıt silinemedi. Cloudflare API hata döndürdü."
                                }
                            }
                        },
                        modifier = Modifier
                            .testTag("delete_record_${record.id}")
                            .size(36.dp),
                        enabled = !actionLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = Color.Gray.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

