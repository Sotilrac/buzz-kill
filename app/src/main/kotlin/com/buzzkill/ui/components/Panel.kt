package com.buzzkill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text as M3Text
import com.buzzkill.ui.theme.BuzzKillTheme
import com.buzzkill.ui.theme.Panel as PanelColors

@Composable
fun Panel(
    label: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PanelColors.Background,
                        PanelColors.DeepBackground,
                    ),
                ),
            )
            .border(width = 1.dp, color = PanelColors.BevelLight, shape = shape)
            .padding(2.dp)
            .border(width = 1.dp, color = PanelColors.BevelDark, shape = shape)
            .padding(12.dp),
    ) {
        if (label != null) {
            M3Text(
                text = label.uppercase(),
                color = PanelColors.LabelDim,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Box(modifier = Modifier.padding(top = 0.dp)) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0806)
@Composable
private fun PanelPreview() {
    BuzzKillTheme {
        Panel(label = "schedule") {
            M3Text(
                text = "panel content",
                color = Color(0xFFCCBBAA),
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
