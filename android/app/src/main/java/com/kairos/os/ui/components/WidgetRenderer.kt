package com.kairos.os.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kairos.os.domain.models.WidgetPayload
import com.kairos.os.domain.models.WidgetItem

@Composable
fun WidgetRenderer(widget: WidgetPayload) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            widget.title?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            when (widget.widgetType) {
                "EMAIL_LIST" -> EmailListWidget(widget)
                "CALENDAR_EVENT" -> CalendarEventWidget(widget)
                "ALARM_CONFIRM" -> AlarmConfirmWidget(widget)
                "NOTE_CARD" -> NoteCardWidget(widget)
                "MUSIC_CARD" -> MusicCardWidget(widget)
                "SEARCH_RESULTS" -> SearchResultsWidget(widget)
                "DIGEST_SUMMARY" -> DigestSummaryWidget(widget)
                else -> GenericCardWidget(widget)
            }
        }
    }
}

@Composable
fun EmailListWidget(widget: WidgetPayload) {
    Column {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun CalendarEventWidget(widget: WidgetPayload) {
    Column {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun AlarmConfirmWidget(widget: WidgetPayload) {
    Column {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun NoteCardWidget(widget: WidgetPayload) {
    Column {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun MusicCardWidget(widget: WidgetPayload) {
    Column {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun SearchResultsWidget(widget: WidgetPayload) {
    Column {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun DigestSummaryWidget(widget: WidgetPayload) {
    Column {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun GenericCardWidget(widget: WidgetPayload) {
    Column {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun WidgetItemRow(item: WidgetItem) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = item.primary, style = MaterialTheme.typography.bodyLarge)
        item.secondary?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
}
