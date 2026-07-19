package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

// SoundCloud Color constants
val SoundCloudOrange = Color(0xFFFF5500)
val SoundCloudDarkBg = Color(0xFF141218)
val SoundCloudCardBg = Color(0xFF211E26)
val SoundCloudAccentGreen = Color(0xFF4CAF50)

data class DailyStat(val day: Int, val listens: Int, val date: String)

@Composable
fun SoundCloudDashboardTab() {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    var chartType by remember { mutableStateOf(0) } // 0 = Line Chart, 1 = Bar Chart
    var selectedPeriod by remember { mutableStateOf(0) } // 0 = 30 Days, 1 = 7 Days

    // 30 Days of realistic, mathematically precise data points summing up to exactly 19,225
    val dailyStats = remember {
        listOf(
            DailyStat(1, 420, "17 Haz"), DailyStat(2, 440, "18 Haz"), DailyStat(3, 410, "19 Haz"),
            DailyStat(4, 430, "20 Haz"), DailyStat(5, 460, "21 Haz"), DailyStat(6, 490, "22 Haz"),
            DailyStat(7, 520, "23 Haz"), DailyStat(8, 500, "24 Haz"), DailyStat(9, 540, "25 Haz"),
            DailyStat(10, 560, "26 Haz"), DailyStat(11, 530, "27 Haz"), DailyStat(12, 570, "28 Haz"),
            DailyStat(13, 600, "29 Haz"), DailyStat(14, 620, "30 Haz"), DailyStat(15, 595, "1 Tem"),
            DailyStat(16, 635, "2 Tem"), DailyStat(17, 655, "3 Tem"), DailyStat(18, 675, "4 Tem"),
            DailyStat(19, 695, "5 Tem"), DailyStat(20, 725, "6 Tem"), DailyStat(21, 745, "7 Tem"),
            DailyStat(22, 710, "8 Tem"), DailyStat(23, 740, "9 Tem"), DailyStat(24, 770, "10 Tem"),
            DailyStat(25, 800, "11 Tem"), DailyStat(26, 830, "12 Tem"), DailyStat(27, 860, "13 Tem"),
            DailyStat(28, 880, "14 Tem"), DailyStat(29, 910, "15 Tem"), DailyStat(30, 910, "16 Tem")
        )
    }

    // Weekly aggregated data for the bar chart
    val weeklyStats = remember {
        listOf(
            DailyStat(1, 3170, "1. Hafta"),
            DailyStat(2, 3920, "2. Hafta"),
            DailyStat(3, 4725, "3. Hafta"),
            DailyStat(4, 5590, "4. Hafta"),
            DailyStat(5, 1820, "Son 2 Gün")
        )
    }

    val totalListens = dailyStats.sumOf { it.listens } // Exact 19,225

    // Animation trigger on tab load
    var animationProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        ) { value, _ ->
            animationProgress = value
        }
    }

    // Refresh simulation
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1500)
            isRefreshing = false
            Toast.makeText(context, "Veriler güncellendi! 🎵", Toast.LENGTH_SHORT).show()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SoundCloudDarkBg)
    ) {
        val isWideScreen = maxWidth > 600.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card with Refresh
            SoundCloudHeaderCard(
                totalListens = totalListens,
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true }
            )

            // Dynamic layout based on screen width
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats grid
                    Box(modifier = Modifier.weight(0.4f)) {
                        SoundCloudQuickStatsGrid()
                    }
                    // Interactive chart
                    Box(modifier = Modifier.weight(0.6f)) {
                        SoundCloudChartCard(
                            dailyStats = dailyStats,
                            weeklyStats = weeklyStats,
                            chartType = chartType,
                            setChartType = { chartType = it },
                            selectedPeriod = selectedPeriod,
                            setSelectedPeriod = { selectedPeriod = it },
                            animationProgress = animationProgress
                        )
                    }
                }
            } else {
                // Quick Stats grid for mobile
                SoundCloudQuickStatsGrid()

                // Interactive Chart for mobile
                SoundCloudChartCard(
                    dailyStats = dailyStats,
                    weeklyStats = weeklyStats,
                    chartType = chartType,
                    setChartType = { chartType = it },
                    selectedPeriod = selectedPeriod,
                    setSelectedPeriod = { selectedPeriod = it },
                    animationProgress = animationProgress
                )
            }

            // Milestone and Goals Tracker
            SoundCloudMilestoneTracker(totalListens = totalListens)

            // Top Performing Tracks List
            SoundCloudTopTracksCard()
        }
    }
}

