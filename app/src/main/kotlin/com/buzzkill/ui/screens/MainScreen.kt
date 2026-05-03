package com.buzzkill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
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
import com.buzzkill.ui.components.CircleStepperButton
import com.buzzkill.ui.components.HardwareButton
import com.buzzkill.ui.components.HardwareButtonStyle
import com.buzzkill.ui.components.Led
import com.buzzkill.ui.components.LedColor
import com.buzzkill.ui.components.LedSwitch
import com.buzzkill.ui.components.Panel
import com.buzzkill.ui.components.SevenSegmentDisplay
import com.buzzkill.ui.theme.BuzzKillTheme
import com.buzzkill.ui.theme.TiltNeonFamily

enum class PermissionItem { Accessibility, Battery, Notifications, ScheduledPowerOn, OemKiller }

@Composable
fun MainScreen(
    state: UiState,
    onToggleEnabled: (Boolean) -> Unit,
    onSetWindow: (start: Int, end: Int) -> Unit,
    onSetInactivitySeconds: (Int) -> Unit,
    onFixPermission: (PermissionItem) -> Unit,
    onTogglePermissionAck: (PermissionItem) -> Unit,
    onTestTriggerDryRun: () -> Unit,
    onTestTriggerLive: () -> Unit,
    onConfirmFirstShutdown: () -> Unit = {},
) {
    var editing by remember { mutableStateOf<TimeEdit?>(null) }
    var pendingEnable by remember { mutableStateOf(false) }

    // BoxWithConstraints gives us the viewport height; we then enforce
    // heightIn(min = viewportHeight) on the inner Column so a weighted Spacer
    // can pin the Footer to the bottom when content is short. When content
    // overflows, verticalScroll takes over and the Spacer collapses to 0.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0806))
            .padding(WindowInsets.systemBars.asPaddingValues()),
    ) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .heightIn(min = viewportHeight - 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()
            SchedulePanel(
                state = state,
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
            SetupPanel(
                state = state,
                onFixPermission = onFixPermission,
                onTogglePermissionAck = onTogglePermissionAck,
                onTestTriggerDryRun = onTestTriggerDryRun,
                onTestTriggerLive = onTestTriggerLive,
            )
            Spacer(Modifier.weight(1f))
            Footer()
        }
    }

    editing?.let { which ->
        val initial =
            if (which == TimeEdit.Start) {
                state.persisted.windowStartMinutes
            } else {
                state.persisted.windowEndMinutes
            }
        TimeEditDialog(
            initialMinutes = initial,
            onDismiss = { editing = null },
            onConfirm = { newMinutes ->
                val (s, e) =
                    when (which) {
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
private fun FirstShutdownDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
            Modifier
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
                text =
                "Once the timer fires, the phone will fully power off. " +
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
            text = "CORPORATIONS KILL FOR YOUR ATTENTION.",
            color = Color(0xFFFF6644),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(4.dp))
        NeonTitle()
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Reclaim the night. Shut it down.",
            color = Color(0xFFCCBBAA),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun NeonTitle() {
    Row(verticalAlignment = Alignment.Bottom) {
        NeonText(
            text = "BUZZ",
            glowColor = Color(0xFFFFAA22),
            coreColor = Color(0xFFFFF6E0),
            flicker = false,
        )
        NeonText(
            text = "KILL",
            glowColor = Color(0xFFFF3322),
            coreColor = Color(0xFFFFE4DC),
            flicker = true,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "v${com.buzzkill.BuildConfig.VERSION_NAME}",
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun Footer() {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "by carlos asmat",
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.clickable { uriHandler.openUri("https://asmat.ca") },
        )
        Text(
            text = "  ·  ",
            color = Color(0xFF332A22),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
        Text(
            text = "see source",
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.clickable { uriHandler.openUri("https://gitlab.com/sotilrac/buzz-kill") },
        )
    }
}

/**
 * CSS-style neon text. Several blurred copies of the glyph in the neon colour are
 * stacked behind a bright near-white core, mimicking the "filled gas tube"
 * appearance: the fill reads as white-hot while the colour radiates outward.
 *
 * The optional [flicker] runs an irregular drop pattern (sometimes one quick dip,
 * sometimes two in rapid succession) at random intervals. Both the gap and the
 * dip pattern are re-rolled each cycle so it never settles into a loop.
 */
@Composable
private fun NeonText(
    text: String,
    glowColor: Color,
    coreColor: Color,
    flicker: Boolean,
) {
    val alpha by rememberFlickerAlpha(flicker)
    val style =
        androidx.compose.ui.text.TextStyle(
            fontFamily = TiltNeonFamily,
            fontSize = 44.sp,
            letterSpacing = 2.sp,
        )

    Box {
        // Outermost bloom.
        Text(
            text = text,
            color = glowColor.copy(alpha = 0.40f * alpha),
            style = style,
            modifier = Modifier.blur(24.dp, BlurredEdgeTreatment.Unbounded),
        )
        // Wide halo.
        Text(
            text = text,
            color = glowColor.copy(alpha = 0.60f * alpha),
            style = style,
            modifier = Modifier.blur(14.dp, BlurredEdgeTreatment.Unbounded),
        )
        // Inner halo.
        Text(
            text = text,
            color = glowColor.copy(alpha = 0.85f * alpha),
            style = style,
            modifier = Modifier.blur(6.dp, BlurredEdgeTreatment.Unbounded),
        )
        // Tight aura just outside the stroke, gives the bright fringe.
        Text(
            text = text,
            color = glowColor.copy(alpha = 0.95f * alpha),
            style = style,
            modifier = Modifier.blur(2.dp, BlurredEdgeTreatment.Unbounded),
        )
        // White-hot core.
        Text(
            text = text,
            color = coreColor.copy(alpha = alpha),
            style = style,
        )
    }
}

@Composable
private fun rememberFlickerAlpha(enabled: Boolean): androidx.compose.runtime.State<Float> {
    val alpha = remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    androidx.compose.runtime.LaunchedEffect(enabled) {
        if (!enabled) {
            alpha.floatValue = 1f
            return@LaunchedEffect
        }
        val rng = kotlin.random.Random(System.nanoTime())
        while (true) {
            // Idle gap before next flicker. Mean ~6 s, jittered.
            kotlinx.coroutines.delay(rng.nextLong(4500L, 8500L))
            // 1 dip about half the time, 2 rapid dips otherwise.
            val flickers = if (rng.nextFloat() < 0.5f) 1 else 2
            repeat(flickers) { i ->
                alpha.floatValue = 0.35f + rng.nextFloat() * 0.30f
                kotlinx.coroutines.delay(rng.nextLong(30L, 70L))
                alpha.floatValue = 1f
                if (i < flickers - 1) kotlinx.coroutines.delay(rng.nextLong(60L, 140L))
            }
        }
    }
    return alpha
}

@Composable
private fun SchedulePanel(
    state: UiState,
    onToggleEnabled: (Boolean) -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onSetInactivitySeconds: (Int) -> Unit,
) {
    val persisted = state.persisted
    Panel(label = "schedule", modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        // 1) Plain-language explanation at the top of the card.
        ScheduleNote()
        Spacer(Modifier.height(14.dp))

        // 2) Two equal-width columns: inputs on the left, toggle + status on the right.
        // IntrinsicSize.Min on the row makes it as tall as its tallest child (the
        // input column). The right column then fillMaxHeight + Arrangement.Center
        // vertically centres the toggle + status against the left side.
        Row(
            verticalAlignment = Alignment.Top,
            modifier =
            Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f),
            ) {
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
                InactivityStepper(
                    seconds = persisted.inactivityTimeoutSeconds,
                    onChange = onSetInactivitySeconds,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                LedSwitch(
                    isOn = persisted.enabled,
                    onToggle = { onToggleEnabled(!persisted.enabled) },
                    label = "armed",
                )
                Spacer(Modifier.height(10.dp))
                ScheduleStatusText(state = state)
            }
        }
    }
}

@Composable
private fun ScheduleStatusText(state: UiState) {
    // Short. The LED next to it already says armed-or-not; we just convey the
    // useful next number.
    val text =
        when (val s = state.status) {
            is StatusLine.NeedsSetup -> "set up first"
            is StatusLine.Disabled -> "Don't let them win"
            is StatusLine.Armed -> "opens in\n${formatDuration(s.nextOpenMinutes * 60)}"
            is StatusLine.Active -> "closes in\n${formatDuration(s.minutesRemainingInWindow * 60)}"
            is StatusLine.Counting -> "killing in\n${formatDuration(s.secondsRemaining)}"
        }
    Text(
        text = text,
        color = Color(0xFF998877),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        // Reserve 2 lines so the toggle above doesn't shift when the status
        // changes from a 1-line to 2-line message.
        minLines = 2,
    )
}

@Composable
private fun LabeledTime(
    label: String,
    minutes: Int,
    onClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SevenSegmentDisplay(
                text = formatHHMM(minutes),
                digitWidth = 18.dp,
                digitHeight = 30.dp,
            )
        }
    }
}

@Composable
private fun InactivityStepper(
    seconds: Int,
    onChange: (Int) -> Unit,
) {
    val minutes = seconds / 60
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "INACTIVITY (MIN)",
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircleStepperButton(
                text = "−",
                onClick = {
                    val next = (minutes - 1).coerceAtLeast(SettingsRepository.MIN_INACTIVITY_MINUTES)
                    onChange(next * 60)
                },
            )
            SevenSegmentDisplay(
                text = formatMinutes(minutes),
                digitWidth = 18.dp,
                digitHeight = 30.dp,
            )
            CircleStepperButton(
                text = "+",
                onClick = {
                    val next = (minutes + 1).coerceAtMost(SettingsRepository.MAX_INACTIVITY_MINUTES)
                    onChange(next * 60)
                },
            )
        }
    }
}

