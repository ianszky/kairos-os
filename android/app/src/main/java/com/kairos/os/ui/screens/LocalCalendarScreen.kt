package com.kairos.os.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kairos.os.domain.tools.CalendarEvent
import com.kairos.os.domain.tools.LocalCalendarController
import com.kairos.os.ui.googleSansFont
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LocalCalendarScreen(
    calendarController: LocalCalendarController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun refreshEvents() {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val endOfSevenDays = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        events = calendarController.listEvents(startOfDay, endOfSevenDays)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CALENDAR] == true &&
            permissions[Manifest.permission.WRITE_CALENDAR] == true) {
            refreshEvents()
        }
    }

    LaunchedEffect(Unit) {
        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (!hasRead || !hasWrite) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
        } else {
            refreshEvents()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "No Events",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Upcoming Events",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = googleSansFont),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+' floating button or prompt @kaicalendar in chat.",
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
                    items(events, key = { it.id }) { event ->
                        CalendarEventCard(event = event)
                    }
                }
            }
        }

        // Floating Circular '+' Button at Bottom Right with sufficient margins
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 8.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Event",
                modifier = Modifier.size(28.dp)
            )
        }

        if (showAddDialog) {
            AddCalendarEventDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, description, duration ->
                    coroutineScope.launch {
                        val startMillis = Calendar.getInstance().timeInMillis + (30 * 60 * 1000)
                        calendarController.createEvent(title, description, startMillis, duration)
                        refreshEvents()
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun CalendarEventCard(event: CalendarEvent) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val startStr = dateFormat.format(Date(event.startMillis))
    val endFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val endStr = endFormat.format(Date(event.endMillis))

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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = googleSansFont,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$startStr - $endStr",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    fontFamily = googleSansFont
                )
                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = googleSansFont
                    )
                }
            }
        }
    }
}

@Composable
fun AddCalendarEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule New Event", style = MaterialTheme.typography.titleLarge.copy(fontFamily = googleSansFont)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Event Title", fontFamily = googleSansFont) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Description", fontFamily = googleSansFont) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Duration (minutes)", fontFamily = googleSansFont) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleText.isNotBlank()) {
                        val duration = durationText.toIntOrNull() ?: 60
                        onAdd(titleText, descText, duration)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text("SCHEDULE", style = MaterialTheme.typography.labelLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.Bold))
            }
        }
    )
}
