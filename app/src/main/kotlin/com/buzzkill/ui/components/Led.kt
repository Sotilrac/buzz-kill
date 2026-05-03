package com.buzzkill.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.buzzkill.ui.theme.BuzzKillTheme
import com.buzzkill.ui.theme.Led

enum class LedColor { Green, Red, Amber }

@Composable
fun Led(
    isOn: Boolean,
    color: LedColor = LedColor.Green,
    diameter: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    val (lit, unlit, glow) =
        when (color) {
            LedColor.Green -> Triple(Led.GreenLit, Led.GreenUnlit, Led.GreenGlow)
            LedColor.Red -> Triple(Led.RedLit, Led.RedUnlit, Led.RedGlow)
            LedColor.Amber -> Triple(Led.AmberLit, Led.AmberUnlit, Led.AmberGlow)
        }

    Canvas(modifier = modifier.size(diameter * 2.4f)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val ledRadius = diameter.toPx() / 2f

        if (isOn) {
            // Outer glow
            drawCircle(
                brush =
                Brush.radialGradient(
                    colors = listOf(glow, glow.copy(alpha = 0f)),
                    center = center,
                    radius = ledRadius * 3f,
                ),
                radius = ledRadius * 3f,
                center = center,
            )
        }

        // Body
        drawCircle(
            color = if (isOn) lit else unlit,
            radius = ledRadius,
            center = center,
        )

        // Inner highlight (top-left, gives the lens look)
        if (isOn) {
            drawCircle(
                brush =
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(center.x - ledRadius * 0.3f, center.y - ledRadius * 0.3f),
                    radius = ledRadius * 0.6f,
                ),
                radius = ledRadius * 0.6f,
                center = Offset(center.x - ledRadius * 0.3f, center.y - ledRadius * 0.3f),
            )
        }

        // Bezel ring
        drawCircle(
            color = Color(0xFF2A2218),
            radius = ledRadius * 1.15f,
            center = center,
            style =
            androidx.compose.ui.graphics.drawscope
                .Stroke(width = ledRadius * 0.18f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0806)
@Composable
private fun LedPreview() {
    BuzzKillTheme {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(16.dp),
        ) {
            Led(isOn = true, color = LedColor.Green)
            Led(isOn = true, color = LedColor.Red)
            Led(isOn = true, color = LedColor.Amber)
            Led(isOn = false, color = LedColor.Green)
            Led(isOn = false, color = LedColor.Red)
        }
    }
}