@Composable
private fun ScheduleNote() {
    Text(
        text =
        "While inside the window, if your screen stays off for the inactivity period, the phone is powered off. " +
            "\nScheduled a power-on in using the Settings to bring it back automatically in the morning.",
        color = Color(0xFF998877),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SetupPanel(
    state: UiState,
    onFixPermission: (PermissionItem) -> Unit,
    onTogglePermissionAck: (PermissionItem) -> Unit,
    onTestTriggerDryRun: () -> Unit,
    onTestTriggerLive: () -> Unit,
) {
    val pending = state.permissions.missingItems.size
    // Default-expanded while anything is missing; collapsed once everything is set.
    var expanded by remember(pending) { mutableStateOf(pending > 0) }

    val trailing =
        when {
            pending > 0 -> "$pending pending  ${if (expanded) "▾" else "▸"}"
            else -> "all set  ${if (expanded) "▾" else "▸"}"
        }

    Panel(
        label = "setup",
        trailing = trailing,
        onLabelClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            ChecklistView(state.permissions, onFixPermission, onTogglePermissionAck)
            Spacer(Modifier.height(16.dp))
            TestTriggerSection(
                onDryRun = onTestTriggerDryRun,
                onLive = onTestTriggerLive,
            )
        }
    }
}

@Composable
private fun ChecklistView(
    perms: PermissionStatus,
    onFix: (PermissionItem) -> Unit,
    onToggleAck: (PermissionItem) -> Unit,
) {
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
            onToggleAck = { onToggleAck(PermissionItem.ScheduledPowerOn) },
        )
        ChecklistRow(
            label = "Battery-killer override",
            granted = perms.oemKillerAcked,
            ledOff = LedColor.Amber,
            onFix = { onFix(PermissionItem.OemKiller) },
            onToggleAck = { onToggleAck(PermissionItem.OemKiller) },
        )
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    granted: Boolean,
    ledOff: LedColor,
    onFix: () -> Unit,
    onToggleAck: (() -> Unit)? = null,
) {
    val rowModifier =
        if (onToggleAck != null) {
            Modifier.fillMaxWidth().clickable(onClick = onToggleAck)
        } else {
            Modifier.fillMaxWidth()
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = rowModifier,
    ) {
        Led(isOn = granted, color = if (granted) LedColor.Green else ledOff)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color(0xFFCCBBAA),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            if (onToggleAck != null) {
                Text(
                    text = if (granted) "tap to un-confirm" else "tap row to confirm",
                    color = Color(0xFF665544),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }
        }
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

private enum class TriggerMode { DryRun, Live }

@Composable
private fun TestTriggerSection(
    onDryRun: () -> Unit,
    onLive: () -> Unit,
) {
    var counting by remember { mutableStateOf<TriggerMode?>(null) }
    var remaining by remember { mutableStateOf(TEST_COUNTDOWN_SECONDS) }

    counting?.let { mode ->
        androidx.compose.runtime.LaunchedEffect(mode) {
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining -= 1
            }
            if (counting == mode) {
                counting = null
                when (mode) {
                    TriggerMode.DryRun -> onDryRun()
                    TriggerMode.Live -> onLive()
                }
                remaining = TEST_COUNTDOWN_SECONDS
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "TEST TRIGGER",
            color = Color(0xFF665544),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
        val caption =
            when (counting) {
                null -> "Verify the shutdown mechanism. DRY RUN finds the target and stops; KILL actually powers off."
                TriggerMode.DryRun -> "Dry run firing in..."
                TriggerMode.Live -> "KILL firing in. Cancel to abort."
            }
        Text(
            text = caption,
            color = if (counting == TriggerMode.Live) Color(0xFFFF6644) else Color(0xFF998877),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )

        if (counting != null) {
            SevenSegmentDisplay(
                text = formatMinutes(remaining),
                digitWidth = 28.dp,
                digitHeight = 48.dp,
                litColor = if (counting == TriggerMode.Live) Color(0xFFFF4422) else Color(0xFFFFAA22),
            )
            HardwareButton(
                text = "cancel",
                onClick = {
                    counting = null
                    remaining = TEST_COUNTDOWN_SECONDS
                },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                HardwareButton(
                    text = "dry run",
                    onClick = {
                        remaining = TEST_COUNTDOWN_SECONDS
                        counting = TriggerMode.DryRun
                    },
                    style = HardwareButtonStyle.Confirm,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                )
                Spacer(Modifier.weight(1f))
                HardwareButton(
                    text = "kill",
                    onClick = {
                        remaining = TEST_COUNTDOWN_SECONDS
                        counting = TriggerMode.Live
                    },
                    style = HardwareButtonStyle.Danger,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                )
            }
        }
    }
}

private const val TEST_COUNTDOWN_SECONDS = 5

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TimeEditDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = true,
        )
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
            Modifier
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
            state =
            UiState(
                persisted =
                PersistedState(
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
                permissions =
                PermissionStatus(
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
            onTogglePermissionAck = {},
            onTestTriggerDryRun = {},
            onTestTriggerLive = {},
        )
    }
}
