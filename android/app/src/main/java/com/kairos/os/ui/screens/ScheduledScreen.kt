package com.kairos.os.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairos.os.domain.models.ScheduledTask
import com.kairos.os.domain.models.ScheduledTaskRun
import com.kairos.os.ui.googleSansFont
import com.kairos.os.ui.viewmodels.ScheduledViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledScreen(
    scheduledViewModel: ScheduledViewModel,
    onOpenConversation: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = RUNS, 1 = JOBS
    val tasks by scheduledViewModel.tasks.collectAsState()
    val runs by scheduledViewModel.runs.collectAsState()
    val isLoading by scheduledViewModel.isLoading.collectAsState()

    // Modals
    var editingTask by remember { mutableStateOf<ScheduledTask?>(null) }
    var manualRunTask by remember { mutableStateOf<ScheduledTask?>(null) }
    var taskToDelete by remember { mutableStateOf<ScheduledTask?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SCHEDULED TASKS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = { scheduledViewModel.refreshAll() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Tab Row (RUNS / JOBS)
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
                            text = "RUNS (${runs.size})",
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
                            text = "JOBS (${tasks.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = googleSansFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedTab == 0) {
                    // RUNS TAB
                    if (runs.isEmpty()) {
                        EmptyScheduledPlaceholder(
                            title = "No Processes Executed Yet",
                            subtitle = "Executed scheduled tasks will appear here as clickable conversation runs."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 220.dp)
                        ) {
                            items(runs, key = { it.id }) { run ->
                                ScheduledRunCard(
                                    run = run,
                                    onClick = {
                                        run.conversationId?.let { convId ->
                                            onOpenConversation(convId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // JOBS TAB
                    if (tasks.isEmpty()) {
                        EmptyScheduledPlaceholder(
                            title = "Scheduled Tasks Live Here",
                            subtitle = "Type a prompt in the chatbox below to configure and activate your automation."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 240.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                ScheduledJobCard(
                                    task = task,
                                    onEdit = { editingTask = task },
                                    onRunManually = { manualRunTask = task },
                                    onToggleActive = { scheduledViewModel.toggleTaskActive(task) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit Modal
        editingTask?.let { task ->
            ScheduleEditModal(
                task = task,
                onDismiss = { editingTask = null },
                onSave = { updatedTask ->
                    scheduledViewModel.updateScheduledTask(
                        taskId = updatedTask.id,
                        prompt = updatedTask.prompt,
                        title = updatedTask.title,
                        frequency = updatedTask.frequency,
                        daysOfWeek = updatedTask.daysOfWeek,
                        timeOfDay = updatedTask.timeOfDay,
                        isActive = updatedTask.isActive,
                        onSuccess = { editingTask = null }
                    )
                },
                onDelete = {
                    taskToDelete = task
                    editingTask = null
                }
            )
        }

        // Manual Run Confirmation Modal
        manualRunTask?.let { task ->
            AlertDialog(
                onDismissRequest = { manualRunTask = null },
                title = {
                    Text(
                        text = "Run Manually",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Run task '${task.title ?: task.prompt.take(30)}' right now? This will dispatch an agent card on the home screen.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val t = task
                            manualRunTask = null
                            scheduledViewModel.runTaskManually(t)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("RUN NOW", fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { manualRunTask = null }) {
                        Text("CANCEL", fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Delete Confirmation Modal
        taskToDelete?.let { task ->
            AlertDialog(
                onDismissRequest = { taskToDelete = null },
                title = {
                    Text(
                        text = "Delete Scheduled Task",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete '${task.title ?: task.prompt.take(30)}'?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = task.id
                            taskToDelete = null
                            scheduledViewModel.deleteScheduledTask(id)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("DELETE", fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskToDelete = null }) {
                        Text("CANCEL", fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

/**
 * Schedule Configuration Panel rendered directly BELOW the bottom chat input box
 * inside LauncherActivity.kt (mirroring the Intentional Friction Layer UI).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleConfigBelowInputPanel(
    promptText: String,
    onActivate: (frequency: String, daysOfWeek: List<Int>, timeOfDay: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFrequency by remember { mutableStateOf("daily") } // "daily", "weekly"
    var selectedHour by remember { mutableIntStateOf(9) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf(1)) } // Mon default
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val daysOfWeekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Frequency Selector (Daily vs Weekly)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "daily" to "Daily",
                "weekly" to "Weekly"
            ).forEach { (freqKey, freqLabel) ->
                val isSelected = selectedFrequency == freqKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.background,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedFrequency = freqKey }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = freqLabel,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = googleSansFont,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        // Day of week chips (only visible when Weekly is selected)
        if (selectedFrequency == "weekly") {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Select days to run",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeekLabels.forEachIndexed { index, label ->
                    val isSelected = selectedDays.contains(index)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.background
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .clickable {
                                selectedDays = if (isSelected) {
                                    selectedDays - index
                                } else {
                                    selectedDays + index
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.take(1),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = googleSansFont,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Time to run button triggering Material 3 TimePicker Dialog
        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
        val amPmStr = if (selectedHour >= 12) "PM" else "AM"
        val displayHour12 = when {
            selectedHour == 0 -> 12
            selectedHour > 12 -> selectedHour - 12
            else -> selectedHour
        }
        val displayTime12 = String.format("%02d:%02d %s", displayHour12, selectedMinute, amPmStr)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .clickable { showTimePickerDialog = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time to run",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Time to run",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = displayTime12,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = googleSansFont,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ACTIVATE Button
        Button(
            onClick = {
                val timeStr = String.format("%02d:%02d:00", selectedHour, selectedMinute)
                onActivate(selectedFrequency, selectedDays.toList().sorted(), timeStr)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )
        ) {
            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ACTIVATE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = googleSansFont,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    // Material 3 TimePicker Dialog
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = {
                Text(
                    text = "Select Time to Run",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = googleSansFont,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("CANCEL", fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun EmptyScheduledPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = googleSansFont,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ScheduledJobCard(
    task: ScheduledTask,
    onEdit: () -> Unit,
    onRunManually: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleActive() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (task.isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (task.isActive) "ACTIVE" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (task.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Separate explicit "Run Manually" button on job card
                    OutlinedButton(
                        onClick = onRunManually,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Manually",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Run",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = googleSansFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Job",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.title ?: task.prompt.take(45),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = googleSansFont,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = task.prompt,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Schedule info badge using Google Sans font
            val daysLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val daysText = if (task.frequency == "daily") "Daily"
            else if (task.daysOfWeek.isNotEmpty()) task.daysOfWeek.map { daysLabels.getOrElse(it) { "" } }.joinToString(", ")
            else "Weekly"

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$daysText at ${task.timeOfDay.take(5)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = googleSansFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ScheduledRunCard(
    run: ScheduledTaskRun,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (run.status) {
                        "completed" -> Icons.Default.CheckCircle
                        "failed" -> Icons.Default.Error
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = run.status,
                    tint = when (run.status) {
                        "completed" -> Color(0xFF4CAF50)
                        "failed" -> Color(0xFFEF5350)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = run.id.take(8) + " - Scheduled Process",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ran on ${run.startedAt.take(16).replace("T", " ")}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(onClick = onClick) {
                Text(
                    text = "View Chat",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = googleSansFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditModal(
    task: ScheduledTask,
    onDismiss: () -> Unit,
    onSave: (ScheduledTask) -> Unit,
    onDelete: () -> Unit
) {
    var prompt by remember { mutableStateOf(task.prompt) }
    var title by remember { mutableStateOf(task.title ?: "") }
    var frequency by remember { mutableStateOf(task.frequency) }
    var isActive by remember { mutableStateOf(task.isActive) }
    var timeOfDay by remember { mutableStateOf(task.timeOfDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Scheduled Task",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = googleSansFont,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", fontFamily = googleSansFont) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt", fontFamily = googleSansFont) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Status",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont)
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        task.copy(
                            prompt = prompt,
                            title = title,
                            frequency = frequency,
                            isActive = isActive,
                            timeOfDay = timeOfDay
                        )
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text("SAVE", fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error, fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", fontFamily = googleSansFont, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
