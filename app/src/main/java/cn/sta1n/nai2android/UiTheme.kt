package cn.sta1n.nai2android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2C5F61),
    onPrimary = Color.White,
    secondary = Color(0xFFB76E4C),
    background = Color(0xFFF5F1EA),
    surface = Color(0xFFFFFCF7),
    surfaceVariant = Color(0xFFE5E0D7),
    onSurface = Color(0xFF252321),
    onSurfaceVariant = Color(0xFF625D56)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FD0CE),
    secondary = Color(0xFFF0B18F)
)

@Composable
fun Nai2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

