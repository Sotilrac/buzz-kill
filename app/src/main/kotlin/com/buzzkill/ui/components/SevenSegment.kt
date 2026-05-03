package com.buzzkill.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.buzzkill.ui.theme.BuzzKillTheme
import com.buzzkill.ui.theme.Lcd

private val DigitMap: Map<Char, BooleanArray> =
    mapOf(
        // segments order: a, b, c, d, e, f, g
        '0' to booleanArrayOf(true, true, true, true, true, true, false),
        '1' to booleanArrayOf(false, true, true, false, false, false, false),
        '2' to booleanArrayOf(true, true, false, true, true, false, true),
        '3' to booleanArrayOf(true, true, true, true, false, false, true),
        '4' to booleanArrayOf(false, true, true, false, false, true, true),
        '5' to booleanArrayOf(true, false, true, true, false, true, true),
        '6' to booleanArrayOf(true, false, true, true, true, true, true),
        '7' to booleanArrayOf(true, true, true, false, false, false, false),
        '8' to booleanArrayOf(true, true, true, true, true, true, true),
        '9' to booleanArrayOf(true, true, true, true, false, true, true),
        '-' to booleanArrayOf(false, false, false, false, false, false, true),
        ' ' to booleanArrayOf(false, false, false, false, false, false, false),
    )

@Composable
fun SevenSegmentDigit(
    char: Char,
    digitWidth: Dp = 32.dp,
    digitHeight: Dp = 56.dp,
    litColor: Color = Lcd.Lit,
    unlitColor: Color = Lcd.Unlit,
) {
    val segments = DigitMap[char] ?: DigitMap[' ']!!
    Canvas(modifier = Modifier.size(digitWidth, digitHeight)) {
        drawSevenSegment(segments, litColor, unlitColor)
    }
}

@Composable
fun SevenSegmentDisplay(
    text: String,
    digitWidth: Dp = 32.dp,
    digitHeight: Dp = 56.dp,
    spacing: Dp = 4.dp,
    litColor: Color = Lcd.Lit,
    unlitColor: Color = Lcd.Unlit,
    background: Color = Lcd.Background,
) {
    Row(
        modifier =
        Modifier
            .background(background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        text.forEach { c ->
            if (c == ':') {
                ColonDot(digitHeight = digitHeight, color = litColor)
            } else {
                SevenSegmentDigit(c, digitWidth, digitHeight, litColor, unlitColor)
            }
        }
    }
}

@Composable
private fun ColonDot(
    digitHeight: Dp,
    color: Color,
) {
    Canvas(modifier = Modifier.size(width = 8.dp, height = digitHeight)) {
        val r = size.minDimension * 0.18f
        val cx = size.width / 2f
        val gap = size.height * 0.18f
        drawCircle(color, radius = r, center = Offset(cx, size.height / 2f - gap))
        drawCircle(color, radius = r, center = Offset(cx, size.height / 2f + gap))
    }
}

private fun DrawScope.drawSevenSegment(
    segments: BooleanArray,
    lit: Color,
    unlit: Color,
) {
    val w = size.width
    val h = size.height
    val t = (minOf(w, h) * 0.13f) // segment thickness
    val pad = t * 0.4f // tiny gap between segments
    val midY = h / 2f

    // Horizontal segments (a top, g middle, d bottom)
    fun horiz(yCenter: Float): Path =
        Path().apply {
            val y0 = yCenter - t / 2f
            val y1 = yCenter + t / 2f
            moveTo(pad + t / 2f, y0)
            lineTo(w - pad - t / 2f, y0)
            lineTo(w - pad, yCenter)
            lineTo(w - pad - t / 2f, y1)
            lineTo(pad + t / 2f, y1)
            lineTo(pad, yCenter)
            close()
        }

    // Vertical segments
    fun vert(
        xCenter: Float,
        yTop: Float,
        yBot: Float,
    ): Path =
        Path().apply {
            val x0 = xCenter - t / 2f
            val x1 = xCenter + t / 2f
            moveTo(x0, yTop + t / 2f)
            lineTo(xCenter, yTop)
            lineTo(x1, yTop + t / 2f)
            lineTo(x1, yBot - t / 2f)
            lineTo(xCenter, yBot)
            lineTo(x0, yBot - t / 2f)
            close()
        }

    val pathA = horiz(t / 2f)
    val pathG = horiz(midY)
    val pathD = horiz(h - t / 2f)
    val pathF = vert(t / 2f, t + pad, midY - pad) // top-left
    val pathB = vert(w - t / 2f, t + pad, midY - pad) // top-right
    val pathE = vert(t / 2f, midY + pad, h - t - pad) // bottom-left
    val pathC = vert(w - t / 2f, midY + pad, h - t - pad) // bottom-right

    val paths = arrayOf(pathA, pathB, pathC, pathD, pathE, pathF, pathG)
    paths.forEachIndexed { i, p ->
        drawPath(p, color = if (segments[i]) lit else unlit)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0806)
@Composable
private fun SevenSegmentPreview() {
    BuzzKillTheme {
        SevenSegmentDisplay(text = "23:45")
    }
}