@Composable
fun SoundCloudHeaderCard(
    totalListens: Int,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("soundcloud_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SoundCloudCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Highlight gradient edge
                    val brush = Brush.verticalGradient(
                        colors = listOf(SoundCloudOrange.copy(alpha = 0.15f), Color.Transparent),
                        startY = 0f,
                        endY = size.height
                    )
                    drawRect(brush = brush)
                }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SoundCloudOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "SoundCloud Logo Icon",
                            tint = SoundCloudOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "SOUNDCLOUD ANALİZLERİ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SoundCloudOrange,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Aylık Dinleyici Performansı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = { if (!isRefreshing) onRefresh() },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .testTag("refresh_stats_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Verileri Yenile",
                        tint = Color.White,
                        modifier = Modifier.graphicsLayer(rotationZ = if (isRefreshing) rotationAngle else 0f)
                    )
                }
            }
        }
    }
}

@Composable
fun SoundCloudQuickStatsGrid() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Toplam Dinlenme",
                value = "19.225",
                change = "+12,4%",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconColor = SoundCloudOrange,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Etkin Dinleyiciler",
                value = "1.420",
                change = "+8.2%",
                icon = Icons.Default.People,
                iconColor = Color(0xFF00E5FF),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Beğeniler",
                value = "4.892",
                change = "+15.1%",
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFFF4081),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Takipçiler",
                value = "3.840",
                change = "+235",
                icon = Icons.Default.PersonAdd,
                iconColor = SoundCloudAccentGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    change: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoundCloudCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Trend Up",
                        tint = SoundCloudAccentGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = change,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SoundCloudAccentGreen
                    )
                    Text(
                        text = "bu ay",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun SoundCloudChartCard(
    dailyStats: List<DailyStat>,
    weeklyStats: List<DailyStat>,
    chartType: Int,
    setChartType: (Int) -> Unit,
    selectedPeriod: Int,
    setSelectedPeriod: (Int) -> Unit,
    animationProgress: Float
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Filter stats depending on the selected period (all 30 days vs last 7 days)
    val displayStats = if (selectedPeriod == 1) {
        dailyStats.takeLast(7)
    } else {
        dailyStats
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("soundcloud_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SoundCloudCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Chart Header Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Büyüme Trendi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "30 Günlük Dinlenme Dağılımı",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chart Type Toggle Buttons (Line vs Bar)
                    IconButton(
                        onClick = {
                            setChartType(0)
                            selectedPointIndex = null
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (chartType == 0) SoundCloudOrange.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Çizgi Grafiği",
                            tint = if (chartType == 0) SoundCloudOrange else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            setChartType(1)
                            selectedPointIndex = null
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (chartType == 1) SoundCloudOrange.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Sütun Grafiği",
                            tint = if (chartType == 1) SoundCloudOrange else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Period Selector
            TabRow(
                selectedTabIndex = selectedPeriod,
                containerColor = Color.White.copy(alpha = 0.03f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp)),
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedPeriod])
                            .fillMaxHeight()
                            .padding(2.dp)
                            .background(SoundCloudOrange.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedPeriod == 0,
                    onClick = {
                        setSelectedPeriod(0)
                        selectedPointIndex = null
                    },
                    text = { Text("30 Gün", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                )
                Tab(
                    selected = selectedPeriod == 1,
                    onClick = {
                        setSelectedPeriod(1)
                        selectedPointIndex = null
                    },
                    text = { Text("7 Gün", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                )
            }

            // High Fidelity Custom Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                if (chartType == 0) {
                    // Custom interactive Line Chart
                    SoundCloudLineChart(
                        points = displayStats,
                        animationProgress = animationProgress,
                        selectedIndex = selectedPointIndex,
                        onPointSelected = { selectedPointIndex = it }
                    )
                } else {
                    // Custom interactive Bar Chart
                    SoundCloudBarChart(
                        points = if (selectedPeriod == 0) weeklyStats else displayStats,
                        animationProgress = animationProgress,
                        selectedIndex = selectedPointIndex,
                        onPointSelected = { selectedPointIndex = it }
                    )
                }
            }

            // Selected Point Details Info / Tooltip Indicator
            AnimatedVisibility(
                visible = selectedPointIndex != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val currentList = if (chartType == 1 && selectedPeriod == 0) weeklyStats else displayStats
                selectedPointIndex?.let { idx ->
                    if (idx in currentList.indices) {
                        val point = currentList[idx]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(SoundCloudOrange, CircleShape)
                                    )
                                    Text(
                                        text = "${point.date}:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "${point.listens} Dinlenme",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "Gizlemek için tıklayın",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    modifier = Modifier.clickable { selectedPointIndex = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundCloudLineChart(
    points: List<DailyStat>,
    animationProgress: Float,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit
) {
    val maxVal = (points.maxOfOrNull { it.listens } ?: 1000) * 1.1f
    val minVal = (points.minOfOrNull { it.listens } ?: 0) * 0.9f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val width = size.width
                    val stepX = width / (points.size - 1)
                    val clickedIdx = (offset.x / stepX).coerceIn(0f, (points.size - 1).toFloat())
                    val roundedIdx = Math.round(clickedIdx)
                    onPointSelected(roundedIdx)
                }
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val width = size.width
                        val stepX = width / (points.size - 1)
                        val clickedIdx = (offset.x / stepX).coerceIn(0f, (points.size - 1).toFloat())
                        onPointSelected(Math.round(clickedIdx))
                    },
                    onDrag = { change, _ ->
                        val width = size.width
                        val stepX = width / (points.size - 1)
                        val clickedIdx = (change.position.x / stepX).coerceIn(0f, (points.size - 1).toFloat())
                        onPointSelected(Math.round(clickedIdx))
                    },
                    onDragEnd = { }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Draw Grid lines and background guidelines
        val gridLines = 4
        val stepY = height / gridLines
        for (i in 0..gridLines) {
            val y = i * stepY
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (points.isEmpty()) return@Canvas

        val stepX = width / (points.size - 1)
        val valueRange = maxVal - minVal

        // Construct path coordinates
        val coordinates = points.mapIndexed { idx, point ->
            val x = idx * stepX
            val ratio = if (valueRange > 0) (point.listens - minVal) / valueRange else 0.5f
            val y = height - (ratio * height * animationProgress)
            Offset(x, y)
        }

        // Draw Area Fill underneath the line with gradient
        if (coordinates.isNotEmpty() && animationProgress > 0.01f) {
            val fillPath = Path().apply {
                moveTo(coordinates.first().x, height)
                coordinates.forEach { offset ->
                    lineTo(offset.x, offset.y)
                }
                lineTo(coordinates.last().x, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(SoundCloudOrange.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )
        }

        // Draw Line Chart Path (Smooth curve)
        if (coordinates.size > 1) {
            val linePath = Path().apply {
                moveTo(coordinates.first().x, coordinates.first().y)
                for (i in 1 until coordinates.size) {
                    val prev = coordinates[i - 1]
                    val curr = coordinates[i]
                    // Smooth cubic bezier control points
                    val controlX1 = prev.x + (curr.x - prev.x) / 2
                    val controlY1 = prev.y
                    val controlX2 = prev.x + (curr.x - prev.x) / 2
                    val controlY2 = curr.y

                    cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                }
            }
            drawPath(
                path = linePath,
                color = SoundCloudOrange,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Draw selection highlight vertical line & dot
        selectedIndex?.let { idx ->
            if (idx in coordinates.indices) {
                val selectedCoord = coordinates[idx]

                // Draw vertical indicator line
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(selectedCoord.x, 0f),
                    end = Offset(selectedCoord.x, height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Outer halo ring
                drawCircle(
                    color = SoundCloudOrange.copy(alpha = 0.4f),
                    radius = 12.dp.toPx(),
                    center = selectedCoord
                )

                // Inner pulsing core dot
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = selectedCoord
                )

                drawCircle(
                    color = SoundCloudOrange,
                    radius = 4.dp.toPx(),
                    center = selectedCoord
                )
            }
        }
    }
}

@Composable
fun SoundCloudBarChart(
    points: List<DailyStat>,
    animationProgress: Float,
    selectedIndex: Int?,
    onPointSelected: (Int?) -> Unit
) {
    val maxVal = (points.maxOfOrNull { it.listens } ?: 1000) * 1.1f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val width = size.width
                    val barWidthWithSpacing = width / points.size
                    val clickedIdx = (offset.x / barWidthWithSpacing).toInt().coerceIn(0, points.size - 1)
                    onPointSelected(clickedIdx)
                }
            }
    ) {
        val width = size.width
        val height = size.height

        val barCount = points.size
        val barWidthWithSpacing = width / barCount
        val barWidth = barWidthWithSpacing * 0.6f
        val spacing = barWidthWithSpacing * 0.4f

        // Draw horizontal grid lines
        val gridLines = 4
        val stepY = height / gridLines
        for (i in 0..gridLines) {
            val y = i * stepY
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        points.forEachIndexed { idx, point ->
            val ratio = if (maxVal > 0) point.listens / maxVal else 0.5f
            val barHeight = ratio * height * animationProgress
            val x = (idx * barWidthWithSpacing) + (spacing / 2)
            val y = height - barHeight

            val isSelected = selectedIndex == idx
            val barColor = if (isSelected) Color.White else SoundCloudOrange
            val alphaMultiplier = if (isSelected) 1f else 0.85f

            // Draw rounded bar with gradient
            val barPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = x,
                            top = y,
                            right = x + barWidth,
                            bottom = height
                        ),
                        topLeft = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        topRight = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        bottomLeft = CornerRadius.Zero,
                        bottomRight = CornerRadius.Zero
                    )
                )
            }

            drawPath(
                path = barPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        barColor.copy(alpha = alphaMultiplier),
                        SoundCloudOrange.copy(alpha = 0.3f)
                    ),
                    startY = y,
                    endY = height
                )
            )

            // Draw a subtle border if selected
            if (isSelected) {
                drawPath(
                    path = barPath,
                    color = SoundCloudOrange,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun SoundCloudMilestoneTracker(totalListens: Int) {
    val targetListens = 25000
    val progress = (totalListens.toFloat() / targetListens).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("soundcloud_milestone_tracker"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoundCloudCardBg)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SoundCloudOrange.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Milestone Trophy",
                            tint = SoundCloudOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Gelecek Kilometre Taşı",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Yaratıcı Doğrulama Rozeti Hedefi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    text = "${(progress * 100).toInt()}% Tamamlandı",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoundCloudOrange
                )
            }

            // Beautiful Gradient Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(SoundCloudOrange, Color(0xFFFF8800))
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mevcut: 19.225 / 25.000",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
                Text(
                    text = "Son 5.775 Dinlenme Kaldı!",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SoundCloudOrange
                )
            }
        }
    }
}

@Composable
fun SoundCloudTopTracksCard() {
    val topTracks = remember {
        listOf(
            Triple("Summer Vibes (Original Mix)", "8.420 Dinlenme", "+14% Bu Ay"),
            Triple("Midnight Blue (Lofi Chill)", "5.110 Dinlenme", "+23% Bu Ay"),
            Triple("Bassline Drop (Edm Remix)", "3.212 Dinlenme", "+5% Bu Ay"),
            Triple("Sunset Boulevard", "2.483 Dinlenme", "+18% Bu Ay")
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("soundcloud_top_tracks_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoundCloudCardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "En Çok Dinlenenler",
                        tint = SoundCloudOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "En Popüler Parçalar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Tümünü Gör",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoundCloudOrange,
                    modifier = Modifier.clickable { }
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                topTracks.forEachIndexed { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SoundCloudOrange.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SoundCloudOrange
                                )
                            }

                            Column {
                                Text(
                                    text = track.first,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.second,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(SoundCloudAccentGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = track.third,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoundCloudAccentGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
