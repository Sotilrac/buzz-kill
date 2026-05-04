package ca.asmat.buzzkill.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import ca.asmat.buzzkill.R

val TiltNeonFamily = FontFamily(Font(R.font.tilt_neon))
val RubikGlitchFamily = FontFamily(Font(R.font.rubik_glitch))

private val BuzzKillColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFAA22), // amber LCD
        onPrimary = Color(0xFF1A0E00),
        secondary = Color(0xFF22FF66), // green LED
        onSecondary = Color(0xFF002211),
        tertiary = Color(0xFFFF3322), // red LED / danger
        onTertiary = Color(0xFF220000),
        background = Color(0xFF0A0806),
        onBackground = Color(0xFFCCBBAA),
        surface = Color(0xFF14110D),
        onSurface = Color(0xFFCCBBAA),
        surfaceVariant = Color(0xFF1A1612),
        onSurfaceVariant = Color(0xFF998877),
        error = Color(0xFFFF3322),
        outline = Color(0xFF332A22),
    )

@Composable
fun BuzzKillTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BuzzKillColorScheme,
        content = content,
    )
}
