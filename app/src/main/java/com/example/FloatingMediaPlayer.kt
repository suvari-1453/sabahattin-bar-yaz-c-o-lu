package com.example

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Media Item definition for the player
data class MediaTrack(
    val title: String,
    val url: String,
    val isVideo: Boolean
)

object MediaPlayerManager {
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0)
    var duration by mutableStateOf(0)
    var mediaName by mutableStateOf("")
    var isVideo by mutableStateOf(false)
    var currentUri by mutableStateOf<Uri?>(null)
    var showPlayer by mutableStateOf(false)
    var isCollapsed by mutableStateOf(false)

    private var mediaPlayer: MediaPlayer? = null
    private var videoViewRef: VideoView? = null

    // Preconfigured sample media
    val samples = listOf(
        MediaTrack("GUNDİ Bro - Dinlendirici Melodi (MP3)", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", false),
        MediaTrack("GUNDİ Bro - Doğa Belgeseli (MP4)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", true),
        MediaTrack("GUNDİ Bro - Uzay Serüveni (MP4/AVI)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", true)
    )

    fun play(context: Context, uri: Uri, name: String, video: Boolean) {
        stop()
        mediaName = name
        currentUri = uri
        isVideo = video
        showPlayer = true
        isCollapsed = false

        if (!video) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, uri)
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                        this@MediaPlayerManager.duration = duration
                        this@MediaPlayerManager.isPlaying = true
                    }
                    setOnCompletionListener {
                        this@MediaPlayerManager.isPlaying = false
                        this@MediaPlayerManager.currentPosition = 0
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playTrack(context: Context, track: MediaTrack) {
        play(context, Uri.parse(track.url), track.title, track.isVideo)
    }

    fun playAttachedFile(context: Context, file: AttachedFile) {
        val isVideoFile = file.mimeType.contains("video", true) || 
                          file.name.endsWith(".mp4", true) || 
                          file.name.endsWith(".avi", true)
        try {
            val tempFile = java.io.File(context.cacheDir, file.name)
            tempFile.writeBytes(file.data)
            val uri = Uri.fromFile(tempFile)
            play(context, uri, file.name, isVideoFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun registerVideoView(videoView: VideoView) {
        videoViewRef = videoView
        isPlaying = videoView.isPlaying
    }

    fun togglePlayPause() {
        if (isVideo) {
            videoViewRef?.let {
                if (it.isPlaying) {
                    it.pause()
                    isPlaying = false
                } else {
                    it.start()
                    isPlaying = true
                }
            }
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    isPlaying = false
                } else {
                    it.start()
                    isPlaying = true
                }
            }
        }
    }

    fun seekTo(position: Int) {
        if (isVideo) {
            videoViewRef?.seekTo(position)
        } else {
            mediaPlayer?.seekTo(position)
        }
        currentPosition = position
    }

    fun updateProgress() {
        if (isVideo) {
            videoViewRef?.let {
                if (it.isPlaying) {
                    currentPosition = it.currentPosition
                    duration = it.duration
                }
            }
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    currentPosition = it.currentPosition
                }
            }
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        videoViewRef?.stopPlayback()
        videoViewRef = null
        isPlaying = false
        currentPosition = 0
        duration = 0
    }

    fun close() {
        stop()
        showPlayer = false
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FloatingMediaPlayer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!MediaPlayerManager.showPlayer) return

    // Position State for Draggability
    var offsetX by remember { mutableStateOf(20f) }
    var offsetY by remember { mutableStateOf(100f) }

    // Rotate animation for collapsed vinyl disk
    var diskRotation by remember { mutableStateOf(0f) }
    LaunchedEffect(MediaPlayerManager.isPlaying) {
        while (MediaPlayerManager.isPlaying) {
            diskRotation = (diskRotation + 3f) % 360f
            delay(16)
        }
    }

    // Ticker to update audio/video progress slider
    LaunchedEffect(MediaPlayerManager.isPlaying) {
        while (true) {
            MediaPlayerManager.updateProgress()
            delay(500)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            AnimatedContent(
                targetState = MediaPlayerManager.isCollapsed,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "PlayerAnimation"
            ) { collapsed ->
                if (collapsed) {
                    // 1. Collapsed Bubble / Vinyl Disc Mode
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF1E1B24), Color(0xFF121214))
                                )
                            )
                            .clickable { MediaPlayerManager.isCollapsed = false }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner disk graphic
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Black)
                                .rotate(diskRotation),
                            contentAlignment = Alignment.Center
                        ) {
                            // Disk grooves
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .background(Color.White.copy(alpha = 0.05f))
                            )
                            Icon(
                                imageVector = if (MediaPlayerManager.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                                contentDescription = "Medya",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    // 2. Expanded Window Player
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1625)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (MediaPlayerManager.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = MediaPlayerManager.mediaName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { MediaPlayerManager.isCollapsed = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Minimize, contentDescription = "Küçült", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { MediaPlayerManager.close() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Render Video Surface if Video Mode
                            if (MediaPlayerManager.isVideo && MediaPlayerManager.currentUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                setVideoURI(MediaPlayerManager.currentUri)
                                                setOnPreparedListener {
                                                    start()
                                                    MediaPlayerManager.registerVideoView(this)
                                                }
                                                setOnCompletionListener {
                                                    MediaPlayerManager.isPlaying = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        update = { videoView ->
                                            // Keep playing in sync
                                        }
                                    )
                                }
                            } else {
                                // Audio wave visualization overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.04f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(10) { i ->
                                            val animatedHeight = remember { mutableStateOf(10f) }
                                            LaunchedEffect(MediaPlayerManager.isPlaying) {
                                                if (MediaPlayerManager.isPlaying) {
                                                    while (true) {
                                                        animatedHeight.value = (10..45).random().toFloat()
                                                        delay(100)
                                                    }
                                                } else {
                                                    animatedHeight.value = 10f
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(animatedHeight.value.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Timeline / Seek bar
                            val progress = if (MediaPlayerManager.duration > 0) {
                                MediaPlayerManager.currentPosition.toFloat() / MediaPlayerManager.duration.toFloat()
                            } else {
                                0f
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDuration(MediaPlayerManager.currentPosition),
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                                Slider(
                                    value = progress,
                                    onValueChange = { newProgress ->
                                        val seekPos = (newProgress * MediaPlayerManager.duration).toInt()
                                        MediaPlayerManager.seekTo(seekPos)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                    )
                                )
                                Text(
                                    text = formatDuration(MediaPlayerManager.duration),
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }

                            // Media Controls Row
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = { MediaPlayerManager.seekTo(0) }) {
                                    Icon(Icons.Default.Replay, contentDescription = "Başa Al", tint = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { MediaPlayerManager.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (MediaPlayerManager.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Oynat/Durdur",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { 
                                    MediaPlayerManager.stop()
                                    MediaPlayerManager.close()
                                }) {
                                    Icon(Icons.Default.Stop, contentDescription = "Durdur", tint = Color.White)
                                }
                                IconButton(onClick = { 
                                    SocialMediaAnalyzerState.showAnalyzer = true
                                }) {
                                    Icon(Icons.Default.Analytics, contentDescription = "Sosyal Medya Analizi", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
