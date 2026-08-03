package com.kairos.os.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kairos.os.domain.tools.CalendarEvent
import com.kairos.os.domain.tools.LocalCalendarController
import com.kairos.os.ui.googleSansFont
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// KAIROS Brand Orange Warm Accent Color
private val OrangeAccent = Color(0xFFFF6B00)
private val GrayMutedText = Color(0xFF888888)

@Composable
fun LocalCalendarScreen(
    calendarController: LocalCalendarController,
    viewMode: String = "week",
    onBack: () -> Unit,
    onRefreshActionReady: ((() -> Unit) -> Unit) = {},
    onSyncingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var observerDebounceJob by remember { mutableStateOf<Job?>(null) }

    fun refreshEvents() {
        val startWindow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val endWindow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 60)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        events = calendarController.listEvents(startWindow, endWindow)
    }

    fun syncAndRefresh() {
        isSyncing = true
        calendarController.requestCloudSync()
        refreshEvents()
        coroutineScope.launch {
            delay(3000)
            refreshEvents()
            isSyncing = false
        }
    }

    SideEffect {
        onRefreshActionReady { syncAndRefresh() }
        onSyncingChanged(isSyncing)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CALENDAR] == true &&
            permissions[Manifest.permission.WRITE_CALENDAR] == true) {
            syncAndRefresh()
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
        }
    }

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                if (hasRead && hasWrite) {
                    syncAndRefresh()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }

    DisposableEffect(context) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                observerDebounceJob?.cancel()
                observerDebounceJob = coroutineScope.launch {
                    delay(300)
                    refreshEvents()
                    isSyncing = false
                }
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI, true, observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            observerDebounceJob?.cancel()
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .padding(horizontal = 16.dp)
    ) {
        if (viewMode == "month") {
            MonthViewContent(
                events = events,
                onEventClick = { editingEvent = it }
            )
        } else {
            WeekViewContent(
                events = events,
                onEventClick = { editingEvent = it }
            )
        }

        // Floating Circular '+' Button at Bottom Right
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 40.dp, end = 12.dp),
            containerColor = OrangeAccent,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Schedule Event",
                modifier = Modifier.size(28.dp)
            )
        }

        if (showAddDialog) {
            AddCalendarEventDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, description, startMillis, endMillis, isAllDay, syncGoogle ->
                    coroutineScope.launch {
                        calendarController.createEvent(
                            title = title,
                            description = description,
                            startMillis = startMillis,
                            endMillis = endMillis,
                            isAllDay = isAllDay,
                            syncGoogle = syncGoogle
                        )
                        refreshEvents()
                        showAddDialog = false
                    }
                }
            )
        }

        editingEvent?.let { targetEvent ->
            EditCalendarEventDialog(
                event = targetEvent,
                onDismiss = { editingEvent = null },
                onUpdate = { title, description, startMillis, endMillis, isAllDay, syncGoogle ->
                    coroutineScope.launch {
                        calendarController.updateEvent(
                            eventId = targetEvent.id,
                            title = title,
                            description = description,
                            startMillis = startMillis,
                            endMillis = endMillis,
                            isAllDay = isAllDay,
                            syncGoogle = syncGoogle
                        )
                        refreshEvents()
                        editingEvent = null
                    }
                },
                onDelete = {
                    coroutineScope.launch {
                        calendarController.deleteEvent(targetEvent.id)
                        refreshEvents()
                        editingEvent = null
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// WEEK VIEW (7-Day Collapsible Accordion)
// ---------------------------------------------------------------------------
@Composable
fun WeekViewContent(
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit
) {
    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var expandedDays by remember { mutableStateOf(setOf(todayDateStr)) }

    val daysList = remember {
        val list = mutableListOf<Calendar>()
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, i)
            list.add(dayCal)
        }
        list
    }

    val dayHeaderFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val dayKeyFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        items(daysList, key = { dayKeyFormatter.format(it.time) }) { dayCal ->
            val dayKey = dayKeyFormatter.format(dayCal.time)
            val isExpanded = expandedDays.contains(dayKey)
            val isToday = dayKey == todayDateStr

            // Filter events for this day
            val startOfDay = dayCal.clone() as Calendar
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)
            startOfDay.set(Calendar.MILLISECOND, 0)

            val endOfDay = dayCal.clone() as Calendar
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)
            endOfDay.set(Calendar.MILLISECOND, 999)

            val dayEvents = events.filter {
                it.startMillis >= startOfDay.timeInMillis && it.startMillis <= endOfDay.timeInMillis
            }

            // Clean borderless container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isToday) OrangeAccent.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedDays = if (isExpanded) {
                                    expandedDays - dayKey
                                } else {
                                    expandedDays + dayKey
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dayHeaderFormatter.format(dayCal.time),
                                fontSize = 16.sp,
                                fontWeight = if (isExpanded || isToday) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = googleSansFont,
                                color = when {
                                    isToday -> OrangeAccent
                                    isExpanded -> MaterialTheme.colorScheme.onSurface
                                    else -> GrayMutedText
                                }
                            )
                            if (isToday) {
                                Text(
                                    text = "Today",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeAccent,
                                    fontFamily = googleSansFont
                                )
                            }
                        }

                        // Event Count Badge
                        if (dayEvents.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isExpanded || isToday) OrangeAccent else GrayMutedText.copy(alpha = 0.4f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${dayEvents.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isExpanded || isToday) MaterialTheme.colorScheme.onSurfaceVariant else GrayMutedText
                        )
                    }

                    // Collapsible Content
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (dayEvents.isEmpty()) {
                                Text(
                                    text = "No events scheduled for this day.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = googleSansFont,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                dayEvents.forEach { event ->
                                    CalendarEventCard(event = event, onClick = { onEventClick(event) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// MONTH VIEW (Top 50% Grid + Bottom 50% Day Agenda)
// ---------------------------------------------------------------------------
@Composable
fun MonthViewContent(
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit
) {
    var displayMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    val monthHeaderFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val isSameDay: (Calendar, Calendar) -> Boolean = { cal1, cal2 ->
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP 50%: Calendar Month Grid Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            shape = RoundedCornerShape(20.dp),
            border = null,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header: < [Month Year] >
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newMonth = displayMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, -1)
                        displayMonth = newMonth
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = monthHeaderFormat.format(displayMonth.time),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = googleSansFont,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = {
                        val newMonth = displayMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, 1)
                        displayMonth = newMonth
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Weekday Labels
                val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdays.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeAccent,
                            fontFamily = googleSansFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid Calculation
                val daysInMonth = displayMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfWeek = displayMonth.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sun=0)

                val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                    for (row in 0 until (totalCells / 7)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - firstDayOfWeek + 1

                                if (dayNum in 1..daysInMonth) {
                                    val cellCal = displayMonth.clone() as Calendar
                                    cellCal.set(Calendar.DAY_OF_MONTH, dayNum)

                                    val isSelected = isSameDay(cellCal, selectedDate)
                                    val isToday = isSameDay(cellCal, Calendar.getInstance())

                                    // Check if this date has events
                                    val cellStart = cellCal.clone() as Calendar
                                    cellStart.set(Calendar.HOUR_OF_DAY, 0)
                                    cellStart.set(Calendar.MINUTE, 0)
                                    val cellEnd = cellCal.clone() as Calendar
                                    cellEnd.set(Calendar.HOUR_OF_DAY, 23)
                                    cellEnd.set(Calendar.MINUTE, 59)

                                    val hasEvents = events.any {
                                        it.startMillis >= cellStart.timeInMillis && it.startMillis <= cellEnd.timeInMillis
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> OrangeAccent
                                                    isToday -> OrangeAccent.copy(alpha = 0.25f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { selectedDate = cellCal },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dayNum",
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> OrangeAccent
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                fontFamily = googleSansFont
                                            )
                                            if (hasEvents && !isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(OrangeAccent)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // BOTTOM 50%: Selected Day Agenda List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f),
            shape = RoundedCornerShape(20.dp),
            border = null,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            )
        ) {
            val selectedDayFormat = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }

            // Filter events for selectedDate
            val startOfDay = selectedDate.clone() as Calendar
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)

            val endOfDay = selectedDate.clone() as Calendar
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)

            val selectedDayEvents = events.filter {
                it.startMillis >= startOfDay.timeInMillis && it.startMillis <= endOfDay.timeInMillis
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "${selectedDayFormat.format(selectedDate.time)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeAccent,
                    fontFamily = googleSansFont
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedDayEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No events on this day.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = googleSansFont
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(selectedDayEvents, key = { it.id }) { event ->
                            SlimCalendarEventCard(event = event, onClick = { onEventClick(event) })
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// EVENT CARDS
// ---------------------------------------------------------------------------
@Composable
fun CalendarEventCard(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val startStr = if (event.isAllDay) "All Day" else dateFormat.format(Date(event.startMillis))
    val endFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val endStr = if (event.isAllDay) "" else " - ${endFormat.format(Date(event.endMillis))}"

    val accountLabel = when {
        !event.accountName.isNullOrBlank() -> event.accountName
        !event.calendarName.isNullOrBlank() -> event.calendarName
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OrangeAccent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = googleSansFont,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$startStr$endStr",
                    fontSize = 12.sp,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Medium,
                    fontFamily = googleSansFont
                )
                if (accountLabel != null) {
                    Text(
                        text = "📅 $accountLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontFamily = googleSansFont
                    )
                }
                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = googleSansFont
                    )
                }
            }
        }
    }
}

@Composable
fun SlimCalendarEventCard(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = if (event.isAllDay) "All Day" else "${timeFormat.format(Date(event.startMillis))} - ${timeFormat.format(Date(event.endMillis))}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OrangeAccent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = googleSansFont,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = OrangeAccent,
                    fontFamily = googleSansFont
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ADD CALENDAR EVENT DIALOG
// ---------------------------------------------------------------------------
@Composable
fun AddCalendarEventDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, description: String, startMillis: Long, endMillis: Long, isAllDay: Boolean, syncGoogle: Boolean) -> Unit
) {
    val context = LocalContext.current
    var titleText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }

    val startCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
    }
    val endCal = remember {
        startCal.clone() as Calendar
    }.apply {
        add(Calendar.HOUR_OF_DAY, 1)
    }

    var startDateState by remember { mutableStateOf(startCal.clone() as Calendar) }
    var startTimeState by remember { mutableStateOf(Pair(startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE))) }
    var endTimeState by remember { mutableStateOf(Pair(endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))) }

    var isAllDay by remember { mutableStateOf(false) }
    var syncGoogle by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    val openDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = startDateState.clone() as Calendar
                newCal.set(Calendar.YEAR, year)
                newCal.set(Calendar.MONTH, month)
                newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                startDateState = newCal
            },
            startDateState.get(Calendar.YEAR),
            startDateState.get(Calendar.MONTH),
            startDateState.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val openStartTimePicker = {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startTimeState = Pair(hourOfDay, minute)
            },
            startTimeState.first,
            startTimeState.second,
            false
        ).show()
    }

    val openEndTimePicker = {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                endTimeState = Pair(hourOfDay, minute)
            },
            endTimeState.first,
            endTimeState.second,
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule New Event", style = MaterialTheme.typography.titleLarge.copy(fontFamily = googleSansFont)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                // Date Picker Button
                OutlinedButton(
                    onClick = openDatePicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(18.dp), tint = OrangeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateFormat.format(startDateState.time), fontFamily = googleSansFont)
                }

                // All-Day Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAllDay = !isAllDay }
                ) {
                    Checkbox(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it },
                        colors = CheckboxDefaults.colors(checkedColor = OrangeAccent)
                    )
                    Text("All-Day Event", fontFamily = googleSansFont, fontSize = 14.sp)
                }

                // Time Pickers (hidden if All-Day)
                if (!isAllDay) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = openStartTimePicker,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Start Time", modifier = Modifier.size(16.dp), tint = OrangeAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format("%02d:%02d", startTimeState.first, startTimeState.second), fontFamily = googleSansFont, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = openEndTimePicker,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = "End Time", modifier = Modifier.size(16.dp), tint = OrangeAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format("%02d:%02d", endTimeState.first, endTimeState.second), fontFamily = googleSansFont, fontSize = 13.sp)
                        }
                    }
                }

                // Sync with Google / Connected Calendars Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { syncGoogle = !syncGoogle }
                ) {
                    Checkbox(
                        checked = syncGoogle,
                        onCheckedChange = { syncGoogle = it },
                        colors = CheckboxDefaults.colors(checkedColor = OrangeAccent)
                    )
                    Text("Sync with Google / Connected Calendars", fontFamily = googleSansFont, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleText.isNotBlank()) {
                        val finalStartCal = startDateState.clone() as Calendar
                        val finalEndCal = startDateState.clone() as Calendar

                        if (isAllDay) {
                            finalStartCal.set(Calendar.HOUR_OF_DAY, 0)
                            finalStartCal.set(Calendar.MINUTE, 0)
                            finalStartCal.set(Calendar.SECOND, 0)
                            finalStartCal.set(Calendar.MILLISECOND, 0)

                            finalEndCal.set(Calendar.HOUR_OF_DAY, 23)
                            finalEndCal.set(Calendar.MINUTE, 59)
                            finalEndCal.set(Calendar.SECOND, 59)
                            finalEndCal.set(Calendar.MILLISECOND, 999)
                        } else {
                            finalStartCal.set(Calendar.HOUR_OF_DAY, startTimeState.first)
                            finalStartCal.set(Calendar.MINUTE, startTimeState.second)

                            finalEndCal.set(Calendar.HOUR_OF_DAY, endTimeState.first)
                            finalEndCal.set(Calendar.MINUTE, endTimeState.second)
                        }

                        onAdd(
                            titleText,
                            descText,
                            finalStartCal.timeInMillis,
                            finalEndCal.timeInMillis,
                            isAllDay,
                            syncGoogle
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White
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

// ---------------------------------------------------------------------------
// EDIT / DELETE CALENDAR EVENT DIALOG
// ---------------------------------------------------------------------------
@Composable
fun EditCalendarEventDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onUpdate: (title: String, description: String, startMillis: Long, endMillis: Long, isAllDay: Boolean, syncGoogle: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var titleText by remember { mutableStateOf(event.title) }
    var descText by remember { mutableStateOf(event.description) }

    val startCal = remember {
        Calendar.getInstance().apply { timeInMillis = event.startMillis }
    }
    val endCal = remember {
        Calendar.getInstance().apply { timeInMillis = event.endMillis }
    }

    var startDateState by remember { mutableStateOf(startCal.clone() as Calendar) }
    var startTimeState by remember { mutableStateOf(Pair(startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE))) }
    var endTimeState by remember { mutableStateOf(Pair(endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))) }

    var isAllDay by remember { mutableStateOf(event.isAllDay) }
    var syncGoogle by remember { mutableStateOf(event.accountName?.contains("google", ignoreCase = true) ?: true) }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    val openDatePicker = {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = startDateState.clone() as Calendar
                newCal.set(Calendar.YEAR, year)
                newCal.set(Calendar.MONTH, month)
                newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                startDateState = newCal
            },
            startDateState.get(Calendar.YEAR),
            startDateState.get(Calendar.MONTH),
            startDateState.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val openStartTimePicker = {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startTimeState = Pair(hourOfDay, minute)
            },
            startTimeState.first,
            startTimeState.second,
            false
        ).show()
    }

    val openEndTimePicker = {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                endTimeState = Pair(hourOfDay, minute)
            },
            endTimeState.first,
            endTimeState.second,
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event", style = MaterialTheme.typography.titleLarge.copy(fontFamily = googleSansFont)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                // Date Picker Button
                OutlinedButton(
                    onClick = openDatePicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(18.dp), tint = OrangeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateFormat.format(startDateState.time), fontFamily = googleSansFont)
                }

                // All-Day Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAllDay = !isAllDay }
                ) {
                    Checkbox(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it },
                        colors = CheckboxDefaults.colors(checkedColor = OrangeAccent)
                    )
                    Text("All-Day Event", fontFamily = googleSansFont, fontSize = 14.sp)
                }

                // Time Pickers (hidden if All-Day)
                if (!isAllDay) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = openStartTimePicker,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Start Time", modifier = Modifier.size(16.dp), tint = OrangeAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format("%02d:%02d", startTimeState.first, startTimeState.second), fontFamily = googleSansFont, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = openEndTimePicker,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = "End Time", modifier = Modifier.size(16.dp), tint = OrangeAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format("%02d:%02d", endTimeState.first, endTimeState.second), fontFamily = googleSansFont, fontSize = 13.sp)
                        }
                    }
                }

                // Sync with Google / Connected Calendars Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { syncGoogle = !syncGoogle }
                ) {
                    Checkbox(
                        checked = syncGoogle,
                        onCheckedChange = { syncGoogle = it },
                        colors = CheckboxDefaults.colors(checkedColor = OrangeAccent)
                    )
                    Text("Sync with Google / Connected Calendars", fontFamily = googleSansFont, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Event",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        if (titleText.isNotBlank()) {
                            val finalStartCal = startDateState.clone() as Calendar
                            val finalEndCal = startDateState.clone() as Calendar

                            if (isAllDay) {
                                finalStartCal.set(Calendar.HOUR_OF_DAY, 0)
                                finalStartCal.set(Calendar.MINUTE, 0)
                                finalStartCal.set(Calendar.SECOND, 0)
                                finalStartCal.set(Calendar.MILLISECOND, 0)

                                finalEndCal.set(Calendar.HOUR_OF_DAY, 23)
                                finalEndCal.set(Calendar.MINUTE, 59)
                                finalEndCal.set(Calendar.SECOND, 59)
                                finalEndCal.set(Calendar.MILLISECOND, 999)
                            } else {
                                finalStartCal.set(Calendar.HOUR_OF_DAY, startTimeState.first)
                                finalStartCal.set(Calendar.MINUTE, startTimeState.second)

                                finalEndCal.set(Calendar.HOUR_OF_DAY, endTimeState.first)
                                finalEndCal.set(Calendar.MINUTE, endTimeState.second)
                            }

                            onUpdate(
                                titleText,
                                descText,
                                finalStartCal.timeInMillis,
                                finalEndCal.timeInMillis,
                                isAllDay,
                                syncGoogle
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text("SAVE", style = MaterialTheme.typography.labelLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.Bold))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.Bold))
            }
        }
    )
}
