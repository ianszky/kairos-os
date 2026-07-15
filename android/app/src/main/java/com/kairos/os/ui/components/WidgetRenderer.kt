package com.kairos.os.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.kairos.os.domain.models.WidgetAction
import com.kairos.os.domain.models.WidgetItem
import com.kairos.os.domain.models.WidgetPayload

@Composable
fun WidgetRenderer(
    widget: WidgetPayload,
    appTarget: String? = null,
    onAction: (WidgetAction) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            widget.title?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
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
            
            if (!widget.actions.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                WidgetActionsRow(actions = widget.actions, appTarget = appTarget, onAction = onAction)
            }
        }
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)

@Composable
fun EmailListWidget(widget: WidgetPayload) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        widget.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.primary.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.primary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    item.secondary?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarEventWidget(widget: WidgetPayload) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        widget.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(item.metadata?.get("date") ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(item.metadata?.get("time") ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.primary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    item.secondary?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmConfirmWidget(widget: WidgetPayload) {
    GenericCardWidget(widget)
}

@Composable
fun NoteCardWidget(widget: WidgetPayload) {
    GenericCardWidget(widget)
}

@Composable
fun MusicCardWidget(widget: WidgetPayload) {
    GenericCardWidget(widget)
}

@Composable
fun SearchResultsWidget(widget: WidgetPayload) {
    GenericCardWidget(widget)
}

@Composable
fun DigestSummaryWidget(widget: WidgetPayload) {
    GenericCardWidget(widget)
}

@Composable
fun GenericCardWidget(widget: WidgetPayload) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        widget.items.forEach { item ->
            WidgetItemRow(item)
        }
    }
}

@Composable
fun WidgetItemRow(item: WidgetItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text = item.primary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        item.secondary?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WidgetActionsRow(
    actions: List<WidgetAction>,
    appTarget: String? = null,
    onAction: (WidgetAction) -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            val isDark = MaterialTheme.colorScheme.surface == Color(0xFF111111)
            val btnBgColor = if (isDark) Color(0xFFE65F00) else MaterialTheme.colorScheme.primary
            val btnTextColor = if (isDark) Color.White else Color.Black

            Button(
                onClick = { onAction(action) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnBgColor,
                    contentColor = btnTextColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!appTarget.isNullOrEmpty()) {
                        AsyncImage(
                            model = "https://logos.composio.dev/api/$appTarget",
                            imageLoader = imageLoader,
                            contentDescription = appTarget,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = btnTextColor
                        )
                    )
                }
            }
        }
    }
}
