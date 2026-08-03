package com.kairos.os.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kairos.os.domain.models.HomeActivityItem
import com.kairos.os.domain.session.AppSession
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

private fun HomeActivityItem.itemKey(): String = when (this) {
    is HomeActivityItem.Agent -> "agent:${agent.id}"
    is HomeActivityItem.AppGrant -> "grant:${session.packageName}:${session.grantedAtMs}"
}

@Composable
private fun HomeActivityCard(
    item: HomeActivityItem,
    onViewAgent: (String) -> Unit,
    onViewGrant: (AppSession) -> Unit,
    onDismissAgent: (String) -> Unit,
    onDismissGrant: (AppSession) -> Unit,
    statusLines: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    when (item) {
        is HomeActivityItem.Agent -> RunningAgentCard(
            agent = item.agent,
            onView = onViewAgent,
            onDismiss = onDismissAgent,
            statusLine = statusLines[item.agent.id],
            modifier = modifier
        )
        is HomeActivityItem.AppGrant -> AppGrantCard(
            session = item.session,
            onView = onViewGrant,
            onDismiss = onDismissGrant,
            modifier = modifier
        )
    }
}

@Composable
fun CollapsedAgentStack(
    items: List<HomeActivityItem>,
    totalCount: Int,
    onTapStack: () -> Unit,
    onViewAgent: (String) -> Unit,
    onViewGrant: (AppSession) -> Unit,
    onDismissAgent: (String) -> Unit,
    onDismissGrant: (AppSession) -> Unit,
    statusLines: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val topPadding = ((items.size - 1) * 20).dp

    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTapStack() }
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUNNING AGENTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (totalCount > 1) "$totalCount tasks  ▲" else "▲",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .clickable { onTapStack() }
        ) {
            items.reversed().forEachIndexed { index, item ->
                val reverseIndex = items.size - 1 - index
                val scale = 1f - (reverseIndex * 0.04f)
                val yOffset = (reverseIndex * 20).dp
                val alphaVal = if (reverseIndex == 0) 1f else 0.8f - (reverseIndex * 0.15f)

                HomeActivityCard(
                    item = item,
                    onViewAgent = onViewAgent,
                    onViewGrant = onViewGrant,
                    onDismissAgent = onDismissAgent,
                    onDismissGrant = onDismissGrant,
                    statusLines = statusLines,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = -yOffset.toPx()
                            alpha = alphaVal
                        }
                )
            }
        }
    }
}

@Composable
fun ExpandedAgentList(
    items: List<HomeActivityItem>,
    onCollapse: () -> Unit,
    onViewAgent: (String) -> Unit,
    onViewGrant: (AppSession) -> Unit,
    onDismissAgent: (String) -> Unit,
    onDismissGrant: (AppSession) -> Unit,
    hazeState: HazeState,
    statusLines: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCollapse() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeChild(state = hazeState)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE AGENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = items,
                    key = { it.itemKey() }
                ) { item ->
                    HomeActivityCard(
                        item = item,
                        onViewAgent = { id ->
                            onCollapse()
                            onViewAgent(id)
                        },
                        onViewGrant = { session ->
                            onCollapse()
                            onViewGrant(session)
                        },
                        onDismissAgent = onDismissAgent,
                        onDismissGrant = onDismissGrant,
                        statusLines = statusLines
                    )
                }
            }
        }
    }
}
