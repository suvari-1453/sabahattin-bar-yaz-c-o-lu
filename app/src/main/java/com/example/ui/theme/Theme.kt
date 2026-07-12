package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.SettingsManager

// Theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE57373), // Gundi Bro red accent
    secondary = Color(0xFFB0BEC5),
    tertiary = Color(0xFF81C784),
    background = Color(0xFF121016),
    surface = Color(0xFF1E1B24),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),
    secondary = Color(0xFF37474F),
    tertiary = Color(0xFF388E3C),
    background = Color(0xFFF5F5F7),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF121016),
    onSurface = Color(0xFF121016)
)

private val MatrixColorScheme = darkColorScheme(
    primary = Color(0xFF00FF00), // Bright green
    secondary = Color(0xFF003300),
    tertiary = Color(0xFF00AA00),
    background = Color(0xFF000000), // Solid black
    surface = Color(0xFF001100),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFF00FF00),
    onSurface = Color(0xFF00FF00)
)

private val CrimsonColorScheme = darkColorScheme(
    primary = Color(0xFFFF0000), // Crimson Red
    secondary = Color(0xFFFFFFFF), // White
    tertiary = Color(0xFF880000),
    background = Color(0xFF1A0000), // Dark red shade
    surface = Color(0xFF330000),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val theme by settingsManager.theme.collectAsState()

    val colorScheme = when (theme) {
        "Light" -> LightColorScheme
        "Matrix" -> MatrixColorScheme
        "Crimson" -> CrimsonColorScheme
        else -> DarkColorScheme // "Dark"
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
