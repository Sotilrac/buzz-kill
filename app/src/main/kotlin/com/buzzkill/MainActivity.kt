package com.buzzkill

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.Intent
import com.buzzkill.oem.ScheduledPowerOnIntents
import com.buzzkill.service.Broadcasts
import com.buzzkill.ui.screens.MainScreen
import com.buzzkill.ui.screens.PermissionItem
import com.buzzkill.ui.theme.BuzzKillTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val notificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> refreshPermissions() }

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
                        onTestTriggerDryRun = { sendTestTrigger(Broadcasts.TEST_TRIGGER_DRY_RUN) },
                        onTestTriggerLive = { sendTestTrigger(Broadcasts.TEST_TRIGGER_LIVE) },
                        onConfirmFirstShutdown = { viewModel.setFirstShutdownConfirmed(true) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        viewModel.refreshPermissions(
            accessibilityEnabled = PermissionsHelper.isAccessibilityEnabled(this),
            batteryOptimizationExempt = PermissionsHelper.isBatteryOptimizationExempt(this),
            notificationsGranted = PermissionsHelper.isNotificationsGranted(this),
        )
    }

    private fun onFixPermission(item: PermissionItem) {
        when (item) {
            PermissionItem.Accessibility -> PermissionsHelper.openAccessibilitySettings(this)
            PermissionItem.Battery -> PermissionsHelper.requestBatteryOptimizationExempt(this)
            PermissionItem.Notifications -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            PermissionItem.ScheduledPowerOn -> {
                val launched = ScheduledPowerOnIntents.launch(this)
                if (!launched) {
                    Toast.makeText(
                        this,
                        "Open Settings → Additional settings → Scheduled power on/off",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                viewModel.setScheduledPowerOnAcked(true)
            }
            PermissionItem.OemKiller -> {
                Toast.makeText(
                    this,
                    "See dontkillmyapp.com/oneplus and lock BuzzKill in Recents.",
                    Toast.LENGTH_LONG,
                ).show()
                viewModel.setOemKillerAcked(true)
            }
        }
    }

    private fun sendTestTrigger(action: String) {
        if (!PermissionsHelper.isAccessibilityEnabled(this)) {
            Toast.makeText(this, "Enable the accessibility service first.", Toast.LENGTH_SHORT).show()
            return
        }
        sendBroadcast(Intent(action).setPackage(packageName))
    }
}
