package com.buzzkill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buzzkill.ui.theme.BuzzKillTheme

enum class HardwareButtonStyle { Neutral, Danger, Confirm }

/**
 * Circular push-button styled like a physical hardware button: outer recess ring,
 * domed cap with vertical-gradient highlight, subtle engraved label. Press state
 * inverts the cap gradient and shifts the whole cap into the recess.
 *
 * Designed for stepper +/- in the schedule panel.
 */
@Composable
fun CircleStepperButton(
    text: String,
    onClick: () -> Unit,
    diameter: androidx.compose.ui.unit.Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val capInset = if (isPressed) 5.dp else 4.dp
    val capOffsetY = if (isPressed) 1.dp else 0.dp

    Box(
        modifier =
        modifier
            .size(diameter)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                // Recess: dark all over with a brighter rim at the bottom-right to
                // suggest light coming from the top-left and the cap sitting in a hole.
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF050402), Color(0xFF2A2218)),
                ),
            ).clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        // The cap.
        Box(
            modifier =
            Modifier
                .size(diameter - capInset * 2)
                .offset(y = capOffsetY)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors =
                        if (isPressed) {
                            listOf(Color(0xFF2A2218), Color(0xFF050402))
                        } else {
                            listOf(Color(0xFF6E5B45), Color(0xFF332A22))
                        },
                    ),
                ).border(
                    0.5.dp,
                    Color(0xFF050402),
                    androidx.compose.foundation.shape.CircleShape,
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text(
                text = text,
                color = Color(0xFFCCBBAA),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                style =
                androidx.compose.ui.text.TextStyle(
                    shadow =
                    androidx.compose.ui.graphics.Shadow(
                        color = Color(0xCC000000),
                        offset =
                        androidx.compose.ui.geometry
                            .Offset(0f, -1f),
                        blurRadius = 0f,
                    ),
                ),
            )
        }
    }
}

@Composable
fun HardwareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HardwareButtonStyle = HardwareButtonStyle.Neutral,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (top, bottom, accent) =
        when (style) {
            HardwareButtonStyle.Neutral -> Triple(Color(0xFF3A3026), Color(0xFF1A1612), Color(0xFFCCBBAA))
            HardwareButtonStyle.Danger -> Triple(Color(0xFF7A2018), Color(0xFF2A0A04), Color(0xFFFFCCBB))
            HardwareButtonStyle.Confirm -> Triple(Color(0xFF1A4A2A), Color(0xFF050E08), Color(0xFFCCFFCC))
        }
    val gradient =
        if (isPressed) {
            Brush.verticalGradient(listOf(bottom, top))
        } else {
            Brush.verticalGradient(listOf(top, bottom))
        }
    val alpha = if (enabled) 1f else 0.4f
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier =
        modifier
            .clip(shape)
            .background(gradient)
            .border(1.dp, Color(0xFF050402).copy(alpha = alpha), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ).padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = accent.copy(alpha = alpha),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0806)
@Composable
private fun HardwareButtonPreview() {
    BuzzKillTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            HardwareButton("fix", onClick = {})
            HardwareButton("test", onClick = {}, style = HardwareButtonStyle.Confirm)
            HardwareButton("trigger", onClick = {}, style = HardwareButtonStyle.Danger)
            HardwareButton("disabled", onClick = {}, enabled = false)
        }
    }
}
