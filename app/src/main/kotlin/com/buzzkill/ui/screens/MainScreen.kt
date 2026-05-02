package com.buzzkill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.buzzkill.data.PermissionStatus
import com.buzzkill.data.PersistedState
import com.buzzkill.data.SettingsRepository
import com.buzzkill.data.StatusLine
import com.buzzkill.data.UiState
import com.buzzkill.ui.components.HardwareButton
import com.buzzkill.ui.components.HardwareButtonStyle
import com.buzzkill.ui.components.Led
import com.buzzkill.ui.components.LedColor
import com.buzzkill.ui.components.LedSwitch
import com.buzzkill.ui.components.Panel
import com.buzzkill.ui.components.SevenSegmentDisplay
import com.buzzkill.ui.theme.BuzzKillTheme

enum class PermissionItem { Accessibility, Battery, Notifications, ScheduledPowerOn, OemKiller }

@Composable
fun MainScreen(
    state: UiState,
    onToggleEnabled: (Boolean) -> Unit,
    onSetWindow: (start: Int, end: Int) -> Unit,
    onSetInactivitySeconds: (Int) -> Unit,
    onFixPermission: (PermissionItem) -> Unit,
    onTestTrigger: () -> Unit,
    onConfirmFirstShutdown: () -> Unit = {},
) {
    var editing by remember { mutableStateOf<TimeEdit?>(null) }
    var pendingEnable by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0806))
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header() }
        item {
            SchedulePanel(
                persisted = state.persisted,
                onToggleEnabled = { newValue ->
                    if (newValue && !state.persisted.firstShutdownConfirmed) {
                        pendingEnable = true
                    } else {
                        onToggleEnabled(newValue)
                    }
                },
                onEditStart = { editing = TimeEdit.Start },
                onEditEnd = { editing = TimeEdit.End },
                onSetInactivitySeconds = onSetInactivitySeconds,
            )
        }
        item {
            StatusPanel(
                state = state,
                onFixPermission = onFixPermission,
            )
        }
        item {
            TestTriggerPanel(onTrigger = onTestTrigger)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    editing?.let { which ->
        val initial = if (which == TimeEdit.Start)
            state.persisted.windowStartMinutes
        else
            state.persisted.windowEndMinutes
        TimeEditDialog(
            initialMinutes = initial,
            onDismiss = { editing = null },
            onConfirm = { newMinutes ->
                val (s, e) = when (which) {
                    TimeEdit.Start -> newMinutes to state.persisted.windowEndMinutes
                    TimeEdit.End -> state.persisted.windowStartMinutes to newMinutes
                }
                onSetWindow(s, e)
                editing = null
            },
        )
    }

    if (pendingEnable) {
        FirstShutdownDialog(
            onConfirm = {
                pendingEnable = false
                onConfirmFirstShutdown()
                onToggleEnabled(true)
            },
            onDismiss = { pendingEnable = false },
        )
    }
}

@Composable
private fun FirstShutdownDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF14110D))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "BEFORE WE BEGIN",
                color = Color(0xFFFFAA22),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
            Text(
                text = "Once the timer fires, the phone will fully power off. " +
                    "It will not turn back on by itself unless you've configured the OEM " +
                    "scheduled power-on. Make sure you've set that and that anything " +
                    "time-sensitive (alarms in another app, on-call rotations) accounts for it.",
                color = Color(0xFFCCBBAA),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                HardwareButton(text = "cancel", onClick = onDismiss)
                HardwareButton(
                    text = "i understand",
                    onClick = onConfirm,
                    style = HardwareButtonStyle.Confirm,
                )
            }
        }
    }
}

private enum class TimeEdit { Start, End }

