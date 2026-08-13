package com.kairos.os.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun AgentThinkingIndicator(statusLine: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .padding(start = 0.dp, top = 16.dp, bottom = 8.dp, end = 18.dp)
        ) {
            val displayLine = statusLine?.takeIf { it.isNotBlank() } ?: "Thinking…"
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(20.dp)
                ) {
                    Dot(delayMillis = 0)
                    Dot(delayMillis = 150)
                    Dot(delayMillis = 300)
                }

                AnimatedContent(
                    targetState = displayLine,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "statusLine"
                ) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** @deprecated Use [AgentThinkingIndicator] */
@Composable
fun TypingIndicator() {
    AgentThinkingIndicator()
}

@Composable
private fun Dot(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis)
        ),
        label = "yOffset"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = yOffset.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}
