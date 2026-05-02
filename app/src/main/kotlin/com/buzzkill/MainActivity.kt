package com.buzzkill

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.buzzkill.ui.screens.MainScreen
import com.buzzkill.ui.screens.PermissionItem
import com.buzzkill.ui.theme.BuzzKillTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuzzKillTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0806),
                ) {
                    val state by viewModel.uiState.collectAsState()
                    MainScreen(
                        state = state,
                        onToggleEnabled = viewModel::setEnabled,
                        onSetWindow = { s, e -> viewModel.setWindow(s, e) },
                        onSetInactivitySeconds = viewModel::setInactivitySeconds,
                        onFixPermission = ::onFixPermission,
                        onTestTrigger = ::onTestTrigger,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Phase 5 will compute these from system state. For now, leave defaults.
    }

    private fun onFixPermission(item: PermissionItem) {
        // Wired in Phase 5. For now, ack-only items can be confirmed here.
        when (item) {
            PermissionItem.ScheduledPowerOn -> viewModel.setScheduledPowerOnAcked(true)
            PermissionItem.OemKiller -> viewModel.setOemKillerAcked(true)
            else -> Toast.makeText(this, "Phase 5: deep-link not wired yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onTestTrigger() {
        // Phase 9 will run the dry-run shutdown sequence here.
        Toast.makeText(this, "Test trigger: not yet wired (phase 9)", Toast.LENGTH_SHORT).show()
    }
}