@Composable
private fun Header() {
    Column {
        Text(
            text = "BUZZKILL",
            color = Color(0xFFFFAA22),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = 6.sp,
        )
        Text(
            text = "phone-off scheduler",
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun SchedulePanel(
    persisted: PersistedState,
    onToggleEnabled: (Boolean) -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onSetInactivitySeconds: (Int) -> Unit,
) {
    Panel(label = "schedule", modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LedSwitch(
                isOn = persisted.enabled,
                onToggle = { onToggleEnabled(!persisted.enabled) },
                label = "armed",
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledTime(
                    label = "window open",
                    minutes = persisted.windowStartMinutes,
                    onClick = onEditStart,
                )
                LabeledTime(
                    label = "window close",
                    minutes = persisted.windowEndMinutes,
                    onClick = onEditEnd,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        InactivityRow(
            seconds = persisted.inactivityTimeoutSeconds,
            onChange = onSetInactivitySeconds,
        )
    }
}

@Composable
private fun LabeledTime(label: String, minutes: Int, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
        )
        SevenSegmentDisplay(
            text = formatHHMM(minutes),
            digitWidth = 22.dp,
            digitHeight = 36.dp,
        )
    }
}

@Composable
private fun InactivityRow(seconds: Int, onChange: (Int) -> Unit) {
    val minutes = seconds / 60
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "INACTIVITY TIMEOUT",
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HardwareButton(
                text = "-",
                onClick = { onChange(((minutes - 1).coerceAtLeast(1)) * 60) },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            )
            SevenSegmentDisplay(
                text = formatMinutes(minutes),
                digitWidth = 22.dp,
                digitHeight = 36.dp,
            )
            Text(
                text = "MIN",
                color = Color(0xFF665544),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
            HardwareButton(
                text = "+",
                onClick = { onChange(((minutes + 1).coerceAtMost(180)) * 60) },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun StatusPanel(
    state: UiState,
    onFixPermission: (PermissionItem) -> Unit,
) {
    Panel(label = "status", modifier = Modifier.fillMaxWidth()) {
        when (val s = state.status) {
            is StatusLine.NeedsSetup -> ChecklistView(state.permissions, onFixPermission)
            is StatusLine.Disabled -> StatusText("Disabled. Toggle ARMED to begin.")
            is StatusLine.Armed -> StatusText("Armed. Window opens in ${formatDuration(s.nextOpenMinutes * 60)}.")
            is StatusLine.Active -> StatusText("Active. ${formatDuration(s.minutesRemainingInWindow * 60)} remaining in window.")
            is StatusLine.Counting -> StatusText("Counting down. ${formatDuration(s.secondsRemaining)} until shutdown.")
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        color = Color(0xFFCCBBAA),
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
    )
}

@Composable
private fun ChecklistView(perms: PermissionStatus, onFix: (PermissionItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChecklistRow(
            label = "Accessibility service",
            granted = perms.accessibilityEnabled,
            ledOff = LedColor.Red,
            onFix = { onFix(PermissionItem.Accessibility) },
        )
        ChecklistRow(
            label = "Battery optimization",
            granted = perms.batteryOptimizationExempt,
            ledOff = LedColor.Red,
            onFix = { onFix(PermissionItem.Battery) },
        )
        ChecklistRow(
            label = "Notifications",
            granted = perms.notificationsGranted,
            ledOff = LedColor.Red,
            onFix = { onFix(PermissionItem.Notifications) },
        )
        ChecklistRow(
            label = "Scheduled power-on",
            granted = perms.scheduledPowerOnAcked,
            ledOff = LedColor.Amber,
            onFix = { onFix(PermissionItem.ScheduledPowerOn) },
        )
        ChecklistRow(
            label = "Battery-killer override",
            granted = perms.oemKillerAcked,
            ledOff = LedColor.Amber,
            onFix = { onFix(PermissionItem.OemKiller) },
        )
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    granted: Boolean,
    ledOff: LedColor,
    onFix: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Led(isOn = granted, color = if (granted) LedColor.Green else ledOff)
        RowLabel(text = label)
        if (!granted) {
            HardwareButton(
                text = "fix",
                onClick = onFix,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun RowScope.RowLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFFCCBBAA),
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun TestTriggerPanel(onTrigger: () -> Unit) {
    var counting by remember { mutableStateOf(false) }
    var remaining by remember { mutableStateOf(TEST_COUNTDOWN_SECONDS) }

    if (counting) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining -= 1
            }
            if (counting) {
                counting = false
                onTrigger()
                remaining = TEST_COUNTDOWN_SECONDS
            }
        }
    }

    Panel(label = "test trigger", modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "10s countdown, dry-run shutdown sequence (does not power off).",
                color = Color(0xFF998877),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )

            if (counting) {
                SevenSegmentDisplay(
                    text = formatMinutes(remaining),
                    digitWidth = 28.dp,
                    digitHeight = 48.dp,
                    litColor = Color(0xFFFF4422),
                )
                HardwareButton(
                    text = "cancel",
                    onClick = {
                        counting = false
                        remaining = TEST_COUNTDOWN_SECONDS
                    },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                )
            } else {
                HardwareButton(
                    text = "TRIGGER",
                    onClick = {
                        remaining = TEST_COUNTDOWN_SECONDS
                        counting = true
                    },
                    style = HardwareButtonStyle.Danger,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 18.dp),
                )
            }
        }
    }
}

private const val TEST_COUNTDOWN_SECONDS = 10

@Composable
private fun TimeEditDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF14110D))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(state = pickerState)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HardwareButton(text = "cancel", onClick = onDismiss)
                HardwareButton(
                    text = "ok",
                    onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) },
                    style = HardwareButtonStyle.Confirm,
                )
            }
        }
    }
}

private fun formatHHMM(minutes: Int): String {
    val h = (minutes / 60).coerceIn(0, 23)
    val m = (minutes % 60).coerceIn(0, 59)
    return "%02d:%02d".format(h, m)
}

private fun formatMinutes(minutes: Int): String = "%02d".format(minutes.coerceIn(0, 999))

private fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return when {
        s >= 3600 -> "%dh %02dm".format(s / 3600, (s % 3600) / 60)
        s >= 60 -> "%dm %02ds".format(s / 60, s % 60)
        else -> "%ds".format(s)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0806, heightDp = 800)
@Composable
private fun MainScreenPreview() {
    BuzzKillTheme {
        MainScreen(
            state = UiState(
                persisted = PersistedState(
                    windowStartMinutes = SettingsRepository.DEFAULT_WINDOW_START,
                    windowEndMinutes = SettingsRepository.DEFAULT_WINDOW_END,
                    inactivityTimeoutSeconds = 30 * 60,
                    enabled = true,
                    isInWindow = false,
                    isArmed = true,
                    lastTriggerTime = null,
                    countdownStartedAt = null,
                    scheduledPowerOnAcked = true,
                    oemKillerAcked = true,
                    firstShutdownConfirmed = false,
                ),
                permissions = PermissionStatus(
                    accessibilityEnabled = true,
                    batteryOptimizationExempt = true,
                    notificationsGranted = true,
                    scheduledPowerOnAcked = true,
                    oemKillerAcked = true,
                ),
                status = StatusLine.Armed(180),
            ),
            onToggleEnabled = {},
            onSetWindow = { _, _ -> },
            onSetInactivitySeconds = {},
            onFixPermission = {},
            onTestTrigger = {},
        )
    }
}
