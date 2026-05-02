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
import androidx.compose.foundation.layout.padding
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

    val (top, bottom, accent) = when (style) {
        HardwareButtonStyle.Neutral -> Triple(Color(0xFF3A3026), Color(0xFF1A1612), Color(0xFFCCBBAA))
        HardwareButtonStyle.Danger -> Triple(Color(0xFF7A2018), Color(0xFF2A0A04), Color(0xFFFFCCBB))
        HardwareButtonStyle.Confirm -> Triple(Color(0xFF1A4A2A), Color(0xFF050E08), Color(0xFFCCFFCC))
    }
    val gradient = if (isPressed) {
        Brush.verticalGradient(listOf(bottom, top))
    } else {
        Brush.verticalGradient(listOf(top, bottom))
    }
    val alpha = if (enabled) 1f else 0.4f
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient)
            .border(1.dp, Color(0xFF050402).copy(alpha = alpha), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(contentPadding),
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
