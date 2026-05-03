package com.buzzkill.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buzzkill.ui.theme.BuzzKillTheme

@Composable
fun LedSwitch(
    isOn: Boolean,
    onToggle: () -> Unit,
    label: String? = null,
    ledColor: LedColor = LedColor.Green,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Led(isOn = isOn, color = ledColor)
        SwitchBody(isOn = isOn, onToggle = onToggle)
        if (label != null) {
            Text(
                text = label.uppercase(),
                color = Color(0xFF665544),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun SwitchBody(isOn: Boolean, onToggle: () -> Unit) {
    // Outer recess (the "panel hole") + inner track + knob, mirroring the layered
    // bezel/cap pattern used by CircleStepperButton.
    val outerWidth = 44.dp
    val outerHeight = 64.dp
    val recessInset = 3.dp
    val trackWidth = outerWidth - recessInset * 2
    val trackHeight = outerHeight - recessInset * 2
    val knobHeight = 28.dp
    val travel = trackHeight - knobHeight

    val offsetFraction by animateFloatAsState(
        targetValue = if (isOn) 0f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "switchOffset",
    )

    // Outer recess: dark vertical gradient suggesting a hole in the front panel.
    Box(
        modifier = Modifier
            .size(outerWidth, outerHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050402), Color(0xFF2A2218)),
                ),
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        // Inner track sits inside the recess.
        Box(
            modifier = Modifier
                .size(trackWidth, trackHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF050402), Color(0xFF14110D)),
                    ),
                )
                .border(0.5.dp, Color(0xFF050402), RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = travel * offsetFraction)
                    .size(width = trackWidth - 4.dp, height = knobHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF6E5B45), Color(0xFF332A22)),
                        ),
                    )
                    .border(1.dp, Color(0xFF14110D), RoundedCornerShape(3.dp))
                    .align(Alignment.TopCenter),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    repeat(3) {
                        Spacer(
                            Modifier
                                .height(1.dp)
                                .width(16.dp)
                                .background(Color(0xFF050402)),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0806)
@Composable
private fun LedSwitchPreview() {
    BuzzKillTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LedSwitch(isOn = true, onToggle = {}, label = "armed")
            LedSwitch(isOn = false, onToggle = {}, label = "armed")
        }
    }
}
