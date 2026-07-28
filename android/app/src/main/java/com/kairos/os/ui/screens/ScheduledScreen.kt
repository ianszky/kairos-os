package com.kairos.os.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairos.os.domain.models.ScheduledTask
import com.kairos.os.domain.models.ScheduledTaskRun
import com.kairos.os.ui.dotoFont
import com.kairos.os.ui.googleSansFont
import com.kairos.os.ui.viewmodels.ScheduledViewModel

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

    // Input state
    var inputText by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("daily") } // "daily", "weekly", "specific_days"
    var selectedHour by remember { mutableIntStateOf(9) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf(1, 3, 5)) } // Mon, Wed, Fri default

    // Modals
    var editingTask by remember { mutableStateOf<ScheduledTask?>(null) }
    var manualRunTask by remember { mutableStateOf<ScheduledTask?>(null) }
    var taskToDelete by remember { mutableStateOf<ScheduledTask?>(null) }

    val daysOfWeekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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
                        text = "SCHEDULED",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
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
                            subtitle = "Executed cron tasks will appear here as clickable conversation runs."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 180.dp)
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
                            subtitle = "Type a prompt below and set a schedule to automate your workflows."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 220.dp)
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

        // Floating Bottom Input & Friction-style Schedule Config Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Animated Schedule Config Panel (slides up when typing text in input)
            AnimatedVisibility(
                visible = inputText.trim().isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SCHEDULE CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Frequency selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "daily" to "Daily",
                            "weekly" to "Weekly",
                            "specific_days" to "Specific Days"
                        ).forEach { (freqKey, freqLabel) ->
                            val isSelected = selectedFrequency == freqKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.background
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedFrequency = freqKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = freqLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = googleSansFont,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Days selector chips (if weekly or specific_days)
                    if (selectedFrequency != "daily") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Days to run",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Time to run",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Hour selector
                            OutlinedTextField(
                                value = String.format("%02d", selectedHour),
                                onValueChange = { input ->
                                    input.toIntOrNull()?.let { selectedHour = it.coerceIn(0, 23) }
                                },
                                modifier = Modifier.width(56.dp),
                                textStyle = TextStyle(
                                    fontFamily = dotoFont,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            Text(
                                text = ":",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            // Minute selector
                            OutlinedTextField(
                                value = String.format("%02d", selectedMinute),
                                onValueChange = { input ->
                                    input.toIntOrNull()?.let { selectedMinute = it.coerceIn(0, 59) }
                                },
                                modifier = Modifier.width(56.dp),
                                textStyle = TextStyle(
                                    fontFamily = dotoFont,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Schedule Submit Button
                    Button(
                        onClick = {
                            val promptText = inputText.trim()
                            if (promptText.isNotEmpty()) {
                                val timeStr = String.format("%02d:%02d:00", selectedHour, selectedMinute)
                                scheduledViewModel.createScheduledTask(
                                    prompt = promptText,
                                    appTarget = null,
                                    frequency = selectedFrequency,
                                    daysOfWeek = selectedDays.toList().sorted(),
                                    timeOfDay = timeStr,
                                    onSuccess = {
                                        inputText = ""
                                        selectedTab = 1 // Switch to JOBS tab to view created task
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACTIVATE CRON JOB",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = googleSansFont,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Chat Input Bar (reusing existing app drawer / chatbox pattern)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "@",
                    style = TextStyle(
                        fontFamily = googleSansFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = googleSansFont,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Schedule a task (e.g. @gmail check unread emails)...",
                                style = TextStyle(
                                    fontFamily = googleSansFont,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
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
                        text = "Run task '${task.title ?: task.prompt.take(30)}' right now? This will dispatch an agent card on the main screen.",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                Row {
                    // Separate explicit "Run Manually" button directly on the job card
                    IconButton(
                        onClick = onRunManually,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Manually",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Job",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Schedule info badge
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
                        fontFamily = dotoFont,
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
