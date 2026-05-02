package com.buzzkill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buzzkill.ui.theme.BuzzKillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuzzKillTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0806),
                ) {
                    PlaceholderScreen()
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0806))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "BuzzKill",
                color = Color(0xFFFFAA22),
                fontFamily = FontFamily.Monospace,
                fontSize = 48.sp,
            )
            Text(
                text = "v0.1.0",
                color = Color(0xFF665544),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0806)
@Composable
private fun PlaceholderScreenPreview() {
    BuzzKillTheme {
        PlaceholderScreen()
    }
}
