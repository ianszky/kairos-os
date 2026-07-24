package com.kairos.os.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import com.kairos.os.domain.models.RunningAgent
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

@Composable
fun CollapsedAgentStack(
    agents: List<RunningAgent>,
    totalCount: Int,
    onTapStack: () -> Unit,
    onViewAgent: (String) -> Unit,
    onCancelAgent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (agents.isEmpty()) return

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
                .clickable { onTapStack() }
        ) {
            agents.reversed().forEachIndexed { index, agent ->
                val reverseIndex = agents.size - 1 - index
                val scale = 1f - (reverseIndex * 0.04f)
                val yOffset = reverseIndex * 8.dp
                val alphaVal = if (reverseIndex == 0) 1f else 0.75f - (reverseIndex * 0.2f)

                RunningAgentCard(
                    agent = agent,
                    onView = onViewAgent,
                    onCancel = onCancelAgent,
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
    agents: List<RunningAgent>,
    onCollapse: () -> Unit,
    onViewAgent: (String) -> Unit,
    onCancelAgent: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeChild(state = hazeState)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCollapse() }
    ) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = agents,
                    key = { it.id }
                ) { agent ->
                    RunningAgentCard(
                        agent = agent,
                        onView = { id ->
                            onCollapse()
                            onViewAgent(id)
                        },
                        onCancel = onCancelAgent
                    )
                }
            }
        }
    }
}
