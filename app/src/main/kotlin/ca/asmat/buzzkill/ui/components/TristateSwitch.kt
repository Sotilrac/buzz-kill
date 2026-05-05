package ca.asmat.buzzkill.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Horizontal three-position physical-style toggle. Drag the knob or tap a
 * section to set the position; on release the knob snaps to the nearest of
 * three stops. Same recessed cap aesthetic as [LedSwitch].
 *
 * Position 0 is the leftmost stop, 2 is the rightmost. Caller decides what
 * each position means.
 */
@Composable
fun TristateSwitch(
    position: Int,
    onPositionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 132.dp,
    height: Dp = 32.dp,
) {
    val recessInset = 3.dp
    val trackWidth = width - recessInset * 2
    val trackHeight = height - recessInset * 2
    val knobWidth = 36.dp
    val travel = trackWidth - knobWidth

    val targetFraction = position.coerceIn(0, 2) / 2f
    var dragFraction by remember { mutableFloatStateOf(Float.NaN) }
    val animatedFraction by animateFloatAsState(
        targetValue = if (dragFraction.isNaN()) targetFraction else dragFraction,
        animationSpec = tween(durationMillis = if (dragFraction.isNaN()) 140 else 0),
        label = "tristateOffset",
    )

    fun fractionToPosition(frac: Float): Int = when {
        frac < 1f / 3f -> 0
        frac < 2f / 3f -> 1
        else -> 2
    }

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050402), Color(0xFF2A2218)),
                ),
            )
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val frac = (tap.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onPositionChange(fractionToPosition(frac))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { off ->
                        dragFraction = (off.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (!dragFraction.isNaN()) {
                            onPositionChange(fractionToPosition(dragFraction))
                        }
                        dragFraction = Float.NaN
                    },
                    onDragCancel = { dragFraction = Float.NaN },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Inner track sits inside the recess.
        Box(
            modifier = Modifier
                .padding(horizontal = recessInset, vertical = recessInset)
                .size(trackWidth, trackHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF050402), Color(0xFF14110D)),
                    ),
                )
                .border(0.5.dp, Color(0xFF050402), RoundedCornerShape(4.dp)),
        ) {
            // Knob
            Box(
                modifier = Modifier
                    .padding(start = travel * animatedFraction)
                    .size(width = knobWidth, height = trackHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF6E5B45), Color(0xFF332A22)),
                        ),
                    )
                    .border(1.dp, Color(0xFF14110D), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    repeat(3) {
                        Spacer(
                            Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(Color(0xFFA89685)),
                        )
                    }
                }
            }
        }
    }
}
