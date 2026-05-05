package ca.asmat.buzzkill.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Three-position physical-style toggle, vertical orientation. Tap cycles through
 * positions (0 → 1 → 2 → 0). Same recess/cap aesthetic as [LedSwitch].
 *
 * Position 0 is the top, 2 is the bottom. Caller decides what each means.
 */
@Composable
fun TristateSwitch(
    position: Int,
    onPositionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerWidth = 44.dp
    val outerHeight = 96.dp
    val recessInset = 3.dp
    val trackWidth = outerWidth - recessInset * 2
    val trackHeight = outerHeight - recessInset * 2
    val knobHeight = 22.dp
    val travel = trackHeight - knobHeight

    val targetFraction = (position.coerceIn(0, 2)) / 2f
    val offsetFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 140),
        label = "tristateOffset",
    )

    Box(
        modifier = modifier
            .size(outerWidth, outerHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050402), Color(0xFF2A2218)),
                ),
            )
            .clickable { onPositionChange((position + 1) % 3) },
        contentAlignment = Alignment.Center,
    ) {
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
                                .background(Color(0xFFA89685)),
                        )
                    }
                }
            }
        }
    }
}
