package com.kairos.os.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairos.os.data.db.LocalAlarm
import com.kairos.os.domain.tools.LocalAlarmController
import com.kairos.os.ui.dotoFont
import com.kairos.os.ui.googleSansFont
import kotlinx.coroutines.launch

@Composable
fun LocalClockScreen(
    alarmController: LocalAlarmController,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var alarms by remember { mutableStateOf<List<LocalAlarm>>(emptyList()) }
    var showAddAlarmDialog by remember { mutableStateOf(false) }

    val timerRemaining by alarmController.timerRemaining.collectAsState()
    val timerDuration by alarmController.timerDuration.collectAsState()
    val timerRunning by alarmController.timerRunning.collectAsState()
    val timerPaused by alarmController.timerPaused.collectAsState()

    var selectedTimerMinutes by remember { mutableIntStateOf(5) }

    fun refreshAlarms() {
        coroutineScope.launch {
            alarms = alarmController.getAllAlarms()
        }
    }

    LaunchedEffect(Unit) {
        refreshAlarms()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .padding(horizontal = 24.dp)
    ) {
        val fabOffset = maxHeight * 0.60f

        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Header (ALARMS / TIMER)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "ALARMS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = googleSansFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "TIMER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = googleSansFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // Alarms Tab Content
                if (alarms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "No Alarms",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Alarms Scheduled",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = googleSansFont),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+' floating button or prompt @kaiclock in chat.",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(alarms, key = { it.id }) { alarm ->
                            AlarmCard(
                                alarm = alarm,
                                onDelete = {
                                    coroutineScope.launch {
                                        alarmController.cancelAlarm(alarm.id)
                                        refreshAlarms()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Timer Tab Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!timerRunning && !timerPaused) {
                        // Idle Timer Setup View with Vertical Slider
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "SELECT TIMER DURATION",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = googleSansFont,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            VerticalTimerSlider(
                                selectedMinutes = selectedTimerMinutes,
                                onMinutesChanged = { selectedTimerMinutes = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = {
                                alarmController.startTimer(selectedTimerMinutes * 60 * 1000L, "Timer")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "START TIMER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = googleSansFont,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        // Active Timer Countdown View
                        val remainingSec = (timerRemaining / 1000).toInt()
                        val minutes = remainingSec / 60
                        val seconds = remainingSec % 60
                        val displayTime = String.format("%02d:%02d", minutes, seconds)
                        val progress = if (timerDuration > 0) timerRemaining.toFloat() / timerDuration.toFloat() else 0f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(240.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = displayTime,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = dotoFont,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (timerPaused) "PAUSED" else "RUNNING",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = googleSansFont,
                                            fontWeight = FontWeight.Bold,
                                            color = if (timerPaused) Color(0xFFFF6B00) else MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (timerPaused) {
                                        alarmController.resumeTimer()
                                    } else {
                                        alarmController.pauseTimer()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (timerPaused) "RESUME" else "PAUSE",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = googleSansFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Button(
                                onClick = { alarmController.cancelTimer() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "CANCEL",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = googleSansFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Circular '+' Button at 60% height (Only visible in Alarms tab)
        if (selectedTab == 0) {
            Box(
                modifier = Modifier
                    .offset(y = fabOffset)
                    .align(Alignment.TopCenter)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { showAddAlarmDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Alarm",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (showAddAlarmDialog) {
            AddAlarmDialog(
                onDismiss = { showAddAlarmDialog = false },
                onAdd = { hour, minute, label ->
                    coroutineScope.launch {
                        alarmController.setAlarm(hour, minute, label)
                        refreshAlarms()
                        showAddAlarmDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun VerticalTimerSlider(
    selectedMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedMinutes - 1).coerceAtLeast(0))

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val centerIndex = listState.firstVisibleItemIndex + 2
        val clampedMinute = (centerIndex + 1).coerceIn(1, 120)
        onMinutesChanged(clampedMinute)
    }

    Box(modifier = modifier.height(250.dp)) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 100.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(120) { index ->
                val minute = index + 1
                val isSelected = minute == selectedMinutes
                Text(
                    text = "$minute min",
                    fontSize = if (isSelected) 36.sp else 22.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = googleSansFont,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable { onMinutesChanged(minute) },
                    textAlign = TextAlign.Center
                )
            }
        }

        // Top Fading Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0f)
                        )
                    )
                )
        )

        // Bottom Fading Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
    }
}

@Composable
fun AlarmCard(
    alarm: LocalAlarm,
    onDelete: () -> Unit
) {
    val amPm = if (alarm.hour >= 12) "PM" else "AM"
    val displayHour = when {
        alarm.hour == 0 -> 12
        alarm.hour > 12 -> alarm.hour - 12
        else -> alarm.hour
    }
    val displayTime = String.format("%02d:%02d", displayHour, alarm.minute)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = dotoFont,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPm,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = googleSansFont,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.label.ifBlank { "Alarm" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Alarm",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onAdd: (Int, Int, String) -> Unit
) {
    var hourText by remember { mutableStateOf("8") }
    var minuteText by remember { mutableStateOf("00") }
    var labelText by remember { mutableStateOf("Morning Alarm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set New Alarm", style = MaterialTheme.typography.titleLarge.copy(fontFamily = googleSansFont)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it },
                        label = { Text("Hour (0-23)", fontFamily = googleSansFont) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it },
                        label = { Text("Minute (0-59)", fontFamily = googleSansFont) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Alarm Label", fontFamily = googleSansFont) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 8
                    val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    onAdd(hour, minute, labelText)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text("SET ALARM", style = MaterialTheme.typography.labelLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.Bold))
            }
        }
    )
}
