package cn.sta1n.nai2android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B68),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3F1E4),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF9A4931),
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF3A0A00),
    tertiary = Color(0xFF66558E),
    background = Color(0xFFF7F8F5),
    surface = Color(0xFFFCFDF9),
    surfaceVariant = Color(0xFFE1E8E4),
    outline = Color(0xFF6F7975),
    onSurface = Color(0xFF191C1B),
    onSurfaceVariant = Color(0xFF404947)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FE5D6),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFBDF4E7),
    secondary = Color(0xFFFFB39D),
    onSecondary = Color(0xFF5A1E0D),
    secondaryContainer = Color(0xFF7B3321),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFD4BBFF),
    onTertiary = Color(0xFF36245F),
    tertiaryContainer = Color(0xFF4D3A76),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF0E1110),
    surface = Color(0xFF151918),
    surfaceVariant = Color(0xFF252B29),
    surfaceTint = Color(0xFF9FE5D6),
    outline = Color(0xFF8D9995),
    outlineVariant = Color(0xFF414846),
    onSurface = Color(0xFFE0E4E1),
    onSurfaceVariant = Color(0xFFBEC9C4)
)

private val StudioShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun Nai2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = StudioShapes,
        content = content
    )
}
